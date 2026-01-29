package com.nguyendevs.ecolens.handlers.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.databinding.ItemCardSpeciesInfoBinding
import com.nguyendevs.ecolens.handlers.animations.HomeAnimationHandler
import com.nguyendevs.ecolens.handlers.util.TextFormatter
import com.nguyendevs.ecolens.model.SpeciesInfo
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeButtonHandler(
    private val context: Context,
    private val binding: ItemCardSpeciesInfoBinding,
    private val homeAnimationHandler: HomeAnimationHandler,
    private val textFormatter: TextFormatter,
    private val onCopySuccess: (String) -> Unit,
    private val onRetryClick: () -> Unit,
    private val handlerScope: CoroutineScope
) {
    private val infoBinding
        get() = binding

    fun hideButtons() {
        infoBinding.btnShareInfo.visibility = View.GONE
        infoBinding.btnRetryIdentification.visibility = View.GONE
    }

    fun showShareButton() {
        homeAnimationHandler.scaleInAnimation(infoBinding.btnShareInfo, duration = 400)
    }

    fun showRetryButton() {
        handlerScope.launch {
            withContext(Dispatchers.Main) {
                homeAnimationHandler.scaleInAnimation(
                        infoBinding.btnRetryIdentification,
                        duration = 400,
                        delay = 100
                )
                setupRetryButton()
            }
        }
    }

    fun hideRetryButton() {
        infoBinding.btnRetryIdentification.visibility = View.GONE
    }

    fun showCopyButton() {
        homeAnimationHandler.scaleInAnimation(
                infoBinding.btnCopyScientificName,
                duration = 400,
                delay = 100
        )
    }

    fun setupCopyButton(info: SpeciesInfo) {
        infoBinding.btnCopyScientificName.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val cleanName = textFormatter.stripHtml(info.scientificName)
            clipboard.setPrimaryClip(ClipData.newPlainText("Scientific Name", cleanName))
            onCopySuccess(cleanName)
        }
    }

    fun setupShareButton(info: SpeciesInfo, imageUri: Uri?) {
        infoBinding.btnShareInfo.setOnClickListener {
            infoBinding.btnShareInfo.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

            handlerScope.launch(Dispatchers.IO) {
                var shareableUri = imageUri

                if (shareableUri != null) {
                    if (shareableUri.scheme == "file" || shareableUri.path?.startsWith("/") == true
                    ) {
                        try {
                            val path = shareableUri.path
                            if (path != null) {
                                val file = File(path)
                                if (file.exists()) {
                                    shareableUri =
                                            FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.provider",
                                                    file
                                            )
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                withContext(Dispatchers.Main) { shareSpeciesInfo(info, shareableUri) }
            }
        }
    }

    private fun setupRetryButton() {
        infoBinding.btnRetryIdentification.setOnClickListener {
            it.visibility = View.GONE
            onRetryClick()
        }
    }

    private fun shareSpeciesInfo(info: SpeciesInfo, imageUri: Uri?) {
        val confidencePercent =
                String.format(
                        "%.2f",
                        if (info.confidence > 1) info.confidence else info.confidence * 100
                )
        val shareText = buildString {
            append(context.getString(R.string.share_title))
            append("\n━━━━━━━━━━━━━━━━━━━━\n\n")
            append(
                    "📌 ${textFormatter.stripHtml(info.commonName)}\n🔬 ${textFormatter.stripHtml(info.scientificName)}\n"
            )
            append(
                    "✅ ${context.getString(R.string.label_confidence_template, confidencePercent)}%\n\n"
            )
            append(
                    "━━━━━━━━━━━━━━━━━━━━\n${context.getString(R.string.share_taxonomy_title)}\n━━━━━━━━━━━━━━━━━━━━\n\n"
            )
            if (info.kingdom.isNotEmpty())
                    append(
                            "• ${context.getString(R.string.label_kingdom)} ${textFormatter.stripHtml(info.kingdom)}\n"
                    )
            if (info.phylum.isNotEmpty())
                    append(
                            "• ${context.getString(R.string.label_phylum)} ${textFormatter.stripHtml(info.phylum)}\n"
                    )
            if (info.className.isNotEmpty())
                    append(
                            "• ${context.getString(R.string.label_class)} ${textFormatter.stripHtml(info.className)}\n"
                    )
            if (info.taxorder.isNotEmpty())
                    append(
                            "• ${context.getString(R.string.label_order)} ${textFormatter.stripHtml(info.taxorder)}\n"
                    )
            if (info.family.isNotEmpty())
                    append(
                            "• ${context.getString(R.string.label_family)} ${textFormatter.stripHtml(info.family)}\n"
                    )
            if (info.genus.isNotEmpty())
                    append(
                            "• ${context.getString(R.string.label_genus)} ${textFormatter.stripHtml(info.genus)}\n"
                    )
            if (info.species.isNotEmpty())
                    append(
                            "• ${context.getString(R.string.label_species)} ${textFormatter.stripHtml(info.species)}\n"
                    )

            val contentList =
                    listOf(
                            info.description to R.string.share_desc_title,
                            info.characteristics to R.string.share_char_title,
                            info.distribution to R.string.share_dist_title,
                            info.habitat to R.string.share_hab_title,
                            info.conservationStatus to R.string.share_cons_title
                    )
            contentList.forEach { (content, title) ->
                if (content.isNotEmpty()) {
                    append(
                            "\n━━━━━━━━━━━━━━━━━━━━\n${context.getString(title)}\n━━━━━━━━━━━━━━━━━━━━\n\n${
                            textFormatter.stripHtml(content)
                        }\n"
                    )
                }
            }
            append("\n━━━━━━━━━━━━━━━━━━━━\n${context.getString(R.string.share_footer)}")
        }

        try {
            val intent =
                    Intent(Intent.ACTION_SEND).apply {
                        if (imageUri != null) {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, imageUri)
                            clipData = ClipData.newRawUri(null, imageUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } else {
                            type = "text/plain"
                        }
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        putExtra(
                                Intent.EXTRA_SUBJECT,
                                context.getString(
                                        R.string.share_subject,
                                        textFormatter.stripHtml(info.commonName)
                                )
                        )
                    }
            context.startActivity(
                    Intent.createChooser(intent, context.getString(R.string.share_chooser_title))
            )
        } catch (e: Exception) {
            Toast.makeText(
                            context,
                            "${context.getString(R.string.error)}: ${e.message}",
                            Toast.LENGTH_SHORT
                    )
                    .show()
        }
    }
}
