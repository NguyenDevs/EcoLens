package com.nguyendevs.ecolens.ui.screens.main

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.ui.screens.chat.ChatHistoryScreen
import com.nguyendevs.ecolens.ui.screens.history.HistoryScreen
import com.nguyendevs.ecolens.ui.screens.home.HomeScreen
import com.nguyendevs.ecolens.ui.screens.settings.SettingsScreen
import com.nguyendevs.ecolens.ui.theme.*
import com.nguyendevs.ecolens.view.EcoLensViewModel

enum class Screen {
    HOME, HISTORY, CHAT, SETTINGS
}

@Composable
fun MainScreen(
    viewModel: EcoLensViewModel,
    onNavigateToCamera: () -> Unit,
    onNavigateToChatDetail: (Long?) -> Unit,
    onNavigateToHistoryDetail: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var showSearchBar by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = com.nguyendevs.ecolens.ui.theme.Background,
        bottomBar = {
            MainBottomNavigation(
                currentScreen = currentScreen,
                onScreenSelected = {
                    currentScreen = it
                    showSearchBar = it == Screen.HOME
                },
                onCameraClick = onNavigateToCamera
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Screen Content với animation
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150)) togetherWith
                            fadeOut(animationSpec = tween(150))
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        uiState = viewModel.uiState.collectAsState().value,
                        onCameraClick = onNavigateToCamera,
                        onImagePreviewClick = { /* Handle zoom */ },
                        onRetryIdentification = { viewModel.retryIdentification() }
                    )
                    Screen.HISTORY -> HistoryScreen(
                        viewModel = viewModel,
                        onItemClick = onNavigateToHistoryDetail
                    )
                    Screen.CHAT -> ChatHistoryScreen(
                        viewModel = viewModel,
                        onSessionClick = onNavigateToChatDetail
                    )
                    Screen.SETTINGS -> SettingsScreen()
                }
            }

            // Search Bar (only on Home screen)
            if (currentScreen == Screen.HOME) {
                SearchBar(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = Spacing.lg, end = Spacing.md)
                )
            }

            // Voice Control FABs (only on Home with species info)
            val homeUiState = viewModel.uiState.collectAsState().value
            if (currentScreen == Screen.HOME &&
                homeUiState.speciesInfo != null &&
                !homeUiState.isLoading) {
                VoiceControlFABs(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 100.dp, end = Spacing.md)
                )
            }
        }
    }
}

