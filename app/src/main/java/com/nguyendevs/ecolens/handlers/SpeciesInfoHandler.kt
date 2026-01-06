package com.nguyendevs.ecolens.handlers

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.graphics.drawable.shapes.Shape
import android.net.Uri
import android.os.Build
import android.text.Html
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ActivityMainModernBinding
import com.nguyendevs.ecolens.model.LoadingStage
import com.nguyendevs.ecolens.model.SpeciesInfo
import kotlinx.coroutines.*
import java.io.File

/**
 * Xử lý hiển thị thông tin loài sinh vật
 * Quản lý animations, shimmer effects và tương tác người dùng
 */
class SpeciesInfoHandler(
    private val context: Context,
    private val binding: ActivityMainModernBinding,
    private val onCopySuccess: (String) -> Unit,
    private val onRetryClick: () -> Unit
) {
    private val handlerScope = CoroutineScope(Dispatchers.Main + Job())
    private val displayedRows = mutableSetOf<Int>()
    private val renderedSections = mutableSetOf<Int>()
    private var isInitialLoad = true
    private var allSectionsRendered = false
    private var confidenceRotationAnimator: ObjectAnimator? = null
    private var taxonomyShimmerAnimator: ValueAnimator? = null
    private var lastDisplayedCommonName: String? = null
    private var lastConfidenceValue: String? = null

    private val infoBinding get() = binding.homeContainer.speciesInfoCard

    companion object {
        private val REGEX_BOLD = Regex("\\*\\*(.*?)\\*\\*")
        private val REGEX_ITALIC = Regex("\\*(.*?)\\*")
    }

    /**
     * Thiết lập HTML cho TextView
     */
    private fun TextView.setHtml(htmlContent: String) {
        text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(htmlContent)
        }
    }

    /**
     * Hiển thị thông tin loài theo từng giai đoạn loading
     */
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

            LoadingStage.COMMON_NAME -> {}

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
                displaySection(
                    R.id.sectionCharacteristics,
                    R.id.tvCharacteristics,
                    info.characteristics,
                    shouldScroll = false
                )
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
                displaySection(R.id.sectionDescription, R.id.tvDescription, info.description, shouldScroll = false)
                displaySection(
                    R.id.sectionCharacteristics,
                    R.id.tvCharacteristics,
                    info.characteristics,
                    shouldScroll = false
                )
                displaySection(R.id.sectionDistribution, R.id.tvDistribution, info.distribution, shouldScroll = false)
                displaySection(R.id.sectionHabitat, R.id.tvHabitat, info.habitat, shouldScroll = false)
                displayConservationStatus(info.conservationStatus, shouldScroll = false)
                allSectionsRendered = true

                setupShareButton(info, imageUri)
                showShareButtonAnimation()
                setupCopyButton(info)

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

    /**
     * Xóa toàn bộ views và reset trạng thái
     */
    private fun clearAllViews() {
        lastDisplayedCommonName = null
        lastConfidenceValue = null
        displayedRows.clear()
        renderedSections.clear()
        allSectionsRendered = false
        stopConfidenceAnimation()
        stopTaxonomyShimmer()

        val viewsToHide = listOf<View>(
            infoBinding.tvCommonName, infoBinding.tvScientificName, infoBinding.confidenceCard,
            infoBinding.taxonomyContainer, infoBinding.rowKingdom, infoBinding.rowPhylum, infoBinding.rowClass,
            infoBinding.rowOrder, infoBinding.rowFamily, infoBinding.rowGenus, infoBinding.rowSpecies,
            infoBinding.sectionDescription, infoBinding.sectionCharacteristics,
            infoBinding.sectionDistribution, infoBinding.sectionHabitat, infoBinding.sectionConservation
        )
        viewsToHide.forEach { view ->
            view.visibility = View.GONE
            view.alpha = 0f
            view.translationY = 0f
        }

        val textViews = listOf(
            infoBinding.tvKingdom, infoBinding.tvPhylum, infoBinding.tvClass, infoBinding.tvOrder,
            infoBinding.tvFamily, infoBinding.tvGenus, infoBinding.tvSpecies
        )
        textViews.forEach { it.text = "" }
    }

    /**
     * Kiểm tra xem tất cả sections đã được render chưa
     */
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

    /**
     * Hiển thị tên khoa học
     */
    private fun displayScientificName(info: SpeciesInfo) {
        infoBinding.tvScientificName.apply {
            setHtml(info.scientificName)
            slideAndFadeIn(this, duration = 500, delay = 100)
        }
        slideAndFadeIn(infoBinding.btnCopyScientificName, duration = 500, delay = 150)
    }

    /**
     * Hiển thị tên thông thường
     */
    private fun displayCommonName(info: SpeciesInfo) {
        infoBinding.tvCommonName.let { view ->
            if (lastDisplayedCommonName == info.commonName &&
                view.visibility == View.VISIBLE &&
                view.alpha == 1f
            ) {
                return
            }

            if (info.commonName == "...") {
                view.text = "..."
                view.alpha = 0f
                view.setTextColor(Color.TRANSPARENT)
                lastDisplayedCommonName = "..."
            } else {
                view.setTextColor(ContextCompat.getColor(context, R.color.green_primary))
                view.setHtml(info.commonName)

                if (lastDisplayedCommonName != info.commonName) {
                    slideAndFadeIn(view, duration = 600)
                } else if (view.visibility != View.VISIBLE) {
                    view.visibility = View.VISIBLE
                    view.alpha = 1f
                }
                lastDisplayedCommonName = info.commonName
            }
        }
    }

    /**
     * Hiển thị độ tin cậy (confidence)
     */
    @SuppressLint("StringFormatInvalid")
    private fun displayConfidence(info: SpeciesInfo, isWaiting: Boolean) {
        val tvConfidence = infoBinding.tvConfidence
        val confidenceCard = infoBinding.confidenceCard
        val iconConfidence = infoBinding.iconConfidence

        if (isWaiting) {
            lastConfidenceValue = "loading"

            tvConfidence.text = context.getString(R.string.confidence, "...%")
            tvConfidence.textSize = 13f

            iconConfidence.setImageResource(R.drawable.ic_rotate)
            iconConfidence.imageTintList = ContextCompat.getColorStateList(context, R.color.text_secondary)
            confidenceCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.gray_light))
            tvConfidence.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))

            confidenceCard.let {
                if (it.visibility != View.VISIBLE) {
                    it.visibility = View.VISIBLE
                    it.alpha = 1f
                }
            }

            if (confidenceRotationAnimator == null) {
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
            val newText = context.getString(R.string.confidence_format, confidencePercent)

            if (lastConfidenceValue == newText &&
                confidenceCard.visibility == View.VISIBLE &&
                confidenceCard.alpha == 1f
            ) {
                return
            }

            tvConfidence.text = newText

            // Chọn màu sắc dựa trên độ tin cậy
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

            iconConfidence.setImageResource(icon)
            iconConfidence.imageTintList = ContextCompat.getColorStateList(context, tint)
            confidenceCard.setCardBackgroundColor(ContextCompat.getColor(context, bg))
            tvConfidence.setTextColor(ContextCompat.getColor(context, text))

            confidenceCard.let { card ->
                if (lastConfidenceValue != newText) {
                    card.visibility = View.VISIBLE
                    card.alpha = 0f
                    card.scaleX = 0.5f
                    card.scaleY = 0.5f

                    card.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(500)
                        .setInterpolator(OvershootInterpolator(1.5f))
                        .start()
                } else {
                    if (card.visibility != View.VISIBLE || card.alpha < 1f) {
                        card.visibility = View.VISIBLE
                        card.alpha = 1f
                        card.scaleX = 1f
                        card.scaleY = 1f
                    }
                }
            }

            lastConfidenceValue = newText
        }
    }

    /**
     * Chuẩn bị container phân loại học với shimmer effect
     */
    private fun prepareTaxonomyContainer() {
        val container = infoBinding.taxonomyContainer
        container.visibility = View.VISIBLE
        container.alpha = 1f

        startTaxonomyShimmer(container)

        val rows = listOf(
            infoBinding.rowKingdom, infoBinding.rowPhylum, infoBinding.rowClass,
            infoBinding.rowOrder, infoBinding.rowFamily, infoBinding.rowGenus, infoBinding.rowSpecies
        )
        rows.forEach { row ->
            row.apply {
                visibility = View.INVISIBLE
                alpha = 0f
                translationY = 0f
            }
        }
    }

    /**
     * Hiển thị phân loại học theo hiệu ứng waterfall
     */
    private fun displayTaxonomyWaterfall(info: SpeciesInfo) {
        val container = infoBinding.taxonomyContainer
        container.visibility = View.VISIBLE
        container.alpha = 1f

        val rows = listOf(
            Triple(infoBinding.rowKingdom, infoBinding.tvKingdom, info.kingdom),
            Triple(infoBinding.rowPhylum, infoBinding.tvPhylum, info.phylum),
            Triple(infoBinding.rowClass, infoBinding.tvClass, info.className),
            Triple(infoBinding.rowOrder, infoBinding.tvOrder, info.taxorder),
            Triple(infoBinding.rowFamily, infoBinding.tvFamily, info.family),
            Triple(infoBinding.rowGenus, infoBinding.tvGenus, info.genus),
            Triple(infoBinding.rowSpecies, infoBinding.tvSpecies, info.species)
        )

        rows.forEach { (rowView, textView, text) ->
            val hasData = text.isNotEmpty() && text != "..." && text != "N/A"
            val rowId = rowView.id

            if (hasData) {
                if (!displayedRows.contains(rowId)) {
                    textView.setHtml(text)

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

    /**
     * Bắt đầu hiệu ứng shimmer cho taxonomy
     */
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

                val isDarkMode =
                    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

                // Màu sắc cho shimmer effect
                val backgroundColor: Int
                val transparent: Int
                val fadeIn1: Int
                val fadeIn2: Int
                val shimmerColor: Int
                val fadeOut2: Int
                val fadeOut1: Int

                if (isDarkMode) {
                    backgroundColor = Color.parseColor("#2C2C2C")
                    transparent = Color.parseColor("#002C2C2C")
                    fadeIn1 = Color.parseColor("#20454545")
                    fadeIn2 = Color.parseColor("#60454545")
                    shimmerColor = Color.parseColor("#FF454545")
                    fadeOut2 = Color.parseColor("#60454545")
                    fadeOut1 = Color.parseColor("#20454545")
                } else {
                    backgroundColor = Color.parseColor("#ECEFF1")
                    transparent = Color.parseColor("#00ECEFF1")
                    fadeIn1 = Color.parseColor("#40F5F7F9")
                    fadeIn2 = Color.parseColor("#80F8F9FB")
                    shimmerColor = Color.parseColor("#FFFAFBFC")
                    fadeOut2 = Color.parseColor("#80F8F9FB")
                    fadeOut1 = Color.parseColor("#40F5F7F9")
                }

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

    /**
     * Dừng hiệu ứng shimmer
     */
    private fun stopTaxonomyShimmer() {
        taxonomyShimmerAnimator?.cancel()
        taxonomyShimmerAnimator = null
        infoBinding.taxonomyContainer.let {
            it.setBackgroundResource(R.drawable.bg_white_rounded)
            it.backgroundTintList = ContextCompat.getColorStateList(context, R.color.surface_variant)
        }
    }

    /**
     * Dừng animation xoay của confidence
     */
    private fun stopConfidenceAnimation() {
        confidenceRotationAnimator?.cancel()
        confidenceRotationAnimator = null
        infoBinding.iconConfidence.rotation = 0f
    }

    /**
     * Hiển thị một section thông tin
     */
    private fun displaySection(sectionId: Int, textViewId: Int, text: String, shouldScroll: Boolean = true) {
        val section = when (sectionId) {
            R.id.sectionDescription -> infoBinding.sectionDescription
            R.id.sectionCharacteristics -> infoBinding.sectionCharacteristics
            R.id.sectionDistribution -> infoBinding.sectionDistribution
            R.id.sectionHabitat -> infoBinding.sectionHabitat
            R.id.sectionConservation -> infoBinding.sectionConservation
            else -> null
        }
        val textView = when (textViewId) {
            R.id.tvDescription -> infoBinding.tvDescription
            R.id.tvCharacteristics -> infoBinding.tvCharacteristics
            R.id.tvDistribution -> infoBinding.tvDistribution
            R.id.tvHabitat -> infoBinding.tvHabitat
            R.id.tvConservationStatus -> infoBinding.tvConservationStatus
            else -> null
        }

        if (text.isNotEmpty()) {
            val trimmedText = text.trim()
            textView?.let { tv ->
                tv.setHtml(trimmedText)
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

    /**
     * Hiển thị trạng thái bảo tồn
     */
    private fun displayConservationStatus(status: String, shouldScroll: Boolean = true) {
        val section = infoBinding.sectionConservation
        val textView = infoBinding.tvConservationStatus

        if (status.isNotEmpty()) {
            textView.let { tv ->
                tv.setHtml(status)
            }

            section.let { sectionView ->
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
            section.visibility = View.GONE
            renderedSections.add(R.id.sectionConservation)
        }
    }

    /**
     * Tìm ScrollView cha của view
     */
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

    /**
     * Cuộn mượt đến view
     */
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
        infoBinding.btnShareInfo.visibility = View.GONE
        infoBinding.btnRetryIdentification.visibility = View.GONE
    }

    private fun showShareButtonAnimation() {
        handlerScope.launch {
            withContext(Dispatchers.Main) {
                infoBinding.btnShareInfo.apply {
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
        }
    }

    private fun showRetryButtonAnimation() {
        handlerScope.launch {
            withContext(Dispatchers.Main) {
                infoBinding.btnRetryIdentification.apply {
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
        }
    }

    private fun hideRetryButton() {
        infoBinding.btnRetryIdentification.visibility = View.GONE
    }

    private fun showCopyButtonAnimation() {
        infoBinding.btnCopyScientificName.apply {
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

    /**
     * Thiết lập nút copy tên khoa học
     */
    private fun setupCopyButton(info: SpeciesInfo) {
        infoBinding.btnCopyScientificName.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val cleanName = stripHtml(info.scientificName)
            clipboard.setPrimaryClip(ClipData.newPlainText("Scientific Name", cleanName))
            onCopySuccess(cleanName)
        }
    }

    /**
     * Thiết lập nút thử lại
     */
    private fun setupRetryButton() {
        infoBinding.btnRetryIdentification.setOnClickListener {
            it.visibility = View.GONE
            onRetryClick()
        }
    }

    /**
     * Thiết lập nút chia sẻ
     */
    private fun setupShareButton(info: SpeciesInfo, imageUri: Uri?) {
        infoBinding.btnShareInfo.setOnClickListener {
            infoBinding.btnShareInfo.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

            handlerScope.launch(Dispatchers.IO) {
                var shareableUri = imageUri

                if (shareableUri != null) {
                    if (shareableUri.scheme == "file" || shareableUri.path?.startsWith("/") == true) {
                        try {
                            val path = shareableUri.path
                            if (path != null) {
                                val file = File(path)
                                if (file.exists()) {
                                    shareableUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    shareSpeciesInfo(info, shareableUri)
                }
            }
        }
    }

    private fun fadeIn(view: View, durationMs: Long) {
        view.animate()
            .alpha(1f)
            .setDuration(durationMs)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    /**
     * Chia sẻ thông tin loài
     */
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
                            stripHtml(content)
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

    /**
     * Loại bỏ HTML tags
     */
    private fun stripHtml(html: String): String {
        var text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            @Suppress("DEPRECATION") Html.fromHtml(html).toString()
        }
        text = text.replace(REGEX_BOLD, "$1")
        text = text.replace(REGEX_ITALIC, "$1")
        return text.trim()
    }

    private fun Float.dpToPx(): Float = this * context.resources.displayMetrics.density

    data class Quadruple<out A, out B, out C, out D>(
        val first: A, val second: B, val third: C, val fourth: D
    )

    /**
     * Animation trượt và fade in cho view
     */
    private fun slideAndFadeIn(view: View, duration: Long = 500, delay: Long = 0) {
        if (view.visibility == View.VISIBLE && view.alpha == 1f) return

        view.alpha = 0f
        view.translationY = 50f
        view.visibility = View.VISIBLE

        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(duration)
            .setStartDelay(delay)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()
    }
}