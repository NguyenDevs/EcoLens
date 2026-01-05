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
        // 1. Lấy Flow từ DB (Thao tác này rất nhanh vì chưa xử lý logic gì cả)
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

        // 2. Trả về Flow ngay lập tức cho UI
        return flow
            .onEach { list ->
                // 3. Khởi chạy Coroutine riêng để check ảnh ngầm (Fire-and-forget)
                // Không dùng withContext hay chặn luồng ở đây để đảm bảo UI hiện ngay lập tức
                CoroutineScope(Dispatchers.IO).launch {
                    checkAndRepairImages(list)
                }
            }
            .flowOn(Dispatchers.IO)
    }

    // Hàm này chạy ngầm hoàn toàn
    private suspend fun checkAndRepairImages(entries: List<HistoryEntry>) {
        // Duyệt qua danh sách để kiểm tra tính toàn vẹn của file ảnh
        entries.forEach { entry ->
            val localPath = entry.localImagePath
            val remoteUrl = entry.imagePath

            // Chỉ kiểm tra file nếu localPath có dữ liệu
            val fileExists = if (localPath.isNotEmpty()) File(localPath).exists() else false

            // Nếu file local không tồn tại nhưng có link online (Firebase)
            if (!fileExists && remoteUrl.startsWith("http")) {
                Log.d(TAG, "Missing local image for entry ${entry.id}. Downloading in background...")

                // Tải về Internal Storage
                val newLocalPath = ImageUtils.downloadImageToInternalStorage(context, remoteUrl)

                if (newLocalPath != null) {
                    // CẬP NHẬT DB: Khi update xong, Room sẽ tự trigger Flow ở trên emit lại danh sách mới
                    // UI sẽ tự động thay placeholder bằng ảnh thật
                    val updatedEntry = entry.copy(localImagePath = newLocalPath)
                    historyRepository.update(updatedEntry)
                    Log.d(TAG, "Restored image: $newLocalPath")
                } else {
                    Log.e(TAG, "Failed to restore image for entry ${entry.id}")
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
                // Xoá cả file vật lý để dọn dẹp bộ nhớ
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