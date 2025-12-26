package com.nguyendevs.ecolens.network

object NativeSecurityManager {

    init {
        System.loadLibrary("native-lib")
    }

    external fun calculateHMAC(message: String): String
}
