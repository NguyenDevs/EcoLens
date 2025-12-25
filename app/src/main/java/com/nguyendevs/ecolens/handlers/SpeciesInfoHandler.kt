package com.nguyendevs.ecolens.handlers

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.graphics.drawable.shapes.Shape
import android.net.Uri
import android.os.Build
import android.text.Html
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.model.LoadingStage
import com.nguyendevs.ecolens.model.SpeciesInfo
import kotlinx.coroutines.*

class SpeciesInfoHandler(
    private val context: Context,
    private val speciesInfoCard: MaterialCardView,
    private val onCopySuccess: (String) -> Unit,
    private val onRetryClick: () -> Unit
) {
    private val handlerScope = CoroutineScope(Dispatchers.Main + Job())
    private val viewCache = mutableMapOf<Int, View>()
    private val displayedRows = mutableSetOf<Int>()
    private val renderedSections = mutableSetOf<Int>()
    private var isInitialLoad = true
    private var allSectionsRendered = false
    private var confidenceRotationAnimator: ObjectAnimator? = null
    private var taxonomyShimmerAnimator: ValueAnimator? = null

    init {
        cacheViews()
    }

    private fun renderHtml(textView: TextView, htmlContent: String) {
        textView.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(htmlContent)
        }
    }

    fun displaySpeciesInfo(info: SpeciesInfo, imageUri: Uri?, stage: LoadingStage) {
        if (stage == LoadingStage.NONE) {
            handlerScope.coroutineContext.cancelChildren()
            clearAllViews()
            isInitialLoad = true
            renderedSections.clear()
            allSectionsRendered = false
            return
        }

        if (info.scientificName.isNotEmpty()) {
            displayScientificName(info)
        }
        if (info.commonName.isNotEmpty() && info.commonName != "...") {
            displayCommonName(info)
        }
        if (stage != LoadingStage.SCIENTIFIC_NAME) {
            displayConfidence(info, isWaiting = false)
        }
        when (stage) {
            LoadingStage.NONE -> {
                handlerScope.coroutineContext.cancelChildren()
                clearAllViews()
                isInitialLoad = true
                renderedSections.clear()
                allSectionsRendered = false
            }

            LoadingStage.SCIENTIFIC_NAME -> {
                isInitialLoad = true
                displayCommonName(SpeciesInfo(commonName = "...", scientificName = ""))
                prepareTaxonomyContainer()
                setupCopyButton(info)
                showCopyButtonAnimation()
                hideButtons()
                displayConfidence(info, isWaiting = true)
            }

            LoadingStage.COMMON_NAME -> {
            }

            LoadingStage.TAXONOMY -> {
                stopTaxonomyShimmer()
                displayTaxonomyWaterfall(info)
            }

            LoadingStage.DESCRIPTION -> {
                stopTaxonomyShimmer()
                displayTaxonomyWaterfall(info)
                displaySection(R.id.sectionDescription, R.id.tvDescription, info.description, shouldScroll = false)
                checkIfAllSectionsRendered(info, imageUri)
            }

            LoadingStage.CHARACTERISTICS -> {
                stopTaxonomyShimmer()
                displayTaxonomyWaterfall(info)
                displaySection(R.id.sectionCharacteristics, R.id.tvCharacteristics, info.characteristics, shouldScroll = false)
                checkIfAllSectionsRendered(info, imageUri)
            }

            LoadingStage.DISTRIBUTION -> {
                displaySection(R.id.sectionDistribution, R.id.tvDistribution, info.distribution, shouldScroll = false)
                checkIfAllSectionsRendered(info, imageUri)
            }

            LoadingStage.HABITAT -> {
                displaySection(R.id.sectionHabitat, R.id.tvHabitat, info.habitat, shouldScroll = false)
                checkIfAllSectionsRendered(info, imageUri)
            }

            LoadingStage.CONSERVATION -> {
                displayConservationStatus(info.conservationStatus, shouldScroll = false)
                checkIfAllSectionsRendered(info, imageUri)
            }

            LoadingStage.COMPLETE -> {
                isInitialLoad = false
                stopTaxonomyShimmer()
                displayTaxonomyWaterfall(info)

                allSectionsRendered = true

                setupShareButton(info, imageUri)
                showShareButtonAnimation()

                if (info.confidence < 50.0) {
                    showRetryButtonAnimation()
                } else {
                    hideRetryButton()
                }
            }
        }
    }

    fun onDestroy() {
        stopConfidenceAnimation()
        stopTaxonomyShimmer()
        handlerScope.cancel()
    }

    private fun cacheViews() {
        viewCache[R.id.tvCommonName] = speciesInfoCard.findViewById(R.id.tvCommonName)
        viewCache[R.id.tvScientificName] = speciesInfoCard.findViewById(R.id.tvScientificName)
        viewCache[R.id.tvConfidence] = speciesInfoCard.findViewById(R.id.tvConfidence)
        viewCache[R.id.confidenceCard] = speciesInfoCard.findViewById(R.id.confidenceCard)
        viewCache[R.id.iconConfidence] = speciesInfoCard.findViewById(R.id.iconConfidence)
        viewCache[R.id.btnCopyScientificName] = speciesInfoCard.findViewById(R.id.btnCopyScientificName)
        viewCache[R.id.btnShareInfo] = speciesInfoCard.findViewById(R.id.btnShareInfo)
        viewCache[R.id.btnRetryIdentification] = speciesInfoCard.findViewById(R.id.btnRetryIdentification)
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

    private fun clearAllViews() {
        displayedRows.clear()
        renderedSections.clear()
        allSectionsRendered = false
        stopConfidenceAnimation()
        stopTaxonomyShimmer()

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

        val textViews = listOf(
            R.id.tvKingdom, R.id.tvPhylum, R.id.tvClass, R.id.tvOrder,
            R.id.tvFamily, R.id.tvGenus, R.id.tvSpecies
        )
        textViews.forEach { (viewCache[it] as? TextView)?.text = "" }
    }

    private fun checkIfAllSectionsRendered(info: SpeciesInfo, imageUri: Uri?) {
        val sectionsWithContent = mutableSetOf<Int>()

        if (info.description.isNotEmpty()) sectionsWithContent.add(R.id.sectionDescription)
        if (info.characteristics.isNotEmpty()) sectionsWithContent.add(R.id.sectionCharacteristics)
        if (info.distribution.isNotEmpty()) sectionsWithContent.add(R.id.sectionDistribution)
        if (info.habitat.isNotEmpty()) sectionsWithContent.add(R.id.sectionHabitat)
        if (info.conservationStatus.isNotEmpty()) sectionsWithContent.add(R.id.sectionConservation)

        val allRendered = sectionsWithContent.all { sectionId ->
            renderedSections.contains(sectionId)
        }

        if (allRendered && !allSectionsRendered) {
            allSectionsRendered = true

            if (!isInitialLoad) {
                setupShareButton(info, imageUri)
                showShareButtonAnimation()
            }
        }
    }

    private fun displayScientificName(info: SpeciesInfo) {
        val tvScientificName = viewCache[R.id.tvScientificName] as? TextView
        tvScientificName?.let {
            renderHtml(it, info.scientificName)
            it.visibility = View.VISIBLE
            fadeIn(it, 300)
        }
    }

    private fun displayCommonName(info: SpeciesInfo) {
        val tvCommonName = viewCache[R.id.tvCommonName] as? TextView
        tvCommonName?.let {
            if (info.commonName == "...") {
                it.text = "..."
                it.alpha = 0f
                it.setTextColor(Color.TRANSPARENT)
            } else {
                it.setTextColor(ContextCompat.getColor(context, R.color.green_primary))
                renderHtml(it, info.commonName)
                it.visibility = View.VISIBLE
                fadeIn(it, 300)
            }
        }
    }

    @SuppressLint("StringFormatInvalid")
    private fun displayConfidence(info: SpeciesInfo, isWaiting: Boolean) {
        val tvConfidence = viewCache[R.id.tvConfidence] as? TextView
        val confidenceCard = viewCache[R.id.confidenceCard] as? MaterialCardView
        val iconConfidence = viewCache[R.id.iconConfidence] as? ImageView

        if (isWaiting) {
            tvConfidence?.text = context.getString(R.string.confidence, "...%")
            tvConfidence?.textSize = 13f
            iconConfidence?.setImageResource(R.drawable.ic_rotate)
            iconConfidence?.imageTintList = ContextCompat.getColorStateList(context, R.color.text_secondary)
            confidenceCard?.setCardBackgroundColor(ContextCompat.getColor(context, R.color.gray_light))
            tvConfidence?.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))

            if (confidenceRotationAnimator == null && iconConfidence != null) {
                confidenceRotationAnimator = ObjectAnimator.ofFloat(iconConfidence, "rotation", 0f, 360f).apply {
                    duration = 1000
                    repeatCount = ObjectAnimator.INFINITE
                    interpolator = LinearInterpolator()
                    start()
                }
            }
        } else {
            stopConfidenceAnimation()

            val confidenceValue = info.confidence.coerceIn(0.0, 100.0)
            val confidencePercent = String.format("%.2f", confidenceValue)
            tvConfidence?.text = context.getString(R.string.confidence_format, confidencePercent)

            val (icon, tint, bg, text) = when {
                confidenceValue >= 50f -> Quadruple(
                    R.drawable.ic_check_circle,
                    R.color.confidence_high,
                    R.color.confidence_high_bg,
                    R.color.confidence_high_text
                )

                confidenceValue >= 25f -> Quadruple(
                    R.drawable.ic_check_warning_circle,
                    R.color.confidence_medium,
                    R.color.confidence_medium_bg,
                    R.color.confidence_medium_text
                )

                else -> Quadruple(
                    R.drawable.ic_check_not_circle,
                    R.color.confidence_low,
                    R.color.confidence_low_bg,
                    R.color.confidence_low_text
                )
            }

            iconConfidence?.setImageResource(icon)
            iconConfidence?.imageTintList = ContextCompat.getColorStateList(context, tint)
            confidenceCard?.setCardBackgroundColor(ContextCompat.getColor(context, bg))
            tvConfidence?.setTextColor(ContextCompat.getColor(context, text))
        }

        confidenceCard?.let {
            if (it.visibility != View.VISIBLE) {
                it.visibility = View.VISIBLE
                fadeIn(it, 300)
            }
        }
    }

    private fun prepareTaxonomyContainer() {
        val container = viewCache[R.id.taxonomyContainer]
        container?.visibility = View.VISIBLE
        container?.alpha = 1f

        startTaxonomyShimmer(container)

        val rowIds = listOf(
            R.id.rowKingdom, R.id.rowPhylum, R.id.rowClass,
            R.id.rowOrder, R.id.rowFamily, R.id.rowGenus, R.id.rowSpecies
        )
        rowIds.forEach { id ->
            viewCache[id]?.apply {
                visibility = View.INVISIBLE
                alpha = 0f
                translationY = 0f
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
            Triple(R.id.rowOrder, R.id.tvOrder, info.taxorder),
            Triple(R.id.rowFamily, R.id.tvFamily, info.family),
            Triple(R.id.rowGenus, R.id.tvGenus, info.genus),
            Triple(R.id.rowSpecies, R.id.tvSpecies, info.species)
        )

        rows.forEach { (rowId, tvId, text) ->
            val rowView = viewCache[rowId]
            val textView = viewCache[tvId] as? TextView

            if (rowView != null && textView != null) {
                val hasData = text.isNotEmpty() && text != "..." && text != "N/A"

                if (hasData) {
                    if (!displayedRows.contains(rowId)) {
                        renderHtml(textView, text)

                        rowView.visibility = View.VISIBLE
                        rowView.alpha = 0f
                        rowView.translationY = -10f

                        rowView.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(300)
                            .setInterpolator(DecelerateInterpolator())
                            .start()

                        displayedRows.add(rowId)
                    }
                } else {
                    if (!displayedRows.contains(rowId)) {
                        rowView.visibility = View.INVISIBLE
                        rowView.alpha = 0f
                    }
                }
            }
        }
    }

    private fun startTaxonomyShimmer(view: View?) {
        if (view == null || taxonomyShimmerAnimator != null) return

        taxonomyShimmerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()

            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                val width = view.width.toFloat()
                val height = view.height.toFloat()

                if (width <= 0 || height <= 0) return@addUpdateListener

                val diagonal = Math.sqrt((width * width + height * height).toDouble()).toFloat()
                val shimmerWidth = diagonal * 0.5f
                val offset = diagonal * (progress - 0.3f)

                val backgroundColor = Color.parseColor("#ECEFF1")
                val transparent = Color.parseColor("#00ECEFF1")
                val fadeIn1 = Color.parseColor("#40F5F7F9")
                val fadeIn2 = Color.parseColor("#80F8F9FB")
                val shimmerColor = Color.parseColor("#FFFAFBFC")
                val fadeOut2 = Color.parseColor("#80F8F9FB")
                val fadeOut1 = Color.parseColor("#40F5F7F9")

                val gradient = LinearGradient(
                    offset, offset,
                    offset + shimmerWidth, offset + shimmerWidth,
                    intArrayOf(transparent, fadeIn1, fadeIn2, shimmerColor, fadeOut2, fadeOut1, transparent),
                    floatArrayOf(0f, 0.2f, 0.35f, 0.5f, 0.65f, 0.8f, 1f),
                    Shader.TileMode.CLAMP
                )

                val paint = Paint().apply {
                    shader = gradient
                    isAntiAlias = true
                    isDither = true
                }

                val bgPaint = Paint().apply {
                    color = backgroundColor
                    isAntiAlias = true
                }

                val shapeDrawable = object : ShapeDrawable(RectShape()) {
                    override fun onDraw(shape: Shape, canvas: Canvas, p: Paint) {
                        val cornerRadius = 20f.dpToPx()
                        val path = Path().apply {
                            addRoundRect(0f, 0f, width, height, cornerRadius, cornerRadius, Path.Direction.CW)
                        }
                        canvas.save()
                        canvas.clipPath(path)
                        canvas.drawRect(0f, 0f, width, height, bgPaint)
                        canvas.drawRect(0f, 0f, width, height, paint)
                        canvas.restore()
                    }
                }

                view.background = shapeDrawable
                view.invalidate()
            }
            start()
        }
    }

    private fun stopTaxonomyShimmer() {
        taxonomyShimmerAnimator?.cancel()
        taxonomyShimmerAnimator = null
        val container = viewCache[R.id.taxonomyContainer]
        container?.let {
            it.setBackgroundResource(R.drawable.bg_white_rounded)
            it.backgroundTintList = ContextCompat.getColorStateList(context, R.color.gray_light_f8)
        }
    }

    private fun stopConfidenceAnimation() {
        confidenceRotationAnimator?.cancel()
        confidenceRotationAnimator = null
        (viewCache[R.id.iconConfidence] as? ImageView)?.rotation = 0f
    }

    private fun displaySection(sectionId: Int, textViewId: Int, text: String, shouldScroll: Boolean = true) {
        val section = viewCache[sectionId] as? LinearLayout
        val textView = viewCache[textViewId] as? TextView

        if (text.isNotEmpty()) {
            val trimmedText = text.trim()
            textView?.let { tv ->
                renderHtml(tv, trimmedText)
            }

            section?.let { sectionView ->
                val wasAlreadyRendered = renderedSections.contains(sectionId)

                if (sectionView.visibility != View.VISIBLE) {
                    sectionView.visibility = View.VISIBLE
                    sectionView.alpha = 0f
                    sectionView.translationY = 15f

                    sectionView.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(450)
                        .setInterpolator(DecelerateInterpolator())
                        .withEndAction {
                            if (!wasAlreadyRendered && shouldScroll && !isInitialLoad) {
                                smoothScrollToView(sectionView)
                            }
                            renderedSections.add(sectionId)
                        }
                        .start()
                } else {
                    renderedSections.add(sectionId)
                }
            }
        } else {
            section?.visibility = View.GONE
            renderedSections.add(sectionId)
        }
    }

    private fun displayConservationStatus(status: String, shouldScroll: Boolean = true) {
        val section = viewCache[R.id.sectionConservation] as? LinearLayout
        val textView = viewCache[R.id.tvConservationStatus] as? TextView

        if (status.isNotEmpty()) {
            textView?.let { tv ->
                renderHtml(tv, status)
            }

            section?.let { sectionView ->
                val wasAlreadyRendered = renderedSections.contains(R.id.sectionConservation)

                if (sectionView.visibility != View.VISIBLE) {
                    sectionView.visibility = View.VISIBLE
                    sectionView.alpha = 0f

                    sectionView.animate()
                        .alpha(1f)
                        .setDuration(400)
                        .setInterpolator(DecelerateInterpolator())
                        .withEndAction {
                            if (!wasAlreadyRendered && shouldScroll && !isInitialLoad) {
                                smoothScrollToView(sectionView)
                            }
                            renderedSections.add(R.id.sectionConservation)
                        }
                        .start()
                } else {
                    renderedSections.add(R.id.sectionConservation)
                }
            }
        } else {
            section?.visibility = View.GONE
            renderedSections.add(R.id.sectionConservation)
        }
    }

    private fun findScrollView(view: View): ScrollView? {
        var parent = view.parent
        while (parent != null) {
            if (parent is ScrollView) {
                return parent
            }
            parent = parent.parent
        }
        return null
    }

    private fun smoothScrollToView(view: View) {
        view.post {
            val scrollView = findScrollView(view)
            scrollView?.let { sv ->
                val scrollY = view.top + view.height - sv.height + sv.paddingBottom + 100
                if (scrollY > sv.scrollY) {
                    sv.smoothScrollTo(0, scrollY)
                }
            }
        }
    }

    private fun hideButtons() {
        val btnShare = viewCache[R.id.btnShareInfo]
        val btnRetry = viewCache[R.id.btnRetryIdentification]
        btnShare?.visibility = View.GONE
        btnRetry?.visibility = View.GONE
    }

    private fun showShareButtonAnimation() {
        val btnShare = viewCache[R.id.btnShareInfo]
        btnShare?.apply {
            visibility = View.VISIBLE
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun showRetryButtonAnimation() {
        val btnRetry = viewCache[R.id.btnRetryIdentification]
        btnRetry?.apply {
            visibility = View.VISIBLE
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setStartDelay(100)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
        setupRetryButton()
    }

    private fun hideRetryButton() {
        val btnRetry = viewCache[R.id.btnRetryIdentification]
        btnRetry?.visibility = View.GONE
    }

    private fun showCopyButtonAnimation() {
        val btnCopy = viewCache[R.id.btnCopyScientificName]
        btnCopy?.apply {
            visibility = View.VISIBLE
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setStartDelay(100)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun setupCopyButton(info: SpeciesInfo) {
        viewCache[R.id.btnCopyScientificName]?.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val cleanName = stripHtml(info.scientificName)
            clipboard.setPrimaryClip(ClipData.newPlainText("Scientific Name", cleanName))
            onCopySuccess(cleanName)
        }
    }

    private fun setupRetryButton() {
        viewCache[R.id.btnRetryIdentification]?.setOnClickListener {
            it.visibility = View.GONE
            onRetryClick()
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
        val confidencePercent =
            String.format("%.2f", if (info.confidence > 1) info.confidence else info.confidence * 100)
        val shareText = buildString {
            append(context.getString(R.string.share_title))
            append("\n━━━━━━━━━━━━━━━━━━━━\n\n")
            append("📌 ${stripHtml(info.commonName)}\n🔬 ${stripHtml(info.scientificName)}\n")
            append("✅ ${context.getString(R.string.label_confidence_template, confidencePercent)}%\n\n")
            append("━━━━━━━━━━━━━━━━━━━━\n${context.getString(R.string.share_taxonomy_title)}\n━━━━━━━━━━━━━━━━━━━━\n\n")
            if (info.kingdom.isNotEmpty()) append("• ${context.getString(R.string.label_kingdom)} ${stripHtml(info.kingdom)}\n")
            if (info.phylum.isNotEmpty()) append("• ${context.getString(R.string.label_phylum)} ${stripHtml(info.phylum)}\n")
            if (info.className.isNotEmpty()) append("• ${context.getString(R.string.label_class)} ${stripHtml(info.className)}\n")
            if (info.taxorder.isNotEmpty()) append("• ${context.getString(R.string.label_order)} ${stripHtml(info.taxorder)}\n")
            if (info.family.isNotEmpty()) append("• ${context.getString(R.string.label_family)} ${stripHtml(info.family)}\n")
            if (info.genus.isNotEmpty()) append("• ${context.getString(R.string.label_genus)} ${stripHtml(info.genus)}\n")
            if (info.species.isNotEmpty()) append("• ${context.getString(R.string.label_species)} ${stripHtml(info.species)}\n")

            val contentList = listOf(
                info.description to R.string.share_desc_title,
                info.characteristics to R.string.share_char_title,
                info.distribution to R.string.share_dist_title,
                info.habitat to R.string.share_hab_title,
                info.conservationStatus to R.string.share_cons_title
            )
            contentList.forEach { (content, title) ->
                if (content.isNotEmpty()) {
                    append(
                        "\n━━━━━━━━━━━━━━━━━━━━\n${context.getString(title)}\n━━━━━━━━━━━━━━━━━━━━\n\n${
                            stripHtml(
                                content
                            )
                        }\n"
                    )
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
                } else {
                    type = "text/plain"
                }
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject, stripHtml(info.commonName)))
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_chooser_title)))
        } catch (e: Exception) {
            Toast.makeText(context, "${context.getString(R.string.error)}: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stripHtml(html: String): String {
        var text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            @Suppress("DEPRECATION") Html.fromHtml(html).toString()
        }
        text = text.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
        text = text.replace(Regex("\\*(.*?)\\*"), "$1")
        return text.trim()
    }

    private fun Float.dpToPx(): Float = this * context.resources.displayMetrics.density

    data class Quadruple<out A, out B, out C, out D>(
        val first: A, val second: B, val third: C, val fourth: D
    )
}