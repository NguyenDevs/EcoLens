package com.nguyendevs.ecolens.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nguyendevs.ecolens.models.SpeciesInfo

/** Chuyển đổi kiểu dữ liệu SpeciesInfo sang JSON và ngược lại để lưu vào Room. */
class HistoryTypeConverters {

    private val gson = Gson()

    /** Chuyển SpeciesInfo thành chuỗi JSON. */
    @TypeConverter
    fun fromSpeciesInfo(info: SpeciesInfo?): String? {
        return info?.let { gson.toJson(it) }
    }

    /** Chuyển chuỗi JSON thành SpeciesInfo. */
    @TypeConverter
    fun toSpeciesInfo(json: String?): SpeciesInfo? {
        return json?.let {
            if (it.isEmpty()) return@let null
            val type = object : TypeToken<SpeciesInfo>() {}.type
            gson.fromJson(it, type)
        }
    }
}