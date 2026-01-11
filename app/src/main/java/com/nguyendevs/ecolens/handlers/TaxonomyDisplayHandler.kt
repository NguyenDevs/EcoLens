package com.nguyendevs.ecolens.handlers

import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import com.nguyendevs.ecolens.databinding.ActivityMainBinding
import com.nguyendevs.ecolens.model.SpeciesInfo

class TaxonomyDisplayHandler(
    private val binding: ActivityMainBinding,
    private val animationHandler: AnimationHandler,
    private val shimmerEffectHandler: ShimmerEffectHandler,
    private val textFormatter: TextFormatter
) {
    private val displayedRows = mutableSetOf<Int>()
    private val infoBinding get() = binding.homeContainer.speciesInfoCard

    fun prepareTaxonomyContainer() {
        val container = infoBinding.taxonomyContainer
        container.visibility = View.VISIBLE
        container.alpha = 1f

        shimmerEffectHandler.startTaxonomyShimmer(container)

        val rows = listOf(
            infoBinding.rowKingdom, infoBinding.rowPhylum, infoBinding.rowClass,
            infoBinding.rowOrder, infoBinding.rowFamily, infoBinding.rowGenus, infoBinding.rowSpecies
        )
        rows.forEach { row ->
            row.apply {
                visibility = View.INVISIBLE
                alpha = 0f
                translationY = 0f
            }
        }
    }

    fun displayTaxonomyWaterfall(info: SpeciesInfo) {
        val container = infoBinding.taxonomyContainer
        container.visibility = View.VISIBLE
        container.alpha = 1f

        val rows = listOf(
            Triple(infoBinding.rowKingdom, infoBinding.tvKingdom, info.kingdom),
            Triple(infoBinding.rowPhylum, infoBinding.tvPhylum, info.phylum),
            Triple(infoBinding.rowClass, infoBinding.tvClass, info.className),
            Triple(infoBinding.rowOrder, infoBinding.tvOrder, info.taxorder),
            Triple(infoBinding.rowFamily, infoBinding.tvFamily, info.family),
            Triple(infoBinding.rowGenus, infoBinding.tvGenus, info.genus),
            Triple(infoBinding.rowSpecies, infoBinding.tvSpecies, info.species)
        )

        rows.forEach { (rowView, textView, text) ->
            val hasData = text.isNotEmpty() && text != "..." && text != "N/A"
            val rowId = rowView.id

            if (hasData) {
                if (!displayedRows.contains(rowId)) {
                    textFormatter.setHtml(textView, text)

                    rowView.visibility = View.VISIBLE
                    rowView.alpha = 0f
                    rowView.translationY = -10f

                    rowView.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(300)
                        .setInterpolator(DecelerateInterpolator())
                        .start()

                    displayedRows.add(rowId)
                }
            } else {
                if (!displayedRows.contains(rowId)) {
                    rowView.visibility = View.INVISIBLE
                    rowView.alpha = 0f
                }
            }
        }
    }

    fun stopShimmer() {
        shimmerEffectHandler.stopTaxonomyShimmer(infoBinding.taxonomyContainer)
    }

    fun clearDisplayedRows() {
        displayedRows.clear()

        val rows = listOf(
            infoBinding.rowKingdom, infoBinding.rowPhylum, infoBinding.rowClass,
            infoBinding.rowOrder, infoBinding.rowFamily, infoBinding.rowGenus, infoBinding.rowSpecies
        )
        rows.forEach { row ->
            row.visibility = View.GONE
            row.alpha = 0f
            row.translationY = 0f
        }

        val textViews = listOf(
            infoBinding.tvKingdom, infoBinding.tvPhylum, infoBinding.tvClass, infoBinding.tvOrder,
            infoBinding.tvFamily, infoBinding.tvGenus, infoBinding.tvSpecies
        )
        textViews.forEach { it.text = "" }
    }
}