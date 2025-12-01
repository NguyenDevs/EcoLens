package com.nguyendevs.ecolens.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nguyendevs.ecolens.model.EcoLensUiState
import com.nguyendevs.ecolens.model.SpeciesInfo
import com.nguyendevs.ecolens.model.HistoryEntry
import com.nguyendevs.ecolens.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class EcoLensViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(EcoLensUiState())
    val uiState: StateFlow<EcoLensUiState> = _uiState.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    private val apiService = RetrofitClient.iNaturalistApi
    private val translationService = RetrofitClient.translationApi

<<<<<<< Updated upstream
=======

    private fun saveToHistory(imageUri: Uri, speciesInfo: SpeciesInfo) {
        val newEntry = HistoryEntry(
            id = System.currentTimeMillis(),
            imageUri = imageUri,
            speciesInfo = speciesInfo
        )
        // Thêm mục mới vào đầu danh sách (mới nhất ở trên)
        _history.value = listOf(newEntry) + _history.value
    }

>>>>>>> Stashed changes
    fun identifySpecies(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                speciesInfo = null
            )

            try {
                val imageFile = uriToFile(context, imageUri)
                val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

                val response = apiService.identifySpecies(imagePart)

                if (response.results.isNotEmpty()) {
                    val topResult = response.results.first()
                    val taxon = topResult.taxon

                    val englishName = taxon.preferred_common_name?.takeIf { it.isNotEmpty() }
                        ?: taxon.name

                    val commonNameVi = translateToVietnamese(englishName)

                    val kingdom = taxon.ancestors?.find { it.rank == "kingdom" }?.name ?: ""
                    val phylum = taxon.ancestors?.find { it.rank == "phylum" }?.name ?: ""
                    val className = taxon.ancestors?.find { it.rank == "class" }?.name ?: ""
                    val order = taxon.ancestors?.find { it.rank == "order" }?.name ?: ""
                    val family = taxon.ancestors?.find { it.rank == "family" }?.name ?: ""
                    val genus = taxon.ancestors?.find { it.rank == "genus" }?.name ?: ""

                    val genusName = if (genus.isEmpty() && taxon.rank == "genus") taxon.name else genus
                    val speciesName = if (taxon.rank == "species") taxon.name else ""

                    val rankVi = when(taxon.rank) {
                        "kingdom" -> "Giới"
                        "phylum" -> "Ngành"
                        "class" -> "Lớp"
                        "order" -> "Bộ"
                        "family" -> "Họ"
                        "genus" -> "Chi"
                        "species" -> "Loài"
                        else -> taxon.rank
                    }

                    var description = ""
                    var characteristics = ""
                    var distribution = ""
                    var habitat = ""

                    try {
                        val taxonDetails = apiService.getTaxonDetails(taxon.id)
                        val wikipediaSummary = taxonDetails.results.firstOrNull()?.wikipedia_summary ?: ""

                        if (wikipediaSummary.isNotEmpty()) {
                            val translatedSummary = translateToVietnamese(wikipediaSummary)
                            val sections = translatedSummary.split(".")

                            description = sections.take(4).joinToString(". ") + "."

                            val fullText = translatedSummary.lowercase()

                            if (fullText.contains("đặc điểm") || fullText.contains("đặc trưng") ||
                                fullText.contains("hình dạng") || fullText.contains("kích thước")) {
                                val charIndex = sections.indexOfFirst {
                                    it.lowercase().contains("đặc điểm") ||
                                            it.lowercase().contains("đặc trưng") ||
                                            it.lowercase().contains("hình dạng") ||
                                            it.lowercase().contains("kích thước")
                                }
                                if (charIndex != -1) {
                                    characteristics = sections.drop(charIndex).take(3).joinToString(". ") + "."
                                }
                            }

                            if (fullText.contains("phân bố") || fullText.contains("sinh sống") ||
                                fullText.contains("tìm thấy")) {
                                val distIndex = sections.indexOfFirst {
                                    it.lowercase().contains("phân bố") ||
                                            it.lowercase().contains("sinh sống") ||
                                            it.lowercase().contains("tìm thấy")
                                }
                                if (distIndex != -1) {
                                    distribution = sections.drop(distIndex).take(2).joinToString(". ") + "."
                                }
                            }

                            if (fullText.contains("môi trường") || fullText.contains("sống ở") ||
                                fullText.contains("sinh cảnh")) {
                                val habIndex = sections.indexOfFirst {
                                    it.lowercase().contains("môi trường") ||
                                            it.lowercase().contains("sống ở") ||
                                            it.lowercase().contains("sinh cảnh")
                                }
                                if (habIndex != -1) {
                                    habitat = sections.drop(habIndex).take(2).joinToString(". ") + "."
                                }
                            }

                            if (characteristics.isEmpty() && distribution.isEmpty()) {
                                description = translatedSummary
                            }
                        }
                    } catch (e: Exception) {
                        description = "Không có thông tin chi tiết"
                    }

                    val conservationStatus = "Chưa có thông tin"

                    val speciesInfo = SpeciesInfo(
                        commonName = commonNameVi,
                        scientificName = taxon.name,
                        kingdom = kingdom,
                        phylum = phylum,
                        className = className,
                        order = order,
                        family = family,
                        genus = genusName,
                        species = speciesName,
                        rank = rankVi,
                        description = description,
                        characteristics = characteristics,
                        distribution = distribution.ifEmpty { "Xem thêm trên iNaturalist" },
                        habitat = habitat,
                        conservationStatus = conservationStatus,
                        confidence = topResult.combined_score
                    )

                    saveToHistory(imageUri, speciesInfo)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        speciesInfo = speciesInfo
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

    private suspend fun translateToVietnamese(text: String): String {
        if (text.isEmpty()) return ""

        return try {
            val response = translationService.translate(
                text = text,
                targetLang = "vi"
            )
            response.data.translations.firstOrNull()?.translatedText ?: text
        } catch (e: Exception) {
            text
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