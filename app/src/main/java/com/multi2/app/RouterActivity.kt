package com.multi2.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Deep-link router — parity with org.waxmoon.play.product.ma.RouterActivity.
 * Handles multi2://main.router/<path> and forwards into MainActivity navigation.
 * Protected by signature permission com.multi2.app.protect.permission.
 */
class RouterActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.data?.path.orEmpty()
        startActivity(
            Intent(this, MainActivity::class.java)
                .setAction("com.multi2.app.main")
                .putExtra(EXTRA_ROUTE, path.trimStart('/'))
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }

    companion object {
        const val EXTRA_ROUTE = "com.multi2.app.extra.ROUTE"
    }
}
