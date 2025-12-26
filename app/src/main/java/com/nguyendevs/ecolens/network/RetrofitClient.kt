package com.nguyendevs.ecolens.network

import android.content.Context
import android.widget.Toast
import com.nguyendevs.ecolens.BuildConfig
import com.nguyendevs.ecolens.api.INaturalistApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val WORKER_BASE_URL = BuildConfig.WORKER_BASE_URL

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    // HMAC Interceptor - ĐẶT TRƯỚC authErrorInterceptor
    private val hmacInterceptor = HMACInterceptor()

    private val authErrorInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)

        // Xử lý 401 Unauthorized từ HMAC
        if (response.code == 401) {
            appContext?.let { context ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "Xác thực thất bại. Vui lòng cập nhật ứng dụng",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Xử lý 429 Rate Limit
        if (response.code == 429) {
            val resetTime = response.header("X-RateLimit-Reset") ?: "unknown"
            appContext?.let { context ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "Quá nhiều yêu cầu. Vui lòng thử lại sau ${resetTime}s",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // ==================== iNaturalist Error ====================
        if (response.code == 401 && request.url.toString().contains("inaturalist")) {
            appContext?.let { context ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "iNaturalist Token hết hạn. Vui lòng làm mới",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // ==================== Gemini Retry Info ====================
        else if (request.url.toString().contains("gemini")) {
            val allFailed = response.header("X-Gemini-All-Failed") == "true"
            val failedKeys = response.header("X-Gemini-Failed-Keys")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()

            if (allFailed) {
                appContext?.let { context ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        Toast.makeText(
                            context,
                            "Tất cả ${failedKeys.size} API keys đều hết quota. Vui lòng thử lại sau.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        response
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(hmacInterceptor)
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authErrorInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.SECONDS)
        .build()

    private val iNaturalistRetrofit = Retrofit.Builder()
        .baseUrl(WORKER_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val iNaturalistApi: INaturalistApi = iNaturalistRetrofit.create(INaturalistApi::class.java)
}