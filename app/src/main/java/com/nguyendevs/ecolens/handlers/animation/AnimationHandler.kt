package com.nguyendevs.ecolens.handlers.animation

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator

class AnimationHandler {
    private var confidenceRotationAnimator: ObjectAnimator? = null

    fun slideAndFadeIn(view: View, duration: Long = 500, delay: Long = 0) {
        if (view.visibility == View.VISIBLE && view.alpha == 1f) return

        view.alpha = 0f
        view.translationY = 50f
        view.visibility = View.VISIBLE

        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(duration)
            .setStartDelay(delay)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()
    }

    fun fadeIn(view: View, durationMs: Long) {
        view.animate()
            .alpha(1f)
            .setDuration(durationMs)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun startConfidenceRotation(view: View) {
        if (confidenceRotationAnimator != null) return

        confidenceRotationAnimator = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    fun stopConfidenceRotation(view: View) {
        confidenceRotationAnimator?.cancel()
        confidenceRotationAnimator = null
        view.rotation = 0f
    }

    fun scaleInAnimation(view: View, duration: Long = 400, delay: Long = 0) {
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

    fun popInAnimation(view: View, duration: Long = 500) {
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

    fun destroy() {
        confidenceRotationAnimator?.cancel()
        confidenceRotationAnimator = null
    }
}