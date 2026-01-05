package com.nguyendevs.ecolens.managers

import android.content.Context
import android.util.Log
import com.nguyendevs.ecolens.database.HistoryRepository
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.model.HistorySortOption
import com.nguyendevs.ecolens.utils.ImageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class HistoryManager(
    private val context: Context,
    private val historyRepository: HistoryRepository
) {

    companion object {
        private const val TAG = "HistoryManager"
    }

    private val downloadingImages = ConcurrentHashMap<Int, Boolean>()

    fun getHistoryBySortOption(
        sortOption: HistorySortOption,
        startDate: Long? = null,
        endDate: Long? = null
    ): Flow<List<HistoryEntry>> {
        val flow = if (startDate != null && endDate != null) {
            when (sortOption) {
                HistorySortOption.NEWEST_FIRST -> historyRepository.getHistoryByDateRangeNewest(startDate, endDate)
                HistorySortOption.OLDEST_FIRST -> historyRepository.getHistoryByDateRangeOldest(startDate, endDate)
            }
        } else {
            when (sortOption) {
                HistorySortOption.NEWEST_FIRST -> historyRepository.getAllHistoryNewestFirst()
                HistorySortOption.OLDEST_FIRST -> historyRepository.getAllHistoryOldestFirst()
            }
        }

        return flow.flowOn(Dispatchers.IO)
    }

    suspend fun repairMissingImagesOnce() = withContext(Dispatchers.IO) {
        runCatching {
            val allEntries = historyRepository.getAllHistoryNewestFirst()

            Log.d(TAG, "Starting one-time image repair check...")

            val entries = mutableListOf<HistoryEntry>()
            allEntries.collect { list ->
                entries.addAll(list)
                return@collect
            }

            entries.forEach { entry ->
                launch {
                    repairSingleImage(entry)
                }
            }
        }.onFailure { e ->
            Log.e(TAG, "Error in repair: ${e.message}", e)
        }
    }

    private suspend fun repairSingleImage(entry: HistoryEntry) {
        if (downloadingImages.containsKey(entry.id)) {
            return
        }

        val localPath = entry.localImagePath
        val remoteUrl = entry.imagePath

        val fileExists = if (localPath.isNotEmpty()) File(localPath).exists() else false

        if (!fileExists && remoteUrl.startsWith("http")) {
            try {
                downloadingImages[entry.id] = true
                Log.d(TAG, "Downloading missing image for entry ${entry.id}...")

                val newLocalPath = ImageUtils.downloadImageToInternalStorage(context, remoteUrl)

                if (newLocalPath != null) {
                    val updatedEntry = entry.copy(localImagePath = newLocalPath)
                    historyRepository.update(updatedEntry)
                    Log.d(TAG, "Restored image: $newLocalPath")
                } else {
                    Log.e(TAG, "Failed to restore image for entry ${entry.id}")
                }
            } finally {
                downloadingImages.remove(entry.id)
            }
        }
    }

    suspend fun toggleFavorite(entry: HistoryEntry) {
        withContext(Dispatchers.IO) {
            runCatching {
                historyRepository.update(entry.copy(isFavorite = !entry.isFavorite))
            }.onFailure { e ->
                Log.e(TAG, "Error toggling favorite: ${e.message}", e)
            }
        }
    }

    suspend fun deleteHistory(entry: HistoryEntry) {
        withContext(Dispatchers.IO) {
            runCatching {
                // Delete local image file if exists
                if (entry.localImagePath.isNotEmpty()) {
                    val file = File(entry.localImagePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
                // Delete from repository (Local DB + Firebase)
                historyRepository.delete(entry)
            }.onFailure { e ->
                Log.e(TAG, "Error deleting history entry: ${e.message}", e)
            }
        }
    }

    suspend fun deleteAllHistory() {
        withContext(Dispatchers.IO) {
            runCatching {
                historyRepository.deleteAll()
                val dir = context.filesDir
                dir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("species_")) {
                        file.delete()
                    }
                }
            }.onFailure { e ->
                Log.e(TAG, "Error deleting history: ${e.message}", e)
            }
        }
    }
}