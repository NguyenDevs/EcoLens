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
        val confidenceValue = info.confidence.coerceIn(0.0, 100.0)
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
                iconConfidence?.setImageResource(R.drawable.ic_check_circle)
                iconConfidence?.imageTintList = ContextCompat.getColorStateList(context, R.color.confidence_high)
                confidenceCard?.setCardBackgroundColor(ContextCompat.getColor(context, R.color.confidence_bg_high))
                tvConfidence?.setTextColor(ContextCompat.getColor(context, R.color.confidence_text_high))
            }
            confidenceValue >= 25f -> {
                iconConfidence?.setImageResource(R.drawable.ic_check_warning_circle)
                iconConfidence?.imageTintList = ContextCompat.getColorStateList(context, R.color.confidence_medium)
                confidenceCard?.setCardBackgroundColor(ContextCompat.getColor(context, R.color.confidence_bg_medium))
                tvConfidence?.setTextColor(ContextCompat.getColor(context, R.color.confidence_text_medium))
            }
            else -> {
                iconConfidence?.setImageResource(R.drawable.ic_check_not_circle)
                iconConfidence?.imageTintList = ContextCompat.getColorStateList(context, R.color.confidence_low)
                confidenceCard?.setCardBackgroundColor(ContextCompat.getColor(context, R.color.confidence_bg_low))
                tvConfidence?.setTextColor(ContextCompat.getColor(context, R.color.confidence_text_low))
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
            append(context.getString(R.string.share_title))
            append("\n━━━━━━━━━━━━━━━━━━━━━\n\n")

            append("📌 ${info.commonName}\n")
            append("🔬 ${info.scientificName}\n")
            append("✅ ${context.getString(R.string.label_confidence_template, confidencePercent)}\n\n")

            append("━━━━━━━━━━━━━━━━━━━━━\n")
            append(context.getString(R.string.share_taxonomy_title))
            append("\n━━━━━━━━━━━━━━━━━━━━━\n\n")

            if (info.kingdom.isNotEmpty()) append("• ${context.getString(R.string.label_kingdom)} ${info.kingdom}\n")
            if (info.phylum.isNotEmpty()) append("• ${context.getString(R.string.label_phylum)} ${info.phylum}\n")
            if (info.className.isNotEmpty()) append("• ${context.getString(R.string.label_class)} ${info.className}\n")
            if (info.order.isNotEmpty()) append("• ${context.getString(R.string.label_order)} ${info.order}\n")
            if (info.family.isNotEmpty()) append("• ${context.getString(R.string.label_family)} ${info.family}\n")
            if (info.genus.isNotEmpty()) append("• ${context.getString(R.string.label_genus)} ${info.genus}\n")
            if (info.species.isNotEmpty()) append("• ${context.getString(R.string.label_species)} ${info.species}\n")

            if (info.description.isNotEmpty()) {
                append("\n━━━━━━━━━━━━━━━━━━━━━\n")
                append(context.getString(R.string.share_desc_title))
                append("\n━━━━━━━━━━━━━━━━━━━━━\n\n")
                append(stripHtml(info.description))
                append("\n")
            }

            if (info.characteristics.isNotEmpty()) {
                append("\n━━━━━━━━━━━━━━━━━━━━━\n")
                append(context.getString(R.string.share_char_title))
                append("\n━━━━━━━━━━━━━━━━━━━━━\n\n")
                append(stripHtml(info.characteristics))
                append("\n")
            }

            if (info.distribution.isNotEmpty()) {
                append("\n━━━━━━━━━━━━━━━━━━━━━\n")
                append(context.getString(R.string.share_dist_title))
                append("\n━━━━━━━━━━━━━━━━━━━━━\n\n")
                append(stripHtml(info.distribution))
                append("\n")
            }

            if (info.habitat.isNotEmpty()) {
                append("\n━━━━━━━━━━━━━━━━━━━━━\n")
                append(context.getString(R.string.share_hab_title))
                append("\n━━━━━━━━━━━━━━━━━━━━━\n\n")
                append(stripHtml(info.habitat))
                append("\n")
            }

            if (info.conservationStatus.isNotEmpty()) {
                append("\n━━━━━━━━━━━━━━━━━━━━━\n")
                append(context.getString(R.string.share_cons_title))
                append("\n━━━━━━━━━━━━━━━━━━━━━\n\n")
                append(stripHtml(info.conservationStatus))
                append("\n")
            }

            append("\n━━━━━━━━━━━━━━━━━━━━━\n")
            append(context.getString(R.string.share_footer))
        }

        try {
            if (imageUri != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject, info.commonName))
                    clipData = ClipData.newRawUri(null, imageUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooserIntent = Intent.createChooser(shareIntent, context.getString(R.string.share_chooser_title))
                chooserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                context.startActivity(chooserIntent)
            } else {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject, info.commonName))
                }
                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_chooser_title)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "${context.getString(R.string.error)}: ${e.message}", Toast.LENGTH_SHORT).show()
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