package com.multi2.app

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.util.Log
import com.multi2.app.engine.EngineCore
import com.multi2.app.engine.HiddenApiBypass
import com.multi2.app.engine.task.EngineTaskManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Multi2 application entry point.
 *
 * Mirrors org.waxmoon.play.market.MoonApplication bootstrap order:
 *  1. attachBaseContext: hidden-API bypass (their libma2023 native role)
 *  2. onCreate: process-role detection, engine executor wiring
 */
@HiltAndroidApp
class Multi2Application : Application() {

    @Inject lateinit var taskManager: EngineTaskManager
    @Inject lateinit var engineCore: EngineCore

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        HiddenApiBypass.apply()
    }

    override fun onCreate() {
        super.onCreate()
        val process = currentProcessName()
        Log.i("Multi2App", "onCreate in process: $process")
        // engine task executor is only needed where tasks are managed (main/admin)
        taskManager.executor = engineCore
    }

    private fun currentProcessName(): String {
        val pid = android.os.Process.myPid()
        val am = getSystemService(ActivityManager::class.java) ?: return "unknown"
        return am.runningAppProcesses.orEmpty()
            .firstOrNull { it.pid == pid }?.processName ?: packageName
    }
}
