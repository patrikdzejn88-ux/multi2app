package com.multi2.app.proxy

import android.app.Activity
import android.os.Bundle

/**
 * Assist/stub activity — parity with org.waxmoon.play.proxy.AssistActivity.
 * One subclass per proxy process slot (:proxy.p0..p9 scaffold, p499 target).
 * The engine launches clones into these stubs. Wired in M2.
 */
open class AssistActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // M2: hand control to engine stub runtime
        finish()
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
