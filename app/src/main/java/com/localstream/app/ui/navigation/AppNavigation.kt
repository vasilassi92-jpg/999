package com.localstream.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.localstream.app.ui.components.LocalStreamBottomBar
import com.localstream.app.ui.screens.apps.AppsScreen
import com.localstream.app.ui.screens.files.FilesScreen
import com.localstream.app.ui.screens.player.PlayerScreen
import com.localstream.app.ui.screens.renderers.RenderersScreen
import com.localstream.app.ui.screens.server.ServerScreen
import com.localstream.app.ui.screens.settings.SettingsScreen
import com.localstream.app.ui.theme.DarkBgBase

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Server.route

    val showBottomBar = currentRoute in BottomNavScreens.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBgBase,
        bottomBar = {
            if (showBottomBar) {
                LocalStreamBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(Screen.Server.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Server.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Server.route) {
                ServerScreen(
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToPlayer = { navController.navigate(Screen.Player.route) }
                )
            }

            composable(Screen.Files.route) {
                FilesScreen(
                    onNavigateToPlayer = { navController.navigate(Screen.Player.route) }
                )
            }

            composable(Screen.Player.route) {
                PlayerScreen(
                    onNavigateToRenderers = { navController.navigate(Screen.Renderers.route) }
                )
            }

            composable(Screen.Renderers.route) {
                RenderersScreen()
            }

            composable(Screen.Apps.route) {
                AppsScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
