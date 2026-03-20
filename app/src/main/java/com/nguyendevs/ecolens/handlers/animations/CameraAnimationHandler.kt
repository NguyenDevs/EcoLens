package com.nguyendevs.ecolens.handlers.animations

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.nguyendevs.ecolens.R

/** Quản lý animation và haptic feedback cho giao diện camera. */
class CameraAnimationHandler(private val context: Context) {

    private var rotateAnimation: Animation? = null

    /** Chạy animation nhấn thu phóng cho nút chụp. */
    fun animateCaptureButton(captureButton: View) {
        captureButton.animate()
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(100)
            .withEndAction {
                captureButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    /** Thực hiện haptic feedback khi chụp ảnh. */
    fun performCaptureHapticFeedback(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    /** Thực hiện haptic feedback xác nhận. */
    fun performConfirmHapticFeedback(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    /** Hiển thị chỉ báo focus tại vị trí chạm màn hình. */
    fun showFocusIndicator(
        focusIndicator: View,
        x: Float,
        y: Float,
        viewFinderTop: Float
    ) {
        focusIndicator.apply {
            animate().cancel()
            this.x = x - (width / 2)
            this.y = y - (height / 2) + viewFinderTop
            visibility = View.VISIBLE
            alpha = 1f
            scaleX = 1.3f
            scaleY = 1.3f

            animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    animate()
                        .alpha(0f)
                        .setStartDelay(500)
                        .setDuration(300)
                        .withEndAction { visibility = View.INVISIBLE }
                        .start()
                }
                .start()
        }
    }

    /** Bắt đầu animation viền xoay vòng lặp. */
    fun startBorderAnimation(borderView: View) {
        rotateAnimation = AnimationUtils.loadAnimation(context, R.anim.rotate_infinite)
        borderView.visibility = View.VISIBLE
        borderView.startAnimation(rotateAnimation)
    }

    /** Dừng animation viền xoay và ẩn view. */
    fun stopBorderAnimation(borderView: View) {
        rotateAnimation?.cancel()
        borderView.clearAnimation()
        borderView.visibility = View.GONE
    }

    /** Chạy animation xoay một vòng duy nhất. */
    fun animateRotateOnce(view: View) {
        val rotateOnce = AnimationUtils.loadAnimation(context, R.anim.rotate_once)
        view.startAnimation(rotateOnce)
    }

    /** Hủy animation và giải phóng tài nguyên. */
    fun cleanup() {
        rotateAnimation?.cancel()
        rotateAnimation = null
    }
}