package com.nguyendevs.ecolens.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nguyendevs.ecolens.managers.LanguageManager

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val languageManager = remember { LanguageManager(context) }
    var currentLanguage by remember { mutableStateOf(languageManager.getLanguage()) }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SettingsItem(
                icon = Icons.Filled.Language,
                title = "Language",
                subtitle = if (currentLanguage == "vi") "Tiếng Việt" else "English",
                onClick = {
                    // Toggle language for simplicity, or show dialog
                    val newLang = if (currentLanguage == "vi") "en" else "vi"
                    languageManager.setLanguage(newLang)
                    currentLanguage = newLang
                    // Note: In a real app, you might need to restart the activity or recreate the UI
                }
            )

            SettingsItem(
                icon = Icons.Filled.Info,
                title = "About",
                subtitle = "EcoLens v1.0",
                onClick = { /* TODO: Show About Dialog/Screen */ }
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = "Contact & Social",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SettingsItem(
                icon = Icons.Filled.Email,
                title = "Feedback",
                subtitle = "Send us an email",
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:tainguyen.devs@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "EcoLens Support")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Handle error
                    }
                }
            )

            SettingsItem(
                icon = Icons.Filled.Share,
                title = "Facebook",
                subtitle = "Follow us on Facebook",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/NguyenDevs"))
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}