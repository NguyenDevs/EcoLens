package com.nguyendevs.ecolens.ui.screens.main

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.model.SpeciesInfo
import com.nguyendevs.ecolens.ui.screens.home.HomeScreen
import com.nguyendevs.ecolens.ui.screens.settings.SettingsScreen
import com.nguyendevs.ecolens.ui.theme.Dimens
import com.nguyendevs.ecolens.ui.theme.EcoLensCorners
import com.nguyendevs.ecolens.ui.theme.NavSelected
import com.nguyendevs.ecolens.ui.theme.NavUnselected
import com.nguyendevs.ecolens.ui.theme.Primary

/** Main screen with bottom navigation. Replaces activity_main.xml (240 lines) */
@Composable
fun MainScreen(
        // Home screen state
        isHomeExpanded: Boolean,
        isLoading: Boolean,
        loadingText: String,
        error: String?,
        imageUri: Uri?,
        speciesInfo: SpeciesInfo?,

        // Settings state
        currentLanguage: String,
        isDarkMode: Boolean,
        isLoggedIn: Boolean,

        // Speaker state
        isSpeaking: Boolean,
        showSpeakerButton: Boolean,

        // Callbacks
        onCameraClick: () -> Unit,
        onZoomClick: () -> Unit,
        onCopyScientificName: (String) -> Unit,
        onRetryClick: () -> Unit,
        onSpeakClick: () -> Unit,
        onMuteClick: () -> Unit,

        // Settings callbacks
        onLanguageClick: () -> Unit,
        onDarkModeToggle: (Boolean) -> Unit,
        onFeedbackClick: () -> Unit,
        onFacebookClick: () -> Unit,
        onInstagramClick: () -> Unit,
        onTiktokClick: () -> Unit,
        onAboutClick: () -> Unit,
        onChangeUsernameClick: () -> Unit,
        onChangePasswordClick: () -> Unit,
        onLinkGoogleClick: () -> Unit,
        onDeleteAccountClick: () -> Unit,
        onLogoutClick: () -> Unit,

        // History/Chat content composables
        historyContent: @Composable () -> Unit,
        chatContent: @Composable () -> Unit,
        modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val navItems =
            listOf(
                    NavItem(Icons.Default.Home, stringResource(R.string.nav_home)),
                    NavItem(Icons.Default.History, stringResource(R.string.nav_history)),
                    NavItem(null, ""), // Placeholder for FAB
                    NavItem(Icons.Default.ChatBubble, stringResource(R.string.nav_chat)),
                    NavItem(Icons.Default.Settings, stringResource(R.string.nav_settings))
            )

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
                bottomBar = {
                    EcoLensBottomNavigation(
                            items = navItems,
                            selectedIndex = selectedTab,
                            onItemSelected = { index ->
                                if (index != 2) { // Skip FAB placeholder
                                    selectedTab = index
                                }
                            }
                    )
                }
        ) { paddingValues ->
            // Content area
            Box(
                    modifier =
                            Modifier.fillMaxSize()
                                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                when (selectedTab) {
                    0 ->
                            HomeScreen(
                                    isExpanded = isHomeExpanded,
                                    isLoading = isLoading,
                                    loadingText = loadingText,
                                    error = error,
                                    imageUri = imageUri,
                                    speciesInfo = speciesInfo,
                                    onZoomClick = onZoomClick,
                                    onCopyScientificName = onCopyScientificName,
                                    onRetryClick = onRetryClick
                            )
                    1 -> historyContent()
                    3 -> chatContent()
                    4 ->
                            SettingsScreen(
                                    currentLanguage = currentLanguage,
                                    isDarkMode = isDarkMode,
                                    isLoggedIn = isLoggedIn,
                                    onLanguageClick = onLanguageClick,
                                    onDarkModeToggle = onDarkModeToggle,
                                    onFeedbackClick = onFeedbackClick,
                                    onFacebookClick = onFacebookClick,
                                    onInstagramClick = onInstagramClick,
                                    onTiktokClick = onTiktokClick,
                                    onAboutClick = onAboutClick,
                                    onChangeUsernameClick = onChangeUsernameClick,
                                    onChangePasswordClick = onChangePasswordClick,
                                    onLinkGoogleClick = onLinkGoogleClick,
                                    onDeleteAccountClick = onDeleteAccountClick,
                                    onLogoutClick = onLogoutClick
                            )
                }
            }
        }

        // Camera FAB (centered above bottom nav)
        FloatingActionButton(
                onClick = onCameraClick,
                modifier =
                        Modifier.align(Alignment.BottomCenter)
                                .offset(y = (-52).dp)
                                .size(Dimens.FabSizeLarge)
                                .zIndex(10f)
                                .shadow(elevation = 27.dp, shape = EcoLensCorners.Fab),
                shape = EcoLensCorners.Fab,
                containerColor = Primary,
                contentColor = Color.White,
                elevation =
                        FloatingActionButtonDefaults.elevation(
                                defaultElevation = 27.dp,
                                pressedElevation = 20.dp
                        )
        ) {
            Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = stringResource(R.string.camera),
                    modifier = Modifier.size(50.dp)
            )
        }

        // Speaker FAB (visible when showing species info and not speaking)
        AnimatedVisibility(
                visible = showSpeakerButton && !isSpeaking && selectedTab == 0,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier =
                        Modifier.align(Alignment.TopEnd)
                                .padding(top = 100.dp, end = Dimens.SpacingMd)
        ) {
            FloatingActionButton(
                    onClick = onSpeakClick,
                    modifier = Modifier.size(50.dp),
                    shape = CircleShape,
                    containerColor = Primary,
                    contentColor = Color.White,
                    elevation =
                            FloatingActionButtonDefaults.elevation(
                                    defaultElevation = Dimens.ElevationXs
                            )
            ) {
                Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = stringResource(R.string.speak),
                        modifier = Modifier.size(30.dp)
                )
            }
        }

        // Mute FAB (visible when speaking)
        AnimatedVisibility(
                visible = isSpeaking && selectedTab == 0,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier =
                        Modifier.align(Alignment.TopEnd)
                                .padding(top = 100.dp, end = Dimens.SpacingMd)
        ) {
            FloatingActionButton(
                    onClick = onMuteClick,
                    modifier = Modifier.size(50.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White,
                    elevation =
                            FloatingActionButtonDefaults.elevation(
                                    defaultElevation = Dimens.ElevationXs
                            )
            ) {
                Icon(
                        imageVector = Icons.Default.VolumeOff,
                        contentDescription = stringResource(R.string.mute),
                        modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

private data class NavItem(val icon: ImageVector?, val label: String)

/** Custom bottom navigation with center FAB space. */
@Composable
private fun EcoLensBottomNavigation(
        items: List<NavItem>,
        selectedIndex: Int,
        onItemSelected: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        NavigationBar(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(Dimens.BottomNavHeight)
                                .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)),
                containerColor = Color.White,
                tonalElevation = Dimens.BottomNavElevation
        ) {
            items.forEachIndexed { index, item ->
                if (item.icon != null) {
                    NavigationBarItem(
                            selected = selectedIndex == index,
                            onClick = { onItemSelected(index) },
                            icon = {
                                Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        modifier = Modifier.size(Dimens.BottomNavIconSize)
                                )
                            },
                            label = { Text(text = item.label, fontSize = 10.sp) },
                            colors =
                                    NavigationBarItemDefaults.colors(
                                            selectedIconColor = NavSelected,
                                            selectedTextColor = NavSelected,
                                            unselectedIconColor = NavUnselected,
                                            unselectedTextColor = NavUnselected,
                                            indicatorColor = Color.Transparent
                                    )
                    )
                } else {
                    // Empty space for FAB
                    NavigationBarItem(
                            selected = false,
                            onClick = {},
                            icon = { Box(modifier = Modifier.size(Dimens.BottomNavIconSize)) },
                            label = {},
                            enabled = false
                    )
                }
            }
        }

        // FAB background circle (for visual effect like XML)
        Box(
                modifier =
                        Modifier.align(Alignment.TopCenter)
                                .offset(y = (-18).dp)
                                .size(90.dp)
                                .clip(EcoLensCorners.Fab)
                                .background(Color.White)
        )
    }
}
