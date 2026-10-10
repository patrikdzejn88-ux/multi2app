package com.multi2.app.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multi2.app.data.SettingsRepository
import com.multi2.app.data.SpaceRepository
import com.multi2.app.data.db.ClonedAppEntity
import com.multi2.app.engine.GoogleSuite
import com.multi2.app.engine.launch.CloneShortcutManager
import com.multi2.app.engine.task.EngineTask
import com.multi2.app.engine.task.EngineTaskManager
import com.multi2.app.engine.task.EngineTaskType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SpaceUiModel(
    val id: Int,
    val title: String,
    val appCount: Int,
    val apps: List<ClonedAppEntity>,
    val googleSuiteInstalled: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val spaceRepository: SpaceRepository,
    private val settingsRepository: SettingsRepository,
    private val cloneLauncher: com.multi2.app.engine.launch.CloneLauncher,
    private val taskManager: EngineTaskManager,
) : ViewModel() {

    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val setupDone = settingsRepository.setupDone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun markSetupDone() {
        viewModelScope.launch { settingsRepository.setSetupDone(true) }
    }

    private val showHidden = settingsRepository.settings
        .map { it.showHiddenApps }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val spaces: StateFlow<List<SpaceUiModel>> =
        combine(spaceRepository.publicSpaces, showHidden) { list, showHidden -> list to showHidden }
            .flatMapLatest { (list, show) ->
                if (list.isEmpty()) {
                    kotlinx.coroutines.flow.flowOf(emptyList())
                } else {
                    combine(
                        list.map { space ->
                            spaceRepository.appsOfSpace(space.id).map { apps ->
                                SpaceUiModel(
                                    id = space.id,
                                    title = space.title,
                                    appCount = apps.size,
                                    apps = if (show) apps else apps.filter { !it.hidden },
                                    googleSuiteInstalled = GoogleSuite.isInstalledInSpace(apps),
                                )
                            }
                        },
                    ) { it.toList() }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { spaceRepository.ensureDefaultSpace() }
    }

    fun createSpace() {
        viewModelScope.launch {
            val next = (spaces.value.maxOfOrNull { it.id } ?: 0) + 1
            spaceRepository.createSpace("Virtual Space-$next", next)
        }
    }

    fun renameSpace(spaceId: Int, newTitle: String) {
        viewModelScope.launch { spaceRepository.renameSpace(spaceId, newTitle) }
    }

    fun acceptPrivacy() {
        viewModelScope.launch { settingsRepository.setPrivacyAccepted() }
    }

    fun deleteSpace(spaceId: Int) {
        viewModelScope.launch { spaceRepository.deleteSpaceById(spaceId) }
    }

    /** "Kill All Apps" — parity: confirm dialog, then stop every running clone. */
    fun killAllApps(onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val running = spaceRepository.getRunningApps()
            running.forEach { cloneLauncher.stop(it.pkg, it.spaceId) }
            onDone(running.size)
        }
    }

    /** Tap on a clone = launch it directly (original parity). */
    fun launchApp(pkg: String, spaceId: Int, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val error = runCatching {
                val slot = cloneLauncher.launch(pkg, spaceId)
                if (slot < 0) "no free slot" else null
            }.getOrElse { it.message ?: "launch failed" }
            onResult(error)
        }
    }

    fun stopApp(pkg: String, spaceId: Int) {
        viewModelScope.launch { cloneLauncher.stop(pkg, spaceId) }
    }

    fun togglePin(app: ClonedAppEntity) {
        viewModelScope.launch { spaceRepository.updateApp(app.copy(pinned = !app.pinned)) }
    }

    fun toggleHidden(app: ClonedAppEntity) {
        viewModelScope.launch { spaceRepository.updateApp(app.copy(hidden = !app.hidden)) }
    }

    fun uninstallApp(pkg: String, spaceId: Int) {
        viewModelScope.launch {
            taskManager.submit(
                EngineTask(
                    id = System.nanoTime(),
                    type = EngineTaskType.UNINSTALL,
                    pkg = pkg,
                    spaceId = spaceId,
                    label = pkg,
                ),
            )
        }
    }

    fun addShortcut(pkg: String, spaceId: Int, label: String, onResult: (Boolean) -> Unit) {
        val ok = CloneShortcutManager.pin(context, pkg, spaceId, label, null)
        onResult(ok)
    }

    /** Install Google Suite (Play Store + GMS + GSF) into a space. */
    fun installGoogleSuite(spaceId: Int, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val available = GoogleSuite.installedOnHost(context)
            available.forEach { pkg ->
                taskManager.submit(
                    EngineTask(
                        id = System.nanoTime(),
                        type = EngineTaskType.INSTALL,
                        pkg = pkg,
                        spaceId = spaceId,
                        label = pkg,
                    ),
                )
            }
            onResult(available.size)
        }
    }

    /** Remove Google Suite packages from a space. */
    fun removeGoogleSuite(spaceId: Int) {
        viewModelScope.launch {
            GoogleSuite.PACKAGES.forEach { pkg ->
                taskManager.submit(
                    EngineTask(
                        id = System.nanoTime(),
                        type = EngineTaskType.UNINSTALL,
                        pkg = pkg,
                        spaceId = spaceId,
                        label = pkg,
                    ),
                )
            }
        }
    }
}
