package com.multi2.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.multi2.app.engine.launch.CloneLauncher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Entry point for clone desktop shortcuts (com.android.virtual.launcher).
 * Parity with CompatShortcutPrepareActivity: resolves extras and launches
 * the clone via CloneLauncher, then closes.
 */
@AndroidEntryPoint
class CompatShortcutPrepareActivity : ComponentActivity() {

    @Inject lateinit var cloneLauncher: CloneLauncher

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(CloneLauncher.EXTRA_PKG)
        val spaceId = intent.getIntExtra(CloneLauncher.EXTRA_SPACE, -1)
        if (pkg == null || spaceId < 0) {
            finish()
            return
        }
        scope.launch {
            cloneLauncher.launch(pkg, spaceId)
            finish()
        }
    }
}
