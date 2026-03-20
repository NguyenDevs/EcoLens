package com.nguyendevs.ecolens.handlers.animations

import android.content.Context
import android.content.res.ColorStateList
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.nguyendevs.ecolens.R

/** Quản lý animation chip, haptic feedback và visibility cho HistoryFragment. */
class HistoryAnimationHandler(private val context: Context) {

    /** Thực hiện haptic feedback xác nhận. */
    fun performConfirmFeedback(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    /** Cập nhật style chip theo trạng thái active/inactive. */
    fun updateChipStyle(chip: Chip, isActive: Boolean) {
        if (isActive) {
            setActiveChipStyle(chip)
        } else {
            setInactiveChipStyle(chip)
        }
    }

    /** Cập nhật style cho nhiều chip cùng lúc. */
    fun updateMultipleChips(vararg chips: Pair<Chip, Boolean>) {
        chips.forEach { (chip, isActive) ->
            updateChipStyle(chip, isActive)
        }
    }

    /** Áp dụng style active (màu primary) cho chip. */
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

    /** Áp dụng style inactive (màu surface) cho chip. */
    private fun setInactiveChipStyle(chip: Chip) {
        val surfaceColor = ContextCompat.getColor(context, R.color.surface)
        val secondaryTextColor = ContextCompat.getColor(context, R.color.text_secondary)
        val errorColor = ContextCompat.getColor(context, R.color.error)
        val strokeWidth = context.resources.displayMetrics.density * 1

        chip.apply {
            chipBackgroundColor = ColorStateList.valueOf(surfaceColor)
            setTextColor(secondaryTextColor)
            chipIconTint = ColorStateList.valueOf(secondaryTextColor)
            closeIconTint = ColorStateList.valueOf(errorColor)
            chipStrokeWidth = strokeWidth
        }
    }

    /** Thiết lập hiệu ứng ripple cho nút sort. */
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

    /** Ẩn dần view với animation fade out. */
    fun fadeOut(view: View, duration: Long = 300L) {
        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .withEndAction {
                view.visibility = View.GONE
            }
            .start()
    }

    /** Hiện dần view với animation fade in. */
    fun fadeIn(view: View, duration: Long = 300L) {
        view.alpha = 0f
        view.visibility = View.VISIBLE
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .start()
    }
}