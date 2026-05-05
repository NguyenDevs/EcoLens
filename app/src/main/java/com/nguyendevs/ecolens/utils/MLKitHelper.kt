package com.nguyendevs.ecolens.utils

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.tasks.await

object MLKitHelper {

    private val options = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
        .enableMultipleObjects() // Cho phép tìm nhiều vật thể để chọn cái tốt nhất
        .enableClassification()  // Thử phân loại sơ bộ
        .build()

    private val objectDetector = ObjectDetection.getClient(options)

    /** Phát hiện vật thể và cắt lấy phần quan trọng nhất trong ảnh. */
    suspend fun detectAndCropObject(bitmap: Bitmap): Bitmap {
        val image = InputImage.fromBitmap(bitmap, 0)
        
        return try {
            val detectedObjects = objectDetector.process(image).await()
            
            if (detectedObjects.isNotEmpty()) {
                // Ưu tiên vật thể lớn nhất hoặc có độ tin cậy cao nhất
                val primaryObject = detectedObjects.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                
                primaryObject?.let { obj ->
                    val box = obj.boundingBox
                    // Thêm một chút padding (10%) xung quanh vật thể để AI dễ nhìn hơn
                    val paddingW = (box.width() * 0.1).toInt()
                    val paddingH = (box.height() * 0.1).toInt()
                    
                    val left = (box.left - paddingW).coerceAtLeast(0)
                    val top = (box.top - paddingH).coerceAtLeast(0)
                    val right = (box.right + paddingW).coerceAtMost(bitmap.width)
                    val bottom = (box.bottom + paddingH).coerceAtMost(bitmap.height)
                    
                    val width = right - left
                    val height = bottom - top
                    
                    if (width > 0 && height > 0) {
                        return Bitmap.createBitmap(bitmap, left, top, width, height)
                    }
                }
            }
            bitmap // Fallback: Trả về ảnh gốc nếu không tìm thấy gì
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }
}
