package com.nguyendevs.ecolens.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nguyendevs.ecolens.ui.navigation.BottomNavigationBar
import com.nguyendevs.ecolens.ui.navigation.Screen
import com.nguyendevs.ecolens.ui.screens.ChatScreen
import com.nguyendevs.ecolens.ui.screens.HistoryDetailScreen
import com.nguyendevs.ecolens.ui.screens.HistoryScreen
import com.nguyendevs.ecolens.ui.screens.HomeScreen
import com.nguyendevs.ecolens.ui.screens.MyGardenScreen
import com.nguyendevs.ecolens.ui.screens.SettingsScreen

@Composable
fun EcoLensApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in listOf(Screen.Home.route, Screen.History.route, Screen.MyGarden.route, Screen.Settings.route)) {
                BottomNavigationBar(navController)
            }
        }
    ) { innerPadding ->
        NavHostContainer(navController = navController, modifier = Modifier.padding(innerPadding))
    }
}

@Composable
fun NavHostContainer(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        composable(Screen.History.route) {
            HistoryScreen(navController)
        }
        composable(Screen.MyGarden.route) {
            MyGardenScreen(navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController)
        }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId")
            val actualSessionId = if (sessionId == -1L) null else sessionId
            ChatScreen(navController, actualSessionId)
        }
        composable(
            route = Screen.HistoryDetail.route,
            arguments = listOf(navArgument("historyId") { type = NavType.IntType })
        ) { backStackEntry ->
            val historyId = backStackEntry.arguments?.getInt("historyId")
            HistoryDetailScreen(navController, historyId ?: 0)
        }
    }
}