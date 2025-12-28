package com.nguyendevs.ecolens.managers

import android.app.Application
import android.net.Uri
import com.google.gson.Gson
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.database.HistoryDao
import com.nguyendevs.ecolens.model.*
import com.nguyendevs.ecolens.network.RetrofitClient
import com.nguyendevs.ecolens.utils.ImageUtils
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

class SpeciesIdentificationManager(
    private val application: Application,
    private val historyDao: HistoryDao
) {
    private val apiService = RetrofitClient.iNaturalistApi
    private val gson = Gson()
    private val streamingHelper = GeminiStreamingHelper(apiService, gson)

    var currentImageUri: Uri? = null
    var currentHistoryEntryId: Int? = null
    var currentLanguageCode: String = "vi"

    private var currentSpeciesInfo: SpeciesInfo? = null

    suspend fun identifySpecies(
        imageUri: Uri,
        languageCode: String,
        existingHistoryId: Int?,
        onStateUpdate: (EcoLensUiState) -> Unit
    ) {
        currentLanguageCode = languageCode
        currentImageUri = imageUri
        currentHistoryEntryId = existingHistoryId

        onStateUpdate(EcoLensUiState(
            isLoading = true,
            loadingStage = LoadingStage.NONE
        ))
        delay(100)

        try {
            val imageFile = withContext(Dispatchers.Default) {
                ImageUtils.uriToFile(application, imageUri, 1024)
            }

            val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

            val response = apiService.identifySpecies(
                image = imagePart,
                locale = languageCode
            )

            if (response.results.isNotEmpty()) {
                val topResult = response.results.first()
                val scientificName = topResult.taxon.name
                val confidence = topResult.combined_score

                currentSpeciesInfo = SpeciesInfo(
                    scientificName = scientificName,
                    confidence = confidence,
                    commonName = "..."
                )

                onStateUpdate(EcoLensUiState(
                    isLoading = true,
                    speciesInfo = currentSpeciesInfo,
                    loadingStage = LoadingStage.SCIENTIFIC_NAME
                ))

                try {
                    streamingHelper.streamTaxonomy(
                        scientificName,
                        confidence,
                        languageCode
                    ) { state ->
                        currentSpeciesInfo = state.speciesInfo
                        onStateUpdate(state)
                    }

                    val infoForDetails = currentSpeciesInfo ?: SpeciesInfo(scientificName = scientificName, confidence = confidence)

                    streamingHelper.streamDetails(
                        scientificName,
                        confidence,
                        languageCode,
                        infoForDetails
                    ) { state ->
                        currentSpeciesInfo = state.speciesInfo
                        onStateUpdate(state)
                    }

                    onStateUpdate(EcoLensUiState(
                        isLoading = false,
                        speciesInfo = currentSpeciesInfo,
                        loadingStage = LoadingStage.COMPLETE
                    ))

                    saveToHistory(existingHistoryId, imageFile)

                } catch (e: GeoBlockedException) {
                    onStateUpdate(EcoLensUiState(
                        isLoading = false,
                        speciesInfo = null,
                        error = application.getString(R.string.error_geo_block)
                    ))
                    return
                } catch (e: Exception) {
                    handleError(e, onStateUpdate)
                    return
                }
            } else {
                onStateUpdate(EcoLensUiState(
                    isLoading = false,
                    error = application.getString(R.string.error_no_result)
                ))
            }
        } catch (e: Exception) {
            handleError(e, onStateUpdate)
        }
    }

    private suspend fun saveToHistory(existingHistoryId: Int?, imageFile: java.io.File) {
        val currentInfo = currentSpeciesInfo ?: return

        val isValidInfo = currentInfo.commonName.isNotEmpty() &&
                currentInfo.commonName != "..." &&
                currentInfo.commonName != "N/A" &&
                !currentInfo.description.contains("An error occurred", ignoreCase = true) &&
                !currentInfo.description.contains("Đã xảy ra lỗi", ignoreCase = true)

        if (isValidInfo) {
            withContext(Dispatchers.IO) {
                val savedPath = if (existingHistoryId != null) {
                    historyDao.getHistoryById(existingHistoryId)?.imagePath
                } else {
                    ImageUtils.saveBitmapToInternalStorage(application, imageFile)
                }

                if (savedPath != null) {
                    if (existingHistoryId != null) {
                        historyDao.updateSpeciesDetails(
                            id = existingHistoryId,
                            commonName = currentInfo.commonName,
                            scientificName = currentInfo.scientificName,
                            kingdom = currentInfo.kingdom,
                            phylum = currentInfo.phylum,
                            className = currentInfo.className,
                            taxorder = currentInfo.taxorder,
                            family = currentInfo.family,
                            genus = currentInfo.genus,
                            species = currentInfo.species,
                            description = currentInfo.description,
                            characteristics = currentInfo.characteristics,
                            distribution = currentInfo.distribution,
                            habitat = currentInfo.habitat,
                            conservationStatus = currentInfo.conservationStatus,
                            confidence = currentInfo.confidence,
                            timestamp = System.currentTimeMillis()
                        )
                        currentHistoryEntryId = existingHistoryId
                    } else {
                        val newId = historyDao.insert(HistoryEntry(
                            imagePath = savedPath,
                            speciesInfo = currentInfo,
                            timestamp = System.currentTimeMillis()
                        ))
                        currentHistoryEntryId = newId.toInt()
                    }
                }
            }
        }
    }

    private fun handleError(e: Exception, onStateUpdate: (EcoLensUiState) -> Unit) {
        val errorMsg = when {
            e.message?.contains("429") == true ->
                application.getString(R.string.error_quota_exceeded)
            else ->
                application.getString(R.string.error_general, e.message)
        }
        onStateUpdate(EcoLensUiState(isLoading = false, error = errorMsg))
    }
}