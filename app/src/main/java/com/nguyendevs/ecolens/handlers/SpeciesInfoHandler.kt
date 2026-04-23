package com.nguyendevs.ecolens.handlers

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.View
import androidx.core.content.ContextCompat
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.adapters.SpeciesImageAdapter
import com.nguyendevs.ecolens.databinding.ItemCardSpeciesInfoBinding
import com.nguyendevs.ecolens.handlers.animations.HomeAnimationHandler
import com.nguyendevs.ecolens.handlers.animations.ShimmerAnimationHandler
import com.nguyendevs.ecolens.handlers.home.ConfidenceDisplayHandler
import com.nguyendevs.ecolens.handlers.home.HomeButtonHandler
import com.nguyendevs.ecolens.handlers.home.SectionDisplayHandler
import com.nguyendevs.ecolens.handlers.home.TaxonomyDisplayHandler
import com.nguyendevs.ecolens.handlers.util.TextFormatter
import com.nguyendevs.ecolens.models.LoadingStage
import com.nguyendevs.ecolens.models.SpeciesInfo
import java.util.Locale
import kotlinx.coroutines.*

/** Coordinator điều phối hiển thị thẻ thông tin chi tiết các loài sinh vật. */
class SpeciesInfoHandler(
        private val context: Context,
        private val binding: ItemCardSpeciesInfoBinding,
        onCopySuccess: (String) -> Unit,
        onRetryClick: () -> Unit
) {
    private val handlerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isInitialLoad = true
    private var allSectionsRendered = false
    private var lastDisplayedCommonName: String? = null
    private var currentImageUri: Uri? = null

    private val infoBinding
        get() = binding

    /** Gán URI của ảnh để sử dụng cho tính năng chia sẻ. */
    fun setImageUri(uri: Uri?) {
        currentImageUri = uri
    }

    private val homeAnimationHandler = HomeAnimationHandler()
    private val shimmerAnimationHandler = ShimmerAnimationHandler(context)
    private val textFormatter = TextFormatter()
    private val taxonomyDisplayHandler =
            TaxonomyDisplayHandler(
                    binding,
                    homeAnimationHandler,
                    shimmerAnimationHandler,
                    textFormatter
            )
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

    private val imagesAdapter = SpeciesImageAdapter()

    init {
        binding.rvSpeciesImages.adapter = imagesAdapter
        binding.headerImages.setOnClickListener { toggleImagesExpand() }
    }

    private fun toggleImagesExpand() {
        if (binding.expandableImages.isExpanded) {
            binding.expandableImages.collapse()
            binding.ivImagesExpandIcon.animate().rotation(-90f).setDuration(200).start()
        } else {
            binding.expandableImages.expand()
            binding.ivImagesExpandIcon.animate().rotation(0f).setDuration(200).start()
        }
    }

    /** Hiển thị thông tin sinh vật tùy chỉnh theo từng giai đoạn tải (LoadingStage). */
    fun displaySpeciesInfo(
            info: SpeciesInfo,
            stage: LoadingStage,
            isTaxonomyTranslating: Boolean = false,
            images: List<String> = emptyList(),
            historyId: Int? = null,
            isFavorite: Boolean = false,
            onFavoriteToggle: (Int, Boolean) -> Unit = { _, _ -> }
    ) {
        if (stage == LoadingStage.NONE) {
            handlerScope.coroutineContext.cancelChildren()
            clearAllViews()
            imagesAdapter.submitList(emptyList())
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

        taxonomyDisplayHandler.setTaxonomyTranslating(isTaxonomyTranslating)

        if (images.isNotEmpty()) {
            if (binding.sectionImages.visibility != View.VISIBLE) {
                homeAnimationHandler.slideAndFadeIn(binding.sectionImages, duration = 400)
                binding.expandableImages.collapse(false)
                binding.ivImagesExpandIcon.rotation = -90f
            }
            imagesAdapter.submitList(images)
        } else if (stage == LoadingStage.NONE) {
            binding.sectionImages.visibility = View.GONE
        }

        when (stage) {
            LoadingStage.NONE -> {
                handlerScope.coroutineContext.cancelChildren()
                clearAllViews()
                imagesAdapter.submitList(emptyList())
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

                sectionDisplayHandler.displaySection(
                        R.id.sectionDescription,
                        R.id.tvDescription,
                        "",
                        isInitialLoad = isInitialLoad
                )
                sectionDisplayHandler.displaySection(
                        R.id.sectionCharacteristics,
                        R.id.tvCharacteristics,
                        "",
                        isInitialLoad = isInitialLoad
                )
                sectionDisplayHandler.displaySection(
                        R.id.sectionDistribution,
                        R.id.tvDistribution,
                        "",
                        isInitialLoad = isInitialLoad
                )
                sectionDisplayHandler.displaySection(
                        R.id.sectionHabitat,
                        R.id.tvHabitat,
                        "",
                        isInitialLoad = isInitialLoad
                )

                val sharedPref = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                val isIucnEnabled = sharedPref.getBoolean("iucn_mode", true)

                if (isIucnEnabled) {
                    sectionDisplayHandler.displaySection(
                            R.id.sectionConservation,
                            R.id.tvConservationStatus,
                            "",
                            isInitialLoad = isInitialLoad
                    )
                }
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

                if (!binding.expandableImages.isExpanded && imagesAdapter.itemCount > 0) {
                    binding.headerImages.postDelayed(
                            { toggleImagesExpand() },
                            2500L
                    )
                }

                homeButtonHandler.setupShareButton(info, currentImageUri)
                homeButtonHandler.showShareButton()
                homeButtonHandler.setupCopyButton(info)
                homeButtonHandler.setupFavoriteButton(historyId, isFavorite, onFavoriteToggle)

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

    /** Dọn dẹp sạch giao diện và đưa các thẻ về trạng thái ẩn. */
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
                        infoBinding.taxonomyContainer,
                        infoBinding.sectionImages
                )
        viewsToHide.forEach { view ->
            view.visibility = View.GONE
            view.alpha = 0f
            view.translationY = 0f
        }
    }

    /** Quét và tính toán xem toàn bộ dữ liệu đã được gán lên lưới giao diện chưa. */
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

    /** Hiển thị tên khoa học với hiệu ứng slide & fade-in. */
    private fun displayScientificName(info: SpeciesInfo) {
        infoBinding.tvScientificName.apply {
            textFormatter.setHtml(this, info.scientificName)
            homeAnimationHandler.slideAndFadeIn(this, duration = 800, delay = 100)
        }
        homeAnimationHandler.slideAndFadeIn(
                infoBinding.btnCopyScientificName,
                duration = 800,
                delay = 150
        )
    }

    /** Chỉnh màu chủ đạo và hiển thị tên thông thường với hiệu ứng nổi bật. */
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

                val capitalizedCommonName =
                        info.commonName.split(" ").joinToString(" ") { word ->
                            word.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault())
                                else it.toString()
                            }
                        }

                textFormatter.setHtml(view, capitalizedCommonName)

                if (lastDisplayedCommonName != info.commonName) {
                    homeAnimationHandler.slideAndFadeIn(view, duration = 1000)
                } else if (view.visibility != View.VISIBLE) {
                    view.visibility = View.VISIBLE
                    view.alpha = 1f
                }
                lastDisplayedCommonName = info.commonName
            }
        }
    }

    /** Hiển thị thẻ tình trạng bảo tồn nếu tính năng IUCN hoặc VN Red List được bật. */
    private fun displayConservationStatus(status: String, shouldScroll: Boolean = true) {
        val sharedPref = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isIucnEnabled = sharedPref.getBoolean("iucn_mode", true)
        val isVnRedListEnabled = sharedPref.getBoolean("vnredlist_mode", true)

        if (!isIucnEnabled && !isVnRedListEnabled) {
            infoBinding.tvConservationDisabledMessage.visibility = View.VISIBLE
            infoBinding.tvConservationDisabledMessage.text = context.getString(R.string.conservation_disabled_message)
            infoBinding.tvConservationStatus.visibility = View.GONE
            infoBinding.dividerConservation.visibility = View.GONE
            infoBinding.tvVnRedListStatus.visibility = View.GONE
            
            sectionDisplayHandler.displaySection(
                    R.id.sectionConservation,
                    R.id.tvConservationDisabledMessage,
                    context.getString(R.string.conservation_disabled_message),
                    shouldScroll,
                    isInitialLoad
            )
        } else {
            infoBinding.tvConservationDisabledMessage.visibility = View.GONE
            
            infoBinding.tvConservationStatus.visibility = View.VISIBLE
            infoBinding.tvConservationStatus.text = if (isIucnEnabled) status else context.getString(R.string.iucn_disabled_message)
            
            infoBinding.dividerConservation.visibility = View.VISIBLE
            
            infoBinding.tvVnRedListStatus.visibility = View.VISIBLE
            infoBinding.tvVnRedListStatus.text = if (isVnRedListEnabled) status else context.getString(R.string.vnredlist_disabled_message)

            sectionDisplayHandler.displaySection(
                    R.id.sectionConservation,
                    R.id.tvConservationStatus,
                    status,
                    shouldScroll,
                    isInitialLoad
            )
        }
    }
}
