package com.nguyendevs.ecolens.managers.history

import android.content.Context
import android.util.Log
import com.nguyendevs.ecolens.database.HistoryRepository
import com.nguyendevs.ecolens.models.history.HistoryEntry
import com.nguyendevs.ecolens.models.history.HistorySortOption
import com.nguyendevs.ecolens.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Manager quản lý lịch sử tra cứu của người dùng. */
class HistoryManager(
    private val context: Context,
    private val historyRepository: HistoryRepository
) {

    companion object {
        private const val TAG = "HistoryManager"
        private const val IMAGE_PREFIX = "species_"
    }

    private val downloadingImages = ConcurrentHashMap<Int, Boolean>()

    /** Truy xuất lịch sử theo tiêu chí sắp xếp và thời gian có sẵn. */
    fun getHistoryBySortOption(
        sortOption: HistorySortOption,
        startDate: Long? = null,
        endDate: Long? = null,
        limit: Int = 20
    ): Flow<List<HistoryEntry>> {
        val flow = if (startDate != null && endDate != null) {
            when (sortOption) {
                HistorySortOption.NEWEST_FIRST ->
                    historyRepository.getHistoryByDateRangeNewest(startDate, endDate, limit)

                HistorySortOption.OLDEST_FIRST ->
                    historyRepository.getHistoryByDateRangeOldest(startDate, endDate, limit)

                HistorySortOption.ALPHABETICAL ->
                    historyRepository.getHistoryByDateRangeAlphabetical(startDate, endDate, limit)

                HistorySortOption.CONFIDENCE_HIGH ->
                    historyRepository.getHistoryByDateRangeConfidenceHigh(startDate, endDate, limit)

                HistorySortOption.FAVORITE ->
                    historyRepository.getFavoriteHistory(limit)
            }
        } else {
            when (sortOption) {
                HistorySortOption.NEWEST_FIRST ->
                    historyRepository.getHistoryNewestFirst(limit)

                HistorySortOption.OLDEST_FIRST ->
                    historyRepository.getHistoryOldestFirst(limit)

                HistorySortOption.ALPHABETICAL ->
                    historyRepository.getHistoryAlphabetical(limit)

                HistorySortOption.CONFIDENCE_HIGH ->
                    historyRepository.getHistoryConfidenceHigh(limit)

                HistorySortOption.FAVORITE ->
                    historyRepository.getFavoriteHistory(limit)
            }
        }

        return flow.flowOn(Dispatchers.IO)
    }

    /** Trích đếm tổng lượng mục lịch sử đã lưu. */
    fun getTotalHistoryCount(): Flow<Int> = historyRepository.getTotalHistoryCount()

    /** Cố gắng sửa chữa các hình ảnh bị thất lạc trong danh sách. */
    suspend fun repairMissingImagesOnce() = withContext(Dispatchers.IO) {
        runCatching {
            val entries = collectAllEntries()
            entries.chunked(4).forEach { batch ->
                batch.map { entry ->
                    async { repairSingleImage(entry) }
                }.awaitAll()
            }
        }.onFailure { e ->
            Log.e(TAG, "Error in repair: ${e.message}", e)
        }
    }

    /** Ánh xạ toàn bộ lịch sử thành danh sách liền mạch để kiểm tra. */
    private suspend fun collectAllEntries(): List<HistoryEntry> {
        val allEntries = historyRepository.getAllHistoryNewestFirst()
        val entries = mutableListOf<HistoryEntry>()

        allEntries.collect { list ->
            entries.addAll(list)
            return@collect
        }

        return entries
    }

    /** Kiểm định và nối lại dòng ảnh hỏng tương ứng một bản ghi lịch sử. */
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

    private suspend fun downloadAndUpdateImage(entry: HistoryEntry, remoteUrl: String) {
        try {
            downloadingImages[entry.id] = true
            val newLocalPath = ImageUtils.downloadImageToInternalStorage(context, remoteUrl)

            if (newLocalPath != null) {
                val updatedEntry = entry.copy(localImagePath = newLocalPath)
                historyRepository.updateLocal(updatedEntry)
            }
        } finally {
            downloadingImages.remove(entry.id)
        }
    }

    /** Xóa mục lịch sử đơn lẻ. */
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

    /** Xóa trắng toàn bộ dữ liệu lịch sử trên ứng dụng. */
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

    private fun deleteLocalImageFile(localPath: String) {
        if (localPath.isNotEmpty()) {
            val file = File(localPath)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private fun deleteAllLocalImages() {
        val dir = context.filesDir
        dir.listFiles()?.forEach { file ->
            if (file.name.startsWith(IMAGE_PREFIX)) {
                file.delete()
            }
        }
    }

    /** Bật/tắt trạng thái yêu thích của mục lịch sử. */
    suspend fun toggleFavorite(entry: HistoryEntry) {
        val newFavoriteStatus = !entry.isFavorite
        historyRepository.updateFavoriteStatus(entry, newFavoriteStatus)
    }
}
