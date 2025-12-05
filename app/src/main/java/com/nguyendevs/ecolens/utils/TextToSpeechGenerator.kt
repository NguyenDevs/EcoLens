package com.nguyendevs.ecolens.utils

import android.content.Context
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.model.SpeciesInfo

object TextToSpeechGenerator {

    // Tạo văn bản để đọc từ thông tin loài
    fun generateSpeechText(context: Context, info: SpeciesInfo): String {
        fun stripHtml(html: String): String {
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_COMPACT).toString()
            } else {
                @Suppress("DEPRECATION")
                android.text.Html.fromHtml(html).toString()
            }
        }

        val sb = StringBuilder()
        sb.append("${info.commonName}. ")
        sb.append("${context.getString(R.string.tts_scientific_name)} ${info.scientificName}. ")

        val taxonomyList = mutableListOf<String>()
        if (info.kingdom.isNotEmpty()) taxonomyList.add("${context.getString(R.string.label_kingdom)} ${stripHtml(info.kingdom)}")
        if (info.phylum.isNotEmpty()) taxonomyList.add("${context.getString(R.string.label_phylum)} ${stripHtml(info.phylum)}")
        if (info.className.isNotEmpty()) taxonomyList.add("${context.getString(R.string.label_class)} ${stripHtml(info.className)}")
        if (info.order.isNotEmpty()) taxonomyList.add("${context.getString(R.string.label_order)} ${stripHtml(info.order)}")
        if (info.family.isNotEmpty()) taxonomyList.add("${context.getString(R.string.label_family)} ${stripHtml(info.family)}")
        if (info.genus.isNotEmpty()) taxonomyList.add("${context.getString(R.string.label_genus)} ${stripHtml(info.genus)}")
        if (info.species.isNotEmpty()) taxonomyList.add("${context.getString(R.string.label_species)} ${stripHtml(info.species)}")

        if (taxonomyList.isNotEmpty()) {
            sb.append("${context.getString(R.string.tts_taxonomy)}: ")
            sb.append(taxonomyList.joinToString(", "))
            sb.append(". ")
        }

        if (info.description.isNotEmpty()) {
            sb.append("${context.getString(R.string.section_description)}. ${stripHtml(info.description)}. ")
        }
        if (info.characteristics.isNotEmpty()) {
            sb.append("${context.getString(R.string.section_characteristics)}. ${stripHtml(info.characteristics)}. ")
        }
        if (info.distribution.isNotEmpty()) {
            sb.append("${context.getString(R.string.section_distribution)}. ${stripHtml(info.distribution)}. ")
        }
        if (info.habitat.isNotEmpty()) {
            sb.append("${context.getString(R.string.section_habitat)}. ${stripHtml(info.habitat)}. ")
        }
        if (info.conservationStatus.isNotEmpty()) {
            sb.append("${context.getString(R.string.section_conservation)}. ${stripHtml(info.conservationStatus)}.")
        }
        return sb.toString()
    }
}