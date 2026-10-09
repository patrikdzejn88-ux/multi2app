package com.multi2.app.engine

import android.content.Context
import java.io.File

/**
 * Sandbox layout (single source of truth, also used by Storage Manager):
 *   filesDir/virtual/spaces/<spaceId>/<pkg>/{apk,data,cache,media,obb,dexopt}
 *
 * Parity with the original's internal layout role (its engine keeps clone
 * apks/data in a private virtual root too).
 */
object EnginePaths {

    private fun root(context: Context): File =
        File(context.filesDir, "virtual/spaces")

    fun spaceDir(context: Context, spaceId: Int): File =
        File(root(context), spaceId.toString())

    fun appDir(context: Context, spaceId: Int, pkg: String): File =
        File(spaceDir(context, spaceId), pkg)

    fun apkDir(context: Context, spaceId: Int, pkg: String): File =
        File(appDir(context, spaceId, pkg), "apk")

    fun dataDir(context: Context, spaceId: Int, pkg: String): File =
        File(appDir(context, spaceId, pkg), "data")

    fun cacheDir(context: Context, spaceId: Int, pkg: String): File =
        File(appDir(context, spaceId, pkg), "cache")

    fun mediaDir(context: Context, spaceId: Int, pkg: String): File =
        File(appDir(context, spaceId, pkg), "media")

    fun obbDir(context: Context, spaceId: Int, pkg: String): File =
        File(appDir(context, spaceId, pkg), "obb")

    fun dexOptDir(context: Context, spaceId: Int, pkg: String): File =
        File(appDir(context, spaceId, pkg), "dexopt")

    fun libDir(context: Context, spaceId: Int, pkg: String): File =
        File(appDir(context, spaceId, pkg), "lib")

    fun prepareAppDirs(context: Context, spaceId: Int, pkg: String) {
        listOf(
            apkDir(context, spaceId, pkg),
            dataDir(context, spaceId, pkg),
            cacheDir(context, spaceId, pkg),
            mediaDir(context, spaceId, pkg),
            obbDir(context, spaceId, pkg),
            dexOptDir(context, spaceId, pkg),
            libDir(context, spaceId, pkg),
        ).forEach { it.mkdirs() }
    }

    fun removeApp(context: Context, spaceId: Int, pkg: String) {
        appDir(context, spaceId, pkg).deleteRecursively()
    }
}
