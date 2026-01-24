package com.nguyendevs.ecolens.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Shape definitions for EcoLens. Converted from res/values/styles.xml shape appearances. */
val EcoLensShapes =
        Shapes(
                // Extra small - for chips, small badges
                extraSmall = RoundedCornerShape(4.dp),

                // Small - for buttons
                small = RoundedCornerShape(8.dp),

                // Medium - for cards, inputs
                medium = RoundedCornerShape(12.dp),

                // Large - for larger cards, dialogs
                large = RoundedCornerShape(16.dp),

                // Extra large - for bottom sheets, large containers
                extraLarge = RoundedCornerShape(24.dp)
        )

/** Custom shape values for specific components. */
object EcoLensCorners {
    // From dimens.xml
    val RadiusXs = 4.dp
    val RadiusSm = 8.dp
    val RadiusMd = 12.dp
    val RadiusLg = 16.dp
    val RadiusXl = 20.dp
    val RadiusXxl = 24.dp
    val RadiusFull = 50.dp // Used for pill shapes

    // Specific component shapes
    val Button = RoundedCornerShape(28.dp)
    val Card = RoundedCornerShape(12.dp)
    val CardLarge = RoundedCornerShape(16.dp)
    val Input = RoundedCornerShape(12.dp)
    val Chip = RoundedCornerShape(20.dp)
    val BottomSheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val Fab = RoundedCornerShape(percent = 40)
    val ImagePreview = RoundedCornerShape(12.dp)
    val ConfidenceBadge = RoundedCornerShape(50.dp)
}
