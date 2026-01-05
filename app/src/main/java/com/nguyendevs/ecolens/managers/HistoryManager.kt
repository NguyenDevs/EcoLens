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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HistoryManager(
    private val context: Context,
    private val historyRepository: HistoryRepository
) {

    companion object {
        private const val TAG = "HistoryManager"
    }

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
        return flow
            .onEach { list ->
                // FIX: Chạy việc check/tải ảnh ở background scope riêng
                // Không chặn flow emit dữ liệu -> UI hiển thị ngay lập tức
                CoroutineScope(Dispatchers.IO).launch {
                    checkAndRepairImages(list)
                }
            }
            .flowOn(Dispatchers.IO)
    }

    private suspend fun checkAndRepairImages(entries: List<HistoryEntry>) {
        withContext(Dispatchers.IO) {
            entries.forEach { entry ->
                val localPath = entry.localImagePath
                val remoteUrl = entry.imagePath
                val fileExists = if (localPath.isNotEmpty()) File(localPath).exists() else false

                if (!fileExists && remoteUrl.startsWith("http")) {
                    Log.d(TAG, "Restoring image for entry ${entry.id} from Firebase")
                    // Hàm này trong ImageUtils đã lưu vào context.filesDir (Internal Storage)
                    val newLocalPath = ImageUtils.downloadImageToInternalStorage(context, remoteUrl)

                    if (newLocalPath != null) {
                        val updatedEntry = entry.copy(localImagePath = newLocalPath)
                        historyRepository.update(updatedEntry)
                        Log.d(TAG, "Restored successfully: $newLocalPath")
                    } else {
                        Log.e(TAG, "Failed to restore image for entry ${entry.id}")
                    }
                }
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