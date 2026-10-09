package com.multi2.app.admin

import android.app.Service
import android.content.Intent
import android.os.IBinder

/** Compat admin service (:admin) — parity with CompatAdminService. Filled in M2. */
class CompatAdminService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
