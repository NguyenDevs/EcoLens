package com.nguyendevs.ecolens.database

import android.net.Uri
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nguyendevs.ecolens.model.SpeciesInfo

class HistoryTypeConverters {

    private val gson = Gson()

    // Chuyển SpeciesInfo thành JSON String
    @TypeConverter
    fun fromSpeciesInfo(info: SpeciesInfo): String {
        return gson.toJson(info)
    }

    // Chuyển JSON String thành SpeciesInfo
    @TypeConverter
    fun toSpeciesInfo(json: String): SpeciesInfo {
        val type = object : TypeToken<SpeciesInfo>() {}.type
        return gson.fromJson(json, type)
    }

    // Chuyển Uri thành String
    @TypeConverter
    fun fromUri(uri: Uri): String {
        return uri.toString()
    }

    // Chuyển String thành Uri
    @TypeConverter
    fun toUri(uriString: String): Uri {
        return Uri.parse(uriString)
    }
}