package com.nguyendevs.ecolens.fragments

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.model.HistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryDetailFragment : Fragment() {

    private var historyEntry: HistoryEntry? = null

    fun setData(entry: HistoryEntry) {
        this.historyEntry = entry
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val entry = historyEntry ?: return
        val info = entry.speciesInfo

        val imageView = view.findViewById<ImageView>(R.id.detailImageView)
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val tvCommonName = view.findViewById<TextView>(R.id.tvCommonName)
        val tvScientificName = view.findViewById<TextView>(R.id.tvScientificName)
        val tvTaxonomy = view.findViewById<TextView>(R.id.tvTaxonomy)

        val tvDescription = view.findViewById<TextView>(R.id.tvDescription)
        val lblDescription = view.findViewById<TextView>(R.id.lblDescription)
        val tvCharacteristics = view.findViewById<TextView>(R.id.tvCharacteristics)
        val lblCharacteristics = view.findViewById<TextView>(R.id.lblCharacteristics)
        val tvDistribution = view.findViewById<TextView>(R.id.tvDistribution)
        val lblDistribution = view.findViewById<TextView>(R.id.lblDistribution)
        val tvHabitat = view.findViewById<TextView>(R.id.tvHabitat)
        val lblHabitat = view.findViewById<TextView>(R.id.lblHabitat)
        val tvConservation = view.findViewById<TextView>(R.id.tvConservationStatus)
        val lblConservation = view.findViewById<TextView>(R.id.lblConservation)

        Glide.with(this)
            .load(entry.imagePath)
            .transition(DrawableTransitionOptions.withCrossFade())
            .centerCrop()
            .into(imageView)

        tvCommonName.text = info.commonName
        tvScientificName.text = info.scientificName

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {

            val taxonomyHtml = buildString {
                if (info.kingdom.isNotEmpty()) append("• ${getString(R.string.label_kingdom)} ${info.kingdom}<br>")
                if (info.phylum.isNotEmpty()) append("• ${getString(R.string.label_phylum)} ${info.phylum}<br>")
                if (info.className.isNotEmpty()) append("• ${getString(R.string.label_class)} ${info.className}<br>")
                if (info.order.isNotEmpty()) append("• ${getString(R.string.label_order)} ${info.order}<br>")
                if (info.family.isNotEmpty()) append("• ${getString(R.string.label_family)} ${info.family}<br>")
                if (info.genus.isNotEmpty()) append("• ${getString(R.string.label_genus)} ${info.genus}<br>")
                if (info.species.isNotEmpty()) append("• ${getString(R.string.label_species)} ${info.species}")
            }
            val taxonomySpanned = fromHtmlCompat(taxonomyHtml)

            val descSpanned = if(info.description.isNotEmpty()) fromHtmlCompat(info.description) else null
            val charSpanned = if(info.characteristics.isNotEmpty()) fromHtmlCompat(info.characteristics) else null
            val distSpanned = if(info.distribution.isNotEmpty()) fromHtmlCompat(info.distribution) else null
            val habSpanned = if(info.habitat.isNotEmpty()) fromHtmlCompat(info.habitat) else null
            val consSpanned = if(info.conservationStatus.isNotEmpty()) fromHtmlCompat(info.conservationStatus) else null

            withContext(Dispatchers.Main) {
                tvTaxonomy.text = taxonomySpanned

                updateSectionUI(tvDescription, lblDescription, descSpanned)
                updateSectionUI(tvCharacteristics, lblCharacteristics, charSpanned)
                updateSectionUI(tvDistribution, lblDistribution, distSpanned)
                updateSectionUI(tvHabitat, lblHabitat, habSpanned)
                updateSectionUI(tvConservation, lblConservation, consSpanned)
            }
        }

        btnBack.setOnClickListener {
            val fragmentContainer = activity?.findViewById<View>(R.id.fragmentContainer)
            fragmentContainer?.visibility = View.GONE
            parentFragmentManager.popBackStack()
        }
    }

    private fun fromHtmlCompat(source: String): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(source)
        }
    }

    private fun updateSectionUI(textView: TextView, labelView: TextView, content: Spanned?) {
        if (content != null && content.isNotEmpty()) {
            labelView.visibility = View.VISIBLE
            textView.visibility = View.VISIBLE
            textView.text = content
        } else {
            labelView.visibility = View.GONE
            textView.visibility = View.GONE
        }
    }
}