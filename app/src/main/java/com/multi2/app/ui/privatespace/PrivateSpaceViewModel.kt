package com.multi2.app.ui.privatespace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multi2.app.data.LockRepository
import com.multi2.app.data.SpaceRepository
import com.multi2.app.data.db.ClonedAppEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PrivateSpaceUiState {
    LOADING,
    NEED_SETUP,     // no pattern set → education + set a lock
    LOCKED,         // pattern set, not unlocked this session
    UNLOCKED,       // show contents
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PrivateSpaceViewModel @Inject constructor(
    private val lockRepository: LockRepository,
    private val spaceRepository: SpaceRepository,
) : ViewModel() {

    val uiState = MutableStateFlow(PrivateSpaceUiState.LOADING)
    val privateSpaceId = MutableStateFlow(-1)
    val apps = MutableStateFlow<List<ClonedAppEntity>>(emptyList())
    val error = MutableStateFlow<String?>(null)

    // setup flow: record → confirm
    private var pendingPattern: String? = null
    val confirming = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            lockRepository.hasLock
                .flatMapLatest { hasLock ->
                    if (!hasLock) {
                        kotlinx.coroutines.flow.flowOf(PrivateSpaceUiState.NEED_SETUP)
                    } else {
                        lockRepository.sessionUnlocked.map { unlocked ->
                            if (unlocked) PrivateSpaceUiState.UNLOCKED else PrivateSpaceUiState.LOCKED
                        }
                    }
                }
                .collect { uiState.value = it }
        }
        viewModelScope.launch {
            privateSpaceId.value = spaceRepository.ensurePrivateSpace()
            refreshApps()
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            val id = privateSpaceId.value
            if (id > 0) {
                spaceRepository.appsOfSpace(id).collect { apps.value = it }
            }
        }
    }

    /** Pattern drawn during setup: first records, second confirms. */
    fun onSetupPattern(pattern: String) {
        if (pattern.length < 4) {
            error.value = "too_short"
            return
        }
        error.value = null
        val pending = pendingPattern
        if (pending == null) {
            pendingPattern = pattern
            confirming.value = true
        } else if (pending == pattern) {
            viewModelScope.launch {
                lockRepository.setPattern(pattern)
                pendingPattern = null
                confirming.value = false
                lockRepository.verify(pattern) // unlock immediately
            }
        } else {
            pendingPattern = null
            confirming.value = false
            error.value = "mismatch"
        }
    }

    fun onUnlockPattern(pattern: String) {
        viewModelScope.launch {
            val ok = lockRepository.verify(pattern)
            if (!ok) error.value = "wrong" else error.value = null
        }
    }

    fun cancelSetup() {
        pendingPattern = null
        confirming.value = false
        error.value = null
    }
}
