package com.nguyendevs.ecolens.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
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

/** Cung cấp tùy chọn xuất dữ liệu đa định dạng từ DOCX đến JSON. */
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

    private fun getCombinedConservation(context: Context, info: com.nguyendevs.ecolens.models.SpeciesInfo): String {
        val disabledMsg = context.getString(R.string.conservation_disabled_message)
        val noDataMsg = context.getString(R.string.no_data)
        
        return buildString {
            if (info.conservationStatus != disabledMsg && info.conservationStatus.isNotBlank() && info.conservationStatus != noDataMsg) {
                append(info.conservationStatus)
            }
            if (info.vnredlistStatus.isNotBlank() && info.vnredlistStatus != disabledMsg && info.vnredlistStatus != noDataMsg) {
                if (isNotEmpty()) {
                    append("<br><br>")
                }
                append(info.vnredlistStatus)
            }
        }
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
            titleRun.fontSize = 16
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
                        imgRun.addBreak()
                        iStream.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val info = entry.speciesInfo

            addDocxSectionHeader(document, context.getString(R.string.taxonomy_title))

            addDocxParagraph(document, context.getString(R.string.label_common_name), info.commonName, true)
            addDocxParagraph(document, context.getString(R.string.label_scientific_name), info.scientificName, true)
            addDocxParagraph(document, context.getString(R.string.label_kingdom), info.kingdom)
            addDocxParagraph(document, context.getString(R.string.label_phylum), info.phylum)
            addDocxParagraph(document, context.getString(R.string.label_class), info.className)
            addDocxParagraph(document, context.getString(R.string.label_order), info.taxorder)
            addDocxParagraph(document, context.getString(R.string.label_family), info.family)
            addDocxParagraph(document, context.getString(R.string.label_genus), info.genus)
            addDocxParagraph(document, context.getString(R.string.label_species), info.species)

            addDocxContentSection(document, context.getString(R.string.section_description), info.description)
            addDocxContentSection(document, context.getString(R.string.section_characteristics), info.characteristics)
            addDocxContentSection(document, context.getString(R.string.section_distribution), info.distribution)
            addDocxContentSection(document, context.getString(R.string.section_habitat), info.habitat)
            val combinedConservation = getCombinedConservation(context, info)
            if (combinedConservation.isNotEmpty()) {
                addDocxContentSection(document, context.getString(R.string.section_conservation), combinedConservation)
            }

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

    private fun addDocxSectionHeader(doc: XWPFDocument, title: String) {
        val p = doc.createParagraph()
        p.alignment = ParagraphAlignment.LEFT
        val r = p.createRun()
        r.setText(title)
        r.isBold = true
        r.isItalic = true
        r.fontSize = 15
        r.addBreak()
    }

    private fun addDocxParagraph(doc: XWPFDocument, label: String, value: String, addColon: Boolean = false) {
        if (value.isBlank()) return
        val p = doc.createParagraph()
        p.alignment = ParagraphAlignment.LEFT

        val r1 = p.createRun()
        r1.setText(if (addColon) "$label: " else "$label ")
        r1.isBold = true
        r1.fontSize = 13

        val r2 = p.createRun()
        r2.setText(stripTags(value))
        r2.fontSize = 13
    }

    private fun addDocxContentSection(doc: XWPFDocument, title: String, content: String) {
        if (content.isBlank()) return

        val titlePara = doc.createParagraph()
        titlePara.alignment = ParagraphAlignment.LEFT
        val titleRun = titlePara.createRun()
        titleRun.setText(title)
        titleRun.isBold = true
        titleRun.isItalic = true
        titleRun.fontSize = 13

        val contentPara = doc.createParagraph()
        contentPara.alignment = ParagraphAlignment.LEFT
        contentPara.setSpacingBetween(1.0)

        val segments = parseRichText(content)

        if (segments.isEmpty()) {
            val r = contentPara.createRun()
            r.setText(content)
            r.fontSize = 13
        } else {
            var isFirstSegment = true
            for (segment in segments) {
                val r = contentPara.createRun()
                if (segment.text == "\n") {
                    if (!isFirstSegment) {
                        r.addBreak()
                    }
                } else {
                    r.setText(segment.text)
                    r.isBold = segment.isBold
                    r.isItalic = segment.isItalic
                    if (segment.isUnderline) r.underline = org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE
                    r.fontSize = 13

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
                isFirstSegment = false
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
                valueCell.setCellValue(stripTags(value))
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

            val combinedConservation = getCombinedConservation(context, info)
            if (combinedConservation.isNotEmpty()) {
                sheet.createRow(rowNum++)
                addMergedSection(context.getString(R.string.section_conservation), combinedConservation)
            }

            sheet.setColumnWidth(0, 7500)
            sheet.setColumnWidth(1, 7500)
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
            val pageWidth = 595
            val pageHeight = 842
            val margin = 50f
            val contentWidth = pageWidth - (2 * margin)

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val titlePaint = Paint().apply {
                textSize = 22f
                isFakeBoldText = true
                color = Color.BLACK
                textAlign = Paint.Align.CENTER
            }

            val sectionHeaderPaint = Paint().apply {
                textSize = 19f
                isFakeBoldText = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                color = Color.BLACK
                textAlign = Paint.Align.LEFT
            }

            val labelPaint = Paint().apply {
                textSize = 18f
                isFakeBoldText = true
                color = Color.BLACK
                textAlign = Paint.Align.LEFT
            }

            val contentPaint = Paint().apply {
                textSize = 18f
                color = Color.BLACK
                textAlign = Paint.Align.LEFT
            }

            var y = margin + 20f

            fun checkNewPage() {
                if (y > pageHeight - margin - 50f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = margin + 20f
                }
            }

            canvas.drawText(
                context.getString(R.string.share_title),
                pageWidth / 2f,
                y,
                titlePaint
            )
            y += 40f

            if (includeImage && entry.localImagePath.isNotEmpty()) {
                val file = File(entry.localImagePath)
                if (file.exists()) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
                        val imageX = (pageWidth - 200) / 2f
                        canvas.drawBitmap(scaledBitmap, imageX, y, Paint())
                        y += 220f
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val info = entry.speciesInfo

            checkNewPage()
            canvas.drawText(context.getString(R.string.taxonomy_title), margin, y, sectionHeaderPaint)
            y += 30f

            fun drawLabelValue(label: String, value: String, addColon: Boolean = false) {
                if (value.isBlank()) return
                checkNewPage()
                val labelText = if (addColon) "$label: " else "$label "
                canvas.drawText(labelText, margin, y, labelPaint)
                val labelWidth = labelPaint.measureText(labelText)

                val cleanValue = stripTags(value)
                val words = cleanValue.split(" ")
                var currentLine = ""
                var lineX = margin + labelWidth

                val valuePaint = Paint().apply {
                    textSize = 18f
                    color = Color.BLACK
                    textAlign = Paint.Align.LEFT
                }

                for (word in words) {
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    if (valuePaint.measureText(testLine) < contentWidth - labelWidth) {
                        currentLine = testLine
                    } else {
                        canvas.drawText(currentLine, lineX, y, valuePaint)
                        y += 25f
                        checkNewPage()
                        currentLine = word
                        lineX = margin
                    }
                }
                if (currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine, lineX, y, valuePaint)
                }
                y += 25f
            }

            drawLabelValue(context.getString(R.string.label_common_name), info.commonName, true)
            drawLabelValue(context.getString(R.string.label_scientific_name), info.scientificName, true)
            drawLabelValue(context.getString(R.string.label_kingdom), info.kingdom)
            drawLabelValue(context.getString(R.string.label_phylum), info.phylum)
            drawLabelValue(context.getString(R.string.label_class), info.className)
            drawLabelValue(context.getString(R.string.label_order), info.taxorder)
            drawLabelValue(context.getString(R.string.label_family), info.family)
            drawLabelValue(context.getString(R.string.label_genus), info.genus)
            drawLabelValue(context.getString(R.string.label_species), info.species)

            fun drawContentSection(title: String, content: String) {
                if (content.isBlank()) return

                y += 10f
                checkNewPage()
                canvas.drawText(title, margin, y, sectionHeaderPaint)
                y += 25f

                val segments = parseRichText(content)

                if (segments.isEmpty()) {
                    val cleanContent = stripTags(content)
                    val lines = cleanContent.split("\n")

                    for (line in lines) {
                        if (line.isBlank()) continue

                        val words = line.split(" ")
                        var currentLine = ""

                        for (word in words) {
                            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                            if (contentPaint.measureText(testLine) < contentWidth) {
                                currentLine = testLine
                            } else {
                                if (currentLine.isNotEmpty()) {
                                    checkNewPage()
                                    canvas.drawText(currentLine, margin, y, contentPaint)
                                    y += 22f
                                }
                                currentLine = word
                            }
                        }

                        if (currentLine.isNotEmpty()) {
                            checkNewPage()
                            canvas.drawText(currentLine, margin, y, contentPaint)
                            y += 22f
                        }
                    }
                } else {
                    var currentLine = ""
                    var currentX = margin

                    for (segment in segments) {
                        if (segment.text == "\n") {
                            if (currentLine.isNotEmpty()) {
                                y += 22f
                                checkNewPage()
                            }
                            currentLine = ""
                            currentX = margin
                            continue
                        }

                        val paint = Paint().apply {
                            textSize = 18f
                            color = Color.BLACK
                            textAlign = Paint.Align.LEFT
                            isFakeBoldText = segment.isBold

                            if (segment.isItalic) {
                                typeface = if (segment.isBold) {
                                    Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                                } else {
                                    Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                                }
                            } else if (segment.isBold) {
                                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            }

                            if (segment.color != null) {
                                try {
                                    this.color = Color.parseColor(segment.color)
                                } catch (e: Exception) {
                                    this.color = Color.BLACK
                                }
                            }
                        }

                        val words = segment.text.split(" ")
                        for (i in words.indices) {
                            val word = words[i]
                            val wordWithSpace = if (i < words.size - 1) "$word " else word
                            val wordWidth = paint.measureText(wordWithSpace)

                            if (currentX + wordWidth > pageWidth - margin) {
                                if (currentLine.isNotEmpty()) {
                                    y += 22f
                                    checkNewPage()
                                }
                                currentX = margin
                                currentLine = ""
                            }

                            canvas.drawText(wordWithSpace, currentX, y, paint)
                            currentX += wordWidth
                            currentLine += wordWithSpace
                        }
                    }

                    if (currentLine.isNotEmpty()) {
                        y += 22f
                    }
                }
            }

            drawContentSection(context.getString(R.string.section_description), info.description)
            drawContentSection(context.getString(R.string.section_characteristics), info.characteristics)
            drawContentSection(context.getString(R.string.section_distribution), info.distribution)
            drawContentSection(context.getString(R.string.section_habitat), info.habitat)
            val combinedConservation = getCombinedConservation(context, info)
            if (combinedConservation.isNotEmpty()) {
                drawContentSection(context.getString(R.string.section_conservation), combinedConservation)
            }

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
            val info = entry.speciesInfo

            val cleanedData = mapOf(
                "title" to context.getString(R.string.share_title),
                "timestamp" to entry.timestamp,
                "taxonomy" to mapOf(
                    "commonName" to stripTags(info.commonName),
                    "scientificName" to stripTags(info.scientificName),
                    "kingdom" to stripTags(info.kingdom),
                    "phylum" to stripTags(info.phylum),
                    "class" to stripTags(info.className),
                    "order" to stripTags(info.taxorder),
                    "family" to stripTags(info.family),
                    "genus" to stripTags(info.genus),
                    "species" to stripTags(info.species)
                ),
                "description" to stripTags(info.description),
                "characteristics" to stripTags(info.characteristics),
                "distribution" to stripTags(info.distribution),
                "habitat" to stripTags(info.habitat),
                "conservationStatus" to getCombinedConservation(context, info).let { if (it.isNotEmpty()) stripTags(it) else null }
            )

            val gson = com.google.gson.GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
            val jsonString = gson.toJson(cleanedData)
            val out = getOutputStream(context, filename, "json", "application/json")
            out?.use { it.write(jsonString.toByteArray()) } ?: return null
            return "$filename.json"
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}