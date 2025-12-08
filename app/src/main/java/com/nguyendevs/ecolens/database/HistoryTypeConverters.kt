package com.nguyendevs.ecolens.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nguyendevs.ecolens.model.SpeciesInfo

class HistoryTypeConverters {

    private val gson = Gson()

    // Chuyển đổi SpeciesInfo thành chuỗi JSON
    @TypeConverter
    fun fromSpeciesInfo(info: SpeciesInfo): String {
        return gson.toJson(info)
    }

    // Chuyển đổi chuỗi JSON thành SpeciesInfo
    @TypeConverter
    fun toSpeciesInfo(json: String): SpeciesInfo {
        val type = object : TypeToken<SpeciesInfo>() {}.type
        return gson.fromJson(json, type)
    }

}