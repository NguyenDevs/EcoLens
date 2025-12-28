package com.nguyendevs.ecolens.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nguyendevs.ecolens.model.HistorySortOption
import com.nguyendevs.ecolens.view.EcoLensViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    navController: NavController,
    historyId: Int,
    viewModel: EcoLensViewModel = viewModel()
) {
    // In a real app, you'd fetch the specific entry by ID.
    // For simplicity, we'll just grab the list and find it.
    val historyList by viewModel.getHistoryBySortOption(HistorySortOption.NEWEST_FIRST).collectAsState(initial = emptyList())
    val entry = historyList.find { it.id == historyId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(entry?.speciesInfo?.commonName ?: "Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (entry != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                AsyncImage(
                    model = entry.imagePath,
                    contentDescription = entry.speciesInfo.commonName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentScale = ContentScale.Crop
                )
                
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = entry.speciesInfo.commonName,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = entry.speciesInfo.scientificName,
                        style = MaterialTheme.typography.titleMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(text = "Description", style = MaterialTheme.typography.titleMedium)
                    Text(text = entry.speciesInfo.description)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(text = "Characteristics", style = MaterialTheme.typography.titleMedium)
                    Text(text = entry.speciesInfo.characteristics)
                    
                    // Add more fields as needed
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                Text("Entry not found", modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
            }
        }
    }
}