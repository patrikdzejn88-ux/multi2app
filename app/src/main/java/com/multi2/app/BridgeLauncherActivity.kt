package com.multi2.app

import android.app.Activity
import android.os.Bundle

/**
 * Clone launch bridge (:admin process) — parity with BridgeLauncherActivity.
 * Receives com.android.virtual.bridge intents and forwards launches into
 * the virtualization engine. Implemented in M2.
 */
class BridgeLauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // M2: resolve target package/user, ask engine to launch, finish
        finish()
    }
}
