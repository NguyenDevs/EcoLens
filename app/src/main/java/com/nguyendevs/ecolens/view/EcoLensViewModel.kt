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

    // Lưu thông tin để retry
    private var currentImageUri: Uri? = null
    private var currentHistoryEntryId: Int? = null

    private data class GeminiRawResponse(
        val commonName: String? = null,
        val scientificName: String? = null,
        val kingdom: String? = null,
        val phylum: String? = null,
        val className: String? = null,
        val taxorder: String? = null,
        val family: String? = null,
        val genus: String? = null,
        val species: String? = null,
        val rank: String? = null,
        val description: String? = null,
        val characteristics: Any? = null,
        val distribution: String? = null,
        val habitat: String? = null,
        val conservationStatus: String? = null,
        val confidence: Double = 0.0
    )

    fun identifySpecies(imageUri: Uri, languageCode: String, existingHistoryId: Int? = null) {
        viewModelScope.launch {
            _uiState.value = EcoLensUiState(isLoading = true, speciesInfo = null, error = null, loadingStage = LoadingStage.NONE)
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

                val response = apiService.identifySpecies(image = imagePart, locale = languageCode)

                if (response.results.isNotEmpty()) {
                    val topResult = response.results.first()
                    val taxon = topResult.taxon
                    val scientificName = taxon.name
                    val confidence = topResult.combined_score

                    _uiState.value = EcoLensUiState(
                        isLoading = true,
                        speciesInfo = SpeciesInfo(
                            scientificName = scientificName,
                            confidence = confidence,
                            commonName = "..."
                        ),
                        loadingStage = LoadingStage.SCIENTIFIC_NAME
                    )
                    delay(400)

                    val geminiInfo = fetchDetailsFromGemini(scientificName, confidence, languageCode)

                    // Kiểm tra xem có dữ liệu đầy đủ không
                    val hasCompleteData = geminiInfo.commonName.isNotEmpty() &&
                            geminiInfo.description.isNotEmpty()

                    if (!hasCompleteData) {
                        _uiState.value = EcoLensUiState(
                            isLoading = false,
                            error = context.getString(R.string.error_incomplete_data)
                        )
                        return@launch
                    }

                    val finalCommonName = geminiInfo.commonName.ifEmpty {
                        taxon.preferred_common_name ?: scientificName
                    }

                    _uiState.value = _uiState.value.copy(
                        speciesInfo = _uiState.value.speciesInfo?.copy(
                            commonName = finalCommonName,
                            rank = taxon.rank
                        ),
                        loadingStage = LoadingStage.COMMON_NAME
                    )
                    delay(300)

                    val taxonomyStages = listOf(
                        { info: SpeciesInfo -> info.copy(kingdom = geminiInfo.kingdom) },
                        { info: SpeciesInfo -> info.copy(phylum = geminiInfo.phylum) },
                        { info: SpeciesInfo -> info.copy(className = geminiInfo.className) },
                        { info: SpeciesInfo -> info.copy(taxorder = geminiInfo.taxorder) },
                        { info: SpeciesInfo -> info.copy(family = geminiInfo.family) },
                        { info: SpeciesInfo -> info.copy(genus = geminiInfo.genus) },
                        { info: SpeciesInfo -> info.copy(species = geminiInfo.species) }
                    )

                    taxonomyStages.forEach { transform ->
                        val updatedInfo = transform(_uiState.value.speciesInfo!!)
                        _uiState.value = _uiState.value.copy(
                            speciesInfo = updatedInfo,
                            loadingStage = LoadingStage.TAXONOMY
                        )
                        delay(200)
                    }

                    val contentStages = listOf(
                        Pair(LoadingStage.DESCRIPTION, geminiInfo.description),
                        Pair(LoadingStage.CHARACTERISTICS, geminiInfo.characteristics),
                        Pair(LoadingStage.DISTRIBUTION, geminiInfo.distribution),
                        Pair(LoadingStage.HABITAT, geminiInfo.habitat),
                        Pair(LoadingStage.CONSERVATION, geminiInfo.conservationStatus)
                    )

                    var currentInfo = _uiState.value.speciesInfo
                    contentStages.forEach { (stage, content) ->
                        if (content.isNotEmpty()) {
                            currentInfo = when(stage) {
                                LoadingStage.DESCRIPTION -> currentInfo?.copy(description = content)
                                LoadingStage.CHARACTERISTICS -> currentInfo?.copy(characteristics = content)
                                LoadingStage.DISTRIBUTION -> currentInfo?.copy(distribution = content)
                                LoadingStage.HABITAT -> currentInfo?.copy(habitat = content)
                                LoadingStage.CONSERVATION -> currentInfo?.copy(conservationStatus = content)
                                else -> currentInfo
                            }
                            _uiState.value = _uiState.value.copy(
                                speciesInfo = currentInfo,
                                loadingStage = stage
                            )
                            delay(300)
                        }
                    }

                    _uiState.value = _uiState.value.copy(isLoading = false, loadingStage = LoadingStage.COMPLETE)

                    // Kiểm tra thông tin hợp lệ trước khi lưu
                    val infoToSave = currentInfo ?: geminiInfo
                    val isValidInfo = infoToSave.commonName.isNotEmpty() &&
                            infoToSave.commonName != "..." &&
                            infoToSave.commonName != "N/A" &&
                            !infoToSave.description.contains("An error occurred", ignoreCase = true) &&
                            !infoToSave.description.contains("Đã xảy ra lỗi", ignoreCase = true)

                    if (isValidInfo) {
                        withContext(Dispatchers.IO) {
                            val savedPath = if (existingHistoryId != null) {
                                // Lấy path cũ từ DB
                                historyDao.getHistoryById(existingHistoryId)?.imagePath
                            } else {
                                ImageUtils.saveBitmapToInternalStorage(context, imageFile)
                            }

                            if (savedPath != null) {
                                if (existingHistoryId != null) {
                                    val info = currentInfo ?: geminiInfo
                                    historyDao.updateSpeciesDetails(
                                        id = existingHistoryId,
                                        commonName = info.commonName,
                                        scientificName = info.scientificName,
                                        kingdom = info.kingdom,
                                        phylum = info.phylum,
                                        className = info.className,
                                        taxorder = info.taxorder,
                                        family = info.family,
                                        genus = info.genus,
                                        species = info.species,
                                        rank = info.rank,
                                        description = info.description,
                                        characteristics = info.characteristics,
                                        distribution = info.distribution,
                                        habitat = info.habitat,
                                        conservationStatus = info.conservationStatus,
                                        confidence = info.confidence,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    currentHistoryEntryId = existingHistoryId
                                } else {
                                    // Tạo entry mới
                                    val newId = historyDao.insert(HistoryEntry(
                                        imagePath = savedPath,
                                        speciesInfo = currentInfo ?: geminiInfo,
                                        timestamp = System.currentTimeMillis()
                                    ))
                                    currentHistoryEntryId = newId.toInt()
                                }
                            }
                        }
                    }
                } else {
                    _uiState.value = EcoLensUiState(isLoading = false, error = context.getString(R.string.error_no_result))
                }
            } catch (e: Exception) {
                val context = getApplication<Application>()
                val errorMsg = when {
                    e.message?.contains("429") == true -> context.getString(R.string.error_quota_exceeded)
                    else -> context.getString(R.string.error_general, e.message)
                }
                _uiState.value = EcoLensUiState(isLoading = false, error = errorMsg)
            }
        }
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

                val welcomeMsg = ChatMessage(sessionId = newId, content = welcomeMessage, isUser = false, timestamp = System.currentTimeMillis())
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
            val userChatMsg = ChatMessage(sessionId = sessionId, content = userMessage, isUser = true, timestamp = System.currentTimeMillis())
            chatDao.insertMessage(userChatMsg)

            val currentSession = chatDao.getSessionById(sessionId)
            val newTitle = if (currentSession?.title == defaultTitle) {
                userMessage.take(30) + "..."
            } else {
                currentSession?.title ?: "Chat"
            }
            chatDao.updateSession(currentSession!!.copy(title = newTitle, lastMessage = userMessage, timestamp = System.currentTimeMillis()))

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

    private suspend fun fetchDetailsFromGemini(
        scientificName: String,
        confidence: Double,
        languageCode: String
    ): SpeciesInfo = withContext(Dispatchers.IO) {
        try {
            val isVietnamese = languageCode != "en"
            val prompt = buildGeminiPrompt(scientificName, isVietnamese)

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = prompt))
                    )
                )
            )
            val response = apiService.askGemini(request)

            val jsonString = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanedJson = cleanJsonString(jsonString)

            val rawInfo = gson.fromJson(cleanedJson, GeminiRawResponse::class.java)

            val characteristicsText = when (val chars = rawInfo.characteristics) {
                is String -> chars
                is List<*> -> chars.joinToString("\n")
                else -> ""
            }

            SpeciesInfo(
                // Thay thế formatTaxonomyRank bằng thẻ <b> trực tiếp
                kingdom = "<b>" + removeRankPrefix(rawInfo.kingdom, if (isVietnamese) "Giới" else "Kingdom") + "</b>",
                phylum = "<b>" + removeRankPrefix(rawInfo.phylum, if (isVietnamese) "Ngành" else "Phylum") + "</b>",
                className = "<b>" + removeRankPrefix(rawInfo.className, if (isVietnamese) "Lớp" else "Class") + "</b>",
                taxorder = "<b>" + removeRankPrefix(rawInfo.taxorder, if (isVietnamese) "Bộ" else "Order") + "</b>",
                family = "<b>" + removeRankPrefix(rawInfo.family, if (isVietnamese) "Họ" else "Family") + "</b>",
                genus = "<b>" + removeRankPrefix(rawInfo.genus, if (isVietnamese) "Chi" else "Genus") + "</b>",
                species = "<b>" + removeRankPrefix(rawInfo.species, if (isVietnamese) "Loài" else "Species") + "</b>",

                commonName = rawInfo.commonName,
                scientificName = scientificName,
                rank = rawInfo.rank,

                description = processMarkdown(rawInfo.description, isVietnamese = isVietnamese),
                characteristics = processMarkdown(characteristicsText, isVietnamese = isVietnamese),
                distribution = processMarkdown(rawInfo.distribution, isVietnamese = isVietnamese),
                habitat = processMarkdown(rawInfo.habitat, isVietnamese = isVietnamese),
                conservationStatus = processMarkdown(rawInfo.conservationStatus, isConservationStatus = true, isVietnamese = isVietnamese),
                confidence = confidence
            )

        } catch (e: Exception) {
            val errorMsg = if (languageCode == "en") "An error occurred" else "Đã xảy ra lỗi"
            SpeciesInfo(commonName = scientificName, scientificName = scientificName, description = errorMsg, confidence = confidence)
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
                                    val chunk = streamResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

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

    private fun buildGeminiPrompt(scientificName: String, isVietnamese: Boolean): String {
        val commonNameDesc = if (isVietnamese) "Tên thường gọi Tiếng Việt chuẩn nhất" else "Common name in English"

        return if (isVietnamese) {
            """
            Bạn là nhà sinh vật học. Cung cấp thông tin chi tiết về loài "$scientificName" bằng Tiếng Việt.
             
            === QUY TẮC FORMAT ===
            - Dùng ** để đánh dấu text cần in đậm (ví dụ: **từ khóa**)
            - Dùng ## để đánh dấu text cần màu xanh highlight (ví dụ: ##Việt Nam##, ##50-60cm##)
            - Dùng • cho bullet points, mỗi dòng một ý
             
            === ĐỊNH DẠNG JSON ===
            {
              "commonName": "$commonNameDesc",
              "kingdom": "Chỉ tên Tiếng Việt",
              "phylum": "Chỉ tên Tiếng Việt",
              "className": "Chỉ tên Tiếng Việt",
              "taxorder": "Chỉ tên Tiếng Việt",
              "family": "Tên khoa học",
              "genus": "Tên khoa học",
              "species": "Tên khoa học",
              "rank": "Cấp phân loại",
              "description": "Tổng quan 4 câu ngắn gọn, dùng **in đậm** cho đặc điểm nổi bật và ##xanh đậm## cho địa danh, tên riêng, số đo.",
              "characteristics": "Danh sách gạch đầu dòng, mỗi dòng bắt đầu với • và một ý về hình thái, kích thước, màu sắc. Dùng **in đậm** và ##xanh đậm##.",
              "distribution": "Ưu tiên Việt Nam trước (nếu có), sau đó toàn cầu. Dùng ##xanh đậm## cho tên địa danh.",
              "habitat": "Mô tả chi tiết môi trường sống.",
              "conservationStatus": "Chỉ ghi một trong các trạng thái: Cực kỳ nguy cấp, Nguy cấp, Sách Đỏ Việt Nam, Sắp nguy cấp, Ít lo ngại, Chưa đánh giá. Thêm một chút thông tin bổ sung từ IUCN nếu có."
            }
             
            CHỈ TRẢ VỀ JSON, KHÔNG THÊM TEXT KHÁC.
            """.trimIndent()
        } else {
            """
            You are a biologist. Provide details about "$scientificName" in English.
             
            === FORMAT RULES ===
            - Use ** for bold text (e.g., **keyword**)
            - Use ## for green highlight (e.g., ##Vietnam##, ##50-60cm##)
            - Use • for bullet points, one point per line
             
            === JSON FORMAT ===
            {
              "commonName": "$commonNameDesc",
              "kingdom": "Name only",
              "phylum": "Name only",
              "className": "Name only",
              "taxorder": "Name only",
              "family": "Scientific name",
              "genus": "Scientific name",
              "species": "Scientific name",
              "rank": "Rank",
              "description": "4-sentence overview with **bold** for key features and ##green highlight## for places/names/measurements.",
              "characteristics": "Bullet list, each line starts with • covering morphology, size, colors. Use **bold** and ##green highlight##.",
              "distribution": "Vietnam first (if applicable), then worldwide. Use ##green highlight## for locations.",
              "habitat": "Specific environment details.",
              "conservationStatus": "Only write one of these statuses: Critically Endangered, Endangered, Vulnerable (Vietnam Red Data Book), Near Threatened, Least Concern, Not Evaluated. Add some additional IUCN info if available."
            }
             
            RETURN ONLY JSON, NO ADDITIONAL TEXT.
            """.trimIndent()
        }
    }



    private fun processMarkdown(text: String?, isConservationStatus: Boolean = false, isVietnamese: Boolean = true): String {
        if (text.isBlank()) return ""

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
        return text.trim().replaceFirst(Regex("^(?i)$prefix\\s*[:\\-\\s]+"), "")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            .trim()
    }
}