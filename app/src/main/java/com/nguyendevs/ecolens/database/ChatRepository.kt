package com.nguyendevs.ecolens.database

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.nguyendevs.ecolens.BuildConfig
import com.nguyendevs.ecolens.models.chat.ChatMessage
import com.nguyendevs.ecolens.models.chat.ChatSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repository quản lý dữ liệu chat với tích hợp Firebase Đồng bộ dữ liệu giữa local Room Database và
 * Firebase Realtime Database
 */
class ChatRepository(private val chatDao: ChatDao, private val context: Context) {

    private val database = FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL)
    private val auth = FirebaseAuth.getInstance()

    // ==================== FIREBASE REFERENCES ====================

    /** Lấy UID của người dùng hiện tại từ Firebase Auth */
    private fun getUserId(): String {
        return auth.currentUser?.uid ?: "anonymous"
    }

    /** Lấy reference đến node chat sessions của user trong Firebase Database */
    private fun getSessionsRef() = database.getReference("chat_sessions").child(getUserId())

    /** Lấy reference đến node chat messages của user trong Firebase Database */
    private fun getMessagesRef() = database.getReference("chat_messages").child(getUserId())

    // ==================== CHAT SESSION - QUERY ====================

    /** Lấy tất cả phiên chat của user hiện tại */
    fun getAllSessions(): kotlinx.coroutines.flow.Flow<List<ChatSession>> {
        return chatDao.getAllSessions(getUserId())
    }

    // ==================== CHAT SESSION - INSERT & UPDATE ====================

    /**
     * Thêm phiên chat mới vào cả local và Firebase
     * @return ID của phiên chat vừa tạo
     */
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

    /** Cập nhật phiên chat vào cả local và Firebase */
    suspend fun updateSession(session: ChatSession) {
        val sessionWithUserId = session.copy(userId = getUserId())
        chatDao.updateSession(sessionWithUserId)
        try {
            getSessionsRef().child(session.id.toString()).setValue(sessionWithUserId).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==================== CHAT SESSION - DELETE ====================

    /**
     * Xóa phiên chat khỏi cả local và Firebase Tự động xóa tất cả tin nhắn liên quan và sắp xếp lại
     * ID
     */
    suspend fun deleteSession(id: Long) {
        chatDao.deleteSession(id)
        try {
            getSessionsRef().child(id.toString()).removeValue().await()
            getMessagesRef().child(id.toString()).removeValue().await()
            // reorderSessionIds(id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Sắp xếp lại ID các phiên chat sau khi xóa Giảm ID của các phiên có ID lớn hơn phiên đã xóa
     */
    private suspend fun reorderSessionIds(deletedId: Long) {
        // Reorder disabled
    }

    /** Di chuyển tất cả tin nhắn từ phiên cũ sang phiên mới Dùng khi reorder session IDs */
    private suspend fun moveMessagesToNewSessionId(oldSessionId: Long, newSessionId: Long) {
        // Disabled
    }

    // ==================== CHAT MESSAGE - INSERT & UPDATE ====================

    /**
     * Thêm tin nhắn mới vào cả local và Firebase
     * @return ID của tin nhắn vừa tạo
     */
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

    /** Cập nhật toàn bộ tin nhắn vào cả local và Firebase */
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

    /** Cập nhật chỉ nội dung tin nhắn vào cả local và Firebase */
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

    // ==================== CHAT MESSAGE - DELETE ====================

    /**
     * Xóa tin nhắn khỏi cả local và Firebase
     * @param reorder Có sắp xếp lại ID các tin nhắn sau khi xóa hay không
     */
    suspend fun deleteMessage(message: ChatMessage, reorder: Boolean = true) {
        chatDao.deleteMessageById(message.id)
        try {
            getMessagesRef()
                    .child(message.sessionId.toString())
                    .child(message.id.toString())
                    .removeValue()
                    .await()

            if (reorder) {
                // reorderMessageIds(message.sessionId, message.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Sắp xếp lại ID các tin nhắn trong một phiên sau khi xóa Giảm ID của các tin nhắn có ID lớn
     * hơn tin nhắn đã xóa
     */
    private suspend fun reorderMessageIds(sessionId: Long, deletedMessageId: Long) {
        // Disabled
    }

    // ==================== SYNC METHODS ====================

    // Tải toàn bộ dữ liệu chat từ Firebase về Local
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
