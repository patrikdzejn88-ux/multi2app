package com.multi2.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multi2.app.R
import com.multi2.app.ui.nav.Routes
import com.multi2.app.ui.util.rememberAppIcon
import kotlinx.coroutines.launch

/**
 * Home ("launcher") — virtual space cards + navigation drawer.
 * Parity with ui/page/router/launcher: space cards with cloned apps,
 * card menu (Import Apps / Edit Title / delete), drawer groups
 * Account / General / Tool (+ NEW Proxy entry).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenApps: () -> Unit,
    onOpenRoute: (String) -> Unit,
    onOpenApp: (String, Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val spaces by viewModel.spaces.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var renamingSpace by remember { mutableStateOf<SpaceUiModel?>(null) }
    var deletingSpace by remember { mutableStateOf<SpaceUiModel?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                DrawerGroupLabel(stringResource(R.string.drawer_header_account))
                DrawerItem(R.string.drawer_vip_buy_txt) { onOpenRoute(Routes.BUY_VIP) }
                DrawerItem(R.string.drawer_vip_free_txt) { onOpenRoute(Routes.FREE_VIP) }
                DrawerItem(R.string.drawer_redeem_code) { onOpenRoute(Routes.CODE_REDEEM) }
                DrawerItem(R.string.drawer_user_migration) { onOpenRoute(Routes.VIP_REBIND) }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                DrawerGroupLabel(stringResource(R.string.drawer_header_general))
                DrawerItem(R.string.drawer_settings) { onOpenRoute(Routes.SETTINGS) }
                DrawerItem(R.string.drawer_switch_language) { onOpenRoute(Routes.LANGUAGE) }
                DrawerItem(R.string.drawer_touch_vibrator) { onOpenRoute(Routes.SETTINGS) }
                DrawerItem(R.string.drawer_display_hidden_apps) { onOpenRoute(Routes.SETTINGS) }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                DrawerGroupLabel(stringResource(R.string.drawer_header_tool))
                DrawerItem(R.string.drawer_proxy) { onOpenRoute(Routes.PROXY) }
                DrawerItem(R.string.drawer_memory_manager) { onOpenRoute(Routes.MEMORY) }
                DrawerItem(R.string.drawer_storage_manager) { onOpenRoute(Routes.STORAGE) }
                DrawerItem(R.string.drawer_file_trans) { onOpenRoute(Routes.FILE_TRANSFER) }
                DrawerItem(R.string.drawer_private_space) { onOpenRoute(Routes.PRIVATE_SPACE) }
                DrawerItem(R.string.drawer_find_help) { /* M5: help chat */ }
                DrawerItem(R.string.drawer_share_app) { /* M5: share */ }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = null)
                        }
                    },
                )
            },
            floatingActionButton = {
                IconButton(onClick = { viewModel.createSpace() }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(spaces, key = { it.id }) { space ->
                    SpaceCard(
                        space = space,
                        onOpenApp = onOpenApp,
                        onImport = onOpenApps,
                        onRename = { renamingSpace = space },
                        onDelete = { deletingSpace = space },
                    )
                }
            }
        }
    }

    renamingSpace?.let { space ->
        var title by remember(space.id) { mutableStateOf(space.title) }
        AlertDialog(
            onDismissRequest = { renamingSpace = null },
            title = { Text(stringResource(R.string.space_card_menu_rename_space)) },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text(stringResource(R.string.space_card_menu_rename_holder)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) viewModel.renameSpace(space.id, title.trim())
                    renamingSpace = null
                }) { Text(stringResource(R.string.btn_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { renamingSpace = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }

    deletingSpace?.let { space ->
        AlertDialog(
            onDismissRequest = { deletingSpace = null },
            title = { Text(stringResource(R.string.app_detail_uninstall)) },
            text = { Text(stringResource(R.string.dialog_message_uninstall)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSpace(space.id)
                    deletingSpace = null
                }) { Text(stringResource(R.string.btn_sure)) }
            },
            dismissButton = {
                TextButton(onClick = { deletingSpace = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }
}

@Composable
private fun DrawerGroupLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun DrawerItem(labelRes: Int, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(stringResource(labelRes)) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

@Composable
private fun SpaceCard(
    space: SpaceUiModel,
    onOpenApp: (String, Int) -> Unit,
    onImport: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(space.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.space_apps_count, space.appCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.space_card_menu_install)) },
                        onClick = {
                            menuOpen = false
                            onImport()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.space_card_menu_rename_space)) },
                        onClick = {
                            menuOpen = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.app_detail_uninstall)) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
            if (space.apps.isEmpty()) {
                Text(
                    stringResource(R.string.state_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                Spacer(Modifier.height(8.dp))
                space.apps.forEach { app ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenApp(app.pkg, space.id) }
                            .padding(vertical = 6.dp),
                    ) {
                        val icon = rememberAppIcon(app.pkg)
                        if (icon != null) {
                            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(32.dp))
                        } else {
                            Spacer(Modifier.size(32.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            app.fakeLabel ?: app.label,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
