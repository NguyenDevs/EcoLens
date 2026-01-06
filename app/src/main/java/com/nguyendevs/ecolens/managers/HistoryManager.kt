package com.nguyendevs.ecolens.managers

import android.content.Context
import android.util.Log
import com.nguyendevs.ecolens.database.HistoryRepository
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.model.HistorySortOption
import com.nguyendevs.ecolens.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Manager quản lý lịch sử nhận diện loài
 * Hỗ trợ sort, filter, repair missing images, và toggle favorite
 */
class HistoryManager(
    private val context: Context,
    private val historyRepository: HistoryRepository
) {

    companion object {
        private const val TAG = "HistoryManager"
        private const val IMAGE_PREFIX = "species_"
    }

    private val downloadingImages = ConcurrentHashMap<Int, Boolean>()

    // ==================== QUERY METHODS ====================

    /**
     * Lấy lịch sử theo sort option và date range (optional)
     * @param sortOption Sort mới nhất hoặc cũ nhất
     * @param startDate Start date của range filter (optional)
     * @param endDate End date của range filter (optional)
     * @return Flow của danh sách history entries
     */
    fun getHistoryBySortOption(
        sortOption: HistorySortOption,
        startDate: Long? = null,
        endDate: Long? = null
    ): Flow<List<HistoryEntry>> {
        val flow = if (startDate != null && endDate != null) {
            when (sortOption) {
                HistorySortOption.NEWEST_FIRST ->
                    historyRepository.getHistoryByDateRangeNewest(startDate, endDate)
                HistorySortOption.OLDEST_FIRST ->
                    historyRepository.getHistoryByDateRangeOldest(startDate, endDate)
            }
        } else {
            when (sortOption) {
                HistorySortOption.NEWEST_FIRST ->
                    historyRepository.getAllHistoryNewestFirst()
                HistorySortOption.OLDEST_FIRST ->
                    historyRepository.getAllHistoryOldestFirst()
            }
        }

        return flow.flowOn(Dispatchers.IO)
    }

    // ==================== IMAGE REPAIR ====================

    /**
     * Repair missing images cho tất cả entries
     * Download lại ảnh từ remote nếu local file không tồn tại
     */
    suspend fun repairMissingImagesOnce() = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "Starting one-time image repair check...")

            val entries = collectAllEntries()

            entries.forEach { entry ->
                launch {
                    repairSingleImage(entry)
                }
            }
        }.onFailure { e ->
            Log.e(TAG, "Error in repair: ${e.message}", e)
        }
    }

    /**
     * Collect tất cả entries từ Flow
     */
    private suspend fun collectAllEntries(): List<HistoryEntry> {
        val allEntries = historyRepository.getAllHistoryNewestFirst()
        val entries = mutableListOf<HistoryEntry>()

        allEntries.collect { list ->
            entries.addAll(list)
            return@collect
        }

        return entries
    }

    /**
     * Repair missing image cho một entry
     * Download từ remote URL nếu local file không tồn tại
     */
    private suspend fun repairSingleImage(entry: HistoryEntry) {
        if (downloadingImages.containsKey(entry.id)) {
            return
        }

        val localPath = entry.localImagePath
        val remoteUrl = entry.imagePath

        val fileExists = if (localPath.isNotEmpty()) {
            File(localPath).exists()
        } else {
            false
        }

        if (!fileExists && remoteUrl.startsWith("http")) {
            downloadAndUpdateImage(entry, remoteUrl)
        }
    }

    /**
     * Download image từ remote và update entry
     */
    private suspend fun downloadAndUpdateImage(entry: HistoryEntry, remoteUrl: String) {
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

    // ==================== FAVORITE OPERATIONS ====================

    /**
     * Toggle favorite status của một history entry
     */
    suspend fun toggleFavorite(entry: HistoryEntry) {
        withContext(Dispatchers.IO) {
            runCatching {
                historyRepository.update(entry.copy(isFavorite = !entry.isFavorite))
            }.onFailure { e ->
                Log.e(TAG, "Error toggling favorite: ${e.message}", e)
            }
        }
    }

    // ==================== DELETE OPERATIONS ====================

    /**
     * Xóa một history entry
     * Tự động xóa local image file nếu tồn tại
     */
    suspend fun deleteHistory(entry: HistoryEntry) {
        withContext(Dispatchers.IO) {
            runCatching {
                deleteLocalImageFile(entry.localImagePath)
                historyRepository.delete(entry)
            }.onFailure { e ->
                Log.e(TAG, "Error deleting history entry: ${e.message}", e)
            }
        }
    }

    /**
     * Xóa toàn bộ lịch sử
     * Tự động xóa tất cả image files có prefix "species_"
     */
    suspend fun deleteAllHistory() {
        withContext(Dispatchers.IO) {
            runCatching {
                historyRepository.deleteAll()
                deleteAllLocalImages()
            }.onFailure { e ->
                Log.e(TAG, "Error deleting history: ${e.message}", e)
            }
        }
    }

    /**
     * Xóa local image file nếu tồn tại
     */
    private fun deleteLocalImageFile(localPath: String) {
        if (localPath.isNotEmpty()) {
            val file = File(localPath)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    /**
     * Xóa tất cả local images có prefix "species_"
     */
    private fun deleteAllLocalImages() {
        val dir = context.filesDir
        dir.listFiles()?.forEach { file ->
            if (file.name.startsWith(IMAGE_PREFIX)) {
                file.delete()
            }
        }
    }
}