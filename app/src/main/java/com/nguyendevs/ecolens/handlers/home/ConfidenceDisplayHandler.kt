package com.nguyendevs.ecolens.handlers.home

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.core.content.ContextCompat
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemCardSpeciesInfoBinding
import com.nguyendevs.ecolens.handlers.animations.HomeAnimationHandler
import com.nguyendevs.ecolens.models.SpeciesInfo
import com.robinhood.ticker.TickerUtils
import com.robinhood.ticker.TickerView

/** Xử lý hiển thị độ tin cậy (confidence) của kết quả nhận dạng loài. */
class ConfidenceDisplayHandler(
        private val context: Context,
        private val binding: ItemCardSpeciesInfoBinding,
        private val homeAnimationHandler: HomeAnimationHandler
) {
    private var lastConfidenceValue: String? = null
    private var colorAnimator: ValueAnimator? = null
    private val infoBinding
        get() = binding

    init {
        setupTickerView()
    }

    private fun setupTickerView() {
        binding.tvConfidence.setCharacterLists(TickerUtils.provideNumberList())
    }

    @SuppressLint("StringFormatInvalid")
    fun displayConfidence(info: SpeciesInfo, isWaiting: Boolean) {
        val tvConfidence = infoBinding.tvConfidence
        val confidenceCard = infoBinding.confidenceCard
        val iconConfidence = infoBinding.iconConfidence

        if (isWaiting) {
            lastConfidenceValue = "loading"

            // Set text instantly without animation for the loading placeholder
            tvConfidence.setText(context.getString(R.string.confidence_format, "--.--"), false)

            iconConfidence.setImageResource(R.drawable.ic_rotate)
            iconConfidence.imageTintList =
                    ContextCompat.getColorStateList(context, R.color.text_secondary)
            
            // Set static color for waiting state as requested
            confidenceCard.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.surface_tint)
            )
            tvConfidence.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))

            confidenceCard.let {
                if (it.visibility != View.VISIBLE) {
                    it.visibility = View.VISIBLE
                    it.alpha = 1f
                }
            }

            // Restore rotation during recognition phase
            homeAnimationHandler.startConfidenceRotation(iconConfidence)
        } else {
            homeAnimationHandler.stopConfidenceRotation(iconConfidence)

            val confidenceValue = info.confidence.coerceIn(0.0, 100.0)
            val confidencePercent = String.format("%.2f", confidenceValue)
            val newText = context.getString(R.string.confidence_format, confidencePercent)

            if (lastConfidenceValue == newText &&
                            confidenceCard.visibility == View.VISIBLE &&
                            confidenceCard.alpha == 1f
            ) {
                return
            }

            // If moving from loading to result, start from 00.00
            if (lastConfidenceValue == "loading") {
                tvConfidence.setText(context.getString(R.string.confidence_format, "00.00"), false)
            }
            tvConfidence.setText(newText)

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
            
            // Smooth color transition
            animateBackgroundColor(ContextCompat.getColor(context, bg))
            tvConfidence.setTextColor(ContextCompat.getColor(context, text))

            confidenceCard.let { card ->
                if (card.visibility != View.VISIBLE || card.alpha < 1f) {
                    card.visibility = View.VISIBLE
                    card.alpha = 1f
                    card.scaleX = 1f
                    card.scaleY = 1f
                }
            }

            lastConfidenceValue = newText
        }
    }

    private fun animateBackgroundColor(targetColor: Int) {
        val currentColor = (infoBinding.confidenceCard.background as? ColorDrawable)?.color
                ?: ContextCompat.getColor(context, R.color.surface_tint)
        
        colorAnimator?.cancel()
        colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), currentColor, targetColor).apply {
            duration = 500
            addUpdateListener { animator ->
                infoBinding.confidenceCard.setCardBackgroundColor(animator.animatedValue as Int)
            }
            start()
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
