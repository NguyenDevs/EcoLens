package com.nguyendevs.ecolens.handlers.display

import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ScrollView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ActivityMainBinding
import com.nguyendevs.ecolens.handlers.util.TextFormatter

class SectionDisplayHandler(
    private val binding: ActivityMainBinding,
    private val textFormatter: TextFormatter
) {
    private val renderedSections = mutableSetOf<Int>()
    private val infoBinding get() = binding.homeContainer.speciesInfoCard

    fun displaySection(
        sectionId: Int,
        textViewId: Int,
        text: String,
        shouldScroll: Boolean = true,
        isInitialLoad: Boolean = false
    ) {
        val section = when (sectionId) {
            R.id.sectionDescription -> infoBinding.sectionDescription
            R.id.sectionCharacteristics -> infoBinding.sectionCharacteristics
            R.id.sectionDistribution -> infoBinding.sectionDistribution
            R.id.sectionHabitat -> infoBinding.sectionHabitat
            R.id.sectionConservation -> infoBinding.sectionConservation
            else -> null
        }
        val textView = when (textViewId) {
            R.id.tvDescription -> infoBinding.tvDescription
            R.id.tvCharacteristics -> infoBinding.tvCharacteristics
            R.id.tvDistribution -> infoBinding.tvDistribution
            R.id.tvHabitat -> infoBinding.tvHabitat
            R.id.tvConservationStatus -> infoBinding.tvConservationStatus
            else -> null
        }

        if (text.isNotEmpty()) {
            val trimmedText = text.trim()
            textView?.let { tv ->
                textFormatter.setHtml(tv, trimmedText)
            }

            section?.let { sectionView ->
                val wasAlreadyRendered = renderedSections.contains(sectionId)

                if (sectionView.visibility != View.VISIBLE) {
                    sectionView.visibility = View.VISIBLE
                    sectionView.alpha = 0f
                    sectionView.translationY = 15f

                    sectionView.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(450)
                        .setInterpolator(DecelerateInterpolator())
                        .withEndAction {
                            if (!wasAlreadyRendered && shouldScroll && !isInitialLoad) {
                                smoothScrollToView(sectionView)
                            }
                            renderedSections.add(sectionId)
                        }
                        .start()
                } else {
                    renderedSections.add(sectionId)
                }
            }
        } else {
            section?.visibility = View.GONE
            renderedSections.add(sectionId)
        }
    }

    fun isSectionRendered(sectionId: Int): Boolean {
        return renderedSections.contains(sectionId)
    }

    fun clearRenderedSections() {
        renderedSections.clear()

        val sections = listOf(
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