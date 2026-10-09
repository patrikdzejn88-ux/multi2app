package com.multi2.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** Night mode parity: Auto / Dark / Light */
enum class NightModeSetting { AUTO, DARK, LIGHT }

data class AppSettings(
    val nightMode: NightModeSetting = NightModeSetting.AUTO,
    val touchVibrator: Boolean = false,
    val showHiddenApps: Boolean = false,
    val keepAlive: Boolean = false,
    val marqueeBorder: Boolean = false,
    val developerMode: Boolean = false,
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val NIGHT_MODE = stringPreferencesKey("night_mode")
        val TOUCH_VIBRATOR = booleanPreferencesKey("touch_vibrator")
        val SHOW_HIDDEN = booleanPreferencesKey("show_hidden_apps")
        val KEEP_ALIVE = booleanPreferencesKey("keep_alive")
        val MARQUEE = booleanPreferencesKey("marquee_border")
        val DEV_MODE = booleanPreferencesKey("developer_mode")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { p ->
        AppSettings(
            nightMode = p[Keys.NIGHT_MODE]?.let { runCatching { NightModeSetting.valueOf(it) }.getOrNull() }
                ?: NightModeSetting.AUTO,
            touchVibrator = p[Keys.TOUCH_VIBRATOR] ?: false,
            showHiddenApps = p[Keys.SHOW_HIDDEN] ?: false,
            keepAlive = p[Keys.KEEP_ALIVE] ?: false,
            marqueeBorder = p[Keys.MARQUEE] ?: false,
            developerMode = p[Keys.DEV_MODE] ?: false,
        )
    }

    suspend fun setNightMode(mode: NightModeSetting) {
        context.settingsStore.edit { it[Keys.NIGHT_MODE] = mode.name }
    }

    suspend fun setTouchVibrator(value: Boolean) {
        context.settingsStore.edit { it[Keys.TOUCH_VIBRATOR] = value }
    }

    suspend fun setShowHiddenApps(value: Boolean) {
        context.settingsStore.edit { it[Keys.SHOW_HIDDEN] = value }
    }

    suspend fun setKeepAlive(value: Boolean) {
        context.settingsStore.edit { it[Keys.KEEP_ALIVE] = value }
    }

    suspend fun setMarqueeBorder(value: Boolean) {
        context.settingsStore.edit { it[Keys.MARQUEE] = value }
    }

    suspend fun setDeveloperMode(value: Boolean) {
        context.settingsStore.edit { it[Keys.DEV_MODE] = value }
    }
}
