package com.multi2.app.ui.deviceinfo

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multi2.app.R

/** Device Info — parity with ui/page/router/virtualDeviceInfo (/vinfo). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualDeviceInfoScreen(
    onBack: () -> Unit,
    viewModel: VirtualDeviceInfoViewModel = hiltViewModel(),
) {
    val virtualId by viewModel.virtualId.collectAsStateWithLifecycle()
    val resetting by viewModel.resetting.collectAsStateWithLifecycle()
    var confirmReset by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.virtual_device_info_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.app_detail_virtual_id),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        virtualId ?: stringResource(R.string.state_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                stringResource(R.string.virtual_id_reset_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { confirmReset = true },
                    enabled = !resetting,
                ) { Text(stringResource(R.string.virtual_device_info_reset_btn)) }
                TextButton(
                    onClick = { virtualId?.let { clipboard.setText(AnnotatedString(it)) } },
                    enabled = virtualId != null,
                ) { Text(stringResource(R.string.vinfo_copy)) }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.virtual_id_reset_title)) },
            text = { Text(stringResource(R.string.virtual_id_reset_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.reset()
                    confirmReset = false
                }) { Text(stringResource(R.string.btn_sure)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }
}
