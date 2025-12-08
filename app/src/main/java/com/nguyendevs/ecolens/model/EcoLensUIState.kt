package com.nguyendevs.ecolens.model

/**
 * Data class chứa trạng thái UI cho màn hình chính
 */
data class EcoLensUiState(
    val isLoading: Boolean = false,
    val speciesInfo: SpeciesInfo? = null,
    val error: String? = null
)