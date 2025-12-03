package com.nguyendevs.ecolens.handlers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.Html
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.model.SpeciesInfo

class SpeciesInfoHandler(
    private val context: Context,
    private val speciesInfoCard: MaterialCardView,
    private val onCopySuccess: (String) -> Unit
) {

    fun displaySpeciesInfo(info: SpeciesInfo, imageUri: Uri?) {
        setupCopyButton(info)
        setupShareButton(info, imageUri)
        displayBasicInfo(info)
        displayTaxonomy(info)
        displaySections(info)
        displayConservationStatus(info.conservationStatus)
    }

    private fun setupCopyButton(info: SpeciesInfo) {
        val btnCopy = speciesInfoCard.findViewById<ImageView>(R.id.btnCopyScientificName)
        btnCopy?.setOnClickListener {
            val textToCopy = info.scientificName
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Scientific Name", textToCopy)
            clipboard.setPrimaryClip(clip)
            //Toast.makeText(context, "Đã copy: $textToCopy", Toast.LENGTH_SHORT).show()

            onCopySuccess(textToCopy)
        }
    }

    private fun setupShareButton(info: SpeciesInfo, imageUri: Uri?) {
        val btnShare = speciesInfoCard.findViewById<ImageView>(R.id.btnShareInfo)
        btnShare?.setOnClickListener {
            shareSpeciesInfo(info, imageUri)
        }
    }

    private fun displayBasicInfo(info: SpeciesInfo) {
        val confidenceValue = info.confidence.coerceIn(0.0, 100.0) // Đảm bảo nằm trong 0-100
        val confidencePercent = String.format("%.2f", confidenceValue)

        speciesInfoCard.findViewById<TextView>(R.id.tvCommonName)?.text = info.commonName
        speciesInfoCard.findViewById<TextView>(R.id.tvScientificName)?.text = info.scientificName
        speciesInfoCard.findViewById<TextView>(R.id.tvConfidence)?.text =
            context.getString(R.string.confidence_format, confidencePercent)

        val confidenceCard = speciesInfoCard.findViewById<MaterialCardView>(R.id.confidenceCard)
        val iconConfidence = speciesInfoCard.findViewById<ImageView>(R.id.iconConfidence)
        val tvConfidence = speciesInfoCard.findViewById<TextView>(R.id.tvConfidence)

        when {
            confidenceValue >= 50f -> {
                // Cao: xanh lá
                iconConfidence?.setImageResource(R.drawable.ic_check_circle)
                iconConfidence?.imageTintList = ContextCompat.getColorStateList(context, R.color.confidence_high) // #00A86B
                confidenceCard?.setCardBackgroundColor(ContextCompat.getColor(context, R.color.confidence_bg_high)) // #E8F5E9
                tvConfidence?.setTextColor(ContextCompat.getColor(context, R.color.confidence_text_high)) // #00796B
            }
            confidenceValue >= 25f -> {
                // Trung bình: cam
                iconConfidence?.setImageResource(R.drawable.ic_check_warning_circle)
                iconConfidence?.imageTintList = ContextCompat.getColorStateList(context, R.color.confidence_medium) // #FF8C00 hoặc #F57C00
                confidenceCard?.setCardBackgroundColor(ContextCompat.getColor(context, R.color.confidence_bg_medium)) // #FFF3E0
                tvConfidence?.setTextColor(ContextCompat.getColor(context, R.color.confidence_text_medium)) // #E65100
            }
            else -> {
                // Thấp: đỏ
                iconConfidence?.setImageResource(R.drawable.ic_check_not_circle)
                iconConfidence?.imageTintList = ContextCompat.getColorStateList(context, R.color.confidence_low) // #D32F2F
                confidenceCard?.setCardBackgroundColor(ContextCompat.getColor(context, R.color.confidence_bg_low)) // #FFEBEE
                tvConfidence?.setTextColor(ContextCompat.getColor(context, R.color.confidence_text_low)) // #C62828
            }
        }
    }

    private fun displayTaxonomy(info: SpeciesInfo) {
        setTaxonomyRow(R.id.rowKingdom, R.id.tvKingdom, info.kingdom)
        setTaxonomyRow(R.id.rowPhylum, R.id.tvPhylum, info.phylum)
        setTaxonomyRow(R.id.rowClass, R.id.tvClass, info.className)
        setTaxonomyRow(R.id.rowOrder, R.id.tvOrder, info.order)
        setTaxonomyRow(R.id.rowFamily, R.id.tvFamily, info.family)
        setTaxonomyRow(R.id.rowGenus, R.id.tvGenus, info.genus)
        setTaxonomyRow(R.id.rowSpecies, R.id.tvSpecies, info.species)
    }

    private fun displaySections(info: SpeciesInfo) {
        setSectionVisibility(R.id.sectionDescription, R.id.tvDescription, info.description)
        setSectionVisibility(R.id.sectionCharacteristics, R.id.tvCharacteristics, info.characteristics)
        setSectionVisibility(R.id.sectionDistribution, R.id.tvDistribution, info.distribution)
        setSectionVisibility(R.id.sectionHabitat, R.id.tvHabitat, info.habitat)
    }

    private fun setTaxonomyRow(rowId: Int, textViewId: Int, text: String) {
        val row = speciesInfoCard.findViewById<LinearLayout>(rowId)
        val textView = speciesInfoCard.findViewById<TextView>(textViewId)

        if (text.isNotEmpty()) {
            val styledText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(text)
            }
            textView?.text = styledText
            row?.visibility = View.VISIBLE
        } else {
            row?.visibility = View.GONE
        }
    }

    private fun setSectionVisibility(sectionId: Int, textViewId: Int, text: String) {
        val section = speciesInfoCard.findViewById<LinearLayout>(sectionId)
        val textView = speciesInfoCard.findViewById<TextView>(textViewId)

        if (text.isNotEmpty()) {
            val htmlText = text.trim()
                .replace("\n•", "<br>•")
                .replace("\n", "<br>")
                .replace("<br>•", "<br>•")

            val spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(htmlText)
            }

            textView?.text = spanned
            section?.visibility = View.VISIBLE
        } else {
            section?.visibility = View.GONE
        }
    }

    private fun displayConservationStatus(status: String) {
        val section = speciesInfoCard.findViewById<LinearLayout>(R.id.sectionConservation)
        val textView = speciesInfoCard.findViewById<TextView>(R.id.tvConservationStatus)

        if (status.isNotEmpty()) {
            textView?.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(status, Html.FROM_HTML_MODE_COMPACT)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(status)
            }

            textView?.setTextColor(ContextCompat.getColor(context, R.color.black))

            section?.visibility = View.VISIBLE
        } else {
            section?.visibility = View.GONE
        }
    }

    private fun shareSpeciesInfo(info: SpeciesInfo, imageUri: Uri?) {
        val confidencePercent = if (info.confidence > 1) {
            String.format("%.2f", info.confidence)
        } else {
            String.format("%.2f", info.confidence * 100)
        }

        val shareText = buildString {
            append("🌿 THÔNG TIN LOÀI\n")
            append("━━━━━━━━━━━━━━━━━━━━━\n\n")

            append("📌 ${info.commonName}\n")
            append("🔬 ${info.scientificName}\n")
            append("✅ Độ tin cậy: $confidencePercent%\n\n")

            append("━━━━━━━━━━━━━━━━━━━━━\n")
            append("🔬 PHÂN LOẠI KHOA HỌC\n")
            append("━━━━━━━━━━━━━━━━━━━━━\n\n")

            if (info.kingdom.isNotEmpty()) append("• Giới: ${info.kingdom}\n")
            if (info.phylum.isNotEmpty()) append("• Ngành: ${info.phylum}\n")
            if (info.className.isNotEmpty()) append("• Lớp: ${info.className}\n")
            if (info.order.isNotEmpty()) append("• Bộ: ${info.order}\n")
            if (info.family.isNotEmpty()) append("• Họ: ${info.family}\n")
            if (info.genus.isNotEmpty()) append("• Chi: ${info.genus}\n")
            if (info.species.isNotEmpty()) append("• Loài: ${info.species}\n")

            if (info.description.isNotEmpty()) {
                append("\n━━━━━━━━━━━━━━━━━━━━━\n")
                append("📖 MÔ TẢ\n")
                append("━━━━━━━━━━━━━━━━━━━━━\n\n")
                append(stripHtml(info.description))
                append("\n")
            }

            if (info.characteristics.isNotEmpty()) {
                append("\n━━━━━━━━━━━━━━━━━━━━━\n")
                append("✨ ĐẶC ĐIỂM\n")
                append("━━━━━━━━━━━━━━━━━━━━━\n\n")
                append(stripHtml(info.characteristics))
                append("\n")
            }

            if (info.distribution.isNotEmpty()) {
                append("\n━━━━━━━━━━━━━━━━━━━━━\n")
                append("🌍 PHÂN BỐ\n")
                append("━━━━━━━━━━━━━━━━━━━━━\n\n")
                append(stripHtml(info.distribution))
                append("\n")
            }

            if (info.habitat.isNotEmpty()) {
                append("\n━━━━━━━━━━━━━━━━━━━━━\n")
                append("🏞️ MÔI TRƯỜNG SỐNG\n")
                append("━━━━━━━━━━━━━━━━━━━━━\n\n")
                append(stripHtml(info.habitat))
                append("\n")
            }

            if (info.conservationStatus.isNotEmpty()) {
                append("\n━━━━━━━━━━━━━━━━━━━━━\n")
                append("🛡️ TÌNH TRẠNG BẢO TỒN\n")
                append("━━━━━━━━━━━━━━━━━━━━━\n\n")
                append(stripHtml(info.conservationStatus))
                append("\n")
            }

            append("\n━━━━━━━━━━━━━━━━━━━━━\n")
            append("📱 Chia sẻ từ EcoLens App")
        }

        try {
            if (imageUri != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_SUBJECT, "Thông tin về ${info.commonName}")
                    clipData = ClipData.newRawUri(null, imageUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooserIntent = Intent.createChooser(shareIntent, "Chia sẻ thông tin loài qua")
                chooserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                context.startActivity(chooserIntent)
            } else {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_SUBJECT, "Thông tin về ${info.commonName}")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ thông tin loài qua"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Không thể chia sẻ: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stripHtml(html: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html).toString()
        }.trim()
    }
}