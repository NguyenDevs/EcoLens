package com.nguyendevs.ecolens.database

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.nguyendevs.ecolens.BuildConfig
import com.nguyendevs.ecolens.models.history.HistoryEntry
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class HistoryRepository(
    private val historyDao: HistoryDao,
    private val context: Context,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val database = FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL)
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String {
        return auth.currentUser?.uid ?: "anonymous"
    }

    private fun getHistoryRef() = database.getReference("history").child(getUserId())

    private fun getStorageRef() = storage.reference.child("users").child(getUserId())

    fun getAllHistoryNewestFirst() = historyDao.getAllHistoryNewestFirst()

    fun getHistoryNewestFirst(limit: Int) = historyDao.getHistoryNewestFirst(limit)

    fun getHistoryOldestFirst(limit: Int) = historyDao.getHistoryOldestFirst(limit)

    fun getHistoryByDateRangeNewest(startDate: Long, endDate: Long, limit: Int) =
        historyDao.getHistoryByDateRangeNewest(startDate, endDate, limit)

    fun getHistoryByDateRangeOldest(startDate: Long, endDate: Long, limit: Int) =
        historyDao.getHistoryByDateRangeOldest(startDate, endDate, limit)

    suspend fun getHistoryById(id: Int): HistoryEntry? {
        return historyDao.getHistoryById(id)
    }

    suspend fun insertLocal(entry: HistoryEntry): Long =
        withContext(Dispatchers.IO) {
            val maxId = historyDao.getMaxId() ?: 0
            val newId = maxId + 1
            val entryWithId = entry.copy(id = newId)
            historyDao.insert(entryWithId)
            newId.toLong()
        }

    suspend fun insert(entry: HistoryEntry): Long {
        val id = insertLocal(entry)
        val entryWithId = entry.copy(id = id.toInt())
        externalScope.launch { syncRemote(entryWithId) }
        return id
    }

    suspend fun updateLocal(entry: HistoryEntry) =
        withContext(Dispatchers.IO) { historyDao.update(entry) }

    suspend fun update(entry: HistoryEntry) {
        withContext(Dispatchers.IO) {
            historyDao.updateSpeciesDetails(
                id = entry.id,
                commonName = entry.speciesInfo.commonName,
                scientificName = entry.speciesInfo.scientificName,
                kingdom = entry.speciesInfo.kingdom,
                phylum = entry.speciesInfo.phylum,
                className = entry.speciesInfo.className,
                taxorder = entry.speciesInfo.taxorder,
                family = entry.speciesInfo.family,
                genus = entry.speciesInfo.genus,
                species = entry.speciesInfo.species,
                description = entry.speciesInfo.description,
                characteristics = entry.speciesInfo.characteristics,
                distribution = entry.speciesInfo.distribution,
                habitat = entry.speciesInfo.habitat,
                conservationStatus = entry.speciesInfo.conservationStatus,
                confidence = entry.speciesInfo.confidence,
                timestamp = entry.timestamp,
                language = entry.language
            )
        }
        externalScope.launch { syncRemote(entry) }
    }

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

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun fetchHistory() =
        withContext(Dispatchers.IO) {
            if (auth.currentUser == null) return@withContext

            try {
                val snapshot = getHistoryRef().get().await()
                if (snapshot.exists()) {
                    val entries = mutableListOf<HistoryEntry>()
                    for (child in snapshot.children) {
                        val remoteEntry = child.getValue(HistoryEntry::class.java)
                        if (remoteEntry != null) {
                            entries.add(remoteEntry)
                        }
                    }

                    entries.sortByDescending { it.timestamp }

                    val batchSize = 5
                    val chunkedEntries = entries.chunked(batchSize)

                    for (chunk in chunkedEntries) {
                        val processedChunk = chunk.map { remoteEntry ->
                            async {
                                var entryToInsert = remoteEntry
                                val localEntry = historyDao.getHistoryById(remoteEntry.id)

                                if (localEntry != null && localEntry.localImagePath.isNotEmpty() && File(localEntry.localImagePath).exists()) {
                                    entryToInsert = entryToInsert.copy(localImagePath = localEntry.localImagePath)
                                } else if (remoteEntry.imagePath.startsWith("http")) {
                                    val localPath = downloadImageToLocal(remoteEntry.imagePath, remoteEntry.id)
                                    if (localPath != null) {
                                        entryToInsert = entryToInsert.copy(localImagePath = localPath)
                                    }
                                }
                                entryToInsert
                            }
                        }.awaitAll()
                        
                        historyDao.insertAll(processedChunk)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

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

    private suspend fun downloadImageToLocal(url: String, id: Int): String? =
        withContext(Dispatchers.IO) {
            try {
                val futureTarget = Glide.with(context)
                    .asBitmap()
                    .load(url)
                    .submit()

                val bitmap = futureTarget.get()

                val filename = "species_${id}_${System.currentTimeMillis()}.jpg"
                val file = File(context.filesDir, filename)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }

                Glide.with(context).clear(futureTarget)

                file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    fun cleanup() {
        externalScope.coroutineContext.cancelChildren()
    }
}
