package com.nguyendevs.ecolens.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ScaleGestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.nguyendevs.ecolens.databinding.ActivityCameraModernBinding
import com.nguyendevs.ecolens.R
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraActivity : AppCompatActivity() {

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val KEY_IMAGE_URI = "image_uri"

        fun newIntent(context: Context): Intent {
            return Intent(context, CameraActivity::class.java)
        }
    }

    private lateinit var binding: ActivityCameraModernBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var outputDirectory: File
    private var rotateAnimation: Animation? = null

    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null

    private val selectImageFromGalleryResult = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val resultIntent = Intent().apply {
                data = it
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                putExtra(KEY_IMAGE_URI, it.toString())

            }
            setResult(RESULT_OK, resultIntent)
            closeCamera()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraModernBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        outputDirectory = getOutputDirectory()

        startCamera()
        setupZoomAndFocus()
        startBorderAnimation()
        binding.captureButton.setOnClickListener {
            performHapticFeedback()
            animateCaptureButton()
            takePhoto()
        }

        binding.closeButton.setOnClickListener {
            binding.closeButton.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            closeCamera()
        }

        binding.uploadButton.setOnClickListener {
            binding.uploadButton.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            openGallery()
        }

        binding.refreshButton.setOnClickListener {
            binding.refreshButton.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            val rotateOnce = AnimationUtils.loadAnimation(this, R.anim.rotate_once)
            binding.refreshButton.startAnimation(rotateOnce)
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            startCamera()
        }

        binding.flashToggle.setOnClickListener {
            binding.flashToggle.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            toggleFlash()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        closeCamera()
    }

    private fun closeCamera() {
        finish()
        overridePendingTransition(R.anim.hold, R.anim.slide_out_bottom)
    }

    override fun onSupportNavigateUp(): Boolean {
        closeCamera()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        stopBorderAnimation()
    }

    private fun openGallery() {
        selectImageFromGalleryResult.launch("image/*")
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                .build()

            updateFlashIcon(ImageCapture.FLASH_MODE_OFF)

            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                cameraControl = camera?.cameraControl
                cameraInfo = camera?.cameraInfo

                if (camera?.cameraInfo?.hasFlashUnit() == true) {
                    binding.flashToggle.visibility = View.VISIBLE
                } else {
                    binding.flashToggle.visibility = View.GONE
                }

            } catch (exc: Exception) {
                if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    Toast.makeText(this, getString(R.string.error_camera_front), Toast.LENGTH_SHORT).show()
                    lensFacing = CameraSelector.LENS_FACING_BACK
                    startCamera()
                } else {
                    Toast.makeText(this, getString(R.string.error_camera_open, exc.message), Toast.LENGTH_SHORT).show()
                    closeCamera()
                }
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupZoomAndFocus() {
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentZoomRatio = cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
                val delta = detector.scaleFactor
                cameraControl?.setZoomRatio(currentZoomRatio * delta)
                return true
            }
        }

        val scaleGestureDetector = ScaleGestureDetector(this, listener)

        binding.viewFinder.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                binding.viewFinder.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                val factory = binding.viewFinder.meteringPointFactory
                val point = factory.createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                    .build()
                cameraControl?.startFocusAndMetering(action)

                showFocusIndicator(event.x, event.y)
            }
            true
        }
    }

    private fun showFocusIndicator(x: Float, y: Float) {
        binding.focusIndicator.apply {
            animate().cancel()
            this.x = x - (width / 2)
            this.y = y - (height / 2) + binding.viewFinder.top
            visibility = View.VISIBLE
            alpha = 1f
            scaleX = 1.3f
            scaleY = 1.3f

            animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    animate()
                        .alpha(0f)
                        .setStartDelay(500)
                        .setDuration(300)
                        .withEndAction { visibility = View.INVISIBLE }
                        .start()
                }
                .start()
        }
    }

    private fun performHapticFeedback() {
        binding.captureButton.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    private fun animateCaptureButton() {
        binding.captureButton.animate()
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(100)
            .withEndAction {
                binding.captureButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun toggleFlash() {
        val imageCapture = imageCapture ?: return
        val currentMode = imageCapture.flashMode
        val newMode = if (currentMode == ImageCapture.FLASH_MODE_ON) {
            ImageCapture.FLASH_MODE_OFF
        } else {
            ImageCapture.FLASH_MODE_ON
        }

        imageCapture.flashMode = newMode
        updateFlashIcon(newMode)
    }

    private fun updateFlashIcon(mode: Int) {
        val iconRes = when (mode) {
            ImageCapture.FLASH_MODE_ON -> R.drawable.ic_lightning
            else -> R.drawable.ic_lightning_off
        }
        binding.flashToggle.setImageResource(iconRes)
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            DateTimeFormatter.ofPattern(FILENAME_FORMAT, Locale.US).format(LocalDateTime.now()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraActivity", "Photo capture failed: ${exc.message}", exc)
                    runOnUiThread {
                        Toast.makeText(baseContext, getString(R.string.error_capture, exc.message), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    MediaScannerConnection.scanFile(
                        this@CameraActivity,
                        arrayOf(photoFile.absolutePath),
                        null,
                        null
                    )

                    val savedUri = FileProvider.getUriForFile(
                        this@CameraActivity,
                        "${applicationContext.packageName}.provider",
                        photoFile
                    )

                    runOnUiThread {
                        val resultIntent = Intent().apply {
                            putExtra(KEY_IMAGE_URI, savedUri.toString())
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        setResult(RESULT_OK, resultIntent)
                        closeCamera()
                    }
                }
            })
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else cacheDir
    }

    private fun startBorderAnimation() {
        rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_infinite)
        binding.captureBorderAnimated.visibility = View.VISIBLE
        binding.captureBorderAnimated.startAnimation(rotateAnimation)
    }

    private fun stopBorderAnimation() {
        rotateAnimation?.cancel()
        binding.captureBorderAnimated.clearAnimation()
        binding.captureBorderAnimated.visibility = View.GONE
    }
}