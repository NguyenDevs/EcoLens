package com.nguyendevs.ecolens.managers

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.database.HistoryRepository
import com.nguyendevs.ecolens.model.*
import com.nguyendevs.ecolens.network.RetrofitClient
import com.nguyendevs.ecolens.utils.ImageUtils
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class SpeciesIdentificationManager(
    private val context: Context,
    private val historyRepository: HistoryRepository
) {
    private val apiService = RetrofitClient.iNaturalistApi
    private val gson by lazy { Gson() }
    private val streamingHelper by lazy { GeminiStreamingHelper(apiService, gson) }

    companion object {
        private const val TAG = "SpeciesIdManager"
        private const val MIME_TYPE_JPEG = "image/jpeg"
        private const val PART_NAME_IMAGE = "image"
        private const val MAX_IMAGE_SIZE = 1024
        private const val DELAY_INIT = 100L
    }

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
        delay(DELAY_INIT)

        try {
            val imageFile = withContext(Dispatchers.Default) {
                ImageUtils.uriToFile(context, imageUri, MAX_IMAGE_SIZE)
            }

            val imagePart = createImagePart(imageFile)

            val response = apiService.identifySpecies(
                image = imagePart,
                locale = languageCode
            )

            val topResult = response.results.firstOrNull()
            if (topResult != null) {
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
                        error = context.getString(R.string.error_geo_block)
                    ))
                    return
                } catch (e: Exception) {
                    Log.e(TAG, "Streaming error: ${e.message}", e)
                    handleError(e, onStateUpdate)
                    return
                }
            } else {
                onStateUpdate(EcoLensUiState(
                    isLoading = false,
                    error = context.getString(R.string.error_no_result)
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Identification error: ${e.message}", e)
            handleError(e, onStateUpdate)
        }
    }

    private fun createImagePart(file: File): MultipartBody.Part {
        val requestFile = file.asRequestBody(MIME_TYPE_JPEG.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(PART_NAME_IMAGE, file.name, requestFile)
    }

    private suspend fun saveToHistory(existingHistoryId: Int?, imageFile: File) {
        val currentInfo = currentSpeciesInfo ?: return

        if (isValidInfo(currentInfo)) {
            withContext(Dispatchers.IO) {
                val savedPath = ImageUtils.saveImageToPublicStorage(context, imageFile)
                
                if (savedPath != null) {
                    val entry = HistoryEntry(
                        id = existingHistoryId ?: 0,
                        imagePath = savedPath,
                        localImagePath = savedPath,
                        speciesInfo = currentInfo,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    if (existingHistoryId != null) {
                        historyRepository.update(entry)
                    } else {
                        val newId = historyRepository.insert(entry)
                        currentHistoryEntryId = newId.toInt()
                    }
                }
            }
        }
    }

    private fun isValidInfo(info: SpeciesInfo): Boolean {
        return info.commonName.isNotEmpty() &&
                info.commonName != "..." &&
                info.commonName != "N/A" &&
                !info.description.contains("An error occurred", ignoreCase = true) &&
                !info.description.contains("Đã xảy ra lỗi", ignoreCase = true)
    }

    private fun handleError(e: Exception, onStateUpdate: (EcoLensUiState) -> Unit) {
        val errorMsg = when {
            e.message?.contains("429") == true ->
                context.getString(R.string.error_quota_exceeded)
            else ->
                context.getString(R.string.error_general, e.message)
        }
        onStateUpdate(EcoLensUiState(isLoading = false, error = errorMsg))
    }
}