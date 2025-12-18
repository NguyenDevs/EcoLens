package com.nguyendevs.ecolens.handlers

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.Html
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
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
        when (stage) {
            LoadingStage.NONE -> {
                handlerScope.coroutineContext.cancelChildren()
                clearAllViews()
                return
            }
            LoadingStage.SCIENTIFIC_NAME -> {
                setupCopyButton(info)
                setupShareButton(info, imageUri)
                displayCommonName(SpeciesInfo(commonName = "...", scientificName = ""))
                displayScientificName(info)
                displayConfidence(info, isWaiting = true)
                viewCache[R.id.taxonomyContainer]?.visibility = View.VISIBLE
                viewCache[R.id.taxonomyContainer]?.alpha = 1f
            }
            LoadingStage.COMMON_NAME -> {
                displayCommonName(info)
                displayConfidence(info, isWaiting = false)
            }
            LoadingStage.TAXONOMY -> {
                displayTaxonomyWaterfall(info)
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
            LoadingStage.COMPLETE -> {
                setupCopyButton(info)
                setupShareButton(info, imageUri)
            }
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
                it.translationY = 0f
            }
        }

        val textViewsToClear = listOf(
            R.id.tvCommonName, R.id.tvScientificName, R.id.tvConfidence,
            R.id.tvKingdom, R.id.tvPhylum, R.id.tvClass, R.id.tvOrder,
            R.id.tvFamily, R.id.tvGenus, R.id.tvSpecies, R.id.tvDescription,
            R.id.tvCharacteristics, R.id.tvDistribution, R.id.tvHabitat, R.id.tvConservationStatus
        )
        textViewsToClear.forEach { (viewCache[it] as? TextView)?.text = "" }
    }

    private fun displayScientificName(info: SpeciesInfo) {
        val tvScientificName = viewCache[R.id.tvScientificName] as? TextView
        tvScientificName?.let {
            it.text = info.scientificName
            it.visibility = View.VISIBLE
            fadeIn(it, 300)
        }
    }

    private fun displayCommonName(info: SpeciesInfo) {
        val tvCommonName = viewCache[R.id.tvCommonName] as? TextView
        tvCommonName?.let {
            it.text = info.commonName
            it.visibility = View.VISIBLE
            fadeIn(it, 300)
        }
    }

    @SuppressLint("StringFormatInvalid")
    private fun displayConfidence(info: SpeciesInfo, isWaiting: Boolean) {
        val tvConfidence = viewCache[R.id.tvConfidence] as? TextView
        val confidenceCard = viewCache[R.id.confidenceCard] as? MaterialCardView
        val iconConfidence = viewCache[R.id.iconConfidence] as? ImageView

        if (isWaiting) {
            tvConfidence?.text = context.getString(R.string.confidence, "...%")
            iconConfidence?.setImageResource(R.drawable.ic_rotate)
            iconConfidence?.imageTintList = ContextCompat.getColorStateList(context, R.color.text_secondary)
            confidenceCard?.setCardBackgroundColor(ContextCompat.getColor(context, R.color.gray_light))
            tvConfidence?.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        } else {
            val confidenceValue = info.confidence.coerceIn(0.0, 100.0)
            val confidencePercent = String.format("%.2f", confidenceValue)
            tvConfidence?.text = context.getString(R.string.confidence_format, confidencePercent)

            val (icon, tint, bg, textCol) = when {
                confidenceValue >= 50f -> Quadruple(R.drawable.ic_check_circle, R.color.confidence_high, R.color.confidence_bg_high, R.color.confidence_text_high)
                confidenceValue >= 25f -> Quadruple(R.drawable.ic_check_warning_circle, R.color.confidence_medium, R.color.confidence_bg_medium, R.color.confidence_text_medium)
                else -> Quadruple(R.drawable.ic_check_not_circle, R.color.confidence_low, R.color.confidence_bg_low, R.color.confidence_text_low)
            }

            iconConfidence?.setImageResource(icon)
            iconConfidence?.imageTintList = ContextCompat.getColorStateList(context, tint)
            confidenceCard?.setCardBackgroundColor(ContextCompat.getColor(context, bg))
            tvConfidence?.setTextColor(ContextCompat.getColor(context, textCol))
        }

        confidenceCard?.let {
            if (it.visibility != View.VISIBLE) {
                it.visibility = View.VISIBLE
                fadeIn(it, 300)
            }
        }
    }

    private fun displayTaxonomyWaterfall(info: SpeciesInfo) {
        val container = viewCache[R.id.taxonomyContainer]
        container?.visibility = View.VISIBLE
        container?.alpha = 1f

        val rows = listOf(
            Triple(R.id.rowKingdom, R.id.tvKingdom, info.kingdom),
            Triple(R.id.rowPhylum, R.id.tvPhylum, info.phylum),
            Triple(R.id.rowClass, R.id.tvClass, info.className),
            Triple(R.id.rowOrder, R.id.tvOrder, info.order),
            Triple(R.id.rowFamily, R.id.tvFamily, info.family),
            Triple(R.id.rowGenus, R.id.tvGenus, info.genus),
            Triple(R.id.rowSpecies, R.id.tvSpecies, info.species)
        )

        var delayAmount = 0L
        rows.forEach { (rowId, tvId, text) ->
            val rowView = viewCache[rowId]
            val textView = viewCache[tvId] as? TextView

            if (rowView != null && textView != null && text.isNotEmpty() && text != "..." && text != "N/A") {
                val formattedText = if (text.contains("<i>")) "<b>" + text.replace("<i>", "</b><i>") else "<b>$text</b>"
                textView.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)
                } else {
                    @Suppress("DEPRECATION") Html.fromHtml(formattedText)
                }

                rowView.visibility = View.VISIBLE
                rowView.alpha = 0f
                rowView.translationY = 20f

                rowView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay(delayAmount)
                    .setInterpolator(DecelerateInterpolator())
                    .start()

                delayAmount += 80
            } else {
                rowView?.visibility = View.GONE
            }
        }
    }

    private fun displaySection(sectionId: Int, textViewId: Int, text: String) {
        val section = viewCache[sectionId] as? LinearLayout
        val textView = viewCache[textViewId] as? TextView

        if (text.isNotEmpty()) {
            val htmlText = text.trim().replace("\n•", "<br>•").replace("\n", "<br>")
            val spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION") Html.fromHtml(htmlText)
            }

            textView?.text = spanned
            section?.let {
                if (it.visibility != View.VISIBLE) {
                    it.visibility = View.VISIBLE
                    it.alpha = 0f
                    it.translationY = 20f
                    it.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(500)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
            }
        } else {
            section?.visibility = View.GONE
        }
    }

    private fun displayConservationStatus(status: String) {
        val section = viewCache[R.id.sectionConservation] as? LinearLayout
        val textView = viewCache[R.id.tvConservationStatus] as? TextView

        if (status.isNotEmpty()) {
            textView?.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(status, Html.FROM_HTML_MODE_COMPACT)
            } else {
                @Suppress("DEPRECATION") Html.fromHtml(status)
            }
            textView?.setTextColor(ContextCompat.getColor(context, R.color.black))

            section?.let {
                if (it.visibility != View.VISIBLE) {
                    it.visibility = View.VISIBLE
                    it.alpha = 0f
                    fadeIn(it, 400)
                }
            }
        } else {
            section?.visibility = View.GONE
        }
    }

    private fun setupCopyButton(info: SpeciesInfo) {
        viewCache[R.id.btnCopyScientificName]?.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Scientific Name", info.scientificName))
            onCopySuccess(info.scientificName)
        }
    }

    private fun setupShareButton(info: SpeciesInfo, imageUri: Uri?) {
        viewCache[R.id.btnShareInfo]?.setOnClickListener {
            shareSpeciesInfo(info, imageUri)
        }
    }

    private fun fadeIn(view: View, durationMs: Long) {
        view.animate()
            .alpha(1f)
            .setDuration(durationMs)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun shareSpeciesInfo(info: SpeciesInfo, imageUri: Uri?) {
        val confidencePercent = String.format("%.2f", if (info.confidence > 1) info.confidence else info.confidence * 100)
        val shareText = buildString {
            append(context.getString(R.string.share_title))
            append("\n━━━━━━━━━━━━━━━━━━━━\n\n")
            append("📌 ${info.commonName}\n🔬 ${info.scientificName}\n")
            append("✅ ${context.getString(R.string.label_confidence_template, confidencePercent)}\n\n")
            append("━━━━━━━━━━━━━━━━━━━━\n${context.getString(R.string.share_taxonomy_title)}\n━━━━━━━━━━━━━━━━━━━━\n\n")
            if (info.kingdom.isNotEmpty()) append("• ${context.getString(R.string.label_kingdom)} ${stripHtml(info.kingdom)}\n")
            if (info.phylum.isNotEmpty()) append("• ${context.getString(R.string.label_phylum)} ${stripHtml(info.phylum)}\n")
            if (info.className.isNotEmpty()) append("• ${context.getString(R.string.label_class)} ${stripHtml(info.className)}\n")
            if (info.order.isNotEmpty()) append("• ${context.getString(R.string.label_order)} ${stripHtml(info.order)}\n")
            if (info.family.isNotEmpty()) append("• ${context.getString(R.string.label_family)} ${stripHtml(info.family)}\n")
            if (info.genus.isNotEmpty()) append("• ${context.getString(R.string.label_genus)} ${stripHtml(info.genus)}\n")
            if (info.species.isNotEmpty()) append("• ${context.getString(R.string.label_species)} ${stripHtml(info.species)}\n")

            listOf(info.description to R.string.share_desc_title, info.characteristics to R.string.share_char_title, info.distribution to R.string.share_dist_title, info.habitat to R.string.share_hab_title, info.conservationStatus to R.string.share_cons_title).forEach { (content, title) ->
                if (content.isNotEmpty()) {
                    append("\n━━━━━━━━━━━━━━━━━━━━\n${context.getString(title)}\n━━━━━━━━━━━━━━━━━━━━\n\n${stripHtml(content)}\n")
                }
            }
            append("\n━━━━━━━━━━━━━━━━━━━━\n${context.getString(R.string.share_footer)}")
        }

        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                if (imageUri != null) {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    clipData = ClipData.newRawUri(null, imageUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject, info.commonName))
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_chooser_title)))
        } catch (e: Exception) {
            Toast.makeText(context, "${context.getString(R.string.error)}: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stripHtml(html: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            @Suppress("DEPRECATION") Html.fromHtml(html).toString()
        }.trim()
    }

    fun onDestroy() {
        handlerScope.cancel()
    }

    data class Quadruple<out A, out B, out C, out D>(val first: A, val second: B, val third: C, val fourth: D)
}