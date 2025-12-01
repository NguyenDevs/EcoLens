package com.nguyendevs.ecolens.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

// Đổi tên file từ HistoryEntry.kt thành HistoryEntity.kt để rõ ràng hơn trong việc sử dụng Room

@Entity(tableName = "history_table")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) // Đặt id tự động tăng
    val id: Long = 0,
    val imageUri: Uri,
    val speciesInfo: SpeciesInfo,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable