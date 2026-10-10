package com.multi2.app.proxy

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.multi2.app.R
import com.multi2.app.engine.guest.GuestActivityLauncher
import com.multi2.app.engine.launch.CloneLauncher

/**
 * Assist/stub activity — parity with org.waxmoon.play.proxy.AssistActivity.
 * Runs in :proxy.pN: boots the guest (GuestRuntime), attaches the guest's
 * launcher Activity and transplants its DecorView into this stub's frame.
 * Lifecycle (resume/pause/destroy) is forwarded to the guest.
 */
open class AssistActivity : Activity() {

    private var guestActivity: Activity? = null

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
        val loading = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (resources.displayMetrics.density * 32).toInt()
            setPadding(pad, pad, pad, pad)
            addView(spinner)
            addView(status)
        }
        status.text = getString(R.string.assist_reserve_loading, pkg)
        val frame = FrameLayout(this)
        frame.addView(loading)
        setContentView(frame)

        Thread {
            val boot = ProxyProcessRuntime.boot(applicationContext, pkg, spaceId, slot)
            runOnUiThread {
                boot.onSuccess { guest ->
                    val launched = GuestActivityLauncher.launch(this, guest, pkg, spaceId)
                    launched.onSuccess { instance ->
                        guestActivity = instance.activity
                        val decor = instance.decorView
                        if (decor != null) {
                            frame.removeAllViews()
                            (decor.parent as? ViewGroup)?.removeView(decor)
                            frame.addView(
                                decor,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                ),
                            )
                        } else {
                            status.text = getString(R.string.memory_space_running_title, pkg)
                            spinner.visibility = android.view.View.GONE
                        }
                    }.onFailure { err ->
                        spinner.visibility = android.view.View.GONE
                        status.text = getString(R.string.launch_fail_detail, err.message ?: "?")
                    }
                }.onFailure { err ->
                    spinner.visibility = android.view.View.GONE
                    status.text = getString(R.string.boot_fail_detail, err.message ?: "?")
                }
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        guestActivity?.let { GuestActivityLauncher.forwardLifecycle(it, "resume") }
    }

    override fun onPause() {
        guestActivity?.let { GuestActivityLauncher.forwardLifecycle(it, "pause") }
        super.onPause()
    }

    override fun onDestroy() {
        guestActivity?.let { GuestActivityLauncher.forwardLifecycle(it, "destroy") }
        super.onDestroy()
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
