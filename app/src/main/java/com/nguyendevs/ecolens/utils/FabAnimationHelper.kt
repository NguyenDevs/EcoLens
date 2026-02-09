package com.nguyendevs.ecolens.utils

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

object FabAnimationHelper {
    private const val CLICK_ANIMATION_DURATION = 100L
    private const val SCALE_DOWN_FACTOR = 0.9f

    fun animateClick(view: View, onAnimationEnd: () -> Unit) {
        view.animate()
                .scaleX(SCALE_DOWN_FACTOR)
                .scaleY(SCALE_DOWN_FACTOR)
                .setDuration(CLICK_ANIMATION_DURATION)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(CLICK_ANIMATION_DURATION)
                            .setInterpolator(AccelerateDecelerateInterpolator())
                            .withEndAction { onAnimationEnd() }
                            .start()
                }
                .start()
    }
}