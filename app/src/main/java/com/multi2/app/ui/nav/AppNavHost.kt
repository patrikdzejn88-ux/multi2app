package com.multi2.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.multi2.app.ui.home.HomeScreen
import com.multi2.app.ui.screens.PlaceholderScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.LAUNCHER) {
        composable(Routes.LAUNCHER) {
            HomeScreen(
                onOpenApps = { navController.navigate(Routes.APPS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenRoute = { route -> navController.navigate(route) },
            )
        }
        composable(Routes.APPS) { PlaceholderScreen("Import Apps") }
        composable(Routes.CHOICE) { PlaceholderScreen("Choose apps") }
        composable(
            route = Routes.APP_DETAIL,
            arguments = listOf(
                navArgument("pkg") { type = NavType.StringType },
                navArgument("userId") { type = NavType.IntType },
            ),
        ) { entry ->
            PlaceholderScreen(
                "App Detail\n${entry.arguments?.getString("pkg")} (user ${entry.arguments?.getInt("userId")})",
            )
        }
        composable(Routes.SETTINGS) { PlaceholderScreen("Settings") }
        composable(Routes.ABOUT) { PlaceholderScreen("About us") }
        composable(Routes.BUY_VIP) { PlaceholderScreen("Buy VIP") }
        composable(Routes.FREE_VIP) { PlaceholderScreen("Free VIP") }
        composable(Routes.CODE_REDEEM) { PlaceholderScreen("Redeem") }
        composable(Routes.VIP_REBIND) { PlaceholderScreen("VIP migration") }
        composable(Routes.LANGUAGE) { PlaceholderScreen("Switch Language") }
        composable(Routes.MEMORY) { PlaceholderScreen("Task Manager") }
        composable(Routes.STORAGE) { PlaceholderScreen("Storage Manager") }
        composable(Routes.SPACE_FOLDER) { PlaceholderScreen("Space Folder") }
        composable(Routes.FILE_TRANSFER) { PlaceholderScreen("File Transfer") }
        composable(Routes.VIRTUAL_DEVICE_INFO) { PlaceholderScreen("Device Info") }
        composable(Routes.LOCK) { PlaceholderScreen("Set a lock") }
        composable(Routes.PRIVATE_SPACE) { PlaceholderScreen("Private Space") }
    }
}
