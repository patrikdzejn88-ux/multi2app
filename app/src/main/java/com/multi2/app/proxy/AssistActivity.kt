package com.multi2.app.proxy

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.multi2.app.R
import com.multi2.app.engine.launch.CloneLauncher

/**
 * Assist/stub activity — parity with org.waxmoon.play.proxy.AssistActivity.
 * Runs in :proxy.pN; boots the guest code via ProxyProcessRuntime and shows
 * a minimal status view until guest Activity attach lands (next iteration).
 */
open class AssistActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pkg = intent.getStringExtra(CloneLauncher.EXTRA_PKG)
        val spaceId = intent.getIntExtra(CloneLauncher.EXTRA_SPACE, -1)
        val slot = intent.getIntExtra(CloneLauncher.EXTRA_SLOT, -1)

        if (pkg == null || spaceId < 0) {
            finish()
            return
        }

        val status = TextView(this)
        val spinner = ProgressBar(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (resources.displayMetrics.density * 32).toInt()
            setPadding(pad, pad, pad, pad)
            addView(spinner)
            addView(status)
        }
        status.text = getString(R.string.assist_reserve_loading, pkg)
        setContentView(layout)

        Thread {
            val result = ProxyProcessRuntime.boot(applicationContext, pkg, spaceId, slot)
            runOnUiThread {
                result.onSuccess {
                    spinner.visibility = android.view.View.GONE
                    status.text = getString(R.string.memory_space_running_title, pkg)
                }.onFailure {
                    spinner.visibility = android.view.View.GONE
                    status.text = getString(R.string.engine_request_task_install_fail)
                }
            }
        }.start()
    }

    class P0 : AssistActivity()
    class P1 : AssistActivity()
    class P2 : AssistActivity()
    class P3 : AssistActivity()
    class P4 : AssistActivity()
    class P5 : AssistActivity()
    class P6 : AssistActivity()
    class P7 : AssistActivity()
    class P8 : AssistActivity()
    class P9 : AssistActivity()
}
