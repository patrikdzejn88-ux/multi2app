package com.multi2.app.ui.appdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multi2.app.data.SpaceRepository
import com.multi2.app.data.db.ClonedAppEntity
import com.multi2.app.engine.launch.CloneLauncher
import com.multi2.app.engine.task.EngineTask
import com.multi2.app.engine.task.EngineTaskManager
import com.multi2.app.engine.task.EngineTaskType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val spaceRepository: SpaceRepository,
    private val cloneLauncher: CloneLauncher,
    private val taskManager: EngineTaskManager,
) : ViewModel() {

    private val pkg: String = checkNotNull(savedStateHandle["pkg"])
    private val spaceId: Int = checkNotNull(savedStateHandle["userId"])

    val app: StateFlow<ClonedAppEntity?> = spaceRepository.app(pkg, spaceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** -1 = not running; otherwise proxy slot. */
    val runningSlot = MutableStateFlow(-1)

    init {
        viewModelScope.launch {
            app.collect { runningSlot.value = it?.slot ?: -1 }
        }
    }

    private fun update(transform: (ClonedAppEntity) -> ClonedAppEntity) {
        viewModelScope.launch {
            app.value?.let { spaceRepository.updateApp(transform(it)) }
        }
    }

    fun setPinned(value: Boolean) = update { it.copy(pinned = value) }
    fun setHidden(value: Boolean) = update { it.copy(hidden = value) }
    fun setTraceable(value: Boolean) = update { it.copy(traceable = value) }
    fun setGoogleInstaller(value: Boolean) = update { it.copy(googleInstaller = value) }
    fun setVirtualSdcard(value: Boolean) = update { it.copy(virtualSdcard = value) }
    fun setHideDevMode(value: Boolean) = update { it.copy(hideDevMode = value) }
    fun setDenyReadHostApps(value: Boolean) = update { it.copy(denyReadHostApps = value) }
    fun setLaunchMode(mode: Int) = update { it.copy(launchMode = mode) }
    fun setFakeLabel(label: String?) = update { it.copy(fakeLabel = label) }

    // ---- engine actions ----

    fun open(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val slot = cloneLauncher.launch(pkg, spaceId)
            onResult(slot >= 0)
        }
    }

    fun stop() {
        viewModelScope.launch {
            taskManager.submit(
                EngineTask(System.nanoTime(), EngineTaskType.FORCE_STOP, pkg, spaceId),
            )
        }
    }

    fun fix() {
        viewModelScope.launch {
            taskManager.submit(
                EngineTask(System.nanoTime(), EngineTaskType.REPAIR, pkg, spaceId),
            )
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            taskManager.submit(
                EngineTask(System.nanoTime(), EngineTaskType.CLEAR_CACHE, pkg, spaceId),
            )
        }
    }

    fun generateDeviceId() {
        viewModelScope.launch {
            taskManager.submit(
                EngineTask(System.nanoTime(), EngineTaskType.GENERATE_ID, pkg, spaceId),
            )
        }
    }

    fun uninstall(onDone: () -> Unit) {
        viewModelScope.launch {
            taskManager.submit(
                EngineTask(System.nanoTime(), EngineTaskType.UNINSTALL, pkg, spaceId),
            )
            onDone()
        }
    }
}
