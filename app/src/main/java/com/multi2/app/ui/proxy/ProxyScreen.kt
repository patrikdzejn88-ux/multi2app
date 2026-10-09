package com.multi2.app.ui.proxy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multi2.app.R
import com.multi2.app.data.db.ProxyEntity

/**
 * Proxy manager — NEW feature (not in the original).
 * SOCKS5/HTTP(S) proxies, global default or per-space assignment,
 * TCP connectivity test. Engine applies proxies to clone traffic in M2.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyScreen(
    onBack: () -> Unit,
    viewModel: ProxyViewModel = hiltViewModel(),
) {
    val proxies by viewModel.proxies.collectAsStateWithLifecycle()
    val spaces by viewModel.spaces.collectAsStateWithLifecycle()
    val testResult by viewModel.testResult.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<ProxyEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.proxy_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                showEditor = true
            }) { Icon(Icons.Default.Add, contentDescription = null) }
        },
    ) { padding ->
        if (proxies.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.proxy_empty))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(proxies, key = { it.id }) { proxy ->
                    ProxyCard(
                        proxy = proxy,
                        spaceTitle = spaces.firstOrNull { it.id == proxy.spaceId }?.title,
                        testResult = testResult?.takeIf { it.proxyId == proxy.id },
                        onToggle = { viewModel.toggleEnabled(proxy) },
                        onEdit = {
                            editing = proxy
                            showEditor = true
                        },
                        onDelete = { viewModel.delete(proxy) },
                        onTest = { viewModel.test(proxy) },
                    )
                }
            }
        }
    }

    if (showEditor) {
        ProxyEditorDialog(
            initial = editing,
            spaces = spaces.map { it.id to it.title },
            onDismiss = { showEditor = false },
            onSave = {
                viewModel.save(it)
                showEditor = false
            },
        )
    }
}

@Composable
private fun ProxyCard(
    proxy: ProxyEntity,
    spaceTitle: String?,
    testResult: ProxyTestResult?,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit,
) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(proxy.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${proxy.protocol} · ${proxy.host}:${proxy.port}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        spaceTitle?.let { stringResource(R.string.proxy_scope_space, it) }
                            ?: stringResource(R.string.proxy_scope_global),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Switch(checked = proxy.enabled, onCheckedChange = { onToggle() })
            }
            if (testResult != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    testResult.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (testResult.ok) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onTest) { Text(stringResource(R.string.proxy_test)) }
                TextButton(onClick = onEdit) { Text(stringResource(R.string.proxy_edit)) }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProxyEditorDialog(
    initial: ProxyEntity?,
    spaces: List<Pair<Int, String>>,
    onDismiss: () -> Unit,
    onSave: (ProxyEntity) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var protocol by remember { mutableStateOf(initial?.protocol ?: ProxyEntity.PROTOCOL_SOCKS5) }
    var host by remember { mutableStateOf(initial?.host ?: "") }
    var port by remember { mutableStateOf(initial?.port?.toString() ?: "") }
    var username by remember { mutableStateOf(initial?.username ?: "") }
    var password by remember { mutableStateOf(initial?.password ?: "") }
    var spaceId by remember { mutableStateOf(initial?.spaceId) }
    var protocolExpanded by remember { mutableStateOf(false) }
    var spaceExpanded by remember { mutableStateOf(false) }

    val valid = name.isNotBlank() && host.isNotBlank() && port.toIntOrNull() in 1..65535

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (initial == null) R.string.proxy_add else R.string.proxy_edit))
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.proxy_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = protocolExpanded,
                    onExpandedChange = { protocolExpanded = it },
                ) {
                    OutlinedTextField(
                        value = protocol,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.proxy_protocol)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(protocolExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = protocolExpanded,
                        onDismissRequest = { protocolExpanded = false },
                    ) {
                        ProxyEntity.PROTOCOLS.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p) },
                                onClick = {
                                    protocol = p
                                    protocolExpanded = false
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text(stringResource(R.string.proxy_host)) },
                        singleLine = true,
                        modifier = Modifier.weight(2f),
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it.filter(Char::isDigit).take(5) },
                        label = { Text(stringResource(R.string.proxy_port)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.proxy_username)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.proxy_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = spaceExpanded,
                    onExpandedChange = { spaceExpanded = it },
                ) {
                    OutlinedTextField(
                        value = spaceId?.let { id -> spaces.firstOrNull { it.first == id }?.second }
                            ?: stringResource(R.string.proxy_scope_global),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.proxy_scope)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(spaceExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = spaceExpanded,
                        onDismissRequest = { spaceExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.proxy_scope_global)) },
                            onClick = {
                                spaceId = null
                                spaceExpanded = false
                            },
                        )
                        spaces.forEach { (id, title) ->
                            DropdownMenuItem(
                                text = { Text(title) },
                                onClick = {
                                    spaceId = id
                                    spaceExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        ProxyEntity(
                            id = initial?.id ?: 0,
                            name = name.trim(),
                            protocol = protocol,
                            host = host.trim(),
                            port = port.toInt(),
                            username = username.ifBlank { null },
                            password = password.ifBlank { null },
                            enabled = initial?.enabled ?: true,
                            spaceId = spaceId,
                        ),
                    )
                },
            ) { Text(stringResource(R.string.btn_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        },
    )
}
