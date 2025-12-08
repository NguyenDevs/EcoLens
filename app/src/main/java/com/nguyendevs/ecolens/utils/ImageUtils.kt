package com.nguyendevs.ecolens.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {

    fun uriToFile(context: Context, uri: Uri, maxDimension: Int): File {
        val cacheDir = context.cacheDir
        val file = File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")

        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            var inSampleSize = 1
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val halfHeight: Int = options.outHeight / 2
                val halfWidth: Int = options.outWidth / 2
                while (halfHeight / inSampleSize >= maxDimension && halfWidth / inSampleSize >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            inputStream = context.contentResolver.openInputStream(uri)
            val scaledOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = inSampleSize
            }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, scaledOptions)
            inputStream?.close()

            if (bitmap != null) {
                val rotatedBitmap = rotateImageIfRequired(context, bitmap, uri)
                FileOutputStream(file).use { out ->
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                if (rotatedBitmap != bitmap) bitmap.recycle()
                rotatedBitmap.recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
        }
        return file
    }

    private fun rotateImageIfRequired(context: Context, bitmap: Bitmap, uri: Uri): Bitmap {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            return bitmap
        } finally {
            inputStream?.close()
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun saveBitmapToInternalStorage(context: Context, file: File): String? {
        return try {
            val filename = "species_${System.currentTimeMillis()}.jpg"
            val destFile = File(context.filesDir, filename)
            file.copyTo(destFile, overwrite = true)
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}