package com.nguyendevs.ecolens.view

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nguyendevs.ecolens.database.ChatRepository
import com.nguyendevs.ecolens.database.HistoryDatabase
import com.nguyendevs.ecolens.database.HistoryRepository
import com.nguyendevs.ecolens.managers.chat.ChatSessionManager
import com.nguyendevs.ecolens.managers.gemini.SpeciesIdentificationManager
import com.nguyendevs.ecolens.managers.history.HistoryManager
import com.nguyendevs.ecolens.models.*
import com.nguyendevs.ecolens.models.chat.ChatMessage
import com.nguyendevs.ecolens.models.chat.ChatSession
import com.nguyendevs.ecolens.models.history.HistoryEntry
import com.nguyendevs.ecolens.models.history.HistorySortOption
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EcoLensViewModel(application: Application) : AndroidViewModel(application) {

    private val historyDao by lazy { HistoryDatabase.getDatabase(application).historyDao() }
    private val chatDao by lazy { HistoryDatabase.getDatabase(application).chatDao() }

    private val historyRepository by lazy {
        HistoryRepository(historyDao, application.applicationContext)
    }
    private val chatRepository by lazy { ChatRepository(chatDao, application.applicationContext) }

    private val speciesManager by lazy {
        SpeciesIdentificationManager(application.applicationContext, historyRepository)
    }
    private val historyManager by lazy {
        HistoryManager(application.applicationContext, historyRepository)
    }
    private val chatManager by lazy { ChatSessionManager(chatRepository, chatDao, viewModelScope) }

    private val _uiState = MutableStateFlow(EcoLensUiState())
    val uiState: StateFlow<EcoLensUiState> = _uiState.asStateFlow()
    var currentImageUri: Uri? = null

    val chatMessages: StateFlow<List<ChatMessage>> = chatManager.chatMessages
    val isStreamingActive: StateFlow<Boolean> = chatManager.isStreamingActive
    val allChatSessions: Flow<List<ChatSession>> = chatManager.allChatSessions

    private var lastLanguageCode: String = "en"

    private val translationCache = mutableMapOf<Int, Pair<String, SpeciesInfo>>()

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _isHistoryLoading.value = true
            historyRepository.fetchHistory()
            _isHistoryLoading.value = false
            historyManager.repairMissingImagesOnce()
        }

        viewModelScope.launch { chatRepository.fetchSessionsAndMessages() }
    }

    fun getCachedTranslation(historyId: Int, targetLang: String): SpeciesInfo? {
        val cached = translationCache[historyId]
        return if (cached != null && cached.first == targetLang) {
            cached.second
        } else {
            null
        }
    }

    fun saveTranslationToCache(historyId: Int, language: String, info: SpeciesInfo) {
        translationCache[historyId] = language to info
    }

    fun identifySpecies(imageUri: Uri, languageCode: String, existingHistoryId: Int? = null) {
        this.currentImageUri = imageUri
        lastLanguageCode = languageCode
        viewModelScope.launch {
            speciesManager.identifySpecies(
                    imageUri = imageUri,
                    languageCode = languageCode,
                    existingHistoryId = existingHistoryId,
                    onStateUpdate = { state -> _uiState.value = state }
            )
        }
    }

    fun retryIdentification() {
        speciesManager.currentImageUri?.let { uri ->
            identifySpecies(
                    imageUri = uri,
                    languageCode = lastLanguageCode,
                    existingHistoryId = speciesManager.currentHistoryEntryId
            )
        }
    }

    fun initNewChatSession(welcomeMessage: String, defaultTitle: String) {
        viewModelScope.launch { chatManager.initNewChatSession(welcomeMessage, defaultTitle) }
    }

    fun loadChatSession(sessionId: Long) {
        chatManager.loadChatSession(sessionId)
    }

    fun sendChatMessage(userMessage: String, defaultTitle: String) {
        viewModelScope.launch { chatManager.sendChatMessage(userMessage, defaultTitle) }
    }

    fun renewAiResponse(aiMessage: ChatMessage) {
        viewModelScope.launch { chatManager.renewAiResponse(aiMessage) }
    }

    fun deleteChatSession(sessionId: Long) {
        viewModelScope.launch { chatManager.deleteChatSession(sessionId) }
    }

    fun startNewChatSession() {
        chatManager.startNewChatSession()
    }

    fun getHistoryBySortOption(
            sortOption: HistorySortOption,
            startDate: Long? = null,
            endDate: Long? = null,
            limit: Int = 20
    ): Flow<List<HistoryEntry>> {
        return historyManager.getHistoryBySortOption(sortOption, startDate, endDate, limit)
    }

    fun deleteHistory(entry: HistoryEntry) {
        viewModelScope.launch { historyManager.deleteHistory(entry) }
    }

    fun deleteAllHistory() {
        viewModelScope.launch { historyManager.deleteAllHistory() }
    }
}
