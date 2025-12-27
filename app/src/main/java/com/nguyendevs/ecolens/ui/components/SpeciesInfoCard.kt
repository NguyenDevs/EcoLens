// ui/components/SpeciesInfoCard.kt
package com.example.ecolens.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nguyendevs.ecolens.ui.screens.home.SpeciesInfo
import com.example.ecolens.ui.theme.*
import com.nguyendevs.ecolens.ui.screens.home.Taxonomy

@Composable
fun SpeciesInfoCard(
    speciesInfo: SpeciesInfo,
    onCopyScientificName: () -> Unit,
    onShare: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.none),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderNormal)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            // Header Section
            HeaderSection(
                commonName = speciesInfo.commonName,
                scientificName = speciesInfo.scientificName,
                confidence = speciesInfo.confidence,
                onCopyScientificName = onCopyScientificName,
                onShare = onShare,
                onRetry = onRetry
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Spacing.lg),
                thickness = 1.dp,
                color = BorderLight
            )

            // Taxonomy Section
            TaxonomySection(
                taxonomy = speciesInfo.taxonomy
            )

            // Description Sections
            speciesInfo.description?.let { description ->
                InfoSection(
                    title = stringResource(R.string.section_description),
                    content = description,
                    modifier = Modifier.padding(top = Spacing.lg)
                )
            }

            speciesInfo.characteristics?.let { characteristics ->
                InfoSection(
                    title = stringResource(R.string.section_characteristics),
                    content = characteristics,
                    modifier = Modifier.padding(top = Spacing.lg)
                )
            }

            speciesInfo.distribution?.let { distribution ->
                InfoSection(
                    title = stringResource(R.string.section_distribution),
                    content = distribution,
                    modifier = Modifier.padding(top = Spacing.lg)
                )
            }

            speciesInfo.habitat?.let { habitat ->
                InfoSection(
                    title = stringResource(R.string.section_habitat),
                    content = habitat,
                    modifier = Modifier.padding(top = Spacing.lg)
                )
            }

            speciesInfo.conservation?.let { conservation ->
                InfoSection(
                    title = stringResource(R.string.section_conservation),
                    content = conservation,
                    modifier = Modifier.padding(top = Spacing.lg)
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    commonName: String,
    scientificName: String,
    confidence: Float,
    onCopyScientificName: () -> Unit,
    onShare: () -> Unit,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Name Section
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = commonName,
                style = MaterialTheme.typography.headlineLarge,
                color = Primary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = Spacing.xxs)
            ) {
                Text(
                    text = scientificName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    color = TextSecondary
                )

                IconButton(
                    onClick = onCopyScientificName,
                    modifier = Modifier.size(IconSize.md)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_copy),
                        contentDescription = stringResource(R.string.copy_scientific_name),
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(IconSize.sm)
                            .alpha(0.6f)
                    )
                }
            }

            // Confidence Badge
            ConfidenceBadge(
                confidence = confidence,
                modifier = Modifier.padding(top = Spacing.sm)
            )
        }

        // Right: Action Buttons
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            ActionButton(
                icon = R.drawable.ic_share,
                contentDescription = stringResource(R.string.share_info),
                onClick = onShare,
                tint = Primary
            )

            ActionButton(
                icon = R.drawable.ic_rotate,
                contentDescription = stringResource(R.string.btn_retry_identification),
                onClick = onRetry,
                tint = Warning
            )
        }
    }
}

@Composable
private fun ConfidenceBadge(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, iconColor) = when {
        confidence >= 0.8f -> Triple(SuccessBg, SuccessText, Success)
        confidence >= 0.5f -> Triple(WarningBg, WarningText, Warning)
        else -> Triple(ErrorBg, ErrorText, Error)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.none)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = Spacing.sm, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = stringResource(R.string.confidence),
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = stringResource(R.string.confidence_format, (confidence * 100).toInt()),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = textColor
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(IconSize.xl)
            .background(SurfaceTint, RoundedCornerShape(12.dp))
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(IconSize.md)
        )
    }
}

@Composable
private fun TaxonomySection(
    taxonomy: Taxonomy
) {
    Text(
        text = stringResource(R.string.taxonomy_title),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(bottom = Spacing.md)
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.none)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            taxonomy.kingdom?.let { TaxonomyRow(stringResource(R.string.label_kingdom), it) }
            taxonomy.phylum?.let { TaxonomyRow(stringResource(R.string.label_phylum), it) }
            taxonomy.classValue?.let { TaxonomyRow(stringResource(R.string.label_class), it) }
            taxonomy.order?.let { TaxonomyRow(stringResource(R.string.label_order), it) }
            taxonomy.family?.let { TaxonomyRow(stringResource(R.string.label_family), it) }
            taxonomy.genus?.let { TaxonomyRow(stringResource(R.string.label_genus), it) }
            taxonomy.species?.let {
                TaxonomyRow(
                    stringResource(R.string.label_species),
                    it,
                    isLast = true
                )
            }
        }
    }
}

@Composable
private fun TaxonomyRow(
    label: String,
    value: String,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLast) 0.dp else Spacing.xs)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(0.3f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            modifier = Modifier.weight(0.7f)
        )
    }
}

@Composable
private fun InfoSection(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = Spacing.xs),
            thickness = 1.dp,
            color = BorderLight
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 24.sp
                ),
                color = TextSecondary
            )
        }
    }
}