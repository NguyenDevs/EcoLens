package com.nguyendevs.ecolens.ui.screens.camera

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.ui.theme.Dimens
import com.nguyendevs.ecolens.ui.theme.PrimaryDark

/** Camera screen composable. Replaces activity_camera.xml (146 lines) */
@Composable
fun CameraScreen(
        isFlashOn: Boolean,
        onFlashToggle: () -> Unit,
        onCloseClick: () -> Unit,
        onCaptureClick: () -> Unit,
        onUploadClick: () -> Unit,
        onSwitchCameraClick: () -> Unit,
        onPreviewViewReady: (PreviewView) -> Unit,
        focusPoint: Offset? = null,
        modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Camera Preview
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(70.dp))

            // Camera preview area
            Box(modifier = Modifier.fillMaxWidth().height(550.dp)) {
                // AndroidView for PreviewView
                CameraPreview(
                        onPreviewViewReady = onPreviewViewReady,
                        modifier = Modifier.fillMaxSize()
                )

                // Viewfinder Frame
                ViewfinderFrame(
                        modifier = Modifier.align(Alignment.Center).width(350.dp).aspectRatio(1f)
                )

                // Focus Indicator
                focusPoint?.let { point ->
                    FocusIndicator(offset = point, modifier = Modifier.size(70.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // Top Controls
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(
                                        start = Dimens.SpacingLg,
                                        end = Dimens.SpacingLg,
                                        top = 42.dp
                                ),
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Flash Toggle
            IconButton(onClick = onFlashToggle, modifier = Modifier.size(Dimens.IconXl)) {
                Icon(
                        imageVector =
                                if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = stringResource(R.string.flash_toggle),
                        tint = Color.White,
                        modifier = Modifier.size(Dimens.IconXl).padding(Dimens.SpacingXs)
                )
            }

            // Close Button
            IconButton(onClick = onCloseClick, modifier = Modifier.size(50.dp)) {
                Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = Color.White,
                        modifier = Modifier.size(50.dp).padding(Dimens.SpacingXs)
                )
            }
        }

        // Bottom Controls
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
        ) {
            // Upload Button
            IconButton(onClick = onUploadClick, modifier = Modifier.size(Dimens.FabSizeNormal)) {
                Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = stringResource(R.string.upload),
                        tint = Color.White,
                        modifier = Modifier.size(Dimens.FabSizeNormal).padding(14.dp)
                )
            }

            // Capture Button
            CaptureButton(onClick = onCaptureClick)

            // Switch Camera Button
            IconButton(
                    onClick = onSwitchCameraClick,
                    modifier = Modifier.size(Dimens.FabSizeNormal)
            ) {
                Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.rotate),
                        tint = Color.White,
                        modifier = Modifier.size(Dimens.FabSizeNormal).padding(12.dp)
                )
            }
        }
    }
}

/** Camera preview using AndroidView to wrap PreviewView */
@Composable
fun CameraPreview(onPreviewViewReady: (PreviewView) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams =
                            ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                            )
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    onPreviewViewReady(this)
                }
            },
            modifier = modifier
    )
}

/** Capture button with press animation */
@Composable
fun CaptureButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by
            animateFloatAsState(
                    targetValue = if (isPressed) 0.9f else 1f,
                    animationSpec = tween(100),
                    label = "capture_scale"
            )

    Box(modifier = modifier.size(68.dp).scale(scale), contentAlignment = Alignment.Center) {
        // Outer border
        Box(modifier = Modifier.size(68.dp).border(2.dp, Color.White, CircleShape))

        // Inner button
        Box(
                modifier =
                        Modifier.size(58.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = onClick
                                ),
                contentAlignment = Alignment.Center
        ) {
            Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.capture),
                    tint = PrimaryDark,
                    modifier = Modifier.size(30.dp).padding(start = 2.dp, top = 0.dp)
            )
        }
    }
}

/** Viewfinder frame with corner brackets */
@Composable
fun ViewfinderFrame(
        modifier: Modifier = Modifier,
        cornerLength: Dp = 40.dp,
        cornerWidth: Dp = 3.dp,
        color: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val cornerLengthPx = cornerLength.toPx()
        val strokeWidth = cornerWidth.toPx()

        // Top-left corner
        drawLine(
                color = color,
                start = Offset(0f, cornerLengthPx),
                end = Offset(0f, 0f),
                strokeWidth = strokeWidth
        )
        drawLine(
                color = color,
                start = Offset(0f, 0f),
                end = Offset(cornerLengthPx, 0f),
                strokeWidth = strokeWidth
        )

        // Top-right corner
        drawLine(
                color = color,
                start = Offset(width - cornerLengthPx, 0f),
                end = Offset(width, 0f),
                strokeWidth = strokeWidth
        )
        drawLine(
                color = color,
                start = Offset(width, 0f),
                end = Offset(width, cornerLengthPx),
                strokeWidth = strokeWidth
        )

        // Bottom-left corner
        drawLine(
                color = color,
                start = Offset(0f, height - cornerLengthPx),
                end = Offset(0f, height),
                strokeWidth = strokeWidth
        )
        drawLine(
                color = color,
                start = Offset(0f, height),
                end = Offset(cornerLengthPx, height),
                strokeWidth = strokeWidth
        )

        // Bottom-right corner
        drawLine(
                color = color,
                start = Offset(width - cornerLengthPx, height),
                end = Offset(width, height),
                strokeWidth = strokeWidth
        )
        drawLine(
                color = color,
                start = Offset(width, height - cornerLengthPx),
                end = Offset(width, height),
                strokeWidth = strokeWidth
        )
    }
}

/** Focus indicator that appears when user taps on preview */
@Composable
fun FocusIndicator(offset: Offset, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(true) }

    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Canvas(modifier = modifier.offset(x = (offset.x - 35).dp, y = (offset.y - 35).dp)) {
            drawCircle(
                    color = Color.White,
                    radius = 35.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
