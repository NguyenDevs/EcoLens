package com.nguyendevs.ecolens.model

data class User(
    val username: String = "",
    val email: String = "",
    val passwordHash: String = "",
    val language: String = "en",
    val darkMode: Boolean = false
)