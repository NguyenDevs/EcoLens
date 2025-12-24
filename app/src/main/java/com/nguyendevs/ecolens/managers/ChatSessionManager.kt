package com.nguyendevs.ecolens.managers

import android.util.Log
import com.google.gson.Gson
import com.nguyendevs.ecolens.api.*
import com.nguyendevs.ecolens.database.ChatDao
import com.nguyendevs.ecolens.model.ChatMessage
import com.nguyendevs.ecolens.model.ChatSession
import com.nguyendevs.ecolens.network.RetrofitClient
import com.nguyendevs.ecolens.utils.MarkdownProcessor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class ChatSessionManager(
    private val chatDao: ChatDao,
    private val scope: CoroutineScope
) {
    private val gson = Gson()
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

    suspend fun renewAiResponse(aiMessage: ChatMessage) {
        if (isGenerating.getAndSet(true)) return
        val sessionId = currentSessionId ?: return.also { isGenerating.set(false) }

        withContext(Dispatchers.IO) {
            try {
                chatDao.deleteMessageById(aiMessage.id)
                executeGeminiStreamingFlow(sessionId)
            } catch (e: Exception) {
                isGenerating.set(false)
                Log.e("ChatSessionManager", "Renew failed: ${e.message}")
            }
        }
    }

    suspend fun deleteChatSession(sessionId: Long) {
        withContext(Dispatchers.IO) {
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
                Log.e("ChatSessionManager", "Delete failed: ${e.message}", e)
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
                                        val formattedText = markdownProcessor.process(accumulatedText)
                                        chatDao.updateMessageContent(messageId, formattedText)
                                        delay(50)
                                    }
                                } catch (e: Exception) {
                                    Log.e("Streaming", "Parse error: ${e.message}")
                                }
                            }
                        }
                    }

                    val finalFormattedText = markdownProcessor.process(accumulatedText)
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
}
