package com.nguyendevs.ecolens.handlers.home

import android.view.View
import android.view.animation.DecelerateInterpolator
import com.nguyendevs.ecolens.databinding.ItemCardSpeciesInfoBinding
import com.nguyendevs.ecolens.handlers.animations.HomeAnimationHandler
import com.nguyendevs.ecolens.handlers.animations.ShimmerAnimationHandler
import com.nguyendevs.ecolens.handlers.util.TextFormatter
import com.nguyendevs.ecolens.models.SpeciesInfo

/** Xử lý hiển thị thông tin phân loại học (taxonomy) với hiệu ứng waterfall. */
class TaxonomyDisplayHandler(
    private val binding: ItemCardSpeciesInfoBinding,
    private val homeAnimationHandler: HomeAnimationHandler,
    private val shimmerAnimationHandler: ShimmerAnimationHandler,
    private val textFormatter: TextFormatter
) {
    private val displayedRows = mutableSetOf<Int>()
    private val infoBinding
        get() = binding

    fun prepareTaxonomyContainer() {
        val container = infoBinding.taxonomyContainer
        container.visibility = View.VISIBLE
        container.alpha = 1f

        val rows =
                listOf(
                        infoBinding.rowKingdom,
                        infoBinding.rowPhylum,
                        infoBinding.rowClass,
                        infoBinding.rowOrder,
                        infoBinding.rowFamily,
                        infoBinding.rowGenus,
                        infoBinding.rowSpecies
                )
        rows.forEach { row ->
            row.apply {
                visibility = View.INVISIBLE
                alpha = 0f
                translationY = 0f
            }
        }

        shimmerAnimationHandler.startTaxonomyShimmer(container)
    }

    fun displayTaxonomyWaterfall(info: SpeciesInfo) {
        val container = infoBinding.taxonomyContainer
        container.visibility = View.VISIBLE
        container.alpha = 1f

        val rows =
                listOf(
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
        shimmerAnimationHandler.stopTaxonomyShimmer(infoBinding.taxonomyContainer)
    }

    fun clearDisplayedRows() {
        displayedRows.clear()

        val rows =
                listOf(
                        infoBinding.rowKingdom,
                        infoBinding.rowPhylum,
                        infoBinding.rowClass,
                        infoBinding.rowOrder,
                        infoBinding.rowFamily,
                        infoBinding.rowGenus,
                        infoBinding.rowSpecies
                )
        rows.forEach { row ->
            row.visibility = View.GONE
            row.alpha = 0f
            row.translationY = 0f
        }

        val textViews =
                listOf(
                        infoBinding.tvKingdom,
                        infoBinding.tvPhylum,
                        infoBinding.tvClass,
                        infoBinding.tvOrder,
                        infoBinding.tvFamily,
                        infoBinding.tvGenus,
                        infoBinding.tvSpecies
                )
        textViews.forEach { it.text = "" }
    }
}
