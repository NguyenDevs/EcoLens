package com.nguyendevs.ecolens.handlers

import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.Html
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.model.LoadingStage
import com.nguyendevs.ecolens.model.SpeciesInfo
import kotlinx.coroutines.*

class SpeciesInfoHandler(
    private val context: Context,
    private val speciesInfoCard: MaterialCardView,
    private val onCopySuccess: (String) -> Unit
) {

    private val handlerScope = CoroutineScope(Dispatchers.Main + Job())
    private val viewCache = mutableMapOf<Int, View>()

    init {
        cacheViews()
    }

    private fun cacheViews() {
        viewCache[R.id.tvCommonName] = speciesInfoCard.findViewById(R.id.tvCommonName)
        viewCache[R.id.tvScientificName] = speciesInfoCard.findViewById(R.id.tvScientificName)
        viewCache[R.id.tvConfidence] = speciesInfoCard.findViewById(R.id.tvConfidence)
        viewCache[R.id.confidenceCard] = speciesInfoCard.findViewById(R.id.confidenceCard)
        viewCache[R.id.iconConfidence] = speciesInfoCard.findViewById(R.id.iconConfidence)
        viewCache[R.id.btnCopyScientificName] = speciesInfoCard.findViewById(R.id.btnCopyScientificName)
        viewCache[R.id.btnShareInfo] = speciesInfoCard.findViewById(R.id.btnShareInfo)
        viewCache[R.id.taxonomyContainer] = speciesInfoCard.findViewById(R.id.taxonomyContainer)

        val rowIds = listOf(
            R.id.rowKingdom, R.id.rowPhylum, R.id.rowClass,
            R.id.rowOrder, R.id.rowFamily, R.id.rowGenus, R.id.rowSpecies
        )
        rowIds.forEach { id ->
            speciesInfoCard.findViewById<View>(id)?.let { viewCache[id] = it }
        }

        viewCache[R.id.tvKingdom] = speciesInfoCard.findViewById(R.id.tvKingdom)
        viewCache[R.id.tvPhylum] = speciesInfoCard.findViewById(R.id.tvPhylum)
        viewCache[R.id.tvClass] = speciesInfoCard.findViewById(R.id.tvClass)
        viewCache[R.id.tvOrder] = speciesInfoCard.findViewById(R.id.tvOrder)
        viewCache[R.id.tvFamily] = speciesInfoCard.findViewById(R.id.tvFamily)
        viewCache[R.id.tvGenus] = speciesInfoCard.findViewById(R.id.tvGenus)
        viewCache[R.id.tvSpecies] = speciesInfoCard.findViewById(R.id.tvSpecies)

        viewCache[R.id.sectionDescription] = speciesInfoCard.findViewById(R.id.sectionDescription)
        viewCache[R.id.sectionCharacteristics] = speciesInfoCard.findViewById(R.id.sectionCharacteristics)
        viewCache[R.id.sectionDistribution] = speciesInfoCard.findViewById(R.id.sectionDistribution)
        viewCache[R.id.sectionHabitat] = speciesInfoCard.findViewById(R.id.sectionHabitat)
        viewCache[R.id.sectionConservation] = speciesInfoCard.findViewById(R.id.sectionConservation)

        viewCache[R.id.tvDescription] = speciesInfoCard.findViewById(R.id.tvDescription)
        viewCache[R.id.tvCharacteristics] = speciesInfoCard.findViewById(R.id.tvCharacteristics)
        viewCache[R.id.tvDistribution] = speciesInfoCard.findViewById(R.id.tvDistribution)
        viewCache[R.id.tvHabitat] = speciesInfoCard.findViewById(R.id.tvHabitat)
        viewCache[R.id.tvConservationStatus] = speciesInfoCard.findViewById(R.id.tvConservationStatus)
    }

    fun displaySpeciesInfo(info: SpeciesInfo, imageUri: Uri?, stage: LoadingStage) {
        setupCopyButton(info)
        setupShareButton(info, imageUri)

        when (stage) {
            LoadingStage.NONE -> {
                handlerScope.coroutineContext.cancelChildren()
                clearAllViews()
            }
            LoadingStage.SCIENTIFIC_NAME -> {
                displayScientificName(info)
                displayConfidence(info, isWaiting = true)
            }
            LoadingStage.COMMON_NAME -> {
                displayCommonName(info)
                displayConfidence(info, isWaiting = false)
            }
            LoadingStage.TAXONOMY -> {
                displayTaxonomy(info)
            }
            LoadingStage.DESCRIPTION -> {
                displaySection(R.id.sectionDescription, R.id.tvDescription, info.description)
            }
            LoadingStage.CHARACTERISTICS -> {
                displaySection(R.id.sectionCharacteristics, R.id.tvCharacteristics, info.characteristics)
            }
            LoadingStage.DISTRIBUTION -> {
                displaySection(R.id.sectionDistribution, R.id.tvDistribution, info.distribution)
            }
            LoadingStage.HABITAT -> {
                displaySection(R.id.sectionHabitat, R.id.tvHabitat, info.habitat)
            }
            LoadingStage.CONSERVATION -> {
                displayConservationStatus(info.conservationStatus)
            }
            LoadingStage.COMPLETE -> {}
        }
    }

    private fun clearAllViews() {
        val viewsToHide = listOf(
            R.id.tvCommonName, R.id.tvScientificName, R.id.confidenceCard,
            R.id.taxonomyContainer, R.id.rowKingdom, R.id.rowPhylum, R.id.rowClass,
            R.id.rowOrder, R.id.rowFamily, R.id.rowGenus, R.id.rowSpecies,
            R.id.sectionDescription, R.id.sectionCharacteristics,
            R.id.sectionDistribution, R.id.sectionHabitat, R.id.sectionConservation
        )
        viewsToHide.forEach { id ->
            viewCache[id]?.let {
                it.visibility = View.GONE
                it.alpha = 0f
            }
        }
    }

    private fun displayScientificName(info: SpeciesInfo) {
        val tvScientificName = viewCache[R.id.tvScientificName] as? TextView
        tvScientificName?.let {
            it.text = info.scientificName
            it.visibility = View.VISIBLE
            fadeIn(it)
        }
    }

    private fun displayCommonName(info: SpeciesInfo) {
        val tvCommonName = viewCache[R.id.tvCommonName] as? TextView
        tvCommonName?.let {
            it.text = info.commonName
            it.visibility = View.VISIBLE
            fadeIn(it)
        }
    }

    private fun displayConfidence(info: SpeciesInfo, isWaiting: Boolean) {
        val tvConfidence = viewCache[R.id.tvConfidence] as? TextView
        val confidenceCard = viewCache[R.id.confidenceCard] as? MaterialCardView
        val iconConfidence = viewCache[R.id.iconConfidence] as? ImageView

        if (isWaiting) {
            tvConfidence?.text = "..."
            iconConfidence?.setImageResource(R.drawable.ic_rotate)
            iconConfidence?.imageTintList = ContextCompat.getColorStateList(context, R.color.text_secondary)
            confidenceCard?.setCardBackgroundColor(ContextCompat.getColor(context, R.color.gray_light))
            tvConfidence?.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        } else {
            val confidenceValue = info.confidence.coerceIn(0.0, 100.0)
            val confidencePercent = String.format("%.2f", confidenceValue)
            tvConfidence?.text = context.getString(R.string.confidence_format, confidencePercent)

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

        confidenceCard?.let {
            if (it.visibility != View.VISIBLE) {
                it.visibility = View.VISIBLE
                fadeIn(it)
            }
        }
    }

    private fun displayTaxonomy(info: SpeciesInfo) {
        val container = viewCache[R.id.taxonomyContainer]
        container?.let {
            if (it.visibility != View.VISIBLE) {
                it.visibility = View.VISIBLE
                fadeIn(it, 300)
            }
        }

        handlerScope.launch {
            val rows = listOf(
                Triple(R.id.rowKingdom, R.id.tvKingdom, info.kingdom),
                Triple(R.id.rowPhylum, R.id.tvPhylum, info.phylum),
                Triple(R.id.rowClass, R.id.tvClass, info.className),
                Triple(R.id.rowOrder, R.id.tvOrder, info.order),
                Triple(R.id.rowFamily, R.id.tvFamily, info.family),
                Triple(R.id.rowGenus, R.id.tvGenus, info.genus),
                Triple(R.id.rowSpecies, R.id.tvSpecies, info.species)
            )

            rows.forEach { (rowId, tvId, text) ->
                if (text.isNotEmpty() && text != "..." && text != "N/A") {
                    setTaxonomyRowData(rowId, tvId, text)
                    delay(100)
                }
            }
        }
    }

    private fun setTaxonomyRowData(rowId: Int, textViewId: Int, text: String) {
        val row = viewCache[rowId] as? LinearLayout
        val textView = viewCache[textViewId] as? TextView

        val formattedText = if (text.contains("<i>")) {
            "<b>" + text.replace("<i>", "</b><i>")
        } else {
            "<b>$text</b>"
        }

        val styledText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(formattedText)
        }

        textView?.text = styledText

        row?.let {
            if (it.visibility != View.VISIBLE) {
                it.visibility = View.VISIBLE
                fadeIn(it, 350)
            }
        }
    }

    private fun displaySection(sectionId: Int, textViewId: Int, text: String) {
        setSectionVisibility(sectionId, textViewId, text)
    }

    private fun displayConservationStatus(status: String) {
        val section = viewCache[R.id.sectionConservation] as? LinearLayout
        val textView = viewCache[R.id.tvConservationStatus] as? TextView

        if (status.isNotEmpty()) {
            textView?.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(status, Html.FROM_HTML_MODE_COMPACT)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(status)
            }

            textView?.setTextColor(ContextCompat.getColor(context, R.color.black))

            section?.let {
                if (it.visibility != View.VISIBLE) {
                    it.visibility = View.VISIBLE
                    fadeIn(it)
                }
            }
        } else {
            section?.visibility = View.GONE
        }
    }

    private fun setupCopyButton(info: SpeciesInfo) {
        val btnCopy = viewCache[R.id.btnCopyScientificName] as? ImageView
        btnCopy?.setOnClickListener {
            val textToCopy = info.scientificName
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Scientific Name", textToCopy)
            clipboard.setPrimaryClip(clip)
            onCopySuccess(textToCopy)
        }
    }

    private fun setupShareButton(info: SpeciesInfo, imageUri: Uri?) {
        val btnShare = viewCache[R.id.btnShareInfo] as? ImageView
        btnShare?.setOnClickListener {
            shareSpeciesInfo(info, imageUri)
        }
    }

    private fun setSectionVisibility(sectionId: Int, textViewId: Int, text: String) {
        val section = viewCache[sectionId] as? LinearLayout
        val textView = viewCache[textViewId] as? TextView

        if (text.isNotEmpty()) {
            val htmlText = text.trim()
                .replace("\n•", "<br>•")
                .replace("\n", "<br>")

            val spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(htmlText)
            }

            textView?.text = spanned
            section?.let {
                if (it.visibility != View.VISIBLE) {
                    it.visibility = View.VISIBLE
                    fadeIn(it)
                }
            }
        } else {
            section?.visibility = View.GONE
        }
    }

    private fun fadeIn(view: View, durationMs: Long = 400) {
        view.alpha = 0f
        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
            duration = durationMs
            interpolator = AccelerateDecelerateInterpolator()
            start()
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
            append("\n━━━━━━━━━━━━━━━━━━━━\n\n")
            append("📌 ${info.commonName}\n")
            append("🔬 ${info.scientificName}\n")
            append("✅ ${context.getString(R.string.label_confidence_template, confidencePercent)}\n\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append(context.getString(R.string.share_taxonomy_title))
            append("\n━━━━━━━━━━━━━━━━━━━━\n\n")

            if (info.kingdom.isNotEmpty()) append("• ${context.getString(R.string.label_kingdom)} ${stripHtml(info.kingdom)}\n")
            if (info.phylum.isNotEmpty()) append("• ${context.getString(R.string.label_phylum)} ${stripHtml(info.phylum)}\n")
            if (info.className.isNotEmpty()) append("• ${context.getString(R.string.label_class)} ${stripHtml(info.className)}\n")
            if (info.order.isNotEmpty()) append("• ${context.getString(R.string.label_order)} ${stripHtml(info.order)}\n")
            if (info.family.isNotEmpty()) append("• ${context.getString(R.string.label_family)} ${stripHtml(info.family)}\n")
            if (info.genus.isNotEmpty()) append("• ${context.getString(R.string.label_genus)} ${stripHtml(info.genus)}\n")
            if (info.species.isNotEmpty()) append("• ${context.getString(R.string.label_species)} ${stripHtml(info.species)}\n")

            val sections = listOf(
                info.description to R.string.share_desc_title,
                info.characteristics to R.string.share_char_title,
                info.distribution to R.string.share_dist_title,
                info.habitat to R.string.share_hab_title,
                info.conservationStatus to R.string.share_cons_title
            )

            sections.forEach { (content, titleRes) ->
                if (content.isNotEmpty()) {
                    append("\n━━━━━━━━━━━━━━━━━━━━\n")
                    append(context.getString(titleRes))
                    append("\n━━━━━━━━━━━━━━━━━━━━\n\n")
                    append(stripHtml(content))
                    append("\n")
                }
            }

            append("\n━━━━━━━━━━━━━━━━━━━━\n")
            append(context.getString(R.string.share_footer))
        }

        try {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                if (imageUri != null) {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    clipData = ClipData.newRawUri(null, imageUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject, info.commonName))
            }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_chooser_title)))
        } catch (e: Exception) {
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

    fun onDestroy() {
        handlerScope.cancel()
    }
}