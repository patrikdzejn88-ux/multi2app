package com.multi2.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.multi2.app.ui.about.AboutScreen
import com.multi2.app.ui.appdetail.AppDetailScreen
import com.multi2.app.ui.apps.AppsScreen
import com.multi2.app.ui.home.HomeScreen
import com.multi2.app.ui.language.LanguageScreen
import com.multi2.app.ui.memory.MemoryScreen
import com.multi2.app.ui.proxy.ProxyScreen
import com.multi2.app.ui.screens.PlaceholderScreen
import com.multi2.app.ui.settings.SettingsScreen
import com.multi2.app.ui.storage.StorageScreen
import com.multi2.app.ui.vip.BuyVipScreen
import com.multi2.app.ui.vip.CodeRedeemScreen
import com.multi2.app.ui.vip.FreeVipScreen
import com.multi2.app.ui.vip.VipRebindScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.LAUNCHER) {
        composable(Routes.LAUNCHER) {
            HomeScreen(
                onOpenApps = { navController.navigate(Routes.APPS) },
                onOpenRoute = { route -> navController.navigate(route) },
                onOpenApp = { pkg, spaceId -> navController.navigate(Routes.appDetail(pkg, spaceId)) },
            )
        }
        composable(Routes.APPS) { AppsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.PROXY) { ProxyScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.ABOUT) { AboutScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.LANGUAGE) { LanguageScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.MEMORY) { MemoryScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.STORAGE) { StorageScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.BUY_VIP) { BuyVipScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.FREE_VIP) { FreeVipScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.CODE_REDEEM) { CodeRedeemScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.VIP_REBIND) { VipRebindScreen(onBack = { navController.popBackStack() }) }
        composable(
            route = Routes.APP_DETAIL,
            arguments = listOf(
                navArgument("pkg") { type = NavType.StringType },
                navArgument("userId") { type = NavType.IntType },
            ),
        ) {
            AppDetailScreen(onBack = { navController.popBackStack() })
        }
        // M3/M5 remainder
        composable(Routes.CHOICE) { PlaceholderScreen("Choose apps") }
        composable(Routes.SPACE_FOLDER) { PlaceholderScreen("Space Folder") }
        composable(Routes.FILE_TRANSFER) { PlaceholderScreen("File Transfer") }
        composable(Routes.VIRTUAL_DEVICE_INFO) { PlaceholderScreen("Device Info") }
        composable(Routes.LOCK) { PlaceholderScreen("Set a lock") }
        composable(Routes.PRIVATE_SPACE) { PlaceholderScreen("Private Space") }
    }
}
