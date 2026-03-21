package com.nguyendevs.ecolens.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nguyendevs.ecolens.models.history.HistoryEntry
import kotlinx.coroutines.flow.Flow

/** DAO hỗ trợ thao tác truy vấn dữ liệu lịch sử nhận diện. */
@Dao
interface HistoryDao {

    /** Thêm mới một bản ghi lịch sử. */
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(entry: HistoryEntry): Long

    /** Thêm nhiều bản ghi lịch sử cùng lúc. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<HistoryEntry>)

    /** Lấy ID cao nhất trong bảng lịch sử. */
    @Query("SELECT MAX(id) FROM history_table") suspend fun getMaxId(): Int?

    /** Lấy danh sách lịch sử mới nhất theo giới hạn, hỗ trợ lọc và tìm kiếm. */
    @Query(
        """
        SELECT * FROM history_table 
        WHERE userId = :userId 
        AND (:search = '' OR commonName LIKE '%' || :search || '%' OR scientificName LIKE '%' || :search || '%')
        AND (:category = '' OR kingdom LIKE '%' || :category || '%')
        ORDER BY timestamp DESC LIMIT :limit
        """
    )
    fun getHistoryNewestFirst(
        userId: String,
        limit: Int,
        category: String = "",
        search: String = ""
    ): Flow<List<HistoryEntry>>

    /** Lấy danh sách lịch sử cũ nhất theo giới hạn, hỗ trợ lọc và tìm kiếm. */
    @Query(
        """
        SELECT * FROM history_table 
        WHERE userId = :userId 
        AND (:search = '' OR commonName LIKE '%' || :search || '%' OR scientificName LIKE '%' || :search || '%')
        AND (:category = '' OR kingdom LIKE '%' || :category || '%')
        ORDER BY timestamp ASC LIMIT :limit
        """
    )
    fun getHistoryOldestFirst(
        userId: String,
        limit: Int,
        category: String = "",
        search: String = ""
    ): Flow<List<HistoryEntry>>

