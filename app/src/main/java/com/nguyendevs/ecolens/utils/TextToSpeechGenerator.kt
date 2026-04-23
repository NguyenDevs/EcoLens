package com.nguyendevs.ecolens.utils

import android.content.Context
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.models.SpeciesInfo

/** Kết xuất văn bản âm thoại tự động dựa trên thông số loài sinh vật. */
object TextToSpeechGenerator {

    /** Chắp nối chuỗi dữ liệu phân mảnh hệ sinh thái thành văn bản đọc liền mạch. */
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
        
        if (info.iucn && info.vnredlist) {
            appendSectionIfNotEmpty(sb, context, R.string.tts_iucn, info.conservationStatus)
            appendSectionIfNotEmpty(sb, context, R.string.tts_vnredlist, info.vnredlistStatus)
        } else if (info.iucn) {
            appendSectionIfNotEmpty(sb, context, R.string.tts_iucn, info.conservationStatus)
        } else if (info.vnredlist) {
            appendSectionIfNotEmpty(sb, context, R.string.tts_vnredlist, info.vnredlistStatus)
        } else {
            sb.append("${context.getString(R.string.conservation_disabled_message)}. ")
        }

        return sb.toString()
    }

    /** Phân xuất thông tin phân loại sinh học ra dạng mảng văn bản. */
    private fun buildTaxonomyList(context: Context, info: SpeciesInfo): List<String> {
        val taxonomyList = mutableListOf<String>()
        if (info.kingdom.isNotEmpty()) taxonomyList.add("${context.getString(R.string.label_kingdom)} ${stripHtml(info.kingdom)}")
        if (info.phylum.isNotEmpty()) taxonomyList.add("${context.getString(R.string.label_phylum)} ${stripHtml(info.phylum)}")
        if (info.className.isNotEmpty()) taxonomyList.add("${context.getString(R.string.label_class)} ${stripHtml(info.className)}")
        if (info.taxorder.isNotEmpty()) taxonomyList.add("${context.getString(R.string.label_order)} ${stripHtml(info.taxorder)}")
        if (info.family.isNotEmpty()) taxonomyList.add("${context.getString(R.string.label_family)} ${stripHtml(info.family)}")
        if (info.genus.isNotEmpty()) taxonomyList.add("${context.getString(R.string.label_genus)} ${stripHtml(info.genus)}")
        if (info.species.isNotEmpty()) taxonomyList.add("${context.getString(R.string.label_species)} ${stripHtml(info.species)}")
        return taxonomyList
    }

    /** Khắc họa từng đề mục cụ thể vào tập tin diễn đọc nếu mang nội dung. */
    private fun appendSectionIfNotEmpty(sb: StringBuilder, context: Context, titleResId: Int, content: String) {
        if (content.isNotEmpty()) {
            sb.append("${context.getString(titleResId)}. ${stripHtml(content)}. ")
        }
    }

    /** Khử trần các thẻ ngôn ngữ đánh dấu siêu văn bản ra khỏi chuỗi chuẩn. */
    private fun stripHtml(html: String): String {
        val stripped = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            @Suppress("DEPRECATION")
            android.text.Html.fromHtml(html).toString()
        }
        return stripped.replace("•", "").replace("*", "").trim()
    }
}