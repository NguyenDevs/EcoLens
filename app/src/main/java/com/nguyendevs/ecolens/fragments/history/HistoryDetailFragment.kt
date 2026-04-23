package com.nguyendevs.ecolens.fragments.history

import android.content.ClipData
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.FragmentSpeciesHistoryDetailBinding
import com.nguyendevs.ecolens.handlers.animations.HistoryDetailAnimationHandler
import com.nguyendevs.ecolens.managers.gemini.GeminiStreamingHelper
import com.nguyendevs.ecolens.managers.main.SpeakerManager
import com.nguyendevs.ecolens.managers.setting.LanguageManager
import com.nguyendevs.ecolens.models.SpeciesInfo
import com.nguyendevs.ecolens.models.history.HistoryEntry
import com.nguyendevs.ecolens.network.RetrofitClient
import com.nguyendevs.ecolens.utils.CustomDialogUtils
import com.nguyendevs.ecolens.utils.ExportUtils
import com.nguyendevs.ecolens.utils.TextToSpeechGenerator
import com.nguyendevs.ecolens.view.EcoLensViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Fragment hiển thị chi tiết lịch sử nhận diện loài với hỗ trợ dịch thuật, TTS và chia sẻ. */
class HistoryDetailFragment : Fragment() {

    private var _binding: FragmentSpeciesHistoryDetailBinding? = null
    private val binding
        get() = _binding!!

    private val viewModel: EcoLensViewModel by activityViewModels()
    private lateinit var speakerManager: SpeakerManager
    private lateinit var languageManager: LanguageManager

    private lateinit var animationHandler: HistoryDetailAnimationHandler

    private var historyEntry: HistoryEntry? = null
    private var isSpeaking = false
    private var cachedTranslatedInfo: SpeciesInfo? = null
    private var isTranslated = false
    private var isTranslating = false
    private var translatedLanguage: String? = null

    companion object {
        private val REGEX_BOLD = Regex("\\*\\*(.+?)\\*\\*")
        private val REGEX_ITALIC = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")
        private val REGEX_CODE = Regex("`(.+?)`")
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString("HISTORY_ENTRY_JSON")?.let { json ->
            historyEntry = Gson().fromJson(json, HistoryEntry::class.java)
        }
        speakerManager = SpeakerManager(requireContext())
        animationHandler = HistoryDetailAnimationHandler(requireContext())
        languageManager = LanguageManager(requireContext())
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpeciesHistoryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (speakerManager.isSpeaking()) {
            speakerManager.pause()
        }

        val entry = historyEntry ?: return
        val info = entry.speciesInfo

        setupBackButton()
        bindHeader(entry, info)
        bindTaxonomy(info)
        bindContent(info)
        setupFab(info)
        setupShareButton(info, entry.imagePath, entry.localImagePath)
        setupFavoriteButton(entry)
        setupMoreOptionsButton()
        setupTranslateButton(entry)

        animationHandler.showFab(binding.fabSpeak)
        binding.fabSpeak.bringToFront()
    }

    /** Thiết lập nút yêu thích và cập nhật trạng thái khi click. */
    private fun setupFavoriteButton(entry: HistoryEntry) {
        updateFavoriteUI(entry.isFavorite)
        binding.btnFavorite.setOnClickListener {
            animationHandler.performConfirmFeedback(it)
            val currentEntry = historyEntry ?: return@setOnClickListener
            val nextState = !currentEntry.isFavorite
            viewModel.toggleFavorite(currentEntry)
            historyEntry = currentEntry.copy(isFavorite = nextState)
            updateFavoriteUI(nextState)
        }
    }

