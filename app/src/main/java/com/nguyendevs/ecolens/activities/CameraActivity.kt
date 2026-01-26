package com.nguyendevs.ecolens.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ActivityCameraBinding
import com.nguyendevs.ecolens.utils.ImageUtils
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Activity quản lý camera để chụp ảnh hoặc chọn ảnh từ thư viện
 * Hỗ trợ zoom, focus, flash, và chuyển đổi camera trước/sau
 * Tự động lưu ảnh vào internal storage và trả về URI
 */
class CameraActivity : AppCompatActivity() {

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val KEY_IMAGE_URI = "image_uri"

        fun newIntent(context: Context): Intent {
            return Intent(context, CameraActivity::class.java)
        }
    }

    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraExecutor: ExecutorService
    private var rotateAnimation: Animation? = null

    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null

    /**
     * Activity result launcher để chọn ảnh từ thư viện
     */
    private val selectImageFromGalleryResult = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedImage(it) }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()
        setupZoomAndFocus()
        setupClickListeners()
        startBorderAnimation()
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
        stopBorderAnimation()
    }

    /**
     * Thiết lập các click listeners cho buttons
     */
    private fun setupClickListeners() {
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
            toggleCamera()
        }

        binding.flashToggle.setOnClickListener {
            binding.flashToggle.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            toggleFlash()
        }
    }

    /**
     * Thiết lập pinch-to-zoom và tap-to-focus
     */
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
                performFocus(event.x, event.y)
            }
            true
        }
    }

    /**
     * Khởi động camera với cấu hình preview và image capture
     */
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

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                cameraControl = camera?.cameraControl
                cameraInfo = camera?.cameraInfo

                binding.flashToggle.visibility = if (camera?.cameraInfo?.hasFlashUnit() == true) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            } catch (exc: Exception) {
                handleCameraError(exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Chuyển đổi giữa camera trước và sau
     */
    private fun toggleCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        startCamera()
    }

    /**
     * Thực hiện focus tại vị trí được tap
     */
    private fun performFocus(x: Float, y: Float) {
        val factory = binding.viewFinder.meteringPointFactory
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        cameraControl?.startFocusAndMetering(action)

        showFocusIndicator(x, y)
    }

    /**
     * Chụp ảnh và lưu vào internal storage
     */
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            externalCacheDir ?: cacheDir,
            DateTimeFormatter.ofPattern(FILENAME_FORMAT, Locale.US)
                .format(LocalDateTime.now()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraActivity", "Photo capture failed: ${exc.message}", exc)
                    runOnUiThread {
                        Toast.makeText(
                            baseContext,
                            getString(R.string.error_capture, exc.message),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    handleCapturedImage(photoFile)
                }
            })
    }

    /**
     * Xử lý ảnh sau khi chụp: lưu vào internal và public storage
     */
    private fun handleCapturedImage(photoFile: File) {
        val internalPath = ImageUtils.saveFileToInternalStorage(this@CameraActivity, photoFile)
        ImageUtils.saveImageToPublicStorage(this@CameraActivity, photoFile)
        if (photoFile.exists()) {
            photoFile.delete()
        }

        runOnUiThread {
            if (internalPath != null) {
                val finalUriString = Uri.fromFile(File(internalPath)).toString()
                val resultIntent = Intent().apply {
                    putExtra(KEY_IMAGE_URI, finalUriString)
                }
                setResult(RESULT_OK, resultIntent)
                closeCamera()
            } else {
                Toast.makeText(
                    baseContext,
                    "Lỗi lưu ảnh vào bộ nhớ ứng dụng",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Mở thư viện để chọn ảnh
     */
    private fun openGallery() {
        selectImageFromGalleryResult.launch("image/*")
    }

    /**
     * Xử lý ảnh được chọn từ thư viện
     */
    private fun handleSelectedImage(uri: Uri) {
        try {
            val tempFile = ImageUtils.uriToFile(this, uri, 1080)
            val internalPath = ImageUtils.saveFileToInternalStorage(this, tempFile)

            val finalUriString = if (internalPath != null) {
                Uri.fromFile(File(internalPath)).toString()
            } else {
                uri.toString()
            }

            val resultIntent = Intent().apply {
                data = uri
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                putExtra(KEY_IMAGE_URI, finalUriString)
            }
            setResult(RESULT_OK, resultIntent)
            closeCamera()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Lỗi xử lý ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Bật/tắt flash
     */
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

    /**
     * Cập nhật icon flash dựa trên mode
     */
    private fun updateFlashIcon(mode: Int) {
        val iconRes = when (mode) {
            ImageCapture.FLASH_MODE_ON -> R.drawable.ic_lightning
            else -> R.drawable.ic_lightning_off
        }
        binding.flashToggle.setImageResource(iconRes)
    }

    /**
     * Hiển thị focus indicator tại vị trí được tap
     */
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

    /**
     * Haptic feedback khi nhấn nút chụp
     */
    private fun performHapticFeedback() {
        binding.captureButton.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    /**
     * Animation thu nhỏ/phóng to nút chụp khi nhấn
     */
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

    /**
     * Bắt đầu animation xoay vòng tròn border
     */
    private fun startBorderAnimation() {
        rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_infinite)
        binding.captureBorderAnimated.visibility = View.VISIBLE
        binding.captureBorderAnimated.startAnimation(rotateAnimation)
    }

    /**
     * Dừng animation border
     */
    private fun stopBorderAnimation() {
        rotateAnimation?.cancel()
        binding.captureBorderAnimated.clearAnimation()
        binding.captureBorderAnimated.visibility = View.GONE
    }

    /**
     * Xử lý lỗi khi khởi động camera
     */
    private fun handleCameraError(exc: Exception) {
        if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            Toast.makeText(
                this,
                getString(R.string.error_camera_front),
                Toast.LENGTH_SHORT
            ).show()
            lensFacing = CameraSelector.LENS_FACING_BACK
            startCamera()
        } else {
            Toast.makeText(
                this,
                getString(R.string.error_camera_open, exc.message),
                Toast.LENGTH_SHORT
            ).show()
            closeCamera()
        }
    }


    /**
     * Đóng camera và quay lại màn hình trước
     */
    @Suppress("DEPRECATION")
    private fun closeCamera() {
        finish()
        overridePendingTransition(R.anim.hold, R.anim.slide_out_bottom)
    }
}