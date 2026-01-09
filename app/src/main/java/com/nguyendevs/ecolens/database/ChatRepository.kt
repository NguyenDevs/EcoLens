package com.nguyendevs.ecolens.database

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.nguyendevs.ecolens.BuildConfig
import com.nguyendevs.ecolens.model.chat.ChatMessage
import com.nguyendevs.ecolens.model.chat.ChatSession
import kotlinx.coroutines.tasks.await

/**
 * Repository quản lý dữ liệu chat với tích hợp Firebase
 * Đồng bộ dữ liệu giữa local Room Database và Firebase Realtime Database
 */
class ChatRepository(
    private val chatDao: ChatDao,
    private val context: Context
) {

    private val database = FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL)
    private val auth = FirebaseAuth.getInstance()

    // ==================== FIREBASE REFERENCES ====================

    /**
     * Lấy UID của người dùng hiện tại từ Firebase Auth
     */
    private fun getUserId(): String {
        return auth.currentUser?.uid ?: "anonymous"
    }

    /**
     * Lấy reference đến node chat sessions của user trong Firebase Database
     */
    private fun getSessionsRef() = database.getReference("chat_sessions").child(getUserId())

    /**
     * Lấy reference đến node chat messages của user trong Firebase Database
     */
    private fun getMessagesRef() = database.getReference("chat_messages").child(getUserId())

    // ==================== CHAT SESSION - INSERT & UPDATE ====================

    /**
     * Thêm phiên chat mới vào cả local và Firebase
     * @return ID của phiên chat vừa tạo
     */
    suspend fun insertSession(session: ChatSession): Long {
        val id = chatDao.insertSession(session)
        val sessionWithId = session.copy(id = id)
        try {
            getSessionsRef().child(id.toString()).setValue(sessionWithId).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return id
    }

    /**
     * Cập nhật phiên chat vào cả local và Firebase
     */
    suspend fun updateSession(session: ChatSession) {
        chatDao.updateSession(session)
        try {
            getSessionsRef().child(session.id.toString()).setValue(session).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==================== CHAT SESSION - DELETE ====================

    /**
     * Xóa phiên chat khỏi cả local và Firebase
     * Tự động xóa tất cả tin nhắn liên quan và sắp xếp lại ID
     */
    suspend fun deleteSession(id: Long) {
        chatDao.deleteSession(id)
        try {
            getSessionsRef().child(id.toString()).removeValue().await()
            getMessagesRef().child(id.toString()).removeValue().await()
            reorderSessionIds(id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Sắp xếp lại ID các phiên chat sau khi xóa
     * Giảm ID của các phiên có ID lớn hơn phiên đã xóa
     */
    private suspend fun reorderSessionIds(deletedId: Long) {
        try {
            val snapshot = getSessionsRef().get().await()
            if (snapshot.exists()) {
                val updates = hashMapOf<String, Any?>()
                val sessionsToUpdate = mutableListOf<ChatSession>()

                for (child in snapshot.children) {
                    val session = child.getValue(ChatSession::class.java)
                    if (session != null && session.id > deletedId) {
                        sessionsToUpdate.add(session)
                    }
                }

                sessionsToUpdate.sortBy { it.id }

                for (session in sessionsToUpdate) {
                    val oldId = session.id
                    val newId = oldId - 1
                    val updatedSession = session.copy(id = newId)

                    updates[oldId.toString()] = null
                    updates[newId.toString()] = updatedSession
                    chatDao.deleteSession(oldId)
                    chatDao.insertSession(updatedSession)
                    moveMessagesToNewSessionId(oldId, newId)
                }

                if (updates.isNotEmpty()) {
                    getSessionsRef().updateChildren(updates).await()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Di chuyển tất cả tin nhắn từ phiên cũ sang phiên mới
     * Dùng khi reorder session IDs
     */
    private suspend fun moveMessagesToNewSessionId(oldSessionId: Long, newSessionId: Long) {
        try {
            val messagesSnapshot = getMessagesRef().child(oldSessionId.toString()).get().await()
            if (messagesSnapshot.exists()) {
                getMessagesRef().child(oldSessionId.toString()).removeValue().await()

                for (child in messagesSnapshot.children) {
                    val message = child.getValue(ChatMessage::class.java)
                    if (message != null) {
                        val updatedMessage = message.copy(sessionId = newSessionId)

                        getMessagesRef()
                            .child(newSessionId.toString())
                            .child(updatedMessage.id.toString())
                            .setValue(updatedMessage)
                            .await()

                        chatDao.deleteMessageById(message.id)
                        chatDao.insertMessage(updatedMessage)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==================== CHAT MESSAGE - INSERT & UPDATE ====================

    /**
     * Thêm tin nhắn mới vào cả local và Firebase
     * @return ID của tin nhắn vừa tạo
     */
    suspend fun insertMessage(message: ChatMessage): Long {
        val id = if (message.id > 0) {
            chatDao.insertMessage(message)
            message.id
        } else {
            chatDao.insertMessage(message)
        }

        val messageWithId = message.copy(id = id)
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

    /**
     * Cập nhật toàn bộ tin nhắn vào cả local và Firebase
     */
    suspend fun updateMessage(message: ChatMessage) {
        chatDao.updateMessage(message)
        try {
            getMessagesRef()
                .child(message.sessionId.toString())
                .child(message.id.toString())
                .setValue(message)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cập nhật chỉ nội dung tin nhắn vào cả local và Firebase
     */
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
                reorderMessageIds(message.sessionId, message.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Sắp xếp lại ID các tin nhắn trong một phiên sau khi xóa
     * Giảm ID của các tin nhắn có ID lớn hơn tin nhắn đã xóa
     */
    private suspend fun reorderMessageIds(sessionId: Long, deletedMessageId: Long) {
        try {
            val snapshot = getMessagesRef().child(sessionId.toString()).get().await()
            if (snapshot.exists()) {
                val updates = hashMapOf<String, Any?>()
                val messagesToUpdate = mutableListOf<ChatMessage>()

                for (child in snapshot.children) {
                    val message = child.getValue(ChatMessage::class.java)
                    if (message != null && message.id > deletedMessageId) {
                        messagesToUpdate.add(message)
                    }
                }

                messagesToUpdate.sortBy { it.id }

                for (message in messagesToUpdate) {
                    val oldId = message.id
                    val newId = oldId - 1
                    val updatedMessage = message.copy(id = newId)

                    updates[oldId.toString()] = null
                    updates[newId.toString()] = updatedMessage

                    chatDao.deleteMessageById(oldId)
                    chatDao.insertMessage(updatedMessage)
                }

                if (updates.isNotEmpty()) {
                    getMessagesRef()
                        .child(sessionId.toString())
                        .updateChildren(updates)
                        .await()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==================== SYNC METHODS ====================

    /**
     * Tải toàn bộ phiên chat và tin nhắn từ Firebase về local
     * Chỉ thực hiện khi người dùng đã đăng nhập
     */
    suspend fun fetchSessionsAndMessages() {
        if (auth.currentUser == null) return

        try {
            val sessionsSnapshot = getSessionsRef().get().await()
            if (sessionsSnapshot.exists()) {
                for (sessionSnapshot in sessionsSnapshot.children) {
                    val session = sessionSnapshot.getValue(ChatSession::class.java)
                    if (session != null) {
                        chatDao.insertSession(session)
                    }
                }
            }

            val messagesSnapshot = getMessagesRef().get().await()
            if (messagesSnapshot.exists()) {
                for (sessionMessagesSnapshot in messagesSnapshot.children) {
                    for (messageSnapshot in sessionMessagesSnapshot.children) {
                        val message = messageSnapshot.getValue(ChatMessage::class.java)
                        if (message != null) {
                            chatDao.insertMessage(message)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}