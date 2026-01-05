package com.nguyendevs.ecolens.model

data class User(
    val username: String = "",
    val email: String = "",
    val language: String = "vi",
    val darkMode: Boolean = false
)