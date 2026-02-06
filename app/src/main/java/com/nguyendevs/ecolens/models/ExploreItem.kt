package com.nguyendevs.ecolens.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity đại diện cho một item trong mục Explore. Mapping với struct dữ liệu trên Firebase Realtime
 * Database.
 */
@Entity(tableName = "explore_table")
data class ExploreItem(
        @PrimaryKey val id: String = "",
        val desc: String = "",
        val image: String = "",
        val name: String = "",
        val name_en: String = "",
        val name_ja: String = "",
        val name_zh: String = ""
)
