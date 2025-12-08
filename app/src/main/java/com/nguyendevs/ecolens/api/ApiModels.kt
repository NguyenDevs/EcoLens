package com.nguyendevs.ecolens.api

// Thông tin về loài sinh vật
data class Taxon(
    val id: Int,
    val name: String,
    val rank: String,
    val preferred_common_name: String = "",
    val ancestors: List<Ancestor> = emptyList()
)

// Thông tin tổ tiên của loài
data class Ancestor(
    val id: Int,
    val name: String,
    val rank: String
)

// Kết quả nhận diện một loài
data class IdentificationResult(
    val taxon: Taxon,
    val combined_score: Double
)

// Phản hồi API nhận diện loài
data class IdentificationResponse(
    val results: List<IdentificationResult>
)

// Phản hồi API chi tiết loài
data class TaxonDetailsResponse(
    val results: List<TaxonDetail>
)

// Chi tiết về loài sinh vật
data class TaxonDetail(
    val id: Int,
    val wikipedia_summary: String = ""
)

// Yêu cầu gửi đến Gemini AI
data class GeminiRequest(
    val contents: List<GeminiContent>
)

// Nội dung tin nhắn Gemini
data class GeminiContent(
    val role: String? = "user",
    val parts: List<GeminiPart>
)

// Phần nội dung văn bản của Gemini
data class GeminiPart(
    val text: String
)

// Phản hồi từ Gemini AI
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

// Ứng cử viên câu trả lời từ Gemini
data class GeminiCandidate(
    val content: GeminiContent?
)