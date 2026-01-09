package com.nguyendevs.ecolens.managers.gemini

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.api.IdentificationResult
import com.nguyendevs.ecolens.database.HistoryRepository
import com.nguyendevs.ecolens.model.*
import com.nguyendevs.ecolens.model.history.HistoryEntry
import com.nguyendevs.ecolens.network.RetrofitClient
import com.nguyendevs.ecolens.utils.ImageUtils
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileNotFoundException

/**
 * Manager xử lý nhận diện loài từ ảnh
 * Flow: iNaturalist API → Gemini streaming (taxonomy + details) → Save to history
 * Hỗ trợ update existing history entry hoặc tạo mới
 */
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

    // ==================== IDENTIFICATION FLOW ====================

    /**
     * Nhận diện loài từ ảnh
     * Flow: Prepare image → iNaturalist API → Gemini streaming → Save history
     *
     * @param imageUri URI của ảnh cần nhận diện
     * @param languageCode Ngôn ngữ cho kết quả (vi/en)
     * @param existingHistoryId ID của history entry cần update (null nếu tạo mới)
     * @param onStateUpdate Callback để update UI state
     */
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
            val imageFile = prepareImageFile(imageUri)
            val topResult = callINaturalistAPI(imageFile, languageCode)

            if (topResult != null) {
                processIdentificationResult(
                    topResult,
                    languageCode,
                    existingHistoryId,
                    imageFile,
                    onStateUpdate
                )
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

    // ==================== IMAGE PREPARATION ====================

    /**
     * Chuẩn bị image file từ URI
     * Resize về max 1024px và validate file existence
     */
    private suspend fun prepareImageFile(imageUri: Uri): File {
        val imageFile = withContext(Dispatchers.Default) {
            ImageUtils.uriToFile(context, imageUri, MAX_IMAGE_SIZE)
        }

        if (!imageFile.exists()) {
            throw FileNotFoundException(
                "Image file could not be created or found: ${imageFile.absolutePath}"
            )
        }

        return imageFile
    }

    /**
     * Tạo MultipartBody.Part từ image file
     */
    private fun createImagePart(file: File): MultipartBody.Part {
        val requestFile = file.asRequestBody(MIME_TYPE_JPEG.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(PART_NAME_IMAGE, file.name, requestFile)
    }

    // ==================== API CALLS ====================

    /**
     * Gọi iNaturalist API để nhận diện loài
     * @return Top result hoặc null nếu không có kết quả
     */
    private suspend fun callINaturalistAPI(
        imageFile: File,
        languageCode: String
    ): IdentificationResult? {
        val imagePart = createImagePart(imageFile)
        val response = apiService.identifySpecies(
            image = imagePart,
            locale = languageCode
        )
        return response.results.firstOrNull()
    }

    // ==================== RESULT PROCESSING ====================

    /**
     * Xử lý kết quả nhận diện
     * Stream taxonomy và details từ Gemini, sau đó save vào history
     */
    private suspend fun processIdentificationResult(
        result: IdentificationResult,
        languageCode: String,
        existingHistoryId: Int?,
        imageFile: File,
        onStateUpdate: (EcoLensUiState) -> Unit
    ) {
        val scientificName = result.taxon.name
        val confidence = result.combined_score

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
            streamTaxonomyAndDetails(scientificName, confidence, languageCode, onStateUpdate)
            saveToHistory(existingHistoryId, imageFile)

            onStateUpdate(EcoLensUiState(
                isLoading = false,
                speciesInfo = currentSpeciesInfo,
                loadingStage = LoadingStage.COMPLETE
            ))

        } catch (e: GeoBlockedException) {
            onStateUpdate(EcoLensUiState(
                isLoading = false,
                speciesInfo = null,
                error = context.getString(R.string.error_geo_block)
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Streaming error: ${e.message}", e)
            handleError(e, onStateUpdate)
        }
    }

    /**
     * Stream taxonomy và details từ Gemini API
     */
    private suspend fun streamTaxonomyAndDetails(
        scientificName: String,
        confidence: Double,
        languageCode: String,
        onStateUpdate: (EcoLensUiState) -> Unit
    ) {
        streamingHelper.streamTaxonomy(
            scientificName,
            confidence,
            languageCode
        ) { state ->
            currentSpeciesInfo = state.speciesInfo
            onStateUpdate(state)
        }

        val infoForDetails = currentSpeciesInfo ?: SpeciesInfo(
            scientificName = scientificName,
            confidence = confidence
        )

        streamingHelper.streamDetails(
            scientificName,
            confidence,
            languageCode,
            infoForDetails
        ) { state ->
            currentSpeciesInfo = state.speciesInfo
            onStateUpdate(state)
        }
    }

    // ==================== HISTORY MANAGEMENT ====================

    /**
     * Lưu kết quả vào history
     * Update existing entry nếu có, nếu không tạo mới
     */
    private suspend fun saveToHistory(existingHistoryId: Int?, imageFile: File) {
        val currentInfo = currentSpeciesInfo ?: return

        if (isValidInfo(currentInfo)) {
            withContext(Dispatchers.IO) {
                if (existingHistoryId != null) {
                    updateExistingHistory(existingHistoryId, currentInfo)
                } else {
                    createNewHistory(imageFile, currentInfo)
                }
            }
        }
    }

    /**
     * Update existing history entry
     */
    private suspend fun updateExistingHistory(historyId: Int, info: SpeciesInfo) {
        val existingEntry = historyRepository.getHistoryById(historyId)
        if (existingEntry != null) {
            val entry = existingEntry.copy(speciesInfo = info)
            Log.d(TAG, "Updating history entry: ${entry.id} - ${entry.speciesInfo.commonName}")
            historyRepository.update(entry)
        }
    }

    /**
     * Tạo history entry mới
     */
    private suspend fun createNewHistory(imageFile: File, info: SpeciesInfo) {
        val localImagePath = if (currentImageUri != null && currentImageUri!!.scheme == "file") {
            currentImageUri!!.path
        } else {
            ImageUtils.saveFileToInternalStorage(context, imageFile)
        }

        if (localImagePath != null) {
            val entry = HistoryEntry(
                id = 0,
                imagePath = localImagePath,
                localImagePath = localImagePath,
                speciesInfo = info,
                timestamp = System.currentTimeMillis(),
                isFavorite = false
            )

            Log.d(TAG, "Inserting new history entry")
            val newId = historyRepository.insert(entry)
            currentHistoryEntryId = newId.toInt()
        }
    }

    /**
     * Validate xem info có hợp lệ để save không
     */
    private fun isValidInfo(info: SpeciesInfo): Boolean {
        return info.commonName.isNotEmpty() &&
                info.commonName != "..." &&
                info.commonName != "N/A" &&
                !info.description.contains("An error occurred", ignoreCase = true) &&
                !info.description.contains("Đã xảy ra lỗi", ignoreCase = true)
    }

    // ==================== ERROR HANDLING ====================

    /**
     * Xử lý lỗi và trả về error message phù hợp
     */
    private fun handleError(e: Exception, onStateUpdate: (EcoLensUiState) -> Unit) {
        val errorMsg = when {
            e.message?.contains("429") == true ->
                context.getString(R.string.error_quota_exceeded)
            e is FileNotFoundException ->
                context.getString(R.string.error_file_not_found)
            else ->
                context.getString(R.string.error_general, e.message)
        }
        onStateUpdate(EcoLensUiState(isLoading = false, error = errorMsg))
    }
}