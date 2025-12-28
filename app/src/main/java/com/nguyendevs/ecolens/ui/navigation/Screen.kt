package com.nguyendevs.ecolens.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object History : Screen("history", "History", Icons.Filled.History)
    object MyGarden : Screen("my_garden", "My Garden", Icons.Filled.LocalFlorist)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
    
    object Chat : Screen("chat/{sessionId}", "Chat") {
        fun createRoute(sessionId: Long?) = "chat/${sessionId ?: -1L}"
    }
    
    object HistoryDetail : Screen("history_detail/{historyId}", "History Detail") {
        fun createRoute(historyId: Int) = "history_detail/$historyId"
    }
}