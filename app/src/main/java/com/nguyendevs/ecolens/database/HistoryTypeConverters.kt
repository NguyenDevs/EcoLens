package com.nguyendevs.ecolens.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nguyendevs.ecolens.model.SpeciesInfo

class HistoryTypeConverters {

    private val gson = Gson()

    @TypeConverter
    fun fromSpeciesInfo(info: SpeciesInfo?): String? {
        return info?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toSpeciesInfo(json: String?): SpeciesInfo? {
        return json?.let {
            if (it.isEmpty()) return@let null
            val type = object : TypeToken<SpeciesInfo>() {}.type
            gson.fromJson(it, type)
        }
    }
}