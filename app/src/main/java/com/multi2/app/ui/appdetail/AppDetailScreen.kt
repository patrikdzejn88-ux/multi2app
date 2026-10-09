package com.multi2.app.ui.appdetail

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multi2.app.R
import com.multi2.app.engine.AppDataExporter
import com.multi2.app.engine.launch.CloneShortcutManager
import com.multi2.app.ui.util.rememberAppIcon
import kotlinx.coroutines.launch

/**
 * App Detail — parity with ui/page/router/appDetail.
 * Engine-bound actions (Open/Stop/Fix/Shortcut/Export/Sync/Device ID) are
 * stubbed to the engine request flow until M2; local options persist to Room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    onBack: () -> Unit,
    onOpenDeviceInfo: (String, Int) -> Unit,
    viewModel: AppDetailViewModel = hiltViewModel(),
) {
    val app by viewModel.app.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showFakeDialog by remember { mutableStateOf(false) }
    var showUninstallDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        val entity = app
        if (uri != null && entity != null) {
            scope.launch {
                val result = AppDataExporter.export(context, entity.spaceId, entity.pkg, uri)
                Toast.makeText(
                    context,
                    if (result.isSuccess) R.string.appzip_save_success else R.string.appzip_save_failed,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        val entity = app
        if (entity == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text(stringResource(R.string.state_empty))
            }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            val icon = rememberAppIcon(entity.pkg)
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(56.dp))
                } else {
                    Spacer(Modifier.size(56.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(entity.fakeLabel ?: entity.label, style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.app_detail_pkg, entity.pkg),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Primary actions (engine)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    viewModel.open { ok ->
                        Toast.makeText(
                            context,
                            if (ok) R.string.app_detail_open else R.string.engine_request_task_install_fail,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }) { Text(stringResource(R.string.app_detail_open), maxLines = 1) }
                OutlinedButton(onClick = { viewModel.stop() }) {
                    Text(stringResource(R.string.app_stop), maxLines = 1)
                }
                OutlinedButton(onClick = { viewModel.fix() }) {
                    Text(stringResource(R.string.app_detail_fix), maxLines = 1)
                }
                OutlinedButton(onClick = {
                    val ok = CloneShortcutManager.pin(
                        context, entity.pkg, entity.spaceId,
                        entity.fakeLabel ?: entity.label, icon?.asBitmap(),
                    )
                    Toast.makeText(
                        context,
                        if (ok) R.string.shortcut_hint_title else R.string.appzip_save_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }) { Text(stringResource(R.string.app_detail_fix_shortcut), maxLines = 1) }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    exportLauncher.launch("${entity.pkg}.zip")
                }) { Text(stringResource(R.string.app_detail_export_data), maxLines = 1) }
                OutlinedButton(onClick = { onOpenDeviceInfo(entity.pkg, entity.spaceId) }) {
                    Text(stringResource(R.string.app_detail_virtual_id), maxLines = 1)
                }
                OutlinedButton(onClick = { viewModel.clearCache() }) {
                    Text(stringResource(R.string.storage_pkg_clear_cache), maxLines = 1)
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // Local options (persisted)
            OptionSwitch(
                title = stringResource(R.string.space_app_item_menu_pin),
                checked = entity.pinned,
                onChange = { viewModel.setPinned(it) },
            )
            OptionSwitch(
                title = stringResource(R.string.app_detail_hidden_app),
                checked = entity.hidden,
                onChange = { viewModel.setHidden(it) },
            )
            OptionSwitch(
                title = stringResource(R.string.app_detail_traceable),
                checked = entity.traceable,
                onChange = { viewModel.setTraceable(it) },
            )
            OptionSwitch(
                title = stringResource(R.string.app_detail_google_installer),
                checked = entity.googleInstaller,
                onChange = { viewModel.setGoogleInstaller(it) },
            )
            OptionSwitch(
                title = stringResource(R.string.hook_virtual_sdcard),
                checked = entity.virtualSdcard,
                onChange = { viewModel.setVirtualSdcard(it) },
            )
            OptionSwitch(
                title = stringResource(R.string.hook_hide_dev_mode),
                checked = entity.hideDevMode,
                onChange = { viewModel.setHideDevMode(it) },
            )
            OptionSwitch(
                title = stringResource(R.string.hook_deny_read_host_apps),
                checked = entity.denyReadHostApps,
                onChange = { viewModel.setDenyReadHostApps(it) },
            )

            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { showFakeDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.app_detail_app_fake))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showUninstallDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.app_detail_uninstall),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (showFakeDialog) {
        var label by remember { mutableStateOf(app?.fakeLabel ?: "") }
        AlertDialog(
            onDismissRequest = { showFakeDialog = false },
            title = { Text(stringResource(R.string.app_detail_app_fake)) },
            text = {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.label_def)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setFakeLabel(label.ifBlank { null })
                    showFakeDialog = false
                }) { Text(stringResource(R.string.btn_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showFakeDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }

    if (showUninstallDialog) {
        AlertDialog(
            onDismissRequest = { showUninstallDialog = false },
            title = { Text(stringResource(R.string.app_detail_uninstall)) },
            text = { Text(stringResource(R.string.dialog_message_uninstall)) },
            confirmButton = {
                TextButton(onClick = {
                    showUninstallDialog = false
                    viewModel.uninstall {
                        Toast.makeText(context, R.string.engine_request_task_uninstall_complete, Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                }) { Text(stringResource(R.string.btn_sure)) }
            },
            dismissButton = {
                TextButton(onClick = { showUninstallDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }
}

@Composable
private fun OptionSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
