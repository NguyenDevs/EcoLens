package com.nguyendevs.ecolens.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.nguyendevs.ecolens.activities.CameraActivity
import com.nguyendevs.ecolens.managers.LanguageManager
import com.nguyendevs.ecolens.managers.SpeakerManager
import com.nguyendevs.ecolens.model.EcoLensUiState
import com.nguyendevs.ecolens.model.LoadingStage
import com.nguyendevs.ecolens.utils.TextToSpeechGenerator
import com.nguyendevs.ecolens.view.EcoLensViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: EcoLensViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val languageManager = remember { LanguageManager(context) }
    val speakerManager = remember { SpeakerManager(context) }
    var isSpeaking by remember { mutableStateOf(false) }

    // Cleanup speaker on dispose
    DisposableEffect(Unit) {
        onDispose {
            speakerManager.shutdown()
        }
    }

    // Update speaking state
    LaunchedEffect(Unit) {
        speakerManager.onSpeechFinished = {
            isSpeaking = false
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uriString = result.data?.getStringExtra(CameraActivity.KEY_IMAGE_URI)
            if (uriString != null) {
                val uri = Uri.parse(uriString)
                imageUri = uri
                viewModel.identifySpecies(uri, languageManager.getLanguage())
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.speciesInfo != null && !uiState.isLoading) {
                    FloatingActionButton(
                        onClick = {
                            if (isSpeaking) {
                                speakerManager.pause()
                                isSpeaking = false
                            } else {
                                uiState.speciesInfo?.let { info ->
                                    val text = TextToSpeechGenerator.generateSpeechText(context, info)
                                    if (text.isNotEmpty()) {
                                        speakerManager.setLanguage(languageManager.getLanguage())
                                        speakerManager.speak(text)
                                        isSpeaking = true
                                    }
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(
                            if (isSpeaking) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                            contentDescription = if (isSpeaking) "Mute" else "Speak"
                        )
                    }
                }

                FloatingActionButton(
                    onClick = {
                        if (isSpeaking) {
                            speakerManager.pause()
                            isSpeaking = false
                        }
                        cameraLauncher.launch(CameraActivity.newIntent(context))
                    }
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Camera")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "Captured Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Crop
                )
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                Text(text = "Identifying...", modifier = Modifier.padding(8.dp))
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            uiState.speciesInfo?.let { info ->
                SpeciesInfoCard(info)
            }
        }
    }
}

@Composable
fun SpeciesInfoCard(info: com.nguyendevs.ecolens.model.SpeciesInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = info.commonName,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = info.scientificName,
                style = MaterialTheme.typography.titleMedium,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = info.description)
        }
    }
}