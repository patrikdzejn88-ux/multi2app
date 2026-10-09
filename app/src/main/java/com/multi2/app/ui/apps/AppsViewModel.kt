package com.multi2.app.ui.apps

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multi2.app.data.SpaceRepository
import com.multi2.app.data.db.SpaceEntity
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
import javax.inject.Inject

/** Max spaces selectable without VIP ("Only premium VIP users can clone more than two spaces"). */
const val FREE_MAX_CLONE_SPACES = 2

@HiltViewModel
class AppsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val spaceRepository: SpaceRepository,
) : ViewModel() {

    private val installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val query = MutableStateFlow("")
    val selected = MutableStateFlow<Set<String>>(emptySet())
    val loading = MutableStateFlow(true)
    val spaces = MutableStateFlow<List<SpaceEntity>>(emptyList())

    /** Filtered list — derived from query without re-running the loader. */
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

    /** Persist clone requests for the chosen spaces; engine installs in M2. */
    fun cloneSelected(spaceIds: List<Int>, onDone: (Int) -> Unit) {
        val chosen = selected.value
        if (chosen.isEmpty() || spaceIds.isEmpty()) return
        viewModelScope.launch {
            val labels = installedApps.value.filter { it.pkg in chosen }.associate { it.pkg to it.label }
            chosen.forEach { pkg ->
                spaceRepository.enqueueClone(pkg, labels[pkg] ?: pkg, spaceIds)
            }
            val count = chosen.size * spaceIds.size
            clearSelection()
            onDone(count)
        }
    }
}
