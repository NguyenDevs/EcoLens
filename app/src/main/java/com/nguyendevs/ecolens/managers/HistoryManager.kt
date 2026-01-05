package com.nguyendevs.ecolens.managers

import android.util.Log
import com.nguyendevs.ecolens.database.HistoryRepository
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.model.HistorySortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class HistoryManager(private val historyRepository: HistoryRepository) {

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
        return flow.flowOn(Dispatchers.IO)
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
            }.onFailure { e ->
                Log.e(TAG, "Error deleting history: ${e.message}", e)
            }
        }
    }
}