package com.multi2.app.engine

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Export App Data — parity with the original's "Export App Data"
 * ("The app information has been saved to the file manager!").
 * Zips the clone's sandbox dir into a user-picked document.
 */
object AppDataExporter {

    suspend fun export(
        context: Context,
        spaceId: Int,
        pkg: String,
        target: Uri,
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val source = EnginePaths.appDir(context, spaceId, pkg)
            require(source.exists()) { "app data not found" }
            var bytes = 0L
            context.contentResolver.openOutputStream(target)?.use { raw ->
                ZipOutputStream(raw.buffered()).use { zip ->
                    source.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val entry = ZipEntry(file.relativeTo(source).path)
                            zip.putNextEntry(entry)
                            file.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                            bytes += file.length()
                        }
                    }
                }
            } ?: error("cannot open target")
            bytes
        }
    }
}
