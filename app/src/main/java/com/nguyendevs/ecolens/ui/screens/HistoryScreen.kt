package com.nguyendevs.ecolens.ui.screens

import android.widget.TextView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.model.HistorySortOption
import com.nguyendevs.ecolens.ui.theme.*
import com.nguyendevs.ecolens.view.EcoLensViewModel
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: EcoLensViewModel,
    onItemClick: (HistoryEntry) -> Unit
) {
    var isOptionsExpanded by remember { mutableStateOf(false) }
    var currentSortOption by remember { mutableStateOf(HistorySortOption.NEWEST_FIRST) }
    var filterStartDate by remember { mutableStateOf<Long?>(null) }
    var filterEndDate by remember { mutableStateOf<Long?>(null) }

    // Trigger load history logic from ViewModel
    val historyList by viewModel.getHistoryBySortOption(currentSortOption, filterStartDate, filterEndDate)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Date Picker State
    var showDateRangePicker by remember { mutableStateOf(false) }
    if (showDateRangePicker) {
        val datePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    filterStartDate = datePickerState.selectedStartDateMillis
                    filterEndDate = datePickerState.selectedEndDateMillis
                    showDateRangePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) { Text("Hủy") }
            }
        ) {
            DateRangePicker(state = datePickerState)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        // Header / Options Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .clickable { isOptionsExpanded = !isOptionsExpanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Lịch sử nhận diện", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Icon(
                imageVector = if (isOptionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = "Expand Options"
            )
        }

        // Expandable Options Area
        AnimatedVisibility(visible = isOptionsExpanded) {
            Column(modifier = Modifier.background(Surface).padding(16.dp)) {
                // Sort Button
                OutlinedButton(
                    onClick = {
                        currentSortOption = if (currentSortOption == HistorySortOption.NEWEST_FIRST)
                            HistorySortOption.OLDEST_FIRST else HistorySortOption.NEWEST_FIRST
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_sort), contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (currentSortOption == HistorySortOption.NEWEST_FIRST) "Mới nhất trước" else "Cũ nhất trước")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Button
                OutlinedButton(
                    onClick = { showDateRangePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_calendar), contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val text = if (filterStartDate != null && filterEndDate != null)
                        "${dateFormatter.format(filterStartDate)} - ${dateFormatter.format(filterEndDate)}"
                    else "Lọc theo ngày"
                    Text(text)
                }

                if (filterStartDate != null) {
                    TextButton(
                        onClick = { filterStartDate = null; filterEndDate = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Xóa bộ lọc", color = Error)
                    }
                }
            }
        }

        // List Content
        if (historyList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_history),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = TextSecondary
                    )
                    Text("Chưa có lịch sử", color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyList) { entry ->
                    HistoryItem(entry = entry, onClick = { onItemClick(entry) })
                }
            }
        }
    }
}

@Composable
fun HistoryItem(entry: HistoryEntry, onClick: () -> Unit) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Markwon setup for rendering markdown in TextView (keeping logic)
    val markwon = remember { Markwon.builder(context).usePlugin(HtmlPlugin.create()).build() }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
            AsyncImage(
                model = entry.imagePath,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Header Row (Date & Time)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = dateFormatter.format(Date(entry.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary
                    )
                    Text(
                        text = timeFormatter.format(Date(entry.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Common Name (Rendered Markdown/HTML)
                AndroidView(
                    factory = { ctx ->
                        TextView(ctx).apply {
                            setTextColor(android.graphics.Color.BLACK)
                            textSize = 16f
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                    },
                    update = { tv ->
                        markwon.setMarkdown(tv, entry.speciesInfo.commonName.ifEmpty { "Không xác định" })
                    }
                )

                // Scientific Name
                AndroidView(
                    factory = { ctx ->
                        TextView(ctx).apply {
                            setTextColor(android.graphics.Color.GRAY)
                            textSize = 14f
                            typeface = android.graphics.Typeface.defaultFromStyle(android.graphics.Typeface.ITALIC)
                        }
                    },
                    update = { tv ->
                        markwon.setMarkdown(tv, entry.speciesInfo.scientificName)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberDateRangePickerState() = androidx.compose.material3.rememberDateRangePickerState()