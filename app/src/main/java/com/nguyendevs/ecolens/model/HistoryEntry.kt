package com.nguyendevs.ecolens.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_table")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imagePath: String,
    @Embedded val speciesInfo: SpeciesInfo,
    val timestamp: Long,
    val isFavorite: Boolean = false
)