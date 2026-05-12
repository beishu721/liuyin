package com.liuyin.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : BottomNavItem("home", "首页", Icons.Default.Home)
    data object Playlist : BottomNavItem("playlist", "播放列表", Icons.AutoMirrored.Filled.List)
    data object Settings : BottomNavItem("settings", "设置", Icons.Default.Settings)

    companion object {
        val items = listOf(Home, Playlist, Settings)
    }
}
