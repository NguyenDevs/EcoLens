package com.nguyendevs.ecolens.handlers

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.utils.ImageUtils
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ExecutorService

/**
 * Xử lý việc chụp ảnh và lưu vào storage
 * - Chụp ảnh với camera
 * - Lưu vào internal storage
 * - Lưu vào public storage (gallery)
 */
class PhotoCaptureHandler(
    private val context: Context,
    private val cameraExecutor: ExecutorService
) {

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    interface PhotoCaptureCallback {
        fun onPhotoSaved(uriString: String)
        fun onPhotoError(message: String)
    }

    private var callback: PhotoCaptureCallback? = null

    fun setCallback(callback: PhotoCaptureCallback) {
        this.callback = callback
    }

    fun takePhoto(imageCapture: ImageCapture) {
        val photoFile = File(
            context.externalCacheDir ?: context.cacheDir,
            DateTimeFormatter.ofPattern(FILENAME_FORMAT, Locale.US)
                .format(LocalDateTime.now()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("PhotoCaptureHandler", "Photo capture failed: ${exc.message}", exc)
                    val errorMessage = context.getString(R.string.error_capture, exc.message)
                    callback?.onPhotoError(errorMessage)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    handleCapturedImage(photoFile)
                }
            })
    }

    private fun handleCapturedImage(photoFile: File) {
        val internalPath = ImageUtils.saveFileToInternalStorage(context, photoFile)
        ImageUtils.saveImageToPublicStorage(context, photoFile)

        if (photoFile.exists()) {
            photoFile.delete()
        }

        if (internalPath != null) {
            val finalUriString = Uri.fromFile(File(internalPath)).toString()
            callback?.onPhotoSaved(finalUriString)
        } else {
            callback?.onPhotoError("Lỗi lưu ảnh vào bộ nhớ ứng dụng")
        }
    }

    fun handleSelectedImage(uri: Uri, callback: PhotoCaptureCallback) {
        try {
            val tempFile = ImageUtils.uriToFile(context, uri, 1080)
            val internalPath = ImageUtils.saveFileToInternalStorage(context, tempFile)

            val finalUriString = if (internalPath != null) {
                Uri.fromFile(File(internalPath)).toString()
            } else {
                uri.toString()
            }

            callback.onPhotoSaved(finalUriString)
        } catch (e: Exception) {
            e.printStackTrace()
            callback.onPhotoError("Lỗi xử lý ảnh: ${e.message}")
        }
    }
}