package com.nguyendevs.ecolens.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
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

    // Endpoint gốc - giữ lại cho backward compatible
    @POST("gemini")
    suspend fun askGemini(
        @Body request: GeminiRequest
    ): GeminiResponse

    // Endpoint mới cho streaming
    @Streaming
    @POST("gemini/stream")
    suspend fun streamGemini(
        @Body request: GeminiRequest
    ): Response<ResponseBody>
}