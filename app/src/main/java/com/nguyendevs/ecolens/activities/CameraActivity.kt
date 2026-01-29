package com.nguyendevs.ecolens.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.managers.CameraManager
import com.nguyendevs.ecolens.handlers.camera.PhotoCaptureHandler
import com.nguyendevs.ecolens.handlers.animations.CameraAnimationHandler
import com.nguyendevs.ecolens.databinding.ActivityCameraBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Activity quản lý camera để chụp ảnh hoặc chọn ảnh từ thư viện
 * Sử dụng các helper classes:
 * - CameraManager: Quản lý camera operations
 * - PhotoCaptureHandler: Xử lý chụp và lưu ảnh
 * - CameraAnimationHandler: Quản lý animations và haptic feedback
 */
class CameraActivity : AppCompatActivity() {

    companion object {
        const val KEY_IMAGE_URI = "image_uri"

        fun newIntent(context: Context): Intent {
            return Intent(context, CameraActivity::class.java)
        }
    }

    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var cameraManager: CameraManager
    private lateinit var photoCaptureHandler: PhotoCaptureHandler
    private lateinit var cameraAnimationHandler: CameraAnimationHandler

    private val selectImageFromGalleryResult = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleGalleryImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        setupCamera()
        setupClickListeners()
        cameraAnimationHandler.startBorderAnimation(binding.captureBorderAnimated)
    }

    private fun initializeComponents() {
        cameraExecutor = Executors.newSingleThreadExecutor()

        cameraManager = CameraManager(this, this, binding.viewFinder)
        photoCaptureHandler = PhotoCaptureHandler(this, cameraExecutor)
        cameraAnimationHandler = CameraAnimationHandler(this)

        setupCallbacks()
    }

    private fun setupCallbacks() {
        cameraManager.setCallback(object : CameraManager.CameraCallback {
            override fun onCameraReady(camera: Camera, imageCapture: ImageCapture) {
                // Camera is ready
            }

            override fun onCameraError(exception: Exception) {
                Toast.makeText(
                    this@CameraActivity,
                    getString(R.string.error_camera_open, exception.message),
                    Toast.LENGTH_SHORT
                ).show()
                closeCamera()
            }

            override fun onFlashAvailabilityChanged(hasFlash: Boolean) {
                binding.flashToggle.visibility = if (hasFlash) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        })

        photoCaptureHandler.setCallback(object : PhotoCaptureHandler.PhotoCaptureCallback {
            override fun onPhotoSaved(uriString: String) {
                runOnUiThread {
                    val resultIntent = Intent().apply {
                        putExtra(KEY_IMAGE_URI, uriString)
                    }
                    setResult(RESULT_OK, resultIntent)
                    closeCamera()
                }
            }

            override fun onPhotoError(message: String) {
                runOnUiThread {
                    Toast.makeText(this@CameraActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCamera() {
        cameraManager.startCamera()

        binding.viewFinder.setOnTouchListener(
            cameraManager.setupZoomAndFocus { x, y ->
                cameraAnimationHandler.performConfirmHapticFeedback(binding.viewFinder)
                cameraAnimationHandler.showFocusIndicator(
                    binding.focusIndicator,
                    x,
                    y,
                    binding.viewFinder.top.toFloat()
                )
            }
        )
    }

    private fun setupClickListeners() {
        binding.captureButton.setOnClickListener {
            cameraAnimationHandler.performCaptureHapticFeedback(binding.captureButton)
            cameraAnimationHandler.animateCaptureButton(binding.captureButton)

            cameraManager.getImageCapture()?.let { imageCapture ->
                photoCaptureHandler.takePhoto(imageCapture)
            }
        }

        binding.closeButton.setOnClickListener {
            cameraAnimationHandler.performCaptureHapticFeedback(binding.closeButton)
            closeCamera()
        }

        binding.uploadButton.setOnClickListener {
            cameraAnimationHandler.performConfirmHapticFeedback(binding.uploadButton)
            openGallery()
        }

        binding.refreshButton.setOnClickListener {
            cameraAnimationHandler.performConfirmHapticFeedback(binding.refreshButton)
            cameraAnimationHandler.animateRotateOnce(binding.refreshButton)
            cameraManager.toggleCamera()
        }

        binding.flashToggle.setOnClickListener {
            cameraAnimationHandler.performConfirmHapticFeedback(binding.flashToggle)
            val newMode = cameraManager.toggleFlash()
            updateFlashIcon(newMode)
        }
    }

    private fun updateFlashIcon(mode: Int) {
        val iconRes = when (mode) {
            ImageCapture.FLASH_MODE_ON -> R.drawable.ic_lightning
            else -> R.drawable.ic_lightning_off
        }
        binding.flashToggle.setImageResource(iconRes)
    }

    private fun openGallery() {
        selectImageFromGalleryResult.launch("image/*")
    }

    private fun handleGalleryImage(uri: Uri) {
        photoCaptureHandler.handleSelectedImage(uri, object : PhotoCaptureHandler.PhotoCaptureCallback {
            override fun onPhotoSaved(uriString: String) {
                runOnUiThread {
                    val resultIntent = Intent().apply {
                        data = uri
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        putExtra(KEY_IMAGE_URI, uriString)
                    }
                    setResult(RESULT_OK, resultIntent)
                    closeCamera()
                }
            }

            override fun onPhotoError(message: String) {
                runOnUiThread {
                    Toast.makeText(this@CameraActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        closeCamera()
    }

    override fun onSupportNavigateUp(): Boolean {
        closeCamera()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        cameraAnimationHandler.stopBorderAnimation(binding.captureBorderAnimated)
        cameraAnimationHandler.cleanup()
    }

    @Suppress("DEPRECATION")
    private fun closeCamera() {
        finish()
        overridePendingTransition(R.anim.hold, R.anim.slide_out_bottom)
    }
}