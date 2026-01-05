package com.nguyendevs.ecolens.database

import android.content.Context
import android.net.Uri
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.nguyendevs.ecolens.model.HistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.io.File

@Dao
interface HistoryDao {

    // --- INSERT ---

    // Thêm một bản ghi lịch sử mới và trả về ID
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntry): Long

    // --- GET (READ) ---

    // Lấy tất cả lịch sử sắp xếp từ mới nhất đến cũ nhất
    @Query("SELECT * FROM history_table ORDER BY timestamp DESC")
    fun getAllHistoryNewestFirst(): Flow<List<HistoryEntry>>

    // Lấy tất cả lịch sử sắp xếp từ cũ nhất đến mới nhất
    @Query("SELECT * FROM history_table ORDER BY timestamp ASC")
    fun getAllHistoryOldestFirst(): Flow<List<HistoryEntry>>

    // Lấy một entry theo ID
    @Query("SELECT * FROM history_table WHERE id = :id LIMIT 1")
    suspend fun getHistoryById(id: Int): HistoryEntry?

    // Lấy lịch sử theo khoảng thời gian, sắp xếp từ mới nhất
    @Query("SELECT * FROM history_table WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getHistoryByDateRangeNewest(startDate: Long, endDate: Long): Flow<List<HistoryEntry>>

    // Lấy lịch sử theo khoảng thời gian, sắp xếp từ cũ nhất
    @Query("SELECT * FROM history_table WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    fun getHistoryByDateRangeOldest(startDate: Long, endDate: Long): Flow<List<HistoryEntry>>

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
    @Query("DELETE FROM history_table")
    suspend fun deleteAll()
}

// Firebase implementation
class HistoryRepository(private val historyDao: HistoryDao, private val context: Context) {
    // Sử dụng URL cụ thể do người dùng cung cấp để đảm bảo kết nối đúng region
    private val database = FirebaseDatabase.getInstance("https://ecolens-658ae-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private val storage = FirebaseStorage.getInstance()

    private fun getUsername(): String {
        val sharedPreferences = context.getSharedPreferences("EcoLensPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("username", "default_user") ?: "default_user"
    }

    private fun getHistoryRef() = database.getReference("history").child(getUsername())
    private fun getStorageRef() = storage.reference.child("images").child(getUsername())

    fun getAllHistoryNewestFirst() = historyDao.getAllHistoryNewestFirst()
    fun getAllHistoryOldestFirst() = historyDao.getAllHistoryOldestFirst()
    fun getHistoryByDateRangeNewest(startDate: Long, endDate: Long) = historyDao.getHistoryByDateRangeNewest(startDate, endDate)
    fun getHistoryByDateRangeOldest(startDate: Long, endDate: Long) = historyDao.getHistoryByDateRangeOldest(startDate, endDate)

    suspend fun insert(entry: HistoryEntry): Long {
        val id = historyDao.insert(entry)
        var entryWithId = entry.copy(id = id.toInt())
        
        // Upload image to Firebase Storage
        if (entry.imagePath.isNotEmpty() && !entry.imagePath.startsWith("http")) {
            try {
                val file = Uri.fromFile(File(entry.imagePath))
                val imageRef = getStorageRef().child("${id}.jpg")
                imageRef.putFile(file).await()
                val downloadUrl = imageRef.downloadUrl.await().toString()
                entryWithId = entryWithId.copy(imagePath = downloadUrl)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            getHistoryRef().child(id.toString()).setValue(entryWithId).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return id
    }

    suspend fun update(entry: HistoryEntry) {
        historyDao.update(entry)
        try {
            getHistoryRef().child(entry.id.toString()).setValue(entry).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteAll() {
        historyDao.deleteAll()
        try {
            getHistoryRef().removeValue().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchHistory() {
        try {
            val snapshot = getHistoryRef().get().await()
            if (snapshot.exists()) {
                for (child in snapshot.children) {
                    val entry = child.getValue(HistoryEntry::class.java)
                    if (entry != null) {
                        historyDao.insert(entry)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}