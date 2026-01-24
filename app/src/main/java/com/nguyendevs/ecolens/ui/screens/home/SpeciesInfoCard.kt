package com.nguyendevs.ecolens.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.model.SpeciesInfo
import com.nguyendevs.ecolens.ui.components.EcoLensCard
import com.nguyendevs.ecolens.ui.components.EcoLensDivider
import com.nguyendevs.ecolens.ui.theme.Dimens
import com.nguyendevs.ecolens.ui.theme.EcoLensCorners
import com.nguyendevs.ecolens.ui.theme.EcoLensTextStyles
import com.nguyendevs.ecolens.ui.theme.EcoLensTheme
import com.nguyendevs.ecolens.ui.theme.Primary
import com.nguyendevs.ecolens.ui.theme.SurfaceVariant
import com.nguyendevs.ecolens.ui.theme.TextSecondary

/** Species information card. Replaces item_card_species_info.xml (456 lines) */
@Composable
fun SpeciesInfoCard(
        speciesInfo: SpeciesInfo,
        onCopyScientificName: (String) -> Unit,
        onRetryClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    EcoLensCard(modifier = modifier) {
        Column(modifier = Modifier.padding(Dimens.SpacingLg)) {
            // Header Section
            HeaderSection(
                    speciesInfo = speciesInfo,
                    onCopyScientificName = onCopyScientificName,
                    onRetryClick = onRetryClick
            )

            EcoLensDivider(modifier = Modifier.padding(vertical = Dimens.SpacingLg))

            // Taxonomy Section
            TaxonomySection(speciesInfo = speciesInfo)

            // Description Section
            AnimatedSection(
                    title = stringResource(R.string.section_description),
                    content = speciesInfo.description,
                    visible = !speciesInfo.description.isNullOrBlank()
            )

            // Characteristics Section
            AnimatedSection(
                    title = stringResource(R.string.section_characteristics),
                    content = speciesInfo.characteristics,
                    visible = !speciesInfo.characteristics.isNullOrBlank()
            )

            // Distribution Section
            AnimatedSection(
                    title = stringResource(R.string.section_distribution),
                    content = speciesInfo.distribution,
                    visible = !speciesInfo.distribution.isNullOrBlank()
            )

            // Habitat Section
            AnimatedSection(
                    title = stringResource(R.string.section_habitat),
                    content = speciesInfo.habitat,
                    visible = !speciesInfo.habitat.isNullOrBlank()
            )

            // Conservation Section
            AnimatedSection(
                    title = stringResource(R.string.section_conservation),
                    content = speciesInfo.conservationStatus,
                    visible = !speciesInfo.conservationStatus.isNullOrBlank()
            )
        }
    }
}

@Composable
private fun HeaderSection(
        speciesInfo: SpeciesInfo,
        onCopyScientificName: (String) -> Unit,
        onRetryClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        // Left: Name Section
        Column(modifier = Modifier.weight(1f)) {
            // Common Name
            Text(
                    text = speciesInfo.commonName,
                    style = EcoLensTextStyles.Headline1,
                    color = Primary
            )

            // Scientific Name with copy button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                        text = speciesInfo.scientificName,
                        style = EcoLensTextStyles.Body1,
                        color = TextSecondary,
                        fontStyle = FontStyle.Italic
                )

                IconButton(
                        onClick = { onCopyScientificName(speciesInfo.scientificName) },
                        modifier = Modifier.size(Dimens.IconMd)
                ) {
                    Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.copy_scientific_name),
                            tint = TextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Confidence Badge
            speciesInfo.confidence?.let { confidence ->
                Spacer(modifier = Modifier.height(Dimens.SpacingSm))
                ConfidenceBadge(confidence = confidence)
            }
        }

        // Right: Action Buttons (hidden by default in XML, keeping same behavior)
        // Share and Retry buttons - visibility="gone" in XML
    }
}

@Composable
private fun ConfidenceBadge(confidence: String) {
    val (bgColor, textColor, iconTint) =
            when {
                confidence.contains("High", ignoreCase = true) ||
                        confidence.contains("Cao", ignoreCase = true) ->
                        Triple(
                                EcoLensTheme.extendedColors.confidenceHighBg,
                                EcoLensTheme.extendedColors.successText,
                                EcoLensTheme.extendedColors.success
                        )
                confidence.contains("Medium", ignoreCase = true) ||
                        confidence.contains("Trung bình", ignoreCase = true) ->
                        Triple(
                                EcoLensTheme.extendedColors.confidenceMediumBg,
                                EcoLensTheme.extendedColors.warningText,
                                EcoLensTheme.extendedColors.warning
                        )
                else ->
                        Triple(
                                EcoLensTheme.extendedColors.confidenceLowBg,
                                MaterialTheme.colorScheme.error,
                                MaterialTheme.colorScheme.error
                        )
            }

    Box(
            modifier =
                    Modifier.clip(EcoLensCorners.ConfidenceBadge)
                            .background(bgColor)
                            .padding(horizontal = Dimens.SpacingSm, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(Dimens.SpacingXs))

            Text(
                    text = confidence,
                    style = EcoLensTextStyles.Label,
                    color = textColor,
                    fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun TaxonomySection(speciesInfo: SpeciesInfo) {
    Text(
            text = stringResource(R.string.taxonomy_title),
            style = EcoLensTextStyles.Headline3,
            modifier = Modifier.padding(bottom = Dimens.SpacingMd)
    )

    Column(
            modifier =
                    Modifier.fillMaxWidth()
                            .clip(EcoLensCorners.Card)
                            .background(SurfaceVariant)
                            .padding(20.dp)
    ) {
        TaxonomyRow(label = stringResource(R.string.label_kingdom), value = speciesInfo.kingdom)
        TaxonomyRow(label = stringResource(R.string.label_phylum), value = speciesInfo.phylum)
        TaxonomyRow(label = stringResource(R.string.label_class), value = speciesInfo.classname)
        TaxonomyRow(label = stringResource(R.string.label_order), value = speciesInfo.order)
        TaxonomyRow(label = stringResource(R.string.label_family), value = speciesInfo.family)
        TaxonomyRow(label = stringResource(R.string.label_genus), value = speciesInfo.genus)
        TaxonomyRow(
                label = stringResource(R.string.label_species),
                value = speciesInfo.species,
                showBottomPadding = false
        )
    }
}

@Composable
private fun TaxonomyRow(label: String, value: String?, showBottomPadding: Boolean = true) {
    if (!value.isNullOrBlank()) {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(bottom = if (showBottomPadding) Dimens.SpacingXs else 0.dp)
        ) {
            Text(
                    text = label,
                    style = EcoLensTextStyles.TaxonomyLabel,
                    modifier = Modifier.weight(0.3f)
            )
            Text(
                    text = value,
                    style = EcoLensTextStyles.TaxonomyValue,
                    modifier = Modifier.weight(0.7f)
            )
        }
    }
}

@Composable
private fun AnimatedSection(title: String, content: String?, visible: Boolean) {
    AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
    ) {
        Column(modifier = Modifier.padding(top = Dimens.SpacingLg)) {
            Text(text = title, style = EcoLensTextStyles.Headline3)

            EcoLensDivider(modifier = Modifier.padding(vertical = Dimens.SpacingXs))

            Text(
                    text = content ?: "",
                    style = EcoLensTextStyles.Body1,
                    color = TextSecondary,
                    lineHeight = 24.sp
            )
        }
    }
}
