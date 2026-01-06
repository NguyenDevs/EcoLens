package com.nguyendevs.ecolens.model

/**
 * Model đại diện cho một ngôn ngữ trong ứng dụng
 *
 * @property code Mã ngôn ngữ (ví dụ: "en", "vi")
 * @property name Tên ngôn ngữ hiển thị
 * @property flagDrawable Resource ID của icon cờ quốc gia
 * @property isSelected Ngôn ngữ đang được chọn hay không
 */
data class Language(
    val code: String,
    val name: String,
    val flagDrawable: Int,
    var isSelected: Boolean = false
)