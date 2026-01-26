package com.nguyendevs.ecolens.handlers.display

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemCardSpeciesInfoBinding
import com.nguyendevs.ecolens.handlers.animation.AnimationHandler
import com.nguyendevs.ecolens.model.SpeciesInfo

/** Xử lý hiển thị độ tin cậy (confidence) của kết quả nhận dạng loài. */
class ConfidenceDisplayHandler(
        private val context: Context,
        private val binding: ItemCardSpeciesInfoBinding,
        private val animationHandler: AnimationHandler
) {
    private var lastConfidenceValue: String? = null
    private val infoBinding
        get() = binding

    @SuppressLint("StringFormatInvalid")
    fun displayConfidence(info: SpeciesInfo, isWaiting: Boolean) {
        val tvConfidence = infoBinding.tvConfidence
        val confidenceCard = infoBinding.confidenceCard
        val iconConfidence = infoBinding.iconConfidence

        if (isWaiting) {
            lastConfidenceValue = "loading"

            tvConfidence.text = context.getString(R.string.confidence, "...%")
            tvConfidence.textSize = 13f

            iconConfidence.setImageResource(R.drawable.ic_rotate)
            iconConfidence.imageTintList =
                    ContextCompat.getColorStateList(context, R.color.text_secondary)
            confidenceCard.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.gray_light)
            )
            tvConfidence.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))

            confidenceCard.let {
                if (it.visibility != View.VISIBLE) {
                    it.visibility = View.VISIBLE
                    it.alpha = 1f
                }
            }

            animationHandler.startConfidenceRotation(iconConfidence)
        } else {
            animationHandler.stopConfidenceRotation(iconConfidence)

            val confidenceValue = info.confidence.coerceIn(0.0, 100.0)
            val confidencePercent = String.format("%.2f", confidenceValue)
            val newText = context.getString(R.string.confidence_format, confidencePercent)

            if (lastConfidenceValue == newText &&
                            confidenceCard.visibility == View.VISIBLE &&
                            confidenceCard.alpha == 1f
            ) {
                return
            }

            tvConfidence.text = newText

            val (icon, tint, bg, text) =
                    when {
                        confidenceValue >= 50f ->
                                Quadruple(
                                        R.drawable.ic_check_circle,
                                        R.color.confidence_high,
                                        R.color.confidence_high_bg,
                                        R.color.confidence_high_text
                                )
                        confidenceValue >= 25f ->
                                Quadruple(
                                        R.drawable.ic_check_warning_circle,
                                        R.color.confidence_medium,
                                        R.color.confidence_medium_bg,
                                        R.color.confidence_medium_text
                                )
                        else ->
                                Quadruple(
                                        R.drawable.ic_check_not_circle,
                                        R.color.confidence_low,
                                        R.color.confidence_low_bg,
                                        R.color.confidence_low_text
                                )
                    }

            iconConfidence.setImageResource(icon)
            iconConfidence.imageTintList = ContextCompat.getColorStateList(context, tint)
            confidenceCard.setCardBackgroundColor(ContextCompat.getColor(context, bg))
            tvConfidence.setTextColor(ContextCompat.getColor(context, text))

            confidenceCard.let { card ->
                if (lastConfidenceValue != newText) {
                    animationHandler.popInAnimation(card)
                } else {
                    if (card.visibility != View.VISIBLE || card.alpha < 1f) {
                        card.visibility = View.VISIBLE
                        card.alpha = 1f
                        card.scaleX = 1f
                        card.scaleY = 1f
                    }
                }
            }

            lastConfidenceValue = newText
        }
    }

    fun clearState() {
        lastConfidenceValue = null
    }

    /** Data class chứa 4 giá trị cho việc styling confidence card. */
    data class Quadruple<out A, out B, out C, out D>(
            val first: A,
            val second: B,
            val third: C,
            val fourth: D
    )
}
