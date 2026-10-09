package com.multi2.app.ui.apps

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multi2.app.R
import com.multi2.app.ui.util.InstalledApp
import com.multi2.app.ui.util.rememberAppIcon

/**
 * Import Apps — installed-app picker (parity with ui/page/router/apps + choice).
 * Select apps → choose target spaces → enqueue clones (engine installs in M2).
 * Import from file (.apk/.xapk/.apks) lands with the engine in M2.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    onBack: () -> Unit,
    viewModel: AppsViewModel = hiltViewModel(),
) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val spaces by viewModel.spaces.collectAsStateWithLifecycle()
    var showCloneDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.apps_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    val importLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.OpenDocument(),
                    ) { uri ->
                        if (uri != null) {
                            val spaceId = spaces.firstOrNull()?.id ?: 1
                            viewModel.importFromFile(uri, spaceId)
                        }
                    }
                    TextButton(onClick = {
                        importLauncher.launch(arrayOf("*/*"))
                    }) {
                        Text(stringResource(R.string.apps_menu_install_from_file))
                    }
                },
            )
        },
        bottomBar = {
            if (selected.isNotEmpty()) {
                Button(
                    onClick = { showCloneDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) { Text("Clone (${selected.size})") }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text(stringResource(R.string.search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                apps.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.state_empty))
                }
                else -> LazyColumn {
                    items(apps, key = { it.pkg }) { app ->
                        AppRow(
                            app = app,
                            checked = app.pkg in selected,
                            onToggle = { viewModel.toggleSelect(app.pkg) },
                        )
                    }
                }
            }
        }
    }

    if (showCloneDialog) {
        CloneSpaceDialog(
            spaceTitles = spaces.map { it.id to it.title },
            onDismiss = { showCloneDialog = false },
            onConfirm = { spaceIds ->
                showCloneDialog = false
                viewModel.cloneSelected(spaceIds) { count ->
                    Toast.makeText(context, "Queued $count clone(s)", Toast.LENGTH_SHORT).show()
                    onBack()
                }
            },
        )
    }
}

@Composable
private fun AppRow(app: InstalledApp, checked: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        val icon = rememberAppIcon(app.pkg)
        if (icon != null) {
            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(40.dp))
        } else {
            Spacer(Modifier.size(40.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(
                app.pkg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
            )
        }
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun CloneSpaceDialog(
    spaceTitles: List<Pair<Int, String>>,
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit,
) {
    var chosen by remember { mutableStateOf(setOf<Int>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.apps_clone_dialog_title)) },
        text = {
            Column {
                spaceTitles.forEach { (id, title) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { chosen = if (id in chosen) chosen - id else chosen + id }
                            .padding(vertical = 4.dp),
                    ) {
                        Checkbox(
                            checked = id in chosen,
                            onCheckedChange = { chosen = if (id in chosen) chosen - id else chosen + id },
                        )
                        Text(title)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(chosen.toList()) },
                enabled = chosen.isNotEmpty(),
            ) { Text(stringResource(R.string.btn_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        },
    )
}
