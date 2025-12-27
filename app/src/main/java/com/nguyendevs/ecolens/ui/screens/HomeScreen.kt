package com.nguyendevs.ecolens.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role.Companion.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.managers.SpeakerManager
import com.nguyendevs.ecolens.model.EcoLensUiState
import com.nguyendevs.ecolens.model.LoadingStage
import com.nguyendevs.ecolens.model.SpeciesInfo
import com.nguyendevs.ecolens.ui.theme.*
import com.nguyendevs.ecolens.view.EcoLensViewModel
import com.nguyendevs.ecolens.utils.TextToSpeechGenerator

@Composable
fun HomeScreen(
    viewModel: EcoLensViewModel,
    uiState: EcoLensUiState,
    imageUri: Uri?,
    modifier: Modifier = Modifier,
    languageCode: String
) {
    val context = LocalContext.current
    var showFullScreenImage by remember { mutableStateOf(false) }

    // Speaker Manager Management
    val speakerManager = remember { SpeakerManager(context) }
    var isSpeaking by remember { mutableStateOf(false) }

    // Dọn dẹp speaker khi component bị hủy
    DisposableEffect(Unit) {
        onDispose {
            speakerManager.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Background)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Search Bar Area (Giả lập search bar cũ)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_search), contentDescription = null, tint = TextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Tìm kiếm...", color = TextSecondary)
                }
            }

            // 2. Image Preview Area
            if (imageUri != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Captured Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Zoom Controls
                        Column(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                        ) {
                            IconButton(
                                onClick = { showFullScreenImage = true },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom", tint = Color.White)
                            }
                        }
                    }
                }
            } else {
                // Initial State (Home Tree Image)
                Image(
                    painter = painterResource(id = R.drawable.home_tree),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // 3. Info / Loading / Error Area
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                if (uiState.isLoading) {
                    LoadingView()
                } else if (uiState.error != null) {
                    ErrorView(error = uiState.error!!)
                } else if (uiState.speciesInfo != null) {
                    SpeciesInfoView(info = uiState.speciesInfo!!)
                }
            }
        }

        // Floating Action Buttons for Speech
        if (uiState.speciesInfo != null && !uiState.isLoading) {
            FloatingActionButton(
                onClick = {
                    if (isSpeaking) {
                        speakerManager.pause()
                        isSpeaking = false
                    } else {
                        val text = TextToSpeechGenerator.generateSpeechText(context, uiState.speciesInfo!!)
                        if (text.isNotEmpty()) {
                            speakerManager.setLanguage(languageCode)
                            speakerManager.speak(text)
                            isSpeaking = true
                            speakerManager.onSpeechFinished = {
                                isSpeaking = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp, end = 16.dp),
                containerColor = Primary
            ) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Default.GraphicEq else Icons.Default.VolumeUp,
                    contentDescription = "Speak",
                    tint = Color.White
                )
            }
        }

        // Full Screen Image Dialog
        if (showFullScreenImage && imageUri != null) {
            Dialog(onDismissRequest = { showFullScreenImage = false }) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Full Screen",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    IconButton(
                        onClick = { showFullScreenImage = false },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = Primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Đang phân tích...", color = TextPrimary)
    }
}

@Composable
fun ErrorView(error: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ErrorBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Lỗi", color = Error, fontWeight = FontWeight.Bold)
            Text(error, color = Error)
        }
    }
}

@Composable
fun SpeciesInfoView(info: SpeciesInfo) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = info.commonName ?: "Không xác định",
                style = MaterialTheme.typography.headlineSmall,
                color = PrimaryDark,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = info.scientificName ?: "",
                style = MaterialTheme.typography.titleMedium,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = info.description ?: "Không có mô tả",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        }
    }
}