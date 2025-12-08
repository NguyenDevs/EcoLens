package com.nguyendevs.ecolens.api

import okhttp3.MultipartBody
import retrofit2.http.*

interface INaturalistApi {
    @Multipart
    @POST("inaturalist/v1/computervision/score_image")
    suspend fun identifySpecies(
        @Part image: MultipartBody.Part,
        @Query("lat") lat: Double = 16.0544,
        @Query("lng") lng: Double = 108.2022,
        @Query("locale") locale: String
    ): IdentificationResponse

    @GET("inaturalist/v1/taxa/{id}")
    suspend fun getTaxonDetails(
        @Path("id") taxonId: Int,
        @Query("locale") locale: String = "vi"
    ): TaxonDetailsResponse

    @POST("gemini")
    suspend fun askGemini(
        @Body request: GeminiRequest
    ): GeminiResponse
}