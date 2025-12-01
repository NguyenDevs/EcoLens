package com.nguyendevs.ecolens.model

import android.net.Uri
import java.io.Serializable

data class HistoryEntry(
    val id: Long = 0,
    val imageUri: Uri,
    val speciesInfo: SpeciesInfo,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable