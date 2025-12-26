package com.nguyendevs.ecolens.model

data class EcoLensUiState(
    val isLoading: Boolean = false,
    val speciesInfo: SpeciesInfo? = null,
    val error: String? = null,
    val loadingStage: LoadingStage = LoadingStage.NONE
)

enum class LoadingStage {
    NONE,
    SCIENTIFIC_NAME,
    COMMON_NAME,
    TAXONOMY,
    DESCRIPTION,
    CHARACTERISTICS,
    DISTRIBUTION,
    HABITAT,
    CONSERVATION,
    COMPLETE
}