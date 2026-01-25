package com.nguyendevs.ecolens.handlers.animation

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator

/**
 * Handler quản lý các hiệu ứng animation cho Views trong ứng dụng.
 *
 * Class này cung cấp các phương thức để tạo các hiệu ứng animation phổ biến như slide, fade, scale
 * và rotation. Được thiết kế để tái sử dụng và dễ dàng áp dụng các animation nhất quán trong toàn
 * bộ ứng dụng.
 */
class AnimationHandler {
    private var confidenceRotationAnimator: ObjectAnimator? = null

    /**
     * Animation trượt từ dưới lên kết hợp với fade in.
     *
     * View sẽ di chuyển từ vị trí bên dưới (50dp) lên vị trí gốc, đồng thời tăng dần độ trong suốt
     * từ 0 đến 1.
     *
     * @param view View cần áp dụng animation
     * @param duration Thời gian animation tính bằng milliseconds, mặc định 500ms
     * @param delay Độ trễ trước khi bắt đầu animation, mặc định 0ms
     */
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

    /**
     * Animation fade in đơn giản.
     *
     * Tăng dần độ trong suốt của view từ trạng thái hiện tại đến 1.
     *
     * @param view View cần áp dụng animation
     * @param durationMs Thời gian animation tính bằng milliseconds
     */
    fun fadeIn(view: View, durationMs: Long) {
        view.animate()
                .alpha(1f)
                .setDuration(durationMs)
                .setInterpolator(DecelerateInterpolator())
                .start()
    }

    /**
     * Bắt đầu animation xoay vòng liên tục cho view confidence.
     *
     * View sẽ xoay 360 độ trong 1 giây và lặp lại vô hạn. Thường dùng để hiển thị trạng thái đang
     * xử lý/loading.
     *
     * @param view View cần áp dụng animation xoay
     */
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

    /**
     * Dừng animation xoay và reset view về vị trí ban đầu.
     *
     * Hủy animator đang chạy và đặt lại góc xoay về 0 độ.
     *
     * @param view View cần dừng animation xoay
     */
    fun stopConfidenceRotation(view: View) {
        confidenceRotationAnimator?.cancel()
        confidenceRotationAnimator = null
        view.rotation = 0f
    }

    /**
     * Animation phóng to kết hợp với fade in.
     *
     * View sẽ phóng to từ 80% lên 100% kích thước gốc, đồng thời tăng dần độ trong suốt từ 0 đến 1.
     *
     * @param view View cần áp dụng animation
     * @param duration Thời gian animation tính bằng milliseconds, mặc định 400ms
     * @param delay Độ trễ trước khi bắt đầu animation, mặc định 0ms
     */
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

    /**
     * Animation pop in với hiệu ứng overshoot.
     *
     * View sẽ phóng to từ 50% lên kích thước lớn hơn 100% rồi co lại về 100%, tạo hiệu ứng "bật ra"
     * sống động. Đồng thời tăng dần độ trong suốt.
     *
     * @param view View cần áp dụng animation
     * @param duration Thời gian animation tính bằng milliseconds, mặc định 500ms
     */
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

    /**
     * Giải phóng tài nguyên và hủy tất cả các animator.
     *
     * Phương thức này nên được gọi khi không còn sử dụng handler nữa, ví dụ trong onDestroy() của
     * Activity hoặc onDestroyView() của Fragment.
     */
    fun destroy() {
        confidenceRotationAnimator?.cancel()
        confidenceRotationAnimator = null
    }
}
