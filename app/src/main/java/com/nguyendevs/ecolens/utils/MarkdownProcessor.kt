package com.nguyendevs.ecolens.utils

import java.util.Locale

/**
 * Xử lý và định dạng text Markdown
 * Chuyển đổi cú pháp Markdown sang HTML và tô màu trạng thái bảo tồn
 */
class MarkdownProcessor {

    /**
     * Xử lý text Markdown thành HTML
     *
     * @param text Text cần xử lý
     * @param isConservationStatus Có phải là trạng thái bảo tồn không
     * @param isVietnamese Sử dụng tiếng Việt hay không
     * @return Text đã được xử lý thành HTML
     */
    fun process(
        text: String?,
        isConservationStatus: Boolean = false,
        isVietnamese: Boolean = true
    ): String {
        if (text.isNullOrBlank()) return ""

        var result = text
            // bold **text**
            .replace(Regex("\\*\\*(.+?)\\*\\*")) {
                "<b>${it.groupValues[1]}</b>"
            }

            // heading ##text##
            .replace(Regex("##(.+?)##")) {
                "<font color='#00796B'><b>${it.groupValues[1]}</b></font>"
            }

            // italic ~~text~~
            .replace(Regex("~~(.+?)~~")) {
                "<i>${it.groupValues[1]}</i>"
            }

            // italic *text* (không ăn **bold**)
            .replace(Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")) {
                "<i>${it.groupValues[1]}</i>"
            }

            // italic `text`
            .replace(Regex("`(.+?)`")) {
                "<i>${it.groupValues[1]}</i>"
            }

            .replace("\n", "<br>")

        if (isConservationStatus) {
            result = colorizeConservationStatus(result, isVietnamese)
        }

        return result
    }

    private fun colorizeConservationStatus(text: String, isVietnamese: Boolean): String {
        // IUCN Red List Categories & Criteria
        // EX - Extinct (Tuyệt chủng) - Black
        // EW - Extinct in the Wild (Tuyệt chủng trong tự nhiên) - Purple
        // CR - Critically Endangered (Cực kỳ nguy cấp) - Red
        // EN - Endangered (Nguy cấp) - Orange
        // VU - Vulnerable (Sắp nguy cấp) - Yellow
        // NT - Near Threatened (Sắp bị đe dọa) - Light Green
        // LC - Least Concern (Ít quan tâm) - Green
        // DD - Data Deficient (Thiếu dữ liệu) - Grey
        // NE - Not Evaluated (Chưa đánh giá) - White/Grey

        val statusMap = if (isVietnamese) {
            mapOf(
                "EX" to "#000000",
                "EW" to "#800080",
                "CR" to "#D81E05",
                "EN" to "#FC7F3F",
                "VU" to "#F9E814",
                "NT" to "#CCE226",
                "LC" to "#60C659",
                "DD" to "#D1D1D6",
                "NE" to "#FFFFFF",
                "Tuyệt chủng" to "#000000",
                "Tuyệt chủng trong tự nhiên" to "#800080",
                "Cực kỳ nguy cấp" to "#D81E05",
                "Nguy cấp" to "#FC7F3F",
                "Sắp nguy cấp" to "#F9E814",
                "Sắp bị đe dọa" to "#CCE226",
                "Ít quan tâm" to "#60C659",
                "Thiếu dữ liệu" to "#D1D1D6",
                "Chưa đánh giá" to "#FFFFFF"
            )
        } else {
            mapOf(
                "EX" to "#000000",
                "EW" to "#800080",
                "CR" to "#D81E05",
                "EN" to "#FC7F3F",
                "VU" to "#F9E814",
                "NT" to "#CCE226",
                "LC" to "#60C659",
                "DD" to "#D1D1D6",
                "NE" to "#FFFFFF",
                "Extinct" to "#000000",
                "Extinct in the Wild" to "#800080",
                "Critically Endangered" to "#D81E05",
                "Endangered" to "#FC7F3F",
                "Vulnerable" to "#F9E814",
                "Near Threatened" to "#CCE226",
                "Least Concern" to "#60C659",
                "Data Deficient" to "#D1D1D6",
                "Not Evaluated" to "#FFFFFF"
            )
        }

        var result = text
        statusMap.entries.sortedByDescending { it.key.length }.forEach { (status, color) ->
            // Match whole word to avoid partial matches (e.g. "EN" inside "ENDANGERED")
            // But for Vietnamese phrases, simple contains is safer.
            // For codes like EX, EW, CR... we should be careful.
            
            if (status.length <= 2) {
                 // For codes, use word boundary
                 result = result.replace(
                    Regex("\\b$status\\b", RegexOption.IGNORE_CASE),
                    "<font color='$color'><b>$status</b></font>"
                )
            } else {
                if (result.contains(status, ignoreCase = true)) {
                    result = result.replace(
                        Regex("(?i)$status"),
                        "<font color='$color'><b>$status</b></font>"
                    )
                }
            }
        }
        return result
    }

    fun getConservationStatusExplanation(code: String, isVietnamese: Boolean = true): String {
        return if (isVietnamese) {
            when (code.uppercase(Locale.getDefault())) {
                "EX" -> "Tuyệt chủng"
                "EW" -> "Tuyệt chủng trong tự nhiên"
                "CR" -> "Cực kỳ nguy cấp"
                "EN" -> "Nguy cấp"
                "VU" -> "Sắp nguy cấp"
                "NT" -> "Sắp bị đe dọa"
                "LC" -> "Ít quan tâm"
                "DD" -> "Thiếu dữ liệu"
                "NE" -> "Chưa đánh giá"
                else -> ""
            }
        } else {
            when (code.uppercase(Locale.getDefault())) {
                "EX" -> "Extinct"
                "EW" -> "Extinct in the Wild"
                "CR" -> "Critically Endangered"
                "EN" -> "Endangered"
                "VU" -> "Vulnerable"
                "NT" -> "Near Threatened"
                "LC" -> "Least Concern"
                "DD" -> "Data Deficient"
                "NE" -> "Not Evaluated"
                else -> ""
            }
        }
    }

    /**
     * Xóa prefix phân loại (rank) khỏi text
     * Ví dụ: "Genus: Panthera" -> "Panthera"
     */
    fun removeRankPrefix(text: String?, prefix: String): String {
        return text?.trim()?.replaceFirst(Regex("^(?i)$prefix\\s*[:\\-\\s]+"), "")
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            ?.trim() ?: ""
    }
}