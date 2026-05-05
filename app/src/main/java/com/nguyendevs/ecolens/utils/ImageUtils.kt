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

        try {
            val futureTarget = com.bumptech.glide.Glide.with(context)
                .asBitmap()
                .load(if (uri.scheme == "http" || uri.scheme == "https") uri.toString() else uri)
                .submit(maxDimension, maxDimension)

            val bitmap = futureTarget.get()

            if (bitmap != null) {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
                }
                com.bumptech.glide.Glide.with(context).clear(futureTarget)

                if (!file.exists() || file.length() == 0L) {
                    throw Exception("Created file is empty or does not exist: ${file.absolutePath}")
                }
                return file
            } else {
                throw Exception("Failed to decode bitmap from URI: $uri")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to process image from URI: $uri", e)
        }
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

    /** Sao chép tệp từ URI vào bộ nhớ nội bộ mà không làm giảm chất lượng (dùng cho lịch sử). */
    fun saveUriToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val filename = "species_${System.currentTimeMillis()}.jpg"
            val destFile = File(context.filesDir, filename)
            
            if (uri.scheme == "http" || uri.scheme == "https") {
                // Tải ảnh từ web về
                val bitmap = com.bumptech.glide.Glide.with(context)
                    .asBitmap()
                    .load(uri.toString())
                    .submit()
                    .get()
                
                FileOutputStream(destFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
            } else {
                // Copy file nội bộ
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
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
            val futureTarget = com.bumptech.glide.Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .submit()
            val bitmap = futureTarget.get()
            val path = saveBitmapToInternalStorage(context, bitmap)
            com.bumptech.glide.Glide.with(context).clear(futureTarget)
            path
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