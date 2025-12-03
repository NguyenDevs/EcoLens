package com.nguyendevs.ecolens.model

data class Language(
    val code: String,
    val name: String,
    val flagEmoji: String,
    var isSelected: Boolean = false
)