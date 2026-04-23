package com.nguyendevs.ecolens.models

/**
 * Model đại diện cho thông tin người dùng
 *
 * @property username Tên người dùng
 * @property email Email của người dùng
 * @property language Ngôn ngữ được chọn (mặc định: "vi")
 * @property darkMode Chế độ tối có được bật hay không
 * @property iucnMode Chế độ IUCN (mặc định: true)
 * @property vnredlistMode Chế độ VN Red List (mặc định: true)
 * @property taxoMode Chế độ dịch Taxonomy (mặc định: false)
 */
data class User(
    val username: String = "",
    val email: String = "",
    val language: String = "vi",
    val darkMode: Boolean = false,
    val iucnMode: Boolean = true,
    val vnredlistMode: Boolean = true,
    val taxoMode: Boolean = false,
    val avatar: String = ""
)