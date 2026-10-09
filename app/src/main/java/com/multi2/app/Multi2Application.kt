package com.multi2.app

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

/**
 * Multi2 application entry point.
 *
 * Mirrors org.waxmoon.play.market.MoonApplication:
 *  - attachBaseContext: engine bootstrap hook point (M2: load libmulti2, staged engine init)
 *  - onCreate: crash reporting, process-role detection, DI warmup
 */
@HiltAndroidApp
class Multi2Application : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // M2: System.loadLibrary("multi2") + staged engine init (iq.o0Oo parity)
    }

    override fun onCreate() {
        super.onCreate()
        // M2: process role detection (main / :admin / :proxy.pN / :tool)
        // M4: Firebase Crashlytics equivalent
    }
}
