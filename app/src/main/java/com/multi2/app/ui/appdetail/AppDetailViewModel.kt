package com.multi2.app.ui.appdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multi2.app.data.SpaceRepository
import com.multi2.app.data.db.ClonedAppEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val spaceRepository: SpaceRepository,
) : ViewModel() {

    private val pkg: String = checkNotNull(savedStateHandle["pkg"])
    private val spaceId: Int = checkNotNull(savedStateHandle["userId"])

    val app: StateFlow<ClonedAppEntity?> = spaceRepository.app(pkg, spaceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    fun uninstall(onDone: () -> Unit) {
        viewModelScope.launch {
            spaceRepository.removeApp(pkg, spaceId)
            onDone()
        }
    }
}
