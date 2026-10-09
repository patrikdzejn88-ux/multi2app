package com.multi2.app.proxy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import com.multi2.app.R
import com.multi2.app.engine.guest.GuestRuntime

/**
 * Runtime living inside a :proxy.pN process. Booted by AssistActivity stubs.
 * Loads the guest (GuestRuntime) and keeps the process alive with the
 * original's "%1$s is running" foreground notification semantics.
 */
object ProxyProcessRuntime {

    private const val TAG = "ProxyRuntime"
    private const val CHANNEL_ID = "multi2.clone"

    var guest: GuestRuntime.GuestApp? = null
        private set

    fun boot(context: Context, pkg: String, spaceId: Int, slot: Int): Result<GuestRuntime.GuestApp> {
        return runCatching {
            Log.i(TAG, "boot $pkg space=$spaceId slot=$slot pid=${android.os.Process.myPid()}")
            val loaded = GuestRuntime.load(context, pkg, spaceId)
            guest = loaded
            showRunningNotification(context, pkg)
            loaded
        }.onFailure { Log.e(TAG, "boot failed: ${it.message}", it) }
    }

    private fun showRunningNotification(context: Context, pkg: String) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Cloned apps", NotificationManager.IMPORTANCE_LOW),
        )
        val label = runCatching {
            context.packageManager.getApplicationInfo(pkg, 0).loadLabel(context.packageManager)
        }.getOrDefault(pkg)
        val notification: Notification = Notification.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.memory_space_running_title, label))
            .setContentText(context.getString(R.string.notification_running_warn))
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .build()
        // foreground-start happens in AdminService; here we just post
        nm.notify(2000 + (guest?.hashCode() ?: 0) % 1000, notification)
    }
}
