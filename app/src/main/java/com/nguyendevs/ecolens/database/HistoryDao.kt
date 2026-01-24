package com.nguyendevs.ecolens.database

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.nguyendevs.ecolens.BuildConfig
import com.nguyendevs.ecolens.model.history.HistoryEntry
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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

/**
 * Repository quản lý lịch sử với Firebase sync. Đồng bộ Local Room DB với Firebase Realtime
 * DB/Storage.
 *
 * @param historyDao DAO instance
 * @param context Application context
 * @param externalScope Coroutine scope cho background tasks
 */
class HistoryRepository(
        private val historyDao: HistoryDao,
        private val context: Context,
        private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val database = FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL)
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ==================== FIREBASE REFERENCES ====================

    /** Lấy UID của người dùng hiện tại từ Firebase Auth */
    private fun getUserId(): String {
        return auth.currentUser?.uid ?: "anonymous"
    }

    /** Lấy reference đến node lịch sử của user trong Firebase Database */
    private fun getHistoryRef() = database.getReference("history").child(getUserId())

    /** Lấy reference đến thư mục lưu trữ của user trong Firebase Storage */
    private fun getStorageRef() = storage.reference.child("users").child(getUserId())

    // ==================== PUBLIC READ METHODS ====================

    fun getAllHistoryNewestFirst() = historyDao.getAllHistoryNewestFirst()

    fun getAllHistoryOldestFirst() = historyDao.getAllHistoryOldestFirst()

    fun getHistoryByDateRangeNewest(startDate: Long, endDate: Long) =
            historyDao.getHistoryByDateRangeNewest(startDate, endDate)

    fun getHistoryByDateRangeOldest(startDate: Long, endDate: Long) =
            historyDao.getHistoryByDateRangeOldest(startDate, endDate)

    suspend fun getHistoryById(id: Int): HistoryEntry? {
        return historyDao.getHistoryById(id)
    }

    // ==================== INSERT METHODS ====================

    /** Thêm bản ghi mới vào local database Tự động tạo ID mới dựa trên ID lớn nhất hiện có */
    suspend fun insertLocal(entry: HistoryEntry): Long =
            withContext(Dispatchers.IO) {
                val maxId = historyDao.getMaxId() ?: 0
                val newId = maxId + 1
                val entryWithId = entry.copy(id = newId)
                historyDao.insert(entryWithId)
                newId.toLong()
            }

    /**
     * Insert với Firebase sync.
     * @param entry Bản ghi cần thêm
     * @return ID của bản ghi mới
     */
    suspend fun insert(entry: HistoryEntry): Long {
        val id = insertLocal(entry)
        val entryWithId = entry.copy(id = id.toInt())
        externalScope.launch { syncRemote(entryWithId) }
        return id
    }

    // ==================== UPDATE METHODS ====================

    /** Cập nhật bản ghi trong local database */
    suspend fun updateLocal(entry: HistoryEntry) =
            withContext(Dispatchers.IO) { historyDao.update(entry) }

    /** Cập nhật bản ghi vào cả local và đồng bộ lên Firebase */
    suspend fun update(entry: HistoryEntry) {
        updateLocal(entry)
        externalScope.launch { syncRemote(entry) }
    }

    /**
     * Đồng bộ một bản ghi lên Firebase (cả Database và Storage) Tự động upload ảnh lên Storage nếu
     * là đường dẫn local
     */
    suspend fun syncRemote(entry: HistoryEntry) =
            withContext(Dispatchers.IO) {
                var entryToSync = entry

                val currentEntry = historyDao.getHistoryByTimestamp(entry.timestamp)
                if (currentEntry != null) {
                    entryToSync = currentEntry
                } else {
                    return@withContext
                }

                if (entryToSync.imagePath.isNotEmpty() && !entryToSync.imagePath.startsWith("http")
                ) {
                    try {
                        val fileUri = Uri.parse(entryToSync.imagePath)
                        val imageRef =
                                getStorageRef()
                                        .child(
                                                "${entryToSync.id}_${System.currentTimeMillis()}.jpg"
                                        )

                        val uploadData =
                                if (entryToSync.imagePath.startsWith("/")) {
                                    val options =
                                            BitmapFactory.Options().apply {
                                                inJustDecodeBounds = true
                                            }
                                    BitmapFactory.decodeFile(entryToSync.imagePath, options)
                                    options.inSampleSize =
                                            calculateInSampleSize(options, 1920, 1920)
                                    options.inJustDecodeBounds = false
                                    val bitmap =
                                            BitmapFactory.decodeFile(entryToSync.imagePath, options)
                                    val baos = ByteArrayOutputStream()
                                    bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                                    bitmap?.recycle()
                                    baos.toByteArray()
                                } else {
                                    null
                                }

                        if (uploadData != null) {
                            imageRef.putBytes(uploadData).await()
                        } else {
                            val uploadUri =
                                    if (entryToSync.imagePath.startsWith("/"))
                                            Uri.fromFile(File(entryToSync.imagePath))
                                    else fileUri
                            imageRef.putFile(uploadUri).await()
                        }

                        val downloadUrl = imageRef.downloadUrl.await().toString()

                        entryToSync =
                                entryToSync.copy(
                                        imagePath = downloadUrl,
                                        localImagePath = entryToSync.imagePath
                                )
                        historyDao.update(entryToSync)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                try {
                    getHistoryRef().child(entryToSync.id.toString()).setValue(entryToSync).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

    // ==================== DELETE METHODS ====================

    /** Xóa toàn bộ lịch sử khỏi cả local và Firebase */
    suspend fun deleteAll() {
        historyDao.deleteAll()
        try {
            getHistoryRef().removeValue().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Xóa một bản ghi khỏi cả local, Firebase Storage và Database Tự động sắp xếp lại ID sau khi
     * xóa
     */
    suspend fun delete(entry: HistoryEntry) {
        val idToDelete = entry.id
        historyDao.deleteById(idToDelete)

        externalScope.launch {
            if (entry.imagePath.startsWith("http")) {
                try {
                    storage.getReferenceFromUrl(entry.imagePath).delete().await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            try {
                getHistoryRef().child(idToDelete.toString()).removeValue().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        reorderIds(idToDelete)
    }

    /**
     * Sắp xếp lại ID sau khi xóa để giữ thứ tự liên tục Sử dụng batch update để tối ưu performance
     */
    private suspend fun reorderIds(deletedId: Int) =
            withContext(Dispatchers.IO) {
                try {
                    val entriesToUpdate = historyDao.getEntriesWithIdGreaterThan(deletedId)
                    if (entriesToUpdate.isEmpty()) return@withContext

                    val updates = hashMapOf<String, Any?>()

                    for (entry in entriesToUpdate) {
                        val oldId = entry.id
                        val newId = oldId - 1
                        val updatedEntry = entry.copy(id = newId)

                        historyDao.deleteById(oldId)
                        historyDao.insert(updatedEntry)

                        updates[oldId.toString()] = null
                        updates[newId.toString()] = updatedEntry
                    }

                    if (updates.isNotEmpty()) {
                        try {
                            getHistoryRef().updateChildren(updates).await()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

    // ==================== SYNC METHODS ====================

    /** Tải toàn bộ lịch sử từ Firebase về local Tự động tải ảnh về local nếu chưa có */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun fetchHistory() =
            withContext(Dispatchers.IO) {
                if (auth.currentUser == null) return@withContext

                try {
                    val snapshot = getHistoryRef().get().await()
                    if (snapshot.exists()) {
                        for (child in snapshot.children) {
                            val remoteEntry = child.getValue(HistoryEntry::class.java)
                            if (remoteEntry != null) {
                                val finalEntry = remoteEntry
                                val localPath = remoteEntry.localImagePath
                                var hasLocalImage = false

                                if (localPath.isNotEmpty()) {
                                    if (localPath.startsWith("/")) {
                                        hasLocalImage = File(localPath).exists()
                                    } else {
                                        try {
                                            val uri = Uri.parse(localPath)
                                            context.contentResolver.openInputStream(uri)?.close()
                                            hasLocalImage = true
                                        } catch (e: Exception) {
                                            hasLocalImage = false
                                        }
                                    }
                                }

                                historyDao.insert(finalEntry)

                                if (!hasLocalImage && remoteEntry.imagePath.startsWith("http")) {
                                    externalScope.launch {
                                        val downloadedPath =
                                                downloadImageToLocal(
                                                        remoteEntry.imagePath,
                                                        remoteEntry.id
                                                )
                                        if (downloadedPath != null) {
                                            val updatedEntry =
                                                    finalEntry.copy(localImagePath = downloadedPath)
                                            historyDao.update(updatedEntry)
                                            try {
                                                if (remoteEntry.localImagePath != downloadedPath) {
                                                    getHistoryRef()
                                                            .child(remoteEntry.id.toString())
                                                            .child("localImagePath")
                                                            .setValue(downloadedPath)
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

    // ==================== UTILITY METHODS ====================

    /** Tính toán inSampleSize để giảm kích thước ảnh khi decode */
    private fun calculateInSampleSize(
            options: BitmapFactory.Options,
            reqWidth: Int,
            reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Tải ảnh từ URL về bộ nhớ local với nén JPEG 80% Sử dụng suspendCancellableCoroutine để tránh
     * blocking call
     */
    private suspend fun downloadImageToLocal(url: String, id: Int): String? =
            suspendCancellableCoroutine { continuation ->
                val target =
                        object : com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                    resource: Bitmap,
                                    transition:
                                            com.bumptech.glide.request.transition.Transition<
                                                    in Bitmap>?
                            ) {
                                try {
                                    val filename = "species_${id}_${System.currentTimeMillis()}.jpg"
                                    val file = File(context.filesDir, filename)
                                    FileOutputStream(file).use { out ->
                                        resource.compress(Bitmap.CompressFormat.JPEG, 80, out)
                                    }
                                    continuation.resume(file.absolutePath, null)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    continuation.resume(null, null)
                                }
                            }

                            override fun onLoadCleared(
                                    placeholder: android.graphics.drawable.Drawable?
                            ) {
                                // Không cần xử lý
                            }

                            override fun onLoadFailed(
                                    errorDrawable: android.graphics.drawable.Drawable?
                            ) {
                                continuation.resume(null, null)
                            }
                        }

                Glide.with(context).asBitmap().load(url).into(target)

                continuation.invokeOnCancellation { Glide.with(context).clear(target) }
            }

    /**
     * Hủy tất cả background jobs khi Repository không còn được sử dụng Nên gọi khi
     * Activity/Fragment bị destroy
     */
    fun cleanup() {
        externalScope.coroutineContext.cancelChildren()
    }
}