    /** Lấy toàn bộ danh sách lịch sử từ mới nhất. */
    @Query("SELECT * FROM history_table WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllHistoryNewestFirst(userId: String): Flow<List<HistoryEntry>>

    /** Lấy toàn bộ danh sách lịch sử từ cũ nhất. */
    @Query("SELECT * FROM history_table WHERE userId = :userId ORDER BY timestamp ASC")
    fun getAllHistoryOldestFirst(userId: String): Flow<List<HistoryEntry>>

    /** Lấy lịch sử sắp xếp theo tên (A-Z), hỗ trợ lọc và tìm kiếm. */
    @Query(
        """
        SELECT * FROM history_table 
        WHERE userId = :userId 
        AND (:search = '' OR commonName LIKE '%' || :search || '%' OR scientificName LIKE '%' || :search || '%')
        AND (:category = '' OR kingdom LIKE '%' || :category || '%')
        ORDER BY commonName ASC LIMIT :limit
        """
    )
    fun getHistoryAlphabetical(
        userId: String,
        limit: Int,
        category: String = "",
        search: String = ""
    ): Flow<List<HistoryEntry>>

    /** Lấy lịch sử có độ tin cậy cao nhất, hỗ trợ lọc và tìm kiếm. */
    @Query(
        """
        SELECT * FROM history_table 
        WHERE userId = :userId 
        AND (:search = '' OR commonName LIKE '%' || :search || '%' OR scientificName LIKE '%' || :search || '%')
        AND (:category = '' OR kingdom LIKE '%' || :category || '%')
        ORDER BY confidence DESC LIMIT :limit
        """
    )
    fun getHistoryConfidenceHigh(
        userId: String,
        limit: Int,
        category: String = "",
        search: String = ""
    ): Flow<List<HistoryEntry>>

    /** Tìm lịch sử theo ID. */
    @Query("SELECT * FROM history_table WHERE id = :id LIMIT 1")
    suspend fun getHistoryById(id: Int): HistoryEntry?

    /** Tìm lịch sử theo thời gian tạo. */
    @Query("SELECT * FROM history_table WHERE timestamp = :timestamp LIMIT 1")
    suspend fun getHistoryByTimestamp(timestamp: Long): HistoryEntry?

    /** Lấy các bản ghi lịch sử có ID lớn hơn ngưỡng cho trước. */
    @Query("SELECT * FROM history_table WHERE id > :id ORDER BY id ASC")
    suspend fun getEntriesWithIdGreaterThan(id: Int): List<HistoryEntry>

    /** Lấy lịch sử theo khoảng thời gian, hỗ trợ lọc và tìm kiếm (mới nhất trước). */
    @Query(
        """
        SELECT * FROM history_table 
        WHERE userId = :userId 
        AND timestamp BETWEEN :startDate AND :endDate 
        AND (:search = '' OR commonName LIKE '%' || :search || '%' OR scientificName LIKE '%' || :search || '%')
        AND (:category = '' OR kingdom LIKE '%' || :category || '%')
        ORDER BY timestamp DESC LIMIT :limit
        """
    )
    fun getHistoryByDateRangeNewest(
        userId: String,
        startDate: Long,
        endDate: Long,
        limit: Int,
        category: String = "",
        search: String = ""
    ): Flow<List<HistoryEntry>>

    /** Lấy lịch sử theo khoảng thời gian, hỗ trợ lọc và tìm kiếm (cũ nhất trước). */
    @Query(
        """
        SELECT * FROM history_table 
        WHERE userId = :userId 
        AND timestamp BETWEEN :startDate AND :endDate 
        AND (:search = '' OR commonName LIKE '%' || :search || '%' OR scientificName LIKE '%' || :search || '%')
        AND (:category = '' OR kingdom LIKE '%' || :category || '%')
        ORDER BY timestamp ASC LIMIT :limit
        """
    )
    fun getHistoryByDateRangeOldest(
        userId: String,
        startDate: Long,
        endDate: Long,
        limit: Int,
        category: String = "",
        search: String = ""
    ): Flow<List<HistoryEntry>>

    /** Lấy lịch sử theo Alpha B trong khoảng thời gian, hỗ trợ lọc. */
    @Query(
        """
        SELECT * FROM history_table 
        WHERE userId = :userId 
        AND timestamp BETWEEN :startDate AND :endDate 
        AND (:search = '' OR commonName LIKE '%' || :search || '%' OR scientificName LIKE '%' || :search || '%')
        AND (:category = '' OR kingdom LIKE '%' || :category || '%')
        ORDER BY commonName ASC LIMIT :limit
        """
    )
    fun getHistoryByDateRangeAlphabetical(
        userId: String,
        startDate: Long,
        endDate: Long,
        limit: Int,
        category: String = "",
        search: String = ""
    ): Flow<List<HistoryEntry>>

    /** Lấy lịch sử có độ tin cậy cao trong khoảng thời gian, hỗ trợ lọc. */
    @Query(
        """
        SELECT * FROM history_table 
        WHERE userId = :userId 
        AND timestamp BETWEEN :startDate AND :endDate 
        AND (:search = '' OR commonName LIKE '%' || :search || '%' OR scientificName LIKE '%' || :search || '%')
        AND (:category = '' OR kingdom LIKE '%' || :category || '%')
        ORDER BY confidence DESC LIMIT :limit
        """
    )
    fun getHistoryByDateRangeConfidenceHigh(
        userId: String,
        startDate: Long,
        endDate: Long,
        limit: Int,
        category: String = "",
        search: String = ""
    ): Flow<List<HistoryEntry>>

    /** Cập nhật một bản ghi lịch sử. */
    @Update suspend fun update(entry: HistoryEntry)

    /** Cập nhật trạng thái yêu thích của bản ghi. */
    @Query("UPDATE history_table SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)

    /** Lấy danh sách lịch sử được yêu thích, hỗ trợ lọc và tìm kiếm. */
    @Query(
        """
        SELECT * FROM history_table 
        WHERE userId = :userId AND isFavorite = 1
        AND (:category = '' OR kingdom LIKE '%' || :category || '%')
        AND (:search = '' OR commonName LIKE '%' || :search || '%' OR scientificName LIKE '%' || :search || '%')
        ORDER BY timestamp DESC LIMIT :limit
        """
    )
    fun getFavoriteHistory(
        userId: String,
        limit: Int,
        category: String = "",
        search: String = ""
    ): Flow<List<HistoryEntry>>

    /** Cập nhật thông tin chi tiết sinh vật của bản ghi. */
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
            timestamp = :timestamp,
            isFavorite = :isFavorite,
            language = :language
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
            timestamp: Long,
            isFavorite: Boolean,
            language: String
    )

    /** Xóa toàn bộ lịch sử. */
    @Query("DELETE FROM history_table") suspend fun deleteAll()

    /** Xóa một lịch sử theo id. */
    @Query("DELETE FROM history_table WHERE id = :id") suspend fun deleteById(id: Int)

    /** Đếm tổng số bản ghi lịch sử của người dùng. */
    @Query("SELECT COUNT(*) FROM history_table WHERE userId = :userId")
    fun getTotalHistoryCount(userId: String): Flow<Int>
}
