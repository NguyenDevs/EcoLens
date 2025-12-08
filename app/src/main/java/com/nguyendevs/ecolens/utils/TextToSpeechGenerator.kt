package com.nguyendevs.ecolens.utils

import android.content.Context
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.model.SpeciesInfo

/**
 * Object chứa các utility functions tạo văn bản cho Text-to-Speech
 */
object TextToSpeechGenerator {

    /**
     * Tạo văn bản đầy đủ từ thông tin loài để đọc bằng TTS
     */
    fun generateSpeechText(context: Context, info: SpeciesInfo): String {
        val sb = StringBuilder()
        sb.append("${info.commonName}. ")
        sb.append("${context.getString(R.string.tts_scientific_name)} ${info.scientificName}. ")

        val taxonomyList = buildTaxonomyList(context, info)
        if (taxonomyList.isNotEmpty()) {
            sb.append("${context.getString(R.string.tts_taxonomy)}: ")
            sb.append(taxonomyList.joinToString(", "))
            sb.append(". ")
        }

        appendSectionIfNotEmpty(sb, context, R.string.section_description, info.description)
        appendSectionIfNotEmpty(sb, context, R.string.section_characteristics, info.characteristics)
        appendSectionIfNotEmpty(sb, context, R.string.section_distribution, info.distribution)
        appendSectionIfNotEmpty(sb, context, R.string.section_habitat, info.habitat)
        appendSectionIfNotEmpty(sb, context, R.string.section_conservation, info.conservationStatus)

        return sb.toString()
    }

    /**
     * Xây dựng danh sách phân loại khoa học
     */
    private fun buildTaxonomyList(context: Context, info: SpeciesInfo): List<String> {
        val taxonomyList = mutableListOf<String>()
        if (info.kingdom.isNotEmpty()) taxonomyList.add("${context.getString(R.string.label_kingdom)} ${stripHtml(info.kingdom)}")
        if (info.phylum.isNotEmpty()) taxonomyList.add("${context.getString(R.string.label_phylum)} ${stripHtml(info.phylum)}")
        if (info.className.isNotEmpty()) taxonomyList.add("${context.getString(R.string.label_class)} ${stripHtml(info.className)}")
        if (info.order.isNotEmpty()) taxonomyList.add("${context.getString(R.string.label_order)} ${stripHtml(info.order)}")
        if (info.family.isNotEmpty()) taxonomyList.add("${context.getString(R.string.label_family)} ${stripHtml(info.family)}")
        if (info.genus.isNotEmpty()) taxonomyList.add("${context.getString(R.string.label_genus)} ${stripHtml(info.genus)}")
        if (info.species.isNotEmpty()) taxonomyList.add("${context.getString(R.string.label_species)} ${stripHtml(info.species)}")
        return taxonomyList
    }

    /**
     * Thêm một section vào văn bản nếu nội dung không rỗng
     */
    private fun appendSectionIfNotEmpty(sb: StringBuilder, context: Context, titleResId: Int, content: String) {
        if (content.isNotEmpty()) {
            sb.append("${context.getString(titleResId)}. ${stripHtml(content)}. ")
        }
    }

    /**
     * Loại bỏ HTML tags khỏi chuỗi
     */
    private fun stripHtml(html: String): String {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            @Suppress("DEPRECATION")
            android.text.Html.fromHtml(html).toString()
        }
    }
}