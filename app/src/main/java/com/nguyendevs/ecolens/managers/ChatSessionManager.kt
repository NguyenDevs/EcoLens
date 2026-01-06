package com.nguyendevs.ecolens.managers

import android.util.Log
import com.google.gson.Gson
import com.nguyendevs.ecolens.api.*
import com.nguyendevs.ecolens.database.ChatDao
import com.nguyendevs.ecolens.database.ChatRepository
import com.nguyendevs.ecolens.model.ChatMessage
import com.nguyendevs.ecolens.model.ChatSession
import com.nguyendevs.ecolens.network.RetrofitClient
import com.nguyendevs.ecolens.utils.MarkdownProcessor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Manager quản lý chat sessions và streaming responses từ Gemini API
 * Hỗ trợ tạo session mới, gửi tin nhắn, streaming response và renew AI messages
 */
class ChatSessionManager(
    private val chatRepository: ChatRepository,
    private val chatDao: ChatDao,
    private val scope: CoroutineScope
) {

    private val gson by lazy { Gson() }
    private val markdownProcessor = MarkdownProcessor()
    private val apiService = RetrofitClient.iNaturalistApi

    var currentSessionId: Long? = null
    private var messageCollectionJob: Job? = null
    private val isGenerating = AtomicBoolean(false)
    private val streamingMessageId = AtomicLong(-1L)

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isStreamingActive = MutableStateFlow(false)
    val isStreamingActive: StateFlow<Boolean> = _isStreamingActive.asStateFlow()

    val allChatSessions: Flow<List<ChatSession>> = chatDao.getAllSessions()

    companion object {
        private const val TAG = "ChatSessionManager"
        private const val PREFIX_DATA = "data: "
        private const val STREAM_DONE = "[DONE]"
        private const val DEFAULT_CHAT_TITLE = "Chat"
        private const val TITLE_MAX_LENGTH = 30
        private const val PREVIEW_MAX_LENGTH = 100
        private const val STREAM_UPDATE_DELAY = 50L
    }

    // ==================== SESSION MANAGEMENT ====================

    /**
     * Khởi tạo chat session mới với welcome message
     * Reuse session trống nếu có, nếu không tạo session mới
     */
    suspend fun initNewChatSession(welcomeMessage: String, defaultTitle: String) {
        currentSessionId = null
        messageCollectionJob?.cancel()
        _chatMessages.value = emptyList()

        withContext(Dispatchers.IO) {
            val latestSession = chatDao.getLatestSession()
            var sessionToReuseId: Long? = null

            if (latestSession != null) {
                val userMsgCount = chatDao.getUserMessageCount(latestSession.id)
                if (userMsgCount == 0) {
                    sessionToReuseId = latestSession.id
                    chatRepository.updateSession(
                        latestSession.copy(timestamp = System.currentTimeMillis())
                    )
                }
            }

            if (sessionToReuseId != null) {
                currentSessionId = sessionToReuseId
                withContext(Dispatchers.Main) {
                    startMessageCollection(sessionToReuseId)
                }
            } else {
                createNewSession(welcomeMessage, defaultTitle)
            }
        }
    }

    /**
     * Tạo session mới với welcome message
     */
    private suspend fun createNewSession(welcomeMessage: String, defaultTitle: String) {
        val newSession = ChatSession(
            title = defaultTitle,
            lastMessage = welcomeMessage,
            timestamp = System.currentTimeMillis()
        )
        val newId = chatRepository.insertSession(newSession)
        currentSessionId = newId

        val welcomeMsg = ChatMessage(
            sessionId = newId,
            content = welcomeMessage,
            isUser = false,
            timestamp = System.currentTimeMillis()
        )
        chatRepository.insertMessage(welcomeMsg)

        withContext(Dispatchers.Main) {
            startMessageCollection(newId)
        }
    }

    /**
     * Load session đã tồn tại
     */
    fun loadChatSession(sessionId: Long) {
        currentSessionId = sessionId
        startMessageCollection(sessionId)
    }

    /**
     * Bắt đầu session mới (reset state)
     */
    fun startNewChatSession() {
        currentSessionId = null
        messageCollectionJob?.cancel()
        _chatMessages.value = emptyList()
    }

    /**
     * Xóa chat session và tất cả messages liên quan
     */
    suspend fun deleteChatSession(sessionId: Long) {
        withContext(Dispatchers.IO) {
            try {
                chatRepository.deleteSession(sessionId)
                if (currentSessionId == sessionId) {
                    withContext(Dispatchers.Main) {
                        currentSessionId = null
                        messageCollectionJob?.cancel()
                        _chatMessages.value = emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Delete failed: ${e.message}", e)
            }
        }
    }

    // ==================== MESSAGE OPERATIONS ====================

    /**
     * Gửi tin nhắn từ user và nhận streaming response từ AI
     */
    suspend fun sendChatMessage(userMessage: String, defaultTitle: String) {
        if (userMessage.isBlank()) return
        val sessionId = currentSessionId ?: return
        if (isGenerating.getAndSet(true)) return

        withContext(Dispatchers.IO) {
            val userChatMsg = ChatMessage(
                sessionId = sessionId,
                content = userMessage,
                isUser = true,
                timestamp = System.currentTimeMillis()
            )
            chatRepository.insertMessage(userChatMsg)

            updateSessionTitleAndPreview(sessionId, userMessage, defaultTitle)
            executeGeminiStreamingFlow(sessionId)
        }
    }

    /**
     * Tạo lại AI response cho một message
     */
    suspend fun renewAiResponse(aiMessage: ChatMessage) {
        if (isGenerating.getAndSet(true)) return
        val sessionId = currentSessionId ?: return.also { isGenerating.set(false) }

        withContext(Dispatchers.IO) {
            try {
                val reuseId = aiMessage.id
                chatRepository.deleteMessage(aiMessage, reorder = false)
                executeGeminiStreamingFlow(sessionId, reuseId)
            } catch (e: Exception) {
                isGenerating.set(false)
                Log.e(TAG, "Renew failed: ${e.message}")
            }
        }
    }

    /**
     * Bắt đầu collect messages từ database cho session
     */
    private fun startMessageCollection(sessionId: Long) {
        messageCollectionJob?.cancel()
        messageCollectionJob = scope.launch {
            chatDao.getMessagesBySession(sessionId)
                .flowOn(Dispatchers.IO)
                .collect { messages ->
                    _chatMessages.value = messages
                }
        }
    }

    // ==================== GEMINI STREAMING ====================

    /**
     * Thực hiện streaming call đến Gemini API
     * Stream response và update message real-time
     */
    private suspend fun executeGeminiStreamingFlow(
        sessionId: Long,
        reuseMessageId: Long? = null
    ) {
        _isStreamingActive.value = true

        val tempMessage = ChatMessage(
            id = reuseMessageId ?: 0,
            sessionId = sessionId,
            content = "",
            isUser = false,
            isStreaming = true,
            timestamp = System.currentTimeMillis()
        )

        val messageId = if (reuseMessageId != null) {
            chatRepository.insertMessage(tempMessage)
            reuseMessageId
        } else {
            chatRepository.insertMessage(tempMessage)
        }

        streamingMessageId.set(messageId)

        try {
            val currentHistory = buildConversationHistory(sessionId)
            val request = GeminiRequest(contents = currentHistory)
            val response = apiService.streamGemini(request)

            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    processStreamingResponse(responseBody, sessionId, messageId)
                }
            } else {
                throw Exception("API error: ${response.code()}")
            }

        } catch (e: Exception) {
            handleStreamingError(e, sessionId, messageId)
        } finally {
            _isStreamingActive.value = false
            isGenerating.set(false)
            streamingMessageId.set(-1L)
        }
    }

    /**
     * Build conversation history để gửi lên Gemini API
     */
    private suspend fun buildConversationHistory(sessionId: Long): List<GeminiContent> {
        val currentHistory = chatDao.getMessagesBySession(sessionId).first()
            .filter { !it.isStreaming }

        return currentHistory.map { msg ->
            val role = if (msg.isUser) "user" else "model"
            GeminiContent(role = role, parts = listOf(GeminiPart(msg.content)))
        }
    }

    /**
     * Xử lý streaming response từ Gemini API
     * Parse từng chunk và update message real-time
     */
    private suspend fun processStreamingResponse(
        responseBody: okhttp3.ResponseBody,
        sessionId: Long,
        messageId: Long
    ) {
        val accumulatedText = StringBuilder()

        responseBody.byteStream().bufferedReader().use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                if (currentLine.startsWith(PREFIX_DATA)) {
                    val jsonData = currentLine.substring(PREFIX_DATA.length).trim()
                    if (jsonData == STREAM_DONE) break

                    processStreamChunk(jsonData, accumulatedText, messageId)
                }
            }
        }

        finalizeStreamingMessage(accumulatedText, sessionId, messageId)
    }

    /**
     * Xử lý một chunk từ streaming response
     */
    private suspend fun processStreamChunk(
        jsonData: String,
        accumulatedText: StringBuilder,
        messageId: Long
    ) {
        try {
            val streamResponse = gson.fromJson(jsonData, GeminiResponse::class.java)
            val chunk = streamResponse.candidates?.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text

            if (!chunk.isNullOrEmpty()) {
                accumulatedText.append(chunk)
                val formattedText = markdownProcessor.process(accumulatedText.toString())
                chatDao.updateMessageContent(messageId, formattedText)
                delay(STREAM_UPDATE_DELAY)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}")
        }
    }

    /**
     * Finalize streaming message sau khi nhận đủ response
     */
    private suspend fun finalizeStreamingMessage(
        accumulatedText: StringBuilder,
        sessionId: Long,
        messageId: Long
    ) {
        val finalFormattedText = markdownProcessor.process(accumulatedText.toString())
        chatRepository.updateMessage(
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
            chatRepository.updateSession(
                it.copy(
                    lastMessage = accumulatedText.take(PREVIEW_MAX_LENGTH).toString(),
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Xử lý lỗi khi streaming
     */
    private suspend fun handleStreamingError(
        e: Exception,
        sessionId: Long,
        messageId: Long
    ) {
        e.printStackTrace()
        val errorMsg = "Lỗi kết nối: ${e.message}"
        chatRepository.updateMessage(
            ChatMessage(
                id = messageId,
                sessionId = sessionId,
                content = errorMsg,
                isUser = false,
                isStreaming = false,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    // ==================== HELPER METHODS ====================

    /**
     * Cập nhật title và preview của session
     */
    private suspend fun updateSessionTitleAndPreview(
        sessionId: Long,
        userMessage: String,
        defaultTitle: String
    ) {
        val currentSession = chatDao.getSessionById(sessionId)
        val newTitle = if (currentSession?.title == defaultTitle) {
            userMessage.take(TITLE_MAX_LENGTH) + "..."
        } else {
            currentSession?.title ?: DEFAULT_CHAT_TITLE
        }
        chatRepository.updateSession(
            currentSession!!.copy(
                title = newTitle,
                lastMessage = userMessage,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}