package com.nguyendevs.ecolens.view

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.api.GeminiContent
import com.nguyendevs.ecolens.api.GeminiPart
import com.nguyendevs.ecolens.api.GeminiRequest
import com.nguyendevs.ecolens.database.HistoryDatabase
import com.nguyendevs.ecolens.database.ChatDao
import com.nguyendevs.ecolens.model.*
import com.nguyendevs.ecolens.network.RetrofitClient
import com.nguyendevs.ecolens.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val chatDao = HistoryDatabase.getDatabase(application).chatDao()
    private var messageCollectionJob: Job? = null

    private val _uiState = MutableStateFlow(EcoLensUiState())
    private var currentSessionId: Long? = null
    val allChatSessions: Flow<List<ChatSession>> = chatDao.getAllSessions()
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
            _uiState.value = EcoLensUiState(isLoading = true, loadingStage = LoadingStage.NONE)

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

                    val initialInfo = SpeciesInfo(
                        scientificName = scientificName,
                        confidence = confidence
                    )

                    _uiState.value = EcoLensUiState(
                        isLoading = true,
                        speciesInfo = initialInfo,
                        loadingStage = LoadingStage.SCIENTIFIC_NAME
                    )

                    delay(300)

                    val infoWithCommonName = initialInfo.copy(
                        commonName = taxon.preferred_common_name?.ifEmpty { taxon.name } ?: taxon.name,
                        rank = taxon.rank
                    )

                    _uiState.value = EcoLensUiState(
                        isLoading = true,
                        speciesInfo = infoWithCommonName,
                        loadingStage = LoadingStage.COMMON_NAME
                    )

                    delay(300)

                    val speciesInfo = fetchDetailsFromGemini(scientificName, confidence, languageCode)

                    val infoWithTaxonomy = infoWithCommonName.copy(
                        kingdom = speciesInfo.kingdom,
                        phylum = speciesInfo.phylum,
                        className = speciesInfo.className,
                        order = speciesInfo.order,
                        family = speciesInfo.family,
                        genus = speciesInfo.genus,
                        species = speciesInfo.species
                    )

                    _uiState.value = EcoLensUiState(
                        isLoading = true,
                        speciesInfo = infoWithTaxonomy,
                        loadingStage = LoadingStage.TAXONOMY
                    )

                    delay(300)

                    speciesInfo.description.takeIf { it.isNotEmpty() }?.let { desc ->
                        _uiState.value = EcoLensUiState(
                            isLoading = true,
                            speciesInfo = infoWithTaxonomy.copy(description = desc),
                            loadingStage = LoadingStage.DESCRIPTION
                        )
                        delay(300)
                    }

                    speciesInfo.characteristics.takeIf { it.isNotEmpty() }?.let { chars ->
                        val current = _uiState.value.speciesInfo
                        _uiState.value = EcoLensUiState(
                            isLoading = true,
                            speciesInfo = current?.copy(characteristics = chars),
                            loadingStage = LoadingStage.CHARACTERISTICS
                        )
                        delay(300)
                    }

                    speciesInfo.distribution.takeIf { it.isNotEmpty() }?.let { dist ->
                        val current = _uiState.value.speciesInfo
                        _uiState.value = EcoLensUiState(
                            isLoading = true,
                            speciesInfo = current?.copy(distribution = dist),
                            loadingStage = LoadingStage.DISTRIBUTION
                        )
                        delay(300)
                    }

                    speciesInfo.habitat.takeIf { it.isNotEmpty() }?.let { hab ->
                        val current = _uiState.value.speciesInfo
                        _uiState.value = EcoLensUiState(
                            isLoading = true,
                            speciesInfo = current?.copy(habitat = hab),
                            loadingStage = LoadingStage.HABITAT
                        )
                        delay(300)
                    }

                    speciesInfo.conservationStatus.takeIf { it.isNotEmpty() }?.let { cons ->
                        val current = _uiState.value.speciesInfo
                        _uiState.value = EcoLensUiState(
                            isLoading = true,
                            speciesInfo = current?.copy(conservationStatus = cons),
                            loadingStage = LoadingStage.CONSERVATION
                        )
                        delay(300)
                    }

                    val finalInfo = _uiState.value.speciesInfo?.copy(
                        commonName = speciesInfo.commonName.ifEmpty {
                            taxon.preferred_common_name ?: scientificName
                        }
                    ) ?: speciesInfo.copy(
                        scientificName = scientificName,
                        confidence = confidence
                    )

                    _uiState.value = EcoLensUiState(
                        isLoading = false,
                        speciesInfo = finalInfo,
                        loadingStage = LoadingStage.COMPLETE
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

                } else {
                    _uiState.value = EcoLensUiState(
                        isLoading = false,
                        error = context.getString(R.string.error_no_result)
                    )
                }

                withContext(Dispatchers.IO) {
                    if (imageFile.exists()) imageFile.delete()
                }

            } catch (e: Exception) {
                _uiState.value = EcoLensUiState(
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
                family = parseToHtml(removeRankPrefix(rawInfo.family, if (isVietnamese) "Họ" else "Family")),
                genus = parseToHtml(removeRankPrefix(rawInfo.genus, if (isVietnamese) "Chi" else "Genus")),
                species = parseToHtml(removeRankPrefix(rawInfo.species, if (isVietnamese) "Loài" else "Species")),

                commonName = rawInfo.commonName,
                scientificName = scientificName,
                rank = rawInfo.rank,

                description = parseToHtml(rawInfo.description),
                characteristics = parseToHtml(characteristicsText),
                distribution = parseToHtml(rawInfo.distribution),
                habitat = parseToHtml(rawInfo.habitat),
                conservationStatus = parseToHtml(rawInfo.conservationStatus, isConservationStatus = true, isVietnamese = isVietnamese),
                confidence = confidence
            )

        } catch (e: Exception) {
            val errorMsg = if (languageCode == "en") "An error occurred" else "Đã xảy ra lỗi"
            SpeciesInfo(commonName = scientificName, scientificName = scientificName, description = errorMsg, confidence = confidence)
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
            - Dùng ~~ để đánh dấu text nghiêng (ví dụ: ~~tên thường~~)
            - Dùng • cho bullet points, mỗi dòng một ý
            
            === ĐỊNH DẠNG JSON ===
            {
              "commonName": "$commonNameDesc",
              "kingdom": "Chỉ tên Tiếng Việt",
              "phylum": "Chỉ tên Tiếng Việt",
              "className": "Chỉ tên Tiếng Việt",
              "order": "Chỉ tên Tiếng Việt",
              "family": "Tên khoa học ~~(tên thường)~~",
              "genus": "Tên khoa học ~~(tên thường)~~",
              "species": "Tên khoa học ~~(tên thường)~~",
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
            - Use ~~ for italic text (e.g., ~~common name~~)
            - Use • for bullet points, one point per line
            
            === JSON FORMAT ===
            {
              "commonName": "$commonNameDesc",
              "kingdom": "Name only",
              "phylum": "Name only",
              "className": "Name only",
              "order": "Name only",
              "family": "Scientific name ~~(common name)~~",
              "genus": "Scientific name ~~(common name)~~",
              "species": "Scientific name ~~(common name)~~",
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

    private fun parseToHtml(text: String, isConservationStatus: Boolean = false, isVietnamese: Boolean = true): String {
        if (text.isBlank()) return ""

        var result = text
            .replace(Regex("^(#{1,6})\\s+(.+)$", RegexOption.MULTILINE)) { matchResult ->
                "<br><b>${matchResult.groupValues[2]}</b>"
            }
            .replace(Regex("\\*\\*(.+?)\\*\\*")) { "<b>${it.groupValues[1]}</b>" }
            .replace(Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")) { "<i>${it.groupValues[1]}</i>" }
            .replace(Regex("~~(.+?)~~")) { "<i>${it.groupValues[1]}</i>" }
            .replace(Regex("##(.+?)##")) { "<font color='#00796B'><b>${it.groupValues[1]}</b></font>" }
            .replace(Regex("^\\*\\s+", RegexOption.MULTILINE)) { "• " }
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
        return json.replace("```json", "", ignoreCase = true)
            .replace("```", "", ignoreCase = true)
            .trim()
            .let { text ->
                val start = text.indexOf('{')
                val end = text.lastIndexOf('}')
                if (start >= 0 && end > start) text.substring(start, end + 1) else text
            }
    }

    private fun removeRankPrefix(text: String, prefix: String): String {
        return text.trim().replaceFirst(Regex("^(?i)$prefix\\s*[:\\-\\s]+"), "")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            .trim()
    }

    private fun startMessageCollection(sessionId: Long) {
        messageCollectionJob?.cancel()
        messageCollectionJob = viewModelScope.launch {
            chatDao.getMessagesBySession(sessionId).collect { messages ->
                _chatMessages.value = messages
            }
        }
    }

    fun loadChatSession(sessionId: Long) {
        currentSessionId = sessionId
        startMessageCollection(sessionId)
    }

    fun startNewChatSession() {
        currentSessionId = null
        messageCollectionJob?.cancel()
        _chatMessages.value = emptyList()
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

    fun sendChatMessage(userMessage: String, defaultTitle: String) {
        if (userMessage.isBlank()) return
        val sessionId = currentSessionId ?: return

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

            val loadingMsg = ChatMessage(sessionId = -1, content = "...", isUser = false, isLoading = true)
            _chatMessages.value = _chatMessages.value + loadingMsg

            try {
                val currentHistory = _chatMessages.value.filter { !it.isLoading && it.sessionId == sessionId }.toMutableList()
                currentHistory.add(userChatMsg)

                val geminiContents = currentHistory.map { msg ->
                    val role = if (msg.isUser) "user" else "model"
                    GeminiContent(role = role, parts = listOf(GeminiPart(msg.content)))
                }

                val cantreply = getApplication<Application>().getString(R.string.cant_reply)
                val request = GeminiRequest(contents = geminiContents)
                val response = apiService.askGemini(request)
                val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: cantreply

                val formattedReply = parseToHtml(reply).replace("\n", "<br>")

                val botChatMsg = ChatMessage(sessionId = sessionId, content = formattedReply, isUser = false, timestamp = System.currentTimeMillis())
                chatDao.insertMessage(botChatMsg)

                chatDao.updateSession(chatDao.getSessionById(sessionId)!!.copy(lastMessage = reply, timestamp = System.currentTimeMillis()))

            } catch (e: Exception) {
                e.printStackTrace()
                val errorMsg = ChatMessage(sessionId = sessionId, content = "Lỗi kết nối: ${e.message}", isUser = false)
                chatDao.insertMessage(errorMsg)
            }
        }
    }

    fun deleteChatSession(sessionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatDao.deleteMessagesBySession(sessionId)
                chatDao.deleteSession(sessionId)

                Log.d("EcoLensViewModel", "Deleted session $sessionId successfully")

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
}