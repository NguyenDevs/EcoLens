package com.nguyendevs.ecolens.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nguyendevs.ecolens.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private lateinit var outputDirectory: File
    private lateinit var viewFinder: PreviewView
    private lateinit var closeButton: ImageView
    private lateinit var uploadButton: ImageView
    private lateinit var flashToggle: ImageView
    private lateinit var rotateButton: ImageView

    private var lensFacing = CameraSelector.LENS_FACING_BACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        viewFinder = findViewById(R.id.viewFinder)
        closeButton = findViewById(R.id.closeButton)
        cameraExecutor = Executors.newSingleThreadExecutor()
        outputDirectory = getOutputDirectory()
        uploadButton = findViewById(R.id.uploadButton)
        flashToggle = findViewById(R.id.flashToggle)
        rotateButton = findViewById(R.id.refreshButton)

        cameraExecutor = Executors.newSingleThreadExecutor()
        outputDirectory = getOutputDirectory()

        startCamera()

        findViewById<FloatingActionButton>(R.id.captureButton).setOnClickListener {
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
    private fun openGallery() {
        selectImageFromGalleryResult.launch("image/*")
    }

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

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        overridePendingTransition(R.anim.hold, R.anim.slide_out_bottom)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
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

                if (camera?.cameraInfo?.hasFlashUnit() == true) {
                    flashToggle.visibility = View.VISIBLE
                } else {
                    flashToggle.visibility = View.GONE
                }

            } catch (exc: Exception) {
                if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    Toast.makeText(this, "Không thể mở camera trước", Toast.LENGTH_SHORT).show()
                    lensFacing = CameraSelector.LENS_FACING_BACK
                    startCamera()
                } else {
                    Toast.makeText(this, "Không thể mở camera: ${exc.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

        }, ContextCompat.getMainExecutor(this))
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
                    Toast.makeText(baseContext, "Lỗi chụp ảnh: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)

                    val resultIntent = Intent().apply {
                        putExtra(KEY_IMAGE_URI, savedUri.toString())
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

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val KEY_IMAGE_URI = "image_uri"

        fun newIntent(context: Context): Intent {
            return Intent(context, CameraActivity::class.java)
        }
    }
}