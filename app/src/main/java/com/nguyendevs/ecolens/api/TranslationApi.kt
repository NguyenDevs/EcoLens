package com.nguyendevs.ecolens.api

import retrofit2.http.*

interface TranslationApi {
    @POST("translate")
    @FormUrlEncoded
    suspend fun translate(
        @Field("q") text: String,
        @Field("source") sourceLang: String = "en",
        @Field("target") targetLang: String = "vi",
        @Field("format") format: String = "text"
    ): TranslationResponse
}