package com.nguyendevs.ecolens.models

/**
 * Trạng thái UI cho màn hình EcoLens
 *
 * @property isLoading Đang tải dữ liệu hay không
 * @property speciesInfo Thông tin loài sinh vật
 * @property error Thông báo lỗi (nếu có)
 * @property loadingStage Giai đoạn tải hiện tại
 * @property isTaxonomyTranslating Đang dịch taxonomy hay không
 */
data class EcoLensUiState(
        val isLoading: Boolean = false,
        val speciesInfo: SpeciesInfo? = null,
        val error: String? = null,
        val loadingStage: LoadingStage = LoadingStage.NONE,
        val isTaxonomyTranslating: Boolean = false,
        val images: List<String> = emptyList()
)

/** Các giai đoạn tải thông tin loài */
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
