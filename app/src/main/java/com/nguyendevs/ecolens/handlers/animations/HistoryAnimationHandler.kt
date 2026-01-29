package com.nguyendevs.ecolens.handlers.animations

import android.content.Context
import android.content.res.ColorStateList
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.nguyendevs.ecolens.R

/**
 * Animation handler cho HistoryFragment
 * - Chip style animations (category filters)
 * - Sort button ripple effects
 * - Haptic feedback
 * - View visibility animations
 */
class HistoryAnimationHandler(private val context: Context) {

    // ==================== HAPTIC FEEDBACK ====================

    fun performConfirmFeedback(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    // ==================== CHIP ANIMATIONS ====================

    /**
     * Update chip style dựa trên active state
     */
    fun updateChipStyle(chip: Chip, isActive: Boolean) {
        if (isActive) {
            setActiveChipStyle(chip)
        } else {
            setInactiveChipStyle(chip)
        }
    }

    /**
     * Batch update multiple chips cùng lúc
     */
    fun updateMultipleChips(vararg chips: Pair<Chip, Boolean>) {
        chips.forEach { (chip, isActive) ->
            updateChipStyle(chip, isActive)
        }
    }

    private fun setActiveChipStyle(chip: Chip) {
        val primaryColor = ContextCompat.getColor(context, R.color.primary)
        val whiteColor = ContextCompat.getColor(context, R.color.white)

        chip.apply {
            chipBackgroundColor = ColorStateList.valueOf(primaryColor)
            setTextColor(whiteColor)
            chipIconTint = ColorStateList.valueOf(whiteColor)
            closeIconTint = ColorStateList.valueOf(whiteColor)
            chipStrokeWidth = 0f
        }
    }

    private fun setInactiveChipStyle(chip: Chip) {
        val surfaceColor = ContextCompat.getColor(context, R.color.surface)
        val secondaryTextColor = ContextCompat.getColor(context, R.color.text_secondary)
        val errorColor = ContextCompat.getColor(context, R.color.error)
        val strokeWidth = context.resources.displayMetrics.density * 1 // 1dp

        chip.apply {
            chipBackgroundColor = ColorStateList.valueOf(surfaceColor)
            setTextColor(secondaryTextColor)
            chipIconTint = ColorStateList.valueOf(secondaryTextColor)
            closeIconTint = ColorStateList.valueOf(errorColor)
            chipStrokeWidth = strokeWidth
        }
    }

    // ==================== SORT BUTTON RIPPLE ====================

    /**
     * Setup ripple effect cho sort button
     */
    fun setupSortButtonRipple(chip: Chip) {
        val primary = ContextCompat.getColor(context, R.color.primary)
        val surface = ContextCompat.getColor(context, R.color.surface)
        val white = ContextCompat.getColor(context, R.color.white)
        val secondary = ContextCompat.getColor(context, R.color.text_secondary)

        val states = arrayOf(
            intArrayOf(android.R.attr.state_pressed),
            intArrayOf()
        )

        chip.apply {
            chipBackgroundColor = ColorStateList(states, intArrayOf(primary, surface))
            setTextColor(ColorStateList(states, intArrayOf(white, secondary)))
            chipIconTint = ColorStateList(states, intArrayOf(white, secondary))
            rippleColor = ColorStateList.valueOf(primary)
        }
    }

    // ==================== VIEW VISIBILITY ANIMATIONS ====================

    /**
     * Fade out view
     */
    fun fadeOut(view: View, duration: Long = 300L) {
        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .withEndAction {
                view.visibility = View.GONE
            }
            .start()
    }

    /**
     * Fade in view
     */
    fun fadeIn(view: View, duration: Long = 300L) {
        view.alpha = 0f
        view.visibility = View.VISIBLE
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .start()
    }
}