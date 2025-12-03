package com.nguyendevs.ecolens.model

data class Language(
    val code: String,
    val name: String,
    val flagDrawable: Int,
    var isSelected: Boolean = false
)