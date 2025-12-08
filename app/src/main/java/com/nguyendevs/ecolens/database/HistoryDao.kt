package com.nguyendevs.ecolens.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nguyendevs.ecolens.model.HistoryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    // Thêm một bản ghi lịch sử mới
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntry)

    // Lấy tất cả lịch sử sắp xếp từ mới nhất đến cũ nhất
    @Query("SELECT * FROM history_table ORDER BY timestamp DESC")
    fun getAllHistoryNewestFirst(): Flow<List<HistoryEntry>>

    // Lấy tất cả lịch sử sắp xếp từ cũ nhất đến mới nhất
    @Query("SELECT * FROM history_table ORDER BY timestamp ASC")
    fun getAllHistoryOldestFirst(): Flow<List<HistoryEntry>>

    // Lấy lịch sử theo khoảng thời gian, sắp xếp từ mới nhất
    @Query("SELECT * FROM history_table WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getHistoryByDateRangeNewest(startDate: Long, endDate: Long): Flow<List<HistoryEntry>>

    // Lấy lịch sử theo khoảng thời gian, sắp xếp từ cũ nhất
    @Query("SELECT * FROM history_table WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    fun getHistoryByDateRangeOldest(startDate: Long, endDate: Long): Flow<List<HistoryEntry>>

    // Cập nhật một bản ghi lịch sử
    @Update
    suspend fun update(entry: HistoryEntry)

    // Xóa tất cả lịch sử
    @Query("DELETE FROM history_table")
    suspend fun deleteAll()
}