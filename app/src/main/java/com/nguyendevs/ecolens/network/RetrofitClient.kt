package com.nguyendevs.ecolens.network

import com.nguyendevs.ecolens.api.INaturalistApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // API Token từ iNaturalist
    private const val INATURALIST_API_TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJ1c2VyX2lkIjo5OTU2ODY1LCJleHAiOjE3NjQ2NjE5ODJ9.dOIN54my7981FsBd0kQk-uo4lF1xD36p6fsmklyfzIP_GV5P2pb6fcxgI7HrBoNwtM-ZDLvewmxzPAVWm3erwg"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Interceptor để thêm Authorization header
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", INATURALIST_API_TOKEN)
            .build()
        chain.proceed(authenticatedRequest)
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