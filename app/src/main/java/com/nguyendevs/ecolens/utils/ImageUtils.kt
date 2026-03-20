package com.nguyendevs.ecolens.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

/** Cung cấp bộ công cụ xử lý kích thước, xoay, và lưu ảnh. */
object ImageUtils {

    /** Ép định dạng tệp ảnh từ URI sang nội bộ với kích thước giới hạn. */
    @Throws(Exception::class)
    fun uriToFile(context: Context, uri: Uri, maxDimension: Int): File {
        val cacheDir = context.cacheDir
        val file = File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")

        if (uri.scheme == "http" || uri.scheme == "https") {
            try {
                val url = URL(uri.toString())
                val connection = url.openConnection()
                connection.setRequestProperty("User-Agent", "EcoLens-Android")
                connection.connect()

                val inputStream = connection.getInputStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (bitmap != null) {
                    val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                        val ratio = Math.min(
                            maxDimension.toFloat() / bitmap.width,
                            maxDimension.toFloat() / bitmap.height
                        )
                        val width = (bitmap.width * ratio).toInt()
                        val height = (bitmap.height * ratio).toInt()
                        Bitmap.createScaledBitmap(bitmap, width, height, true)
                    } else {
                        bitmap
                    }

                    FileOutputStream(file).use { out ->
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }

                    if (scaledBitmap != bitmap) bitmap.recycle()
                    scaledBitmap.recycle()

                    return file
                } else {
                    throw Exception("Failed to decode bitmap from URL: $uri")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw Exception("Failed to download image from URL: $uri", e)
            }
        }

        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
                ?: throw FileNotFoundException("Cannot open input stream for URI: $uri")

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

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
            } else {
                throw Exception("Failed to decode bitmap from URI: $uri")
            }

            if (!file.exists() || file.length() == 0L) {
                throw Exception("Created file is empty or does not exist: ${file.absolutePath}")
            }

        } finally {
            inputStream?.close()
        }
        return file
    }

    /** Triển khai phục hồi hướng xoay ảnh bằng dữ liệu EXIF. */
    private fun rotateImageIfRequired(context: Context, bitmap: Bitmap, uri: Uri): Bitmap {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

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

    /** Xoay dữ liệu thô của Bitmap theo quỹ đạo góc chỉ định. */
    private fun rotateBitmap(bitmap: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** Lưu đệm dữ liệu Bitmap vào vùng nhớ cục bộ hệ thống. */
    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
        return try {
            val filename = "species_${System.currentTimeMillis()}.jpg"
            val destFile = File(context.filesDir, filename)
            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Bản sao chép tập tin ảnh nguyên bản vào bộ nhớ lưu trữ ứng dụng. */
    fun saveFileToInternalStorage(context: Context, sourceFile: File): String? {
        return try {
            val filename = "species_${System.currentTimeMillis()}.jpg"
            val destFile = File(context.filesDir, filename)
            sourceFile.copyTo(destFile, overwrite = true)
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Tải bản thể tệp từ đám mây và lưu giữ cố định trên thiết bị. */
    fun downloadImageToInternalStorage(context: Context, imageUrl: String): String? {
        return try {
            val url = URL(imageUrl)
            val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
            saveBitmapToInternalStorage(context, bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Chuyển lưu hình ảnh ra thư mục Public Gallery lưu giữ lâu dài. */
    fun saveImageToPublicStorage(context: Context, sourceFile: File): String? {
        val filename = "species_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EcoLens")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        return if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    sourceFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                uri.toString()
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    resolver.delete(uri, null, null)
                } catch (deleteEx: Exception) {
                    deleteEx.printStackTrace()
                }
                null
            }
        } else {
            null
        }
    }
}