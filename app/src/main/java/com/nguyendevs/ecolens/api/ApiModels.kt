package com.nguyendevs.ecolens.api

// iNaturalist API Models
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

// Translation API Models
data class TranslationResponse(
    val data: TranslationData
)

data class TranslationData(
    val translations: List<Translation>
)

data class Translation(
    val translatedText: String
)