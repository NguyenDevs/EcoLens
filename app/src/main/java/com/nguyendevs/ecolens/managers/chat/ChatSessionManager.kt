package com.nguyendevs.ecolens.managers.chat

import android.util.Log
import com.google.gson.Gson
import com.nguyendevs.ecolens.api.GeminiContent
import com.nguyendevs.ecolens.api.GeminiPart
import com.nguyendevs.ecolens.api.GeminiRequest
import com.nguyendevs.ecolens.api.GeminiResponse
import com.nguyendevs.ecolens.database.ChatDao
import com.nguyendevs.ecolens.database.ChatRepository
import com.nguyendevs.ecolens.models.chat.ChatMessage
import com.nguyendevs.ecolens.models.chat.ChatSession
import com.nguyendevs.ecolens.network.RetrofitClient
import com.nguyendevs.ecolens.utils.MarkdownProcessor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody

/** Quản lý phiên trò chuyện và tương tác với AI qua API. */
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
    private var lastStreamUpdateTime = 0L

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isStreamingActive = MutableStateFlow(false)
    val isStreamingActive: StateFlow<Boolean> = _isStreamingActive.asStateFlow()

    val allChatSessions: Flow<List<ChatSession>> = chatRepository.getAllSessions()

    companion object {
        private const val TAG = "ChatSessionManager"
        private const val PREFIX_DATA = "data: "
        private const val STREAM_DONE = "[DONE]"
        private const val DEFAULT_CHAT_TITLE = "Chat"
        private const val TITLE_MAX_LENGTH = 30
        private const val PREVIEW_MAX_LENGTH = 100
        private const val STREAM_UPDATE_DELAY = 50L
    }

    /** Khởi tạo phiên trò chuyện mới. */
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
                withContext(Dispatchers.Main) { startMessageCollection(sessionToReuseId) }
            } else {
                createNewSession(welcomeMessage, defaultTitle)
            }
        }
    }

    /** Tạo phiên mới với nội dung lời chào mở đầu. */
    private suspend fun createNewSession(welcomeMessage: String, defaultTitle: String) {
        val newSession =
                ChatSession(
                        title = defaultTitle,
                        lastMessage = welcomeMessage,
                        timestamp = System.currentTimeMillis()
                )
        val newId = chatRepository.insertSession(newSession)
        currentSessionId = newId

        val welcomeMsg =
                ChatMessage(
                        sessionId = newId,
                        content = welcomeMessage,
                        isUser = false,
                        timestamp = System.currentTimeMillis()
                )
        chatRepository.insertMessage(welcomeMsg)

        withContext(Dispatchers.Main) { startMessageCollection(newId) }
    }

    /** Tải dữ liệu của phiên hiện tại. */
    fun loadChatSession(sessionId: Long) {
        currentSessionId = sessionId
        startMessageCollection(sessionId)
    }

    /** Bắt đầu một phiên trò chuyện trống không tì vết. */
    fun startNewChatSession() {
        currentSessionId = null
        messageCollectionJob?.cancel()
        _chatMessages.value = emptyList()
    }

    /** Xóa phiên và toàn bộ tin nhắn liên quan lưu trong máy. */
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
                Unit
            } catch (e: Exception) {
                Log.e(TAG, "Delete failed: ${e.message}", e)
            }
        }
    }

    /** Gửi thông điệp từ người dùng và tính toán phản hồi từ AI. */
    suspend fun sendChatMessage(userMessage: String, defaultTitle: String) {
        if (userMessage.isBlank()) return
        val sessionId = currentSessionId ?: return
        if (isGenerating.getAndSet(true)) return

        withContext(Dispatchers.IO) {
            val userChatMsg =
                    ChatMessage(
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

    /** Bỏ qua tin cũ và yêu cầu trả lời lại dạng stream. */
    suspend fun renewAiResponse(aiMessage: ChatMessage) {
        if (isGenerating.getAndSet(true)) return
        val sessionId = currentSessionId ?: run {
            isGenerating.set(false)
            return
        }

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

    /** Mở công việc đồng bộ hóa dữ liệu tin nhắn. */
    private fun startMessageCollection(sessionId: Long) {
        messageCollectionJob?.cancel()
        messageCollectionJob =
                scope.launch {
                    chatDao.getMessagesBySession(sessionId).flowOn(Dispatchers.IO).collect {
                            messages ->
                        _chatMessages.value = messages
                    }
                }
    }

    /** Thực hiện gọi API lấy luồng tin nhắn trả về real-time. */
    private suspend fun executeGeminiStreamingFlow(sessionId: Long, reuseMessageId: Long? = null) {
        _isStreamingActive.value = true
        lastStreamUpdateTime = 0L

        val tempMessage =
                ChatMessage(
                        id = reuseMessageId ?: 0,
                        sessionId = sessionId,
                        content = "",
                        isUser = false,
                        isStreaming = true,
                        timestamp = System.currentTimeMillis()
                )

        val messageId =
                if (reuseMessageId != null) {
                    chatRepository.insertMessage(tempMessage)
                    reuseMessageId
                } else {
                    chatRepository.insertMessage(tempMessage)
                }

        streamingMessageId.set(messageId)

        try {
            val currentHistory = buildConversationHistory(sessionId)
            val systemInstruction =
                    com.nguyendevs.ecolens.utils.PromptBuilder.buildChatSystemInstruction()
            val request =
                    GeminiRequest(contents = currentHistory, system_instruction = systemInstruction)
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

    /** Phân tích và nén lịch sử theo định dạng chuẩn gửi đi Gemini. */
    private suspend fun buildConversationHistory(sessionId: Long): List<GeminiContent> {
        val currentHistory =
                chatDao.getMessagesBySession(sessionId).first().filter { !it.isStreaming }

        return currentHistory.map { msg ->
            val role = if (msg.isUser) "user" else "model"
            GeminiContent(role = role, parts = listOf(GeminiPart(msg.content)))
        }
    }

    /** Lần theo và xử lý luồng stream nội dung văn bản AI. */
    private suspend fun processStreamingResponse(
            responseBody: ResponseBody,
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

    /** Phân giải thông tin rời rạc trong Stream dữ liệu. */
    private suspend fun processStreamChunk(
            jsonData: String,
            accumulatedText: StringBuilder,
            messageId: Long
    ) {
        try {
            val streamResponse = gson.fromJson(jsonData, GeminiResponse::class.java)
            val chunk =
                    streamResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (!chunk.isNullOrEmpty()) {
                accumulatedText.append(chunk)
                val now = System.currentTimeMillis()
                if (now - lastStreamUpdateTime > 200) {
                    val formattedText = markdownProcessor.process(accumulatedText.toString())
                    chatDao.updateMessageContent(messageId, formattedText)
                    lastStreamUpdateTime = now
                }
                delay(STREAM_UPDATE_DELAY)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}")
        }
    }

    /** Gói gọn phản hồi stream và đánh dấu dừng quá trình. */
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

    /** Bắt và phân loại lỗi kết nối luồng chat. */
    private suspend fun handleStreamingError(e: Exception, sessionId: Long, messageId: Long) {
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

    /** Cập nhật thanh tiêu đề phiên và xem trước tin nhắn ngắn gọn. */
    private suspend fun updateSessionTitleAndPreview(
            sessionId: Long,
            userMessage: String,
            defaultTitle: String
    ) {
        val currentSession = chatDao.getSessionById(sessionId)
        val newTitle =
                if (currentSession?.title == defaultTitle) {
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
