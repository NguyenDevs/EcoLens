package com.nguyendevs.ecolens.view

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.Gson
import com.nguyendevs.ecolens.BuildConfig
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.Locale

class EcoLensViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(EcoLensUiState())
    val uiState: StateFlow<EcoLensUiState> = _uiState.asStateFlow()

    private val apiService = RetrofitClient.iNaturalistApi
    private val geminiModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private val historyDao = HistoryDatabase.Companion.getDatabase(application).historyDao()

    val history: StateFlow<List<HistoryEntry>> = historyDao.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }

            // Tạo tên file duy nhất
            val filename = "species_${UUID.randomUUID()}.jpg"
            // Lưu vào thư mục files của app (vĩnh viễn)
            val file = File(context.filesDir, filename)

            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            stream.flush()
            stream.close()

            file.absolutePath // Trả về đường dẫn tuyệt đối
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveToHistory(imageUri: Uri, speciesInfo: SpeciesInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            val localPath = saveImageToInternalStorage(getApplication(), imageUri)
            if (localPath != null) {
                val newEntry = HistoryEntry(
                    imagePath = localPath,
                    speciesInfo = speciesInfo,
                    date = System.currentTimeMillis()
                )
                historyDao.insert(newEntry)
            }
        }
    }

    fun toggleFavorite(entry: HistoryEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedEntry = entry.copy(isFavorite = !entry.isFavorite)
            historyDao.update(updatedEntry)
        }
    }

    fun identifySpecies(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                speciesInfo = null
            )

            try {
                val imageFile = uriToFile(getApplication(), imageUri)
                val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

                val response = apiService.identifySpecies(imagePart)

                if (response.results.isNotEmpty()) {
                    val topResult = response.results.first()
                    val taxon = topResult.taxon
                    val scientificName = taxon.name // Tên khoa học

                    // 3. Gọi Gemini để lấy thông tin chi tiết bằng Tiếng Việt
                    val speciesInfo = fetchDetailsFromGemini(scientificName, topResult.combined_score)

                    // Điền thêm các thông tin phân loại học từ iNaturalist nếu Gemini bị lỗi hoặc thiếu
                    val finalInfo = speciesInfo.copy(
                        commonName = speciesInfo.commonName.ifEmpty { taxon.preferred_common_name ?: scientificName },
                        scientificName = scientificName,
                        confidence = topResult.combined_score,
                        // Nếu Gemini không trả về phân loại, dùng tạm của iNat (tùy chọn)
                        kingdom = if(speciesInfo.kingdom.isEmpty()) taxon.ancestors?.find { it.rank == "kingdom" }?.name ?: "" else speciesInfo.kingdom
                    )

                    saveToHistory(imageUri, finalInfo)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        speciesInfo = finalInfo
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Không thể nhận diện sinh vật trong ảnh. Vui lòng thử ảnh khác."
                    )
                }

                imageFile.delete()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Đã xảy ra lỗi: ${e.message}"
                )
            }
        }
    }

    private suspend fun fetchDetailsFromGemini(scientificName: String, confidence: Double): SpeciesInfo {
        return try {
            // Prompt đã được tinh chỉnh để yêu cầu Gemini bỏ từ chỉ cấp bậc
            val prompt = """
                Hãy đóng vai một nhà sinh vật học. Cung cấp thông tin chi tiết về loài sinh vật có tên khoa học là "$scientificName" bằng Tiếng Việt.
                Trả về kết quả duy nhất dưới dạng JSON (không markdown, không code block) với cấu trúc sau:
                {
                    "commonName": "Tên thường gọi tiếng Việt chuẩn nhất",
                    "kingdom": "Tên giới (CHỈ TÊN, ví dụ: 'Động vật', KHÔNG ghi 'Giới Động vật')",
                    "phylum": "Tên ngành (CHỈ TÊN, ví dụ: 'Dây sống', KHÔNG ghi 'Ngành Dây sống')",
                    "className": "Tên lớp (CHỈ TÊN, ví dụ: 'Thú', KHÔNG ghi 'Lớp Thú')",
                    "order": "Tên bộ (CHỈ TÊN, bỏ từ 'Bộ' ở đầu)",
                    "family": "Tên họ (CHỈ TÊN, bỏ từ 'Họ' ở đầu)",
                    "genus": "Tên chi (CHỈ TÊN, bỏ từ 'Chi' ở đầu)",
                    "species": "Tên loài (CHỈ TÊN, bỏ từ 'Loài' ở đầu)",
                    "rank": "Cấp phân loại hiện tại (Ví dụ: Loài)",
                    "description": "Mô tả tổng quan ngắn gọn và thú vị (khoảng 5 câu).",
                    "characteristics": "Đặc điểm hình thái, kích thước, màu sắc nổi bật.",
                    "distribution": "Phân bố địa lý (ưu tiên nơi tìm thấy ở Việt Nam sau đó là thế giới, nếu không có ở Việt Nam thì chỉ cần nói rằng loài này hiện không có ở Việt Nam và đang chỉ có ở đâu trên thế giới).",
                    "habitat": "Môi trường sống (rừng, biển, đô thị...).",
                    "conservationStatus": "Tình trạng bảo tồn (ví dụ: Sách đỏ, Ít quan tâm)."
                }
            """.trimIndent()

            val response = geminiModel.generateContent(prompt)
            val jsonString = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: ""

            val gson = Gson()
            val info = gson.fromJson(jsonString, SpeciesInfo::class.java)

            val cleanedInfo = info.copy(
                kingdom = removeRankPrefix(info.kingdom, "Giới"),
                phylum = removeRankPrefix(info.phylum, "Ngành"),
                className = removeRankPrefix(info.className, "Lớp"),
                order = removeRankPrefix(info.order, "Bộ"),
                family = removeRankPrefix(info.family, "Họ"),
                genus = removeRankPrefix(info.genus, "Chi"),
                species = removeRankPrefix(info.species, "Loài"),
                scientificName = scientificName,
                confidence = confidence
            )

            cleanedInfo

        } catch (e: Exception) {
            Log.e("Gemini", "Error parsing Gemini response", e)
            SpeciesInfo(
                commonName = scientificName,
                scientificName = scientificName,
                description = "Không thể lấy thông tin chi tiết từ AI vào lúc này.",
                confidence = confidence
            )
        }
    }

    private fun removeRankPrefix(text: String, prefix: String): String {
        val trimmedText = text.trim()
        val regex = Regex("^(?i)$prefix\\s*[:]?\\s*")
        return trimmedText.replaceFirst(regex, "").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }


    private fun uriToFile(context: Context, uri: Uri): File {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }

        val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        outputStream.flush()
        outputStream.close()

        return file
    }
}