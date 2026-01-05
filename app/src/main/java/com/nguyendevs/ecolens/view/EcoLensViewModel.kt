package com.nguyendevs.ecolens.view

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nguyendevs.ecolens.database.ChatRepository
import com.nguyendevs.ecolens.database.HistoryDatabase
import com.nguyendevs.ecolens.database.HistoryRepository
import com.nguyendevs.ecolens.managers.*
import com.nguyendevs.ecolens.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EcoLensViewModel(application: Application) : AndroidViewModel(application) {

    private val historyDao by lazy { HistoryDatabase.getDatabase(application).historyDao() }
    private val chatDao by lazy { HistoryDatabase.getDatabase(application).chatDao() }

    private val historyRepository by lazy { HistoryRepository(historyDao, application.applicationContext) }
    private val chatRepository by lazy { ChatRepository(chatDao, application.applicationContext) }

    // Managers
    private val speciesManager by lazy { SpeciesIdentificationManager(application.applicationContext, historyRepository) }

    // Fix: HistoryManager constructor now requires Context
    private val historyManager by lazy { HistoryManager(application.applicationContext, historyRepository) }

    private val chatManager by lazy { ChatSessionManager(chatRepository, chatDao, viewModelScope) }

    // UI State
    private val _uiState = MutableStateFlow(EcoLensUiState())
    val uiState: StateFlow<EcoLensUiState> = _uiState.asStateFlow()
    var currentImageUri: Uri? = null

    // Chat State
    val chatMessages: StateFlow<List<ChatMessage>> = chatManager.chatMessages
    val isStreamingActive: StateFlow<Boolean> = chatManager.isStreamingActive
    val allChatSessions: Flow<List<ChatSession>> = chatManager.allChatSessions

    private var lastLanguageCode: String = "en"

    init {
        viewModelScope.launch {
            historyRepository.fetchHistory()
            chatRepository.fetchSessionsAndMessages()
        }
    }

    // ==================== SPECIES IDENTIFICATION ====================

    fun identifySpecies(imageUri: Uri, languageCode: String, existingHistoryId: Int? = null) {
        this.currentImageUri = imageUri
        lastLanguageCode = languageCode
        viewModelScope.launch {
            speciesManager.identifySpecies(
                imageUri = imageUri,
                languageCode = languageCode,
                existingHistoryId = existingHistoryId,
                onStateUpdate = { state ->
                    _uiState.value = state
                }
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

    // ==================== CHAT FUNCTIONS ====================

    fun initNewChatSession(welcomeMessage: String, defaultTitle: String) {
        viewModelScope.launch {
            chatManager.initNewChatSession(welcomeMessage, defaultTitle)
        }
    }

    fun loadChatSession(sessionId: Long) {
        chatManager.loadChatSession(sessionId)
    }

    fun sendChatMessage(userMessage: String, defaultTitle: String) {
        viewModelScope.launch {
            chatManager.sendChatMessage(userMessage, defaultTitle)
        }
    }

    fun renewAiResponse(aiMessage: ChatMessage) {
        viewModelScope.launch {
            chatManager.renewAiResponse(aiMessage)
        }
    }

    fun deleteChatSession(sessionId: Long) {
        viewModelScope.launch {
            chatManager.deleteChatSession(sessionId)
        }
    }

    fun startNewChatSession() {
        chatManager.startNewChatSession()
    }

    // ==================== HISTORY FUNCTIONS ====================

    fun getHistoryBySortOption(
        sortOption: HistorySortOption,
        startDate: Long? = null,
        endDate: Long? = null
    ): Flow<List<HistoryEntry>> {
        return historyManager.getHistoryBySortOption(sortOption, startDate, endDate)
    }

    fun toggleFavorite(entry: HistoryEntry) {
        viewModelScope.launch {
            historyManager.toggleFavorite(entry)
        }
    }

    fun deleteAllHistory() {
        viewModelScope.launch {
            historyManager.deleteAllHistory()
        }
    }
}