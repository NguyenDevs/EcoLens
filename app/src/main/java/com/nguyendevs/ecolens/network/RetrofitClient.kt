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

/**
 * Singleton Retrofit client cho API requests. Quản lý HTTP config, interceptors và error handling.
 */
object RetrofitClient {

    private const val WORKER_BASE_URL = BuildConfig.WORKER_BASE_URL
    private var appContext: Context? = null
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    /**
     * Khởi tạo context cho Toast messages.
     * @param context Application context
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Hiển thị Toast an toàn trên main thread.
     * @param message Nội dung thông báo
     */
    private fun showToast(message: String) {
        appContext?.let { context ->
            mainHandler.post { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
        }
    }

    /** Logging interceptor cho debug mode. */
    private val loggingInterceptor =
            HttpLoggingInterceptor().apply {
                level =
                        if (BuildConfig.DEBUG) {
                            HttpLoggingInterceptor.Level.BODY
                        } else {
                            HttpLoggingInterceptor.Level.NONE
                        }
            }

    /** HMAC interceptor xác thực requests. */
    private val hmacInterceptor = HMACInterceptor()

    /** Error interceptor xử lý 401, 429 và Gemini errors. */
    private val authErrorInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)

        when {
            response.code == 401 && request.url.toString().contains("inaturalist") -> {
                showToast("iNaturalist Token hết hạn. Vui lòng làm mới")
            }
            response.code == 401 -> {
                showToast("Xác thực thất bại. Vui lòng cập nhật ứng dụng")
            }
            response.code == 429 -> {
                val resetTime = response.header("X-RateLimit-Reset") ?: "unknown"
                showToast("Quá nhiều yêu cầu. Vui lòng thử lại sau ${resetTime}s")
            }
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

    /** OkHttp client với interceptors và timeout config. */
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

    /** Retrofit instance cho Worker API. */
    private val iNaturalistRetrofit =
            Retrofit.Builder()
                    .baseUrl(WORKER_BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

    /** API service instance. */
    val iNaturalistApi: INaturalistApi = iNaturalistRetrofit.create(INaturalistApi::class.java)
}
