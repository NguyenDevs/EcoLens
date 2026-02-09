package com.nguyendevs.ecolens.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.google.gson.Gson
import com.nguyendevs.ecolens.models.history.HistoryEntry
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import org.apache.poi.util.Units
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.XWPFDocument

object ExportUtils {

    enum class ExportFormat {
        DOCX,
        XLSX,
        PDF,
        JSON
    }

    fun saveImageToGallery(context: Context, imagePath: String): Boolean {
        val bitmap = BitmapFactory.decodeFile(imagePath) ?: return false
        val filename = "EcoLens_${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        var imageUri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues =
                        ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                            put(
                                    MediaStore.MediaColumns.RELATIVE_PATH,
                                    Environment.DIRECTORY_PICTURES + "/EcoLens"
                            )
                        }
                val contentResolver = context.contentResolver
                imageUri =
                        contentResolver.insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                        )
                fos = imageUri?.let { contentResolver.openOutputStream(it) }
            } else {
                val imagesDir =
                        Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_PICTURES
                                )
                                .toString() + "/EcoLens"
                val file = File(imagesDir)
                if (!file.exists()) file.mkdirs()
                val image = File(imagesDir, filename)
                fos = FileOutputStream(image)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Cleanup on error if URI was created
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && imageUri != null) {
                context.contentResolver.delete(imageUri, null, null)
            }
        }
        return false
    }

    fun exportHistory(
            context: Context,
            entry: HistoryEntry,
            format: ExportFormat,
            includeImage: Boolean
    ): String? {
        val filename = "EcoLens_Export_${System.currentTimeMillis()}"
        return when (format) {
            ExportFormat.DOCX -> exportToDocx(context, entry, filename, includeImage)
            ExportFormat.XLSX -> exportToXlsx(context, entry, filename, includeImage)
            ExportFormat.PDF -> exportToPdf(context, entry, filename, includeImage)
            ExportFormat.JSON -> exportToJson(context, entry, filename)
        }
    }

    private fun getOutputStream(
            context: Context,
            filename: String,
            extension: String,
            mimeType: String
    ): OutputStream? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues =
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "$filename.$extension")
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(
                                MediaStore.MediaColumns.RELATIVE_PATH,
                                Environment.DIRECTORY_DOCUMENTS + "/EcoLens/Exports"
                        )
                    }
            context.contentResolver.insert(
                            MediaStore.Files.getContentUri("external"),
                            contentValues
                    )
                    ?.let { context.contentResolver.openOutputStream(it) }
        } else {
            val docsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                            .toString() + "/EcoLens/Exports"
            val file = File(docsDir)
            if (!file.exists()) file.mkdirs()
            val outFile = File(docsDir, "$filename.$extension")
            FileOutputStream(outFile)
        }
    }

    private fun exportToDocx(
            context: Context,
            entry: HistoryEntry,
            filename: String,
            includeImage: Boolean
    ): String? {
        try {
            val document = XWPFDocument()

            // Title
            val titleParagraph = document.createParagraph()
            titleParagraph.alignment = ParagraphAlignment.CENTER
            val titleRun = titleParagraph.createRun()
            titleRun.text = "EcoLens - Species Report"
            titleRun.isBold = true
            titleRun.fontSize = 20
            titleRun.addBreak()

            // Image
            if (includeImage && entry.localImagePath.isNotEmpty()) {
                try {
                    val file = File(entry.localImagePath)
                    if (file.exists()) {
                        val iStream = FileInputStream(file)
                        val imgParagraph = document.createParagraph()
                        imgParagraph.alignment = ParagraphAlignment.CENTER
                        val imgRun = imgParagraph.createRun()
                        imgRun.addPicture(
                                iStream,
                                XWPFDocument.PICTURE_TYPE_JPEG,
                                file.name,
                                Units.toEMU(300.0),
                                Units.toEMU(300.0)
                        )
                        iStream.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Content
            val info = entry.speciesInfo
            addDocxParagraph(document, "Common Name: ", info.commonName)
            addDocxParagraph(document, "Scientific Name: ", info.scientificName)
            addDocxParagraph(document, "Family: ", info.family)
            addDocxParagraph(document, "Kingdom: ", info.kingdom)
            addDocxParagraph(document, "Conservation Status: ", info.conservationStatus)

            document.createParagraph().createRun().apply {
                addBreak()
                text = "Description:"
                isBold = true
            }
            document.createParagraph().createRun().text = info.description

            // Save
            val out =
                    getOutputStream(
                            context,
                            filename,
                            "docx",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    )
            out?.use { document.write(it) } ?: return null
            document.close()
            return "$filename.docx"
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun addDocxParagraph(doc: XWPFDocument, label: String, value: String) {
        if (value.isBlank()) return
        val p = doc.createParagraph()
        val r1 = p.createRun()
        r1.text = label
        r1.isBold = true
        val r2 = p.createRun()
        r2.text = value
    }

    private fun exportToXlsx(
            context: Context,
            entry: HistoryEntry,
            filename: String,
            includeImage: Boolean
    ): String? {
        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Species Info")

            var rowNum = 0
            val info = entry.speciesInfo

            // Image support in Excel is complex, skipping for simplicity or basic implementation
            // depending on libraries
            // Assuming we just list data for now as POI android issues might occur with drawings

            // If includeImage is strictly required for Excel, we need Drawing patriarch.
            // Let's implement basic rows first.

            val headers = listOf("Field", "Value")
            val headerRow = sheet.createRow(rowNum++)
            headers.forEachIndexed { index, s -> headerRow.createCell(index).setCellValue(s) }

            val data =
                    listOf(
                            "Common Name" to info.commonName,
                            "Scientific Name" to info.scientificName,
                            "Family" to info.family,
                            "Kingdom" to info.kingdom,
                            "Description" to info.description,
                            "Confidence" to "${info.confidence}"
                    )

            data.forEach { (key, value) ->
                val row = sheet.createRow(rowNum++)
                row.createCell(0).setCellValue(key)
                row.createCell(1).setCellValue(value)
            }

            // Image handling (Simplified: Add a note if image is requested but not fully supported
            // to avoid crashes)
            // Or try to add it.
            if (includeImage && entry.localImagePath.isNotEmpty()) {
                try {
                    val file = File(entry.localImagePath)
                    if (file.exists()) {
                        val inputStream = FileInputStream(file)
                        val bytes = inputStream.readBytes()
                        val pictureIdx = workbook.addPicture(bytes, XSSFWorkbook.PICTURE_TYPE_JPEG)
                        inputStream.close()

                        val drawing = sheet.createDrawingPatriarch()
                        val helper = workbook.creationHelper
                        val anchor = helper.createClientAnchor()

                        // Position image
                        anchor.setCol1(3)
                        anchor.setRow1(1)
                        anchor.setCol2(8)
                        anchor.setRow2(15)

                        drawing.createPicture(anchor, pictureIdx)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val out =
                    getOutputStream(
                            context,
                            filename,
                            "xlsx",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    )
            out?.use { workbook.write(it) } ?: return null
            workbook.close()
            return "$filename.xlsx"
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun exportToPdf(
            context: Context,
            entry: HistoryEntry,
            filename: String,
            includeImage: Boolean
    ): String? {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()
            val titlePaint =
                    Paint().apply {
                        textSize = 24f
                        isFakeBoldText = true
                        color = Color.BLACK
                    }
            val contentPaint =
                    Paint().apply {
                        textSize = 14f
                        color = Color.BLACK
                    }

            var y = 50f
            canvas.drawText("EcoLens Species Report", 50f, y, titlePaint)
            y += 40f

            // Image
            if (includeImage && entry.localImagePath.isNotEmpty()) {
                val file = File(entry.localImagePath)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
                    canvas.drawBitmap(scaledBitmap, 50f, y, paint)
                    y += 220f
                }
            }

            val info = entry.speciesInfo
            val lines =
                    listOf(
                            "Common Name: ${info.commonName}",
                            "Scientific Name: ${info.scientificName}",
                            "Kingdom: ${info.kingdom}",
                            "Family: ${info.family}",
                            "Conservation: ${info.conservationStatus}"
                    )

            for (line in lines) {
                canvas.drawText(line, 50f, y, contentPaint)
                y += 25f
            }

            // Initial description with simple wrapping
            y += 10f
            canvas.drawText("Description:", 50f, y, contentPaint)
            y += 20f

            // Simple text wrapping (very basic)
            val description = info.description
            val words = description.split(" ")
            var currentLine = ""
            for (word in words) {
                if (contentPaint.measureText(currentLine + word) < 450) {
                    currentLine += "$word "
                } else {
                    canvas.drawText(currentLine, 50f, y, contentPaint)
                    y += 20f
                    if (y > 800) break // Simple page overflow protection
                    currentLine = "$word "
                }
            }
            canvas.drawText(currentLine, 50f, y, contentPaint)

            pdfDocument.finishPage(page)

            val out = getOutputStream(context, filename, "pdf", "application/pdf")
            out?.use { pdfDocument.writeTo(it) } ?: return null
            pdfDocument.close()
            return "$filename.pdf"
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun exportToJson(context: Context, entry: HistoryEntry, filename: String): String? {
        try {
            val jsonString = Gson().toJson(entry)
            val out = getOutputStream(context, filename, "json", "application/json")
            out?.use { it.write(jsonString.toByteArray()) } ?: return null
            return "$filename.json"
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
