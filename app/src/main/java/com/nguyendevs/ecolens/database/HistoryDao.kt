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

    // --- INSERT ---

    // Thêm một bản ghi lịch sử mới và trả về ID
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntry): Long

    // --- GET (READ) ---

    // Lấy tất cả lịch sử sắp xếp từ mới nhất đến cũ nhất
    @Query("SELECT * FROM history_table WHERE userId = :userId OR userId IS NULL ORDER BY timestamp DESC")
    fun getAllHistoryNewestFirst(userId: String?): Flow<List<HistoryEntry>>

    // Lấy tất cả lịch sử sắp xếp từ cũ nhất đến mới nhất
    @Query("SELECT * FROM history_table WHERE userId = :userId OR userId IS NULL ORDER BY timestamp ASC")
    fun getAllHistoryOldestFirst(userId: String?): Flow<List<HistoryEntry>>

    // Lấy một entry theo ID
    @Query("SELECT * FROM history_table WHERE id = :id LIMIT 1")
    suspend fun getHistoryById(id: Int): HistoryEntry?

    // Lấy một entry theo timestamp
    @Query("SELECT * FROM history_table WHERE timestamp = :timestamp LIMIT 1")
    suspend fun getHistoryByTimestamp(timestamp: Long): HistoryEntry?

    // Lấy lịch sử theo khoảng thời gian, sắp xếp từ mới nhất
    @Query("SELECT * FROM history_table WHERE (userId = :userId OR userId IS NULL) AND timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getHistoryByDateRangeNewest(userId: String?, startDate: Long, endDate: Long): Flow<List<HistoryEntry>>

    // Lấy lịch sử theo khoảng thời gian, sắp xếp từ cũ nhất
    @Query("SELECT * FROM history_table WHERE (userId = :userId OR userId IS NULL) AND timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    fun getHistoryByDateRangeOldest(userId: String?, startDate: Long, endDate: Long): Flow<List<HistoryEntry>>

    // --- UPDATE ---

    // Cập nhật một bản ghi lịch sử (Generic)
    @Update
    suspend fun update(entry: HistoryEntry)

    // Cập nhật chi tiết thông tin loài, timestamp và ngôn ngữ gốc
    @Query("""
        UPDATE history_table 
        SET commonName = :commonName,
            scientificName = :scientificName,
            kingdom = :kingdom,
            phylum = :phylum,
            className = :className,
            taxorder = :taxorder,
            family = :family,
            genus = :genus,
            species = :species,
            description = :description,
            characteristics = :characteristics,
            distribution = :distribution,
            habitat = :habitat,
            conservationStatus = :conservationStatus,
            confidence = :confidence,
            timestamp = :timestamp
        WHERE id = :id
    """)
    suspend fun updateSpeciesDetails(
        id: Int,
        commonName: String,
        scientificName: String,
        kingdom: String,
        phylum: String,
        className: String,
        taxorder: String,
        family: String,
        genus: String,
        species: String,
        description: String,
        characteristics: String,
        distribution: String,
        habitat: String,
        conservationStatus: String,
        confidence: Double,
        timestamp: Long
    )

    // --- DELETE ---

    // Xóa tất cả lịch sử
    @Query("DELETE FROM history_table WHERE userId = :userId OR userId IS NULL")
    suspend fun deleteAll(userId: String?)
}