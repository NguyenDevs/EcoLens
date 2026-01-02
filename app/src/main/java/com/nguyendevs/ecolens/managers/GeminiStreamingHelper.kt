package com.nguyendevs.ecolens.managers

import android.util.Log
import com.google.gson.Gson
import com.nguyendevs.ecolens.api.*
import com.nguyendevs.ecolens.model.*
import com.nguyendevs.ecolens.utils.MarkdownProcessor
import com.nguyendevs.ecolens.utils.PromptBuilder
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException

class GeoBlockedException : IOException("Geo blocked")

class GeminiStreamingHelper(
    private val apiService: INaturalistApi,
    private val gson: Gson
) {
    private val markdownProcessor = MarkdownProcessor()

    companion object {
        private const val TAG = "GeminiStreaming"
        private const val PREFIX_DATA = "data: "
        private const val STREAM_DONE = "[DONE]"
        private const val UI_DELAY = 150L
    }

    suspend fun streamTaxonomy(
        scientificName: String,
        confidence: Double,
        languageCode: String,
        onStateUpdate: (EcoLensUiState) -> Unit
    ) = withContext(Dispatchers.IO) {
        val isVietnamese = languageCode != "en"
        val prompt = PromptBuilder.buildTaxonomyPrompt(scientificName, isVietnamese)
        val request = createGeminiRequest(prompt)

        val response = apiService.streamGemini(request)
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string() ?: ""
            if (errorBody.contains("User location is not supported", ignoreCase = true)) {
                throw GeoBlockedException()
            }
            throw IOException("API Error: ${response.code()} - $errorBody")
        }

        processStreamResponse(response, TaxonomyResponse::class.java) { taxonomy ->
            updateTaxonomyUISync(taxonomy, scientificName, isVietnamese, confidence, onStateUpdate)
        }
    }

    suspend fun streamDetails(
        scientificName: String,
        confidence: Double,
        languageCode: String,
        currentInfo: SpeciesInfo,
        onStateUpdate: (EcoLensUiState) -> Unit
    ) = withContext(Dispatchers.IO) {
        val isVietnamese = languageCode != "en"
        val prompt = PromptBuilder.buildDetailsPrompt(scientificName, isVietnamese)
        val request = createGeminiRequest(prompt)

        try {
            val response = apiService.streamGemini(request)
            if (response.isSuccessful) {
                processStreamResponse(response, DetailsResponse::class.java) { details ->
                    updateDetailsUISync(details, isVietnamese, currentInfo, onStateUpdate)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "StreamDetails Error: ${e.message}")
        }
    }

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
                        val jsonData = currentLine.substring(PREFIX_DATA.length).trim()
                        if (jsonData == STREAM_DONE) break

                        try {
                            val streamResponse = gson.fromJson(jsonData, GeminiResponse::class.java)
                            val chunk = streamResponse.candidates?.firstOrNull()
                                ?.content?.parts?.firstOrNull()?.text

                            if (!chunk.isNullOrEmpty()) {
                                accumulatedJson.append(chunk)
                                val cleanedJson = cleanJsonString(accumulatedJson.toString())
                                try {
                                    val result = gson.fromJson(cleanedJson, type)
                                    onUpdate(result)
                                } catch (e: Exception) {
                                    // JSON incomplete, ignore
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Parse error: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stream processing error: ${e.message}")
            throw e
        }
    }

    private suspend fun updateTaxonomyUISync(
        taxonomy: TaxonomyResponse,
        scientificName: String,
        isVietnamese: Boolean,
        confidence: Double,
        onStateUpdate: (EcoLensUiState) -> Unit
    ) = withContext(Dispatchers.Main) {
        var updated = SpeciesInfo(
            scientificName = scientificName,
            confidence = confidence,
            commonName = taxonomy.commonName ?: "..."
        )

        suspend fun updateState(stage: LoadingStage) {
            onStateUpdate(EcoLensUiState(isLoading = true, speciesInfo = updated, loadingStage = stage))
            delay(UI_DELAY)
        }

        if (!taxonomy.commonName.isNullOrBlank() && taxonomy.commonName != "...") {
            updated = updated.copy(commonName = taxonomy.commonName)
            updateState(LoadingStage.COMMON_NAME)
        }

        fun format(value: String, vi: String, en: String) =
            "<b>${markdownProcessor.removeRankPrefix(value, if (isVietnamese) vi else en)}</b>"

        taxonomy.kingdom?.let { updated = updated.copy(kingdom = format(it, "Giới", "Kingdom")); updateState(LoadingStage.TAXONOMY) }
        taxonomy.phylum?.let { updated = updated.copy(phylum = format(it, "Ngành", "Phylum")); updateState(LoadingStage.TAXONOMY) }
        taxonomy.className?.let { updated = updated.copy(className = format(it, "Lớp", "Class")); updateState(LoadingStage.TAXONOMY) }
        taxonomy.taxorder?.let { updated = updated.copy(taxorder = format(it, "Bộ", "Order")); updateState(LoadingStage.TAXONOMY) }
        taxonomy.family?.let { updated = updated.copy(family = format(it, "Họ", "Family")); updateState(LoadingStage.TAXONOMY) }
        taxonomy.genus?.let { updated = updated.copy(genus = format(it, "Chi", "Genus")); updateState(LoadingStage.TAXONOMY) }
        taxonomy.species?.let { updated = updated.copy(species = format(it, "Loài", "Species")); updateState(LoadingStage.TAXONOMY) }
    }

    private suspend fun updateDetailsUISync(
        details: DetailsResponse,
        isVietnamese: Boolean,
        currentInfo: SpeciesInfo,
        onStateUpdate: (EcoLensUiState) -> Unit
    ) = withContext(Dispatchers.Main) {
        var updated = currentInfo

        suspend fun updateState(stage: LoadingStage) {
            onStateUpdate(EcoLensUiState(isLoading = true, speciesInfo = updated, loadingStage = stage))
            delay(200)
        }

        details.description?.takeIf { it.isNotBlank() }?.let {
            updated = updated.copy(description = markdownProcessor.process(it, isVietnamese = isVietnamese))
            updateState(LoadingStage.DESCRIPTION)
        }

        val characteristicsText = when (val chars = details.characteristics) {
            is String -> chars
            is List<*> -> chars.joinToString("\n")
            else -> ""
        }
        if (characteristicsText.isNotBlank()) {
            updated = updated.copy(characteristics = markdownProcessor.process(characteristicsText, isVietnamese = isVietnamese))
            updateState(LoadingStage.CHARACTERISTICS)
        }

        details.distribution?.takeIf { it.isNotBlank() }?.let {
            updated = updated.copy(distribution = markdownProcessor.process(it, isVietnamese = isVietnamese))
            updateState(LoadingStage.DISTRIBUTION)
        }

        details.habitat?.takeIf { it.isNotBlank() }?.let {
            updated = updated.copy(habitat = markdownProcessor.process(it, isVietnamese = isVietnamese))
            updateState(LoadingStage.HABITAT)
        }

        details.conservationStatus?.takeIf { it.isNotBlank() }?.let {
            updated = updated.copy(conservationStatus = markdownProcessor.process(it, isConservationStatus = true, isVietnamese = isVietnamese))
            updateState(LoadingStage.CONSERVATION)
        }
    }

    private fun cleanJsonString(json: String): String {
        val firstBrace = json.indexOf('{')
        val lastBrace = json.lastIndexOf('}')
        return if (firstBrace != -1 && lastBrace > firstBrace) {
            json.substring(firstBrace, lastBrace + 1)
        } else {
            json.replace("```json", "", ignoreCase = true)
                .replace("```", "")
                .trim()
        }
    }

    data class TaxonomyResponse(
        val commonName: String? = null,
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
        val habitat: String? = null,
        val conservationStatus: String? = null
    )
}