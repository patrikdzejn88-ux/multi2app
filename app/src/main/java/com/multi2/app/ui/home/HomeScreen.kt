package com.multi2.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.multi2.app.R
import com.multi2.app.ui.nav.Routes
import kotlinx.coroutines.launch

/**
 * Home ("launcher") — virtual space cards + navigation drawer.
 * Parity with org.waxmoon.play.product.ma.ui.page.router.launcher.
 *
 * Drawer groups (1:1): Account (Buy VIP, Free VIP, Redeem code, VIP migration),
 * General (Settings, Switch Language, Touch vibrator, Display hidden apps),
 * Tool (Task Manager, Storage Manager, File Transfer, Private Space, Find Help, Share app).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRoute: (String) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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
                DrawerItem(R.string.drawer_touch_vibrator) { /* toggle */ }
                DrawerItem(R.string.drawer_display_hidden_apps) { /* toggle */ }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                DrawerGroupLabel(stringResource(R.string.drawer_header_tool))
                DrawerItem(R.string.drawer_memory_manager) { onOpenRoute(Routes.MEMORY) }
                DrawerItem(R.string.drawer_storage_manager) { onOpenRoute(Routes.STORAGE) }
                DrawerItem(R.string.drawer_file_trans) { onOpenRoute(Routes.FILE_TRANSFER) }
                DrawerItem(R.string.drawer_private_space) { onOpenRoute(Routes.PRIVATE_SPACE) }
                DrawerItem(R.string.drawer_find_help) { /* help chat */ }
                DrawerItem(R.string.drawer_share_app) { /* share */ }
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
        ) { padding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // M3: real space cards from Room; scaffold shows one space + add tile
                item { SpaceCard(title = stringResource(R.string.virtual_space_card_title_holder, 1)) }
                item { AddSpaceCard(onClick = onOpenApps) }
            }
        }
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
private fun SpaceCard(title: String) {
    Card(Modifier.fillMaxWidth().aspectRatio(1f)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.state_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun AddSpaceCard(onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(stringResource(R.string.apps_title), style = MaterialTheme.typography.labelMedium)
        }
    }
}
