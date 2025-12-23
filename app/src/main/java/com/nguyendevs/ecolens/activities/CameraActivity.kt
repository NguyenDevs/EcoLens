package com.nguyendevs.ecolens.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.ScaleGestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nguyendevs.ecolens.R
import java.io.File
import java.text.SimpleDateFormat
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

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var closeButton: ImageView
    private lateinit var flashToggle: ImageView
    private lateinit var outputDirectory: File
    private lateinit var rotateButton: ImageView
    private lateinit var uploadButton: ImageView
    private lateinit var viewFinder: PreviewView
    private lateinit var captureButton: FloatingActionButton
    private lateinit var focusIndicator: ImageView

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
            finish()
            overridePendingTransition(R.anim.hold, R.anim.slide_out_bottom)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        viewFinder = findViewById(R.id.viewFinder)
        closeButton = findViewById(R.id.closeButton)
        captureButton = findViewById(R.id.captureButton)
        focusIndicator = findViewById(R.id.focusIndicator)
        cameraExecutor = Executors.newSingleThreadExecutor()
        outputDirectory = getOutputDirectory()
        uploadButton = findViewById(R.id.uploadButton)
        flashToggle = findViewById(R.id.flashToggle)
        rotateButton = findViewById(R.id.refreshButton)

        startCamera()
        setupZoomAndFocus()

        captureButton.setOnClickListener {
            performHapticFeedback()
            animateCaptureButton()
            takePhoto()
        }

        closeButton.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.hold, R.anim.slide_out_bottom)
        }

        uploadButton.setOnClickListener {
            openGallery()
        }

        rotateButton.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            startCamera()
        }

        flashToggle.setOnClickListener {
            toggleFlash()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        overridePendingTransition(R.anim.hold, R.anim.slide_out_bottom)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
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
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
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
                    flashToggle.visibility = View.VISIBLE
                } else {
                    flashToggle.visibility = View.GONE
                }

            } catch (exc: Exception) {
                if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    Toast.makeText(this, getString(R.string.error_camera_front), Toast.LENGTH_SHORT).show()
                    lensFacing = CameraSelector.LENS_FACING_BACK
                    startCamera()
                } else {
                    Toast.makeText(this, getString(R.string.error_camera_open, exc.message), Toast.LENGTH_SHORT).show()
                    finish()
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

        viewFinder.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                val factory = viewFinder.meteringPointFactory
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
        focusIndicator.animate().cancel()

        focusIndicator.x = x - (focusIndicator.width / 2)
        focusIndicator.y = y - (focusIndicator.height / 2) + viewFinder.top

        focusIndicator.visibility = View.VISIBLE
        focusIndicator.alpha = 1f
        focusIndicator.scaleX = 1.3f
        focusIndicator.scaleY = 1.3f

        focusIndicator.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                focusIndicator.animate()
                    .alpha(0f)
                    .setStartDelay(500)
                    .setDuration(300)
                    .withEndAction {
                        focusIndicator.visibility = View.INVISIBLE
                    }
                    .start()
            }
            .start()
    }

    private fun performHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {
            Log.e("CameraActivity", "Vibration failed: ${e.message}")
        }
    }

    private fun animateCaptureButton() {
        captureButton.animate()
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(100)
            .withEndAction {
                captureButton.animate()
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
        val newMode = when (currentMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_OFF
        }

        imageCapture.flashMode = newMode
        updateFlashIcon(newMode)
    }

    private fun updateFlashIcon(mode: Int) {
        val iconRes = when (mode) {
            ImageCapture.FLASH_MODE_ON -> R.drawable.ic_lightning
            else -> R.drawable.ic_lightning_off
        }
        flashToggle.setImageResource(iconRes)
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraActivity", "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(baseContext, getString(R.string.error_capture, exc.message), Toast.LENGTH_SHORT).show()
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

                    val resultIntent = Intent().apply {
                        putExtra(KEY_IMAGE_URI, savedUri.toString())
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                    overridePendingTransition(R.anim.hold, R.anim.slide_out_bottom)
                }
            })
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else cacheDir
    }
}