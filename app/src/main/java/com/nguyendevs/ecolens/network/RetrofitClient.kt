package com.nguyendevs.ecolens.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.nguyendevs.ecolens.BuildConfig
import com.nguyendevs.ecolens.api.INaturalistApi
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Client Retrofit cho các API requests Quản lý cấu hình HTTP client và xử lý các lỗi xác thực */
object RetrofitClient {

    private const val WORKER_BASE_URL = BuildConfig.WORKER_BASE_URL

    private var appContext: Context? = null

    // Shared main thread handler để tránh tạo mới mỗi lần
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    /** Khởi tạo context cho việc hiển thị thông báo */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /** Hiển thị toast message an toàn trên main thread */
    private fun showToast(message: String) {
        appContext?.let { context ->
            mainHandler.post { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
        }
    }

    /** Logging interceptor cho debug */
    private val loggingInterceptor =
            HttpLoggingInterceptor().apply {
                level =
                        if (BuildConfig.DEBUG) {
                            HttpLoggingInterceptor.Level.BODY
                        } else {
                            HttpLoggingInterceptor.Level.NONE
                        }
            }

    /** HMAC interceptor để xác thực requests Phải đặt trước authErrorInterceptor */
    private val hmacInterceptor = HMACInterceptor()

    /** Interceptor xử lý các lỗi xác thực và rate limit */
    private val authErrorInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)

        when {
            // Xử lý lỗi 401 Unauthorized từ HMAC
            response.code == 401 && request.url.toString().contains("inaturalist") -> {
                showToast("iNaturalist Token hết hạn. Vui lòng làm mới")
            }
            response.code == 401 -> {
                showToast("Xác thực thất bại. Vui lòng cập nhật ứng dụng")
            }
            // Xử lý lỗi 429 Rate Limit
            response.code == 429 -> {
                val resetTime = response.header("X-RateLimit-Reset") ?: "unknown"
                showToast("Quá nhiều yêu cầu. Vui lòng thử lại sau ${resetTime}s")
            }
            // Xử lý thông tin retry từ Gemini API
            request.url.toString().contains("gemini") -> {
                val allFailed = response.header("X-Gemini-All-Failed") == "true"
                if (allFailed) {
                    val failedKeys =
                            response.header("X-Gemini-Failed-Keys")?.split(",")?.filter {
                                it.isNotEmpty()
                            }
                                    ?: emptyList()
                    showToast(
                            "Tất cả ${failedKeys.size} API keys đều hết quota. Vui lòng thử lại sau."
                    )
                }
            }
        }

        response
    }

    /** OkHttp client với các interceptor và timeout cấu hình */
    private val okHttpClient =
            OkHttpClient.Builder()
                    .addInterceptor(hmacInterceptor)
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(authErrorInterceptor)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .callTimeout(0, TimeUnit.SECONDS)
                    .build()

    /** Retrofit instance cho iNaturalist API */
    private val iNaturalistRetrofit =
            Retrofit.Builder()
                    .baseUrl(WORKER_BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

    /** API service cho iNaturalist */
    val iNaturalistApi: INaturalistApi = iNaturalistRetrofit.create(INaturalistApi::class.java)
}
