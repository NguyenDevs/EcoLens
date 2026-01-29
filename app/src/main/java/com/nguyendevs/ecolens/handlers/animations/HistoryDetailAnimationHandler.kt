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

/**
 * Animation handler dành riêng cho HistoryDetailFragment
 * - Image loading animations
 * - FAB state animations (speak/mute)
 * - Haptic feedback
 */
class HistoryDetailAnimationHandler(private val context: Context) {

    companion object {
        private const val ANIMATION_DURATION = 150L
    }

    // ==================== HAPTIC FEEDBACK ====================

    fun performConfirmFeedback(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    // ==================== IMAGE LOADING ANIMATIONS ====================

    /**
     * Load image với fade-in animation
     * Ưu tiên local file, fallback về remote URL
     */
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

    // ==================== FAB ANIMATIONS ====================

    /**
     * Show FAB với scale animation
     */
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

    /**
     * Animate FAB state change (speaking/not speaking)
     *
     * @param fab FloatingActionButton
     * @param isSpeaking True nếu đang nói (active state)
     */
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