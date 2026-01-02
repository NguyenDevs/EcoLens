package com.nguyendevs.ecolens.managers

import android.util.Log
import com.nguyendevs.ecolens.database.HistoryDao
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.model.HistorySortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class HistoryManager(private val historyDao: HistoryDao) {

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
                HistorySortOption.NEWEST_FIRST -> historyDao.getHistoryByDateRangeNewest(startDate, endDate)
                HistorySortOption.OLDEST_FIRST -> historyDao.getHistoryByDateRangeOldest(startDate, endDate)
            }
        } else {
            when (sortOption) {
                HistorySortOption.NEWEST_FIRST -> historyDao.getAllHistoryNewestFirst()
                HistorySortOption.OLDEST_FIRST -> historyDao.getAllHistoryOldestFirst()
            }
        }
        return flow.flowOn(Dispatchers.IO)
    }

    suspend fun toggleFavorite(entry: HistoryEntry) {
        withContext(Dispatchers.IO) {
            runCatching {
                historyDao.update(entry.copy(isFavorite = !entry.isFavorite))
            }.onFailure { e ->
                Log.e(TAG, "Error toggling favorite: ${e.message}", e)
            }
        }
    }

    suspend fun deleteAllHistory() {
        withContext(Dispatchers.IO) {
            runCatching {
                historyDao.deleteAll()
            }.onFailure { e ->
                Log.e(TAG, "Error deleting history: ${e.message}", e)
            }
        }
    }
}