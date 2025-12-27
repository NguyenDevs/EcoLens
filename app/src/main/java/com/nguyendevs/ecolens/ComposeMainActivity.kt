package com.nguyendevs.ecolens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.nguyendevs.ecolens.managers.LanguageManager
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.ui.screens.*
import com.nguyendevs.ecolens.ui.theme.EcoLensTheme
import com.nguyendevs.ecolens.view.EcoLensViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ComposeMainActivity : ComponentActivity() {
    private val viewModel: EcoLensViewModel by viewModels()
    private lateinit var languageManager: LanguageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        languageManager = LanguageManager(this)

        setContent {
            EcoLensTheme {
                MainAppContent(viewModel, languageManager)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: EcoLensViewModel, languageManager: LanguageManager) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var currentImageUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher để mở thư viện ảnh từ màn hình CameraScreen
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            // Quay về Home để hiển thị và xử lý ảnh
            navController.navigate("home") {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            viewModel.identifySpecies(uri, languageManager.getLanguage())
        }
    }

    // Xác định các màn hình cần ẩn BottomBar (ví dụ: màn hình chi tiết, camera)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf("home", "history", "my_garden", "settings")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    val currentDestination = navBackStackEntry?.destination

                    val items = listOf(
                        "home" to Icons.Default.Home,
                        "history" to Icons.Default.History,
                        "my_garden" to Icons.Default.LocalFlorist,
                        "settings" to Icons.Default.Settings
                    )

                    items.forEach { (route, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = null) },
                            selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == "home") {
                FloatingActionButton(
                    onClick = {
                        navController.navigate("camera")
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Màn hình chính (Home)
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    imageUri = currentImageUri,
                    languageCode = languageManager.getLanguage()
                )
            }

            // 2. Màn hình Camera
            composable("camera") {
                CameraScreen(
                    onImageCaptured = { uri ->
                        currentImageUri = uri
                        navController.popBackStack() // Đóng camera, về Home
                        viewModel.identifySpecies(uri, languageManager.getLanguage())
                    },
                    onCloseClick = { navController.popBackStack() },
                    onGalleryClick = { galleryLauncher.launch("image/*") }
                )
            }

            // 3. Màn hình Lịch sử (Danh sách)
            composable("history") {
                HistoryScreen(
                    viewModel = viewModel,
                    onItemClick = { entry ->
                        // Serialize object thành JSON để truyền qua Navigation arguments
                        val json = Gson().toJson(entry)
                        val encodedJson = URLEncoder.encode(json, StandardCharsets.UTF_8.toString())
                        navController.navigate("history_detail/$encodedJson")
                    }
                )
            }

            // 4. Màn hình Chi tiết Lịch sử
            composable(
                route = "history_detail/{jsonEntry}",
                arguments = listOf(navArgument("jsonEntry") { type = NavType.StringType })
            ) { backStackEntry ->
                val json = backStackEntry.arguments?.getString("jsonEntry")
                if (json != null) {
                    val decodedJson = URLDecoder.decode(json, StandardCharsets.UTF_8.toString())
                    val entry = Gson().fromJson(decodedJson, HistoryEntry::class.java)
                    HistoryDetailScreen(
                        historyEntry = entry,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            // 5. Màn hình Khu vườn của tôi (Chat History List)
            composable("my_garden") {
                ChatHistoryScreen(
                    viewModel = viewModel,
                    onSessionClick = { sessionId ->
                        navController.navigate("chat_detail/$sessionId")
                    },
                    onNewChatClick = {
                        navController.navigate("chat_detail/new")
                    }
                )
            }

            // 6. Màn hình Chat Chi tiết
            composable(
                route = "chat_detail/{sessionId}",
                arguments = listOf(navArgument("sessionId") {
                    type = NavType.StringType
                    nullable = true
                })
            ) { backStackEntry ->
                val sessionIdStr = backStackEntry.arguments?.getString("sessionId")
                val sessionId = if (sessionIdStr == "new") null else sessionIdStr?.toLongOrNull()

                ChatScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    sessionId = sessionId
                )
            }

            // 7. Màn hình Cài đặt
            composable("settings") {
                SettingsScreen(
                    onLanguageClick = { navController.navigate("language_selection") },
                    onAboutClick = { navController.navigate("about") }
                )
            }

            // 8. Màn hình Chọn ngôn ngữ
            composable("language_selection") {
                LanguageSelectionScreen(
                    onBackClick = { navController.popBackStack() },
                    onLanguageSelected = {
                        // Khởi động lại Activity để áp dụng ngôn ngữ mới
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                )
            }

            // 9. Màn hình Giới thiệu
            composable("about") {
                AboutScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}