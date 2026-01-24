package com.nguyendevs.ecolens.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.ui.theme.Dimens

/**
 * Full screen image viewer with zoom/pan support. Replaces the fullscreen viewer in
 * activity_main.xml
 */
@Composable
fun FullScreenImageViewer(
        imageUri: Uri?,
        visible: Boolean,
        onCloseClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    AnimatedVisibility(
            visible = visible && imageUri != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = modifier
    ) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        Box(
                modifier =
                        Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)

                                if (scale > 1f) {
                                    val maxX = (scale - 1f) * size.width / 2f
                                    val maxY = (scale - 1f) * size.height / 2f
                                    offset =
                                            Offset(
                                                    x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                                    y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                            )
                                } else {
                                    offset = Offset.Zero
                                }
                            }
                        }
        ) {
            // Image with zoom/pan
            AsyncImage(
                    model = imageUri,
                    contentDescription = stringResource(R.string.full_image),
                    contentScale = ContentScale.Fit,
                    modifier =
                            Modifier.fillMaxSize()
                                    .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offset.x,
                                            translationY = offset.y
                                    )
            )

            // Close button
            IconButton(
                    onClick = onCloseClick,
                    modifier =
                            Modifier.align(Alignment.TopEnd)
                                    .padding(Dimens.SpacingLg)
                                    .size(Dimens.TouchTargetMin)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
