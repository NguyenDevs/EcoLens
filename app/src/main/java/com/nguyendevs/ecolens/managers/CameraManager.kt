package com.nguyendevs.ecolens.managers

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.nguyendevs.ecolens.R
import java.util.concurrent.TimeUnit

/**
 * Quản lý các thao tác với camera
 * - Khởi tạo và binding camera
 * - Zoom, focus, flash
 * - Chuyển đổi camera trước/sau
 */
class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {

    interface CameraCallback {
        fun onCameraReady(camera: Camera, imageCapture: ImageCapture)
        fun onCameraError(exception: Exception)
        fun onFlashAvailabilityChanged(hasFlash: Boolean)
    }

    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null
    private var callback: CameraCallback? = null

    fun setCallback(callback: CameraCallback) {
        this.callback = callback
    }

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageCapture
                )
                cameraControl = camera?.cameraControl
                cameraInfo = camera?.cameraInfo

                val hasFlash = camera?.cameraInfo?.hasFlashUnit() == true
                callback?.onFlashAvailabilityChanged(hasFlash)
                callback?.onCameraReady(camera!!, imageCapture!!)

            } catch (exc: Exception) {
                handleCameraError(exc)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    fun toggleCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        startCamera()
    }

    fun setupZoomAndFocus(onFocusPerformed: (x: Float, y: Float) -> Unit): View.OnTouchListener {
        val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentZoomRatio = cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
                val delta = detector.scaleFactor
                cameraControl?.setZoomRatio(currentZoomRatio * delta)
                return true
            }
        }

        val scaleGestureDetector = ScaleGestureDetector(context, scaleListener)

        return View.OnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                performFocus(event.x, event.y)
                onFocusPerformed(event.x, event.y)
            }
            true
        }
    }

    private fun performFocus(x: Float, y: Float) {
        val factory = previewView.meteringPointFactory
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        cameraControl?.startFocusAndMetering(action)
    }

    fun toggleFlash(): Int {
        val imageCapture = imageCapture ?: return ImageCapture.FLASH_MODE_OFF
        val currentMode = imageCapture.flashMode
        val newMode = if (currentMode == ImageCapture.FLASH_MODE_ON) {
            ImageCapture.FLASH_MODE_OFF
        } else {
            ImageCapture.FLASH_MODE_ON
        }

        imageCapture.flashMode = newMode
        return newMode
    }

    fun getImageCapture(): ImageCapture? = imageCapture

    fun getCurrentFlashMode(): Int = imageCapture?.flashMode ?: ImageCapture.FLASH_MODE_OFF

    fun isUsingFrontCamera(): Boolean = lensFacing == CameraSelector.LENS_FACING_FRONT

    private fun handleCameraError(exc: Exception) {
        if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            Toast.makeText(
                context,
                context.getString(R.string.error_camera_front),
                Toast.LENGTH_SHORT
            ).show()
            lensFacing = CameraSelector.LENS_FACING_BACK
            startCamera()
        } else {
            callback?.onCameraError(exc)
        }
    }
}