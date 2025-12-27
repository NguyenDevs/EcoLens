package com.nguyendevs.ecolens.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nguyendevs.ecolens.ui.theme.*

// Loading indicator với dots animation
@Composable
fun LoadingIndicator(
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
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )

        AnimatedDots()
    }
}

@Composable
private fun AnimatedDots() {
    var dotCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            dotCount = (dotCount + 1) % 4
        }
    }

    Text(
        text = ".".repeat(dotCount),
        style = MaterialTheme.typography.bodyLarge,
        color = Primary,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
    )
}

// Shimmer effect for loading
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val shimmerWidth = width * 0.5f
        val offset = width * (shimmerOffset - 0.3f)

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFECEFF1),
                    Color(0xFFF5F7F9),
                    Color(0xFFF8F9FB),
                    Color(0xFFFAFBFC),
                    Color(0xFFF8F9FB),
                    Color(0xFFF5F7F9),
                    Color(0xFFECEFF1)
                ),
                start = Offset(offset, offset),
                end = Offset(offset + shimmerWidth, offset + shimmerWidth)
            )
        )
    }
}

// Pulsing animation for icons
@Composable
fun PulsingIcon(
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        icon()
    }
}

// Rotating animation
@Composable
fun RotatingIcon(
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier.graphicsLayer {
            rotationZ = rotation
        }
    ) {
        icon()
    }
}

// Gradient divider
@Composable
fun GradientDivider(
    modifier: Modifier = Modifier,
    startColor: Color = Primary,
    endColor: Color = Color.Transparent
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(startColor, endColor)
            )
        )
    }
}

// Empty state component
@Composable
fun EmptyState(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.graphicsLayer { alpha = 0.3f }) {
            icon()
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = TextSecondary,
            modifier = Modifier.padding(top = Spacing.md)
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = Spacing.xs)
        )
    }
}