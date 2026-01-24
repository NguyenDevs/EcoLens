package com.nguyendevs.ecolens.ui.screens.home

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.model.SpeciesInfo
import com.nguyendevs.ecolens.ui.components.EcoLensCard
import com.nguyendevs.ecolens.ui.theme.Dimens
import com.nguyendevs.ecolens.ui.theme.EcoLensCorners
import com.nguyendevs.ecolens.ui.theme.EcoLensTextStyles
import com.nguyendevs.ecolens.ui.theme.EcoLensTheme
import com.nguyendevs.ecolens.ui.theme.Primary
import com.nguyendevs.ecolens.ui.theme.PrimaryDark

/** Home screen composable. Replaces screen_home.xml (244 lines) */
@Composable
fun HomeScreen(
        isExpanded: Boolean,
        isLoading: Boolean,
        loadingText: String,
        error: String?,
        imageUri: Uri?,
        speciesInfo: SpeciesInfo?,
        onZoomClick: () -> Unit,
        onCopyScientificName: (String) -> Unit,
        onRetryClick: () -> Unit,
        modifier: Modifier = Modifier
) {
        val scrollState = rememberScrollState()

        Column(
                modifier =
                        modifier.fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .verticalScroll(scrollState)
                                .padding(
                                        start = Dimens.PaddingScreenHorizontal,
                                        end = Dimens.PaddingScreenHorizontal,
                                        top = 10.dp,
                                        bottom = 48.dp
                                )
        ) {
                // Header
                Text(
                        text = stringResource(R.string.app_title),
                        style = EcoLensTextStyles.Display,
                        color = Primary,
                        modifier = Modifier.padding(bottom = Dimens.SpacingXs)
                )

                Text(
                        text = stringResource(R.string.app_subtitle_home),
                        style = EcoLensTextStyles.Body2,
                        modifier = Modifier.padding(start = 4.dp, bottom = Dimens.SpacingLg)
                )

                // Image Preview Card
                ImagePreviewCard(
                        isExpanded = isExpanded,
                        isLoading = isLoading,
                        imageUri = imageUri,
                        onZoomClick = onZoomClick
                )

                // Loading Indicator
                AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
                        Row(
                                modifier = Modifier.fillMaxWidth().padding(top = Dimens.SpacingLg),
                                horizontalArrangement =
                                        androidx.compose.foundation.layout.Arrangement.Center
                        ) {
                                Text(
                                        text = loadingText,
                                        style = EcoLensTextStyles.Body1,
                                        fontWeight = FontWeight.Bold,
                                        color = Primary
                                )
                        }
                }

                // Error Card
                AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
                        ErrorCard(
                                errorMessage = error ?: "",
                                modifier = Modifier.padding(top = Dimens.SpacingLg)
                        )
                }

                // Species Info Card
                AnimatedVisibility(
                        visible = speciesInfo != null && error == null,
                        enter = fadeIn(),
                        exit = fadeOut()
                ) {
                        speciesInfo?.let { info ->
                                SpeciesInfoCard(
                                        speciesInfo = info,
                                        onCopyScientificName = onCopyScientificName,
                                        onRetryClick = onRetryClick,
                                        modifier = Modifier.padding(top = Dimens.SpacingLg)
                                )
                        }
                }
        }
}

/** Image preview card with initial and expanded states. */
@Composable
fun ImagePreviewCard(
        isExpanded: Boolean,
        isLoading: Boolean,
        imageUri: Uri?,
        onZoomClick: () -> Unit,
        modifier: Modifier = Modifier
) {
        val height by
                animateFloatAsState(
                        targetValue = if (isExpanded) 290f else 170f,
                        animationSpec = tween(durationMillis = 400),
                        label = "card_height"
                )

        EcoLensCard(modifier = modifier.height(height.dp), shape = EcoLensCorners.CardLarge) {
                Box(modifier = Modifier.fillMaxSize()) {
                        // Initial State (visible when not expanded)
                        AnimatedVisibility(
                                visible = !isExpanded,
                                enter = fadeIn(),
                                exit = fadeOut(animationSpec = tween(200))
                        ) { InitialState() }

                        // Image Preview (visible when expanded)
                        AnimatedVisibility(
                                visible = isExpanded && imageUri != null,
                                enter = fadeIn(animationSpec = tween(300)),
                                exit = fadeOut()
                        ) {
                                AsyncImage(
                                        model = imageUri,
                                        contentDescription = stringResource(R.string.image_preview),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                )
                        }

                        // Loading Overlay
                        AnimatedVisibility(
                                visible = isLoading && isExpanded,
                                enter = fadeIn(),
                                exit = fadeOut()
                        ) {
                                Box(
                                        modifier =
                                                Modifier.fillMaxSize()
                                                        .background(
                                                                EcoLensTheme.extendedColors
                                                                        .overlayLight
                                                        ),
                                        contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator(color = Primary) }
                        }

                        // Zoom Button
                        AnimatedVisibility(
                                visible = isExpanded && imageUri != null && !isLoading,
                                enter = fadeIn(),
                                exit = fadeOut(),
                                modifier =
                                        Modifier.align(Alignment.BottomEnd)
                                                .padding(Dimens.SpacingMd)
                        ) {
                                IconButton(
                                        onClick = onZoomClick,
                                        modifier =
                                                Modifier.size(Dimens.TouchTargetMin)
                                                        .clip(EcoLensCorners.Card)
                                                        .background(Color.Black.copy(alpha = 0.6f))
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.ZoomIn,
                                                contentDescription =
                                                        stringResource(R.string.zoom_in),
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                        )
                                }
                        }
                }
        }
}

/** Initial state content for image preview card. */
@Composable
private fun InitialState() {
        Box(modifier = Modifier.fillMaxSize().padding(Dimens.SpacingMd)) {
                Row(
                        modifier = Modifier.align(Alignment.CenterStart).padding(top = 36.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(Dimens.IconXl)
                        )

                        Column(modifier = Modifier.padding(start = Dimens.SpacingXs)) {
                                Text(
                                        text = stringResource(R.string.banner_title),
                                        style = EcoLensTextStyles.Headline1,
                                        fontSize = 27.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = PrimaryDark
                                )
                                Text(
                                        text = stringResource(R.string.banner_subtitle),
                                        style = EcoLensTextStyles.Headline3,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Primary
                                )
                        }
                }

                // Decorative images (simplified - using icons instead of actual images for now)
                // In production, these would be actual ShapeableImageView equivalents
        }
}

/** Error card component. */
@Composable
fun ErrorCard(errorMessage: String, modifier: Modifier = Modifier) {
        Row(
                modifier =
                        modifier.fillMaxWidth()
                                .clip(EcoLensCorners.Card)
                                .background(EcoLensTheme.extendedColors.confidenceLowBg)
                                .padding(Dimens.SpacingMd),
                verticalAlignment = Alignment.CenterVertically
        ) {
                Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(Dimens.IconMd)
                )

                Spacer(modifier = Modifier.width(Dimens.SpacingSm))

                Text(
                        text = errorMessage,
                        style = EcoLensTextStyles.Body2,
                        color = MaterialTheme.colorScheme.error
                )
        }
}
