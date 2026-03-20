package com.nguyendevs.ecolens.api

import com.google.gson.annotations.SerializedName

/** Response từ IUCN API chứa danh sách đánh giá bảo tồn. */
data class IucnResponse(
    val assessments: List<IucnAssessment>? = null
)

/** Một đánh giá bảo tồn IUCN với mã hạng đỏ. */
data class IucnAssessment(
    @SerializedName("red_list_category_code") val redListCategoryCode: String? = null
)
