package com.multi2.app.ui.apps

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multi2.app.data.SpaceRepository
import com.multi2.app.data.db.SpaceEntity
import com.multi2.app.engine.task.EngineTask
import com.multi2.app.engine.task.EngineTaskManager
import com.multi2.app.engine.task.EngineTaskType
import com.multi2.app.ui.util.InstalledApp
import com.multi2.app.ui.util.InstalledAppsLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** Personal build: no VIP gating — clone into any number of spaces. */

@HiltViewModel
class AppsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val spaceRepository: SpaceRepository,
    private val taskManager: EngineTaskManager,
) : ViewModel() {

    private val installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val query = MutableStateFlow("")
    val selected = MutableStateFlow<Set<String>>(emptySet())
    val loading = MutableStateFlow(true)
    val spaces = MutableStateFlow<List<SpaceEntity>>(emptyList())

    val apps: StateFlow<List<InstalledApp>> =
        combine(installedApps, query) { list, q ->
            if (q.isBlank()) list else list.filter {
                it.label.contains(q, ignoreCase = true) || it.pkg.contains(q, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.Default) { InstalledAppsLoader.loadInstalledApps(context) }
            installedApps.value = loaded
            loading.value = false
        }
        viewModelScope.launch {
            spaceRepository.spaces.collect { spaces.value = it }
        }
    }

    fun setQuery(value: String) { query.value = value }

    fun toggleSelect(pkg: String) {
        selected.value = selected.value.let { if (pkg in it) it - pkg else it + pkg }
    }

    fun clearSelection() { selected.value = emptySet() }

    /** Submit real INSTALL engine tasks (copy apk → analyze → install into sandbox). */
    fun cloneSelected(spaceIds: List<Int>, onDone: (Int) -> Unit) {
        val chosen = selected.value
        if (chosen.isEmpty() || spaceIds.isEmpty()) return
        val labels = installedApps.value.filter { it.pkg in chosen }.associate { it.pkg to it.label }
        var count = 0
        chosen.forEach { pkg ->
            spaceIds.forEach { spaceId ->
                taskManager.submit(
                    EngineTask(
                        id = System.nanoTime(),
                        type = EngineTaskType.INSTALL,
                        pkg = pkg,
                        spaceId = spaceId,
                        label = labels[pkg] ?: pkg,
                    ),
                )
                count++
            }
        }
        clearSelection()
        onDone(count)
    }

    /** Import .apk/.xapk/.apks picked via SAF into the given space. */
    fun importFromFile(uri: Uri, spaceId: Int) {
        viewModelScope.launch {
            val local = withContext(Dispatchers.IO) {
                val name = queryDisplayName(uri) ?: "import.apk"
                val dest = File(context.cacheDir, "imports/$name").apply {
                    parentFile?.mkdirs()
                }
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        dest.outputStream().use { input?.copyTo(it) }
                    }
                }.getOrNull()?.let { dest }
            } ?: return@launch
            taskManager.submit(
                EngineTask(
                    id = System.nanoTime(),
                    type = EngineTaskType.INSTALL,
                    pkg = "import",
                    spaceId = spaceId,
                    label = local.name,
                    sourceApkPath = local.absolutePath,
                ),
            )
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val projection = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }
}
