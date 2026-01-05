package com.nguyendevs.ecolens.database

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

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

    @Query("DELETE FROM history_table WHERE id = :id")
    suspend fun deleteById(id: Int)
}

// Firebase implementation
class HistoryRepository(private val historyDao: HistoryDao, private val context: Context) {
    // Sử dụng URL cụ thể do người dùng cung cấp để đảm bảo kết nối đúng region
    private val database = FirebaseDatabase.getInstance("https://ecolens-658ae-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Lấy UID từ Firebase Auth
    private fun getUserId(): String {
        return auth.currentUser?.uid ?: "anonymous"
    }

    private fun getHistoryRef() = database.getReference("history").child(getUserId())
    private fun getStorageRef() = storage.reference.child("users").child(getUserId())

    fun getAllHistoryNewestFirst() = historyDao.getAllHistoryNewestFirst()
    fun getAllHistoryOldestFirst() = historyDao.getAllHistoryOldestFirst()
    fun getHistoryByDateRangeNewest(startDate: Long, endDate: Long) = historyDao.getHistoryByDateRangeNewest(startDate, endDate)
    fun getHistoryByDateRangeOldest(startDate: Long, endDate: Long) = historyDao.getHistoryByDateRangeOldest(startDate, endDate)
    
    suspend fun getHistoryById(id: Int): HistoryEntry? {
        return historyDao.getHistoryById(id)
    }

    suspend fun insert(entry: HistoryEntry): Long {
        val id = historyDao.insert(entry)
        var entryWithId = entry.copy(id = id.toInt())

        if (entry.imagePath.isNotEmpty() && !entry.imagePath.startsWith("http")) {
            try {
                val fileUri = Uri.parse(entry.imagePath)
                val imageRef = getStorageRef().child("${id}.jpg")

                val uploadUri = if (entry.imagePath.startsWith("/")) Uri.fromFile(File(entry.imagePath)) else fileUri

                imageRef.putFile(uploadUri).await()
                val downloadUrl = imageRef.downloadUrl.await().toString()
                
                entryWithId = entryWithId.copy(
                    imagePath = downloadUrl,
                    localImagePath = entry.imagePath
                )
                historyDao.update(entryWithId)
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

    suspend fun delete(entry: HistoryEntry) {
        val idToDelete = entry.id
        historyDao.deleteById(idToDelete)
        
        try {
            getHistoryRef().child(idToDelete.toString()).removeValue().await()
            reorderIds(idToDelete)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun reorderIds(deletedId: Int) {
        try {
            val snapshot = getHistoryRef().get().await()
            if (snapshot.exists()) {
                val updates = hashMapOf<String, Any?>()
                val entriesToUpdate = mutableListOf<HistoryEntry>()

                for (child in snapshot.children) {
                    val entry = child.getValue(HistoryEntry::class.java)
                    if (entry != null && entry.id > deletedId) {
                        entriesToUpdate.add(entry)
                    }
                }

                entriesToUpdate.sortBy { it.id }

                for (entry in entriesToUpdate) {
                    val oldId = entry.id
                    val newId = oldId - 1
                    val updatedEntry = entry.copy(id = newId)
                    updates[oldId.toString()] = null
                    updates[newId.toString()] = updatedEntry
                    
                    // Update local database
                    historyDao.deleteById(oldId)
                    historyDao.insert(updatedEntry)
                }

                if (updates.isNotEmpty()) {
                    getHistoryRef().updateChildren(updates).await()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchHistory() {
        // Chỉ fetch nếu đã đăng nhập
        if (auth.currentUser == null) return

        try {
            val snapshot = getHistoryRef().get().await()
            if (snapshot.exists()) {
                for (child in snapshot.children) {
                    val remoteEntry = child.getValue(HistoryEntry::class.java)
                    if (remoteEntry != null) {
                        var finalEntry = remoteEntry
                        val localPath = remoteEntry.localImagePath
                        
                        // Check if local image exists
                        var hasLocalImage = false
                        if (localPath.isNotEmpty()) {
                            if (localPath.startsWith("/")) {
                                hasLocalImage = File(localPath).exists()
                            } else {
                                // It's a URI string
                                try {
                                    val uri = Uri.parse(localPath)
                                    context.contentResolver.openInputStream(uri)?.close()
                                    hasLocalImage = true
                                } catch (e: Exception) {
                                    hasLocalImage = false
                                }
                            }
                        }
                        
                        if (!hasLocalImage && remoteEntry.imagePath.startsWith("http")) {
                            val downloadedPath = downloadImageToLocal(remoteEntry.imagePath, remoteEntry.id)
                            if (downloadedPath != null) {
                                finalEntry = finalEntry.copy(localImagePath = downloadedPath)
                                // Update Firebase with new local path
                                getHistoryRef().child(remoteEntry.id.toString()).child("localImagePath").setValue(downloadedPath)
                            }
                        }

                        historyDao.insert(finalEntry)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun downloadImageToLocal(url: String, id: Int): String? {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = Glide.with(context)
                    .asBitmap()
                    .load(url)
                    .submit()
                    .get()
                
                // Create a temp file first
                //val tempFile = File(context.cacheDir, "temp_restore_${id}.jpg")
                //FileOutputStream(tempFile).use { out ->
                //    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                //}
                
                // Save to internal storage instead of public storage
                val internalPath = ImageUtils.saveBitmapToInternalStorage(context, bitmap)
                
                // Clean up temp
                //if (tempFile.exists()) tempFile.delete()
                
                internalPath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}