package com.multi2.app.proxy

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Notification relay (:admin) — parity with ProxyNotificationService.
 * Forwards notifications posted by clones to the host shade with per-clone
 * "is running" keep-alive semantics. Filled in M3.
 */
class ProxyNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // M3: relay clone notifications
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // M3: mirror removal
    }
}
