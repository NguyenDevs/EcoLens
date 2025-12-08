package com.nguyendevs.ecolens.model

/**
 * Data class đại diện cho một ngôn ngữ trong app
 */
data class Language(
    val code: String,
    val name: String,
    val flagDrawable: Int,
    var isSelected: Boolean = false
)