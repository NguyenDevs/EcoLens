package com.nguyendevs.ecolens.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageHelper {

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun compressBitmap(imagePath: String, reqWidth: Int = 1024, reqHeight: Int = 1024): ByteArray? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imagePath, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        val bitmap = BitmapFactory.decodeFile(imagePath, options) ?: return null

        val scaledBitmap = scaleBitmap(bitmap, reqWidth)
        val baos = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos)
        
        if (scaledBitmap != bitmap) bitmap.recycle()
        scaledBitmap.recycle()
        
        return baos.toByteArray()
    }

    /** Chuẩn bị ảnh chuyên dụng cho AI để đạt tốc độ nhận diện nhanh nhất. */
    fun prepareImageForAI(bitmap: Bitmap): ByteArray {
        val scaledBitmap = scaleBitmap(bitmap, 1024)
        val baos = ByteArrayOutputStream()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            scaledBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 75, baos)
        } else {
            @Suppress("DEPRECATION")
            scaledBitmap.compress(Bitmap.CompressFormat.WEBP, 75, baos)
        }
        
        if (scaledBitmap != bitmap) scaledBitmap.recycle()
        return baos.toByteArray()
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scale = maxDimension.toFloat() / Math.max(width, height)
        if (scale >= 1f) return bitmap
        
        val matrix = Matrix().apply { postScale(scale, scale) }
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    fun saveBitmapToInternal(context: Context, bitmap: Bitmap, prefix: String, id: Int): String? {
        return try {
            val filename = "${prefix}_${id}_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
