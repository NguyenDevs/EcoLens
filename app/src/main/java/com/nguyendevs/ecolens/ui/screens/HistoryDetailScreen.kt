package com.nguyendevs.ecolens.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.managers.SpeakerManager
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.model.SpeciesInfo
import com.nguyendevs.ecolens.ui.theme.*
import com.nguyendevs.ecolens.utils.TextToSpeechGenerator
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    historyEntry: HistoryEntry,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val speakerManager = remember { SpeakerManager(context) }
    var isSpeaking by remember { mutableStateOf(false) }
    val info = historyEntry.speciesInfo

    DisposableEffect(Unit) {
        onDispose { speakerManager.shutdown() }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isSpeaking) {
                        speakerManager.pause()
                        isSpeaking = false
                    } else {
                        val text = TextToSpeechGenerator.generateSpeechText(context, info)
                        if (text.isNotEmpty()) {
                            speakerManager.speak(text)
                            isSpeaking = true
                            speakerManager.onSpeechFinished = { isSpeaking = false }
                        }
                    }
                },
                containerColor = if (isSpeaking) Error else Primary
            ) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Default.GraphicEq else Icons.Default.VolumeUp,
                    contentDescription = "Speak",
                    tint = Color.White
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .verticalScroll(scrollState)
                .padding(padding)
        ) {
            // Header Image Area
            Box(modifier = Modifier.height(300.dp).fillMaxWidth()) {
                AsyncImage(
                    model = historyEntry.imagePath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Overlay Gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )

                // Toolbar Items
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    IconButton(onClick = { shareSpeciesInfo(context, info, historyEntry.imagePath) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }
                }

                // Title Overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    HtmlText(
                        html = info.commonName,
                        style = MaterialTheme.typography.headlineMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                    )
                    HtmlText(
                        html = info.scientificName,
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White.copy(alpha = 0.8f), fontStyle = FontStyle.Italic)
                    )

                    // Tags
                    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (info.kingdom.isNotEmpty()) Tag(info.kingdom)
                        if (info.family.isNotEmpty()) Tag(info.family)
                    }
                }
            }

            // Content Area
            Column(modifier = Modifier.padding(16.dp)) {
                // Taxonomy Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Phân loại học", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        TaxonomyRow("Giới", info.kingdom)
                        TaxonomyRow("Ngành", info.phylum)
                        TaxonomyRow("Lớp", info.className)
                        TaxonomyRow("Bộ", info.taxorder)
                        TaxonomyRow("Họ", info.family)
                        TaxonomyRow("Chi", info.genus)
                        TaxonomyRow("Loài", info.species)
                    }
                }

                // Info Sections
                InfoSection("Mô tả", info.description)
                InfoSection("Đặc điểm", info.characteristics)
                InfoSection("Phân bố", info.distribution)
                InfoSection("Môi trường sống", info.habitat)
                InfoSection("Tình trạng bảo tồn", info.conservationStatus)

                Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
            }
        }
    }
}

@Composable
fun Tag(text: String) {
    Surface(
        color = Primary.copy(alpha = 0.8f),
        shape = RoundedCornerShape(4.dp)
    ) {
        HtmlText(
            html = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium.copy(color = Color.White)
        )
    }
}

@Composable
fun TaxonomyRow(label: String, value: String) {
    if (value.isNotEmpty()) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(text = "$label:", modifier = Modifier.width(80.dp), fontWeight = FontWeight.SemiBold, color = TextSecondary)
            HtmlText(html = value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun InfoSection(title: String, content: String) {
    if (content.isNotEmpty()) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            HtmlText(html = content, style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, lineHeight = 24.sp))
        }
    }
}

@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current) {
    val spanned = remember(html) {
        HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
    }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                textSize = style.fontSize.value
                setTextColor(style.color.toArgb(context))
            }
        },
        update = {
            it.text = spanned
            it.setTextColor(style.color.toArgb(it.context)) // Ensure color updates
        }
    )
}

// Helper color extension for AndroidView
fun androidx.compose.ui.graphics.Color.toArgb(context: android.content.Context): Int {
    return android.graphics.Color.argb((alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
}

// Share Logic (Copied from Fragment)
fun shareSpeciesInfo(context: android.content.Context, info: SpeciesInfo, imagePath: String?) {
    // Logic tương tự Fragment cũ
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "EcoLens: ${info.commonName}")
        putExtra(Intent.EXTRA_TEXT, "${info.commonName}\n${info.scientificName}\n\n${info.description}")
        if (imagePath != null) {
            val file = File(imagePath)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}