package com.nguyendevs.ecolens.fragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.managers.SpeakerManager
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.model.SpeciesInfo
import com.nguyendevs.ecolens.utils.TextToSpeechGenerator

class HistoryDetailFragment : Fragment() {

    private var historyEntry: HistoryEntry? = null
    private lateinit var speakerManager: SpeakerManager
    private var isSpeaking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString("HISTORY_ENTRY_JSON")?.let { json ->
            historyEntry = Gson().fromJson(json, HistoryEntry::class.java)
        }
        speakerManager = SpeakerManager(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_history_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val entry = historyEntry ?: return
        val info = entry.speciesInfo

        setupToolbar(view)
        bindHeader(view, entry, info)
        bindContent(view, info)
        setupFab(view, info)
    }

    private fun setupToolbar(view: View) {
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun bindHeader(view: View, entry: HistoryEntry, info: SpeciesInfo) {
        val ivImage = view.findViewById<ImageView>(R.id.ivDetailImage)
        val tvCommon = view.findViewById<TextView>(R.id.tvCommonName)
        val tvScientific = view.findViewById<TextView>(R.id.tvScientificName)

        Glide.with(this).load(entry.imagePath).centerCrop().into(ivImage)
        tvCommon.text = info.commonName
        tvScientific.text = info.scientificName
    }

    private fun bindContent(view: View, info: SpeciesInfo) {
        val container = view.findViewById<LinearLayout>(R.id.containerSections)
        container.removeAllViews()

        addSection(container, getString(R.string.section_description), info.description)
        addSection(container, getString(R.string.section_characteristics), info.characteristics)
        addSection(container, getString(R.string.section_distribution), info.distribution)
        addSection(container, getString(R.string.section_habitat), info.habitat)
        addSection(container, getString(R.string.section_conservation), info.conservationStatus)
    }

    private fun addSection(container: LinearLayout, title: String, content: String) {
        if (content.isBlank()) return

        val titleView = TextView(context).apply {
            text = title
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 32
                bottomMargin = 8
            }
        }

        val contentView = TextView(context).apply {
            text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY) else Html.fromHtml(content)
            textSize = 15f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setLineSpacing(0f, 1.4f)
        }

        container.addView(titleView)
        container.addView(contentView)
    }

    private fun setupFab(view: View, info: SpeciesInfo) {
        val fab = view.findViewById<FloatingActionButton>(R.id.fabAction)

        speakerManager.onSpeechFinished = {
            activity?.runOnUiThread {
                isSpeaking = false
                fab.setImageResource(R.drawable.ic_speak)
                fab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.green_primary))
            }
        }

        fab.setOnClickListener {
            if (isSpeaking) {
                speakerManager.pause()
                fab.setImageResource(R.drawable.ic_speak)
                fab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.green_primary))
                isSpeaking = false
            } else {
                speakerManager.speak(TextToSpeechGenerator.generateSpeechText(requireContext(), info))
                fab.setImageResource(R.drawable.ic_mute)
                fab.backgroundTintList = ColorStateList.valueOf(Color.RED)
                isSpeaking = true
            }
        }
    }

    override fun onDestroy() {
        speakerManager.shutdown()
        super.onDestroy()
    }
}