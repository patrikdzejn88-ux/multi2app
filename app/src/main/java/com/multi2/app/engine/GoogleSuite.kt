package com.multi2.app.engine

import android.content.Context
import android.content.pm.PackageManager

/**
 * Google Suite per space — parity with the original's
 * "Install Google Suite" space-card menu. Installs the host's Google
 * packages (Play Store, GMS, GSF) into the virtual space as regular
 * clones so guests can use Google accounts / Play services.
 */
object GoogleSuite {

    val PACKAGES = listOf(
        "com.google.android.gsf",   // Google Services Framework
        "com.google.android.gms",   // Google Play services
        "com.android.vending",      // Play Store
    )

    fun installedOnHost(context: Context): List<String> {
        val pm = context.packageManager
        return PACKAGES.filter { pkg ->
            runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess
        }
    }

    fun isAvailable(context: Context): Boolean {
        val pm = context.packageManager
        return runCatching {
            pm.getPackageInfo("com.google.android.gms", PackageManager.GET_ACTIVITIES)
        }.isSuccess
    }

    /** Suite is considered installed in a space when GMS is present there. */
    fun isInstalledInSpace(spaceApps: List<com.multi2.app.data.db.ClonedAppEntity>): Boolean =
        spaceApps.any { it.pkg == "com.google.android.gms" }
}
