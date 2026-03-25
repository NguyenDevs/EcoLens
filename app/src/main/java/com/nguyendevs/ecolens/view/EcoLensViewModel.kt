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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** ViewModel chính quản lý trạng thái UI, lịch sử nhận diện và chat AI. */
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
    val totalHistoryCount: Flow<Int> = historyManager.getTotalHistoryCount()

    val currentChatSessionId: Long?
        get() = chatManager.currentSessionId

    private var lastLanguageCode: String = "en"
    private var lastLat: Double = 16.0544
    private var lastLng: Double = 108.2022

    private val translationCache = android.util.LruCache<Int, Pair<String, SpeciesInfo>>(10)

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading.asStateFlow()

    /** Khởi tạo ViewModel, tải lịch sử và session chat. */
    init {
        viewModelScope.launch {
            try {
                _isHistoryLoading.value = true
                historyRepository.fetchHistory()
            } finally {
                _isHistoryLoading.value = false
                historyManager.repairMissingImagesOnce()
            }
        }

        viewModelScope.launch { chatRepository.fetchSessionsAndMessages() }
    }

    /** Lấy bản dịch ngôn ngữ đã lưu trong cache. */
    fun getCachedTranslation(historyId: Int, targetLang: String): SpeciesInfo? {
        val cached = translationCache.get(historyId)
        return if (cached != null && cached.first == targetLang) {
            cached.second
        } else {
            null
        }
    }

    /** Lưu bản dịch mới vào cache. */
    fun saveTranslationToCache(historyId: Int, language: String, info: SpeciesInfo) {
        translationCache.put(historyId, language to info)
    }

    /** Bắt đầu nhận diện loài từ ảnh. */
    fun identifySpecies(
            imageUri: Uri,
            languageCode: String,
            lat: Double = 16.0544,
            lng: Double = 108.2022,
            existingHistoryId: Int? = null
    ) {
        this.currentImageUri = imageUri
        lastLanguageCode = languageCode
        lastLat = lat
        lastLng = lng
        
        _uiState.value = EcoLensUiState(isLoading = true, loadingStage = LoadingStage.NONE)
        
        viewModelScope.launch {
            speciesManager.identifySpecies(
                    imageUri = imageUri,
                    languageCode = languageCode,
                    existingHistoryId = existingHistoryId,
                    lat = lat,
                    lng = lng,
                    onStateUpdate = { state -> _uiState.value = state }
            )
        }
    }

    /** Thử lại quá trình nhận diện với ảnh và cấu hình trước đó. */
    fun retryIdentification() {
        speciesManager.currentImageUri?.let { uri ->
            identifySpecies(
                    imageUri = uri,
                    languageCode = lastLanguageCode,
                    lat = lastLat,
                    lng = lastLng,
                    existingHistoryId = speciesManager.currentHistoryEntryId
            )
        }
    }

    /** Khởi tạo một phiên chat mới với tin nhắn chào mừng. */
    fun initNewChatSession(welcomeMessage: String, defaultTitle: String) {
        viewModelScope.launch { chatManager.initNewChatSession(welcomeMessage, defaultTitle) }
    }

    /** Tải lại phiên chat theo ID. */
    fun loadChatSession(sessionId: Long) {
        chatManager.loadChatSession(sessionId)
    }

    /** Gửi tin nhắn mới của người dùng vào chat. */
    fun sendChatMessage(userMessage: String, defaultTitle: String) {
        viewModelScope.launch { chatManager.sendChatMessage(userMessage, defaultTitle) }
    }

    /** Yêu cầu AI tạo lại phản hồi mới. */
    fun renewAiResponse(aiMessage: ChatMessage) {
        viewModelScope.launch { chatManager.renewAiResponse(aiMessage) }
    }

    /** Xóa một phiên chat. */
    fun deleteChatSession(sessionId: Long) {
        viewModelScope.launch { chatManager.deleteChatSession(sessionId) }
    }

    /** Bắt đầu phiên chat hoàn toàn mới. */
    fun startNewChatSession() {
        chatManager.startNewChatSession()
    }

    private val _historySortOption = MutableStateFlow(HistorySortOption.NEWEST_FIRST)
    val historySortOption: StateFlow<HistorySortOption> = _historySortOption.asStateFlow()
    
    private val _historySearchQuery = MutableStateFlow("")
    private val _historyCategory = MutableStateFlow("")
    val historyCategory: StateFlow<String> = _historyCategory.asStateFlow()
    
    private val _historyStartDate = MutableStateFlow<Long?>(null)
    val historyStartDate: StateFlow<Long?> = _historyStartDate.asStateFlow()
    
    private val _historyEndDate = MutableStateFlow<Long?>(null)
    val historyEndDate: StateFlow<Long?> = _historyEndDate.asStateFlow()
    
    private val _historyLimit = MutableStateFlow(20)

    /** Luồng dữ liệu lịch sử phản ứng theo các điều kiện lọc, tìm kiếm và sắp xếp. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val historyList: Flow<List<HistoryEntry>> = combine(
        _historySortOption,
        _historySearchQuery,
        _historyCategory,
        _historyStartDate,
        _historyEndDate,
        _historyLimit
    ) { arrayOfParams ->
        HistoryParams(
            sort = arrayOfParams[0] as HistorySortOption,
            search = arrayOfParams[1] as String,
            category = arrayOfParams[2] as String,
            start = arrayOfParams[3] as Long?,
            end = arrayOfParams[4] as Long?,
            limit = arrayOfParams[5] as Int
        )
    }.flatMapLatest { params ->
        historyManager.getHistoryBySortOption(
            params.sort,
            params.start,
            params.end,
            params.limit,
            params.category,
            params.search
        )
    }.flowOn(kotlinx.coroutines.Dispatchers.IO)

    private data class HistoryParams(
        val sort: HistorySortOption,
        val search: String,
        val category: String,
        val start: Long?,
        val end: Long?,
        val limit: Int
    )

    /** Cập nhật các tham số truy vấn lịch sử. */
    fun updateHistoryFilter(
        sort: HistorySortOption? = null,
        search: String? = null,
        category: String? = null,
        start: Long? = null,
        end: Long? = null,
        limit: Int? = null,
        resetLimit: Boolean = false
    ) {
        sort?.let { _historySortOption.value = it }
        search?.let { _historySearchQuery.value = it }
        category?.let { _historyCategory.value = it }
        start?.let { _historyStartDate.value = it }
        end?.let { _historyEndDate.value = it }
        limit?.let { _historyLimit.value = it }
        if (resetLimit) _historyLimit.value = 20
    }

    /** Truy xuất lịch sử theo tùy chọn sắp xếp và khoảng thời gian (Legacy support). */
    fun getHistoryBySortOption(
            sortOption: HistorySortOption,
            startDate: Long? = null,
            endDate: Long? = null,
            limit: Int = 20
    ): Flow<List<HistoryEntry>> {
        return historyManager.getHistoryBySortOption(sortOption, startDate, endDate, limit)
    }

    /** Thay đổi trạng thái yêu thích của mục lịch sử. */
    fun toggleFavorite(entry: HistoryEntry) {
        viewModelScope.launch {
            historyManager.toggleFavorite(entry)
            if (_uiState.value.historyId == entry.id) {
                val nextState = !entry.isFavorite
                _uiState.update { currentState ->
                    currentState.copy(isFavorite = nextState)
                }
            }
        }
    }

    /** Xóa một mục lịch sử. */
    fun deleteHistory(entry: HistoryEntry) {
        viewModelScope.launch { historyManager.deleteHistory(entry) }
    }

    /** Xóa toàn bộ lịch sử. */
    fun deleteAllHistory() {
        viewModelScope.launch { historyManager.deleteAllHistory() }
    }

    /** Đặt lại trạng thái màn hình về mặc định. */
    fun resetState() {
        _uiState.value = EcoLensUiState()
        currentImageUri = null
        lastLanguageCode = "en"
        translationCache.evictAll()
    }

    override fun onCleared() {
        super.onCleared()
        historyRepository.cleanup()
    }
}
