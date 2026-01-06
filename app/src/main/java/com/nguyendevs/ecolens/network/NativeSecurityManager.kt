package com.nguyendevs.ecolens.network

/**
 * Quản lý các chức năng bảo mật native
 * Sử dụng JNI để gọi các hàm bảo mật được implement trong C/C++
 */
object NativeSecurityManager {

    init {
        System.loadLibrary("native-lib")
    }

    /**
     * Tính toán chữ ký HMAC cho message
     *
     * @param message Chuỗi message cần tính HMAC
     * @return Chữ ký HMAC dạng string
     */
    external fun calculateHMAC(message: String): String
}