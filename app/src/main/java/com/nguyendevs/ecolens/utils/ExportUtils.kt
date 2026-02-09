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
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.models.history.HistoryEntry
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.util.Units
import org.apache.poi.xssf.usermodel.XSSFRichTextString
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.regex.Pattern

object ExportUtils {

    enum class ExportFormat {
        DOCX,
        XLSX,
        PDF,
        JSON
    }

    data class RichTextSegment(
        val text: String,
        val isBold: Boolean = false,
        val isItalic: Boolean = false,
        val isUnderline: Boolean = false,
        val color: String? = null
    )

    private fun parseRichText(input: String): List<RichTextSegment> {
        val segments = mutableListOf<RichTextSegment>()
        if (input.isEmpty()) return segments

        val text = input
        val stack = mutableListOf<Map<String, Any>>()
        var currentPos = 0
        var currentText = StringBuilder()

        while (currentPos < text.length) {
            val openTagStart = text.indexOf('<', currentPos)

            if (openTagStart == -1) {
                currentText.append(text.substring(currentPos))
                break
            }

            if (openTagStart > currentPos) {
                currentText.append(text.substring(currentPos, openTagStart))
            }

            val openTagEnd = text.indexOf('>', openTagStart)
            if (openTagEnd == -1) {
                currentText.append(text.substring(currentPos))
                break
            }

            val fullTag = text.substring(openTagStart, openTagEnd + 1)
            val tagContent = text.substring(openTagStart + 1, openTagEnd)

            when {
                tagContent == "br" || tagContent == "br/" -> {
                    val currentFormatting = getCurrentFormatting(stack)
                    if (currentText.isNotEmpty()) {
                        segments.add(RichTextSegment(
                            currentText.toString(),
                            currentFormatting["bold"] as? Boolean ?: false,
                            currentFormatting["italic"] as? Boolean ?: false,
                            currentFormatting["underline"] as? Boolean ?: false,
                            currentFormatting["color"] as? String
                        ))
                        currentText.clear()
                    }
                    segments.add(RichTextSegment("\n"))
                    currentPos = openTagEnd + 1
                }
                tagContent.startsWith("/") -> {
                    val tagName = tagContent.substring(1).trim()

                    if (currentText.isNotEmpty()) {
                        val currentFormatting = getCurrentFormatting(stack)
                        segments.add(RichTextSegment(
                            currentText.toString(),
                            currentFormatting["bold"] as? Boolean ?: false,
                            currentFormatting["italic"] as? Boolean ?: false,
                            currentFormatting["underline"] as? Boolean ?: false,
                            currentFormatting["color"] as? String
                        ))
                        currentText.clear()
                    }

                    if (stack.isNotEmpty()) {
                        val lastTag = stack.lastOrNull()
                        if (lastTag != null && lastTag["tag"] == tagName) {
                            stack.removeAt(stack.lastIndex)
                        }
                    }

                    currentPos = openTagEnd + 1
                }
                else -> {
                    val formatting = mutableMapOf<String, Any>()

                    when {
                        tagContent == "b" -> {
                            formatting["tag"] = "b"
                            formatting["bold"] = true
                        }
                        tagContent == "i" -> {
                            formatting["tag"] = "i"
                            formatting["italic"] = true
                        }
                        tagContent == "u" -> {
                            formatting["tag"] = "u"
                            formatting["underline"] = true
                        }
                        tagContent.startsWith("font") -> {
                            formatting["tag"] = "font"
                            val colorPattern = Pattern.compile("color\\s*=\\s*['\"]([^'\"]+)['\"]")
                            val matcher = colorPattern.matcher(tagContent)
                            if (matcher.find()) {
                                formatting["color"] = matcher.group(1)
                            }
                        }
                    }

                    if (currentText.isNotEmpty()) {
                        val currentFormatting = getCurrentFormatting(stack)
                        segments.add(RichTextSegment(
                            currentText.toString(),
                            currentFormatting["bold"] as? Boolean ?: false,
                            currentFormatting["italic"] as? Boolean ?: false,
                            currentFormatting["underline"] as? Boolean ?: false,
                            currentFormatting["color"] as? String
                        ))
                        currentText.clear()
                    }

                    if (formatting.isNotEmpty()) {
                        stack.add(formatting)
                    }

                    currentPos = openTagEnd + 1
                }
            }
        }

        if (currentText.isNotEmpty()) {
            val currentFormatting = getCurrentFormatting(stack)
            segments.add(RichTextSegment(
                currentText.toString(),
                currentFormatting["bold"] as? Boolean ?: false,
                currentFormatting["italic"] as? Boolean ?: false,
                currentFormatting["underline"] as? Boolean ?: false,
                currentFormatting["color"] as? String
            ))
        }

        return segments
    }

