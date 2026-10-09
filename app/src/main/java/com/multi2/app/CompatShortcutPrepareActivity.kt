package com.multi2.app

import android.app.Activity
import android.os.Bundle

/**
 * Entry point for clone desktop shortcuts (com.android.virtual.launcher).
 * Parity with CompatShortcutPrepareActivity. Implemented in M3.
 */
class CompatShortcutPrepareActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // M3: read shortcut extras (pkg, userId), route to BridgeLauncher
        finish()
    }
}
