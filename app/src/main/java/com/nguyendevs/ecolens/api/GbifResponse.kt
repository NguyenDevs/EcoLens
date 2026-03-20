package com.nguyendevs.ecolens.api

import com.google.gson.annotations.SerializedName

/** Response từ GBIF chứa thông tin phân loại học của loài. */
data class GbifResponse(
    val kingdom: String? = null,
    val phylum: String? = null,
    @SerializedName("class") val className: String? = null,
    @SerializedName("order") val taxorder: String? = null,
    val family: String? = null,
    val genus: String? = null,
    val species: String? = null,
    val scientificName: String? = null,
    val canonicalName: String? = null
)
