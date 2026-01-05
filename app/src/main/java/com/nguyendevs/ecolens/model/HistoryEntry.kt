package com.nguyendevs.ecolens.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_table")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imagePath: String = "",
    @Embedded val speciesInfo: SpeciesInfo = SpeciesInfo(),
    val timestamp: Long = 0,
    val isFavorite: Boolean = false
)