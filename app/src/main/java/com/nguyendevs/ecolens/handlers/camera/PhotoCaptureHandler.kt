package com.nguyendevs.ecolens.handlers.camera

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
import java.util.Locale
import java.util.concurrent.ExecutorService

/** Xử lý việc chụp ảnh bằng camera và lưu vào bộ nhớ trong/thư viện. */
class PhotoCaptureHandler(
    private val context: Context,
    private val cameraExecutor: ExecutorService
) {

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    /** Interface callback trả về kết quả chụp ảnh. */
    interface PhotoCaptureCallback {
        fun onPhotoSaved(uriString: String)
        fun onPhotoError(message: String)
    }

    private var callback: PhotoCaptureCallback? = null

    /** Đăng ký callback để nhận kết quả chụp ảnh. */
    fun setCallback(callback: PhotoCaptureCallback) {
        this.callback = callback
    }

    /** Chụp ảnh và lưu vào cache, sau đó xử lý vào bộ nhớ trong. */
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

    /** Lưu ảnh vào bộ nhớ trong, xóa file cache, và gọi callback kèm URI. */
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

    /** Xử lý ảnh được chọn từ thư viện, lưu vào bộ nhớ trong và trả URI. */
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