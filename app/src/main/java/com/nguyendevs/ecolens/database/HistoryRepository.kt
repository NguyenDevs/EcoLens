package com.nguyendevs.ecolens.database

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.nguyendevs.ecolens.network.SecurityProvider
import com.nguyendevs.ecolens.models.history.HistoryEntry
import com.nguyendevs.ecolens.utils.ImageHelper
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** Quản lý dữ liệu lịch sử, đồng bộ cả local Room và Firebase. */
class HistoryRepository(
    private val historyDao: HistoryDao,
    private val context: Context,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val database = FirebaseDatabase.getInstance(SecurityProvider.getFirebaseUrl())
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /** Trả về UID của người dùng. */
    private fun getUserId(): String = auth.currentUser?.uid ?: "anonymous"
    /** Trả về nhánh tham chiếu dữ liệu lịch sử trên Firebase. */
    private fun getHistoryRef() = database.getReference("history").child(getUserId())
    /** Trả về nhánh tham chiếu dữ liệu hình ảnh trên Firebase Storage. */
    private fun getStorageRef() = storage.reference.child("users").child(getUserId())

    /** Lấy toàn bộ lịch sử mới nhất. */
    fun getAllHistoryNewestFirst() = historyDao.getAllHistoryNewestFirst(getUserId())

    /** Lấy lịch sử mới nhất theo giới hạn, hỗ trợ lọc và tìm kiếm. */
    fun getHistoryNewestFirst(limit: Int, category: String = "", search: String = "") =
        historyDao.getHistoryNewestFirst(getUserId(), limit, category, search)

    /** Lấy lịch sử cũ nhất theo giới hạn, hỗ trợ lọc và tìm kiếm. */
    fun getHistoryOldestFirst(limit: Int, category: String = "", search: String = "") =
        historyDao.getHistoryOldestFirst(getUserId(), limit, category, search)

    /** Lấy lịch sử mới nhất trong khoảng thời gian, hỗ trợ lọc và tìm kiếm. */
    fun getHistoryByDateRangeNewest(startDate: Long, endDate: Long, limit: Int, category: String = "", search: String = "") =
        historyDao.getHistoryByDateRangeNewest(getUserId(), startDate, endDate, limit, category, search)

    /** Lấy lịch sử cũ nhất trong khoảng thời gian, hỗ trợ lọc và tìm kiếm. */
    fun getHistoryByDateRangeOldest(startDate: Long, endDate: Long, limit: Int, category: String = "", search: String = "") =
        historyDao.getHistoryByDateRangeOldest(getUserId(), startDate, endDate, limit, category, search)

    /** Lấy lịch sử theo Alpha B, hỗ trợ lọc và tìm kiếm. */
    fun getHistoryAlphabetical(limit: Int, category: String = "", search: String = "") =
        historyDao.getHistoryAlphabetical(getUserId(), limit, category, search)

    /** Lấy lịch sử có độ xác tín cao, hỗ trợ lọc và tìm kiếm. */
    fun getHistoryConfidenceHigh(limit: Int, category: String = "", search: String = "") =
        historyDao.getHistoryConfidenceHigh(getUserId(), limit, category, search)

    /** Lấy lịch sử yêu thích, hỗ trợ lọc và tìm kiếm. */
    fun getFavoriteHistory(limit: Int, category: String = "", search: String = "") =
        historyDao.getFavoriteHistory(getUserId(), limit, category, search)

    /** Lấy lịch sử theo Alpha B trong khoảng thời gian, hỗ trợ lọc. */
    fun getHistoryByDateRangeAlphabetical(startDate: Long, endDate: Long, limit: Int, category: String = "", search: String = "") =
        historyDao.getHistoryByDateRangeAlphabetical(getUserId(), startDate, endDate, limit, category, search)

    /** Lấy lịch sử có độ xác tín cao trong khoảng thời gian, hỗ trợ lọc. */
    fun getHistoryByDateRangeConfidenceHigh(startDate: Long, endDate: Long, limit: Int, category: String = "", search: String = "") =
        historyDao.getHistoryByDateRangeConfidenceHigh(getUserId(), startDate, endDate, limit, category, search)

    /** Truy tìm lịch sử dựa theo ID. */
    suspend fun getHistoryById(id: Int): HistoryEntry? = historyDao.getHistoryById(id)

    /** Trả về tổng lượng mục lịch sử của người dùng. */
    fun getTotalHistoryCount(): Flow<Int> = historyDao.getTotalHistoryCount(getUserId())

    /** Cấy bản ghi lịch sử vào bộ nhớ cục bộ. */
    suspend fun insertLocal(entry: HistoryEntry): Long = withContext(Dispatchers.IO) {
        val maxId = historyDao.getMaxId() ?: 0
        val newId = maxId + 1
        val entryWithId = entry.copy(id = newId, userId = getUserId())
        historyDao.insert(entryWithId)
        newId.toLong()
    }

    /** Cấy bản ghi lịch sử vào cục bộ lẫn Firebase. */
    suspend fun insert(entry: HistoryEntry): Long {
        val id = insertLocal(entry)
        externalScope.launch { syncRemote(entry.copy(id = id.toInt(), userId = getUserId())) }
        return id
    }

    /** Cập nhật bản ghi trong Room cục bộ. */
    suspend fun updateLocal(entry: HistoryEntry) = withContext(Dispatchers.IO) {
        historyDao.update(entry)
    }

    /** Đồng bộ thông tin sinh vật mới cập nhật lên mọi kho lưu trữ. */
    suspend fun update(entry: HistoryEntry) {
        updateLocal(entry)
        externalScope.launch { syncRemote(entry) }
    }

    /** Cập nhật trạng thái đánh dấu sao yêu thích của bản ghi. */
    suspend fun updateFavoriteStatus(entry: HistoryEntry, isFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            historyDao.updateFavoriteStatus(entry.id, isFavorite)
        }
        externalScope.launch { syncRemote(entry.copy(isFavorite = isFavorite)) }
    }

    /** Thực hiện đẩy file ảnh và thông tin của bản ghi lên đám mây. */
    suspend fun syncRemote(entry: HistoryEntry) = withContext(Dispatchers.IO) {
        var entryToSync = historyDao.getHistoryByTimestamp(entry.timestamp) ?: return@withContext

        if (entryToSync.imagePath.isNotEmpty() && !entryToSync.imagePath.startsWith("http")) {
            try {
                val imageRef = getStorageRef().child("${entryToSync.id}_${System.currentTimeMillis()}.jpg")
                val uploadData = ImageHelper.compressBitmap(entryToSync.imagePath)
                
                if (uploadData != null) {
                    imageRef.putBytes(uploadData).await()
                    val downloadUrl = imageRef.downloadUrl.await().toString()
                    entryToSync = entryToSync.copy(imagePath = downloadUrl, localImagePath = entryToSync.imagePath)
                    historyDao.update(entryToSync)
                }
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

    /** Xóa toàn bộ dữ liệu lịch sử trên hệ thống máy chủ. */
    suspend fun deleteAll() {
        try {
            getHistoryRef().removeValue().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Rút gọn dữ liệu trên cả thiết bị và đám mây đối với bản ghi cấp định. */
    suspend fun delete(entry: HistoryEntry) {
        historyDao.deleteById(entry.id)
        externalScope.launch {
            if (entry.imagePath.startsWith("http")) {
                try {
                    storage.getReferenceFromUrl(entry.imagePath).delete().await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            try {
                getHistoryRef().child(entry.id.toString()).removeValue().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Tải chép lại toàn bộ danh sách lịch sử từ máy chủ Firebase. */
    suspend fun fetchHistory() = withContext(Dispatchers.IO) {
        if (auth.currentUser == null) return@withContext
        try {
            val snapshot = getHistoryRef().get().await()
            if (snapshot.exists()) {
                val entries = snapshot.children.mapNotNull { it.getValue(HistoryEntry::class.java) }
                val processedEntries = entries.map { remoteEntry ->
                    val localEntry = historyDao.getHistoryById(remoteEntry.id)
                    if (localEntry != null && File(localEntry.localImagePath).exists()) {
                        remoteEntry.copy(localImagePath = localEntry.localImagePath)
                    } else if (remoteEntry.imagePath.startsWith("http")) {
                        val localPath = downloadImageToLocal(remoteEntry.imagePath, remoteEntry.id)
                        remoteEntry.copy(localImagePath = localPath ?: "")
                    } else {
                        remoteEntry
                    }
                }
                historyDao.insertAll(processedEntries)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Mồi nền tải ảnh trên mạng qua cấu trúc Glide và lưu trữ vào ổ. */
    private suspend fun downloadImageToLocal(url: String, id: Int): String? = withContext(Dispatchers.IO) {
        try {
            val bitmap = Glide.with(context).asBitmap().load(url).submit().get()
            val path = ImageHelper.saveBitmapToInternal(context, bitmap, "species", id)
            path
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Giải phóng những luồng xử lý không đồng bộ ngoại biên. */
    fun cleanup() {
        externalScope.coroutineContext.cancelChildren()
    }
}
