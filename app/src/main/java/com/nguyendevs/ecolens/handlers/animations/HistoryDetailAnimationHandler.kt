package com.nguyendevs.ecolens.handlers.animations

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.GenericTransitionOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nguyendevs.ecolens.R
import java.io.File

/** Quản lý animation image loading, FAB state và haptic feedback cho HistoryDetailFragment. */
class HistoryDetailAnimationHandler(private val context: Context) {

    companion object {
        private const val ANIMATION_DURATION = 150L
    }

    /** Thực hiện haptic feedback xác nhận. */
    fun performConfirmFeedback(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    /** Tải ảnh vào ImageView với animation fade-in, ưu tiên local file trước remote URL. */
    fun loadImageWithFadeIn(
        imageView: ImageView,
        localPath: String?,
        remoteUrl: String?,
        centerCrop: Boolean = true
    ) {
        var loadSource: Any = remoteUrl ?: R.mipmap.ic_launcher
        if (!localPath.isNullOrEmpty()) {
            val file = File(localPath)
            if (file.exists()) {
                loadSource = file
            }
        }

        val glideRequest = Glide.with(context)
            .load(loadSource)
            .transition(GenericTransitionOptions.with(R.anim.fade_in_2))
            .placeholder(R.mipmap.ic_launcher)
            .error(R.mipmap.ic_launcher)

        if (centerCrop) {
            glideRequest.centerCrop()
        }

        glideRequest.into(imageView)
    }

    /** Hiển thị FAB với animation scale và fade-in. */
    fun showFab(fab: FloatingActionButton) {
        fab.show()
        fab.alpha = 0f
        fab.scaleX = 0.5f
        fab.scaleY = 0.5f
        fab.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300L)
            .start()
    }

    /** Chuyển đổi trạng thái FAB (đang đọc/dừng) với animation scale. */
    fun animateFabState(fab: FloatingActionButton, isSpeaking: Boolean) {
        fab.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(ANIMATION_DURATION)
            .withEndAction {
                if (isSpeaking) {
                    fab.setImageResource(R.drawable.ic_mute)
                    fab.backgroundTintList = ColorStateList.valueOf(Color.RED)
                } else {
                    fab.setImageResource(R.drawable.ic_speak)
                    fab.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.green_primary)
                    )
                }

                fab.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(ANIMATION_DURATION)
                    .start()
            }
            .start()
    }
}