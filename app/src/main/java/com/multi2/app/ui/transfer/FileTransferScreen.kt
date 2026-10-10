package com.multi2.app.ui.transfer

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multi2.app.R
import com.multi2.app.ui.util.rememberAppIcon

/**
 * File Transfer — parity with ui/page/router/fileTransfer.
 * Same-network send/receive between two devices: sender serves the app's
 * apks and shows a QR + ip:port code; receiver enters the code (camera
 * scan lands with MLKit integration later) and the payload imports
 * through the engine as a normal install.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileTransferScreen(
    onBack: () -> Unit,
    viewModel: FileTransferViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val spaces by viewModel.spaces.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var code by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.file_trans_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.phase == TransferPhase.IDLE) {
                TabRow(selectedTabIndex = tab) {
                    Tab(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        text = { Text(stringResource(R.string.file_trans_send)) },
                    )
                    Tab(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        text = { Text(stringResource(R.string.file_trans_receive)) },
                    )
                }
                when (tab) {
                    0 -> SendTab(catalog, onSend = viewModel::startSending)
                    1 -> ReceiveTab(
                        code = code,
                        onCodeChange = { code = it },
                        spaces = spaces.map { it.id to it.title },
                        onStart = {
                            viewModel.startReceiving(
                                code,
                                spaces.firstOrNull()?.id ?: 1,
                            )
                        },
                    )
                }
            } else {
                TransferProgress(state, onDone = viewModel::reset)
            }
        }
    }
}

@Composable
private fun SendTab(
    catalog: List<com.multi2.app.data.db.ClonedAppEntity>,
    onSend: (com.multi2.app.data.db.ClonedAppEntity) -> Unit,
) {
    if (catalog.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.file_trans_empty_tip))
        }
    } else {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(catalog, key = { "${it.spaceId}:${it.pkg}" }) { app ->
                Card(Modifier.fillMaxWidth().clickable { onSend(app) }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp),
                    ) {
                        val icon = rememberAppIcon(app.pkg)
                        if (icon != null) {
                            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(36.dp))
                        } else {
                            Spacer(Modifier.size(36.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(app.fakeLabel ?: app.label, maxLines = 1)
                            Text(
                                "VP-${app.spaceId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiveTab(
    code: String,
    onCodeChange: (String) -> Unit,
    spaces: List<Pair<Int, String>>,
    onStart: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.file_trans_connect_target_device),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = code,
            onValueChange = onCodeChange,
            placeholder = { Text(stringResource(R.string.file_trans_code_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = onStart, enabled = code.contains(":"), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.file_trans_start))
        }
        Text(
            stringResource(R.string.file_trans_same_network_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun TransferProgress(state: TransferUiState, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state.phase) {
            TransferPhase.WAITING -> {
                val qr = remember(state.code) {
                    state.code?.let { qrBitmap(it).asImageBitmap() }
                }
                if (qr != null) {
                    Image(bitmap = qr, contentDescription = null, modifier = Modifier.size(240.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    state.code.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.file_trans_waiting),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
            TransferPhase.TRANSFERRING -> {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(
                        if (state.code != null) R.string.file_trans_sending
                        else R.string.file_trans_receiving,
                        (state.progress * 100).toInt(),
                    ),
                    textAlign = TextAlign.Center,
                )
            }
            TransferPhase.DONE -> {
                Text(stringResource(R.string.file_trans_complete), style = MaterialTheme.typography.titleMedium)
                state.message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onDone) { Text(stringResource(R.string.btn_ok)) }
            }
            TransferPhase.ERROR -> {
                Text(
                    stringResource(R.string.file_trans_failed),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                state.message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDone) { Text(stringResource(R.string.btn_ok)) }
            }
            else -> Unit
        }
    }
}
