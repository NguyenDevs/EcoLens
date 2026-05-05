package com.nguyendevs.ecolens.database

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.nguyendevs.ecolens.network.NativeSecurityManager
import com.nguyendevs.ecolens.models.chat.ChatMessage
import com.nguyendevs.ecolens.models.chat.ChatSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** Quản lý dữ liệu chat, đồng bộ giữa Room Database và Firebase Realtime Database. */
class ChatRepository(private val chatDao: ChatDao, private val context: Context) {

    private val database = FirebaseDatabase.getInstance(NativeSecurityManager.getFirebaseUrl())
    private val auth = FirebaseAuth.getInstance()

    /** Trả về UID người dùng hiện tại. */
    private fun getUserId(): String {
        return auth.currentUser?.uid ?: "anonymous"
    }

    /** Trả về reference đến node chat sessions của user trên Firebase. */
    private fun getSessionsRef() = database.getReference("chat_sessions").child(getUserId())

    /** Trả về reference đến node chat messages của user trên Firebase. */
    private fun getMessagesRef() = database.getReference("chat_messages").child(getUserId())

    /** Lấy tất cả phiên chat của user hiện tại. */
    fun getAllSessions(): kotlinx.coroutines.flow.Flow<List<ChatSession>> {
        return chatDao.getAllSessions(getUserId())
    }

    /** Thêm phiên chat mới vào local và Firebase, trả về ID mới tạo. */
    suspend fun insertSession(session: ChatSession): Long {
        val sessionWithUserId = session.copy(userId = getUserId())
        val id = chatDao.insertSession(sessionWithUserId)
        val sessionWithId = sessionWithUserId.copy(id = id)
        try {
            getSessionsRef().child(id.toString()).setValue(sessionWithId).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return id
    }

    /** Cập nhật phiên chat vào local và Firebase. */
    suspend fun updateSession(session: ChatSession) {
        val sessionWithUserId = session.copy(userId = getUserId())
        chatDao.updateSession(sessionWithUserId)
        try {
            getSessionsRef().child(session.id.toString()).setValue(sessionWithUserId).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Xóa phiên chat và tất cả tin nhắn liên quan khỏi local và Firebase. */
    suspend fun deleteSession(id: Long) {
        chatDao.deleteSession(id)
        try {
            getSessionsRef().child(id.toString()).removeValue().await()
            getMessagesRef().child(id.toString()).removeValue().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Sắp xếp lại ID phiên chat sau khi xóa (hiện tại bị tắt). */
    private suspend fun reorderSessionIds(deletedId: Long) {
    }

    /** Di chuyển tin nhắn sang phiên mới khi reorder (hiện tại bị tắt). */
    private suspend fun moveMessagesToNewSessionId(oldSessionId: Long, newSessionId: Long) {
    }

    /** Thêm tin nhắn mới vào local và Firebase, trả về ID mới tạo. */
    suspend fun insertMessage(message: ChatMessage): Long {
        val messageWithUserId = message.copy(userId = getUserId())
        val id =
                if (message.id > 0) {
                    chatDao.insertMessage(messageWithUserId)
                    message.id
                } else {
                    chatDao.insertMessage(messageWithUserId)
                }

        val messageWithId = messageWithUserId.copy(id = id)
        try {
            getMessagesRef()
                    .child(message.sessionId.toString())
                    .child(id.toString())
                    .setValue(messageWithId)
                    .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return id
    }

    /** Cập nhật toàn bộ tin nhắn vào local và Firebase. */
    suspend fun updateMessage(message: ChatMessage) {
        val messageWithUserId = message.copy(userId = getUserId())
        chatDao.updateMessage(messageWithUserId)
        try {
            getMessagesRef()
                    .child(message.sessionId.toString())
                    .child(message.id.toString())
                    .setValue(messageWithUserId)
                    .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Cập nhật chỉ nội dung tin nhắn vào local và Firebase. */
    suspend fun updateMessageContent(messageId: Long, content: String, sessionId: Long) {
        chatDao.updateMessageContent(messageId, content)
        try {
            getMessagesRef()
                    .child(sessionId.toString())
                    .child(messageId.toString())
                    .child("content")
                    .setValue(content)
                    .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Xóa tin nhắn khỏi local và Firebase. */
    suspend fun deleteMessage(message: ChatMessage, reorder: Boolean = true) {
        chatDao.deleteMessageById(message.id)
        try {
            getMessagesRef()
                    .child(message.sessionId.toString())
                    .child(message.id.toString())
                    .removeValue()
                    .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Sắp xếp lại ID tin nhắn sau khi xóa (hiện tại bị tắt). */
    private suspend fun reorderMessageIds(sessionId: Long, deletedMessageId: Long) {
    }

    /** Tải toàn bộ sessions và messages từ Firebase về local. */
    suspend fun fetchSessionsAndMessages() =
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                if (auth.currentUser == null) return@withContext

                val sessionsJob = async {
                    try {
                        val sessionsSnapshot = getSessionsRef().get().await()
                        if (sessionsSnapshot.exists()) {
                            val sessions = mutableListOf<ChatSession>()
                            for (sessionSnapshot in sessionsSnapshot.children) {
                                val session = sessionSnapshot.getValue(ChatSession::class.java)
                                if (session != null) {
                                    sessions.add(session)
                                }
                            }
                            if (sessions.isNotEmpty()) {
                                for (session in sessions) {
                                    chatDao.insertSession(session)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val messagesJob = async {
                    try {
                        val messagesSnapshot = getMessagesRef().get().await()
                        if (messagesSnapshot.exists()) {
                            val messages = mutableListOf<ChatMessage>()
                            for (sessionMessagesSnapshot in messagesSnapshot.children) {
                                for (messageSnapshot in sessionMessagesSnapshot.children) {
                                    val message = messageSnapshot.getValue(ChatMessage::class.java)
                                    if (message != null) {
                                        messages.add(message)
                                    }
                                }
                            }
                            if (messages.isNotEmpty()) {
                                for (message in messages) {
                                    chatDao.insertMessage(message)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                sessionsJob.await()
                messagesJob.await()
            }
}
