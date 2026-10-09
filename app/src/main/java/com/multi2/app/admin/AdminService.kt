package com.multi2.app.admin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/**
 * Virtualization manager service (:admin) — parity with AdminService.
 * Owns the engine lifecycle: installs, launches, stops, repairs clones.
 * M2: bind to engine core, expose AIDL over AdminProvider.
 */
class AdminService : Service() {

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Multi2 engine",
            NotificationManager.IMPORTANCE_MIN,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(com.multi2.app.R.string.app_name))
            .setContentText(getString(com.multi2.app.R.string.admin_launch_loading))
            .setSmallIcon(com.multi2.app.R.drawable.ic_launcher)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(ID, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "multi2.admin"
        private const val ID = 1001
    }
}
