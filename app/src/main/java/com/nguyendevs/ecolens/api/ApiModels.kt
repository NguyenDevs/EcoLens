package com.nguyendevs.ecolens.api

data class Taxon(
    val id: Int,
    val name: String,
    val rank: String,
    val preferred_common_name: String = "",
    val ancestors: List<Ancestor> = emptyList()
)

data class Ancestor(
    val id: Int,
    val name: String,
    val rank: String
)

data class IdentificationResult(
    val taxon: Taxon,
    val combined_score: Double
)

data class IdentificationResponse(
    val results: List<IdentificationResult>
)

data class TaxonDetailsResponse(
    val results: List<TaxonDetail>
)

data class TaxonDetail(
    val id: Int,
    val wikipedia_summary: String = ""
)

data class GeminiRequest(
    val contents: List<GeminiContent>
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)