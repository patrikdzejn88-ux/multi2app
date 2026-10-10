package com.multi2.app.ui.deviceinfo

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multi2.app.engine.VirtualDeviceInfo
import com.multi2.app.engine.VirtualDeviceStore
import com.multi2.app.engine.task.EngineTask
import com.multi2.app.engine.task.EngineTaskManager
import com.multi2.app.engine.task.EngineTaskType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class VirtualDeviceInfoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val taskManager: EngineTaskManager,
) : ViewModel() {

    private val pkg: String = savedStateHandle.get<String>("pkg").orEmpty()
    private val spaceId: Int = savedStateHandle.get<Int>("userId") ?: 1

    val info = MutableStateFlow<VirtualDeviceInfo?>(null)
    val resetting = MutableStateFlow(false)

    init { load() }

    fun load() {
        viewModelScope.launch {
            info.value = withContext(Dispatchers.IO) {
                VirtualDeviceStore.load(context, spaceId, pkg)
            }
        }
    }

    /** "Reset virtual ID" — regenerate the full set via the engine task. */
    fun reset() {
        viewModelScope.launch {
            resetting.value = true
            taskManager.submit(
                EngineTask(System.nanoTime(), EngineTaskType.GENERATE_ID, pkg, spaceId),
            )
            withContext(Dispatchers.IO) { delay(600) }
            load()
            resetting.value = false
        }
    }
}
