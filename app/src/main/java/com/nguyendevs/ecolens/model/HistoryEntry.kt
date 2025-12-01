package com.nguyendevs.ecolens.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "history_table")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imageUri: Uri,
    val speciesInfo: SpeciesInfo,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable