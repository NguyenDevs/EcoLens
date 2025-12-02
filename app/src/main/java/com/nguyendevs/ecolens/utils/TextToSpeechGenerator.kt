package com.nguyendevs.ecolens.utils

import com.nguyendevs.ecolens.model.SpeciesInfo

object TextToSpeechGenerator {
    fun generateSpeechText(info: SpeciesInfo): String {
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
        sb.append("Tên khoa học ${info.scientificName}. ")

        val taxonomyList = mutableListOf<String>()
        if (info.kingdom.isNotEmpty()) taxonomyList.add("Giới ${stripHtml(info.kingdom)}")
        if (info.phylum.isNotEmpty()) taxonomyList.add("Ngành ${stripHtml(info.phylum)}")
        if (info.className.isNotEmpty()) taxonomyList.add("Lớp ${stripHtml(info.className)}")
        if (info.order.isNotEmpty()) taxonomyList.add("Bộ ${stripHtml(info.order)}")
        if (info.family.isNotEmpty()) taxonomyList.add("Họ ${stripHtml(info.family)}")
        if (info.genus.isNotEmpty()) taxonomyList.add("Chi ${stripHtml(info.genus)}")
        if (info.species.isNotEmpty()) taxonomyList.add("Loài ${stripHtml(info.species)}")

        if (taxonomyList.isNotEmpty()) {
            sb.append("Phân loại khoa học: ")
            sb.append(taxonomyList.joinToString(", "))
            sb.append(". ")
        }

        if (info.description.isNotEmpty()) {
            sb.append("Mô tả. ${stripHtml(info.description)}. ")
        }
        if (info.characteristics.isNotEmpty()) {
            sb.append("Đặc điểm. ${stripHtml(info.characteristics)}. ")
        }
        if (info.distribution.isNotEmpty()) {
            sb.append("Phân bố. ${stripHtml(info.distribution)}. ")
        }
        if (info.habitat.isNotEmpty()) {
            sb.append("Môi trường sống. ${stripHtml(info.habitat)}. ")
        }
        if (info.conservationStatus.isNotEmpty()) {
            sb.append("Tình trạng bảo tồn. ${stripHtml(info.conservationStatus)}.")
        }
        return sb.toString()
    }
}