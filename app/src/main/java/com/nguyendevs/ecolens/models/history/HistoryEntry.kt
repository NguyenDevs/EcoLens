package com.nguyendevs.ecolens.models.history

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nguyendevs.ecolens.models.SpeciesInfo

/**
 * Entity đại diện cho một mục trong lịch sử nhận diện
 *
 * @property id ID tự động tăng của mục lịch sử
 * @property imagePath Đường dẫn ảnh gốc
 * @property localImagePath Đường dẫn ảnh lưu cục bộ
 * @property speciesInfo Thông tin chi tiết về loài
 * @property timestamp Thời gian nhận diện (milliseconds)
 * @property isFavorite Đánh dấu yêu thích hay không
 * @property language Ngôn ngữ sử dụng khi nhận diện (vi, en, zh, ja)
 */
@Entity(tableName = "history_table")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imagePath: String = "",
    val localImagePath: String = "",
    @Embedded val speciesInfo: SpeciesInfo = SpeciesInfo(),
    val timestamp: Long = 0,
    val language: String = "vi"
)