package com.nguyendevs.ecolens.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.ui.components.EcoLensCard
import com.nguyendevs.ecolens.ui.components.EcoLensDivider
import com.nguyendevs.ecolens.ui.theme.Dimens
import com.nguyendevs.ecolens.ui.theme.EcoLensCorners
import com.nguyendevs.ecolens.ui.theme.EcoLensTextStyles
import com.nguyendevs.ecolens.ui.theme.Primary
import com.nguyendevs.ecolens.ui.theme.TextSecondary

/** Data class representing a history item. */
data class HistoryItemData(
        val id: String,
        val commonName: String,
        val scientificName: String,
        val confidence: String?,
        val timestamp: String,
        val imageUri: String?
)

/** History screen composable. Replaces screen_species_history.xml */
@Composable
fun HistoryScreen(
        items: List<HistoryItemData>,
        onItemClick: (HistoryItemData) -> Unit,
        onDeleteClick: (HistoryItemData) -> Unit,
        modifier: Modifier = Modifier
) {
    Column(
            modifier =
                    modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(top = 10.dp)
    ) {
        // Header
        Text(
                text = stringResource(R.string.history_title),
                style = EcoLensTextStyles.Display,
                color = Primary,
                modifier =
                        Modifier.padding(
                                start = Dimens.PaddingScreenHorizontal,
                                end = Dimens.PaddingScreenHorizontal,
                                bottom = 15.dp
                        )
        )

        EcoLensDivider()

        if (items.isEmpty()) {
            // Empty state
            EmptyHistoryState(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                    modifier =
                            Modifier.fillMaxSize()
                                    .padding(
                                            horizontal = Dimens.PaddingScreenHorizontal,
                                            vertical = Dimens.SpacingMd
                                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
            ) {
                items(items = items, key = { it.id }) { item ->
                    HistoryItem(
                            item = item,
                            onClick = { onItemClick(item) },
                            onDeleteClick = { onDeleteClick(item) }
                    )
                }
            }
        }
    }
}

/** Single history item composable. */
@Composable
fun HistoryItem(
        item: HistoryItemData,
        onClick: () -> Unit,
        onDeleteClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    EcoLensCard(modifier = modifier, onClick = onClick) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(Dimens.SpacingMd),
                verticalAlignment = Alignment.CenterVertically
        ) {
            // Image thumbnail
            Box(
                    modifier =
                            Modifier.size(70.dp)
                                    .clip(EcoLensCorners.Card)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (item.imageUri != null) {
                    AsyncImage(
                            model = item.imageUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(32.dp).align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(Dimens.SpacingMd))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = item.commonName,
                        style = EcoLensTextStyles.Headline3,
                        color = Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )

                Text(
                        text = item.scientificName,
                        style = EcoLensTextStyles.Body2,
                        color = TextSecondary,
                        fontStyle = FontStyle.Italic,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(Dimens.SpacingXs))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.confidence?.let { conf ->
                        ConfidenceChip(confidence = conf)
                        Spacer(modifier = Modifier.width(Dimens.SpacingSm))
                    }

                    Text(
                            text = item.timestamp,
                            style = EcoLensTextStyles.Caption,
                            color = TextSecondary
                    )
                }
            }

            // Delete button
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(Dimens.TouchTargetMin)) {
                Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(Dimens.IconMd)
                )
            }
        }
    }
}

@Composable
private fun ConfidenceChip(confidence: String) {
    val (bgColor, textColor) =
            when {
                confidence.contains("High", ignoreCase = true) ||
                        confidence.contains("Cao", ignoreCase = true) ->
                        Pair(Color(0xFFE8F5E9), Color(0xFF00796B))
                confidence.contains("Medium", ignoreCase = true) ||
                        confidence.contains("Trung bình", ignoreCase = true) ->
                        Pair(Color(0xFFFFF3E0), Color(0xFFE65100))
                else -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
            }

    Box(
            modifier =
                    Modifier.clip(RoundedCornerShape(50))
                            .background(bgColor)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
                text = confidence,
                style = EcoLensTextStyles.Caption,
                color = textColor,
                fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyHistoryState(modifier: Modifier = Modifier) {
    Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingLg))

        Text(
                text = stringResource(R.string.empty_history_title),
                style = EcoLensTextStyles.Headline2,
                color = TextSecondary
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingSm))

        Text(
                text = stringResource(R.string.empty_history_message),
                style = EcoLensTextStyles.Body2,
                color = TextSecondary.copy(alpha = 0.7f)
        )
    }
}
