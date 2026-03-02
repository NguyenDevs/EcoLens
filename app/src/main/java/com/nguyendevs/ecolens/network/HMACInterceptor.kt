package com.nguyendevs.ecolens.network

import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

/**
 * Interceptor xác thực HMAC cho các request đến Workers
 */
class HMACInterceptor : Interceptor {

    companion object {
        private const val APP_ID = "com.nguyendevs.ecolens"
    }

    /**
     * Chặn và thêm các header xác thực HMAC vào request
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url

        if (!url.host.contains("workers.dev")) {
            return chain.proceed(request)
        }

        val timestamp = System.currentTimeMillis().toString()
        val requestId = UUID.randomUUID().toString()
        val path = url.encodedPath

        // Format: METHOD:PATH:TIMESTAMP:REQUEST_ID
        val message = "${request.method}:$path:$timestamp:$requestId"
        val signature = NativeSecurityManager.calculateHMAC(message)

        val newRequest = request.newBuilder()
            .addHeader("X-App-Id", APP_ID)
            .addHeader("X-Timestamp", timestamp)
            .addHeader("X-Request-Id", requestId)
            .addHeader("X-Signature", signature)
            .build()

        return chain.proceed(newRequest)
    }
}