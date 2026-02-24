package com.nguyendevs.ecolens.api

// ==================== INATURALIST API MODELS ====================

/** Thông tin taxon từ iNaturalist */
data class Taxon(
        val id: Int,
        val name: String,
        val rank: String,
        val preferred_common_name: String = "",
        val ancestors: List<Ancestor> = emptyList()
)

data class Ancestor(val id: Int, val name: String, val rank: String)

/** Kết quả nhận diện một loài từ iNaturalist */
data class IdentificationResult(val taxon: Taxon, val combined_score: Double)

/** Response chứa danh sách kết quả nhận diện */
data class IdentificationResponse(val results: List<IdentificationResult>)

/** Response chứa chi tiết taxon từ iNaturalist */
data class TaxonDetailsResponse(val results: List<TaxonDetail>)

/** Chi tiết taxon bao gồm Wikipedia summary và danh sách ảnh */
data class TaxonDetail(
        val id: Int,
        val wikipedia_summary: String = "",
        val taxon_photos: List<TaxonPhoto> = emptyList()
)

data class TaxonPhoto(val photo: PhotoDetails)

data class PhotoDetails(
        val id: Long,
        val medium_url: String? = null,
        val large_url: String? = null
)

// ==================== GEMINI API MODELS ====================

/** Request gửi đến Gemini API */
data class GeminiRequest(
        val contents: List<GeminiContent>,
        val system_instruction: GeminiContent? = null
)

/** Nội dung một message trong Gemini conversation */
data class GeminiContent(val role: String? = "user", val parts: List<GeminiPart>)

/** Một phần của message (text hoặc media) */
data class GeminiPart(val text: String)

/** Response từ Gemini API */
data class GeminiResponse(val candidates: List<GeminiCandidate>?)

/** Một candidate response từ Gemini */
data class GeminiCandidate(val content: GeminiContent?)
