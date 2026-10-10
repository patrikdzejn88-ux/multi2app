package com.multi2.app.engine.launch

import android.content.Context
import android.content.Intent
import com.multi2.app.data.SpaceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Allocates proxy process slots (:proxy.p0..p9 scaffold) and starts the
 * stub activity hosting the clone in that process.
 *
 * Original: 500 slots (p0..p499). Scaffold ships 10; expansion = manifest loop.
 */
@Singleton
class CloneLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val spaceRepository: SpaceRepository,
) {
    companion object {
        const val MAX_SLOTS = 500 // p0..p499, parity with the original
        const val EXTRA_PKG = "com.multi2.extra.PKG"
        const val EXTRA_SPACE = "com.multi2.extra.SPACE_ID"
        const val EXTRA_SLOT = "com.multi2.extra.SLOT"
    }

    /** Returns slot index on success, -1 when no slot is free. */
    suspend fun launch(pkg: String, spaceId: Int): Int {
        val running = spaceRepository.getRunningApps()
        val used = running.map { it.slot }.toSet()
        val slot = (0 until MAX_SLOTS).firstOrNull { it !in used } ?: return -1
        spaceRepository.assignSlot(pkg, spaceId, slot)

        // p0..p9 are nested classes (AssistActivity$Pn); p10..p499 generated top-level
        val stubName = if (slot < 10) {
            "com.multi2.app.proxy.AssistActivity\$P$slot"
        } else {
            "com.multi2.app.proxy.StubActivityP$slot"
        }
        val stubClass = Class.forName(stubName)
        val intent = Intent(context, stubClass).apply {
            putExtra(EXTRA_PKG, pkg)
            putExtra(EXTRA_SPACE, spaceId)
            putExtra(EXTRA_SLOT, slot)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        context.startActivity(intent)
        return slot
    }

    suspend fun stop(pkg: String, spaceId: Int) {
        val app = spaceRepository.getAppOnce(pkg, spaceId) ?: return
        if (app.slot < 0) return
        val am = context.getSystemService(android.app.ActivityManager::class.java)
        val processName = "${context.packageName}:proxy.p${app.slot}"
        am?.runningAppProcesses.orEmpty()
            .filter { it.processName == processName }
            .forEach { android.os.Process.killProcess(it.pid) }
        spaceRepository.releaseSlot(pkg, spaceId)
    }
}
