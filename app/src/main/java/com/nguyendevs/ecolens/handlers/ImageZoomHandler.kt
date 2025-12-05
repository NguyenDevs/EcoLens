package com.nguyendevs.ecolens.handlers

import android.net.Uri
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide

class ImageZoomHandler(
    private val btnZoomIn: ImageView,
    private val btnZoomOut: ImageView,
    private val fullScreenContainer: View,
    private val fullScreenImage: ImageView
) {

    private var currentImageUri: Uri? = null

    // Thiết lập các sự kiện zoom ảnh
    fun setup() {
        btnZoomIn.setOnClickListener {
            currentImageUri?.let { uri ->
                fullScreenContainer.visibility = View.VISIBLE
                Glide.with(fullScreenImage.context)
                    .load(uri)
                    .into(fullScreenImage)
            }
        }

        btnZoomOut.setOnClickListener {
            fullScreenContainer.visibility = View.GONE
        }

        fullScreenContainer.setOnClickListener {
            fullScreenContainer.visibility = View.GONE
        }
    }

    // Đặt URI ảnh hiện tại
    fun setImageUri(uri: Uri?) {
        currentImageUri = uri
        btnZoomIn.visibility = if (uri != null) View.VISIBLE else View.GONE
    }

    // Kiểm tra trạng thái fullscreen
    fun isFullScreenVisible() = fullScreenContainer.visibility == View.VISIBLE

    // Ẩn chế độ fullscreen
    fun hideFullScreen() {
        fullScreenContainer.visibility = View.GONE
    }
}