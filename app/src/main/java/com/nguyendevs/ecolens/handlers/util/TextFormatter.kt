package com.nguyendevs.ecolens.handlers.util

import android.os.Build
import android.text.Html
import android.widget.TextView

/** Tiện ích xử lý HTML và markdown trong text. */
class TextFormatter {
    companion object {
        private val REGEX_BOLD = Regex("\\*\\*(.+?)\\*\\*")
        private val REGEX_ITALIC = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")
        private val REGEX_CODE = Regex("`(.+?)`")
    }

    /** Set text từ HTML vào TextView, hỗ trợ cả API cũ và mới. */
    fun setHtml(textView: TextView, htmlContent: String) {
        textView.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(htmlContent)
        }
    }

    /** Xóa HTML tags và markdown formatting khỏi chuỗi. */
    fun stripHtml(html: String): String {
        var text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html).toString()
        }

        text = text.replace(REGEX_BOLD, "$1")
        text = text.replace(REGEX_ITALIC, "$1")
        text = text.replace(REGEX_CODE, "$1")

        return text.trim()
    }
}