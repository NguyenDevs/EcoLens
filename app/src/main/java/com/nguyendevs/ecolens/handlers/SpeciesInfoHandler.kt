package com.nguyendevs.ecolens.handlers

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.View
import androidx.core.content.ContextCompat
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ActivityMainBinding
import com.nguyendevs.ecolens.handlers.animation.AnimationHandler
import com.nguyendevs.ecolens.handlers.animation.ShimmerEffectHandler
import com.nguyendevs.ecolens.handlers.display.ConfidenceDisplayHandler
import com.nguyendevs.ecolens.handlers.display.SectionDisplayHandler
import com.nguyendevs.ecolens.handlers.display.TaxonomyDisplayHandler
import com.nguyendevs.ecolens.handlers.interaction.ButtonHandler
import com.nguyendevs.ecolens.handlers.util.TextFormatter
import com.nguyendevs.ecolens.model.LoadingStage
import com.nguyendevs.ecolens.model.SpeciesInfo
import kotlinx.coroutines.*

/**
 * Main coordinator cho việc hiển thị thông tin loài sinh vật
 * Điều phối các handler con để xử lý từng phần riêng biệt
 */
class SpeciesInfoHandler(
    private val context: Context,
    private val binding: ActivityMainBinding,
    onCopySuccess: (String) -> Unit,
    onRetryClick: () -> Unit
) {
    private val handlerScope = CoroutineScope(Dispatchers.Main + Job())
    private var isInitialLoad = true
    private var allSectionsRendered = false
    private var lastDisplayedCommonName: String? = null

    private val infoBinding get() = binding.homeContainer.speciesInfoCard

    // Các handler con
    private val animationHandler = AnimationHandler()
    private val shimmerEffectHandler = ShimmerEffectHandler(context)
    private val textFormatter = TextFormatter()
    private val taxonomyDisplayHandler = TaxonomyDisplayHandler(
        binding, animationHandler, shimmerEffectHandler, textFormatter
    )
    private val confidenceDisplayHandler = ConfidenceDisplayHandler(
        context, binding, animationHandler
    )
    private val sectionDisplayHandler = SectionDisplayHandler(binding, textFormatter)
    private val buttonHandler = ButtonHandler(
        context, binding, animationHandler, textFormatter, onCopySuccess, onRetryClick, handlerScope
    )

    /**
     * Hiển thị thông tin loài theo từng giai đoạn loading
     */
    fun displaySpeciesInfo(info: SpeciesInfo, imageUri: Uri?, stage: LoadingStage) {
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
                buttonHandler.setupCopyButton(info)
                buttonHandler.showCopyButton()
                buttonHandler.hideButtons()
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
                    R.id.sectionDescription, R.id.tvDescription, info.description,
                    shouldScroll = false, isInitialLoad = isInitialLoad
                )
                checkIfAllSectionsRendered(info, imageUri)
            }

            LoadingStage.CHARACTERISTICS -> {
                taxonomyDisplayHandler.stopShimmer()
                taxonomyDisplayHandler.displayTaxonomyWaterfall(info)
                sectionDisplayHandler.displaySection(
                    R.id.sectionCharacteristics, R.id.tvCharacteristics, info.characteristics,
                    shouldScroll = false, isInitialLoad = isInitialLoad
                )
                checkIfAllSectionsRendered(info, imageUri)
            }

            LoadingStage.DISTRIBUTION -> {
                sectionDisplayHandler.displaySection(
                    R.id.sectionDistribution, R.id.tvDistribution, info.distribution,
                    shouldScroll = false, isInitialLoad = isInitialLoad
                )
                checkIfAllSectionsRendered(info, imageUri)
            }

            LoadingStage.HABITAT -> {
                sectionDisplayHandler.displaySection(
                    R.id.sectionHabitat, R.id.tvHabitat, info.habitat,
                    shouldScroll = false, isInitialLoad = isInitialLoad
                )
                checkIfAllSectionsRendered(info, imageUri)
            }

            LoadingStage.CONSERVATION -> {
                displayConservationStatus(info.conservationStatus, shouldScroll = false)
                checkIfAllSectionsRendered(info, imageUri)
            }

            LoadingStage.COMPLETE -> {
                isInitialLoad = false
                taxonomyDisplayHandler.stopShimmer()
                taxonomyDisplayHandler.displayTaxonomyWaterfall(info)
                sectionDisplayHandler.displaySection(
                    R.id.sectionDescription, R.id.tvDescription, info.description,
                    shouldScroll = false, isInitialLoad = false
                )
                sectionDisplayHandler.displaySection(
                    R.id.sectionCharacteristics, R.id.tvCharacteristics, info.characteristics,
                    shouldScroll = false, isInitialLoad = false
                )
                sectionDisplayHandler.displaySection(
                    R.id.sectionDistribution, R.id.tvDistribution, info.distribution,
                    shouldScroll = false, isInitialLoad = false
                )
                sectionDisplayHandler.displaySection(
                    R.id.sectionHabitat, R.id.tvHabitat, info.habitat,
                    shouldScroll = false, isInitialLoad = false
                )
                displayConservationStatus(info.conservationStatus, shouldScroll = false)
                allSectionsRendered = true

                buttonHandler.setupShareButton(info, imageUri)
                buttonHandler.showShareButton()
                buttonHandler.setupCopyButton(info)

                if (info.confidence < 50.0) {
                    buttonHandler.showRetryButton()
                } else {
                    buttonHandler.hideRetryButton()
                }
            }
        }
    }

    fun onDestroy() {
        animationHandler.destroy()
        shimmerEffectHandler.destroy()
        handlerScope.cancel()
    }

    /**
     * Xóa toàn bộ views và reset trạng thái
     */
    private fun clearAllViews() {
        lastDisplayedCommonName = null
        confidenceDisplayHandler.clearState()
        taxonomyDisplayHandler.clearDisplayedRows()
        sectionDisplayHandler.clearRenderedSections()
        allSectionsRendered = false

        val viewsToHide = listOf<View>(
            infoBinding.tvCommonName, infoBinding.tvScientificName, infoBinding.confidenceCard,
            infoBinding.taxonomyContainer
        )
        viewsToHide.forEach { view ->
            view.visibility = View.GONE
            view.alpha = 0f
            view.translationY = 0f
        }
    }

    /**
     * Kiểm tra xem tất cả sections đã được render chưa
     */
    private fun checkIfAllSectionsRendered(info: SpeciesInfo, imageUri: Uri?) {
        val sectionsWithContent = mutableSetOf<Int>()

        if (info.description.isNotEmpty()) sectionsWithContent.add(R.id.sectionDescription)
        if (info.characteristics.isNotEmpty()) sectionsWithContent.add(R.id.sectionCharacteristics)
        if (info.distribution.isNotEmpty()) sectionsWithContent.add(R.id.sectionDistribution)
        if (info.habitat.isNotEmpty()) sectionsWithContent.add(R.id.sectionHabitat)
        if (info.conservationStatus.isNotEmpty()) sectionsWithContent.add(R.id.sectionConservation)

        val allRendered = sectionsWithContent.all { sectionId ->
            sectionDisplayHandler.isSectionRendered(sectionId)
        }

        if (allRendered && !allSectionsRendered) {
            allSectionsRendered = true

            if (!isInitialLoad) {
                buttonHandler.setupShareButton(info, imageUri)
                buttonHandler.showShareButton()
            }
        }
    }

    /**
     * Hiển thị tên khoa học
     */
    private fun displayScientificName(info: SpeciesInfo) {
        infoBinding.tvScientificName.apply {
            textFormatter.setHtml(this, info.scientificName)
            animationHandler.slideAndFadeIn(this, duration = 500, delay = 100)
        }
        animationHandler.slideAndFadeIn(infoBinding.btnCopyScientificName, duration = 500, delay = 150)
    }

    /**
     * Hiển thị tên thông thường
     */
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
                textFormatter.setHtml(view, info.commonName)

                if (lastDisplayedCommonName != info.commonName) {
                    animationHandler.slideAndFadeIn(view, duration = 600)
                } else if (view.visibility != View.VISIBLE) {
                    view.visibility = View.VISIBLE
                    view.alpha = 1f
                }
                lastDisplayedCommonName = info.commonName
            }
        }
    }

    /**
     * Hiển thị trạng thái bảo tồn
     */
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