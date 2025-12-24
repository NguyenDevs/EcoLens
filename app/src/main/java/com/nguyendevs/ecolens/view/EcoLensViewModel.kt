package com.nguyendevs.ecolens.view

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.api.*
import com.nguyendevs.ecolens.database.HistoryDatabase
import com.nguyendevs.ecolens.model.*
import com.nguyendevs.ecolens.network.RetrofitClient
import com.nguyendevs.ecolens.utils.ImageUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class EcoLensViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.iNaturalistApi
    private val historyDao = HistoryDatabase.getDatabase(application).historyDao()
    private val chatDao = HistoryDatabase.getDatabase(application).chatDao()
    private val gson = Gson()

    private val _uiState = MutableStateFlow(EcoLensUiState())
    val uiState: StateFlow<EcoLensUiState> = _uiState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isStreamingActive = MutableStateFlow(false)
    val isStreamingActive: StateFlow<Boolean> = _isStreamingActive.asStateFlow()

    val allChatSessions: Flow<List<ChatSession>> = chatDao.getAllSessions()

    private var messageCollectionJob: Job? = null
    private var currentSessionId: Long? = null
    private val isGenerating = AtomicBoolean(false)
    private val streamingMessageId = AtomicLong(-1L)

    private var currentImageUri: Uri? = null
    private var currentHistoryEntryId: Int? = null

    // Response models for streaming
    private data class TaxonomyResponse(
        val commonName: String? = null,
        val kingdom: String? = null,
        val phylum: String? = null,
        val className: String? = null,
        val taxorder: String? = null,
        val family: String? = null,
        val genus: String? = null,
        val species: String? = null,
        val rank: String? = null
    )

    private data class DetailsResponse(
        val description: String? = null,
        val characteristics: Any? = null,
        val distribution: String? = null,
        val habitat: String? = null,
        val conservationStatus: String? = null
    )

    // ==================== SPECIES IDENTIFICATION ====================

    fun identifySpecies(imageUri: Uri, languageCode: String, existingHistoryId: Int? = null) {
        viewModelScope.launch {
            _uiState.value = EcoLensUiState(
                isLoading = true,
                speciesInfo = null,
                error = null,
                loadingStage = LoadingStage.NONE
            )
            delay(100)

            currentImageUri = imageUri
            currentHistoryEntryId = existingHistoryId

            try {
                val context = getApplication<Application>()
                val imageFile = withContext(Dispatchers.Default) {
                    ImageUtils.uriToFile(context, imageUri, 1024)
                }

                val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

                val response = apiService.identifySpecies(
                    image = imagePart,
                    locale = languageCode
                )

                if (response.results.isNotEmpty()) {
                    val topResult = response.results.first()
                    val taxon = topResult.taxon
                    val scientificName = taxon.name
                    val confidence = topResult.combined_score

                    // Hiển thị scientific name ngay lập tức
                    _uiState.value = EcoLensUiState(
                        isLoading = true,
                        speciesInfo = SpeciesInfo(
                            scientificName = scientificName,
                            confidence = confidence,
                            commonName = "..."
                        ),
                        loadingStage = LoadingStage.SCIENTIFIC_NAME
                    )

                    // STREAMING PHASE 1: Taxonomy (nhanh)
                    streamTaxonomy(scientificName, confidence, languageCode)

                    // STREAMING PHASE 2: Details (chi tiết)
                    streamDetails(scientificName, confidence, languageCode)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        loadingStage = LoadingStage.COMPLETE
                    )

                    // Lưu vào database
                    saveToHistory(existingHistoryId, context, imageFile)

                } else {
                    _uiState.value = EcoLensUiState(
                        isLoading = false,
                        error = context.getString(R.string.error_no_result)
                    )
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private suspend fun streamTaxonomy(
        scientificName: String,
        confidence: Double,
        languageCode: String
    ) {
        val isVietnamese = languageCode != "en"
        val prompt = buildTaxonomyPrompt(scientificName, isVietnamese)

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = prompt))
                )
            )
        )

        try {
            val response = apiService.streamGemini(request)

            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    var accumulatedJson = ""

                    responseBody.byteStream().bufferedReader().use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val currentLine = line ?: continue
                            if (currentLine.startsWith("data: ")) {
                                val jsonData = currentLine.substring(6).trim()
                                if (jsonData == "[DONE]") break

                                try {
                                    val streamResponse = gson.fromJson(jsonData, GeminiResponse::class.java)
                                    val chunk = streamResponse.candidates?.firstOrNull()
                                        ?.content?.parts?.firstOrNull()?.text

                                    if (!chunk.isNullOrEmpty()) {
                                        accumulatedJson += chunk

                                        // Thử parse ngay khi có đủ data
                                        val cleanedJson = cleanJsonString(accumulatedJson)
                                        try {
                                            val taxonomyInfo = gson.fromJson(
                                                cleanedJson,
                                                TaxonomyResponse::class.java
                                            )

                                            // Update UI ngay khi có data
                                            updateTaxonomyUI(taxonomyInfo, isVietnamese)

                                        } catch (e: Exception) {
                                            // Chưa đủ JSON, tiếp tục accumulate
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("StreamTaxonomy", "Parse error: ${e.message}")
                                }
                            }
                        }
                    }

                    // Final parse
                    val cleanedJson = cleanJsonString(accumulatedJson)
                    val taxonomyInfo = gson.fromJson(cleanedJson, TaxonomyResponse::class.java)
                    updateTaxonomyUI(taxonomyInfo, isVietnamese)
                }
            }
        } catch (e: Exception) {
            Log.e("StreamTaxonomy", "Error: ${e.message}")
        }
    }

    private suspend fun streamDetails(
        scientificName: String,
        confidence: Double,
        languageCode: String
    ) {
        val isVietnamese = languageCode != "en"
        val prompt = buildDetailsPrompt(scientificName, isVietnamese)

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = prompt))
                )
            )
        )

        try {
            val response = apiService.streamGemini(request)

            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    var accumulatedJson = ""

                    responseBody.byteStream().bufferedReader().use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val currentLine = line ?: continue
                            if (currentLine.startsWith("data: ")) {
                                val jsonData = currentLine.substring(6).trim()
                                if (jsonData == "[DONE]") break

                                try {
                                    val streamResponse = gson.fromJson(jsonData, GeminiResponse::class.java)
                                    val chunk = streamResponse.candidates?.firstOrNull()
                                        ?.content?.parts?.firstOrNull()?.text

                                    if (!chunk.isNullOrEmpty()) {
                                        accumulatedJson += chunk

                                        val cleanedJson = cleanJsonString(accumulatedJson)
                                        try {
                                            val detailsInfo = gson.fromJson(
                                                cleanedJson,
                                                DetailsResponse::class.java
                                            )

                                            // Update UI progressively
                                            updateDetailsUI(detailsInfo, isVietnamese)

                                        } catch (e: Exception) {
                                            // Continue accumulating
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("StreamDetails", "Parse error: ${e.message}")
                                }
                            }
                        }
                    }

                    // Final parse
                    val cleanedJson = cleanJsonString(accumulatedJson)
                    val detailsInfo = gson.fromJson(cleanedJson, DetailsResponse::class.java)
                    updateDetailsUI(detailsInfo, isVietnamese)
                }
            }
        } catch (e: Exception) {
            Log.e("StreamDetails", "Error: ${e.message}")
        }
    }

    private suspend fun updateTaxonomyUI(taxonomy: TaxonomyResponse, isVietnamese: Boolean) {
        withContext(Dispatchers.Main) {
            val current = _uiState.value.speciesInfo ?: return@withContext

            var updated = current

            // Update common name
            if (!taxonomy.commonName.isNullOrBlank() && taxonomy.commonName != "...") {
                updated = updated.copy(commonName = taxonomy.commonName)
                _uiState.value = _uiState.value.copy(
                    speciesInfo = updated,
                    loadingStage = LoadingStage.COMMON_NAME
                )
                delay(200)
            }

            // Update taxonomy fields progressively
            val taxonomyUpdates = listOf(
                { info: SpeciesInfo ->
                    taxonomy.kingdom?.let {
                        info.copy(kingdom = "<b>${removeRankPrefix(it, if (isVietnamese) "Giới" else "Kingdom")}</b>")
                    } ?: info
                },
                { info: SpeciesInfo ->
                    taxonomy.phylum?.let {
                        info.copy(phylum = "<b>${removeRankPrefix(it, if (isVietnamese) "Ngành" else "Phylum")}</b>")
                    } ?: info
                },
                { info: SpeciesInfo ->
                    taxonomy.className?.let {
                        info.copy(className = "<b>${removeRankPrefix(it, if (isVietnamese) "Lớp" else "Class")}</b>")
                    } ?: info
                },
                { info: SpeciesInfo ->
                    taxonomy.taxorder?.let {
                        info.copy(taxorder = "<b>${removeRankPrefix(it, if (isVietnamese) "Bộ" else "Order")}</b>")
                    } ?: info
                },
                { info: SpeciesInfo ->
                    taxonomy.family?.let {
                        info.copy(family = "<b>${removeRankPrefix(it, if (isVietnamese) "Họ" else "Family")}</b>")
                    } ?: info
                },
                { info: SpeciesInfo ->
                    taxonomy.genus?.let {
                        info.copy(genus = "<b>${removeRankPrefix(it, if (isVietnamese) "Chi" else "Genus")}</b>")
                    } ?: info
                },
                { info: SpeciesInfo ->
                    taxonomy.species?.let {
                        info.copy(species = "<b>${removeRankPrefix(it, if (isVietnamese) "Loài" else "Species")}</b>")
                    } ?: info
                }
            )

            taxonomyUpdates.forEach { transform ->
                updated = transform(updated)
                _uiState.value = _uiState.value.copy(
                    speciesInfo = updated,
                    loadingStage = LoadingStage.TAXONOMY
                )
                delay(150)
            }
        }
    }

    private suspend fun updateDetailsUI(details: DetailsResponse, isVietnamese: Boolean) {
        withContext(Dispatchers.Main) {
            val current = _uiState.value.speciesInfo ?: return@withContext
            var updated = current

            val characteristicsText = when (val chars = details.characteristics) {
                is String -> chars
                is List<*> -> chars.joinToString("\n")
                else -> ""
            }

            // Update each detail section as it arrives
            val detailUpdates = listOf(
                Triple(LoadingStage.DESCRIPTION, details.description) { info: SpeciesInfo, content: String ->
                    info.copy(description = processMarkdown(content, isVietnamese = isVietnamese))
                },
                Triple(LoadingStage.CHARACTERISTICS, characteristicsText) { info: SpeciesInfo, content: String ->
                    info.copy(characteristics = processMarkdown(content, isVietnamese = isVietnamese))
                },
                Triple(LoadingStage.DISTRIBUTION, details.distribution) { info: SpeciesInfo, content: String ->
                    info.copy(distribution = processMarkdown(content, isVietnamese = isVietnamese))
                },
                Triple(LoadingStage.HABITAT, details.habitat) { info: SpeciesInfo, content: String ->
                    info.copy(habitat = processMarkdown(content, isVietnamese = isVietnamese))
                },
                Triple(LoadingStage.CONSERVATION, details.conservationStatus) { info: SpeciesInfo, content: String ->
                    info.copy(conservationStatus = processMarkdown(content, isConservationStatus = true, isVietnamese = isVietnamese))
                }
            )

            detailUpdates.forEach { (stage, content, transform) ->
                if (!content.isNullOrBlank()) {
                    updated = transform(updated, content)
                    _uiState.value = _uiState.value.copy(
                        speciesInfo = updated,
                        loadingStage = stage
                    )
                    delay(200)
                }
            }
        }
    }

    private fun buildTaxonomyPrompt(scientificName: String, isVietnamese: Boolean): String {
        return if (isVietnamese) {
            """
            Cung cấp thông tin phân loại khoa học về "$scientificName" bằng Tiếng Việt.
            
            Trả về JSON với format:
            {
              "commonName": "Tên thường gọi Tiếng Việt",
              "kingdom": "Tên Tiếng Việt",
              "phylum": "Tên Tiếng Việt",
              "className": "Tên Tiếng Việt",
              "taxorder": "Tên Tiếng Việt",
              "family": "Tên khoa học",
              "genus": "Tên khoa học",
              "species": "Tên khoa học",
              "rank": "Cấp phân loại"
            }
            
            CHỈ TRẢ VỀ JSON, KHÔNG THÊM TEXT KHÁC.
            """.trimIndent()
        } else {
            """
            Provide taxonomic classification for "$scientificName" in English.
            
            Return JSON format:
            {
              "commonName": "Common name",
              "kingdom": "Kingdom name",
              "phylum": "Phylum name",
              "className": "Class name",
              "taxorder": "Order name",
              "family": "Family name",
              "genus": "Genus name",
              "species": "Species name",
              "rank": "Rank"
            }
            
            RETURN ONLY JSON, NO ADDITIONAL TEXT.
            """.trimIndent()
        }
    }

    private fun buildDetailsPrompt(scientificName: String, isVietnamese: Boolean): String {
        return if (isVietnamese) {
            """
            Cung cấp thông tin chi tiết về "$scientificName" bằng Tiếng Việt.
            
            === QUY TẮC FORMAT ===
            - Dùng ** để in đậm (ví dụ: **từ khóa**)
            - Dùng ## để highlight xanh (ví dụ: ##Việt Nam##)
            - Dùng • cho bullet points
            
            === JSON FORMAT ===
            {
              "description": "Tổng quan 4 câu, dùng **in đậm** và ##xanh đậm##",
              "characteristics": "Danh sách gạch đầu dòng, mỗi dòng bắt đầu với •",
              "distribution": "Ưu tiên Việt Nam, dùng ##xanh đậm## cho địa danh",
              "habitat": "Mô tả môi trường sống",
              "conservationStatus": "Một trong: Cực kỳ nguy cấp, Nguy cấp, Sách Đỏ Việt Nam, Sắp nguy cấp, Ít lo ngại, Chưa đánh giá"
            }
            
            CHỈ TRẢ VỀ JSON.
            """.trimIndent()
        } else {
            """
            Provide detailed information about "$scientificName" in English.
            
            === FORMAT RULES ===
            - Use ** for bold (e.g., **keyword**)
            - Use ## for green highlight (e.g., ##Vietnam##)
            - Use • for bullet points
            
            === JSON FORMAT ===
            {
              "description": "4-sentence overview with **bold** and ##green##",
              "characteristics": "Bullet list, each line starts with •",
              "distribution": "Vietnam first if applicable, use ##green## for locations",
              "habitat": "Environment details",
              "conservationStatus": "One of: Critically Endangered, Endangered, Vulnerable, Near Threatened, Least Concern, Not Evaluated"
            }
            
            RETURN ONLY JSON.
            """.trimIndent()
        }
    }

    private suspend fun saveToHistory(existingHistoryId: Int?, context: Application, imageFile: java.io.File) {
        val currentInfo = _uiState.value.speciesInfo ?: return

        val isValidInfo = currentInfo.commonName.isNotEmpty() &&
                currentInfo.commonName != "..." &&
                currentInfo.commonName != "N/A" &&
                !currentInfo.description.contains("An error occurred", ignoreCase = true) &&
                !currentInfo.description.contains("Đã xảy ra lỗi", ignoreCase = true)

        if (isValidInfo) {
            withContext(Dispatchers.IO) {
                val savedPath = if (existingHistoryId != null) {
                    historyDao.getHistoryById(existingHistoryId)?.imagePath
                } else {
                    ImageUtils.saveBitmapToInternalStorage(context, imageFile)
                }

                if (savedPath != null) {
                    if (existingHistoryId != null) {
                        historyDao.updateSpeciesDetails(
                            id = existingHistoryId,
                            commonName = currentInfo.commonName,
                            scientificName = currentInfo.scientificName,
                            kingdom = currentInfo.kingdom,
                            phylum = currentInfo.phylum,
                            className = currentInfo.className,
                            taxorder = currentInfo.taxorder,
                            family = currentInfo.family,
                            genus = currentInfo.genus,
                            species = currentInfo.species,
                            rank = currentInfo.rank,
                            description = currentInfo.description,
                            characteristics = currentInfo.characteristics,
                            distribution = currentInfo.distribution,
                            habitat = currentInfo.habitat,
                            conservationStatus = currentInfo.conservationStatus,
                            confidence = currentInfo.confidence,
                            timestamp = System.currentTimeMillis()
                        )
                        currentHistoryEntryId = existingHistoryId
                    } else {
                        val newId = historyDao.insert(HistoryEntry(
                            imagePath = savedPath,
                            speciesInfo = currentInfo,
                            timestamp = System.currentTimeMillis()
                        ))
                        currentHistoryEntryId = newId.toInt()
                    }
                }
            }
        }
    }

    private fun handleError(e: Exception) {
        val context = getApplication<Application>()
        val errorMsg = when {
            e.message?.contains("429") == true ->
                context.getString(R.string.error_quota_exceeded)
            else ->
                context.getString(R.string.error_general, e.message)
        }
        _uiState.value = EcoLensUiState(isLoading = false, error = errorMsg)
    }

    fun retryIdentification() {
        currentImageUri?.let { uri ->
            identifySpecies(
                imageUri = uri,
                languageCode = getApplication<Application>().resources.configuration.locales[0].language,
                existingHistoryId = currentHistoryEntryId
            )
        }
    }

    // ==================== CHAT FUNCTIONS ====================

    fun initNewChatSession(welcomeMessage: String, defaultTitle: String) {
        currentSessionId = null
        messageCollectionJob?.cancel()
        _chatMessages.value = emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            val latestSession = chatDao.getLatestSession()
            var sessionToReuseId: Long? = null

            if (latestSession != null) {
                val userMsgCount = chatDao.getUserMessageCount(latestSession.id)
                if (userMsgCount == 0) {
                    sessionToReuseId = latestSession.id
                    chatDao.updateSession(latestSession.copy(timestamp = System.currentTimeMillis()))
                }
            }

            if (sessionToReuseId != null) {
                currentSessionId = sessionToReuseId
                withContext(Dispatchers.Main) {
                    startMessageCollection(sessionToReuseId)
                }
            } else {
                val newSession = ChatSession(
                    title = defaultTitle,
                    lastMessage = welcomeMessage,
                    timestamp = System.currentTimeMillis()
                )
                val newId = chatDao.insertSession(newSession)
                currentSessionId = newId

                val welcomeMsg = ChatMessage(
                    sessionId = newId,
                    content = welcomeMessage,
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                chatDao.insertMessage(welcomeMsg)

                withContext(Dispatchers.Main) {
                    startMessageCollection(newId)
                }
            }
        }
    }

    fun loadChatSession(sessionId: Long) {
        currentSessionId = sessionId
        startMessageCollection(sessionId)
    }

    fun sendChatMessage(userMessage: String, defaultTitle: String) {
        if (userMessage.isBlank()) return
        val sessionId = currentSessionId ?: return

        if (isGenerating.getAndSet(true)) return

        viewModelScope.launch(Dispatchers.IO) {
            val userChatMsg = ChatMessage(
                sessionId = sessionId,
                content = userMessage,
                isUser = true,
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertMessage(userChatMsg)

            val currentSession = chatDao.getSessionById(sessionId)
            val newTitle = if (currentSession?.title == defaultTitle) {
                userMessage.take(30) + "..."
            } else {
                currentSession?.title ?: "Chat"
            }
            chatDao.updateSession(
                currentSession!!.copy(
                    title = newTitle,
                    lastMessage = userMessage,
                    timestamp = System.currentTimeMillis()
                )
            )

            executeGeminiStreamingFlow(sessionId)
        }
    }

    fun renewAiResponse(aiMessage: ChatMessage) {
        if (isGenerating.getAndSet(true)) return

        val sessionId = currentSessionId
        if (sessionId == null) {
            isGenerating.set(false)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatDao.deleteMessageById(aiMessage.id)
                executeGeminiStreamingFlow(sessionId)
            } catch (e: Exception) {
                isGenerating.set(false)
                Log.e("EcoLensViewModel", "Renew failed: ${e.message}")
            }
        }
    }

    fun deleteChatSession(sessionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatDao.deleteMessagesBySession(sessionId)
                chatDao.deleteSession(sessionId)
                if (currentSessionId == sessionId) {
                    withContext(Dispatchers.Main) {
                        currentSessionId = null
                        messageCollectionJob?.cancel()
                        _chatMessages.value = emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e("EcoLensViewModel", "Delete chat session failed: ${e.message}", e)
            }
        }
    }

    fun startNewChatSession() {
        currentSessionId = null
        messageCollectionJob?.cancel()
        _chatMessages.value = emptyList()
    }

    private fun startMessageCollection(sessionId: Long) {
        messageCollectionJob?.cancel()
        messageCollectionJob = viewModelScope.launch {
            chatDao.getMessagesBySession(sessionId).collect { messages ->
                _chatMessages.value = messages
            }
        }
    }

    private suspend fun executeGeminiStreamingFlow(sessionId: Long) {
        _isStreamingActive.value = true

        val tempMessage = ChatMessage(
            sessionId = sessionId,
            content = "",
            isUser = false,
            isStreaming = true,
            timestamp = System.currentTimeMillis()
        )

        val messageId = chatDao.insertMessage(tempMessage)
        streamingMessageId.set(messageId)

        try {
            val currentHistory = chatDao.getMessagesBySession(sessionId).first()
                .filter { !it.isStreaming }

            val geminiContents = currentHistory.map { msg ->
                val role = if (msg.isUser) "user" else "model"
                GeminiContent(role = role, parts = listOf(GeminiPart(msg.content)))
            }

            val request = GeminiRequest(contents = geminiContents)
            val response = apiService.streamGemini(request)

            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    var accumulatedText = ""

                    responseBody.byteStream().bufferedReader().use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val currentLine = line ?: continue
                            if (currentLine.startsWith("data: ")) {
                                val jsonData = currentLine.substring(6).trim()
                                if (jsonData == "[DONE]") break

                                try {
                                    val streamResponse = gson.fromJson(jsonData, GeminiResponse::class.java)
                                    val chunk = streamResponse.candidates?.firstOrNull()
                                        ?.content?.parts?.firstOrNull()?.text

                                    if (!chunk.isNullOrEmpty()) {
                                        accumulatedText += chunk
                                        val formattedText = processMarkdown(accumulatedText)
                                        chatDao.updateMessageContent(messageId, formattedText)
                                        delay(50)
                                    }
                                } catch (e: Exception) {
                                    Log.e("Streaming", "Parse error: ${e.message}")
                                }
                            }
                        }
                    }

                    val finalFormattedText = processMarkdown(accumulatedText)
                    chatDao.updateMessage(
                        ChatMessage(
                            id = messageId,
                            sessionId = sessionId,
                            content = finalFormattedText,
                            isUser = false,
                            isStreaming = false,
                            timestamp = System.currentTimeMillis()
                        )
                    )

                    val updatedSession = chatDao.getSessionById(sessionId)
                    updatedSession?.let {
                        chatDao.updateSession(it.copy(
                            lastMessage = accumulatedText.take(100),
                            timestamp = System.currentTimeMillis()
                        ))
                    }
                }
            } else {
                throw Exception("API error: ${response.code()}")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            val errorMsg = "Lỗi kết nối: ${e.message}"
            chatDao.updateMessage(
                ChatMessage(
                    id = messageId,
                    sessionId = sessionId,
                    content = errorMsg,
                    isUser = false,
                    isStreaming = false,
                    timestamp = System.currentTimeMillis()
                )
            )
        } finally {
            _isStreamingActive.value = false
            isGenerating.set(false)
            streamingMessageId.set(-1L)
        }
    }

    // ==================== HISTORY FUNCTIONS ====================

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

    fun toggleFavorite(entry: HistoryEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                historyDao.update(entry.copy(isFavorite = !entry.isFavorite))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            historyDao.deleteAll()
        }
    }

    // ==================== UTILITY FUNCTIONS ====================

    private fun processMarkdown(text: String?, isConservationStatus: Boolean = false, isVietnamese: Boolean = true): String {
        if (text.isNullOrBlank()) return ""

        var result = text
            .replace(Regex("\\*\\*(.+?)\\*\\*")) { "<b>${it.groupValues[1]}</b>" }
            .replace(Regex("##(.+?)##")) { "<font color='#00796B'><b>${it.groupValues[1]}</b></font>" }
            .replace(Regex("~~(.+?)~~")) { "<i>${it.groupValues[1]}</i>" }
            .replace("\n", "<br>")

        if (isConservationStatus) {
            result = colorizeConservationStatus(result, isVietnamese)
        }

        return result
    }

    private fun colorizeConservationStatus(text: String, isVietnamese: Boolean): String {
        val statusMap = if (isVietnamese) {
            mapOf(
                "Cực kỳ nguy cấp" to "#8B0000",
                "Nguy cấp" to "#8B0000",
                "Sách Đỏ Việt Nam" to "#c97408",
                "Sách Đỏ" to "#c97408",
                "Sắp nguy cấp" to "#eddb11",
                "Ít lo ngại" to "#55f200",
                "Chưa đánh giá" to "#05deff"
            )
        } else {
            mapOf(
                "Critically Endangered" to "#8B0000",
                "Endangered" to "#8B0000",
                "Vulnerable (Vietnam Red Data Book)" to "#c97408",
                "Vulnerable" to "#c97408",
                "Near Threatened" to "#eddb11",
                "Least Concern" to "#55f200",
                "Not Evaluated" to "#05deff"
            )
        }

        var result = text
        statusMap.entries.sortedByDescending { it.key.length }.forEach { (status, color) ->
            if (result.contains(status, ignoreCase = true)) {
                result = result.replace(
                    Regex("(?i)$status"),
                    "<font color='$color'><b>$status</b></font>"
                )
            }
        }
        return result
    }

    private fun cleanJsonString(json: String): String {
        val firstBrace = json.indexOf('{')
        val lastBrace = json.lastIndexOf('}')

        return if (firstBrace != -1 && lastBrace > firstBrace) {
            json.substring(firstBrace, lastBrace + 1)
        } else {
            json.replace("```json", "", ignoreCase = true)
                .replace("```", "", ignoreCase = true)
                .trim()
        }
    }

    private fun removeRankPrefix(text: String?, prefix: String): String {
        return text?.trim()?.replaceFirst(Regex("^(?i)$prefix\\s*[:\\-\\s]+"), "")
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            ?.trim() ?: ""
    }
}