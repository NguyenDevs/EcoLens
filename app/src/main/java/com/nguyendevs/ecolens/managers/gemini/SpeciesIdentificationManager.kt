package com.nguyendevs.ecolens.managers.gemini

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.api.GbifResponse
import com.nguyendevs.ecolens.api.IdentificationResult
import com.nguyendevs.ecolens.database.HistoryRepository
import com.nguyendevs.ecolens.managers.setting.LanguageManager
import com.nguyendevs.ecolens.models.*
import com.nguyendevs.ecolens.models.history.HistoryEntry
import com.nguyendevs.ecolens.network.RetrofitClient
import com.nguyendevs.ecolens.utils.ImageUtils
import com.nguyendevs.ecolens.utils.MarkdownProcessor
import java.io.File
import java.io.FileNotFoundException
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

/** Quản lý quá trình nhận diện và xử lý luồng dữ liệu phân loại sinh vật qua API. */
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

    /** Đẩy hình ảnh qua mô hình để chạy phân tích và đồng bộ các luồng thông tin thu được. */
    suspend fun identifySpecies(
            imageUri: Uri,
            languageCode: String,
            existingHistoryId: Int?,
            lat: Double = 16.0544,
            lng: Double = 108.2022,
            onStateUpdate: (EcoLensUiState) -> Unit
    ) {
        currentLanguageCode = languageCode
        currentImageUri = imageUri
        currentHistoryEntryId = existingHistoryId

        onStateUpdate(EcoLensUiState(isLoading = true, loadingStage = LoadingStage.NONE))
        delay(DELAY_INIT)

        try {
            val imageFile = prepareImageFile(imageUri)
            val topResult = callINaturalistAPI(imageFile, languageCode, lat, lng)

            if (topResult != null) {
                processIdentificationResult(
                        topResult,
                        languageCode,
                        existingHistoryId,
                        imageFile,
                        onStateUpdate
                )
            } else {
                onStateUpdate(
                        EcoLensUiState(
                                isLoading = false,
                                error = getLocalizedString(R.string.error_no_result)
                        )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Identification error: ${e.message}", e)
            handleError(e, onStateUpdate)
        }
    }

    /** Chuyển đổi và định cỡ lại bức ảnh từ URI xuống kích cỡ hợp lệ. */
    private suspend fun prepareImageFile(imageUri: Uri): File {
        val imageFile =
                withContext(Dispatchers.Default) {
                    ImageUtils.uriToFile(context, imageUri, MAX_IMAGE_SIZE)
                }

        if (!imageFile.exists()) {
            throw FileNotFoundException(
                    "Image file could not be created or found: ${imageFile.absolutePath}"
            )
        }

        return imageFile
    }

    /** Chuyển hóa tệp bức ảnh thành phần cấu thành dạng thức multipart. */
    private fun createImagePart(file: File): MultipartBody.Part {
        val requestFile = file.asRequestBody(MIME_TYPE_JPEG.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(PART_NAME_IMAGE, file.name, requestFile)
    }

    /** Gọi iNaturalist API nhằm xác định ra loài sinh vật có mức tin cậy cao nhất. */
    private suspend fun callINaturalistAPI(
            imageFile: File,
            languageCode: String,
            lat: Double = 16.0544,
            lng: Double = 108.2022
    ): IdentificationResult? {
        val imagePart = createImagePart(imageFile)
        val response = apiService.identifySpecies(
                image = imagePart,
                lat = lat,
                lng = lng,
                locale = languageCode
        )
        return response.results.firstOrNull()
    }

    /** Phát luồng lấy chi tiết GBIF, phân bố bổ sung và kiểm tra cấp độ bảo tồn. */
    private suspend fun processIdentificationResult(
            result: IdentificationResult,
            languageCode: String,
            existingHistoryId: Int?,
            imageFile: File,
            onStateUpdate: (EcoLensUiState) -> Unit
    ) {
        val scientificName = result.taxon.name
        val confidence = result.combined_score

        val sharedPref = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isIucnEnabled = sharedPref.getBoolean("iucn_mode", true)
        val isVnRedListEnabled = sharedPref.getBoolean("vnredlist_mode", true)
        val isTaxoModeEnabled = sharedPref.getBoolean("taxo_mode", false)

        currentSpeciesInfo =
                SpeciesInfo(
                        scientificName = scientificName,
                        confidence = confidence,
                        commonName = "...",
                        iucn = isIucnEnabled,
                        vnredlist = isVnRedListEnabled,
                        conservationStatus = if (isIucnEnabled) getLocalizedString(R.string.searching_iucn) else "Vô hiệu",
                        vnredlistStatus = if (isVnRedListEnabled) getLocalizedString(R.string.searching_vnredlist) else "Vô hiệu"
                )

        onStateUpdate(
                EcoLensUiState(
                        isLoading = true,
                        speciesInfo = currentSpeciesInfo,
                        loadingStage = LoadingStage.SCIENTIFIC_NAME
                )
        )

        try {
            val commonName = streamingHelper.getCommonName(scientificName, languageCode)
            if (!commonName.isNullOrEmpty()) {
                currentSpeciesInfo = currentSpeciesInfo?.copy(commonName = commonName)
                onStateUpdate(
                        EcoLensUiState(
                                isLoading = true,
                                speciesInfo = currentSpeciesInfo,
                                loadingStage = LoadingStage.COMMON_NAME
                        )
                )
            }

            coroutineScope {
                val gbifDeferred =
                        async(Dispatchers.IO) {
                            try {
                                val url =
                                        "https://api.gbif.org/v1/species/match?name=$scientificName"
                                apiService.getGbifTaxonomy(url)
                            } catch (e: Exception) {
                                Log.e(TAG, "GBIF Error: ${e.message}")
                                null
                            }
                        }

                val iucnDeferred =
                        if (isIucnEnabled) {
                            async(Dispatchers.IO) {
                                try {
                                    val parts = scientificName.split(" ")
                                    if (parts.size >= 2) {
                                        apiService.getIucnStatus(parts[0], parts[1])
                                    } else {
                                        null
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "IUCN Error: ${e.message}")
                                    null
                                }
                            }
                        } else {
                            null
                        }

                val vnredlistDeferred =
                        if (isVnRedListEnabled) {
                            async(Dispatchers.IO) {
                                try {
                                    val slug = scientificName.trim().lowercase().replace(" ", "-")
                                    val url = "http://vnredlist.vast.vn/$slug/"
                                    val doc = org.jsoup.Jsoup.connect(url).get()
                                    val result = mutableMapOf<String, String>()
                                    doc.select("h3.cust_title").forEach { title ->
                                        val key = title.text().trim()
                                        val value = title.nextElementSibling()?.text()?.trim()
                                        if (value != null) result[key] = value
                                    }
                                    VnRedListScrapeResult(
                                        phanHangBaoTon = result["Phân hạng bảo tồn"] ?: result["Phân hạng"],
                                        tieuChuanDanhGia = result["Tiêu chuẩn đánh giá"],
                                        dienGiaiDanhGia = result["Diễn giải đánh giá theo các tiêu chuẩn"],
                                        namCongBo = result["Năm công bố"]
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "VN Red List Error: ${e.message}")
                                    null
                                }
                            }
                        } else {
                            null
                        }

                val infoForDetails =
                        currentSpeciesInfo
                                ?: SpeciesInfo(
                                        scientificName = scientificName,
                                        confidence = confidence,
                                        iucn = isIucnEnabled,
                                        vnredlist = isVnRedListEnabled
                                )

                val geminiDetailsDeferred =
                        async(Dispatchers.IO) {
                            streamingHelper.streamDetails(
                                    scientificName,
                                    confidence,
                                    languageCode,
                                    infoForDetails
                            ) { state ->
                                val incomingInfo = state.speciesInfo
                                val current = currentSpeciesInfo

                                val mergedInfo =
                                        if (incomingInfo != null && current != null) {
                                            incomingInfo.copy(
                                                    kingdom = current.kingdom,
                                                    phylum = current.phylum,
                                                    className = current.className,
                                                    taxorder = current.taxorder,
                                                    family = current.family,
                                                    genus = current.genus,
                                                    species = current.species,
                                                    iucn = isIucnEnabled,
                                                    vnredlist = isVnRedListEnabled
                                            )
                                        } else {
                                            incomingInfo
                                        }

                                currentSpeciesInfo = mergedInfo
                                onStateUpdate(
                                        state.copy(speciesInfo = mergedInfo, images = state.images)
                                )
                            }
                        }

                val imagesDeferred =
                        async(Dispatchers.IO) {
                            try {
                                val detailResponse = apiService.getTaxonDetails(result.taxon.id)
                                val photos =
                                        detailResponse.results.firstOrNull()?.taxon_photos
                                                ?: emptyList()
                                photos.mapNotNull { it.photo.large_url ?: it.photo.medium_url }
                            } catch (e: Exception) {
                                Log.e(TAG, "Fetch photos error: ${e.message}")
                                emptyList<String>()
                            }
                        }

                val gbifResult = gbifDeferred.await()
                val iucnResult = iucnDeferred?.await()
                val vnredlistResult = vnredlistDeferred?.await()
                val fetchedImages = imagesDeferred.await()

                if (fetchedImages.isNotEmpty()) {
                    onStateUpdate(
                            EcoLensUiState(
                                    isLoading = true,
                                    speciesInfo = currentSpeciesInfo,
                                    loadingStage = LoadingStage.TAXONOMY,
                                    images = fetchedImages
                            )
                    )
                }

                if (gbifResult != null) {
                    updateTaxonomyFromGbif(gbifResult, onStateUpdate)
                }

                if (isTaxoModeEnabled &&
                                gbifResult != null &&
                                languageCode == LanguageManager.LANG_VI
                ) {
                    onStateUpdate(
                            EcoLensUiState(
                                    isLoading = true,
                                    speciesInfo = currentSpeciesInfo,
                                    loadingStage = LoadingStage.TAXONOMY,
                                    isTaxonomyTranslating = true
                            )
                    )

                    val translatedTaxonomy =
                            streamingHelper.translateTaxonomy(
                                    kingdom = gbifResult.kingdom ?: "",
                                    phylum = gbifResult.phylum ?: "",
                                    className = gbifResult.className ?: "",
                                    taxorder = gbifResult.taxorder ?: "",
                                    family = gbifResult.family ?: "",
                                    genus = gbifResult.genus ?: "",
                                    species = gbifResult.species ?: ""
                            )

                    if (translatedTaxonomy != null) {
                        updateTaxonomyFromTranslation(translatedTaxonomy, onStateUpdate)
                    }

                    onStateUpdate(
                            EcoLensUiState(
                                    isLoading = true,
                                    speciesInfo = currentSpeciesInfo,
                                    loadingStage = LoadingStage.TAXONOMY,
                                    isTaxonomyTranslating = false
                            )
                    )
                }

                geminiDetailsDeferred.await()

                if (isIucnEnabled) {
                    val iucnCode =
                            iucnResult?.assessments?.firstOrNull()?.redListCategoryCode ?: "NE"

                    val searchingText = getLocalizedString(R.string.searching_iucn)
                    val vnSearchingText = getLocalizedString(R.string.searching_vnredlist)
                    currentSpeciesInfo =
                            currentSpeciesInfo?.copy(
                                conservationStatus = searchingText,
                                vnredlistStatus = if (isVnRedListEnabled) vnSearchingText else "Vô hiệu"
                            )
                    onStateUpdate(
                            EcoLensUiState(
                                    isLoading = true,
                                    speciesInfo = currentSpeciesInfo,
                                    loadingStage = LoadingStage.CONSERVATION,
                                    images = fetchedImages
                            )
                    )

                    val infoForConservation =
                            currentSpeciesInfo
                                    ?: SpeciesInfo(
                                            scientificName = scientificName,
                                            confidence = confidence,
                                            iucn = true,
                                            vnredlist = isVnRedListEnabled
                                    )

                    streamingHelper.streamConservation(
                            scientificName,
                            iucnCode,
                            languageCode,
                            infoForConservation
                    ) { state ->
                        currentSpeciesInfo = state.speciesInfo
                        onStateUpdate(state.copy(images = fetchedImages))
                    }
                } else {
                    currentSpeciesInfo = currentSpeciesInfo?.copy(conservationStatus = "Vô hiệu")
                }

                if (isVnRedListEnabled) {
                    if (vnredlistResult != null) {
                        val buildStatus = buildString {
                            if (!vnredlistResult.phanHangBaoTon.isNullOrEmpty()) append("• <b>Phân hạng:</b> ${vnredlistResult.phanHangBaoTon}<br>")
                            if (!vnredlistResult.tieuChuanDanhGia.isNullOrEmpty()) append("• <b>Tiêu chuẩn:</b> ${vnredlistResult.tieuChuanDanhGia}<br>")
                            if (!vnredlistResult.dienGiaiDanhGia.isNullOrEmpty()) append("• <b>Diễn giải:</b> ${vnredlistResult.dienGiaiDanhGia}<br>")
                            if (!vnredlistResult.namCongBo.isNullOrEmpty()) append("• <b>Năm công bố:</b> ${vnredlistResult.namCongBo}<br>")
                        }
                        
                        val processedStatus = MarkdownProcessor().process(
                            text = buildStatus,
                            isConservationStatus = true,
                            isVietnamese = true // VN Red List luôn trả về tiếng Việt
                        )
                        
                        val statusText = if (processedStatus.isNotEmpty()) processedStatus else "Không có dữ liệu"
                        currentSpeciesInfo = currentSpeciesInfo?.copy(vnredlistStatus = statusText)
                    } else {
                        currentSpeciesInfo = currentSpeciesInfo?.copy(vnredlistStatus = "Không có dữ liệu")
                    }
                } else {
                    currentSpeciesInfo = currentSpeciesInfo?.copy(vnredlistStatus = "Vô hiệu")
                }

                saveToHistory(existingHistoryId, imageFile, languageCode)

                onStateUpdate(
                        EcoLensUiState(
                                isLoading = false,
                                speciesInfo = currentSpeciesInfo,
                                loadingStage = LoadingStage.COMPLETE,
                                images = fetchedImages,
                                historyId = currentHistoryEntryId,
                                isFavorite = false
                        )
                )
            }
        } catch (e: GeoBlockedException) {
            onStateUpdate(
                    EcoLensUiState(
                            isLoading = false,
                            speciesInfo = null,
                            error = getLocalizedString(R.string.error_geo_block)
                    )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Streaming error: ${e.message}", e)
            handleError(e, onStateUpdate)
        }
    }

    private suspend fun updateTaxonomyFromGbif(
            gbif: GbifResponse,
            onStateUpdate: (EcoLensUiState) -> Unit
    ) =
            withContext(Dispatchers.Main) {
                fun format(value: String?): String {
                    return if (value != null) "<b>$value</b>" else ""
                }

                suspend fun updateAndDelay(block: (SpeciesInfo) -> SpeciesInfo) {
                    val current = currentSpeciesInfo ?: return
                    val updated = block(current)

                    if (updated != current) {
                        currentSpeciesInfo = updated
                        onStateUpdate(
                                EcoLensUiState(
                                        isLoading = true,
                                        speciesInfo = updated,
                                        loadingStage = LoadingStage.TAXONOMY
                                )
                        )
                        delay(100)
                    }
                }

                if (gbif.kingdom != null) updateAndDelay { it.copy(kingdom = format(gbif.kingdom)) }
                if (gbif.phylum != null) updateAndDelay { it.copy(phylum = format(gbif.phylum)) }
                if (gbif.className != null)
                        updateAndDelay { it.copy(className = format(gbif.className)) }
                if (gbif.taxorder != null)
                        updateAndDelay { it.copy(taxorder = format(gbif.taxorder)) }
                if (gbif.family != null) updateAndDelay { it.copy(family = format(gbif.family)) }
                if (gbif.genus != null) updateAndDelay { it.copy(genus = format(gbif.genus)) }
                if (gbif.species != null) updateAndDelay { it.copy(species = format(gbif.species)) }
            }

    private suspend fun updateTaxonomyFromTranslation(
            translation: GeminiStreamingHelper.TaxonomyTranslationResponse,
            onStateUpdate: (EcoLensUiState) -> Unit
    ) =
            withContext(Dispatchers.Main) {
                fun format(value: String?): String {
                    if (value == null) return ""

                    var cleaned = value.trim()
                    val prefixes = listOf("Giới ", "Ngành ", "Lớp ", "Bộ ", "Họ ", "Chi ", "Loài ")
                    for (prefix in prefixes) {
                        if (cleaned.startsWith(prefix, ignoreCase = true)) {
                            cleaned = cleaned.substring(prefix.length).trim()
                        }
                    }

                    return "<b>$cleaned</b>"
                }

                val current = currentSpeciesInfo ?: return@withContext
                val updated =
                        current.copy(
                                kingdom = format(translation.kingdom),
                                phylum = format(translation.phylum),
                                className = format(translation.className),
                                taxorder = format(translation.taxorder),
                                family = format(translation.family),
                                genus = format(translation.genus),
                                species = format(translation.species)
                        )

                currentSpeciesInfo = updated
                onStateUpdate(
                        EcoLensUiState(
                                isLoading = true,
                                speciesInfo = updated,
                                loadingStage = LoadingStage.TAXONOMY
                        )
                )
            }

    /** Ghi log lại cấu trúc loài sinh vật đã duyệt xong xuống cơ sở dữ liệu. */
    private suspend fun saveToHistory(
            existingHistoryId: Int?,
            imageFile: File,
            languageCode: String
    ) {
        val currentInfo = currentSpeciesInfo ?: return

        if (isValidInfo(currentInfo)) {
            withContext(Dispatchers.IO) {
                if (existingHistoryId != null) {
                    updateExistingHistory(existingHistoryId, currentInfo, languageCode)
                } else {
                    createNewHistory(imageFile, currentInfo, languageCode)
                }
            }
        }
    }

    /** Nối chi tiết vừa tra cứu cập nhật lên nhật ký khám phá cũ. */
    private suspend fun updateExistingHistory(
            historyId: Int,
            info: SpeciesInfo,
            languageCode: String
    ) {
        val existingEntry = historyRepository.getHistoryById(historyId)
        if (existingEntry != null) {
            val entry = existingEntry.copy(speciesInfo = info, language = languageCode)
            Log.d(TAG, "Updating history entry: ${entry.id} - ${entry.speciesInfo.commonName}")
            historyRepository.update(entry)
        }
    }

    /** Thiết lập thông số và phát sinh lịch sử rà soát loài mới. */
    private suspend fun createNewHistory(imageFile: File, info: SpeciesInfo, languageCode: String) {
        val localImagePath =
                if (currentImageUri != null && currentImageUri!!.scheme == "file") {
                    currentImageUri!!.path
                } else {
                    ImageUtils.saveFileToInternalStorage(context, imageFile)
                }

        if (localImagePath != null) {
            val entry =
                    HistoryEntry(
                            id = 0,
                            imagePath = localImagePath,
                            localImagePath = localImagePath,
                            speciesInfo = info,
                            timestamp = System.currentTimeMillis(),
                            language = languageCode
                    )

            Log.d(TAG, "Inserting new history entry")
            val newId = historyRepository.insert(entry)
            currentHistoryEntryId = newId.toInt()
        }
    }

    /** Rà soát logic loại trừ các phản hồi mập mờ khỏi bản ghi lưu trữ. */
    private fun isValidInfo(info: SpeciesInfo): Boolean {
        return info.commonName.isNotEmpty() &&
                info.commonName != "..." &&
                info.commonName != "N/A" &&
                !info.description.contains("An error occurred", ignoreCase = true) &&
                !info.description.contains("Đã xảy ra lỗi", ignoreCase = true)
    }

    /** Bắt và biến đổi các ngoại lệ kỹ thuật thành lời nhắn chuẩn giao diện. */
    private fun handleError(e: Exception, onStateUpdate: (EcoLensUiState) -> Unit) {
        val errorMsg =
                when {
                    e.message?.contains("429") == true ->
                            getLocalizedString(R.string.error_quota_exceeded)
                    e is FileNotFoundException -> getLocalizedString(R.string.error_file_not_found)
                    else -> getLocalizedString(R.string.error_general, e.message)
                }
        onStateUpdate(EcoLensUiState(isLoading = false, error = errorMsg))
    }

    /** Rút chuỗi địa phương từ kho resource theo ngôn ngữ tương thích. */
    private fun getLocalizedString(resId: Int, vararg args: Any?): String {
        val locale = java.util.Locale(currentLanguageCode)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        val localizedContext = context.createConfigurationContext(config)
        return localizedContext.getString(resId, *args)
    }

    data class VnRedListScrapeResult(
        val phanHangBaoTon: String?,
        val tieuChuanDanhGia: String?,
        val dienGiaiDanhGia: String?,
        val namCongBo: String?
    )
}
