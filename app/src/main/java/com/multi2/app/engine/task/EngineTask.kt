package com.multi2.app.engine.task

import com.multi2.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Engine task types — parity with engine_va_request_task_* set. */
enum class EngineTaskType {
    INSTALL, UNINSTALL, REPAIR,
    CLEAR_CACHE, CLEAR_DATA, FORCE_STOP, GENERATE_ID,
}

/** Task states with the original's user-facing strings. */
enum class EngineTaskState(val stringRes: Int) {
    CHECKING(R.string.engine_request_task_checking),
    QUEUED(R.string.engine_request_task_in_queue),
    COPYING(R.string.engine_request_task_copying),
    ANALYZING(R.string.engine_request_task_in_analyze),
    INSTALLING(R.string.engine_request_task_installing),
    INSTALL_OK(R.string.engine_request_task_install_complete),
    INSTALL_FAIL(R.string.engine_request_task_install_fail),
    UNINSTALLING(R.string.engine_request_task_uninstalling),
    UNINSTALL_OK(R.string.engine_request_task_uninstall_complete),
    UNINSTALL_FAIL(R.string.engine_request_task_uninstall_fail),
    REPAIRING(R.string.engine_request_task_repairing),
    REPAIR_OK(R.string.engine_request_task_repairing_complete),
    REPAIR_FAIL(R.string.engine_request_task_repairing_fail),
    CLEARING_CACHE(R.string.engine_request_task_clear_app_cache),
    CLEAR_CACHE_OK(R.string.engine_request_task_clear_app_cache_complete),
    CLEAR_CACHE_FAIL(R.string.engine_request_task_clear_app_cache_fail),
    CLEARING_DATA(R.string.engine_request_task_clear_app_data),
    CLEAR_DATA_OK(R.string.engine_request_task_clear_app_data_complete),
    CLEAR_DATA_FAIL(R.string.engine_request_task_clear_app_data_fail),
    FORCE_STOPPING(R.string.engine_request_task_force_stop),
    FORCE_STOP_OK(R.string.engine_request_task_force_stop_complete),
    FORCE_STOP_FAIL(R.string.engine_request_task_force_stop_fail),
    GENERATING(R.string.engine_request_task_generate_device),
    GENERATE_OK(R.string.engine_request_task_generate_device_complete),
    GENERATE_FAIL(R.string.engine_request_task_generate_device_fail),
    POLICY_FAIL(R.string.engine_request_task_fail_for_policy),
    UNKNOWN(R.string.engine_request_unknown),
}

data class EngineTask(
    val id: Long,
    val type: EngineTaskType,
    val pkg: String,
    val spaceId: Int,
    val label: String = pkg,
    val sourceApkPath: String? = null,   // import-from-file path; null = clone from host
    val state: EngineTaskState = EngineTaskState.QUEUED,
    val error: String? = null,
)

/**
 * Serial engine task queue. Executes one task at a time on IO,
 * exposes live state for VirtualRequestActivity / snackbars.
 */
@Singleton
class EngineTaskManager @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _current = MutableStateFlow<EngineTask?>(null)
    val current: StateFlow<EngineTask?> = _current

    private val _lastResult = MutableStateFlow<EngineTask?>(null)
    val lastResult: StateFlow<EngineTask?> = _lastResult

    var executor: EngineTaskExecutor? = null

    fun submit(task: EngineTask) {
        scope.launch {
            val exec = executor
            if (exec == null) {
                finish(task, EngineTaskState.UNKNOWN, "engine not ready")
                return@launch
            }
            _current.value = task.copy(state = EngineTaskState.CHECKING)
            try {
                exec.execute(task) { newState ->
                    _current.value = _current.value?.copy(state = newState)
                }
                val finalState = _current.value?.state ?: EngineTaskState.UNKNOWN
                finish(task, finalState, null)
            } catch (t: Throwable) {
                finish(task, failStateOf(task.type), t.message)
            }
        }
    }

    private suspend fun finish(task: EngineTask, state: EngineTaskState, error: String?) {
        _current.value = task.copy(state = state, error = error)
        _lastResult.value = _current.value
        delay(1200) // let the UI show the result before clearing
        _current.value = null
    }

    private fun failStateOf(type: EngineTaskType): EngineTaskState = when (type) {
        EngineTaskType.INSTALL -> EngineTaskState.INSTALL_FAIL
        EngineTaskType.UNINSTALL -> EngineTaskState.UNINSTALL_FAIL
        EngineTaskType.REPAIR -> EngineTaskState.REPAIR_FAIL
        EngineTaskType.CLEAR_CACHE -> EngineTaskState.CLEAR_CACHE_FAIL
        EngineTaskType.CLEAR_DATA -> EngineTaskState.CLEAR_DATA_FAIL
        EngineTaskType.FORCE_STOP -> EngineTaskState.FORCE_STOP_FAIL
        EngineTaskType.GENERATE_ID -> EngineTaskState.GENERATE_FAIL
    }
}

/** Implemented by the engine core (ApkInstaller etc.), wired in EngineBootstrap. */
interface EngineTaskExecutor {
    suspend fun execute(task: EngineTask, report: (EngineTaskState) -> Unit)
}
