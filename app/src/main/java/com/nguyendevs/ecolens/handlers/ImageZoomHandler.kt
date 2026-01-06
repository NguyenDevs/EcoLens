package com.nguyendevs.ecolens.handlers

import android.net.Uri
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide

/**
 * Handler quản lý zoom ảnh với chế độ fullscreen
 * Hỗ trợ zoom in/out với fade animation mượt mà
 */
class ImageZoomHandler(
    private val btnZoomIn: ImageView,
    private val btnZoomOut: ImageView,
    private val fullScreenContainer: View,
    private val fullScreenImage: ImageView
) {

    private var currentImageUri: Uri? = null

    init {
        setupClickListeners()
    }

    // ==================== SETUP ====================

    /**
     * Cấu hình click listeners cho zoom buttons và fullscreen container
     */
    private fun setupClickListeners() {
        btnZoomIn.setOnClickListener {
            currentImageUri?.let { uri ->
                showFullScreen(uri)
            }
        }

        btnZoomOut.setOnClickListener {
            hideFullScreen()
        }

        fullScreenContainer.setOnClickListener {
            hideFullScreen()
        }
    }

    // ==================== PUBLIC METHODS ====================

    /**
     * Set URI của ảnh cần zoom
     * Tự động hiển thị/ẩn button zoom in
     */
    fun setImageUri(uri: Uri?) {
        currentImageUri = uri
        btnZoomIn.visibility = if (uri != null) View.VISIBLE else View.GONE
    }

    /**
     * Kiểm tra xem fullscreen có đang hiển thị không
     */
    fun isFullScreenVisible() = fullScreenContainer.visibility == View.VISIBLE

    /**
     * Ẩn fullscreen view với fade animation
     */
    fun hideFullScreen() {
        if (isFullScreenVisible()) {
            fullScreenContainer.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction { fullScreenContainer.visibility = View.GONE }
                .start()
        }
    }

    // ==================== PRIVATE METHODS ====================

    /**
     * Hiển thị ảnh ở chế độ fullscreen với fade in animation
     */
    private fun showFullScreen(uri: Uri) {
        fullScreenContainer.alpha = 0f
        fullScreenContainer.visibility = View.VISIBLE
        fullScreenContainer.animate().alpha(1f).setDuration(200).start()

        Glide.with(fullScreenImage.context)
            .load(uri)
            .into(fullScreenImage)
    }
}