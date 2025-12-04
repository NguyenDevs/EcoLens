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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntry)

    // Query cho sắp xếp theo ngày mới nhất
    @Query("SELECT * FROM history_table ORDER BY timestamp DESC")
    fun getAllHistoryNewestFirst(): Flow<List<HistoryEntry>>

    // Query cho sắp xếp theo ngày cũ nhất
    @Query("SELECT * FROM history_table ORDER BY timestamp ASC")
    fun getAllHistoryOldestFirst(): Flow<List<HistoryEntry>>

    // Query mặc định (tương thích với code cũ)
    @Query("SELECT * FROM history_table ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntry>>

    @Update
    suspend fun update(entry: HistoryEntry)

    @Query("DELETE FROM history_table")
    suspend fun deleteAll()
}