    private fun getCurrentFormatting(stack: List<Map<String, Any>>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        for (item in stack) {
            if (item["bold"] == true) result["bold"] = true
            if (item["italic"] == true) result["italic"] = true
            if (item["underline"] == true) result["underline"] = true
            if (item.containsKey("color")) result["color"] = item["color"]!!
        }

        return result
    }

    private fun stripTags(input: String): String {
        return input
            .replace(Regex("<br/?>"), "\n")
            .replace(Regex("<[^>]*>"), "")
    }

    fun saveImageToGallery(context: Context, imagePath: String): Boolean {
        val bitmap = BitmapFactory.decodeFile(imagePath) ?: return false
        val filename = "EcoLens_${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        var imageUri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EcoLens")
                }
                val contentResolver = context.contentResolver
                imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { contentResolver.openOutputStream(it) }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/EcoLens"
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
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$filename.$extension")
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/EcoLens/Exports")
            }
            val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            uri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString() + "/EcoLens/Exports"
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

            val titleParagraph = document.createParagraph()
            titleParagraph.alignment = ParagraphAlignment.CENTER
            val titleRun = titleParagraph.createRun()
            titleRun.setText(context.getString(R.string.share_title))
            titleRun.isBold = true
            titleRun.fontSize = 20
            titleRun.addBreak()

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

            val info = entry.speciesInfo

            addDocxParagraph(document, context.getString(R.string.label_common_name), info.commonName)
            addDocxParagraph(document, context.getString(R.string.label_scientific_name), info.scientificName)

            addDocxHeader(document, context.getString(R.string.taxonomy_title))
            addDocxParagraph(document, context.getString(R.string.label_kingdom), info.kingdom)
            addDocxParagraph(document, context.getString(R.string.label_phylum), info.phylum)
            addDocxParagraph(document, context.getString(R.string.label_class), info.className)
            addDocxParagraph(document, context.getString(R.string.label_order), info.taxorder)
            addDocxParagraph(document, context.getString(R.string.label_family), info.family)
            addDocxParagraph(document, context.getString(R.string.label_genus), info.genus)
            addDocxParagraph(document, context.getString(R.string.label_species), info.species)

            addDocxSection(document, context.getString(R.string.section_description), info.description)
            addDocxSection(document, context.getString(R.string.section_characteristics), info.characteristics)
            addDocxSection(document, context.getString(R.string.section_distribution), info.distribution)
            addDocxSection(document, context.getString(R.string.section_habitat), info.habitat)
            addDocxSection(document, context.getString(R.string.section_conservation), info.conservationStatus)

            val out = getOutputStream(
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
        r1.setText("$label ")
        r1.isBold = true

        val segments = parseRichText(value)
        if (segments.isEmpty()) {
            val r2 = p.createRun()
            r2.setText(value)
        } else {
            for (segment in segments) {
                val r = p.createRun()
                r.setText(segment.text)
                r.isBold = segment.isBold
                r.isItalic = segment.isItalic
                if (segment.isUnderline) r.underline = org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE
                if (segment.color != null) {
                    try {
                        val color = Color.parseColor(segment.color)
                        val hexColor = String.format("%02X%02X%02X", Color.red(color), Color.green(color), Color.blue(color))
                        r.color = hexColor
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun addDocxHeader(doc: XWPFDocument, title: String) {
        val p = doc.createParagraph()
        val r = p.createRun()
        r.addBreak()
        r.setText(title)
        r.isBold = true
        r.fontSize = 14
    }

    private fun addDocxSection(doc: XWPFDocument, title: String, content: String) {
        if (content.isBlank()) return
        addDocxHeader(doc, title)
        val p = doc.createParagraph()

        val segments = parseRichText(content)
        if (segments.isEmpty()) {
            val r = p.createRun()
            r.setText(content)
        } else {
            for (segment in segments) {
                val r = p.createRun()
                r.setText(segment.text)
                r.isBold = segment.isBold
                r.isItalic = segment.isItalic
                if (segment.isUnderline) r.underline = org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE
                if (segment.color != null) {
                    try {
                        val color = Color.parseColor(segment.color)
                        val hexColor = String.format("%02X%02X%02X", Color.red(color), Color.green(color), Color.blue(color))
                        r.color = hexColor
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
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

            val labelStyle = workbook.createCellStyle().apply {
                val font = workbook.createFont()
                font.bold = true
                setFont(font)
                verticalAlignment = VerticalAlignment.TOP
            }

            val valueStyle = workbook.createCellStyle().apply {
                wrapText = true
                verticalAlignment = VerticalAlignment.TOP
            }

            var rowNum = 0
            val info = entry.speciesInfo

            fun addSimpleRow(label: String, value: String) {
                if (value.isBlank()) return
                val row = sheet.createRow(rowNum++)
                val labelCell = row.createCell(0)
                labelCell.setCellValue(label)
                labelCell.cellStyle = labelStyle

                val valueCell = row.createCell(1)
                val richText = XSSFRichTextString()
                val segments = parseRichText(value)

                if (segments.isEmpty()) {
                    richText.append(value)
                } else {
                    for (segment in segments) {
                        val font = workbook.createFont()
                        if (segment.isBold) font.bold = true
                        if (segment.isItalic) font.italic = true
                        if (segment.isUnderline) font.underline = Font.U_SINGLE

                        if (segment.color != null) {
                            try {
                                val color = Color.parseColor(segment.color)
                                val r = Color.red(color).toByte()
                                val g = Color.green(color).toByte()
                                val b = Color.blue(color).toByte()

                                val xssfColor = org.apache.poi.xssf.usermodel.XSSFColor(
                                    byteArrayOf(r, g, b),
                                    null
                                )
                                font.setColor(xssfColor)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        richText.append(segment.text, font)
                    }
                }

                valueCell.setCellValue(richText)
                valueCell.cellStyle = valueStyle
            }

            fun addMergedSection(label: String, value: String) {
                if (value.isBlank()) return
                val startRow = rowNum

                val labelRow = sheet.createRow(rowNum++)
                val labelCell = labelRow.createCell(0)
                labelCell.setCellValue(label)
                labelCell.cellStyle = labelStyle

                val valueCell = labelRow.createCell(1)
                val richText = XSSFRichTextString()
                val segments = parseRichText(value)

                if (segments.isEmpty()) {
                    richText.append(value)
                } else {
                    for (segment in segments) {
                        val font = workbook.createFont()
                        if (segment.isBold) font.bold = true
                        if (segment.isItalic) font.italic = true
                        if (segment.isUnderline) font.underline = Font.U_SINGLE

                        if (segment.color != null) {
                            try {
                                val color = Color.parseColor(segment.color)
                                val r = Color.red(color).toByte()
                                val g = Color.green(color).toByte()
                                val b = Color.blue(color).toByte()

                                val xssfColor = org.apache.poi.xssf.usermodel.XSSFColor(
                                    byteArrayOf(r, g, b),
                                    null
                                )
                                font.setColor(xssfColor)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        richText.append(segment.text, font)
                    }
                }

                valueCell.setCellValue(richText)
                valueCell.cellStyle = valueStyle

                val mergeRegion = org.apache.poi.ss.util.CellRangeAddress(
                    startRow,
                    startRow + 5,
                    1,
                    7
                )
                sheet.addMergedRegion(mergeRegion)

                for (i in 1..5) {
                    sheet.createRow(rowNum++)
                }
            }

            addSimpleRow(context.getString(R.string.label_common_name), info.commonName)
            addSimpleRow(context.getString(R.string.label_scientific_name), info.scientificName)
            sheet.createRow(rowNum++)

            addSimpleRow(context.getString(R.string.label_kingdom), info.kingdom)
            addSimpleRow(context.getString(R.string.label_phylum), info.phylum)
            addSimpleRow(context.getString(R.string.label_class), info.className)
            addSimpleRow(context.getString(R.string.label_order), info.taxorder)
            addSimpleRow(context.getString(R.string.label_family), info.family)
            addSimpleRow(context.getString(R.string.label_genus), info.genus)
            addSimpleRow(context.getString(R.string.label_species), info.species)
            sheet.createRow(rowNum++)

            addMergedSection(context.getString(R.string.section_description), info.description)
            sheet.createRow(rowNum++)

            addMergedSection(context.getString(R.string.section_characteristics), info.characteristics)
            sheet.createRow(rowNum++)

            addMergedSection(context.getString(R.string.section_distribution), info.distribution)
            sheet.createRow(rowNum++)

            addMergedSection(context.getString(R.string.section_habitat), info.habitat)
            sheet.createRow(rowNum++)

            addMergedSection(context.getString(R.string.section_conservation), info.conservationStatus)

            sheet.setColumnWidth(0, 9500)
            sheet.setColumnWidth(1, 8700)
            for (i in 2..7) {
                sheet.setColumnWidth(i, 3000)
            }

            val out = getOutputStream(
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
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val titlePaint = Paint().apply {
                textSize = 24f
                isFakeBoldText = true
                color = Color.BLACK
            }
            val contentPaint = Paint().apply {
                textSize = 14f
                color = Color.BLACK
            }
            val labelPaint = Paint().apply {
                textSize = 14f
                isFakeBoldText = true
                color = Color.BLACK
            }

            var y = 50f
            canvas.drawText(context.getString(R.string.share_title), 50f, y, titlePaint)
            y += 40f

            if (includeImage && entry.localImagePath.isNotEmpty()) {
                val file = File(entry.localImagePath)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
                    canvas.drawBitmap(scaledBitmap, 50f, y, Paint())
                    y += 220f
                }
            }

            val info = entry.speciesInfo

            fun drawLine(label: String, value: String) {
                if (value.isBlank()) return
                canvas.drawText("$label ", 50f, y, labelPaint)
                val labelWidth = labelPaint.measureText("$label ")
                canvas.drawText(stripTags(value), 50f + labelWidth, y, contentPaint)
                y += 25f
            }

            drawLine(context.getString(R.string.label_common_name), info.commonName)
            drawLine(context.getString(R.string.label_scientific_name), info.scientificName)

            y += 10f
            canvas.drawText(context.getString(R.string.taxonomy_title), 50f, y, labelPaint)
            y += 25f

            drawLine(context.getString(R.string.label_kingdom), info.kingdom)
            drawLine(context.getString(R.string.label_phylum), info.phylum)
            drawLine(context.getString(R.string.label_class), info.className)
            drawLine(context.getString(R.string.label_order), info.taxorder)
            drawLine(context.getString(R.string.label_family), info.family)
            drawLine(context.getString(R.string.label_genus), info.genus)
            drawLine(context.getString(R.string.label_species), info.species)

            fun drawSection(title: String, content: String) {
                if (content.isBlank()) return
                y += 10f
                canvas.drawText(title, 50f, y, labelPaint)
                y += 20f

                val cleanContent = stripTags(content)
                val words = cleanContent.split(" ")
                var currentLine = ""
                for (word in words) {
                    if (contentPaint.measureText(currentLine + word) < 450) {
                        currentLine += "$word "
                    } else {
                        canvas.drawText(currentLine, 50f, y, contentPaint)
                        y += 20f
                        if (y > 800) break
                        currentLine = "$word "
                    }
                }
                canvas.drawText(currentLine, 50f, y, contentPaint)
                y += 20f
            }

            drawSection(context.getString(R.string.section_description), info.description)
            drawSection(context.getString(R.string.section_characteristics), info.characteristics)
            drawSection(context.getString(R.string.section_distribution), info.distribution)
            drawSection(context.getString(R.string.section_habitat), info.habitat)
            drawSection(context.getString(R.string.section_conservation), info.conservationStatus)

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