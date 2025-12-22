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
        // Thêm xử lý lỗi 429 cho Gemini
        else if (response.code == 429) {
            appContext?.let { context ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "Vượt hạn mức token Gemini. Vui lòng thử lại sau.",
                        Toast.LENGTH_LONG
                    ).show()
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