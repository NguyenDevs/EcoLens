package com.nguyendevs.ecolens.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Typography styles for EcoLens. Converted from res/values/styles.xml */
val EcoLensTypography =
        Typography(
                // Display - 28sp, medium weight
                displaySmall =
                        TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 28.sp,
                                lineHeight = 36.sp
                        ),

                // Headline1 - 22sp, bold
                headlineLarge =
                        TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                lineHeight = 28.sp
                        ),

                // Headline2 - 18sp, bold
                headlineMedium =
                        TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                lineHeight = 24.sp
                        ),

                // Headline3 - 16sp, bold
                headlineSmall =
                        TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                lineHeight = 22.sp
                        ),

                // Body1 - 15sp, normal with extra line spacing
                bodyLarge =
                        TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 15.sp,
                                lineHeight = 24.sp
                        ),

                // Body2 - 14sp, normal
                bodyMedium =
                        TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                        ),

                // Label - 12sp, bold, uppercase
                labelLarge =
                        TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                        ),

                // Caption - 11sp, normal
                labelSmall =
                        TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                        ),

                // Button text - 15sp, bold
                labelMedium =
                        TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                lineHeight = 20.sp
                        ),

                // Title - 16sp, medium
                titleLarge =
                        TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                lineHeight = 24.sp
                        ),
                titleMedium =
                        TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                        ),
                titleSmall =
                        TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                        ),
                bodySmall =
                        TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                        )
        )

/** Custom text styles that don't map directly to Material3 Typography. */
object EcoLensTextStyles {
    val Display =
            TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 28.sp,
                    lineHeight = 36.sp
            )

    val Headline1 =
            TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 28.sp
            )

    val Headline2 =
            TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 24.sp
            )

    val Headline3 =
            TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
            )

    val Body1 =
            TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    lineHeight = 24.sp
            )

    val Body2 =
            TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
            )

    val Label =
            TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
            )

    val Caption =
            TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
            )

    val Button =
            TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
            )

    val TaxonomyLabel =
            TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextSecondary
            )

    val TaxonomyValue =
            TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextPrimary
            )
}
