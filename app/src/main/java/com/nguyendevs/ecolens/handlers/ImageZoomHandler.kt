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

    init {
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

    fun setImageUri(uri: Uri?) {
        currentImageUri = uri
        btnZoomIn.visibility = if (uri != null) View.VISIBLE else View.GONE
    }

    fun isFullScreenVisible() = fullScreenContainer.visibility == View.VISIBLE

    private fun showFullScreen(uri: Uri) {
        fullScreenContainer.alpha = 0f
        fullScreenContainer.visibility = View.VISIBLE
        fullScreenContainer.animate().alpha(1f).setDuration(200).start()

        Glide.with(fullScreenImage.context)
            .load(uri)
            .into(fullScreenImage)
    }

    fun hideFullScreen() {
        if (isFullScreenVisible()) {
            fullScreenContainer.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction { fullScreenContainer.visibility = View.GONE }
                .start()
        }
    }
}