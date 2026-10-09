package com.multi2.app.ui.deviceinfo

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multi2.app.engine.EnginePaths
import com.multi2.app.engine.task.EngineTask
import com.multi2.app.engine.task.EngineTaskManager
import com.multi2.app.engine.task.EngineTaskType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VirtualDeviceInfoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val taskManager: EngineTaskManager,
) : ViewModel() {

    private val pkg: String = savedStateHandle.get<String>("pkg").orEmpty()
    private val spaceId: Int = savedStateHandle.get<Int>("userId") ?: 1

    val virtualId = MutableStateFlow<String?>(null)
    val resetting = MutableStateFlow(false)

    init { load() }

    fun load() {
        viewModelScope.launch {
            virtualId.value = withContext(Dispatchers.IO) { readVirtualId() }
        }
    }

    private fun readVirtualId(): String? {
        val file = File(EnginePaths.dataDir(context, spaceId, pkg), "virtual_id")
        return if (file.exists()) file.readText().trim() else null
    }

    /** "Reset virtual ID" — parity: warn about data loss, then GENERATE_ID task. */
    fun reset() {
        viewModelScope.launch {
            resetting.value = true
            taskManager.submit(
                EngineTask(System.nanoTime(), EngineTaskType.GENERATE_ID, pkg, spaceId),
            )
            // task queue is serial; poll the file briefly for the new value
            withContext(Dispatchers.IO) {
                repeat(20) {
                    kotlinx.coroutines.delay(150)
                    if (!resetting.value) return@withContext
                }
            }
            load()
            resetting.value = false
        }
    }
}
