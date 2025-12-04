package com.nguyendevs.ecolens.view

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.database.HistoryDatabase
import com.nguyendevs.ecolens.model.EcoLensUiState
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.model.HistorySortOption
import com.nguyendevs.ecolens.model.SpeciesInfo
import com.nguyendevs.ecolens.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class EcoLensViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "EcoLensViewModel"

    private val _uiState = MutableStateFlow(EcoLensUiState())
    val uiState: StateFlow<EcoLensUiState> = _uiState.asStateFlow()

    private val _searchTextAction = MutableLiveData<String?>()
    val searchTextAction: LiveData<String?> get() = _searchTextAction

    private val apiService = RetrofitClient.iNaturalistApi
    private val historyDao = HistoryDatabase.getDatabase(application).historyDao()

    // Phương thức mới cho sắp xếp và lọc lịch sử
    fun getHistoryBySortOption(sortOption: HistorySortOption): Flow<List<HistoryEntry>> {
        return when (sortOption) {
            HistorySortOption.NEWEST_FIRST -> historyDao.getAllHistoryNewestFirst()
            HistorySortOption.OLDEST_FIRST -> historyDao.getAllHistoryOldestFirst()
        }
    }

    fun deleteAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            historyDao.deleteAll()
        }
    }

    fun triggerSearch(query: String) {
        _searchTextAction.value = query
    }

    fun resetSearchAction() {
        _searchTextAction.value = null
    }

    fun toggleFavorite(entry: HistoryEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updatedEntry = entry.copy(isFavorite = !entry.isFavorite)
                historyDao.update(updatedEntry)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi toggle favorite: ${e.message}", e)
            }
        }
    }

    fun identifySpecies(imageUri: Uri, languageCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                speciesInfo = null
            )
            try {
                val context = getApplication<Application>()

                val (imageFile, bitmapForHistory) = withContext(Dispatchers.IO) {

                    val file = uriToFile(context, imageUri)

                    val inputStream = context.contentResolver.openInputStream(imageUri)
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    val scaledBitmap = scaleBitmapDown(originalBitmap, 1024)
                    if (originalBitmap != scaledBitmap) {
                        originalBitmap?.recycle() // Giải phóng ảnh gốc to
                    }

                    Pair(file, scaledBitmap)
                }

                val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

                Log.d(TAG, "Đang gửi ảnh đến Worker để nhận diện...")
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
                            taxon.ancestors?.find { it.rank == "kingdom" }?.name ?: ""
                        }
                    )

                    // Lưu vào lịch sử (đã có bitmap tối ưu)
                    bitmapForHistory?.let { saveToHistory(it, finalInfo) }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        speciesInfo = finalInfo
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = context.getString(R.string.error_no_result)
                    )
                }

                // Xóa file tạm
                withContext(Dispatchers.IO) {
                    if (imageFile.exists()) imageFile.delete()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi nhận diện loài: ${e.message}", e)
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
            val context = getApplication<Application>()
            val isVietnamese = languageCode != "en"

            val highlightColor = "#00796B"
            val dangerColor = "#8B0000"
            val redBookColor = "#c97408"
            val vulnerableColor = "#eddb11"
            val leastConcernColor = "#55f200"
            val notRankedColor = "#05deff"

            val commonNameDesc = if (isVietnamese)
                "Tên thường gọi Tiếng Việt chuẩn nhất"
            else
                "Common name in English"

            val prompt = if (isVietnamese) {
                """
                Bạn là nhà sinh vật học chuyên nghiệp. Hãy cung cấp thông tin chi tiết về loài "$scientificName" bằng Tiếng Việt chuẩn mực.
                
                === ĐỊNH DẠNG KẾT XUẤT ===
                CHỈ trả về JSON hợp lệ (CẤM dùng markdown, KHÔNG có ```json, chỉ được dùng th <b> cho in đậm, <i> cho chữ nghiêng):
                {
                  "commonName": "$commonNameDesc",
                  "kingdom": "Chỉ tên Tiếng Việt",
                  "phylum": "Chỉ tên Tiếng Việt",
                  "className": "Chỉ tên Tiếng Việt",
                  "order": "Chỉ tên Tiếng Việt",
                  "family": "Tên khoa học <i>(tên thường nếu có)</i>",
                  "genus": "Tên khoa học <i>(tên thường nếu có)</i>",
                  "species": "Tên khoa học <i>(tên thường nếu có)</i>",
                  "rank": "Cấp phân loại",
                  "description": "Tổng quan 4 câu ngắn gọn, dùng <b>in đậm</b> cho đặc điểm nổi bật và <font color='$highlightColor'><b>xanh đậm</b></font> cho địa danh, tên riêng, số đo.",
                  "characteristics": "Danh sách gạch đầu dòng (•) mỗi dòng một ý về hình thái, kích thước, màu sắc. Dùng <b>in đậm</b> và <font color='$highlightColor'><b>xanh đậm</b></font>.",
                  "distribution": "Ưu tiên Việt Nam trước (nếu có), sau đó toàn cầu. Dùng <font color='$highlightColor'><b>xanh đậm</b></font> cho tên địa danh.",
                  "habitat": "Mô tả chi tiết môi trường sống, có định dạng đẹp.",
                  "conservationStatus": "Trạng thái bảo tồn kèm màu: <font color='$dangerColor'><b>Cực kỳ nguy cấp</b></font>, <font color='$dangerColor'><b>Nguy cấp</b></font>, <font color='$redBookColor'><b>Sách Đỏ Việt Nam</b></font>, <font color='$vulnerableColor'><b>Sắp nguy cấp</b></font>, <font color='$leastConcernColor'><b>Ít lo ngại</b></font>, <font color='$notRankedColor'><b>Chưa đánh giá</b></font> và thêm thông tin từ IUCN."
                }
                QUAN TRỌNG: Chỉ trả về đúng đối tượng JSON. Không giải thích, không văn bản thừa.
                """.trimIndent()
            } else {
                """
                You are a professional biologist. Provide detailed information about "$scientificName" in English.
                
                === OUTPUT FORMAT ===
                Return ONLY valid JSON (DO NOT use markdown, NO ```json):
                {
                  "commonName": "$commonNameDesc",
                  "kingdom": "Name only",
                  "phylum": "Name only",
                  "className": "Name only",
                  "order": "Name only",
                  "family": "Scientific name",
                  "genus": "Scientific name",
                  "species": "Scientific name",
                  "rank": "Taxonomic rank",
                  "description": "4-sentence overview with <b>bold</b> for key features and <font color='$highlightColor'><b>green bold</b></font> for places/names/measurements.",
                  "characteristics": "Bullet points (•) on new lines covering morphology, size, colors. Use <b>bold</b> and <font color='$highlightColor'><b>green bold</b></font> formatting.",
                  "distribution": "Vietnam first (if applicable), then worldwide. Use <font color='$highlightColor'><b>green bold</b></font> for locations.",
                  "habitat": "Specific environment details with formatting.",
                  "conservationStatus": "Status with color: <font color='$dangerColor'><b>Critically Endangered</b></font>, <font color='$dangerColor'><b>Endangered</b></font>, <font color='$redBookColor'><b>Vulnerable (Vietnam Red Data Book)</b></font>, <font color='$vulnerableColor'><b>Near Threatened</b></font>, <font color='$leastConcernColor'><b>Least Concern</b></font>, <font color='$notRankedColor'><b>Not Evaluated</b></font> and additional info from IUCN."
                }
                CRITICAL: Return ONLY the JSON object. No explanations, no markdown fences, no extra text.
                """.trimIndent()
            }

            val workerUrl = "https://ecolens.tainguyen-devs.workers.dev/gemini"
            val requestBody = mapOf(
                "contents" to listOf(
                    mapOf("parts" to listOf(mapOf("text" to prompt)))
                )
            )

            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(180, TimeUnit.SECONDS)
                .build()

            val json = Gson().toJson(requestBody)
            val body = json.toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(workerUrl)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            Log.d(TAG, "Raw Gemini response: $responseBody")

            val geminiResponse = Gson().fromJson(responseBody, GeminiResponse::class.java)
            val jsonString = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""

            val cleanedJson = jsonString
                .replace("```json", "", ignoreCase = true)
                .replace("```", "", ignoreCase = true)
                .trim()
                .let { text ->
                    val start = text.indexOf('{')
                    val end = text.lastIndexOf('}')
                    if (start >= 0 && end > start) text.substring(start, end + 1) else text
                }

            if (cleanedJson.isEmpty() || !cleanedJson.startsWith("{")) {
                throw IllegalStateException("Invalid JSON from Gemini")
            }

            val rawInfo = Gson().fromJson(cleanedJson, SpeciesInfo::class.java)
                ?: throw IllegalStateException("Failed to parse SpeciesInfo")

            val cleanedInfo = rawInfo.copy(
                kingdom = removeRankPrefix(rawInfo.kingdom ?: "", if (isVietnamese) "Giới" else "Kingdom"),
                phylum = removeRankPrefix(rawInfo.phylum ?: "", if (isVietnamese) "Ngành" else "Phylum"),
                className = removeRankPrefix(rawInfo.className ?: "", if (isVietnamese) "Lớp" else "Class"),
                order = removeRankPrefix(rawInfo.order ?: "", if (isVietnamese) "Bộ" else "Order"),
                family = removeRankPrefix(rawInfo.family ?: "", if (isVietnamese) "Họ" else "Family"),
                genus = removeRankPrefix(rawInfo.genus ?: "", if (isVietnamese) "Chi" else "Genus"),
                species = removeRankPrefix(rawInfo.species ?: "", if (isVietnamese) "Loài" else "Species"),

                // Áp dụng dọn dẹp Markdown cho các trường mô tả
                description = cleanMarkdownToHtml(rawInfo.description),
                characteristics = cleanMarkdownToHtml(rawInfo.characteristics),
                distribution = cleanMarkdownToHtml(rawInfo.distribution),
                habitat = cleanMarkdownToHtml(rawInfo.habitat),
                conservationStatus = cleanMarkdownToHtml(rawInfo.conservationStatus),

                scientificName = scientificName,
                confidence = confidence
            )

            cleanedInfo

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lấy thông tin từ Gemini: ${e.message}", e)
            val errorMsg = if (languageCode == "en")
                getApplication<Application>().getString(R.string.error_occurred)
            else
                getApplication<Application>().getString(R.string.error_default)

            SpeciesInfo(
                commonName = scientificName,
                scientificName = scientificName,
                description = errorMsg,
                confidence = confidence
            )
        }
    }


    private fun cleanMarkdownToHtml(text: String?): String {
        if (text.isNullOrBlank()) return ""

        return text
            // **bold** hoặc __bold__ → <b>bold</b>
            .replace(Regex("(?<!\\\\)\\*\\*(?!\\s)(.+?)(?<!\\\\)\\*\\*")) { "<b>${it.groupValues[1]}</b>" }
            .replace(Regex("(?<!\\\\)__(?!\\s)(.+?)(?<!\\\\)__")) { "<b>${it.groupValues[1]}</b>" }

            // *italic* hoặc _italic_ → <i>italic</i>
            .replace(Regex("(?<!\\\\)\\*(?!\\s)(.+?)(?<!\\\\)\\*")) { "<i>${it.groupValues[1]}</i>" }
            .replace(Regex("(?<!\\\\)_(?!\\s)(.+?)(?<!\\\\)_")) { "<i>${it.groupValues[1]}</i>" }

            // Xử lý trường hợp bị escape \* → bỏ escape (tùy chọn)
            .replace("\\*", "*")
            .replace("\\_", "_")
    }


    private fun removeRankPrefix(text: String, prefix: String): String {
        val trimmed = text.trim()
        val regex = Regex("^(?i)$prefix\\s*[:\\-\\s]+")
        return trimmed.replaceFirst(regex, "").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }.trim()
    }

    private suspend fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
        return withContext(Dispatchers.IO) {
            try {
                val filename = "species_${UUID.randomUUID()}.jpg"
                val file = File(context.filesDir, filename)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                bitmap.recycle()
                file.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi lưu ảnh: ${e.message}", e)
                null
            }
        }
    }

    private fun scaleBitmapDown(bitmap: Bitmap?, maxDimension: Int): Bitmap? {
        if (bitmap == null) return null
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        var newWidth = originalWidth
        var newHeight = originalHeight

        if (originalWidth > maxDimension || originalHeight > maxDimension) {
            val ratio = originalWidth.toFloat() / originalHeight.toFloat()
            if (ratio > 1) {
                newWidth = maxDimension
                newHeight = (maxDimension / ratio).toInt()
            } else {
                newHeight = maxDimension
                newWidth = (maxDimension * ratio).toInt()
            }
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }
        return bitmap
    }

    private fun saveToHistory(bitmap: Bitmap, speciesInfo: SpeciesInfo) {
        viewModelScope.launch {
            val path = saveBitmapToInternalStorage(getApplication(), bitmap)
            if (path != null) {
                val entry = HistoryEntry(
                    imagePath = path,
                    speciesInfo = speciesInfo,
                    timestamp = System.currentTimeMillis(),
                    isFavorite = false
                )
                historyDao.insert(entry)
            }
        }
    }

    private suspend fun uriToFile(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)!!
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val file = File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
        bitmap.recycle()
        file
    }

    // Gemini response model
    data class GeminiResponse(val candidates: List<Candidate>?)
    data class Candidate(val content: Content?)
    data class Content(val parts: List<Part>?)
    data class Part(val text: String?)
}