package com.nguyendevs.ecolens.handlers

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.View
import androidx.core.content.ContextCompat
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemCardSpeciesInfoBinding
import com.nguyendevs.ecolens.handlers.animations.HomeAnimationHandler
import com.nguyendevs.ecolens.handlers.animations.ShimmerAnimationHandler
import com.nguyendevs.ecolens.handlers.home.ConfidenceDisplayHandler
import com.nguyendevs.ecolens.handlers.home.SectionDisplayHandler
import com.nguyendevs.ecolens.handlers.home.TaxonomyDisplayHandler
import com.nguyendevs.ecolens.handlers.home.HomeButtonHandler
import com.nguyendevs.ecolens.handlers.util.TextFormatter
import com.nguyendevs.ecolens.model.LoadingStage
import com.nguyendevs.ecolens.model.SpeciesInfo
import kotlinx.coroutines.*
import java.util.Locale

/**
 * Main coordinator cho việc hiển thị thông tin loài sinh vật Điều phối các handler con để xử lý
 * từng phần riêng biệt
 */
class SpeciesInfoHandler(
        private val context: Context,
        private val binding: ItemCardSpeciesInfoBinding,
        onCopySuccess: (String) -> Unit,
        onRetryClick: () -> Unit
) {
    private val handlerScope = CoroutineScope(Dispatchers.Main + Job())
    private var isInitialLoad = true
    private var allSectionsRendered = false
    private var lastDisplayedCommonName: String? = null
    private var currentImageUri: Uri? = null

    private val infoBinding
        get() = binding

    /** Thiết lập URI hình ảnh để sử dụng khi chia sẻ */
    fun setImageUri(uri: Uri?) {
        currentImageUri = uri
    }

    private val homeAnimationHandler = HomeAnimationHandler()
    private val shimmerAnimationHandler = ShimmerAnimationHandler(context)
    private val textFormatter = TextFormatter()
    private val taxonomyDisplayHandler =
            TaxonomyDisplayHandler(binding, homeAnimationHandler, shimmerAnimationHandler, textFormatter)
    private val confidenceDisplayHandler =
            ConfidenceDisplayHandler(context, binding, homeAnimationHandler)
    private val sectionDisplayHandler = SectionDisplayHandler(binding, textFormatter)
    private val homeButtonHandler =
            HomeButtonHandler(
                    context,
                    binding,
                    homeAnimationHandler,
                    textFormatter,
                    onCopySuccess,
                    onRetryClick,
                    handlerScope
            )

    /** Hiển thị thông tin loài theo từng giai đoạn loading */
    fun displaySpeciesInfo(info: SpeciesInfo, stage: LoadingStage) {
        if (stage == LoadingStage.NONE) {
            handlerScope.coroutineContext.cancelChildren()
            clearAllViews()
            isInitialLoad = true
            allSectionsRendered = false
            return
        }

        if (info.scientificName.isNotEmpty()) {
            displayScientificName(info)
        }
        if (info.commonName.isNotEmpty() && info.commonName != "...") {
            displayCommonName(info)
        }
        if (stage != LoadingStage.SCIENTIFIC_NAME) {
            confidenceDisplayHandler.displayConfidence(info, isWaiting = false)
        }

        when (stage) {
            LoadingStage.NONE -> {
                handlerScope.coroutineContext.cancelChildren()
                clearAllViews()
                isInitialLoad = true
                allSectionsRendered = false
            }
            LoadingStage.SCIENTIFIC_NAME -> {
                isInitialLoad = true
                displayCommonName(SpeciesInfo(commonName = "...", scientificName = ""))
                taxonomyDisplayHandler.prepareTaxonomyContainer()
                homeButtonHandler.setupCopyButton(info)
                homeButtonHandler.showCopyButton()
                homeButtonHandler.hideButtons()
                confidenceDisplayHandler.displayConfidence(info, isWaiting = true)
            }
            LoadingStage.COMMON_NAME -> {}
            LoadingStage.TAXONOMY -> {
                taxonomyDisplayHandler.stopShimmer()
                taxonomyDisplayHandler.displayTaxonomyWaterfall(info)
            }
            LoadingStage.DESCRIPTION -> {
                taxonomyDisplayHandler.stopShimmer()
                taxonomyDisplayHandler.displayTaxonomyWaterfall(info)
                sectionDisplayHandler.displaySection(
                        R.id.sectionDescription,
                        R.id.tvDescription,
                        info.description,
                        shouldScroll = false,
                        isInitialLoad = isInitialLoad
                )
                checkIfAllSectionsRendered(info)
            }
            LoadingStage.CHARACTERISTICS -> {
                taxonomyDisplayHandler.stopShimmer()
                taxonomyDisplayHandler.displayTaxonomyWaterfall(info)
                sectionDisplayHandler.displaySection(
                        R.id.sectionCharacteristics,
                        R.id.tvCharacteristics,
                        info.characteristics,
                        shouldScroll = false,
                        isInitialLoad = isInitialLoad
                )
                checkIfAllSectionsRendered(info)
            }
            LoadingStage.DISTRIBUTION -> {
                sectionDisplayHandler.displaySection(
                        R.id.sectionDistribution,
                        R.id.tvDistribution,
                        info.distribution,
                        shouldScroll = false,
                        isInitialLoad = isInitialLoad
                )
                checkIfAllSectionsRendered(info)
            }
            LoadingStage.HABITAT -> {
                sectionDisplayHandler.displaySection(
                        R.id.sectionHabitat,
                        R.id.tvHabitat,
                        info.habitat,
                        shouldScroll = false,
                        isInitialLoad = isInitialLoad
                )
                checkIfAllSectionsRendered(info)
            }
            LoadingStage.CONSERVATION -> {
                displayConservationStatus(info.conservationStatus, shouldScroll = false)
                checkIfAllSectionsRendered(info)
            }
            LoadingStage.COMPLETE -> {
                isInitialLoad = false
                taxonomyDisplayHandler.stopShimmer()
                taxonomyDisplayHandler.displayTaxonomyWaterfall(info)
                sectionDisplayHandler.displaySection(
                        R.id.sectionDescription,
                        R.id.tvDescription,
                        info.description,
                        shouldScroll = false,
                        isInitialLoad = false
                )
                sectionDisplayHandler.displaySection(
                        R.id.sectionCharacteristics,
                        R.id.tvCharacteristics,
                        info.characteristics,
                        shouldScroll = false,
                        isInitialLoad = false
                )
                sectionDisplayHandler.displaySection(
                        R.id.sectionDistribution,
                        R.id.tvDistribution,
                        info.distribution,
                        shouldScroll = false,
                        isInitialLoad = false
                )
                sectionDisplayHandler.displaySection(
                        R.id.sectionHabitat,
                        R.id.tvHabitat,
                        info.habitat,
                        shouldScroll = false,
                        isInitialLoad = false
                )
                displayConservationStatus(info.conservationStatus, shouldScroll = false)
                allSectionsRendered = true

                homeButtonHandler.setupShareButton(info, currentImageUri)
                homeButtonHandler.showShareButton()
                homeButtonHandler.setupCopyButton(info)

                if (info.confidence < 50.0) {
                    homeButtonHandler.showRetryButton()
                } else {
                    homeButtonHandler.hideRetryButton()
                }
            }
        }
    }

    fun onDestroy() {
        homeAnimationHandler.destroy()
        shimmerAnimationHandler.destroy()
        handlerScope.cancel()
    }

    /** Xóa toàn bộ views và reset trạng thái */
    private fun clearAllViews() {
        lastDisplayedCommonName = null
        confidenceDisplayHandler.clearState()
        taxonomyDisplayHandler.clearDisplayedRows()
        sectionDisplayHandler.clearRenderedSections()
        allSectionsRendered = false

        val viewsToHide =
                listOf<View>(
                        infoBinding.tvCommonName,
                        infoBinding.tvScientificName,
                        infoBinding.confidenceCard,
                        infoBinding.taxonomyContainer
                )
        viewsToHide.forEach { view ->
            view.visibility = View.GONE
            view.alpha = 0f
            view.translationY = 0f
        }
    }

    /** Kiểm tra xem tất cả sections đã được render chưa */
    private fun checkIfAllSectionsRendered(info: SpeciesInfo) {
        val sectionsWithContent = mutableSetOf<Int>()

        if (info.description.isNotEmpty()) sectionsWithContent.add(R.id.sectionDescription)
        if (info.characteristics.isNotEmpty()) sectionsWithContent.add(R.id.sectionCharacteristics)
        if (info.distribution.isNotEmpty()) sectionsWithContent.add(R.id.sectionDistribution)
        if (info.habitat.isNotEmpty()) sectionsWithContent.add(R.id.sectionHabitat)
        if (info.conservationStatus.isNotEmpty()) sectionsWithContent.add(R.id.sectionConservation)

        val allRendered =
                sectionsWithContent.all { sectionId ->
                    sectionDisplayHandler.isSectionRendered(sectionId)
                }

        if (allRendered && !allSectionsRendered) {
            allSectionsRendered = true

            if (!isInitialLoad) {
                homeButtonHandler.setupShareButton(info, currentImageUri)
                homeButtonHandler.showShareButton()
            }
        }
    }

    /** Hiển thị tên khoa học */
    private fun displayScientificName(info: SpeciesInfo) {
        infoBinding.tvScientificName.apply {
            textFormatter.setHtml(this, info.scientificName)
            homeAnimationHandler.slideAndFadeIn(this, duration = 500, delay = 100)
        }
        homeAnimationHandler.slideAndFadeIn(
                infoBinding.btnCopyScientificName,
                duration = 500,
                delay = 150
        )
    }

    /** Hiển thị tên thông thường */
    private fun displayCommonName(info: SpeciesInfo) {
        infoBinding.tvCommonName.let { view ->
            if (lastDisplayedCommonName == info.commonName &&
                            view.visibility == View.VISIBLE &&
                            view.alpha == 1f
            ) {
                return
            }

            if (info.commonName == "...") {
                view.text = "..."
                view.alpha = 0f
                view.setTextColor(Color.TRANSPARENT)
                lastDisplayedCommonName = "..."
            } else {
                view.setTextColor(ContextCompat.getColor(context, R.color.green_primary))

                val capitalizedCommonName = info.commonName.split(" ").joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }
                
                textFormatter.setHtml(view, capitalizedCommonName)

                if (lastDisplayedCommonName != info.commonName) {
                    homeAnimationHandler.slideAndFadeIn(view, duration = 600)
                } else if (view.visibility != View.VISIBLE) {
                    view.visibility = View.VISIBLE
                    view.alpha = 1f
                }
                lastDisplayedCommonName = info.commonName
            }
        }
    }

    /** Hiển thị trạng thái bảo tồn */
    private fun displayConservationStatus(status: String, shouldScroll: Boolean = true) {
        sectionDisplayHandler.displaySection(
                R.id.sectionConservation,
                R.id.tvConservationStatus,
                status,
                shouldScroll,
                isInitialLoad
        )
    }
}
