package com.nguyendevs.ecolens.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nguyendevs.ecolens.models.history.HistoryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<HistoryEntry>)

    @Query("SELECT MAX(id) FROM history_table")
    suspend fun getMaxId(): Int?

    @Query("SELECT * FROM history_table ORDER BY timestamp DESC LIMIT :limit")
    fun getHistoryNewestFirst(limit: Int): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM history_table ORDER BY timestamp ASC LIMIT :limit")
    fun getHistoryOldestFirst(limit: Int): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM history_table ORDER BY timestamp DESC")
    fun getAllHistoryNewestFirst(): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM history_table ORDER BY timestamp ASC")
    fun getAllHistoryOldestFirst(): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM history_table WHERE id = :id LIMIT 1")
    suspend fun getHistoryById(id: Int): HistoryEntry?

    @Query("SELECT * FROM history_table WHERE timestamp = :timestamp LIMIT 1")
    suspend fun getHistoryByTimestamp(timestamp: Long): HistoryEntry?

    @Query("SELECT * FROM history_table WHERE id > :id ORDER BY id ASC")
    suspend fun getEntriesWithIdGreaterThan(id: Int): List<HistoryEntry>

    @Query("SELECT * FROM history_table WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC LIMIT :limit")
    fun getHistoryByDateRangeNewest(startDate: Long, endDate: Long, limit: Int): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM history_table WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp ASC LIMIT :limit")
    fun getHistoryByDateRangeOldest(startDate: Long, endDate: Long, limit: Int): Flow<List<HistoryEntry>>

    @Update
    suspend fun update(entry: HistoryEntry)

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
            timestamp = :timestamp,
            language = :language
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
            timestamp: Long,
            language: String
    )

    @Query("DELETE FROM history_table")
    suspend fun deleteAll()

    @Query("DELETE FROM history_table WHERE id = :id")
    suspend fun deleteById(id: Int)
}
