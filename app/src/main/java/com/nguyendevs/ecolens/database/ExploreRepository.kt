package com.nguyendevs.ecolens.database

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.nguyendevs.ecolens.BuildConfig
import com.nguyendevs.ecolens.models.ExploreItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** Quản lý dữ liệu explore items từ Firebase, có cache trong bộ nhớ. */
class ExploreRepository {

    private val firebaseDatabase: FirebaseDatabase =
            FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL)
    private val exploreRef = firebaseDatabase.getReference("explore/items")

    companion object {
        private var allItemsCache: List<ExploreItem> = emptyList()
        private var displayedItemsCache: List<ExploreItem> = emptyList()
    }

    /** Lấy ngẫu nhiên [count] items, ưu tiên dùng cache nếu đã có. */
    suspend fun getRandomExploreItems(count: Int): List<ExploreItem> =
            withContext(Dispatchers.IO) {
                if (displayedItemsCache.isNotEmpty()) {
                    return@withContext displayedItemsCache
                }

                if (allItemsCache.isEmpty()) {
                    fetchFromFirebase()
                }

                if (allItemsCache.isEmpty()) {
                    return@withContext emptyList()
                }

                val items = allItemsCache.shuffled().take(count)
                displayedItemsCache = items
                return@withContext items
            }

    /** Lấy toàn bộ explore items, ưu tiên dùng cache nếu đã có. */
    suspend fun getAllExploreItems(): List<ExploreItem> = withContext(Dispatchers.IO) {
        if (allItemsCache.isNotEmpty()) {
            return@withContext allItemsCache
        }
        fetchFromFirebase()
        return@withContext allItemsCache
    }

    /** Tải toàn bộ explore items từ Firebase và lưu vào cache. */
    private suspend fun fetchFromFirebase() {
        try {
            val snapshot = exploreRef.get().await()
            val items = mutableListOf<ExploreItem>()
            for (childSnapshot in snapshot.children) {
                val item = childSnapshot.getValue(ExploreItem::class.java)
                if (item != null) {
                    items.add(item)
                }
            }
            allItemsCache = items
        } catch (e: Exception) {
            Log.e("ExploreRepository", "Error fetching from Firebase", e)
        }
    }
}
