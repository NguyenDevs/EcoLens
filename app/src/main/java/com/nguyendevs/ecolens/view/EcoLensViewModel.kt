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
import com.nguyendevs.ecolens.model.*
import com.nguyendevs.ecolens.model.chat.ChatMessage
import com.nguyendevs.ecolens.model.chat.ChatSession
import com.nguyendevs.ecolens.model.history.HistoryEntry
import com.nguyendevs.ecolens.model.history.HistorySortOption
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel chính cho ứng dụng EcoLens
 * Quản lý nhận diện loài, lịch sử và chat
 */
class EcoLensViewModel(application: Application) : AndroidViewModel(application) {

    private val historyDao by lazy { HistoryDatabase.getDatabase(application).historyDao() }
    private val chatDao by lazy { HistoryDatabase.getDatabase(application).chatDao() }

    private val historyRepository by lazy { HistoryRepository(historyDao, application.applicationContext) }
    private val chatRepository by lazy { ChatRepository(chatDao, application.applicationContext) }

    private val speciesManager by lazy {
        SpeciesIdentificationManager(
            application.applicationContext,
            historyRepository
        )
    }
    private val historyManager by lazy { HistoryManager(application.applicationContext, historyRepository) }
    private val chatManager by lazy { ChatSessionManager(chatRepository, chatDao, viewModelScope) }

    private val _uiState = MutableStateFlow(EcoLensUiState())
    val uiState: StateFlow<EcoLensUiState> = _uiState.asStateFlow()
    var currentImageUri: Uri? = null

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

    // ==================== NHẬN DIỆN LOÀI ====================

    /**
     * Nhận diện loài từ ảnh
     *
     * @param imageUri URI của ảnh
     * @param languageCode Mã ngôn ngữ
     * @param existingHistoryId ID lịch sử hiện có (nếu có)
     */
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

    /**
     * Thử lại nhận diện với ảnh hiện tại
     */
    fun retryIdentification() {
        speciesManager.currentImageUri?.let { uri ->
            identifySpecies(
                imageUri = uri,
                languageCode = lastLanguageCode,
                existingHistoryId = speciesManager.currentHistoryEntryId
            )
        }
    }

    // ==================== CHỨC NĂNG CHAT ====================

    /**
     * Khởi tạo phiên chat mới với tin nhắn chào mừng
     */
    fun initNewChatSession(welcomeMessage: String, defaultTitle: String) {
        viewModelScope.launch {
            chatManager.initNewChatSession(welcomeMessage, defaultTitle)
        }
    }

    /**
     * Tải một phiên chat đã có
     */
    fun loadChatSession(sessionId: Long) {
        chatManager.loadChatSession(sessionId)
    }

    /**
     * Gửi tin nhắn chat từ người dùng
     */
    fun sendChatMessage(userMessage: String, defaultTitle: String) {
        viewModelScope.launch {
            chatManager.sendChatMessage(userMessage, defaultTitle)
        }
    }

    /**
     * Tạo lại phản hồi AI cho một tin nhắn
     */
    fun renewAiResponse(aiMessage: ChatMessage) {
        viewModelScope.launch {
            chatManager.renewAiResponse(aiMessage)
        }
    }

    /**
     * Xóa một phiên chat
     */
    fun deleteChatSession(sessionId: Long) {
        viewModelScope.launch {
            chatManager.deleteChatSession(sessionId)
        }
    }

    /**
     * Bắt đầu phiên chat mới
     */
    fun startNewChatSession() {
        chatManager.startNewChatSession()
    }

    // ==================== CHỨC NĂNG LỊCH SỬ ====================

    /**
     * Lấy lịch sử theo tùy chọn sắp xếp và khoảng thời gian
     */
    fun getHistoryBySortOption(
        sortOption: HistorySortOption,
        startDate: Long? = null,
        endDate: Long? = null
    ): Flow<List<HistoryEntry>> {
        return historyManager.getHistoryBySortOption(sortOption, startDate, endDate)
    }

    /**
     * Bật/tắt đánh dấu yêu thích cho một mục lịch sử

    fun toggleFavorite(entry: HistoryEntry) {
        viewModelScope.launch {
            historyManager.toggleFavorite(entry)
        }
    }
     */

    /**
     * Xóa một mục lịch sử
     */
    fun deleteHistory(entry: HistoryEntry) {
        viewModelScope.launch {
            historyManager.deleteHistory(entry)
        }
    }

    /**
     * Xóa toàn bộ lịch sử
     */
    fun deleteAllHistory() {
        viewModelScope.launch {
            historyManager.deleteAllHistory()
        }
    }
}