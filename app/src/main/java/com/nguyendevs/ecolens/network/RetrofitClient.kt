package com.nguyendevs.ecolens.network

import com.nguyendevs.ecolens.api.INaturalistApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val WORKER_BASE_URL = "https://ecolens.tainguyen-devs.workers.dev/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Sẽ log toàn bộ request/response
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS) // Tăng timeout
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val iNaturalistRetrofit = Retrofit.Builder()
        .baseUrl(WORKER_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val iNaturalistApi: INaturalistApi = iNaturalistRetrofit.create(INaturalistApi::class.java)
}