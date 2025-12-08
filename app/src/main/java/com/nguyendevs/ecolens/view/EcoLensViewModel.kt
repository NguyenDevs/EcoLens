package com.nguyendevs.ecolens.view

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.api.GeminiContent
import com.nguyendevs.ecolens.api.GeminiPart
import com.nguyendevs.ecolens.api.GeminiRequest
import com.nguyendevs.ecolens.database.HistoryDatabase
import com.nguyendevs.ecolens.model.ChatMessage
import com.nguyendevs.ecolens.model.EcoLensUiState
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.model.HistorySortOption
import com.nguyendevs.ecolens.model.SpeciesInfo
import com.nguyendevs.ecolens.network.RetrofitClient
import com.nguyendevs.ecolens.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.util.Locale

class EcoLensViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.iNaturalistApi
    private val historyDao = HistoryDatabase.getDatabase(application).historyDao()

    private val _uiState = MutableStateFlow(EcoLensUiState())
    val uiState: StateFlow<EcoLensUiState> = _uiState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private data class GeminiRawResponse(
        val commonName: String = "",
        val scientificName: String = "",
        val kingdom: String = "",
        val phylum: String = "",
        val className: String = "",
        val order: String = "",
        val family: String = "",
        val genus: String = "",
        val species: String = "",
        val rank: String = "",
        val description: String = "",
        val characteristics: Any? = null,
        val distribution: String = "",
        val habitat: String = "",
        val conservationStatus: String = "",
        val confidence: Double = 0.0
    )

    fun getHistoryBySortOption(
        sortOption: HistorySortOption,
        startDate: Long? = null,
        endDate: Long? = null
    ): Flow<List<HistoryEntry>> {
        return if (startDate != null && endDate != null) {
            when (sortOption) {
                HistorySortOption.NEWEST_FIRST -> historyDao.getHistoryByDateRangeNewest(startDate, endDate)
                HistorySortOption.OLDEST_FIRST -> historyDao.getHistoryByDateRangeOldest(startDate, endDate)
            }
        } else {
            when (sortOption) {
                HistorySortOption.NEWEST_FIRST -> historyDao.getAllHistoryNewestFirst()
                HistorySortOption.OLDEST_FIRST -> historyDao.getAllHistoryOldestFirst()
            }
        }
    }

    fun deleteAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            historyDao.deleteAll()
        }
    }

    fun toggleFavorite(entry: HistoryEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                historyDao.update(entry.copy(isFavorite = !entry.isFavorite))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun identifySpecies(imageUri: Uri, languageCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, speciesInfo = null)

            try {
                val context = getApplication<Application>()

                val imageFile = withContext(Dispatchers.Default) {
                    ImageUtils.uriToFile(context, imageUri, 1024)
                }

                val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

                val response = apiService.identifySpecies(image = imagePart, locale = languageCode)

                if (response.results.isNotEmpty()) {
                    val topResult = response.results.first()
                    val taxon = topResult.taxon
                    val scientificName = taxon.name
                    val confidence = topResult.combined_score

                    val speciesInfo = fetchDetailsFromGemini(scientificName, confidence, languageCode)
                    val finalInfo = speciesInfo.copy(
                        commonName = speciesInfo.commonName.ifEmpty { taxon.preferred_common_name ?: scientificName },
                        scientificName = scientificName,
                        confidence = confidence,
                        kingdom = speciesInfo.kingdom.ifEmpty {
                            taxon.ancestors.find { it.rank == "kingdom" }?.name ?: ""
                        }
                    )

                    withContext(Dispatchers.IO) {
                        val savedPath = ImageUtils.saveBitmapToInternalStorage(context, imageFile)
                        if (savedPath != null) {
                            historyDao.insert(
                                HistoryEntry(
                                    imagePath = savedPath,
                                    speciesInfo = finalInfo,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }

                    _uiState.value = _uiState.value.copy(isLoading = false, speciesInfo = finalInfo)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = context.getString(R.string.error_no_result)
                    )
                }

                withContext(Dispatchers.IO) {
                    if (imageFile.exists()) imageFile.delete()
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = getApplication<Application>().getString(R.string.error_prefix, e.message ?: "Unknown")
                )
            }
        }
    }

    private suspend fun fetchDetailsFromGemini(
        scientificName: String,
        confidence: Double,
        languageCode: String
    ): SpeciesInfo = withContext(Dispatchers.IO) {
        try {
            val isVietnamese = languageCode != "en"
            val highlightColor = "#00796B"
            val dangerColor = "#8B0000"
            val redBookColor = "#c97408"
            val vulnerableColor = "#eddb11"
            val leastConcernColor = "#55f200"
            val notRankedColor = "#05deff"

            val commonNameDesc = if (isVietnamese) "Tên thường gọi Tiếng Việt chuẩn nhất" else "Common name in English"

            val prompt = if (isVietnamese) {
                """
                Bạn là nhà sinh vật học. Cung cấp thông tin chi tiết về loài "$scientificName" bằng Tiếng Việt.
                === ĐỊNH DẠNG JSON ===
                {
                  "commonName": "$commonNameDesc",
                  "kingdom": "Chỉ tên Tiếng Việt",
                  "phylum": "Chỉ tên Tiếng Việt",
                  "className": "Chỉ tên Tiếng Việt",
                  "order": "Chỉ tên Tiếng Việt",
                  "family": "Tên khoa học (tên thường)",
                  "genus": "Tên khoa học (tên thường)",
                  "species": "Tên khoa học (tên thường)",
                  "rank": "Cấp phân loại",
                  "description": "Tổng quan 4 câu ngắn gọn, dùng <b>in đậm</b> cho đặc điểm nổi bật và <font color='$highlightColor'><b>xanh đậm</b></font> cho địa danh, tên riêng, số đo.",
                  "characteristics": "Danh sách gạch đầu dòng (•) mỗi dòng một ý về hình thái, kích thước, màu sắc. Dùng <b>in đậm</b> và <font color='$highlightColor'><b>xanh đậm</b></font>.",
                  "distribution": "Ưu tiên Việt Nam trước (nếu có), sau đó toàn cầu. Dùng <font color='$highlightColor'><b>xanh đậm</b></font> cho tên địa danh.",
                  "habitat": "Mô tả chi tiết môi trường sống, có định dạng đẹp.",
                  "conservationStatus": "Trạng thái bảo tồn kèm màu: <font color='$dangerColor'><b>Cực kỳ nguy cấp</b></font>, <font color='$dangerColor'><b>Nguy cấp</b></font>, <font color='$redBookColor'><b>Sách Đỏ Việt Nam</b></font>, <font color='$vulnerableColor'><b>Sắp nguy cấp</b></font>, <font color='$leastConcernColor'><b>Ít lo ngại</b></font>, <font color='$notRankedColor'><b>Chưa đánh giá</b></font> và thêm thông tin từ IUCN."
                }
                CHỈ TRẢ VỀ JSON.
                """.trimIndent()
            } else {
                """
                You are a biologist. Provide details about "$scientificName" in English.
                === JSON FORMAT ===
                {
                  "commonName": "$commonNameDesc",
                  "kingdom": "Name only",
                  "phylum": "Name only",
                  "className": "Name only",
                  "order": "Name only",
                  "family": "Scientific name",
                  "genus": "Scientific name",
                  "species": "Scientific name",
                  "rank": "Rank",
                  "description": "4-sentence overview with <b>bold</b> for key features and <font color='$highlightColor'><b>green bold</b></font> for places/names/measurements.",
                  "characteristics": "Bullet points (•) on new lines covering morphology, size, colors. Use <b>bold</b> and <font color='$highlightColor'><b>green bold</b></font> formatting.",
                  "distribution": "Vietnam first (if applicable), then worldwide. Use <font color='$highlightColor'><b>green bold</b></font> for locations.",
                  "habitat": "Specific environment details with formatting.",
                  "conservationStatus": "Status with color: <font color='$dangerColor'><b>Critically Endangered</b></font>, <font color='$dangerColor'><b>Endangered</b></font>, <font color='$redBookColor'><b>Vulnerable (Vietnam Red Data Book)</b></font>, <font color='$vulnerableColor'><b>Near Threatened</b></font>, <font color='$leastConcernColor'><b>Least Concern</b></font>, <font color='$notRankedColor'><b>Not Evaluated</b></font> and additional info from IUCN."
                }
                RETURN ONLY JSON.
                """.trimIndent()
            }

            val request = GeminiRequest(listOf(GeminiContent(listOf(GeminiPart(prompt)))))
            val response = apiService.askGemini(request)

            val jsonString = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanedJson = cleanJsonString(jsonString)

            val rawInfo = Gson().fromJson(cleanedJson, GeminiRawResponse::class.java)

            val characteristicsText = when (val chars = rawInfo.characteristics) {
                is String -> chars
                is List<*> -> chars.joinToString("\n")
                else -> ""
            }

            SpeciesInfo(
                kingdom = removeRankPrefix(rawInfo.kingdom, if (isVietnamese) "Giới" else "Kingdom"),
                phylum = removeRankPrefix(rawInfo.phylum, if (isVietnamese) "Ngành" else "Phylum"),
                className = removeRankPrefix(rawInfo.className, if (isVietnamese) "Lớp" else "Class"),
                order = removeRankPrefix(rawInfo.order, if (isVietnamese) "Bộ" else "Order"),
                family = removeRankPrefix(rawInfo.family, if (isVietnamese) "Họ" else "Family"),
                genus = removeRankPrefix(rawInfo.genus, if (isVietnamese) "Chi" else "Genus"),
                species = removeRankPrefix(rawInfo.species, if (isVietnamese) "Loài" else "Species"),

                commonName = rawInfo.commonName,
                scientificName = scientificName,
                rank = rawInfo.rank,

                description = cleanMarkdownToHtml(rawInfo.description),
                characteristics = cleanMarkdownToHtml(characteristicsText),
                distribution = cleanMarkdownToHtml(rawInfo.distribution),
                habitat = cleanMarkdownToHtml(rawInfo.habitat),
                conservationStatus = cleanMarkdownToHtml(rawInfo.conservationStatus),
                confidence = confidence
            )

        } catch (e: Exception) {
            val errorMsg = if (languageCode == "en") "An error occurred" else "Đã xảy ra lỗi"
            SpeciesInfo(commonName = scientificName, scientificName = scientificName, description = errorMsg, confidence = confidence)
        }
    }

    private fun cleanJsonString(json: String): String {
        return json.replace("```json", "", ignoreCase = true)
            .replace("```", "", ignoreCase = true)
            .trim()
            .let { text ->
                val start = text.indexOf('{')
                val end = text.lastIndexOf('}')
                if (start >= 0 && end > start) text.substring(start, end + 1) else text
            }
    }

    private fun cleanMarkdownToHtml(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return text.replace(Regex("(?<!\\\\)\\*\\*(?!\\s)(.+?)(?<!\\\\)\\*\\*")) { "<b>${it.groupValues[1]}</b>" }
            .replace(Regex("(?<!\\\\)__(?!\\s)(.+?)(?<!\\\\)__")) { "<b>${it.groupValues[1]}</b>" }
            .replace(Regex("(?<!\\\\)\\*(?!\\s)(.+?)(?<!\\\\)\\*")) { "<i>${it.groupValues[1]}</i>" }
            .replace(Regex("(?<!\\\\)_(?!\\s)(.+?)(?<!\\\\)_")) { "<i>${it.groupValues[1]}</i>" }
            .replace("\\*", "*").replace("\\_", "_")
    }

    private fun removeRankPrefix(text: String, prefix: String): String {
        return text.trim().replaceFirst(Regex("^(?i)$prefix\\s*[:\\-\\s]+"), "")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            .trim()
    }

    fun sendChatMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        val currentList = _chatMessages.value.toMutableList()
        currentList.add(ChatMessage(userMessage, true))
        currentList.add(ChatMessage("Đang suy nghĩ...", false, isLoading = true))
        _chatMessages.value = currentList

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = "Bạn là EcoLens AI, chuyên gia sinh học. Trả lời ngắn gọn bằng Tiếng Việt: \"$userMessage\""
                val request = GeminiRequest(listOf(GeminiContent(listOf(GeminiPart(prompt)))))
                val response = apiService.askGemini(request)

                val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Xin lỗi, tôi không thể trả lời."

                withContext(Dispatchers.Main) {
                    val updatedList = _chatMessages.value.toMutableList()
                    updatedList.removeLastOrNull()
                    updatedList.add(ChatMessage(reply, false))
                    _chatMessages.value = updatedList
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val updatedList = _chatMessages.value.toMutableList()
                    updatedList.removeLastOrNull()
                    updatedList.add(ChatMessage("Lỗi: ${e.message}", false))
                    _chatMessages.value = updatedList
                }
            }
        }
    }
}