    /** Cập nhật giao diện nút yêu thích theo trạng thái. */
    private fun updateFavoriteUI(isFavorite: Boolean) {
        if (isFavorite) {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite)
            binding.btnFavorite.imageTintList =
                    ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.primary)
                    )
            binding.btnFavorite.backgroundTintList =
                    ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.surface_tint)
                    )
        } else {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite)
            binding.btnFavorite.imageTintList =
                    ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.text_secondary)
                    )
            binding.btnFavorite.backgroundTintList =
                    ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.border_light)
                    )
        }
    }

    override fun onStop() {
        super.onStop()
        if (isSpeaking) {
            speakerManager.pause()
            updateFabUI(false)
        }
    }

    override fun onDestroy() {
        speakerManager.shutdown()
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



    /** Thiết lập nút back và transparent scrim cho collapsing toolbar. */
    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            animationHandler.performConfirmFeedback(it)
            parentFragmentManager.popBackStack()
        }

        binding.collapsingToolbar.setContentScrimColor(Color.TRANSPARENT)
        binding.collapsingToolbar.setStatusBarScrimColor(Color.TRANSPARENT)
    }

    /** Thiết lập nút more options để mở history menu. */
    private fun setupMoreOptionsButton() {
        binding.btnMoreOptions.setOnClickListener {
            animationHandler.performConfirmFeedback(it)
            showHistoryMenu(it)
        }
    }

    /** Hiển thị popup menu với tùy chọn xóa, tải ảnh, xuất file. */
    private fun showHistoryMenu(anchor: View) {
        val widthPx = (280 * resources.displayMetrics.density).toInt()
        val popupView = LayoutInflater.from(requireContext())
            .inflate(R.layout.popup_history_menu, null)

        val popup = PopupWindow(popupView, widthPx, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            exitTransition = android.transition.Fade(android.transition.Fade.OUT).apply {
                duration = 250
                interpolator = AccelerateDecelerateInterpolator()
            }
        }

        val itemOptions = listOf(
            popupView.findViewById<ViewGroup>(R.id.itemDelete) to { showDeleteConfirmDialog() },
            popupView.findViewById<ViewGroup>(R.id.itemDownload) to {
                historyEntry?.let { entry ->
                    if (entry.localImagePath.isNotEmpty() && File(entry.localImagePath).exists()) {
                        showDownloadConfirmation(entry.localImagePath)
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.error_image_not_found), Toast.LENGTH_SHORT).show()
                    }
                }
            },
            popupView.findViewById<ViewGroup>(R.id.itemExport) to { showExportOptions() }
        )

        for (pair in itemOptions) {
            val view = pair.first
            val action = pair.second
            view?.alpha = 0f
            view?.translationY = -12f
            view?.setOnClickListener {
                popup.dismiss()
                action()
            }
        }

        val anchorLoc = IntArray(2)
        anchor.getLocationOnScreen(anchorLoc)
        val xOffset = anchorLoc[0] + anchor.width - widthPx
        val yOffset = anchorLoc[1] + anchor.height + (6 * resources.displayMetrics.density).toInt()

        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, xOffset, yOffset)

        // Animation mở popup
        popupView.alpha = 0f
        popupView.scaleX = 0.90f
        popupView.scaleY = 0.90f
        popupView.pivotX = widthPx.toFloat()
        popupView.pivotY = 0f
        popupView.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(320)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Animation cho từng item
        for (index in itemOptions.indices) {
            val pair = itemOptions[index]
            val view = pair.first
            view?.animate()
                ?.alpha(1f)?.translationY(0f)
                ?.setStartDelay(80L + index * 70L)
                ?.setDuration(300)
                ?.setInterpolator(AccelerateDecelerateInterpolator())
                ?.start()
        }
    }

    /** Hiển thị dialog xác nhận lưu ảnh vào thư viện. */
    private fun showDownloadConfirmation(imagePath: String) {
        CustomDialogUtils.showConfirmationDialog(
                context = requireContext(),
                title = getString(R.string.dialog_save_picture_title),
                message = getString(R.string.dialog_save_picture_message),
                confirmText = getString(R.string.action_save),
                onConfirm = {
                    if (ExportUtils.saveImageToGallery(requireContext(), imagePath)) {
                        Toast.makeText(
                                        requireContext(),
                                        getString(R.string.save_success),
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                    } else {
                        Toast.makeText(
                                        requireContext(),
                                        getString(R.string.error_save_failed),
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                    }
                }
        )
    }

    /** Hiển thị bottom sheet chọn định dạng xuất file. */
    private fun showExportOptions() {
        val exportSheet = ExportHistoryBottomSheet.newInstance()
        exportSheet.onExportConfirmed = { format, includeImage ->
            historyEntry?.let { entry ->
                val result =
                        ExportUtils.exportHistory(requireContext(), entry, format, includeImage)
                if (result != null) {
                    Toast.makeText(
                                    requireContext(),
                                    getString(R.string.export_success, result),
                                    Toast.LENGTH_LONG
                            )
                            .show()
                } else {
                    Toast.makeText(
                                    requireContext(),
                                    getString(R.string.error_export_failed),
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                }
            }
        }
        exportSheet.show(childFragmentManager, ExportHistoryBottomSheet.TAG)
    }

    /** Hiển thị dialog xác nhận xóa lịch sử. */
    private fun showDeleteConfirmDialog() {
        CustomDialogUtils.showConfirmationDialog(
                context = requireContext(),
                title = getString(R.string.dialog_delete_history_title),
                message = getString(R.string.dialog_delete_history_message),
                confirmText = getString(R.string.action_delete),
                onConfirm = {
                    historyEntry?.let { entry ->
                        viewModel.deleteHistory(entry)
                        Toast.makeText(
                                        requireContext(),
                                        getString(R.string.delete_success),
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                        parentFragmentManager.popBackStack()
                    }
                }
        )
    }

    /** Thiết lập nút share, tải ảnh local hoặc remote để đính kèm. */
    private fun setupShareButton(info: SpeciesInfo, remoteUrl: String?, localImagePath: String?) {
        binding.btnShareInfo.setOnClickListener {
            if (isTranslating) return@setOnClickListener
            animationHandler.performConfirmFeedback(it)

            lifecycleScope.launch(Dispatchers.IO) {
                var imageUri: Uri? = null

                if (!localImagePath.isNullOrEmpty()) {
                    val file = File(localImagePath)
                    if (file.exists()) {
                        try {
                            imageUri =
                                    FileProvider.getUriForFile(
                                            requireContext(),
                                            "${requireContext().packageName}.provider",
                                            file
                                    )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                if (imageUri == null && !remoteUrl.isNullOrEmpty()) {
                    try {
                        val remoteUri = Uri.parse(remoteUrl)
                        if (remoteUri.scheme == "http" || remoteUri.scheme == "https") {
                            val file =
                                    com.nguyendevs.ecolens.utils.ImageUtils.uriToFile(
                                            requireContext(),
                                            remoteUri,
                                            1024
                                    )
                            imageUri =
                                    FileProvider.getUriForFile(
                                            requireContext(),
                                            "${requireContext().packageName}.provider",
                                            file
                                    )
                        } else {
                            imageUri = remoteUri
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                withContext(Dispatchers.Main) { shareSpeciesInfo(info, imageUri) }
            }
        }
    }

    /** Chia sẻ thông tin loài qua Intent, có thể kèm ảnh. */
    private fun shareSpeciesInfo(info: SpeciesInfo, imageUri: Uri?) {
        val confidencePercent =
                String.format(
                        "%.2f",
                        if (info.confidence > 1) info.confidence else info.confidence * 100
                )

        val context = requireContext()
        val shareText = buildString {
            append(context.getString(R.string.share_title))
            append("\n━━━━━━━━━━━━━━━━━━━━\n\n")
            append("📌 ${stripHtml(info.commonName)}\n🔬 ${stripHtml(info.scientificName)}\n")
            append(
                    "✅ ${context.getString(R.string.label_confidence_template, confidencePercent)}%\n\n"
            )
            append(
                    "━━━━━━━━━━━━━━━━━━━━\n${context.getString(R.string.share_taxonomy_title)}\n━━━━━━━━━━━━━━━━━━━━\n\n"
            )

            if (info.kingdom.isNotEmpty())
                    append(
                            "• ${context.getString(R.string.label_kingdom)} ${stripHtml(info.kingdom)}\n"
                    )
            if (info.phylum.isNotEmpty())
                    append(
                            "• ${context.getString(R.string.label_phylum)} ${stripHtml(info.phylum)}\n"
                    )
            if (info.className.isNotEmpty())
                    append(
                            "• ${context.getString(R.string.label_class)} ${stripHtml(info.className)}\n"
                    )
            if (info.taxorder.isNotEmpty())
                    append(
                            "• ${context.getString(R.string.label_order)} ${stripHtml(info.taxorder)}\n"
                    )
            if (info.family.isNotEmpty())
                    append(
                            "• ${context.getString(R.string.label_family)} ${stripHtml(info.family)}\n"
                    )
            if (info.genus.isNotEmpty())
                    append(
                            "• ${context.getString(R.string.label_genus)} ${stripHtml(info.genus)}\n"
                    )
            if (info.species.isNotEmpty())
                    append(
                            "• ${context.getString(R.string.label_species)} ${stripHtml(info.species)}\n"
                    )

            val contentList =
                    listOf(
                            info.description to R.string.share_desc_title,
                            info.characteristics to R.string.share_char_title,
                            info.distribution to R.string.share_dist_title,
                            info.habitat to R.string.share_hab_title,
                            info.conservationStatus to R.string.share_cons_title
                    )
            contentList.forEach { (content, title) ->
                if (content.isNotEmpty()) {
                    append(
                            "\n━━━━━━━━━━━━━━━━━━━━\n${context.getString(title)}\n━━━━━━━━━━━━━━━━━━━━\n\n${stripHtml(content)}\n"
                    )
                }
            }
            append("\n━━━━━━━━━━━━━━━━━━━━\n${context.getString(R.string.share_footer)}")
        }

        try {
            val intent =
                    Intent(Intent.ACTION_SEND).apply {
                        if (imageUri != null) {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, imageUri)
                            clipData = ClipData.newRawUri(null, imageUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } else {
                            type = "text/plain"
                        }
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        putExtra(
                                Intent.EXTRA_SUBJECT,
                                context.getString(
                                        R.string.share_subject,
                                        stripHtml(info.commonName)
                                )
                        )
                    }
            startActivity(
                    Intent.createChooser(intent, context.getString(R.string.share_chooser_title))
            )
        } catch (e: Exception) {
            Toast.makeText(
                            context,
                            "${context.getString(R.string.error)}: ${e.message}",
                            Toast.LENGTH_SHORT
                    )
                    .show()
        }
    }

    /** Thiết lập nút dịch, hiển thị khi ngôn ngữ entry khác ngôn ngữ hiện tại. */
    private fun setupTranslateButton(entry: HistoryEntry) {
        val currentLang = languageManager.getLanguage()
        if (entry.language != currentLang) {
            binding.btnTranslate.visibility = View.VISIBLE
            binding.btnTranslate.setOnClickListener {
                if (isTranslating) return@setOnClickListener
                animationHandler.performConfirmFeedback(it)
                handleTranslateClick(entry, currentLang)
            }
        } else {
            binding.btnTranslate.visibility = View.GONE
        }
    }

    /** Xử lý click dịch: toggle về bản gốc hoặc dùng cache/dịch mới. */
    private fun handleTranslateClick(entry: HistoryEntry, targetLang: String) {
        if (isTranslated) {
            isTranslated = false
            updateUI(entry.speciesInfo)
            binding.btnTranslate.setImageResource(R.drawable.ic_translate)
        } else {
            val cached = viewModel.getCachedTranslation(entry.id, targetLang)
            if (cached != null) {
                cachedTranslatedInfo = cached
                isTranslated = true
                translatedLanguage = targetLang
                updateUI(cached)
            } else if (cachedTranslatedInfo != null && translatedLanguage == targetLang) {
                isTranslated = true
                updateUI(cachedTranslatedInfo!!)
            } else {
                performTranslation(entry, targetLang)
            }
        }
    }

    /** Thực hiện dịch thông tin loài qua Gemini API và cập nhật UI. */
    private fun performTranslation(entry: HistoryEntry, targetLang: String) {
        isTranslating = true
        setButtonsEnabled(false)
        binding.btnTranslate.visibility = View.INVISIBLE
        binding.loadingTranslate.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val streamingHelper = GeminiStreamingHelper(RetrofitClient.iNaturalistApi, Gson())
                val originalInfo = entry.speciesInfo

                val commonName =
                        streamingHelper.getCommonName(originalInfo.scientificName, targetLang)
                                ?: originalInfo.commonName

                var translatedInfo =
                        originalInfo.copy(
                                commonName = commonName,
                                kingdom = originalInfo.kingdom,
                                phylum = originalInfo.phylum,
                                className = originalInfo.className,
                                taxorder = originalInfo.taxorder,
                                family = originalInfo.family,
                                genus = originalInfo.genus,
                                species = originalInfo.species
                        )

                streamingHelper.streamDetails(
                        originalInfo.scientificName,
                        originalInfo.confidence,
                        targetLang,
                        translatedInfo
                ) { state -> state.speciesInfo?.let { info -> translatedInfo = info } }

                val iucnCode = "NE"
                streamingHelper.streamConservation(
                        originalInfo.scientificName,
                        iucnCode,
                        targetLang,
                        translatedInfo
                ) { state -> state.speciesInfo?.let { info -> translatedInfo = info } }

                cachedTranslatedInfo = translatedInfo
                isTranslated = true
                translatedLanguage = targetLang

                viewModel.saveTranslationToCache(entry.id, targetLang, translatedInfo)

                withContext(Dispatchers.Main) {
                    updateUI(translatedInfo)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                                    requireContext(),
                                    getString(R.string.error_general, e.message),
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isTranslating = false
                    setButtonsEnabled(true)
                    binding.btnTranslate.visibility = View.VISIBLE
                    binding.loadingTranslate.visibility = View.GONE
                }
            }
        }
    }

    /** Bật/tắt tất cả các nút tương tác khi đang dịch. */
    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnBack.isEnabled = enabled
        binding.btnMoreOptions.isEnabled = enabled
        binding.btnShareInfo.isEnabled = enabled
        binding.fabSpeak.isEnabled = enabled
        binding.btnTranslate.isEnabled = enabled
    }

    /** Cập nhật toàn bộ UI với thông tin loài mới. */
    private fun updateUI(info: SpeciesInfo) {
        bindHeader(historyEntry!!, info)
        bindTaxonomy(info)
        bindContent(info)
    }

    /** Hiển thị ảnh, tên phổ thông, khoa học và các tag kingdom/family/species. */
    private fun bindHeader(entry: HistoryEntry, info: SpeciesInfo) {
        animationHandler.loadImageWithFadeIn(
                imageView = binding.ivDetailImage,
                localPath = entry.localImagePath,
                remoteUrl = entry.imagePath,
                centerCrop = true
        )

        binding.tvCommonName.setHtml(info.commonName)
        binding.tvScientificName.setHtml(info.scientificName)

        if (info.kingdom.isNotEmpty()) {
            binding.tagKingdom.setHtml(info.kingdom)
            binding.tagKingdom.visibility = View.VISIBLE
        } else {
            binding.tagKingdom.visibility = View.GONE
        }

        if (info.family.isNotEmpty()) {
            binding.tagFamily.setHtml(info.family)
            binding.tagFamily.visibility = View.VISIBLE
        } else {
            binding.tagFamily.visibility = View.GONE
        }

        if (info.species.isNotEmpty()) {
            binding.tagSpecies.setHtml(info.species)
            binding.tagSpecies.visibility = View.VISIBLE
        } else {
            binding.tagSpecies.visibility = View.GONE
        }
    }

    /** Hiển thị thông tin phân loại học vào các TextView tương ứng. */
    private fun bindTaxonomy(info: SpeciesInfo) {
        fun TextView.bindValue(value: String) {
            if (value.isNotEmpty()) {
                setHtml(value)
            } else {
                text = "N/A"
            }
        }

        binding.layoutTaxonomy.tvKingdom.bindValue(info.kingdom)
        binding.layoutTaxonomy.tvPhylum.bindValue(info.phylum)
        binding.layoutTaxonomy.tvClass.bindValue(info.className)
        binding.layoutTaxonomy.tvOrder.bindValue(info.taxorder)
        binding.layoutTaxonomy.tvFamily.bindValue(info.family)
        binding.layoutTaxonomy.tvGenus.bindValue(info.genus)
        binding.layoutTaxonomy.tvSpecies.bindValue(info.species)
    }

    /** Tạo và hiển thị các section nội dung (mô tả, đặc điểm, phân bố, sinh cảnh, bảo tồn). */
    private fun bindContent(info: SpeciesInfo) {
        binding.containerSections.removeAllViews()

        addSection(
                binding.containerSections,
                getString(R.string.section_description),
                info.description,
                iconRes = R.drawable.ic_desc_section
        )
        addSection(
                binding.containerSections,
                getString(R.string.section_characteristics),
                info.characteristics,
                iconRes = R.drawable.ic_character_section
        )
        addSection(
                binding.containerSections,
                getString(R.string.section_distribution),
                info.distribution,
                iconRes = R.drawable.ic_distribution_section
        )
        addSection(
                binding.containerSections,
                getString(R.string.section_habitat),
                info.habitat,
                iconRes = R.drawable.ic_habitat_section
        )

        if (!info.iucn && !info.vnredlist) {
            addSection(
                    binding.containerSections,
                    getString(R.string.section_conservation),
                    getString(R.string.conservation_disabled_message),
                    isCenter = true,
                    iconRes = R.drawable.ic_conservation_section
            )
        } else {
            val iucnContent = if (!info.iucn) getString(R.string.iucn_disabled_message) else info.conservationStatus
            val vnRedListContent = if (!info.vnredlist) getString(R.string.vnredlist_disabled_message) else info.vnredlistStatus
            
            val finalContent = buildString {
                append(iucnContent)
                append("\n\n---\n\n")
                append(vnRedListContent)
            }
            addSection(
                    binding.containerSections,
                    getString(R.string.section_conservation),
                    finalContent,
                    iconRes = R.drawable.ic_conservation_section
            )
        }
    }

    /** Thêm một section nội dung vào container với tiêu đề, divider và text. */
    private fun addSection(
            container: LinearLayout,
            title: String,
            content: String,
            isCenter: Boolean = false,
            iconRes: Int = 0
    ) {
        if (content.isBlank()) return

        val context = container.context
        val titleColor = ContextCompat.getColor(context, R.color.text_primary)
        val contentColor = ContextCompat.getColor(context, R.color.text_secondary)
        val dividerColor = ContextCompat.getColor(context, R.color.border_light)
        val topMargin = 24.dpToPx()
        val bottomMarginTitle = 10.dpToPx()
        val bottomMarginDivider = 12.dpToPx()
        val dividerHeight = 1.dpToPx()

        val headerLayout =
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    layoutParams =
                            LinearLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT
                                    )
                                    .apply {
                                        this.topMargin = topMargin
                                        this.bottomMargin = bottomMarginTitle
                                    }
                }

        if (iconRes != 0) {
            val iconView =
                    android.widget.ImageView(context).apply {
                        setImageResource(iconRes)
                        layoutParams =
                                LinearLayout.LayoutParams(28.dpToPx(), 28.dpToPx()).apply {
                                    marginEnd = 8.dpToPx()
                                }
                    }
            headerLayout.addView(iconView)
        }

        val titleView =
                TextView(context).apply {
                    text = title
                    textSize = 20f
                    setTextColor(titleColor)
                    setTypeface(null, Typeface.BOLD)
                    layoutParams =
                            LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                }
        headerLayout.addView(titleView)

        val divider =
                View(context).apply {
                    layoutParams =
                            LinearLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            dividerHeight
                                    )
                                    .apply { this.bottomMargin = bottomMarginDivider }
                    setBackgroundColor(dividerColor)
                }

        val contentView =
                TextView(context).apply {
                    textSize = 15f
                    setTextColor(contentColor)
                    setLineSpacing(0f, 1.4f)
                    if (isCenter) {
                        gravity = Gravity.CENTER
                        text = content
                    } else {
                        setHtml(content)
                    }
                }

        container.addView(headerLayout)
        container.addView(divider)
        container.addView(contentView)
    }

    /** Thiết lập FAB text-to-speech, toggle đọc/dừng khi click. */
    private fun setupFab(info: SpeciesInfo) {
        speakerManager.onSpeechFinished = { activity?.runOnUiThread { updateFabUI(false) } }

        binding.fabSpeak.setOnClickListener {
            if (isTranslating) return@setOnClickListener
            animationHandler.performConfirmFeedback(it)

            lifecycleScope.launch(Dispatchers.IO) {
                if (isSpeaking) {
                    speakerManager.pause()
                    withContext(Dispatchers.Main) { updateFabUI(false) }
                } else {
                    val infoToSpeak =
                            if (isTranslated && cachedTranslatedInfo != null) cachedTranslatedInfo!!
                            else info
                    val langToSpeak =
                            if (isTranslated && translatedLanguage != null) translatedLanguage!!
                            else historyEntry?.language ?: "vi"

                    speakerManager.setLanguage(langToSpeak)

                    val speechText =
                            TextToSpeechGenerator.generateSpeechText(requireContext(), infoToSpeak)
                    withContext(Dispatchers.Main) {
                        speakerManager.speak(speechText)
                        updateFabUI(true)
                    }
                }
            }
        }
    }

    /** Cập nhật giao diện FAB theo trạng thái đang đọc/dừng. */
    private fun updateFabUI(speaking: Boolean) {
        isSpeaking = speaking
        animationHandler.animateFabState(binding.fabSpeak, speaking)
    }

    /** Extension function để set text từ HTML vào TextView. */
    private fun TextView.setHtml(html: String) {
        text =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
                } else {
                    @Suppress("DEPRECATION") Html.fromHtml(html)
                }
    }

    /** Xóa HTML tags và markdown formatting khỏi chuỗi text. */
    private fun stripHtml(html: String): String {
        var text =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
                } else {
                    @Suppress("DEPRECATION") Html.fromHtml(html).toString()
                }

        text = text.replace(REGEX_BOLD, "$1")
        text = text.replace(REGEX_ITALIC, "$1")
        text = text.replace(REGEX_CODE, "$1")

        return text.trim()
    }

    /** Chuyển đổi dp sang px. */
    private fun Int.dpToPx(): Int {
        return TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        this.toFloat(),
                        resources.displayMetrics
                )
                .toInt()
    }
}
