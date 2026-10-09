package com.multi2.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multi2.app.data.SettingsRepository
import com.multi2.app.data.SpaceRepository
import com.multi2.app.data.db.ClonedAppEntity
import dagger.hilt.android.lifecycle.HiltViewModel
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
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val spaceRepository: SpaceRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val showHidden = settingsRepository.settings
        .map { it.showHiddenApps }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val spaces: StateFlow<List<SpaceUiModel>> =
        combine(spaceRepository.spaces, showHidden) { list, showHidden -> list to showHidden }
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

    fun deleteSpace(spaceId: Int) {
        viewModelScope.launch { spaceRepository.deleteSpaceById(spaceId) }
    }
}
