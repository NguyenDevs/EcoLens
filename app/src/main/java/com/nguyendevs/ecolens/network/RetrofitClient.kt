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
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authErrorInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)

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
            val retryCount = response.header("X-Gemini-Retry-Count")?.toIntOrNull() ?: 0
            val failedKeys = response.header("X-Gemini-Failed-Keys")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
            val successKey = response.header("X-Gemini-Success-Key")
            val allFailed = response.header("X-Gemini-All-Failed") == "true"

            appContext?.let { context ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    when {
                        // Trường hợp 1: Tất cả keys đều hết quota
                        allFailed -> {
                            Toast.makeText(
                                context,
                                "Tất cả ${failedKeys.size} API keys đều hết quota. Vui lòng thử lại sau.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        // Trường hợp 2: Có retry nhưng thành công
                        retryCount > 1 && successKey != null && successKey != "none" -> {
                            Toast.makeText(
                                context,
                                "Đã chuyển sang API key #${successKey.toInt() + 1} (${failedKeys.size} key hết quota)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        // Trường hợp 3: Lỗi 429 đơn lẻ (không phải từ retry logic)
                        response.code == 429 && retryCount == 0 -> {
                            Toast.makeText(
                                context,
                                "Vượt hạn mức token Gemini. Vui lòng thử lại sau.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        response
    }

    private val okHttpClient = OkHttpClient.Builder()
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