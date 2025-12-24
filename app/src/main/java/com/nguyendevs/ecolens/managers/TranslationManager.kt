package com.nguyendevs.ecolens.managers

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.nguyendevs.ecolens.model.SpeciesInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.Locale

class TranslationManager {

    companion object {
        private const val TAG = "TranslationManager"
    }

    /**
     * Dịch một chuỗi văn bản đơn lẻ.
     * Trả về text gốc nếu dịch thất bại hoặc text rỗng.
     */
    suspend fun translateText(text: String, fromLangCode: String, toLangCode: String): String {
        if (text.isBlank() || fromLangCode.equals(toLangCode, ignoreCase = true)) {
            return text
        }

        // Chuyển đổi mã ngôn ngữ sang format của ML Kit
        val sourceLang = mapLanguageCode(fromLangCode) ?: return text
        val targetLang = mapLanguageCode(toLangCode) ?: return text

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(targetLang)
            .build()

        val translator = Translation.getClient(options)

        return try {
            // Tải model nếu cần thiết (chỉ tải qua Wifi để tiết kiệm data user)
            val conditions = DownloadConditions.Builder().requireWifi().build()
            translator.downloadModelIfNeeded(conditions).await()

            // Thực hiện dịch
            translator.translate(text).await()
        } catch (e: Exception) {
            Log.e(TAG, "Translation error: ${e.message}")
            text
        } finally {
            translator.close()
        }
    }

    /**
     * Dịch toàn bộ đối tượng SpeciesInfo.
     * Hàm này sẽ dịch song song các trường quan trọng để tăng tốc độ.
     */
    suspend fun translateSpeciesInfo(info: SpeciesInfo, fromLangCode: String, targetLangCode: String): SpeciesInfo {
        // Nếu ngôn ngữ nguồn và đích giống nhau, không làm gì cả
        if (fromLangCode.equals(targetLangCode, ignoreCase = true)) {
            return info
        }

        return withContext(Dispatchers.Default) {
            // Tạo bản copy mới với các trường được dịch
            info.copy(
                commonName = translateText(info.commonName, fromLangCode, targetLangCode),
                description = translateText(info.description, fromLangCode, targetLangCode),
                characteristics = translateText(info.characteristics, fromLangCode, targetLangCode),
                distribution = translateText(info.distribution, fromLangCode, targetLangCode),
                habitat = translateText(info.habitat, fromLangCode, targetLangCode),
                conservationStatus = translateText(info.conservationStatus, fromLangCode, targetLangCode),

                // Các trường phân loại học (thường là tên khoa học hoặc Latin, có thể dịch hoặc giữ nguyên tùy ý)
                kingdom = translateText(info.kingdom, fromLangCode, targetLangCode),
                phylum = translateText(info.phylum, fromLangCode, targetLangCode),
                className = translateText(info.className, fromLangCode, targetLangCode),
                taxorder = translateText(info.taxorder, fromLangCode, targetLangCode),
                family = translateText(info.family, fromLangCode, targetLangCode),
                // Genus và Species thường là tên khoa học Latin, không nên dịch
                genus = info.genus,
                species = info.species
            )
        }
    }

    /**
     * Helper mapping mã ngôn ngữ ("vi", "en") sang hằng số ML Kit
     */
    private fun mapLanguageCode(code: String): String? {
        return when (code.lowercase(Locale.getDefault())) {
            "vi" -> TranslateLanguage.VIETNAMESE
            "en" -> TranslateLanguage.ENGLISH
            // Thêm các ngôn ngữ khác nếu cần
            else -> null
        }
    }
}