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
                    chatRepository.updateSession(latestSession.copy(timestamp = System.currentTimeMillis()))
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
        }
    }

    fun loadChatSession(sessionId: Long) {
        currentSessionId = sessionId
        startMessageCollection(sessionId)
    }

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

            executeGeminiStreamingFlow(sessionId)
        }
    }

    suspend fun renewAiResponse(aiMessage: ChatMessage) {
        if (isGenerating.getAndSet(true)) return
        val sessionId = currentSessionId ?: return.also { isGenerating.set(false) }

        withContext(Dispatchers.IO) {
            try {
                chatRepository.deleteMessage(aiMessage)
                executeGeminiStreamingFlow(sessionId)
            } catch (e: Exception) {
                isGenerating.set(false)
                Log.e(TAG, "Renew failed: ${e.message}")
            }
        }
    }

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

    fun startNewChatSession() {
        currentSessionId = null
        messageCollectionJob?.cancel()
        _chatMessages.value = emptyList()
    }

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

    private suspend fun executeGeminiStreamingFlow(sessionId: Long) {
        _isStreamingActive.value = true

        val tempMessage = ChatMessage(
            sessionId = sessionId,
            content = "",
            isUser = false,
            isStreaming = true,
            timestamp = System.currentTimeMillis()
        )

        // Insert initial empty message to both local and firebase
        val messageId = chatRepository.insertMessage(tempMessage)
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
                    val accumulatedText = StringBuilder()

                    responseBody.byteStream().bufferedReader().use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val currentLine = line ?: continue
                            if (currentLine.startsWith(PREFIX_DATA)) {
                                val jsonData = currentLine.substring(PREFIX_DATA.length).trim()
                                if (jsonData == STREAM_DONE) break

                                try {
                                    val streamResponse = gson.fromJson(jsonData, GeminiResponse::class.java)
                                    val chunk = streamResponse.candidates?.firstOrNull()
                                        ?.content?.parts?.firstOrNull()?.text

                                    if (!chunk.isNullOrEmpty()) {
                                        accumulatedText.append(chunk)
                                        val formattedText = markdownProcessor.process(accumulatedText.toString())
                                        // Update ONLY local DB during streaming for performance
                                        chatDao.updateMessageContent(messageId, formattedText)
                                        delay(STREAM_UPDATE_DELAY)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Parse error: ${e.message}")
                                }
                            }
                        }
                    }

                    val finalFormattedText = markdownProcessor.process(accumulatedText.toString())
                    // Update BOTH local and Firebase when streaming is complete
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
                        chatRepository.updateSession(it.copy(
                            lastMessage = accumulatedText.take(PREVIEW_MAX_LENGTH).toString(),
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
        } finally {
            _isStreamingActive.value = false
            isGenerating.set(false)
            streamingMessageId.set(-1L)
        }
    }
}