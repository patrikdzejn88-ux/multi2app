package com.multi2.app.ui.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Installed-app metadata for the Import Apps list. */
@Stable
data class InstalledApp(
    val pkg: String,
    val label: String,
)

object InstalledAppsLoader {

    private val iconCache = LruCache<String, ImageBitmap>(128)

    fun loadInstalledApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val apps: List<ApplicationInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(0)
        }
        val self = context.packageName
        return apps
            .asSequence()
            .filter { it.packageName != self }
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { info ->
                InstalledApp(
                    pkg = info.packageName,
                    label = pm.getApplicationLabel(info).toString(),
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun loadIcon(context: Context, pkg: String): ImageBitmap {
        iconCache.get(pkg)?.let { return it }
        val drawable: Drawable = runCatching {
            context.packageManager.getApplicationIcon(pkg)
        }.getOrElse { context.packageManager.defaultActivityIcon }
        val bitmap = drawableToBitmap(drawable)
        val image = bitmap.asImageBitmap()
        iconCache.put(pkg, image)
        return image
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 192
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 192
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            drawable.draw(canvas)
        } else {
            drawable.draw(canvas)
        }
        return bitmap
    }
}

/** Async icon state for a package, cached in LruCache. */
@Composable
fun rememberAppIcon(pkg: String): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    return produceState<ImageBitmap?>(initialValue = null, pkg) {
        value = withContext(Dispatchers.Default) {
            InstalledAppsLoader.loadIcon(context, pkg)
        }
    }.value
}
