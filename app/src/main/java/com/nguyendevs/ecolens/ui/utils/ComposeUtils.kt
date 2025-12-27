package com.nguyendevs.ecolens.ui.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.Html
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.*

// HTML Text Utils
object HtmlTextUtils {
    fun stripHtml(html: String): String {
        var text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html).toString()
        }
        text = text.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
        text = text.replace(Regex("\\*(.*?)\\*"), "$1")
        return text.trim()
    }
}

// Copy to clipboard
fun Context.copyToClipboard(text: String, label: String = "EcoLens") {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(this, "Đã sao chép", Toast.LENGTH_SHORT).show()
}

// Share text
fun Context.shareText(text: String, chooserTitle: String = "Chia sẻ") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(intent, chooserTitle))
}

// Share with image
fun Context.shareTextWithImage(text: String, imageUri: Uri?, chooserTitle: String = "Chia sẻ") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        if (imageUri != null) {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            clipData = ClipData.newRawUri(null, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            type = "text/plain"
        }
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(intent, chooserTitle))
}

// Open URL
fun Context.openUrl(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(this, "Không thể mở liên kết", Toast.LENGTH_SHORT).show()
    }
}

// Send email
fun Context.sendEmail(email: String, subject: String = "") {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(this, "Không tìm thấy ứng dụng Email", Toast.LENGTH_SHORT).show()
    }
}

// Date formatters
@Composable
fun rememberDateFormatter(pattern: String = "dd/MM/yyyy"): SimpleDateFormat {
    return remember {
        SimpleDateFormat(pattern, Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
    }
}

@Composable
fun rememberTimeFormatter(pattern: String = "HH:mm"): SimpleDateFormat {
    return remember {
        SimpleDateFormat(pattern, Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
    }
}