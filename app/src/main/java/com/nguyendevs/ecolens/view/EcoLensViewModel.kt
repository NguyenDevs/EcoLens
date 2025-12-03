package com.nguyendevs.ecolens.view

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import java.util.concurrent.TimeUnit
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
import com.nguyendevs.ecolens.model.SpeciesInfo
import com.nguyendevs.ecolens.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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

class EcoLensViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "EcoLensViewModel"

    private val _uiState = MutableStateFlow(EcoLensUiState())
    val uiState: StateFlow<EcoLensUiState> = _uiState.asStateFlow()

    private val _searchTextAction = MutableLiveData<String?>()
    val searchTextAction: LiveData<String?> get() = _searchTextAction

    private val apiService = RetrofitClient.iNaturalistApi

    fun triggerSearch(query: String) {
        _searchTextAction.value = query
    }

    fun resetSearchAction() {
        _searchTextAction.value = null
    }

    private val historyDao = HistoryDatabase.getDatabase(application).historyDao()

    val history: StateFlow<List<HistoryEntry>> = historyDao.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private suspend fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
        return withContext(Dispatchers.IO) {
            try {
                val filename = "species_${UUID.randomUUID()}.jpg"
                val file = File(context.filesDir, filename)

                Log.d(TAG, "Đường dẫn file sẽ lưu: ${file.absolutePath}")

                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    outputStream.flush()
                }
                bitmap.recycle()

                file.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi lưu ảnh: ${e.message}", e)
                e.printStackTrace()
                null
            }
        }
    }

    private fun saveToHistory(bitmap: Bitmap, speciesInfo: SpeciesInfo) {
        viewModelScope.launch {
            try {
                val localPath = saveBitmapToInternalStorage(getApplication(), bitmap)

                if (localPath != null) {
                    withContext(Dispatchers.IO) {
                        val newEntry = HistoryEntry(
                            imagePath = localPath,
                            speciesInfo = speciesInfo,
                            timestamp = System.currentTimeMillis(),
                            isFavorite = false
                        )
                        historyDao.insert(newEntry)
                    }
                } else {
                    Log.e(TAG, "❌ Không thể lưu ảnh vào internal storage")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi lưu vào lịch sử: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }

    fun toggleFavorite(entry: HistoryEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updatedEntry = entry.copy(isFavorite = !entry.isFavorite)
                historyDao.update(updatedEntry)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi toggle favorite: ${e.message}", e)
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
            var bitmapForHistory: Bitmap? = null
            try {
                val context = getApplication<Application>()
                bitmapForHistory = try {
                    context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi khi đọc bitmap cho history: ${e.message}")
                    null
                }

                if (bitmapForHistory != null) {
                    Log.d(TAG, "✅ Đã đọc bitmap cho history: ${bitmapForHistory.width}x${bitmapForHistory.height}")
                }

                val imageFile = uriToFile(context, imageUri)
                val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

                Log.d(TAG, "📤 Đang gửi request đến Worker...")

                val response = try {
                    apiService.identifySpecies(
                        image = imagePart,
                        locale = languageCode
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Lỗi khi gọi API: ${e.message}", e)

                    // Log raw response nếu có
                    if (e is retrofit2.HttpException) {
                        val errorBody = e.response()?.errorBody()?.string()
                        Log.e(TAG, "📛 HTTP Error Body: $errorBody")
                    }
                    throw e
                }

                Log.d(TAG, "✅ Response từ Worker thành công")

                if (response.results.isNotEmpty()) {
                    val topResult = response.results.first()
                    val taxon = topResult.taxon
                    val scientificName = taxon.name

                    Log.d(TAG, "Tìm thấy loài: $scientificName, confidence: ${topResult.combined_score}")

                    Log.d(TAG, "Gọi Gemini API...")
                    val speciesInfo = fetchDetailsFromGemini(scientificName, topResult.combined_score, languageCode)

                    val finalInfo = speciesInfo.copy(
                        commonName = speciesInfo.commonName.ifEmpty { taxon.preferred_common_name ?: scientificName },
                        scientificName = scientificName,
                        confidence = topResult.combined_score,
                        kingdom = if(speciesInfo.kingdom.isEmpty()) taxon.ancestors?.find { it.rank == "kingdom" }?.name ?: "" else speciesInfo.kingdom
                    )
                    Log.d(TAG, "Thông tin cuối cùng: ${finalInfo.commonName}")

                    if (bitmapForHistory != null) {
                        saveToHistory(bitmapForHistory, finalInfo)
                    } else {
                        Log.e(TAG, "⚠️ Không thể lưu lịch sử vì bitmap null")
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        speciesInfo = finalInfo
                    )
                } else {
                    Log.w(TAG, "Không tìm thấy kết quả từ API")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = context.getString(R.string.error_no_result)
                    )
                }

                imageFile.delete()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi nhận diện: ${e.message}", e)
                e.printStackTrace()
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
    ): SpeciesInfo {
        return try {
            val context = getApplication<Application>()
            val highlightColor = "#00796B"
            val dangerColor = "#8B0000"
            val redBookColor = "#c97408"
            val vulnerableColor = "#eddb11"
            val leastConcernColor = "#55f200"
            val notRankedColor = "#05deff"

            val langInstruction = if (languageCode == "en") "in English" else "bằng Tiếng Việt"
            val commonNameDesc = if (languageCode == "en") "Common name in English" else "Tên thường gọi Tiếng Việt chuẩn nhất"

            val prompt = """
                   You are a professional biologist. Provide detailed information about "$scientificName" $langInstruction.
                       === OUTPUT FORMAT ===
                   Return ONLY valid JSON (no markdown, no ```json):
                   {
                   "commonName": "$commonNameDesc",
                   "kingdom": "Vietnamese Name only",
                   "phylum": "Vietnamese Name only", 
                   "className": "Vietnamese Name only",
                   "order": "Vietnamese Name only",
                   "family": "Scientific name <i>(common name)</i> if available",
                   "genus": "Scientific name <i>(common name)</i> if available",
                   "species": "Scientific name <i>(common name)</i> if available",
                   "rank": "Taxonomic rank",
                   "description": "4-sentence overview with <b>bold</b> for key features and <font color='$highlightColor'><b>green bold</b></font> for places/names/measurements.",
                   "characteristics": "Bullet points (•) on new lines covering morphology, size, colors. Use <b>bold</b> and <font color='$highlightColor'><b>green bold</b></font> formatting.",
                   "distribution": "Vietnam first (if applicable), then worldwide. Use <font color='$highlightColor'><b>green bold</b></font> for locations.",
                   "habitat": "Specific environment details with formatting.",
                   "conservationStatus": "Status with color: <font color='$dangerColor'><b>Critically Endangered/Endangered</b></font>, <font color='$redBookColor'><b>Red Book/Vulnerable</b></font>, <font color='$vulnerableColor'><b>Near Threatened</b></font>, <font color='$leastConcernColor'><b>Least Concern</b></font>, <font color='$notRankedColor'><b>Not Ranked</b></font> and some info from IUCN"
                   }
                   CRITICAL: Return ONLY the JSON object. No explanations, no markdown fences, no extra text.
                   """.trimIndent()

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

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            val responseBody = response.body?.string() ?: ""
            Log.d(TAG, "📥 Raw Gemini response: $responseBody")

            val geminiResponse = Gson().fromJson(responseBody, GeminiResponse::class.java)
            val jsonString = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""

            Log.d(TAG, "📄 Extracted JSON string: $jsonString")

            // ✅ CLEAN JSON AGGRESSIVELY
            val cleanedJson = jsonString
                .replace("```json", "")
                .replace("```", "")
                .trim()
                .let { text ->
                    // Extract only JSON object
                    val start = text.indexOf('{')
                    val end = text.lastIndexOf('}')
                    if (start >= 0 && end > start) {
                        text.substring(start, end + 1)
                    } else {
                        text
                    }
                }

            Log.d(TAG, "🧹 Cleaned JSON: $cleanedJson")

            // ✅ CHECK IF JSON IS VALID BEFORE PARSING
            if (cleanedJson.isEmpty() || !cleanedJson.startsWith("{")) {
                throw IllegalStateException("Invalid JSON response from Gemini")
            }

            val info = Gson().fromJson(cleanedJson, SpeciesInfo::class.java)

            // ✅ NULL CHECK BEFORE USING
            if (info == null) {
                Log.e(TAG, "❌ Gson returned null - JSON parsing failed")
                throw IllegalStateException("Failed to parse JSON to SpeciesInfo")
            }

            val cleanedInfo = info.copy(
                kingdom = removeRankPrefix(info.kingdom ?: "", "Giới|Kingdom"),
                phylum = removeRankPrefix(info.phylum ?: "", "Ngành|Phylum"),
                className = removeRankPrefix(info.className ?: "", "Lớp|Class"),
                order = removeRankPrefix(info.order ?: "", "Bộ|Order"),
                family = removeRankPrefix(info.family ?: "", "Họ|Family"),
                genus = removeRankPrefix(info.genus ?: "", "Chi|Genus"),
                species = removeRankPrefix(info.species ?: "", "Loài|Species"),
                scientificName = scientificName,
                confidence = confidence
            )

            cleanedInfo

        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi khi parse Gemini response: ${e.message}", e)
            e.printStackTrace()

            val context = getApplication<Application>()
            val errorMsg = if (languageCode == "en")
                context.getString(R.string.error_occurred)
            else
                context.getString(R.string.error_default)

            SpeciesInfo(
                commonName = scientificName,
                scientificName = scientificName,
                description = errorMsg,
                confidence = confidence
            )
        }
    }

    private fun removeRankPrefix(text: String, prefixPattern: String): String {
        val trimmedText = text.trim()
        val regex = Regex("^(?i)($prefixPattern)\\s*[:]?\\s*")
        return trimmedText.replaceFirst(regex, "").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }

    private suspend fun uriToFile(context: Context, uri: Uri): File {
        return withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            }
            bitmap.recycle()
            file
        }
    }

    data class GeminiResponse(
        val candidates: List<Candidate>?
    )

    data class Candidate(
        val content: Content?
    )

    data class Content(
        val parts: List<Part>?
    )

    data class Part(
        val text: String?
    )
}