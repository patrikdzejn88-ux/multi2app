package com.multi2.app.engine.install

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.multi2.app.engine.EnginePaths
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedApk(
    val pkg: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val apkFiles: List<File>,
)

/**
 * Real APK installer into the sandbox:
 *  - clone from host: copies base + split apks of an installed app
 *  - import from file: .apk direct, .xapk/.apks as zip containers
 *  - parses metadata via PackageManager.getPackageArchiveInfo
 */
@Singleton
class ApkInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Copy host app's apks into the sandbox and parse them. */
    suspend fun installFromHost(pkg: String, spaceId: Int): ParsedApk = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val info: ApplicationInfo = pm.getApplicationInfo(pkg, 0)
        val sources = buildList {
            add(info.sourceDir)
            info.splitSourceDirs?.let { addAll(it) }
        }
        val destDir = EnginePaths.apkDir(context, spaceId, pkg).apply { deleteRecursively(); mkdirs() }
        val copied = sources.mapIndexed { i, src ->
            val name = if (i == 0) "base.apk" else "split_$i.apk"
            val dest = File(destDir, name)
            File(src).inputStream().use { input -> dest.outputStream().use { input.copyTo(it) } }
            // Android 14+ refuses to dex-load writable files:
            // "Writable dex file ... is not allowed" (SecurityException)
            dest.setReadOnly()
            dest
        }
        parse(copied.first(), spaceId, expectedPkg = pkg)
    }

    /** Import .apk/.xapk/.apks file into the sandbox. */
    suspend fun installFromFile(source: File, spaceId: Int): ParsedApk = withContext(Dispatchers.IO) {
        val tmpDir = File(context.cacheDir, "import_${source.nameWithoutExtension}").apply {
            deleteRecursively(); mkdirs()
        }
        val apks: List<File> = when (source.extension.lowercase()) {
            "apk" -> listOf(source)
            "xapk", "apks" -> extractApks(source, tmpDir)
            else -> throw IllegalArgumentException("file format cannot be parsed")
        }
        if (apks.isEmpty()) throw IllegalArgumentException("no apk found in package")
        val parsed = parse(apks.first(), spaceId, expectedPkg = null)
        val destDir = EnginePaths.apkDir(context, spaceId, parsed.pkg).apply { deleteRecursively(); mkdirs() }
        val copied = apks.mapIndexed { i, apk ->
            val dest = File(destDir, if (i == 0) "base.apk" else "split_$i.apk")
            apk.inputStream().use { input -> dest.outputStream().use { input.copyTo(it) } }
            dest.setReadOnly() // Android 14+ DCL restriction
            dest
        }
        tmpDir.deleteRecursively()
        parsed.copy(apkFiles = copied)
    }

    /** Extract native libs matching device ABI from apks into the sandbox lib dir. */
    suspend fun extractNativeLibs(pkg: String, spaceId: Int): File = withContext(Dispatchers.IO) {
        val libDir = EnginePaths.libDir(context, spaceId, pkg).apply { deleteRecursively(); mkdirs() }
        val abiPriority = Build.SUPPORTED_ABIS.toList()
        val apkDir = EnginePaths.apkDir(context, spaceId, pkg)
        apkDir.listFiles { f -> f.extension == "apk" }?.forEach { apk ->
            ZipFile(apk).use { zip ->
                val libEntries = zip.entries().asSequence()
                    .filter { !it.isDirectory && it.name.startsWith("lib/") && it.name.endsWith(".so") }
                    .toList()
                for (abi in abiPriority) {
                    val forAbi = libEntries.filter { it.name.startsWith("lib/$abi/") }
                    if (forAbi.isNotEmpty()) {
                        val abiDir = File(libDir, abi).apply { mkdirs() }
                        forAbi.forEach { entry ->
                            val dest = File(abiDir, entry.name.substringAfterLast('/'))
                            if (!dest.exists()) {
                                zip.getInputStream(entry).use { input ->
                                    dest.outputStream().use { input.copyTo(it) }
                                }
                                dest.setReadOnly()
                            }
                        }
                        break // best-matching ABI only
                    }
                }
            }
        }
        libDir
    }

    private fun extractApks(container: File, destDir: File): List<File> {
        val out = mutableListOf<File>()
        ZipFile(container).use { zip ->
            zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }
                .forEach { entry ->
                    val dest = File(destDir, entry.name.substringAfterLast('/'))
                    zip.getInputStream(entry).use { input ->
                        dest.outputStream().use { input.copyTo(it) }
                    }
                    out += dest
                }
        }
        // base.apk first
        return out.sortedByDescending { it.name.equals("base.apk", ignoreCase = true) }
    }

    private fun parse(baseApk: File, spaceId: Int, expectedPkg: String?): ParsedApk {
        val pm = context.packageManager
        val info: PackageInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(baseApk.absolutePath, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(baseApk.absolutePath, 0)
        }
        requireNotNull(info) { "The current file format cannot be parsed" }
        val appInfo = info.applicationInfo ?: throw IllegalArgumentException("no applicationInfo")
        appInfo.sourceDir = baseApk.absolutePath
        appInfo.publicSourceDir = baseApk.absolutePath
        val pkg = appInfo.packageName
        if (expectedPkg != null && pkg != expectedPkg) {
            throw IllegalArgumentException("package mismatch: $pkg != $expectedPkg")
        }
        val label = runCatching { appInfo.loadLabel(pm).toString() }.getOrDefault(pkg)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        val apkDir = baseApk.parentFile
        val all = apkDir?.listFiles { f -> f.extension == "apk" }?.toList() ?: listOf(baseApk)
        return ParsedApk(
            pkg = pkg,
            label = label,
            versionName = info.versionName,
            versionCode = versionCode,
            apkFiles = all,
        )
    }
}
