package com.nguyendevs.ecolens.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API interface cho các service backend
 * Bao gồm iNaturalist species identification và Gemini AI chat
 */
interface INaturalistApi {

    // ==================== INATURALIST - SPECIES IDENTIFICATION ====================

    /**
     * Nhận diện loài từ ảnh sử dụng iNaturalist Computer Vision
     * @param image Ảnh cần nhận diện (MultipartBody)
     * @param lat Latitude mặc định (Đà Nẵng, Vietnam)
     * @param lng Longitude mặc định (Đà Nẵng, Vietnam)
     * @param locale Ngôn ngữ cho kết quả (vi, en, etc.)
     * @return Danh sách kết quả nhận diện với điểm số
     */
    @Multipart
    @POST("inaturalist/v1/computervision/score_image")
    suspend fun identifySpecies(
        @Part image: MultipartBody.Part,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("locale") locale: String
    ): IdentificationResponse

    /**
     * Lấy thông tin chi tiết về một taxon (loài sinh học)
     * @param taxonId ID của taxon cần lấy thông tin
     * @param locale Ngôn ngữ cho kết quả
     * @return Chi tiết taxon bao gồm Wikipedia summary
     */
    @GET("inaturalist/v1/taxa/{id}")
    suspend fun getTaxonDetails(
        @Path("id") taxonId: Int,
        @Query("locale") locale: String = "vi"
    ): TaxonDetailsResponse

    // ==================== EXTERNAL APIS (GBIF & IUCN) ====================

    @GET
    suspend fun getGbifTaxonomy(@Url url: String): GbifResponse

    @GET("iucn/api/v4/taxa/scientific_name")
    suspend fun getIucnStatus(
        @Query("genus_name") genusName: String,
        @Query("species_name") speciesName: String
    ): IucnResponse

    // ==================== GEMINI AI - CHAT ====================

    /**
     * Gửi câu hỏi đến Gemini AI và nhận response đầy đủ
     * @param request Request chứa nội dung conversation
     * @return Response từ Gemini với các candidates
     */
    @POST("gemini")
    suspend fun askGemini(
        @Body request: GeminiRequest
    ): GeminiResponse

    /**
     * Stream response từ Gemini AI theo thời gian thực
     * Sử dụng cho hiệu ứng "typing" khi AI trả lời
     * @param request Request chứa nội dung conversation
     * @return ResponseBody stream để đọc từng phần response
     */
    @Streaming
    @POST("gemini/stream")
    suspend fun streamGemini(
        @Body request: GeminiRequest
    ): Response<ResponseBody>

    @POST("groq")
    suspend fun askGroq(
        @Body request: GeminiRequest
    ): GeminiResponse

    @Streaming
    @POST("groq/stream")
    suspend fun streamGroq(
        @Body request: GeminiRequest
    ): Response<ResponseBody>
}