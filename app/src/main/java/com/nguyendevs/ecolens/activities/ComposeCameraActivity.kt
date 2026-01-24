package com.nguyendevs.ecolens.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.ui.screens.camera.CameraScreen
import com.nguyendevs.ecolens.ui.theme.EcoLensTheme
import com.nguyendevs.ecolens.utils.ImageUtils
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Executors

/** Compose-based Camera Activity. Uses AndroidView to embed PreviewView inside Compose. */
class ComposeCameraActivity : ComponentActivity() {

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val KEY_IMAGE_URI = "image_uri"

        fun newIntent(context: Context): Intent {
            return Intent(context, ComposeCameraActivity::class.java)
        }
    }

    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var isFlashOn by remember { mutableStateOf(false) }
            var focusPoint by remember { mutableStateOf<Offset?>(null) }
            var previewView by remember { mutableStateOf<PreviewView?>(null) }

            EcoLensTheme {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) {
                    CameraScreen(
                            isFlashOn = isFlashOn,
                            onFlashToggle = {
                                isFlashOn = !isFlashOn
                                toggleFlash(isFlashOn)
                            },
                            onCloseClick = { closeCamera() },
                            onCaptureClick = { takePhoto() },
                            onUploadClick = { /* TODO: Open gallery */},
                            onSwitchCameraClick = { switchCamera() },
                            onPreviewViewReady = { view ->
                                previewView = view
                                startCamera(view)
                            },
                            focusPoint = focusPoint
                    )
                }
            }
        }
    }

    private fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(
                {
                    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                    val preview =
                            Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                    imageCapture =
                            ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                                    .build()

                    val cameraSelector =
                            CameraSelector.Builder().requireLensFacing(lensFacing).build()

                    try {
                        cameraProvider.unbindAll()
                        camera =
                                cameraProvider.bindToLifecycle(
                                        this,
                                        cameraSelector,
                                        preview,
                                        imageCapture
                                )
                        cameraControl = camera?.cameraControl
                        cameraInfo = camera?.cameraInfo
                    } catch (exc: Exception) {
                        Toast.makeText(
                                        this,
                                        getString(R.string.error_camera_open, exc.message),
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                        closeCamera()
                    }
                },
                ContextCompat.getMainExecutor(this)
        )
    }

    private fun toggleFlash(enabled: Boolean) {
        imageCapture?.flashMode =
                if (enabled) {
                    ImageCapture.FLASH_MODE_ON
                } else {
                    ImageCapture.FLASH_MODE_OFF
                }
    }

    private fun switchCamera() {
        lensFacing =
                if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }
        // TODO: Restart camera with new lens facing
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile =
                File(
                        externalCacheDir ?: cacheDir,
                        DateTimeFormatter.ofPattern(FILENAME_FORMAT, Locale.US)
                                .format(LocalDateTime.now()) + ".jpg"
                )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        runOnUiThread {
                            Toast.makeText(
                                            baseContext,
                                            getString(R.string.error_capture, exc.message),
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                        }
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        handleCapturedImage(photoFile)
                    }
                }
        )
    }

    private fun handleCapturedImage(photoFile: File) {
        val internalPath =
                ImageUtils.saveFileToInternalStorage(this@ComposeCameraActivity, photoFile)
        ImageUtils.saveImageToPublicStorage(this@ComposeCameraActivity, photoFile)
        if (photoFile.exists()) {
            photoFile.delete()
        }

        runOnUiThread {
            if (internalPath != null) {
                val finalUriString = Uri.fromFile(File(internalPath)).toString()
                val resultIntent = Intent().apply { putExtra(KEY_IMAGE_URI, finalUriString) }
                setResult(RESULT_OK, resultIntent)
                closeCamera()
            } else {
                Toast.makeText(baseContext, "Lỗi lưu ảnh vào bộ nhớ ứng dụng", Toast.LENGTH_SHORT)
                        .show()
            }
        }
    }

    private fun closeCamera() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
