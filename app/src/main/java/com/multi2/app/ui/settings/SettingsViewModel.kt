package com.multi2.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multi2.app.data.AppSettings
import com.multi2.app.data.NightModeSetting
import com.multi2.app.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setNightMode(mode: NightModeSetting) {
        viewModelScope.launch { settingsRepository.setNightMode(mode) }
    }

    fun setTouchVibrator(value: Boolean) {
        viewModelScope.launch { settingsRepository.setTouchVibrator(value) }
    }

    fun setShowHiddenApps(value: Boolean) {
        viewModelScope.launch { settingsRepository.setShowHiddenApps(value) }
    }

    fun setKeepAlive(value: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepAlive(value) }
    }

    fun setMarqueeBorder(value: Boolean) {
        viewModelScope.launch { settingsRepository.setMarqueeBorder(value) }
    }

    fun setDeveloperMode(value: Boolean) {
        viewModelScope.launch { settingsRepository.setDeveloperMode(value) }
    }
}
