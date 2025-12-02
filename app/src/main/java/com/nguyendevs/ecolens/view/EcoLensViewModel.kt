package com.nguyendevs.ecolens.view

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.Locale

class EcoLensViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "EcoLensViewModel"

    private val _uiState = MutableStateFlow(EcoLensUiState())
    val uiState: StateFlow<EcoLensUiState> = _uiState.asStateFlow()

    private val apiService = RetrofitClient.iNaturalistApi
    private val geminiModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private val historyDao = HistoryDatabase.getDatabase(application).historyDao()

    val history: StateFlow<List<HistoryEntry>> = historyDao.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Lưu ảnh từ URI vào internal storage
     * FIX: Đọc bitmap từ Main thread trước, sau đó mới chuyển sang IO để lưu file
     */
    private suspend fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            Log.d(TAG, "Bắt đầu lưu ảnh từ URI: $uri")

            // ĐỌC BITMAP TRÊN MAIN THREAD (hoặc thread hiện tại) trước
            // để tránh mất quyền truy cập URI khi chuyển dispatcher
            val bitmap = withContext(Dispatchers.Main) {
                try {
                    val contentResolver = context.contentResolver

                    // Sử dụng InputStream để đọc an toàn hơn
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    } ?: run {
                        // Fallback về cách cũ nếu InputStream fail
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
                        } else {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(contentResolver, uri)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi khi đọc bitmap: ${e.message}", e)
                    null
                }
            }

            if (bitmap == null) {
                Log.e(TAG, "❌ Không thể đọc bitmap từ URI")
                return null
            }

            Log.d(TAG, "✅ Đã load bitmap, kích thước: ${bitmap.width}x${bitmap.height}")

            // SAU ĐÓ MỚI CHUYỂN SANG IO DISPATCHER để lưu file
            withContext(Dispatchers.IO) {
                val filename = "species_${UUID.randomUUID()}.jpg"
                val file = File(context.filesDir, filename)

                Log.d(TAG, "Đường dẫn file sẽ lưu: ${file.absolutePath}")

                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    outputStream.flush()
                }

                // Giải phóng bitmap
                bitmap.recycle()

                Log.d(TAG, "✅ Đã lưu ảnh thành công tại: ${file.absolutePath}")
                Log.d(TAG, "File tồn tại: ${file.exists()}, Kích thước: ${file.length()} bytes")

                file.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi khi lưu ảnh: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }

    private fun saveToHistory(imageUri: Uri, speciesInfo: SpeciesInfo) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Bắt đầu lưu vào lịch sử...")

                val localPath = saveImageToInternalStorage(getApplication(), imageUri)

                if (localPath != null) {
                    withContext(Dispatchers.IO) {
                        val newEntry = HistoryEntry(
                            imagePath = localPath,
                            speciesInfo = speciesInfo,
                            timestamp = System.currentTimeMillis(),
                            isFavorite = false
                        )

                        Log.d(TAG, "Chuẩn bị insert vào database: ${newEntry.speciesInfo.commonName}")
                        historyDao.insert(newEntry)
                        Log.d(TAG, "✅ Đã insert thành công vào database")
                    }

                    // Kiểm tra xem đã lưu chưa
                    Log.d(TAG, "Số lượng entries trong history hiện tại: ${history.value.size}")
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
                Log.d(TAG, "✅ Đã cập nhật favorite cho entry ID: ${entry.id}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi toggle favorite: ${e.message}", e)
            }
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
                Log.d(TAG, "Bắt đầu nhận diện loài từ URI: $imageUri")

                val imageFile = uriToFile(getApplication(), imageUri)
                val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

                Log.d(TAG, "Gọi API iNaturalist...")
                val response = apiService.identifySpecies(imagePart)

                if (response.results.isNotEmpty()) {
                    val topResult = response.results.first()
                    val taxon = topResult.taxon
                    val scientificName = taxon.name

                    Log.d(TAG, "Tìm thấy loài: $scientificName, confidence: ${topResult.combined_score}")

                    // Gọi Gemini để lấy thông tin chi tiết
                    Log.d(TAG, "Gọi Gemini API...")
                    val speciesInfo = fetchDetailsFromGemini(scientificName, topResult.combined_score)

                    val finalInfo = speciesInfo.copy(
                        commonName = speciesInfo.commonName.ifEmpty { taxon.preferred_common_name ?: scientificName },
                        scientificName = scientificName,
                        confidence = topResult.combined_score,
                        kingdom = if(speciesInfo.kingdom.isEmpty()) taxon.ancestors?.find { it.rank == "kingdom" }?.name ?: "" else speciesInfo.kingdom
                    )

                    Log.d(TAG, "Thông tin cuối cùng: ${finalInfo.commonName}")

                    // Lưu vào lịch sử (không chờ kết quả)
                    saveToHistory(imageUri, finalInfo)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        speciesInfo = finalInfo
                    )

                    Log.d(TAG, "✅ Hoàn tất nhận diện")
                } else {
                    Log.w(TAG, "Không tìm thấy kết quả từ API")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Không thể nhận diện sinh vật trong ảnh. Vui lòng thử ảnh khác."
                    )
                }

                imageFile.delete()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi nhận diện: ${e.message}", e)
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
            val prompt = """
                Hãy đóng vai một nhà sinh vật học. Cung cấp thông tin chi tiết về loài sinh vật có tên khoa học là "$scientificName" bằng Tiếng Việt.
                Trả về kết quả duy nhất dưới dạng JSON (không markdown, không code block) với cấu trúc sau:
                {
                    "commonName": "Tên thường gọi tiếng Việt chuẩn nhất",
                    "kingdom": "Tên giới (CHỈ TÊN, ví dụ: 'Động vật', KHÔNG ghi 'Giới Động vật')",
                    "phylum": "Tên ngành (CHỈ TÊN, ví dụ: 'Dây sống', KHÔNG ghi 'Ngành Dây sống')",
                    "className": "Tên lớp (CHỈ TÊN, ví dụ: 'Thú', KHÔNG ghi 'Lớp Thú')",
                    "order": "Tên bộ (CHỈ TÊN, bỏ từ 'Bộ' ở đầu)",
                    "family": "Tên họ (CHỈ TÊN, bỏ từ 'Họ' ở đầu, nếu có tên tiếng anh, ưu tiên dùng tên tiếng Anh)",
                    "genus": "Tên chi (CHỈ TÊN, bỏ từ 'Chi' ở đầu, nếu có tên tiếng anh, ưu tiên dùng tên tiếng Anh)",
                    "species": "Tên loài (CHỈ TÊN, bỏ từ 'Loài' ở đầu, nếu có tên tiếng anh, ưu tiên dùng tên tiếng Anh)",
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

            Log.d(TAG, "Gemini response: ${jsonString.take(100)}...")

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
            Log.e(TAG, "❌ Lỗi khi parse Gemini response: ${e.message}", e)
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

    private suspend fun uriToFile(context: Context, uri: Uri): File {
        return withContext(Dispatchers.IO) {
            // Đọc bitmap trên IO dispatcher (vì đây là temp file cho API call)
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

    // Hàm debug để kiểm tra lịch sử
    fun debugHistory() {
        viewModelScope.launch {
            Log.d(TAG, "=== DEBUG HISTORY ===")
            Log.d(TAG, "Số lượng entries: ${history.value.size}")
            history.value.forEachIndexed { index, entry ->
                Log.d(TAG, "Entry $index: ${entry.speciesInfo.commonName}, path: ${entry.imagePath}")
            }
        }
    }

    // Hàm xóa toàn bộ lịch sử (nếu cần test)
    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            historyDao.deleteAll()
            Log.d(TAG, "✅ Đã xóa toàn bộ lịch sử")
        }
    }
}