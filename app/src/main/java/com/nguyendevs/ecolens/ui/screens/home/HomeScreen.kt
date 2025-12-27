// ui/screens/home/HomeScreen.kt
package com.nguyendevs.ecolens.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nguyendevs.ecolens.ui.components.*
import com.nguyendevs.ecolens.ui.theme.*

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onCameraClick: () -> Unit,
    onImagePreviewClick: () -> Unit,
    onRetryIdentification: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg)
            .padding(top = 10.dp, bottom = 100.dp)
    ) {
        // Header
        Text(
            text = stringResource(R.string.app_title),
            style = MaterialTheme.typography.displayLarge,
            color = Primary,
            modifier = Modifier.padding(bottom = Spacing.xs)
        )

        Text(
            text = stringResource(R.string.app_subtitle_home),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier
                .padding(start = 4.dp)
                .padding(bottom = Spacing.lg)
        )

        // Image Preview Card
        ImagePreviewCard(
            imageUri = uiState.imageUri,
            isLoading = uiState.isLoading,
            onCameraClick = onCameraClick,
            onImageClick = if (uiState.imageUri != null) onImagePreviewClick else null,
            modifier = Modifier.fillMaxWidth()
        )

        // Loading Indicator
        AnimatedVisibility(
            visible = uiState.isLoading,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            LoadingIndicator(
                text = stringResource(R.string.loading_analyzing),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg)
            )
        }

        // Error Card
        AnimatedVisibility(
            visible = uiState.error != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            ErrorCard(
                message = uiState.error ?: stringResource(R.string.error_default),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg)
            )
        }

        // Species Info Card
        AnimatedVisibility(
            visible = uiState.speciesInfo != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            uiState.speciesInfo?.let { species ->
                SpeciesInfoCard(
                    speciesInfo = species,
                    onCopyScientificName = { /* TODO */ },
                    onShare = { /* TODO */ },
                    onRetry = onRetryIdentification,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.lg)
                )
            }
        }
    }
}

@Composable
private fun ImagePreviewCard(
    imageUri: String?,
    isLoading: Boolean,
    onCameraClick: () -> Unit,
    onImageClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(170.dp)
            .clickable(enabled = onImageClick != null) { onImageClick?.invoke() },
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.none),
        border = BorderStroke(1.dp, BorderNormal)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Initial State
            AnimatedVisibility(
                visible = imageUri == null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                InitialStateLayout(
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Image Preview
            imageUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = stringResource(R.string.image_preview),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Loading Overlay
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(OverlayLight),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Primary
                    )
                }
            }

            // Zoom Button
            if (imageUri != null && !isLoading) {
                IconButton(
                    onClick = { onImageClick?.invoke() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(Spacing.md)
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_zoomin),
                        contentDescription = stringResource(R.string.zoom_in),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun InitialStateLayout(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(Spacing.md)
    ) {
        // Camera Icon
        Icon(
            painter = painterResource(R.drawable.ic_camera_home),
            contentDescription = stringResource(R.string.camera),
            tint = Primary,
            modifier = size(IconSize.xl)
                .align(Alignment.TopStart)
                .padding(top = 36.dp)
        )

        // Title and Subtitle
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = IconSize.xl + Spacing.xs, top = 35.dp)
        ) {
            Text(
                text = stringResource(R.string.banner_title),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = PrimaryDark
            )

            Text(
                text = stringResource(R.string.banner_subtitle),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Primary
            )
        }

        // Decorative Images
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 46.dp)
        ) {
            // Background image
            Image(
                painter = painterResource(R.drawable.home_tree_2),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.3f,
                modifier = Modifier
                    .size(95.dp)
                    .rotate(-25f)
                    .offset(x = 33.dp, y = 19.dp)
            )

            // Foreground image
            Image(
                painter = painterResource(R.drawable.home_tree),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.8f,
                modifier = Modifier
                    .size(100.dp)
                    .rotate(-8f)
                    .offset(x = 33.dp, y = 10.dp)
            )
        }
    }
}

@Composable
private fun LoadingIndicator(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ErrorCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ErrorBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = stringResource(R.string.error),
                tint = Error,
                modifier = size(IconSize.md)
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = ErrorText,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Data classes
data class HomeUiState(
    val imageUri: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val speciesInfo: SpeciesInfo? = null
)

data class SpeciesInfo(
    val commonName: String,
    val scientificName: String,
    val confidence: Float,
    val taxonomy: Taxonomy,
    val description: String? = null,
    val characteristics: String? = null,
    val distribution: String? = null,
    val habitat: String? = null,
    val conservation: String? = null
)

data class Taxonomy(
    val kingdom: String?,
    val phylum: String?,
    val classValue: String?,
    val order: String?,
    val family: String?,
    val genus: String?,
    val species: String?
)