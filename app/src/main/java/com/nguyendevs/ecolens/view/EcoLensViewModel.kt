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

            val langInstruction = if (languageCode == "en") "in English" else "bằng tiếng Việt"
            val commonNameDesc = if (languageCode == "en") "Common name in English" else "Tên thường gọi tiếng Việt chuẩn nhất"

            val prompt = """
            You are a professional biologist. Please provide detailed information about the species with scientific name "$scientificName" $langInstruction.
            
            === FORMAT REQUIREMENTS ===
            
            1. CONTENT FIELDS (description, characteristics, distribution, habitat, conservationStatus):
               - Use <b>...</b> for bold text for main features and notable properties
               - Use <font color='$highlightColor'><b>...</b></font> for green color and bold for:
                 + Specific place names
                 + Proper names
                 + Important size measurements (length, weight, dimensions...)
               - ONLY use basic HTML tags (<b>, <font>), DO NOT use Markdown (**)
            
            2. CLASSIFICATION FIELDS (kingdom, phylum, className, order, family, genus, species):
               - ONLY write the NAME, DO NOT add prefixes ("Kingdom", "Phylum", "Class", etc.)
               - Example CORRECT: "Animalia", "Chordata", "Mammalia" (or Vietnamese equivalents)
               - Example WRONG: "Kingdom Animalia", "Phylum Chordata"
               - For family, genus, species: Prioritize English/Latin names. If there's a local name, put it in parentheses AND wrap the ENTIRE parenthetical phrase in <i>...</i>
                 + Example: "Felidae <i>(Cat Family)</i>", "Panthera <i>(Big Cats)</i>"
            
            3. CONSERVATIONSTATUS FIELD - COLOR CODING:
               - Critically Endangered/Endangered: <font color='$dangerColor'><b>...</b></font>
               - Red Book/Vulnerable: <font color='$redBookColor'><b>...</b></font>
               - Near Threatened: <font color='$vulnerableColor'><b>...</b></font>
               - Least Concern: <font color='$leastConcernColor'><b>...</b></font>
               - Not Ranked: <font color='$notRankedColor'><b>...</b></font>
            
            === CONTENT REQUIREMENTS ===
            
            - description: General overview (about 5 sentences), with paragraph indentation.
            - characteristics: Present in bullet point format (•), new line between points, focus on morphology, size, colors.
            - distribution: Prioritize distribution in Vietnam first (if applicable), then worldwide.
            - habitat: Specific living environment, with paragraph indentation.
            - conservationStatus: Current conservation status.
            
            === OUTPUT FORMAT ===
            
            Return pure JSON (NO markdown fence, NO ```json):
            
            {
                "commonName": "$commonNameDesc",
                "kingdom": "Kingdom name (name only)",
                "phylum": "Phylum name (name only)",
                "className": "Class name (name only)",
                "order": "Order name (name only)",
                "family": "Family name (common name if available)",
                "genus": "Genus name (common name if available)",
                "species": "Species name (common name if available)",
                "rank": "Classification rank",
                "description": "...",
                "characteristics": "...",
                "distribution": "...",
                "habitat": "...",
                "conservationStatus": "..."
            }
        """.trimIndent()

            val workerUrl = "https://ecolens.tainguyen-devs.workers.dev/gemini"
            val requestBody = mapOf(
                "contents" to listOf(
                    mapOf("parts" to listOf(mapOf("text" to prompt)))
                )
            )

            val client = OkHttpClient()
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
            val geminiResponse = Gson().fromJson(responseBody, GeminiResponse::class.java)
            val jsonString = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""

            val cleanedJson = jsonString.replace("```json", "").replace("```", "").trim()
            val info = Gson().fromJson(cleanedJson, SpeciesInfo::class.java)

            val cleanedInfo = info.copy(
                kingdom = removeRankPrefix(info.kingdom, "Giới|Kingdom"),
                phylum = removeRankPrefix(info.phylum, "Ngành|Phylum"),
                className = removeRankPrefix(info.className, "Lớp|Class"),
                order = removeRankPrefix(info.order, "Bộ|Order"),
                family = removeRankPrefix(info.family, "Họ|Family"),
                genus = removeRankPrefix(info.genus, "Chi|Genus"),
                species = removeRankPrefix(info.species, "Loài|Species"),
                scientificName = scientificName,
                confidence = confidence
            )

            cleanedInfo

        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi khi parse Gemini response: ${e.message}", e)

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