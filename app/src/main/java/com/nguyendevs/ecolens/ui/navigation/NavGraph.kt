package com.nguyendevs.ecolens.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nguyendevs.ecolens.ui.screens.chat.ChatScreen
import com.nguyendevs.ecolens.ui.screens.history.HistoryDetailScreen
import com.nguyendevs.ecolens.view.EcoLensViewModel

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object ChatDetail : Screen("chat/{sessionId}") {
        fun createRoute(sessionId: Long?) = "chat/${sessionId ?: "new"}"
    }
    object HistoryDetail : Screen("history/{entryId}") {
        fun createRoute(entryId: Int) = "history/$entryId"
    }
}

@Composable
fun EcoLensNavHost(
    navController: NavHostController,
    viewModel: EcoLensViewModel,
    onNavigateToCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route,
        modifier = modifier
    ) {
        // Main screen with bottom navigation
        composable(Screen.Main.route) {
            com.nguyendevs.ecolens.ui.screens.main.MainScreen(
                viewModel = viewModel,
                onNavigateToCamera = onNavigateToCamera,
                onNavigateToChatDetail = { sessionId ->
                    navController.navigate(Screen.ChatDetail.createRoute(sessionId))
                },
                onNavigateToHistoryDetail = { entryId ->
                    navController.navigate(Screen.HistoryDetail.createRoute(entryId))
                }
            )
        }

        // Chat detail screen
        composable(
            route = Screen.ChatDetail.route,
            arguments = listOf(
                navArgument("sessionId") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val sessionIdStr = backStackEntry.arguments?.getString("sessionId")
            val sessionId = sessionIdStr?.toLongOrNull()

            ChatScreen(
                sessionId = sessionId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // History detail screen
        composable(
            route = Screen.HistoryDetail.route,
            arguments = listOf(
                navArgument("entryId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getInt("entryId") ?: return@composable

            HistoryDetailScreen(
                entryId = entryId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
