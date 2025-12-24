package com.nguyendevs.ecolens.managers

import android.util.Log
import com.google.gson.Gson
import com.nguyendevs.ecolens.api.*
import com.nguyendevs.ecolens.model.*
import com.nguyendevs.ecolens.utils.MarkdownProcessor
import com.nguyendevs.ecolens.utils.PromptBuilder
import kotlinx.coroutines.*

class GeminiStreamingHelper(
    private val apiService: INaturalistApi,
    private val gson: Gson
) {
    private val markdownProcessor = MarkdownProcessor()

    suspend fun streamTaxonomy(
        scientificName: String,
        confidence: Double,
        languageCode: String,
        onStateUpdate: (EcoLensUiState) -> Unit
    ) = withContext(Dispatchers.IO) {
        val isVietnamese = languageCode != "en"
        val prompt = PromptBuilder.buildTaxonomyPrompt(scientificName, isVietnamese)

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = prompt))
                )
            )
        )

        try {
            val response = apiService.streamGemini(request)
            if (!response.isSuccessful) return@withContext

            response.body()?.byteStream()?.bufferedReader()?.use { reader ->
                var accumulatedJson = ""
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue

                    if (currentLine.startsWith("data: ")) {
                        val jsonData = currentLine.substring(6).trim()
                        if (jsonData == "[DONE]") break

                        try {
                            val streamResponse = gson.fromJson(jsonData, GeminiResponse::class.java)
                            val chunk = streamResponse.candidates?.firstOrNull()
                                ?.content?.parts?.firstOrNull()?.text

                            if (!chunk.isNullOrEmpty()) {
                                accumulatedJson += chunk

                                val cleanedJson = cleanJsonString(accumulatedJson)
                                try {
                                    val taxonomyInfo = gson.fromJson(
                                        cleanedJson,
                                        TaxonomyResponse::class.java
                                    )
                                    updateTaxonomyUISync(taxonomyInfo, scientificName, isVietnamese, confidence, onStateUpdate)
                                } catch (e: Exception) {
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("StreamTaxonomy", "Parse error: ${e.message}")
                        }
                    }
                }

                if (accumulatedJson.isNotBlank()) {
                    try {
                        val cleanedJson = cleanJsonString(accumulatedJson)
                        val taxonomyInfo = gson.fromJson(
                            cleanedJson,
                            TaxonomyResponse::class.java
                        )
                        updateTaxonomyUISync(taxonomyInfo, scientificName, isVietnamese, confidence, onStateUpdate)
                    } catch (e: Exception) {
                        Log.e("StreamTaxonomy", "Final parse error: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("StreamTaxonomy", "Error: ${e.message}")
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

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = prompt))
                )
            )
        )

        try {
            val response = apiService.streamGemini(request)

            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    var accumulatedJson = ""

                    responseBody.byteStream().bufferedReader().use { reader ->
                        var line: String?

                        while (reader.readLine().also { line = it } != null) {
                            val currentLine = line ?: continue

                            if (currentLine.startsWith("data: ")) {
                                val jsonData = currentLine.substring(6).trim()
                                if (jsonData == "[DONE]") break

                                try {
                                    val streamResponse = gson.fromJson(jsonData, GeminiResponse::class.java)
                                    val chunk = streamResponse.candidates?.firstOrNull()
                                        ?.content?.parts?.firstOrNull()?.text

                                    if (!chunk.isNullOrEmpty()) {
                                        accumulatedJson += chunk

                                        val cleanedJson = cleanJsonString(accumulatedJson)
                                        try {
                                            val detailsInfo = gson.fromJson(
                                                cleanedJson,
                                                DetailsResponse::class.java
                                            )
                                            updateDetailsUISync(detailsInfo, isVietnamese, currentInfo, onStateUpdate)
                                        } catch (e: Exception) {
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("StreamDetails", "Parse error: ${e.message}")
                                }
                            }
                        }

                        if (accumulatedJson.isNotBlank()) {
                            try {
                                val cleanedJson = cleanJsonString(accumulatedJson)
                                val detailsInfo = gson.fromJson(
                                    cleanedJson,
                                    DetailsResponse::class.java
                                )
                                updateDetailsUISync(detailsInfo, isVietnamese, currentInfo, onStateUpdate)
                            } catch (e: Exception) {
                                Log.e("StreamDetails", "Final parse error: ${e.message}")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("StreamDetails", "Error: ${e.message}")
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

        if (!taxonomy.commonName.isNullOrBlank() && taxonomy.commonName != "...") {
            updated = updated.copy(commonName = taxonomy.commonName)
            onStateUpdate(EcoLensUiState(
                isLoading = true,
                speciesInfo = updated,
                loadingStage = LoadingStage.COMMON_NAME
            ))
            delay(100)
        }

        var hasUpdates = false

        taxonomy.kingdom?.let {
            updated = updated.copy(
                kingdom = "<b>${markdownProcessor.removeRankPrefix(it, if (isVietnamese) "Giới" else "Kingdom")}</b>"
            )
            hasUpdates = true
        }

        taxonomy.phylum?.let {
            updated = updated.copy(
                phylum = "<b>${markdownProcessor.removeRankPrefix(it, if (isVietnamese) "Ngành" else "Phylum")}</b>"
            )
            hasUpdates = true
        }

        taxonomy.className?.let {
            updated = updated.copy(
                className = "<b>${markdownProcessor.removeRankPrefix(it, if (isVietnamese) "Lớp" else "Class")}</b>"
            )
            hasUpdates = true
        }

        taxonomy.taxorder?.let {
            updated = updated.copy(
                taxorder = "<b>${markdownProcessor.removeRankPrefix(it, if (isVietnamese) "Bộ" else "Order")}</b>"
            )
            hasUpdates = true
        }

        taxonomy.family?.let {
            updated = updated.copy(
                family = "<b>${markdownProcessor.removeRankPrefix(it, if (isVietnamese) "Họ" else "Family")}</b>"
            )
            hasUpdates = true
        }

        taxonomy.genus?.let {
            updated = updated.copy(
                genus = "<b>${markdownProcessor.removeRankPrefix(it, if (isVietnamese) "Chi" else "Genus")}</b>"
            )
            hasUpdates = true
        }

        taxonomy.species?.let {
            updated = updated.copy(
                species = "<b>${markdownProcessor.removeRankPrefix(it, if (isVietnamese) "Loài" else "Species")}</b>"
            )
            hasUpdates = true
        }

        if (hasUpdates) {
            onStateUpdate(EcoLensUiState(
                isLoading = true,
                speciesInfo = updated,
                loadingStage = LoadingStage.TAXONOMY
            ))
        }
    }

    private suspend fun updateDetailsUISync(
        details: DetailsResponse,
        isVietnamese: Boolean,
        currentInfo: SpeciesInfo,
        onStateUpdate: (EcoLensUiState) -> Unit
    ) = withContext(Dispatchers.Main) {
        var updated = currentInfo

        val characteristicsText = when (val chars = details.characteristics) {
            is String -> chars
            is List<*> -> chars.joinToString("\n")
            else -> ""
        }

        details.description?.let { desc ->
            if (desc.isNotBlank()) {
                updated = updated.copy(
                    description = markdownProcessor.process(desc, isVietnamese = isVietnamese)
                )
                onStateUpdate(EcoLensUiState(
                    isLoading = true,
                    speciesInfo = updated,
                    loadingStage = LoadingStage.DESCRIPTION
                ))
            }
        }

        if (characteristicsText.isNotBlank()) {
            updated = updated.copy(
                characteristics = markdownProcessor.process(characteristicsText, isVietnamese = isVietnamese)
            )
            onStateUpdate(EcoLensUiState(
                isLoading = true,
                speciesInfo = updated,
                loadingStage = LoadingStage.CHARACTERISTICS
            ))
        }

        details.distribution?.let { dist ->
            if (dist.isNotBlank()) {
                updated = updated.copy(
                    distribution = markdownProcessor.process(dist, isVietnamese = isVietnamese)
                )
                onStateUpdate(EcoLensUiState(
                    isLoading = true,
                    speciesInfo = updated,
                    loadingStage = LoadingStage.DISTRIBUTION
                ))
            }
        }

        details.habitat?.let { hab ->
            if (hab.isNotBlank()) {
                updated = updated.copy(
                    habitat = markdownProcessor.process(hab, isVietnamese = isVietnamese)
                )
                onStateUpdate(EcoLensUiState(
                    isLoading = true,
                    speciesInfo = updated,
                    loadingStage = LoadingStage.HABITAT
                ))
            }
        }

        details.conservationStatus?.let { status ->
            if (status.isNotBlank()) {
                updated = updated.copy(
                    conservationStatus = markdownProcessor.process(
                        status,
                        isConservationStatus = true,
                        isVietnamese = isVietnamese
                    )
                )
                onStateUpdate(EcoLensUiState(
                    isLoading = true,
                    speciesInfo = updated,
                    loadingStage = LoadingStage.CONSERVATION
                ))
            }
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