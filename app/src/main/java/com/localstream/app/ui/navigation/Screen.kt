package com.localstream.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    object Server : Screen("server", "My Server", Icons.Default.Dns, "tab_server")
    object Files : Screen("files", "Files", Icons.Default.Folder, "tab_files")
    object Player : Screen("player", "Player", Icons.Default.PlayCircle, "tab_player")
    object Renderers : Screen("renderers", "Renderers", Icons.Default.Cast, "tab_renderers")
    object Apps : Screen("apps", "Apps", Icons.Default.Apps, "tab_apps")
    object Settings : Screen("settings", "Settings", Icons.Default.Settings, "tab_settings")
}

val BottomNavScreens: List<Screen> by lazy {
    listOf(
        Screen.Server,
        Screen.Files,
        Screen.Player,
        Screen.Renderers,
        Screen.Apps
    )
}
