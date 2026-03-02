package com.nguyendevs.ecolens.handlers.home

import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ScrollView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemCardSpeciesInfoBinding
import com.nguyendevs.ecolens.handlers.util.TextFormatter
import net.cachapa.expandablelayout.ExpandableLayout

/** Xử lý hiển thị các section thông tin chi tiết (mô tả, đặc điểm, phân bố...). */
class SectionDisplayHandler(
        private val binding: ItemCardSpeciesInfoBinding,
        private val textFormatter: TextFormatter
) {
    private val renderedSections = mutableSetOf<Int>()
    private val sectionsWithData = mutableSetOf<Int>()
    private val infoBinding
        get() = binding

    init {
        setupToggleListeners()
    }

    private fun setupToggleListeners() {
        val togglePairs =
                listOf(
                        Pair(
                                R.id.sectionDescription,
                                Triple(
                                        infoBinding.headerDescription,
                                        infoBinding.expandableDescription,
                                        infoBinding.ivDescriptionExpandIcon
                                )
                        ),
                        Pair(
                                R.id.sectionCharacteristics,
                                Triple(
                                        infoBinding.headerCharacteristics,
                                        infoBinding.expandableCharacteristics,
                                        infoBinding.ivCharacteristicsExpandIcon
                                )
                        ),
                        Pair(
                                R.id.sectionDistribution,
                                Triple(
                                        infoBinding.headerDistribution,
                                        infoBinding.expandableDistribution,
                                        infoBinding.ivDistributionExpandIcon
                                )
                        ),
                        Pair(
                                R.id.sectionHabitat,
                                Triple(
                                        infoBinding.headerHabitat,
                                        infoBinding.expandableHabitat,
                                        infoBinding.ivHabitatExpandIcon
                                )
                        ),
                        Pair(
                                R.id.sectionConservation,
                                Triple(
                                        infoBinding.headerConservation,
                                        infoBinding.expandableConservation,
                                        infoBinding.ivConservationExpandIcon
                                )
                        )
                )

        togglePairs.forEach { (sectionId, views) ->
            val (header, content, icon) = views
            header.setOnClickListener {
                if (sectionsWithData.contains(sectionId)) {
                    toggleSection(content, icon)
                }
            }
            icon.rotation = -90f
        }
    }

    private fun toggleSection(expandableLayout: ExpandableLayout, iconView: View) {
        if (expandableLayout.isExpanded) {
            expandableLayout.collapse()
            iconView.animate().rotation(-90f).setDuration(200).start()
        } else {
            expandableLayout.expand()
            iconView.animate().rotation(0f).setDuration(200).start()
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
        val expandableLayout =
                when (sectionId) {
                    R.id.sectionDescription -> infoBinding.expandableDescription
                    R.id.sectionCharacteristics -> infoBinding.expandableCharacteristics
                    R.id.sectionDistribution -> infoBinding.expandableDistribution
                    R.id.sectionHabitat -> infoBinding.expandableHabitat
                    R.id.sectionConservation -> infoBinding.expandableConservation
                    else -> null
                }

        if (text.isNotEmpty()) {
            sectionsWithData.add(sectionId)
            val trimmedText = text.trim()
            textView?.let { tv -> textFormatter.setHtml(tv, trimmedText) }

            section?.let { sectionView ->
                val wasAlreadyRendered = renderedSections.contains(sectionId)

                if (sectionView.visibility != View.VISIBLE) {
                    sectionView.visibility = View.VISIBLE
                    sectionView.alpha = 0f
                    sectionView.translationY = 15f

                    val delayIndex =
                            when (sectionId) {
                                R.id.sectionConservation -> 0
                                R.id.sectionHabitat -> 1
                                R.id.sectionDistribution -> 2
                                R.id.sectionCharacteristics -> 3
                                R.id.sectionDescription -> 4
                                else -> 0
                            }
                    val startDelay = 450L + delayIndex * 750L

                    expandableLayout?.collapse(false)
                    iconView?.rotation = -90f

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

                                if (expandableLayout != null && iconView != null) {
                                    expandableLayout.postDelayed(
                                            {
                                                if (!expandableLayout.isExpanded) {
                                                    toggleSection(expandableLayout, iconView)
                                                }
                                            },
                                            startDelay
                                    )
                                }
                            }
                            .start()
                } else {
                    renderedSections.add(sectionId)
                    val isCurrentlyExpanded = expandableLayout?.isExpanded ?: false
                    if (!isCurrentlyExpanded && expandableLayout != null && iconView != null) {
                        toggleSection(expandableLayout, iconView)
                    }
                }
            }
        } else {
            section?.let { sectionView ->
                if (sectionView.visibility != View.VISIBLE) {
                    sectionView.visibility = View.VISIBLE
                    sectionView.alpha = 0f
                    sectionView.translationY = 15f

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
        sectionsWithData.clear()

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
        icons.forEach { icon -> icon.rotation = -90f }

        val contents =
                listOf(
                        infoBinding.expandableDescription,
                        infoBinding.expandableCharacteristics,
                        infoBinding.expandableDistribution,
                        infoBinding.expandableHabitat,
                        infoBinding.expandableConservation
                )
        contents.forEach { content -> content.collapse(false) }
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
