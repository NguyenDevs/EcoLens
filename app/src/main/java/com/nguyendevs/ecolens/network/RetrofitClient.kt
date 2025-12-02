package com.nguyendevs.ecolens.network

import com.nguyendevs.ecolens.api.INaturalistApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.nguyendevs.ecolens.BuildConfig
import java.util.concurrent.TimeUnit


object RetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // SỬA: Đổi tên từ iNaturalistInterceptor thành authInterceptor
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("Authorization", "Bearer ${BuildConfig.INATURALIST_API_TOKEN}")
            .header("Accept", "application/json")
            .method(original.method, original.body)

        val request = requestBuilder.build()
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val iNaturalistRetrofit = Retrofit.Builder()
        .baseUrl("https://api.inaturalist.org/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val translationRetrofit = Retrofit.Builder()
        .baseUrl("https://libretranslate.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val iNaturalistApi: INaturalistApi = iNaturalistRetrofit.create(INaturalistApi::class.java)
}