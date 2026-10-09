package com.multi2.app.ui.memory

import android.app.ActivityManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class RunningClone(
    val pid: Int,
    val processName: String,     // e.g. com.multi2.app:proxy.p3
    val slot: Int,               // pN index
    val pssKb: Int,              // last known PSS, 0 until engine reports
)

data class MemoryInfoUi(
    val availMb: Long,
    val totalMb: Long,
    val lowMemory: Boolean,
)

@HiltViewModel
class MemoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val running = MutableStateFlow<List<RunningClone>>(emptyList())
    val memInfo = MutableStateFlow(MemoryInfoUi(0, 0, false))

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val (clones, info) = withContext(Dispatchers.Default) {
                val am = context.getSystemService(ActivityManager::class.java)
                val prefix = "${context.packageName}:proxy.p"
                val processes = am?.runningAppProcesses.orEmpty()
                    .filter { it.processName.startsWith(prefix) }
                    .map { proc ->
                        RunningClone(
                            pid = proc.pid,
                            processName = proc.processName,
                            slot = proc.processName.removePrefix(prefix).toIntOrNull() ?: -1,
                            pssKb = 0, // M2: per-clone PSS via engine/debug meminfo
                        )
                    }
                val mi = ActivityManager.MemoryInfo()
                am?.getMemoryInfo(mi)
                processes to MemoryInfoUi(
                    availMb = mi.availMem / 1_048_576,
                    totalMb = mi.totalMem / 1_048_576,
                    lowMemory = mi.lowMemory,
                )
            }
            running.value = clones
            memInfo.value = info
        }
    }

    /** "Boost speed" — kill all clone (proxy) processes. */
    fun boost(onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val killed = running.value.size
            running.value.forEach { android.os.Process.killProcess(it.pid) }
            refresh()
            onDone(killed)
        }
    }
}
