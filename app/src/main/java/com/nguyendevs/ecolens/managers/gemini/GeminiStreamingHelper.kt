package com.nguyendevs.ecolens.managers.gemini

import android.util.Log
import com.google.gson.Gson
import com.nguyendevs.ecolens.api.*
import com.nguyendevs.ecolens.models.*
import com.nguyendevs.ecolens.utils.MarkdownProcessor
import com.nguyendevs.ecolens.utils.PromptBuilder
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException

class GeoBlockedException : IOException("Geo blocked")

/** Xử lý luồng dữ liệu trả lời dạng stream qua Gemini API. */
class GeminiStreamingHelper(
    private val apiService: INaturalistApi,
    private val gson: Gson
) {
    private val markdownProcessor = MarkdownProcessor()

    companion object {
        private const val TAG = "GeminiStreaming"
        private const val PREFIX_DATA = "data: "
        private const val STREAM_DONE = "[DONE]"
        private const val DETAILS_DELAY = 0L
    }

    suspend fun getCommonName(
        scientificName: String,
        languageCode: String
    ): String? = withContext(Dispatchers.IO) {
        val prompt = PromptBuilder.buildCommonNamePrompt(scientificName, languageCode)
        val request = createGeminiRequest(prompt)

        try {
            val response = apiService.askGemini(request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrEmpty()) {
                val cleanedJson = cleanJsonString(text)
                val result = gson.fromJson(cleanedJson, CommonNameResponse::class.java)
                return@withContext result.commonName
            }
        } catch (e: Exception) {
            Log.e(TAG, "GetCommonName Error: ${e.message}")
        }
        return@withContext null
    }

    suspend fun translateTaxonomy(
        kingdom: String,
        phylum: String,
        className: String,
        taxorder: String,
        family: String,
        genus: String,
        species: String
    ): TaxonomyTranslationResponse? = withContext(Dispatchers.IO) {
        val prompt = PromptBuilder.buildTaxonomyTranslationPrompt(
            kingdom, phylum, className, taxorder, family, genus, species
        )
        val request = createGeminiRequest(prompt)

        try {
            val response = apiService.askGroq(request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrEmpty()) {
                val cleanedJson = cleanJsonString(text)
                return@withContext gson.fromJson(cleanedJson, TaxonomyTranslationResponse::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "TranslateTaxonomy Error: ${e.message}")
        }
        return@withContext null
    }

    suspend fun streamDetails(
        scientificName: String,
        confidence: Double,
        languageCode: String,
        currentInfo: SpeciesInfo,
        retryCount: Int = 0,
        onStateUpdate: (EcoLensUiState) -> Unit
    ): Unit = withContext(Dispatchers.IO) {
        val isVietnamese = languageCode == "vi"
        val prompt = PromptBuilder.buildDetailsPrompt(scientificName, languageCode)
        val request = createGeminiRequest(prompt)

        try {
            val response = apiService.streamGroq(request)
            if (response.isSuccessful) {
                processStreamResponse(response, DetailsResponse::class.java) { details ->
                    updateDetailsUISync(details, isVietnamese, currentInfo, onStateUpdate)
                }
            } else {
                val errorMsg = response.errorBody()?.string()
                Log.e(TAG, "StreamDetails Failed (HTTP ${response.code()}): $errorMsg")

                if (response.code() >= 500 && retryCount < 1) {
                    delay(1000)
                    streamDetails(scientificName, confidence, languageCode, currentInfo, retryCount + 1, onStateUpdate)
                } else {
                    withContext(Dispatchers.Main) {
                        onStateUpdate(EcoLensUiState(isLoading = true, speciesInfo = currentInfo.copy(description = "Không thể tải thông tin từ AI (Lỗi ${response.code()})")))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "StreamDetails Exception: ${e.message}")
            if (retryCount < 1) {
                delay(1000)
                streamDetails(scientificName, confidence, languageCode, currentInfo, retryCount + 1, onStateUpdate)
            }
        }
        Unit
    }

    suspend fun streamConservation(
        scientificName: String,
        iucnCode: String,
        languageCode: String,
        currentInfo: SpeciesInfo,
        onStateUpdate: (EcoLensUiState) -> Unit
    ) = withContext(Dispatchers.IO) {
        val isVietnamese = languageCode == "vi"
        val prompt = PromptBuilder.buildConservationPrompt(scientificName, iucnCode, languageCode)
        val request = createGeminiRequest(prompt)

        try {
            val response = apiService.streamGroq(request)
            if (response.isSuccessful) {
                processStreamResponse(response, ConservationResponse::class.java) { conservation ->
                    updateConservationUISync(conservation, isVietnamese, currentInfo, onStateUpdate)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "StreamConservation Error: ${e.message}")
        }
        Unit
    }

    /** Gói dữ liệu Prompt tạo mới request cho Gemini. */
    private fun createGeminiRequest(prompt: String): GeminiRequest {
        return GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = prompt))
                )
            )
        )
    }

    /** Tách và xử lý luồng sự kiện phân đoạn JSON. */
    private suspend fun <T> processStreamResponse(
        response: Response<ResponseBody>,
        type: Class<T>,
        onUpdate: suspend (T) -> Unit
    ) {
        try {
            response.body()?.byteStream()?.bufferedReader()?.use { reader ->
                val accumulatedJson = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue

                    if (currentLine.startsWith(PREFIX_DATA)) {
                        processDataLine(currentLine, accumulatedJson, type, onUpdate)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stream processing error: ${e.message}")
            throw e
        }
    }

    /** Giải quyết nội dung truyền tải trực tiếp qua stream dòng dữ liệu. */
    private suspend fun <T> processDataLine(
        line: String,
        accumulatedJson: StringBuilder,
        type: Class<T>,
        onUpdate: suspend (T) -> Unit
    ) {
        val jsonData = line.substring(PREFIX_DATA.length).trim()
        if (jsonData == STREAM_DONE) return

        try {
            val streamResponse = gson.fromJson(jsonData, GeminiResponse::class.java)
            val chunk = streamResponse.candidates?.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text

            if (chunk != null) {
                accumulatedJson.append(chunk)
                val rawJson = accumulatedJson.toString()
                val details = tryParsePartialJson(rawJson, type)
                if (details != null) {
                    onUpdate(details)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}")
        }
    }

    /** Thử parse JSON chưa hoàn chỉnh bằng cách tự động đóng các dấu ngoặc. */
    private fun <T> tryParsePartialJson(json: String, type: Class<T>): T? {
        val cleaned = cleanJsonString(json)
        if (cleaned.isEmpty()) return null

        val candidates = mutableListOf<String>()
        candidates.add(cleaned)
        candidates.add("$cleaned\"}")
        candidates.add("$cleaned}")
        candidates.add("$cleaned\"}]}")
        
        for (candidate in candidates) {
            try {
                return gson.fromJson(candidate, type)
            } catch (e: Exception) {
            }
        }
        return null
    }

    /** Thanh lọc JSON khỏi các thẻ đánh dấu Markdown phức tạp. */
    private fun cleanJsonString(json: String): String {
        val firstBrace = json.indexOf('{')
        if (firstBrace == -1) return ""
        
        val lastBrace = json.lastIndexOf('}')
        return if (lastBrace > firstBrace) {
            json.substring(firstBrace, lastBrace + 1)
        } else {
            json.substring(firstBrace)
        }
    }

    /** Cập nhật UI ngay khi nội dung mô tả của loài đã đủ kiện đồng bộ. */
    private suspend fun updateDetailsUISync(
        details: DetailsResponse,
        isVietnamese: Boolean,
        currentInfo: SpeciesInfo,
        onStateUpdate: (EcoLensUiState) -> Unit
    ) = withContext(Dispatchers.Main) {
        var updated = currentInfo

        suspend fun updateState(stage: LoadingStage) {
            onStateUpdate(EcoLensUiState(isLoading = true, speciesInfo = updated, loadingStage = stage))
            delay(DETAILS_DELAY)
        }

        details.description?.takeIf { it.isNotBlank() }?.let {
            updated = updated.copy(
                description = markdownProcessor.process(it, isVietnamese = isVietnamese)
            )
            updateState(LoadingStage.DESCRIPTION)
        }

        val characteristicsText = when (val chars = details.characteristics) {
            is String -> chars
            is List<*> -> chars.joinToString("\n")
            else -> ""
        }
        if (characteristicsText.isNotBlank()) {
            updated = updated.copy(
                characteristics = markdownProcessor.process(characteristicsText, isVietnamese = isVietnamese)
            )
            updateState(LoadingStage.CHARACTERISTICS)
        }

        details.distribution?.takeIf { it.isNotBlank() }?.let {
            updated = updated.copy(
                distribution = markdownProcessor.process(it, isVietnamese = isVietnamese)
            )
            updateState(LoadingStage.DISTRIBUTION)
        }

        details.habitat?.takeIf { it.isNotBlank() }?.let {
            updated = updated.copy(
                habitat = markdownProcessor.process(it, isVietnamese = isVietnamese)
            )
            updateState(LoadingStage.HABITAT)
        }
    }

    /** Cập nhật trình bày khung bảo tồn. */
    private suspend fun updateConservationUISync(
        conservation: ConservationResponse,
        isVietnamese: Boolean,
        currentInfo: SpeciesInfo,
        onStateUpdate: (EcoLensUiState) -> Unit
    ) = withContext(Dispatchers.Main) {
        conservation.conservationStatus?.takeIf { it.isNotBlank() }?.let {
            val updated = currentInfo.copy(
                conservationStatus = markdownProcessor.process(
                    it,
                    isConservationStatus = true,
                    isVietnamese = isVietnamese
                )
            )
            onStateUpdate(EcoLensUiState(isLoading = true, speciesInfo = updated, loadingStage = LoadingStage.CONSERVATION))
        }
    }



    data class CommonNameResponse(
        val commonName: String? = null
    )

    data class TaxonomyTranslationResponse(
        val kingdom: String? = null,
        val phylum: String? = null,
        val className: String? = null,
        val taxorder: String? = null,
        val family: String? = null,
        val genus: String? = null,
        val species: String? = null
    )

    data class DetailsResponse(
        val description: String? = null,
        val characteristics: Any? = null,
        val distribution: String? = null,
        val habitat: String? = null
    )

    data class ConservationResponse(
        val conservationStatus: String? = null
    )

    suspend fun translateText(
        text: String,
        languageCode: String
    ): String? = withContext(Dispatchers.IO) {
        val prompt = PromptBuilder.buildTextTranslationPrompt(text, languageCode)
        val request = createGeminiRequest(prompt)

        try {
            val response = apiService.askGroq(request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!responseText.isNullOrEmpty()) {
                val cleanedJson = cleanJsonString(responseText)
                val result = gson.fromJson(cleanedJson, TextTranslationResponse::class.java)
                return@withContext result.translatedText
            }
        } catch (e: Exception) {
            Log.e(TAG, "TranslateText Error: ${e.message}")
        }
        return@withContext null
    }

    data class TextTranslationResponse(
        val translatedText: String? = null
    )
}
