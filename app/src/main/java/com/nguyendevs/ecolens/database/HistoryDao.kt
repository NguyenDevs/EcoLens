package com.nguyendevs.ecolens.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nguyendevs.ecolens.models.history.HistoryEntry
import kotlinx.coroutines.flow.Flow

/** Room DAO cho history table. Định nghĩa các operations CRUD trên database. */
@Dao
interface HistoryDao {

    /**
     * Thêm hoặc thay thế một bản ghi lịch sử mới
     * @return ID của bản ghi vừa thêm
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(entry: HistoryEntry): Long

    // ==================== QUERY - READ ====================

    /** Lấy ID lớn nhất hiện có trong bảng lịch sử */
    @Query("SELECT MAX(id) FROM history_table") suspend fun getMaxId(): Int?

    /** Lấy tất cả lịch sử sắp xếp từ mới nhất đến cũ nhất */
    @Query("SELECT * FROM history_table ORDER BY timestamp DESC")
    fun getAllHistoryNewestFirst(): Flow<List<HistoryEntry>>

    /** Lấy tất cả lịch sử sắp xếp từ cũ nhất đến mới nhất */
    @Query("SELECT * FROM history_table ORDER BY timestamp ASC")
    fun getAllHistoryOldestFirst(): Flow<List<HistoryEntry>>

    /** Lấy một bản ghi lịch sử theo ID */
    @Query("SELECT * FROM history_table WHERE id = :id LIMIT 1")
    suspend fun getHistoryById(id: Int): HistoryEntry?

    /** Lấy một bản ghi lịch sử theo timestamp */
    @Query("SELECT * FROM history_table WHERE timestamp = :timestamp LIMIT 1")
    suspend fun getHistoryByTimestamp(timestamp: Long): HistoryEntry?

    /** Lấy các bản ghi có ID lớn hơn giá trị cho trước (dùng cho reorder) */
    @Query("SELECT * FROM history_table WHERE id > :id ORDER BY id ASC")
    suspend fun getEntriesWithIdGreaterThan(id: Int): List<HistoryEntry>

    /** Lấy lịch sử trong khoảng thời gian, sắp xếp từ mới đến cũ */
    @Query(
            "SELECT * FROM history_table WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC"
    )
    fun getHistoryByDateRangeNewest(startDate: Long, endDate: Long): Flow<List<HistoryEntry>>

    /** Lấy lịch sử trong khoảng thời gian, sắp xếp từ cũ đến mới */
    @Query(
            "SELECT * FROM history_table WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp ASC"
    )
    fun getHistoryByDateRangeOldest(startDate: Long, endDate: Long): Flow<List<HistoryEntry>>

    // ==================== UPDATE ====================

    /** Cập nhật một bản ghi lịch sử */
    @Update suspend fun update(entry: HistoryEntry)

    /** Cập nhật chi tiết thông tin loài sinh học */
    @Query(
            """
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
    """
    )
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

    // ==================== DELETE ====================

    /** Xóa toàn bộ lịch sử */
    @Query("DELETE FROM history_table") suspend fun deleteAll()

    /** Xóa một bản ghi lịch sử theo ID */
    @Query("DELETE FROM history_table WHERE id = :id") suspend fun deleteById(id: Int)
}
