// ui/screens/history/HistoryScreen.kt
package com.example.ecolens.ui.screens.history

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ecolens.R
import com.example.ecolens.model.HistoryEntry
import com.example.ecolens.model.HistorySortOption
import com.example.ecolens.ui.theme.*
import com.example.ecolens.view.EcoLensViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: EcoLensViewModel,
    modifier: Modifier = Modifier
) {
    var sortOption by remember { mutableStateOf(HistorySortOption.NEWEST_FIRST) }
    var filterStartDate by remember { mutableStateOf<Long?>(null) }
    var filterEndDate by remember { mutableStateOf<Long?>(null) }
    var isOptionsExpanded by remember { mutableStateOf(false) }

    val historyList by viewModel.getHistoryBySortOption(
        sortOption,
        filterStartDate,
        filterEndDate
    ).collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(top = 10.dp, bottom = 1.dp)
        ) {
            Text(
                text = stringResource(R.string.history_title_label),
                style = MaterialTheme.typography.displayLarge,
                color = Primary,
                modifier = Modifier.padding(bottom = 15.dp)
            )

            HorizontalDivider(
                thickness = 1.5.dp,
                color = BorderNormal,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = (-20).dp, vertical = 10.dp)
            )

            // Options Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isOptionsExpanded = !isOptionsExpanded }
                    .padding(vertical = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.options),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = Spacing.xxs)
                )

                Icon(
                    painter = painterResource(R.drawable.ic_arrow_down),
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(IconSize.md)
                        .then(
                            if (isOptionsExpanded)
                                Modifier.graphicsLayer { rotationZ = 180f }
                            else Modifier
                        )
                )
            }

            // Options Container
            AnimatedVisibility(
                visible = isOptionsExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    // Sort Option
                    SortOptionCard(
                        currentSort = sortOption,
                        onSortChange = { sortOption = it }
                    )

                    // Date Filter Option
                    DateFilterCard(
                        startDate = filterStartDate,
                        endDate = filterEndDate,
                        onDateRangeSelected = { start, end ->
                            filterStartDate = start
                            filterEndDate = end
                        },
                        onClearFilter = {
                            filterStartDate = null
                            filterEndDate = null
                        }
                    )
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = BorderNormal,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xs)
            )
        }

        // Content
        if (historyList.isEmpty()) {
            EmptyHistoryState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.xl)
            )
        } else {
            HistoryList(
                historyList = historyList,
                onItemClick = { /* Navigate to detail */ },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.lg)
            )
        }
    }
}

@Composable
private fun SortOptionCard(
    currentSort: HistorySortOption,
    onSortChange: (HistorySortOption) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.none),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderNormal)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onSortChange(
                        if (currentSort == HistorySortOption.NEWEST_FIRST)
                            HistorySortOption.OLDEST_FIRST
                        else HistorySortOption.NEWEST_FIRST
                    )
                }
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(IconSize.xl)
                    .background(Color.Transparent, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_sort),
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(IconSize.md)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.md)
            ) {
                Text(
                    text = stringResource(R.string.sort_by),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (currentSort == HistorySortOption.NEWEST_FIRST)
                        stringResource(R.string.sort_newest_first)
                    else stringResource(R.string.sort_oldest_first),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SuccessText,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun DateFilterCard(
    startDate: Long?,
    endDate: Long?,
    onDateRangeSelected: (Long, Long) -> Unit,
    onClearFilter: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.none),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderNormal)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* Show date picker */ }
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(IconSize.xl)
                    .background(Color.Transparent, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_calendar),
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(IconSize.md)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.md)
            ) {
                Text(
                    text = stringResource(R.string.filter_by_date),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = if (startDate != null && endDate != null) {
                            "${dateFormatter.format(Date(startDate))} - ${dateFormatter.format(Date(endDate))}"
                        } else {
                            stringResource(R.string.select_date)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (startDate != null) Primary else TextSecondary,
                        modifier = Modifier.weight(1f)
                    )

                    if (startDate != null) {
                        IconButton(
                            onClick = onClearFilter,
                            modifier = Modifier.size(IconSize.sm)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.clear_filter),
                                tint = Error,
                                modifier = Modifier.size(IconSize.sm)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryList(
    historyList: List<HistoryEntry>,
    onItemClick: (HistoryEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(
            items = historyList,
            key = { it.id }
        ) { entry ->
            val index = historyList.indexOf(entry)
            val isFirstOfDay = index == 0 ||
                    !isSameDay(entry.timestamp, historyList[index - 1].timestamp)

            HistoryItem(
                entry = entry,
                showDateHeader = isFirstOfDay,
                onClick = { onItemClick(entry) }
            )
        }
    }
}

@Composable
private fun HistoryItem(
    entry: HistoryEntry,
    showDateHeader: Boolean,
    onClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (showDateHeader) {
            Text(
                text = dateFormatter.format(Date(entry.timestamp)),
                style = MaterialTheme.typography.bodyLarge,
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    start = Spacing.xxs,
                    top = 8.dp,
                    bottom = 8.dp
                )
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = Elevation.none),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderNormal)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image
                AsyncImage(
                    model = entry.imagePath,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .background(Background, RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                // Info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Spacing.md)
                ) {
                    Text(
                        text = entry.speciesInfo.commonName.ifEmpty {
                            stringResource(R.string.unknown_common_name)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = PrimaryDark,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Text(
                        text = entry.speciesInfo.scientificName.ifEmpty {
                            stringResource(R.string.unknown_scientific_name)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        maxLines = 1,
                        modifier = Modifier.padding(top = Spacing.xxs)
                    )

                    Text(
                        text = timeFormatter.format(Date(entry.timestamp)),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextTertiary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Arrow
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = stringResource(R.string.view_detail),
                    tint = TextSecondary,
                    modifier = Modifier.size(IconSize.md)
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_history),
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer { alpha = 0.3f }
        )

        Text(
            text = stringResource(R.string.history_empty),
            style = MaterialTheme.typography.headlineSmall,
            color = TextSecondary,
            modifier = Modifier.padding(top = Spacing.md)
        )

        Text(
            text = stringResource(R.string.history_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = Spacing.xs)
        )
    }
}

private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp1)) == formatter.format(Date(timestamp2))
}