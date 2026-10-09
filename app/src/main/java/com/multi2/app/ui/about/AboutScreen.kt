package com.multi2.app.ui.about

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.multi2.app.BuildConfig
import com.multi2.app.R
import com.multi2.app.ui.settings.SettingsViewModel

/** About us — parity with ui/page/router/about (+hidden developer test dialog). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var devTapCount by remember { mutableIntStateOf(0) }
    var showDevDialog by remember { mutableStateOf(false) }
    val deviceId = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
        ) {
            AboutRow(stringResource(R.string.about_version), BuildConfig.VERSION_NAME)
            AboutRow(stringResource(R.string.about_device_id), deviceId) {
                clipboard.setText(AnnotatedString(deviceId))
                Toast.makeText(context, R.string.about_device_id, Toast.LENGTH_SHORT).show()
            }
            AboutRow(stringResource(R.string.about_developer), "WaxMoon-clone") {
                devTapCount++
                if (devTapCount >= 3) {
                    devTapCount = 0
                    showDevDialog = true
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            AboutRow(stringResource(R.string.about_contact), stringResource(R.string.about_email)) {
                context.startActivity(
                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:waxmoon2023@gmail.com")),
                )
            }
            AboutRow(stringResource(R.string.about_github), "Github") {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com")))
            }
            AboutRow(stringResource(R.string.about_telegram), "Telegram") {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me")))
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            AboutRow(stringResource(R.string.about_privacy_policy), stringResource(R.string.about_privacy_policy_content)) { }
            AboutRow(stringResource(R.string.about_open_license), stringResource(R.string.about_open_license_content)) { }
        }
    }

    if (showDevDialog) {
        var account by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDevDialog = false },
            title = { Text(stringResource(R.string.about_developer_dialog_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.about_developer_dialog_tip))
                    OutlinedTextField(
                        value = account,
                        onValueChange = { account = it },
                        placeholder = { Text(stringResource(R.string.about_developer_dialog_holder)) },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setDeveloperMode(account.isNotBlank())
                    Toast.makeText(context, R.string.about_developer_toast_enter_test, Toast.LENGTH_SHORT).show()
                    showDevDialog = false
                }) { Text(stringResource(R.string.btn_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDevDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }
}

@Composable
private fun AboutRow(title: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}
