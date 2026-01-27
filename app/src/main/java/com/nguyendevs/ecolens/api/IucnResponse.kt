package com.nguyendevs.ecolens.api

import com.google.gson.annotations.SerializedName

data class IucnResponse(
    val assessments: List<IucnAssessment>? = null
)

data class IucnAssessment(
    @SerializedName("red_list_category_code") val redListCategoryCode: String? = null
)
