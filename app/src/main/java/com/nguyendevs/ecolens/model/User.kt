package com.nguyendevs.ecolens.model

/**
 * Model đại diện cho thông tin người dùng
 *
 * @property username Tên người dùng
 * @property email Email của người dùng
 * @property language Ngôn ngữ được chọn (mặc định: "vi")
 * @property darkMode Chế độ tối có được bật hay không
 */
data class User(
    val username: String = "",
    val email: String = "",
    val language: String = "vi",
    val darkMode: Boolean = false
)