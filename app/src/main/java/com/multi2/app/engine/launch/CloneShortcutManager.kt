package com.multi2.app.engine.launch

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import com.multi2.app.CompatShortcutPrepareActivity
import com.multi2.app.R

/**
 * Clone desktop shortcuts — parity with the original's Shortcut flow.
 * Pins a launcher shortcut that enters through CompatShortcutPrepareActivity
 * (action com.android.virtual.launcher) and cold-starts the clone.
 */
object CloneShortcutManager {

    fun supportsShortcuts(context: Context): Boolean {
        val sm = context.getSystemService(ShortcutManager::class.java) ?: return false
        return sm.isRequestPinShortcutSupported
    }

    fun pin(context: Context, pkg: String, spaceId: Int, label: String, icon: Bitmap?): Boolean {
        val sm = context.getSystemService(ShortcutManager::class.java) ?: return false
        if (!sm.isRequestPinShortcutSupported) return false

        val launchIntent = Intent(context, CompatShortcutPrepareActivity::class.java).apply {
            action = "com.android.virtual.launcher"
            putExtra(CloneLauncher.EXTRA_PKG, pkg)
            putExtra(CloneLauncher.EXTRA_SPACE, spaceId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val shortcut = ShortcutInfo.Builder(context, "clone_${spaceId}_$pkg")
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(
                if (icon != null) Icon.createWithBitmap(icon)
                else Icon.createWithResource(context, R.drawable.ic_launcher),
            )
            .setIntent(launchIntent)
            .build()

        return sm.requestPinShortcut(shortcut, null)
    }
}
