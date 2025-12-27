package com.example.ecolens.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ecolens.R
import com.example.ecolens.ui.theme.*

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    var showLanguageSelection by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var currentLanguage by remember { mutableStateOf("Tiếng Việt") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(top = 10.dp, bottom = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.displayLarge,
                color = Primary,
                modifier = Modifier.padding(bottom = 15.dp)
            )

            HorizontalDivider(
                thickness = 1.5.dp,
                color = BorderNormal,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = (-20).dp, vertical = 10.dp)
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            // Preferences Section
            SettingsSection(
                title = stringResource(R.string.preferences)
            ) {
                SettingsItem(
                    icon = R.drawable.ic_language,
                    title = stringResource(R.string.language_label),
                    subtitle = currentLanguage,
                    onClick = { showLanguageSelection = true }
                )
            }

            // Support Section
            SettingsSection(
                title = stringResource(R.string.support)
            ) {
                SettingsItem(
                    icon = R.drawable.ic_mail,
                    title = stringResource(R.string.email_support),
                    onClick = { /* Open email */ }
                )
            }

            // About Section
            SettingsSection(
                title = stringResource(R.string.about_label)
            ) {
                SettingsItem(
                    icon = R.drawable.ic_facebook,
                    title = stringResource(R.string.follow_on_facebook),
                    onClick = { /* Open Facebook */ },
                    showDivider = true
                )

                SettingsItem(
                    icon = R.drawable.ic_instagram,
                    title = stringResource(R.string.follow_on_instagram),
                    onClick = { /* Open Instagram */ },
                    showDivider = true
                )

                SettingsItem(
                    icon = R.drawable.ic_tiktok,
                    title = stringResource(R.string.follow_on_tiktok),
                    onClick = { /* Open TikTok */ },
                    showDivider = true
                )

                SettingsItem(
                    icon = R.drawable.ic_info,
                    title = stringResource(R.string.about_app_title),
                    onClick = { showAbout = true }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Dialogs
    if (showLanguageSelection) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = {
                currentLanguage = it
                showLanguageSelection = false
            },
            onDismiss = { showLanguageSelection = false }
        )
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = Spacing.xxs, bottom = Spacing.sm)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = Elevation.none),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderNormal)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: Int,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    showDivider: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(IconSize.lg)
                    .background(Color.Transparent, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = PrimaryDark,
                    modifier = Modifier.size(IconSize.lg)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.md)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = PrimaryDark,
                    fontWeight = FontWeight.Bold
                )

                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                }
            }
        }

        if (showDivider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = BorderNormal
            )
        }
    }
}

// ==================== Language Selection Dialog ====================
@Composable
private fun LanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_label)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                LanguageOption(
                    flag = R.drawable.flag_vietnam,
                    name = stringResource(R.string.lang_vietnamese),
                    isSelected = currentLanguage == "Tiếng Việt",
                    onClick = { onLanguageSelected("Tiếng Việt") }
                )

                LanguageOption(
                    flag = R.drawable.flag_england,
                    name = stringResource(R.string.lang_english),
                    isSelected = currentLanguage == "English",
                    onClick = { onLanguageSelected("English") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun LanguageOption(
    flag: Int,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Primary10 else Surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Primary else BorderNormal
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(48.dp, 32.dp),
                shape = RoundedCornerShape(4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = Elevation.none),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
            ) {
                // Flag image would go here
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BorderLight)
                )
            }

            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Primary else TextPrimary,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.md)
            )
        }
    }
}

// ==================== About Dialog ====================
@Composable
private fun AboutDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.about_label),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Card(
                    modifier = Modifier.size(120.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = Elevation.none)
                ) {
                    // App icon
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Primary10)
                    )
                }

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Primary
                )

                Card(
                    shape = RoundedCornerShape(999.dp),
                    colors = CardDefaults.cardColors(containerColor = BorderNormal)
                ) {
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(
                            horizontal = Spacing.sm,
                            vertical = Spacing.xxs
                        )
                    )
                }

                Text(
                    text = stringResource(R.string.about_app_desc),
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = "© 2024 NguyenDevs",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}