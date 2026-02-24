package com.nguyendevs.ecolens.handlers.home

import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ScrollView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemCardSpeciesInfoBinding
import com.nguyendevs.ecolens.handlers.util.TextFormatter

/** Xử lý hiển thị các section thông tin chi tiết (mô tả, đặc điểm, phân bố...). */
class SectionDisplayHandler(
        private val binding: ItemCardSpeciesInfoBinding,
        private val textFormatter: TextFormatter
) {
    private val renderedSections = mutableSetOf<Int>()
    private val expandedStates = mutableMapOf<Int, Boolean>()
    private val infoBinding
        get() = binding

    init {
        setupToggleListeners()
    }

    private fun setupToggleListeners() {
        val togglePairs =
                listOf(
                        Triple(
                                infoBinding.headerDescription,
                                infoBinding.tvDescription,
                                infoBinding.ivDescriptionExpandIcon
                        ),
                        Triple(
                                infoBinding.headerCharacteristics,
                                infoBinding.tvCharacteristics,
                                infoBinding.ivCharacteristicsExpandIcon
                        ),
                        Triple(
                                infoBinding.headerDistribution,
                                infoBinding.tvDistribution,
                                infoBinding.ivDistributionExpandIcon
                        ),
                        Triple(
                                infoBinding.headerHabitat,
                                infoBinding.tvHabitat,
                                infoBinding.ivHabitatExpandIcon
                        ),
                        Triple(
                                infoBinding.headerConservation,
                                infoBinding.tvConservationStatus,
                                infoBinding.ivConservationExpandIcon
                        )
                )

        togglePairs.forEach { (header, content, icon) ->
            expandedStates[header.id] = false
            header.setOnClickListener { toggleSection(header.id, content, icon) }
        }
    }

    private fun toggleSection(headerId: Int, contentView: View, iconView: View) {
        val isExpanded = expandedStates[headerId] ?: false
        val newState = !isExpanded
        expandedStates[headerId] = newState

        val rotation = if (newState) 180f else 0f
        iconView.animate()
                .rotation(rotation)
                .setDuration(300)
                .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                .start()

        if (newState) {
            contentView.visibility = View.VISIBLE
            contentView.alpha = 0f
            contentView.translationY = -20f
            contentView
                    .animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(250)
                    .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                    .withEndAction {}
                    .start()
        } else {
            contentView
                    .animate()
                    .alpha(0f)
                    .translationY(-30f)
                    .setDuration(250)
                    .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                    .withEndAction { contentView.visibility = View.GONE }
                    .start()
        }
    }

    fun displaySection(
            sectionId: Int,
            textViewId: Int,
            text: String,
            shouldScroll: Boolean = true,
            isInitialLoad: Boolean = false
    ) {
        val section =
                when (sectionId) {
                    R.id.sectionDescription -> infoBinding.sectionDescription
                    R.id.sectionCharacteristics -> infoBinding.sectionCharacteristics
                    R.id.sectionDistribution -> infoBinding.sectionDistribution
                    R.id.sectionHabitat -> infoBinding.sectionHabitat
                    R.id.sectionConservation -> infoBinding.sectionConservation
                    else -> null
                }
        val textView =
                when (textViewId) {
                    R.id.tvDescription -> infoBinding.tvDescription
                    R.id.tvCharacteristics -> infoBinding.tvCharacteristics
                    R.id.tvDistribution -> infoBinding.tvDistribution
                    R.id.tvHabitat -> infoBinding.tvHabitat
                    R.id.tvConservationStatus -> infoBinding.tvConservationStatus
                    else -> null
                }

        val iconView =
                when (sectionId) {
                    R.id.sectionDescription -> infoBinding.ivDescriptionExpandIcon
                    R.id.sectionCharacteristics -> infoBinding.ivCharacteristicsExpandIcon
                    R.id.sectionDistribution -> infoBinding.ivDistributionExpandIcon
                    R.id.sectionHabitat -> infoBinding.ivHabitatExpandIcon
                    R.id.sectionConservation -> infoBinding.ivConservationExpandIcon
                    else -> null
                }
        val headerId =
                when (sectionId) {
                    R.id.sectionDescription -> infoBinding.headerDescription.id
                    R.id.sectionCharacteristics -> infoBinding.headerCharacteristics.id
                    R.id.sectionDistribution -> infoBinding.headerDistribution.id
                    R.id.sectionHabitat -> infoBinding.headerHabitat.id
                    R.id.sectionConservation -> infoBinding.headerConservation.id
                    else -> null
                }

        if (text.isNotEmpty()) {
            val trimmedText = text.trim()
            textView?.let { tv -> textFormatter.setHtml(tv, trimmedText) }

            section?.let { sectionView ->
                val wasAlreadyRendered = renderedSections.contains(sectionId)

                if (sectionView.visibility != View.VISIBLE) {
                    sectionView.visibility = View.VISIBLE
                    sectionView.alpha = 0f
                    sectionView.translationY = 15f

                    // Initially hide content
                    textView?.visibility = View.GONE
                    iconView?.rotation = 0f
                    if (headerId != null) {
                        expandedStates[headerId] = false
                    }

                    // Calculate delay based on section index for a cascading effect
                    val delayIndex =
                            when (sectionId) {
                                R.id.sectionDescription -> 0
                                R.id.sectionCharacteristics -> 1
                                R.id.sectionDistribution -> 2
                                R.id.sectionHabitat -> 3
                                R.id.sectionConservation -> 4
                                else -> 0
                            }
                    val startDelay = delayIndex * 500L

                    sectionView
                            .animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(450)
                            .setInterpolator(DecelerateInterpolator())
                            .withEndAction {
                                if (!wasAlreadyRendered && shouldScroll && !isInitialLoad) {
                                    smoothScrollToView(sectionView)
                                }
                                renderedSections.add(sectionId)

                                // Expand the section with delay
                                if (headerId != null && textView != null && iconView != null) {
                                    textView.postDelayed(
                                            {
                                                val isCurrentlyExpanded =
                                                        expandedStates[headerId] ?: false
                                                if (!isCurrentlyExpanded) {
                                                    toggleSection(headerId, textView, iconView)
                                                }
                                            },
                                            startDelay
                                    )
                                }
                            }
                            .start()
                } else {
                    renderedSections.add(sectionId)
                    // If already visible but content is collapsed, ensure we expand it
                    val isCurrentlyExpanded = headerId?.let { expandedStates[it] } ?: false
                    if (!isCurrentlyExpanded &&
                                    headerId != null &&
                                    textView != null &&
                                    iconView != null
                    ) {
                        toggleSection(headerId, textView, iconView)
                    }
                }
            }
        } else {
            section?.let { sectionView ->
                if (sectionView.visibility != View.VISIBLE) {
                    sectionView.visibility = View.VISIBLE
                    sectionView.alpha = 0f
                    sectionView.translationY = 15f

                    textView?.visibility = View.GONE
                    iconView?.rotation = 0f
                    if (headerId != null) {
                        expandedStates[headerId] = false
                    }

                    sectionView
                            .animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(450)
                            .setInterpolator(DecelerateInterpolator())
                            .withEndAction { renderedSections.add(sectionId) }
                            .start()
                } else {
                    renderedSections.add(sectionId)
                }
            }
        }
    }

    fun hideSection(sectionId: Int) {
        val section =
                when (sectionId) {
                    R.id.sectionDescription -> infoBinding.sectionDescription
                    R.id.sectionCharacteristics -> infoBinding.sectionCharacteristics
                    R.id.sectionDistribution -> infoBinding.sectionDistribution
                    R.id.sectionHabitat -> infoBinding.sectionHabitat
                    R.id.sectionConservation -> infoBinding.sectionConservation
                    else -> null
                }
        section?.visibility = View.GONE
        renderedSections.add(sectionId)
    }

    fun isSectionRendered(sectionId: Int): Boolean {
        return renderedSections.contains(sectionId)
    }

    fun clearRenderedSections() {
        renderedSections.clear()

        val sections =
                listOf(
                        infoBinding.sectionDescription,
                        infoBinding.sectionCharacteristics,
                        infoBinding.sectionDistribution,
                        infoBinding.sectionHabitat,
                        infoBinding.sectionConservation
                )
        sections.forEach { section ->
            section.visibility = View.GONE
            section.alpha = 0f
            section.translationY = 0f
        }

        val icons =
                listOf(
                        infoBinding.ivDescriptionExpandIcon,
                        infoBinding.ivCharacteristicsExpandIcon,
                        infoBinding.ivDistributionExpandIcon,
                        infoBinding.ivHabitatExpandIcon,
                        infoBinding.ivConservationExpandIcon
                )
        icons.forEach { icon -> icon.rotation = 0f }

        val contents =
                listOf(
                        infoBinding.tvDescription,
                        infoBinding.tvCharacteristics,
                        infoBinding.tvDistribution,
                        infoBinding.tvHabitat,
                        infoBinding.tvConservationStatus
                )
        contents.forEach { content ->
            content.visibility = View.GONE
            content.alpha = 0f
            content.translationY = 0f
        }
        expandedStates.keys.forEach { key -> expandedStates[key] = false }
    }

    private fun smoothScrollToView(view: View) {
        view.post {
            val scrollView = findScrollView(view)
            scrollView?.let { sv ->
                val scrollY = view.top + view.height - sv.height + sv.paddingBottom + 100
                if (scrollY > sv.scrollY) {
                    sv.smoothScrollTo(0, scrollY)
                }
            }
        }
    }

    private fun findScrollView(view: View): ScrollView? {
        var parent = view.parent
        while (parent != null) {
            if (parent is ScrollView) {
                return parent
            }
            parent = parent.parent
        }
        return null
    }
}
