package com.nguyendevs.ecolens.utils

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

/** Lớp hỗ trợ tạo hiệu ứng nhấn tương tác cho nút ấn nổi. */
object FabAnimationHelper {
    private const val CLICK_ANIMATION_DURATION = 100L
    private const val SCALE_DOWN_FACTOR = 0.9f

    /** Kích hoạt hiệu ứng thu phóng lò xo khi nhấn nút mượt mà. */
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