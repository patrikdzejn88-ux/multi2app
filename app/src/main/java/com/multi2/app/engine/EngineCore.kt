package com.multi2.app.engine

import android.content.Context
import com.multi2.app.data.SpaceRepository
import com.multi2.app.data.db.ClonedAppEntity
import com.multi2.app.engine.install.ApkInstaller
import com.multi2.app.engine.task.EngineTask
import com.multi2.app.engine.task.EngineTaskExecutor
import com.multi2.app.engine.task.EngineTaskState
import com.multi2.app.engine.task.EngineTaskType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine core — executes EngineTasks against the sandbox.
 * Wired into EngineTaskManager from Multi2Application (EngineBootstrap).
 */
@Singleton
class EngineCore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apkInstaller: ApkInstaller,
    private val spaceRepository: SpaceRepository,
) : EngineTaskExecutor {

    override suspend fun execute(task: EngineTask, report: (EngineTaskState) -> Unit) {
        when (task.type) {
            EngineTaskType.INSTALL -> install(task, report)
            EngineTaskType.UNINSTALL -> uninstall(task, report)
            EngineTaskType.REPAIR -> repair(task, report)
            EngineTaskType.CLEAR_CACHE -> clearCache(task, report)
            EngineTaskType.CLEAR_DATA -> clearData(task, report)
            EngineTaskType.FORCE_STOP -> forceStop(task, report)
            EngineTaskType.GENERATE_ID -> generateId(task, report)
        }
    }

    private suspend fun install(task: EngineTask, report: (EngineTaskState) -> Unit) {
        report(EngineTaskState.COPYING)
        val parsed = if (task.sourceApkPath != null) {
            apkInstaller.installFromFile(File(task.sourceApkPath), task.spaceId)
        } else {
            apkInstaller.installFromHost(task.pkg, task.spaceId)
        }
        report(EngineTaskState.ANALYZING)
        EnginePaths.prepareAppDirs(context, task.spaceId, parsed.pkg)
        apkInstaller.extractNativeLibs(parsed.pkg, task.spaceId)
        report(EngineTaskState.INSTALLING)
        spaceRepository.enqueueClone(
            pkg = parsed.pkg,
            label = parsed.label,
            spaceIds = listOf(task.spaceId),
        )
        // mark installed
        val existing = spaceRepository.getAppOnce(parsed.pkg, task.spaceId)
        if (existing != null) {
            spaceRepository.updateApp(existing.copy(installState = ClonedAppEntity.INSTALL_STATE_INSTALLED))
        }
        report(EngineTaskState.INSTALL_OK)
    }

    private suspend fun uninstall(task: EngineTask, report: (EngineTaskState) -> Unit) {
        report(EngineTaskState.UNINSTALLING)
        stopProcessOf(task)
        EnginePaths.removeApp(context, task.spaceId, task.pkg)
        spaceRepository.removeApp(task.pkg, task.spaceId)
        report(EngineTaskState.UNINSTALL_OK)
    }

    private suspend fun repair(task: EngineTask, report: (EngineTaskState) -> Unit) {
        report(EngineTaskState.REPAIRING)
        stopProcessOf(task)
        apkInstaller.installFromHost(task.pkg, task.spaceId)
        apkInstaller.extractNativeLibs(task.pkg, task.spaceId)
        report(EngineTaskState.REPAIR_OK)
    }

    private suspend fun clearCache(task: EngineTask, report: (EngineTaskState) -> Unit) {
        report(EngineTaskState.CLEARING_CACHE)
        EnginePaths.cacheDir(context, task.spaceId, task.pkg).deleteRecursively()
        EnginePaths.cacheDir(context, task.spaceId, task.pkg).mkdirs()
        report(EngineTaskState.CLEAR_CACHE_OK)
    }

    private suspend fun clearData(task: EngineTask, report: (EngineTaskState) -> Unit) {
        report(EngineTaskState.CLEARING_DATA)
        stopProcessOf(task)
        EnginePaths.dataDir(context, task.spaceId, task.pkg).deleteRecursively()
        EnginePaths.cacheDir(context, task.spaceId, task.pkg).deleteRecursively()
        EnginePaths.prepareAppDirs(context, task.spaceId, task.pkg)
        report(EngineTaskState.CLEAR_DATA_OK)
    }

    private suspend fun forceStop(task: EngineTask, report: (EngineTaskState) -> Unit) {
        report(EngineTaskState.FORCE_STOPPING)
        stopProcessOf(task)
        report(EngineTaskState.FORCE_STOP_OK)
    }

    private suspend fun generateId(task: EngineTask, report: (EngineTaskState) -> Unit) {
        report(EngineTaskState.GENERATING)
        val idFile = File(EnginePaths.dataDir(context, task.spaceId, task.pkg), "virtual_id")
        idFile.parentFile?.mkdirs()
        idFile.writeText(UUID.randomUUID().toString())
        report(EngineTaskState.GENERATE_OK)
    }

    /** Kill the proxy process hosting this clone (slot recorded in DB). */
    private suspend fun stopProcessOf(task: EngineTask) {
        val app = spaceRepository.getAppOnce(task.pkg, task.spaceId) ?: return
        if (app.slot < 0) return
        val am = context.getSystemService(android.app.ActivityManager::class.java)
        val prefix = "${context.packageName}:proxy.p${app.slot}"
        am?.runningAppProcesses.orEmpty()
            .filter { it.processName == prefix }
            .forEach { android.os.Process.killProcess(it.pid) }
        spaceRepository.updateApp(app.copy(slot = -1))
    }
}
