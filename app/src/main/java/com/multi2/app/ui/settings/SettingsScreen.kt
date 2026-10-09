package com.multi2.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multi2.app.BuildConfig
import com.multi2.app.R
import com.multi2.app.data.NightModeSetting

/**
 * Settings — parity with ui/page/router/setting + appearance group.
 * Battery optimization, keep-alive, night mode, vibrator, hidden apps,
 * marquee border, hidden developer options (tap version 3x).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showNightModeDialog by remember { mutableStateOf(false) }
    var versionTapCount by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Battery optimization (real system integration)
            val pm = context.getSystemService(PowerManager::class.java)
            val ignoring = remember {
                pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
            }
            SettingsRow(
                title = stringResource(R.string.settings_battery_optimize),
                subtitle = stringResource(R.string.settings_battery_optimize_description),
                trailing = {
                    Switch(checked = ignoring, onCheckedChange = {
                        if (!ignoring) {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}"),
                                ),
                            )
                        }
                    })
                },
            )
            SettingsRow(
                title = stringResource(R.string.settings_keep_alive),
                subtitle = stringResource(R.string.settings_keep_alive_description),
                trailing = {
                    Switch(checked = settings.keepAlive, onCheckedChange = { viewModel.setKeepAlive(it) })
                },
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SettingsRow(
                title = stringResource(R.string.appearance_night_mode),
                subtitle = when (settings.nightMode) {
                    NightModeSetting.AUTO -> stringResource(R.string.appearance_night_mode_auto_description)
                    NightModeSetting.DARK -> stringResource(R.string.appearance_night_mode_dark_description)
                    NightModeSetting.LIGHT -> stringResource(R.string.appearance_night_mode_light_description)
                },
                onClick = { showNightModeDialog = true },
            )
            SettingsRow(
                title = stringResource(R.string.appearance_marquee_border),
                subtitle = stringResource(R.string.appearance_marquee_border_description),
                trailing = {
                    Switch(checked = settings.marqueeBorder, onCheckedChange = { viewModel.setMarqueeBorder(it) })
                },
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SettingsRow(
                title = stringResource(R.string.drawer_touch_vibrator),
                trailing = {
                    Switch(checked = settings.touchVibrator, onCheckedChange = { viewModel.setTouchVibrator(it) })
                },
            )
            SettingsRow(
                title = stringResource(R.string.drawer_display_hidden_apps),
                trailing = {
                    Switch(checked = settings.showHiddenApps, onCheckedChange = { viewModel.setShowHiddenApps(it) })
                },
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // Developer options (hidden: 3 taps on version, parity with original)
            SettingsRow(
                title = stringResource(R.string.settings_developer_test),
                subtitle = if (settings.developerMode) "Activated" else stringResource(R.string.settings_developer_test_description),
                trailing = {
                    Switch(checked = settings.developerMode, onCheckedChange = { viewModel.setDeveloperMode(it) })
                },
            )
            SettingsRow(
                title = stringResource(R.string.about_version),
                subtitle = BuildConfig.VERSION_NAME,
                onClick = {
                    versionTapCount++
                    if (versionTapCount >= 3) {
                        versionTapCount = 0
                        viewModel.setDeveloperMode(true)
                    }
                },
            )
        }
    }

    if (showNightModeDialog) {
        AlertDialog(
            onDismissRequest = { showNightModeDialog = false },
            title = { Text(stringResource(R.string.appearance_night_mode)) },
            text = {
                Column {
                    NightModeSetting.entries.forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setNightMode(mode)
                                    showNightModeDialog = false
                                }
                                .padding(vertical = 4.dp),
                        ) {
                            RadioButton(
                                selected = settings.nightMode == mode,
                                onClick = {
                                    viewModel.setNightMode(mode)
                                    showNightModeDialog = false
                                },
                            )
                            Text(
                                when (mode) {
                                    NightModeSetting.AUTO -> stringResource(R.string.appearance_night_mode_auto)
                                    NightModeSetting.DARK -> stringResource(R.string.appearance_night_mode_dark)
                                    NightModeSetting.LIGHT -> stringResource(R.string.appearance_night_mode_light)
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNightModeDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
        trailing?.invoke()
    }
}
