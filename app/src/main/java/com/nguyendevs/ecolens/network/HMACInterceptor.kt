package com.nguyendevs.ecolens.network

import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

class HMACInterceptor : Interceptor {

    companion object {
        private const val APP_ID = "com.nguyendevs.ecolens"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url

        if (!url.host.contains("workers.dev")) {
            return chain.proceed(request)
        }

        val timestamp = System.currentTimeMillis().toString()
        val requestId = UUID.randomUUID().toString()
        val path = url.encodedPath

        val message = "${request.method}:$path:$timestamp:$requestId"

        // 🔐 HMAC tính hoàn toàn trong native
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
