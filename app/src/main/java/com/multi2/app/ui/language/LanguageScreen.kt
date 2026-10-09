package com.multi2.app.ui.language

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multi2.app.R
import com.multi2.app.ui.settings.SettingsViewModel

private val LANGUAGES = listOf(
    "system" to "Follow System",
    "en" to "English",
    "zh-CN" to "简体中文",
    "zh-TW" to "繁體中文",
    "ru" to "Русский",
    "es" to "Español",
    "pt" to "Português",
    "ar" to "العربية",
    "hi" to "हिन्दी",
    "id" to "Bahasa Indonesia",
    "vi" to "Tiếng Việt",
    "tr" to "Türkçe",
    "de" to "Deutsch",
    "fr" to "Français",
    "ja" to "日本語",
    "ko" to "한국어",
)

/** Switch Language — parity with ui/page/router/language. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.drawer_switch_language)) },
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
            LANGUAGES.forEach { (tag, name) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setLanguage(tag) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    RadioButton(
                        selected = settings.language == tag,
                        onClick = { viewModel.setLanguage(tag) },
                    )
                    Text(name)
                }
            }
        }
    }
}
