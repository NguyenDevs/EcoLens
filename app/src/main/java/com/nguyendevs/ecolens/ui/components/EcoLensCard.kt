package com.nguyendevs.ecolens.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.nguyendevs.ecolens.ui.theme.BorderNormal
import com.nguyendevs.ecolens.ui.theme.Dimens
import com.nguyendevs.ecolens.ui.theme.EcoLensCorners

/**
 * EcoLens styled card component. Matches Widget.App.Card from styles.xml
 *
 * @param modifier Modifier for the card
 * @param shape Shape of the card corners
 * @param elevation Card elevation
 * @param containerColor Background color of the card
 * @param borderColor Border color (set to Color.Transparent to hide)
 * @param content Card content
 */
@Composable
fun EcoLensCard(
        modifier: Modifier = Modifier,
        shape: Shape = EcoLensCorners.Card,
        elevation: Dp = Dimens.ElevationNone,
        containerColor: Color = MaterialTheme.colorScheme.surface,
        borderColor: Color = BorderNormal,
        onClick: (() -> Unit)? = null,
        content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
                onClick = onClick,
                modifier = modifier.fillMaxWidth(),
                shape = shape,
                colors = CardDefaults.cardColors(containerColor = containerColor),
                elevation = CardDefaults.cardElevation(defaultElevation = elevation),
                border =
                        if (borderColor != Color.Transparent) {
                            BorderStroke(Dimens.BorderNormal, borderColor)
                        } else null,
                content = content
        )
    } else {
        Card(
                modifier = modifier.fillMaxWidth(),
                shape = shape,
                colors = CardDefaults.cardColors(containerColor = containerColor),
                elevation = CardDefaults.cardElevation(defaultElevation = elevation),
                border =
                        if (borderColor != Color.Transparent) {
                            BorderStroke(Dimens.BorderNormal, borderColor)
                        } else null,
                content = content
        )
    }
}

/** Elevated card variant for EcoLens. Matches Widget.App.Card.Elevated from styles.xml */
@Composable
fun EcoLensElevatedCard(
        modifier: Modifier = Modifier,
        shape: Shape = EcoLensCorners.Card,
        containerColor: Color = MaterialTheme.colorScheme.surface,
        onClick: (() -> Unit)? = null,
        content: @Composable ColumnScope.() -> Unit
) {
    EcoLensCard(
            modifier = modifier,
            shape = shape,
            elevation = Dimens.ElevationSm,
            containerColor = containerColor,
            borderColor = Color.Transparent,
            onClick = onClick,
            content = content
    )
}
