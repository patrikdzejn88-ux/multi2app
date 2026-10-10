package com.multi2.app.ui.privatespace

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multi2.app.R
import com.multi2.app.ui.lock.PatternLockView
import com.multi2.app.ui.util.rememberAppIcon

/**
 * Private Space — parity with ui/page/privateSpace + lock/password:
 *  - NEED_SETUP: education screen + pattern record/confirm
 *  - LOCKED: unlock with pattern
 *  - UNLOCKED: private space contents (cloned apps)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateSpaceScreen(
    onBack: () -> Unit,
    onOpenApp: (String, Int) -> Unit,
    viewModel: PrivateSpaceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val confirming by viewModel.confirming.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val spaceId by viewModel.privateSpaceId.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.drawer_private_space)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        when (uiState) {
            PrivateSpaceUiState.LOADING -> Column(Modifier.padding(padding)) {}

            PrivateSpaceUiState.NEED_SETUP -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.private_space_education_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.private_space_hide_apps_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(
                        if (confirming) R.string.lockpattern_confirm_header
                        else R.string.lockpattern_choose_header,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.lockpattern_setting_step_touch),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(16.dp))
                PatternLockView(
                    onPatternComplete = viewModel::onSetupPattern,
                    modifier = Modifier.fillMaxWidth(0.7f),
                )
                PatternError(error)
                if (confirming) {
                    TextButton(onClick = { viewModel.cancelSetup() }) {
                        Text(stringResource(R.string.lockpattern_retry_button_text))
                    }
                }
            }

            PrivateSpaceUiState.LOCKED -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(R.string.lockpattern_confirm_generic),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(24.dp))
                PatternLockView(
                    onPatternComplete = viewModel::onUnlockPattern,
                    modifier = Modifier.fillMaxWidth(0.7f),
                )
                PatternError(error)
            }

            PrivateSpaceUiState.UNLOCKED -> Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                if (apps.isEmpty()) {
                    Text(
                        stringResource(R.string.state_empty),
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(apps, key = { it.pkg }) { app ->
                            Card(
                                Modifier.fillMaxWidth().clickable { onOpenApp(app.pkg, spaceId) },
                            ) {
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
                                    Text(app.fakeLabel ?: app.label, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatternError(error: String?) {
    if (error == null) return
    Spacer(Modifier.height(12.dp))
    Text(
        when (error) {
            "too_short" -> stringResource(R.string.lockpattern_too_short)
            "wrong" -> stringResource(R.string.lockpattern_wrong)
            else -> stringResource(R.string.lockpattern_need_to_confirm)
        },
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
}
