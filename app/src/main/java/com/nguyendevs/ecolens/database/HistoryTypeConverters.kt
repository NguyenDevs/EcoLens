package com.nguyendevs.ecolens.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nguyendevs.ecolens.model.SpeciesInfo

class HistoryTypeConverters {

    private val gson = Gson()

    @TypeConverter
    fun fromSpeciesInfo(info: SpeciesInfo): String {
        return gson.toJson(info)
    }

    @TypeConverter
    fun toSpeciesInfo(json: String): SpeciesInfo {
        val type = object : TypeToken<SpeciesInfo>() {}.type
        return gson.fromJson(json, type)
    }

}