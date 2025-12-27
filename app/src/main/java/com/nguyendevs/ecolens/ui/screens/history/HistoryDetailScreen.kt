// ui/screens/history/HistoryDetailScreen.kt
package com.nguyendevs.ecolens.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.ui.components.SpeciesInfoCard
import com.nguyendevs.ecolens.ui.theme.*
import com.nguyendevs.ecolens.ui.utils.copyToClipboard
import com.nguyendevs.ecolens.ui.utils.shareTextWithImage
import com.nguyendevs.ecolens.view.EcoLensViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun HistoryDetailScreen(
    entryId: Int,
    viewModel: EcoLensViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var historyEntry by remember { mutableStateOf<HistoryEntry?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(entryId) {
        // Load history entry from database
        scope.launch {
            // This is a placeholder - you need to add getHistoryById to ViewModel
            // historyEntry = viewModel.getHistoryById(entryId).first()
        }
    }

    historyEntry?.let { entry ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Image Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    AsyncImage(
                        model = entry.imagePath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.6f)
                                    )
                                )
                            )
                    )

                    // Back button
                    FloatingActionButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(Spacing.md)
                            .size(40.dp),
                        containerColor = Color.Transparent,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = (-24).dp)
                        .background(
                            Color.White,
                            RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 80.dp)
                ) {
                    // Header Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = entry.speciesInfo.commonName,
                                style = MaterialTheme.typography.headlineLarge,
                                color = PrimaryDark
                            )

                            Text(
                                text = entry.speciesInfo.scientificName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontStyle = FontStyle.Italic,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = Spacing.xxs)
                            )
                        }

                        // Share button
                        IconButton(
                            onClick = {
                                // Build share text
                                val shareText = buildShareText(context, entry)
                                context.shareTextWithImage(
                                    text = shareText,
                                    imageUri = android.net.Uri.parse(entry.imagePath)
                                )
                            },
                            modifier = Modifier
                                .size(IconSize.xl)
                                .background(SurfaceTint, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_share),
                                contentDescription = stringResource(R.string.share_info),
                                tint = Primary,
                                modifier = Modifier.size(IconSize.md)
                            )
                        }
                    }

                    // Tags
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        if (entry.speciesInfo.kingdom.isNotEmpty()) {
                            Tag(
                                text = entry.speciesInfo.kingdom,
                                backgroundColor = SuccessBg,
                                textColor = SuccessText
                            )
                        }

                        if (entry.speciesInfo.family.isNotEmpty()) {
                            Tag(
                                text = entry.speciesInfo.family,
                                backgroundColor = BorderNormal,
                                textColor = TextSecondary
                            )
                        }

                        if (entry.speciesInfo.species.isNotEmpty()) {
                            Tag(
                                text = entry.speciesInfo.species,
                                backgroundColor = BorderNormal,
                                textColor = TextSecondary
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Spacing.lg),
                        thickness = 1.dp,
                        color = BorderLight
                    )

                    // Taxonomy
                    Text(
                        text = stringResource(R.string.taxonomy_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = Spacing.sm)
                    )

                    TaxonomyGrid(
                        taxonomy = entry.speciesInfo,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Detail sections
                    if (entry.speciesInfo.description.isNotEmpty()) {
                        DetailSection(
                            title = stringResource(R.string.section_description),
                            content = entry.speciesInfo.description,
                            modifier = Modifier.padding(top = Spacing.lg)
                        )
                    }

                    if (entry.speciesInfo.characteristics.isNotEmpty()) {
                        DetailSection(
                            title = stringResource(R.string.section_characteristics),
                            content = entry.speciesInfo.characteristics,
                            modifier = Modifier.padding(top = Spacing.lg)
                        )
                    }

                    if (entry.speciesInfo.distribution.isNotEmpty()) {
                        DetailSection(
                            title = stringResource(R.string.section_distribution),
                            content = entry.speciesInfo.distribution,
                            modifier = Modifier.padding(top = Spacing.lg)
                        )
                    }

                    if (entry.speciesInfo.habitat.isNotEmpty()) {
                        DetailSection(
                            title = stringResource(R.string.section_habitat),
                            content = entry.speciesInfo.habitat,
                            modifier = Modifier.padding(top = Spacing.lg)
                        )
                    }

                    if (entry.speciesInfo.conservationStatus.isNotEmpty()) {
                        DetailSection(
                            title = stringResource(R.string.section_conservation),
                            content = entry.speciesInfo.conservationStatus,
                            modifier = Modifier.padding(top = Spacing.lg)
                        )
                    }
                }
            }

            // Speak FAB
            FloatingActionButton(
                onClick = { /* Handle TTS */ },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.lg),
                containerColor = Primary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 13.dp,
                    pressedElevation = Elevation.xl
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_speak),
                    contentDescription = stringResource(R.string.speak),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun Tag(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            modifier = Modifier.padding(
                horizontal = Spacing.sm,
                vertical = 6.dp
            )
        )
    }
}

