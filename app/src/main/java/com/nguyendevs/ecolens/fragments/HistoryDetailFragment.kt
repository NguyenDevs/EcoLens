package com.nguyendevs.ecolens.fragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.text.LineBreaker
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.managers.SpeakerManager
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.model.SpeciesInfo
import com.nguyendevs.ecolens.utils.TextToSpeechGenerator

class HistoryDetailFragment : Fragment() {

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
        if (speakerManager.isSpeaking()) {
            speakerManager.pause()
        }

        val entry = historyEntry ?: return
        val info = entry.speciesInfo

        setupBackButton(view)
        bindHeader(view, entry, info)
        bindTaxonomy(view, info)
        bindContent(view, info)
        setupFab(view, info)

        view.findViewById<FloatingActionButton>(R.id.fab_speak)?.let { fab ->
            fab.show()
            fab.bringToFront()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isSpeaking) {
            speakerManager.pause()
            isSpeaking = false
            view?.findViewById<FloatingActionButton>(R.id.fab_speak)?.let { fab ->
                fab.setImageResource(R.drawable.ic_speak)
                fab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.green_primary))
            }
        }
    }

    override fun onDestroy() {
        speakerManager.shutdown()
        super.onDestroy()
    }

    private fun setupBackButton(view: View) {
        val btnBack = view.findViewById<FloatingActionButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val collapsingToolbar = view.findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbar)
        collapsingToolbar.setContentScrimColor(Color.TRANSPARENT)
        collapsingToolbar.setStatusBarScrimColor(Color.TRANSPARENT)
    }

    private fun bindHeader(view: View, entry: HistoryEntry, info: SpeciesInfo) {
        val ivImage = view.findViewById<ImageView>(R.id.ivDetailImage)
        val tvCommon = view.findViewById<TextView>(R.id.tvCommonName)
        val tvScientific = view.findViewById<TextView>(R.id.tvScientificName)
        val tagKingdom = view.findViewById<TextView>(R.id.tagKingdom)
        val tagFamily = view.findViewById<TextView>(R.id.tagFamily)

        Glide.with(this).load(entry.imagePath).centerCrop().into(ivImage)

        tvCommon.setHtml(info.commonName)
        tvScientific.setHtml(info.scientificName)

        if (info.kingdom.isNotEmpty()) {
            tagKingdom.setHtml(info.kingdom)
            tagKingdom.visibility = View.VISIBLE
        } else {
            tagKingdom.visibility = View.GONE
        }

        if (info.family.isNotEmpty()) {
            tagFamily.setHtml(info.family)
            tagFamily.visibility = View.VISIBLE
        } else {
            tagFamily.visibility = View.GONE
        }
    }

    private fun bindTaxonomy(view: View, info: SpeciesInfo) {
        val taxonomyLayout = view.findViewById<View>(R.id.layoutTaxonomy) ?: return

        fun setTaxonomyText(viewId: Int, value: String) {
            val textView = taxonomyLayout.findViewById<TextView>(viewId)
            if (value.isNotEmpty()) {
                textView.setHtml(value)
            } else {
                textView.text = "N/A"
            }
        }

        setTaxonomyText(R.id.tvKingdom, info.kingdom)
        setTaxonomyText(R.id.tvPhylum, info.phylum)
        setTaxonomyText(R.id.tvClass, info.className)
        setTaxonomyText(R.id.tvOrder, info.taxorder)
        setTaxonomyText(R.id.tvFamily, info.family)
        setTaxonomyText(R.id.tvGenus, info.genus)
        setTaxonomyText(R.id.tvSpecies, info.species)
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
            textSize = 20f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 24.dpToPx()
                bottomMargin = 10.dpToPx()
            }
        }

        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                1.dpToPx()
            ).apply {
                bottomMargin = 12.dpToPx()
            }
            setBackgroundColor(Color.parseColor("#F0F0F0"))
        }

        val contentView = TextView(context).apply {
            textSize = 15f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setLineSpacing(0f, 1.4f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                justificationMode = LineBreaker.JUSTIFICATION_MODE_INTER_WORD
            }
            setHtml(content)
        }

        container.addView(titleView)
        container.addView(divider)
        container.addView(contentView)
    }

    private fun setupFab(view: View, info: SpeciesInfo) {
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_speak)

        speakerManager.onSpeechFinished = {
            activity?.runOnUiThread {
                updateFabUI(fab, false)
            }
        }

        fab.setOnClickListener {
            if (isSpeaking) {
                speakerManager.pause()
                updateFabUI(fab, false)
            } else {
                speakerManager.speak(TextToSpeechGenerator.generateSpeechText(requireContext(), info))
                updateFabUI(fab, true)
            }
        }
    }

    private fun updateFabUI(fab: FloatingActionButton, speaking: Boolean) {
        isSpeaking = speaking
        if (speaking) {
            fab.setImageResource(R.drawable.ic_mute)
            fab.backgroundTintList = ColorStateList.valueOf(Color.RED)
        } else {
            fab.setImageResource(R.drawable.ic_speak)
            fab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.green_primary))
        }
    }

    private fun Int.dpToPx(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}