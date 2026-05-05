package com.nguyendevs.ecolens.network

import com.nguyendevs.ecolens.BuildConfig
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SecurityProvider {

    fun getApiUrl(): String = BuildConfig.WORKER_BASE_URL

    fun getAppSecret(): String = BuildConfig.APP_SECRET

    fun getFirebaseUrl(): String = BuildConfig.FIREBASE_URL

    fun calculateHMAC(message: String): String {
        val secret = getAppSecret()
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        mac.init(secretKey)
        val hash = mac.doFinal(message.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