@Composable
private fun TaxonomyGrid(
    taxonomy: com.nguyendevs.ecolens.model.SpeciesInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
            if (taxonomy.kingdom.isNotEmpty()) {
                TaxonomyRow(
                    label = stringResource(R.string.label_kingdom),
                    value = taxonomy.kingdom
                )
            }
            if (taxonomy.phylum.isNotEmpty()) {
                TaxonomyRow(
                    label = stringResource(R.string.label_phylum),
                    value = taxonomy.phylum
                )
            }
            if (taxonomy.className.isNotEmpty()) {
                TaxonomyRow(
                    label = stringResource(R.string.label_class),
                    value = taxonomy.className
                )
            }
            if (taxonomy.taxorder.isNotEmpty()) {
                TaxonomyRow(
                    label = stringResource(R.string.label_order),
                    value = taxonomy.taxorder
                )
            }
            if (taxonomy.family.isNotEmpty()) {
                TaxonomyRow(
                    label = stringResource(R.string.label_family),
                    value = taxonomy.family
                )
            }
            if (taxonomy.genus.isNotEmpty()) {
                TaxonomyRow(
                    label = stringResource(R.string.label_genus),
                    value = taxonomy.genus
                )
            }
            if (taxonomy.species.isNotEmpty()) {
                TaxonomyRow(
                    label = stringResource(R.string.label_species),
                    value = taxonomy.species,
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
private fun DetailSection(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
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

        Text(
            text = content,
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 24.sp
            ),
            color = TextSecondary
        )
    }
}

private fun buildShareText(
    context: android.content.Context,
    entry: HistoryEntry
): String {
    val info = entry.speciesInfo
    val confidencePercent = String.format(
        "%.2f",
        if (info.confidence > 1) info.confidence else info.confidence * 100
    )

    return buildString {
        append(context.getString(R.string.share_title))
        append("\n━━━━━━━━━━━━━━━━━━━━\n\n")
        append("📌 ${info.commonName}\n🔬 ${info.scientificName}\n")
        append("✅ ${context.getString(R.string.label_confidence_template, confidencePercent)}%\n\n")

        append("━━━━━━━━━━━━━━━━━━━━\n${context.getString(R.string.share_taxonomy_title)}\n━━━━━━━━━━━━━━━━━━━━\n\n")
        if (info.kingdom.isNotEmpty()) append("• ${context.getString(R.string.label_kingdom)} ${info.kingdom}\n")
        if (info.phylum.isNotEmpty()) append("• ${context.getString(R.string.label_phylum)} ${info.phylum}\n")
        if (info.className.isNotEmpty()) append("• ${context.getString(R.string.label_class)} ${info.className}\n")
        if (info.taxorder.isNotEmpty()) append("• ${context.getString(R.string.label_order)} ${info.taxorder}\n")
        if (info.family.isNotEmpty()) append("• ${context.getString(R.string.label_family)} ${info.family}\n")
        if (info.genus.isNotEmpty()) append("• ${context.getString(R.string.label_genus)} ${info.genus}\n")
        if (info.species.isNotEmpty()) append("• ${context.getString(R.string.label_species)} ${info.species}\n")

        if (info.description.isNotEmpty()) {
            append("\n━━━━━━━━━━━━━━━━━━━━\n${context.getString(R.string.share_desc_title)}\n━━━━━━━━━━━━━━━━━━━━\n\n${info.description}\n")
        }

        append("\n━━━━━━━━━━━━━━━━━━━━\n${context.getString(R.string.share_footer)}")
    }
}