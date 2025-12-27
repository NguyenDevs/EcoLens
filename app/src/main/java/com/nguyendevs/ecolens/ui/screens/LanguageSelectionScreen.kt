package com.nguyendevs.ecolens.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.managers.LanguageManager
import com.nguyendevs.ecolens.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionScreen(
    onBackClick: () -> Unit,
    onLanguageSelected: () -> Unit // Callback to restart activity
) {
    val context = LocalContext.current
    val languageManager = remember { LanguageManager(context) }
    val currentLang = languageManager.getLanguage()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chọn ngôn ngữ") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LanguageItem(
                name = "Tiếng Việt",
                flagRes = R.drawable.flag_vietnam,
                isSelected = currentLang == LanguageManager.LANG_VI,
                onClick = {
                    if (currentLang != LanguageManager.LANG_VI) {
                        languageManager.setLanguage(LanguageManager.LANG_VI)
                        onLanguageSelected()
                    }
                }
            )
            LanguageItem(
                name = "English",
                flagRes = R.drawable.flag_england,
                isSelected = currentLang == LanguageManager.LANG_EN,
                onClick = {
                    if (currentLang != LanguageManager.LANG_EN) {
                        languageManager.setLanguage(LanguageManager.LANG_EN)
                        onLanguageSelected()
                    }
                }
            )
        }
    }
}

@Composable
fun LanguageItem(name: String, flagRes: Int, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) Primary else Color.Transparent

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = flagRes),
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) Primary else TextPrimary
            )
        }
    }
}