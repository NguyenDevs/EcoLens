package com.nguyendevs.ecolens.fragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.FragmentHistoryDetailModernBinding
import com.nguyendevs.ecolens.managers.SpeakerManager
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.model.SpeciesInfo
import com.nguyendevs.ecolens.utils.TextToSpeechGenerator
import java.io.File

class HistoryDetailFragment : Fragment() {

    private var _binding: FragmentHistoryDetailModernBinding? = null
    private val binding get() = _binding!!

    private lateinit var speakerManager: SpeakerManager
    private var historyEntry: HistoryEntry? = null
    private var isSpeaking = false

    private fun TextView.setHtml(html: String) {
        text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html)
        }
    }

    companion object {
        private val REGEX_BOLD = Regex("\\*\\*(.*?)\\*\\*")
        private val REGEX_ITALIC = Regex("\\*(.*?)\\*")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString("HISTORY_ENTRY_JSON")?.let { json ->
            historyEntry = Gson().fromJson(json, HistoryEntry::class.java)
        }
        speakerManager = SpeakerManager(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentHistoryDetailModernBinding.inflate(inflater, container, false)
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
        setupShareButton(info, entry.imagePath)
        binding.fabSpeak.show()
        binding.fabSpeak.bringToFront()
    }

    override fun onStop() {
        super.onStop()
        if (isSpeaking) {
            speakerManager.pause()
            isSpeaking = false
            binding.fabSpeak.setImageResource(R.drawable.ic_speak)
            binding.fabSpeak.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.green_primary))
        }
    }

    override fun onDestroy() {
        speakerManager.shutdown()
        super.onDestroy()
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.collapsingToolbar.setContentScrimColor(Color.TRANSPARENT)
        binding.collapsingToolbar.setStatusBarScrimColor(Color.TRANSPARENT)
    }

    private fun setupShareButton(info: SpeciesInfo, imagePath: String?) {
        binding.btnShareInfo.setOnClickListener {
            binding.btnShareInfo.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            var imageUri: Uri? = null
            if (!imagePath.isNullOrEmpty()) {
                val file = File(imagePath)
                if (file.exists()) {
                    try {
                        imageUri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.provider",
                            file
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        imageUri = Uri.parse(imagePath)
                    }
                }
            }
            shareSpeciesInfo(info, imageUri)
        }
    }

    private fun shareSpeciesInfo(info: SpeciesInfo, imageUri: Uri?) {
        val confidencePercent =
            String.format("%.2f", if (info.confidence > 1) info.confidence else info.confidence * 100)

        val context = requireContext()
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
            startActivity(Intent.createChooser(intent, context.getString(R.string.share_chooser_title)))
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
        text = text.replace(REGEX_BOLD, "$1")
        text = text.replace(REGEX_ITALIC, "$1")
        return text.trim()
    }

    private fun bindHeader(entry: HistoryEntry, info: SpeciesInfo) {
        Glide.with(this).load(entry.imagePath).centerCrop().into(binding.ivDetailImage)

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

    private fun bindContent(info: SpeciesInfo) {
        binding.containerSections.removeAllViews()

        addSection(binding.containerSections, getString(R.string.section_description), info.description)
        addSection(binding.containerSections, getString(R.string.section_characteristics), info.characteristics)
        addSection(binding.containerSections, getString(R.string.section_distribution), info.distribution)
        addSection(binding.containerSections, getString(R.string.section_habitat), info.habitat)
        addSection(binding.containerSections, getString(R.string.section_conservation), info.conservationStatus)
    }

    private fun addSection(container: LinearLayout, title: String, content: String) {
        if (content.isBlank()) return

        val context = container.context
        val titleColor = ContextCompat.getColor(context, R.color.text_primary)
        val contentColor = ContextCompat.getColor(context, R.color.text_secondary)
        val dividerColor = Color.parseColor("#E0E0E0")
        val topMargin = 24.dpToPx()
        val bottomMarginTitle = 10.dpToPx()
        val bottomMarginDivider = 12.dpToPx()
        val dividerHeight = 1.dpToPx()

        val titleView = TextView(context).apply {
            text = title
            textSize = 20f
            setTextColor(titleColor)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                this.topMargin = topMargin
                this.bottomMargin = bottomMarginTitle
            }
        }

        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dividerHeight
            ).apply {
                this.bottomMargin = bottomMarginDivider
            }
            setBackgroundColor(dividerColor)
        }

        val contentView = TextView(context).apply {
            textSize = 15f
            setTextColor(contentColor)
            setLineSpacing(0f, 1.4f)
            setHtml(content)
        }

        container.addView(titleView)
        container.addView(divider)
        container.addView(contentView)
    }

    private fun setupFab(info: SpeciesInfo) {
        speakerManager.onSpeechFinished = {
            activity?.runOnUiThread {
                updateFabUI(false)
            }
        }

        binding.fabSpeak.setOnClickListener {
            if (isSpeaking) {
                speakerManager.pause()
                updateFabUI(false)
            } else {
                speakerManager.speak(TextToSpeechGenerator.generateSpeechText(requireContext(), info))
                updateFabUI(true)
            }
        }
    }

    private fun updateFabUI(speaking: Boolean) {
        isSpeaking = speaking
        if (speaking) {
            binding.fabSpeak.setImageResource(R.drawable.ic_mute)
            binding.fabSpeak.backgroundTintList = ColorStateList.valueOf(Color.RED)
        } else {
            binding.fabSpeak.setImageResource(R.drawable.ic_speak)
            binding.fabSpeak.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.green_primary))
        }
    }

    private fun Int.dpToPx(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}