@Composable
private fun MainBottomNavigation(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    onCameraClick: () -> Unit
) {
    Box {
        // White background for Camera FAB
        FloatingActionButton(
            onClick = { },
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-22).dp),
            containerColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp
            ),
            shape = MaterialTheme.shapes.small
        ) {}

        // Main Navigation Bar
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = com.nguyendevs.ecolens.ui.theme.Surface,
            tonalElevation = 8.dp
        ) {
            // Home
            NavigationBarItem(
                selected = currentScreen == Screen.HOME,
                onClick = { onScreenSelected(Screen.HOME) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_home),
                        contentDescription = null,
                        modifier = Modifier.size(com.nguyendevs.ecolens.ui.theme.IconSize.md)
                    )
                },
                label = { Text(stringResource(R.string.nav_home)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = com.nguyendevs.ecolens.ui.theme.NavSelected,
                    selectedTextColor = com.nguyendevs.ecolens.ui.theme.NavSelected,
                    unselectedIconColor = com.nguyendevs.ecolens.ui.theme.NavUnselected,
                    unselectedTextColor = com.nguyendevs.ecolens.ui.theme.NavUnselected,
                    indicatorColor = Color.Transparent
                )
            )

            // History
            NavigationBarItem(
                selected = currentScreen == Screen.HISTORY,
                onClick = { onScreenSelected(Screen.HISTORY) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_history),
                        contentDescription = null,
                        modifier = Modifier.size(com.nguyendevs.ecolens.ui.theme.IconSize.md)
                    )
                },
                label = { Text(stringResource(R.string.nav_history)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = com.nguyendevs.ecolens.ui.theme.NavSelected,
                    selectedTextColor = com.nguyendevs.ecolens.ui.theme.NavSelected,
                    unselectedIconColor = com.nguyendevs.ecolens.ui.theme.NavUnselected,
                    unselectedTextColor = com.nguyendevs.ecolens.ui.theme.NavUnselected,
                    indicatorColor = Color.Transparent
                )
            )

            // Placeholder for Camera (middle)
            Spacer(modifier = Modifier.weight(1f))

            // Chat
            NavigationBarItem(
                selected = currentScreen == Screen.CHAT,
                onClick = { onScreenSelected(Screen.CHAT) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_chat_bot),
                        contentDescription = null,
                        modifier = Modifier.size(com.nguyendevs.ecolens.ui.theme.IconSize.md)
                    )
                },
                label = { Text(stringResource(R.string.nav_my_garden)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = com.nguyendevs.ecolens.ui.theme.NavSelected,
                    selectedTextColor = com.nguyendevs.ecolens.ui.theme.NavSelected,
                    unselectedIconColor = com.nguyendevs.ecolens.ui.theme.NavUnselected,
                    unselectedTextColor = com.nguyendevs.ecolens.ui.theme.NavUnselected,
                    indicatorColor = Color.Transparent
                )
            )

            // Settings
            NavigationBarItem(
                selected = currentScreen == Screen.SETTINGS,
                onClick = { onScreenSelected(Screen.SETTINGS) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = null,
                        modifier = Modifier.size(com.nguyendevs.ecolens.ui.theme.IconSize.md)
                    )
                },
                label = { Text(stringResource(R.string.nav_settings)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = com.nguyendevs.ecolens.ui.theme.NavSelected,
                    selectedTextColor = com.nguyendevs.ecolens.ui.theme.NavSelected,
                    unselectedIconColor = com.nguyendevs.ecolens.ui.theme.NavUnselected,
                    unselectedTextColor = com.nguyendevs.ecolens.ui.theme.NavUnselected,
                    indicatorColor = Color.Transparent
                )
            )
        }

        // Camera FAB
        FloatingActionButton(
            onClick = onCameraClick,
            modifier = Modifier
                .size(70.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-22).dp),
            containerColor = com.nguyendevs.ecolens.ui.theme.Primary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 27.dp,
                pressedElevation = 20.dp
            ),
            shape = MaterialTheme.shapes.small
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_camera),
                contentDescription = stringResource(R.string.camera),
                tint = Color.White,
                modifier = Modifier.size(50.dp)
            )
        }
    }
}

@Composable
private fun SearchBar(
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val width by animateDpAsState(
        targetValue = if (isExpanded) 330.dp else 50.dp,
        animationSpec = tween(320)
    )

    Card(
        modifier = modifier.width(width),
        shape = com.nguyendevs.ecolens.ui.theme.ButtonShape,
        colors = CardDefaults.cardColors(containerColor = com.nguyendevs.ecolens.ui.theme.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = com.nguyendevs.ecolens.ui.theme.Elevation.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (!isExpanded) {
                        isExpanded = true
                    } else {
                        // Perform Google search
                        isExpanded = false
                        searchQuery = ""
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = stringResource(R.string.search),
                    tint = com.nguyendevs.ecolens.ui.theme.Primary,
                    modifier = Modifier.size(com.nguyendevs.ecolens.ui.theme.IconSize.md)
                )
            }

            if (isExpanded) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = Spacing.sm),
                    placeholder = {
                        Text(
                            stringResource(R.string.search_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = com.nguyendevs.ecolens.ui.theme.TextTertiary
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }
        }
    }
}

@Composable
private fun VoiceControlFABs(
    modifier: Modifier = Modifier
) {
    var isSpeaking by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        if (!isSpeaking) {
            FloatingActionButton(
                onClick = { isSpeaking = true },
                modifier = Modifier.size(50.dp),
                containerColor = com.nguyendevs.ecolens.ui.theme.Primary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = com.nguyendevs.ecolens.ui.theme.Elevation.xs
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_speak),
                    contentDescription = stringResource(R.string.speak),
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        } else {
            FloatingActionButton(
                onClick = { isSpeaking = false },
                modifier = Modifier.size(50.dp),
                containerColor = com.nguyendevs.ecolens.ui.theme.Error,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = com.nguyendevs.ecolens.ui.theme.Elevation.xs
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_mute),
                    contentDescription = stringResource(R.string.mute),
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}