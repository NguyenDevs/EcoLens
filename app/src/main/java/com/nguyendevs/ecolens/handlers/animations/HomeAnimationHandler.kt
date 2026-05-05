package com.nguyendevs.ecolens.handlers.animations

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator

/** Quản lý các hiệu ứng animation cho Views trong màn hình chính. */
class HomeAnimationHandler {
    private var confidenceRotationAnimator: ObjectAnimator? = null

    /** Trượt view từ dưới lên kết hợp fade-in. */
    fun slideAndFadeIn(view: View, duration: Long = 200, delay: Long = 0) {
        if (view.visibility == View.VISIBLE && view.alpha == 1f) return

        view.alpha = 0f
        view.translationY = 30f
        view.visibility = View.VISIBLE

        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(duration)
                .setStartDelay(delay)
                .setInterpolator(DecelerateInterpolator(2.2f))
                .start()
    }

    /** Tăng dần độ mờ của view đến 1. */
    fun fadeIn(view: View, durationMs: Long) {
        view.animate()
                .alpha(1f)
                .setDuration(durationMs)
                .setInterpolator(DecelerateInterpolator())
                .start()
    }

    /** Bắt đầu animation xoay vòng liên tục cho biểu tượng confidence. */
    fun startConfidenceRotation(view: View) {
        if (confidenceRotationAnimator != null) return

        confidenceRotationAnimator =
                ObjectAnimator.ofFloat(view, "rotation", 0f, 360f).apply {
                    duration = 1000
                    repeatCount = ObjectAnimator.INFINITE
                    interpolator = LinearInterpolator()
                    start()
                }
    }

    /** Dừng animation xoay và reset góc về 0. */
    fun stopConfidenceRotation(view: View) {
        confidenceRotationAnimator?.cancel()
        confidenceRotationAnimator = null
        view.rotation = 0f
    }

    /** Phóng to view từ 80% kết hợp fade-in. */
    fun scaleInAnimation(view: View, duration: Long = 200, delay: Long = 0) {
        view.apply {
            visibility = View.VISIBLE
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
        }
    }

    /** Animation bật ra với hiệu ứng overshoot từ 50%. */
    fun popInAnimation(view: View, duration: Long = 200) {
        view.apply {
            visibility = View.VISIBLE
            alpha = 0f
            scaleX = 0.5f
            scaleY = 0.5f

            animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(duration)
                    .setInterpolator(OvershootInterpolator(1.5f))
                    .start()
        }
    }

    /** Hủy tất cả animator và giải phóng tài nguyên. */
    fun destroy() {
        confidenceRotationAnimator?.cancel()
        confidenceRotationAnimator = null
    }
}
