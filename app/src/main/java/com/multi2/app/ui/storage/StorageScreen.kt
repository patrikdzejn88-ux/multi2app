package com.multi2.app.ui.storage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multi2.app.R

/** Storage Manager — parity with ui/page/router/storage. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    onBack: () -> Unit,
    viewModel: StorageViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var confirmUninstall by remember { mutableStateOf<AppStorageUi?>(null) }
    var confirmClearData by remember { mutableStateOf<AppStorageUi?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.storage_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.setSort(
                            if (sort == StorageSort.BY_SIZE) StorageSort.BY_SPACE else StorageSort.BY_SIZE,
                        )
                    }) {
                        Text(
                            stringResource(
                                if (sort == StorageSort.BY_SIZE) R.string.storage_sort_by_space
                                else R.string.storage_sort_by_size,
                            ),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            busy -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            items.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.storage_empty_tip))
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { "${it.spaceId}:${it.pkg}" }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(item.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "VP-${item.spaceId} · " + stringResource(R.string.storage_pkg_sumary, formatBytes(item.totalBytes)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                "${stringResource(R.string.storage_pkg_install_dir)}: ${formatBytes(item.installBytes)} · " +
                                    "${stringResource(R.string.storage_pkg_data_dir)}: ${formatBytes(item.dataBytes)} · " +
                                    "${stringResource(R.string.storage_pkg_cache_dir)}: ${formatBytes(item.cacheBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                TextButton(onClick = { viewModel.clearCache(item) }) {
                                    Text(stringResource(R.string.storage_pkg_clear_cache))
                                }
                                TextButton(onClick = { confirmClearData = item }) {
                                    Text(stringResource(R.string.storage_pkg_clear_data))
                                }
                                TextButton(onClick = { confirmUninstall = item }) {
                                    Text(
                                        stringResource(R.string.storage_pkg_uninstall),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    confirmClearData?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmClearData = null },
            title = { Text(stringResource(R.string.storage_pkg_clear_data)) },
            text = { Text(stringResource(R.string.storage_clear_data_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearData(item)
                    confirmClearData = null
                }) { Text(stringResource(R.string.btn_sure)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearData = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }

    confirmUninstall?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmUninstall = null },
            title = { Text(stringResource(R.string.storage_pkg_uninstall)) },
            text = { Text(stringResource(R.string.storage_uninstall_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.uninstall(item)
                    confirmUninstall = null
                }) { Text(stringResource(R.string.btn_sure)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmUninstall = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }
}
