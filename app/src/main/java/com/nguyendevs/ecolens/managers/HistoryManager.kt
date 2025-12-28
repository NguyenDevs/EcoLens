package com.nguyendevs.ecolens.managers

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.nguyendevs.ecolens.database.HistoryDao
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.model.HistorySortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

class HistoryManager(private val historyDao: HistoryDao) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun getHistoryBySortOption(
        sortOption: HistorySortOption,
        startDate: Long? = null,
        endDate: Long? = null
    ): Flow<List<HistoryEntry>> {
        val userId = auth.currentUser?.uid
        return if (startDate != null && endDate != null) {
            when (sortOption) {
                HistorySortOption.NEWEST_FIRST -> historyDao.getHistoryByDateRangeNewest(userId, startDate, endDate)
                HistorySortOption.OLDEST_FIRST -> historyDao.getHistoryByDateRangeOldest(userId, startDate, endDate)
            }
        } else {
            when (sortOption) {
                HistorySortOption.NEWEST_FIRST -> historyDao.getAllHistoryNewestFirst(userId)
                HistorySortOption.OLDEST_FIRST -> historyDao.getAllHistoryOldestFirst(userId)
            }
        }
    }

    suspend fun toggleFavorite(entry: HistoryEntry) {
        withContext(Dispatchers.IO) {
            try {
                val updatedEntry = entry.copy(isFavorite = !entry.isFavorite)
                historyDao.update(updatedEntry)
                syncEntryToFirebase(updatedEntry)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteAllHistory() {
        val userId = auth.currentUser?.uid
        withContext(Dispatchers.IO) {
            historyDao.deleteAll(userId)
            if (userId != null) {
                try {
                    val batch = firestore.batch()
                    val snapshot = firestore.collection("users").document(userId)
                        .collection("history").get().await()
                    for (doc in snapshot.documents) {
                        batch.delete(doc.reference)
                    }
                    batch.commit().await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun syncEntryToFirebase(entry: HistoryEntry) {
        val userId = auth.currentUser?.uid ?: return
        if (entry.userId != userId) return

        withContext(Dispatchers.IO) {
            try {
                // Upload image if it's a local path
                var imageUrl = entry.imagePath
                if (!imageUrl.startsWith("http")) {
                    val file = File(imageUrl)
                    if (file.exists()) {
                        val storageRef = storage.reference.child("users/$userId/history/${file.name}")
                        storageRef.putFile(Uri.fromFile(file)).await()
                        imageUrl = storageRef.downloadUrl.await().toString()
                    }
                }

                val entryToSave = entry.copy(imagePath = imageUrl)
                firestore.collection("users").document(userId)
                    .collection("history").document(entry.timestamp.toString())
                    .set(entryToSave).await()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun syncFromFirebase() {
        val userId = auth.currentUser?.uid ?: return
        withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("users").document(userId)
                    .collection("history").get().await()
                
                for (doc in snapshot.documents) {
                    val entry = doc.toObject(HistoryEntry::class.java)
                    if (entry != null) {
                        val existingEntry = historyDao.getHistoryByTimestamp(entry.timestamp)
                        if (existingEntry == null) {
                            historyDao.insert(entry.copy(id = 0)) // Reset ID to let Room generate it
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}