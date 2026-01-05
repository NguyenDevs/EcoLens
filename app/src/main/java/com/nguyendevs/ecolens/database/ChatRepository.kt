package com.nguyendevs.ecolens.database

import android.content.Context
import com.google.firebase.database.FirebaseDatabase
import com.nguyendevs.ecolens.model.ChatMessage
import com.nguyendevs.ecolens.model.ChatSession
import kotlinx.coroutines.tasks.await

class ChatRepository(private val chatDao: ChatDao, private val context: Context) {
    // Sử dụng URL cụ thể do người dùng cung cấp để đảm bảo kết nối đúng region
    private val database = FirebaseDatabase.getInstance("https://ecolens-658ae-default-rtdb.asia-southeast1.firebasedatabase.app/")
    
    private fun getUsername(): String {
        val sharedPreferences = context.getSharedPreferences("EcoLensPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("username", "default_user") ?: "default_user"
    }

    private fun getSessionsRef() = database.getReference("chat_sessions").child(getUsername())
    private fun getMessagesRef() = database.getReference("chat_messages").child(getUsername())

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

    suspend fun updateSession(session: ChatSession) {
        chatDao.updateSession(session)
        try {
            getSessionsRef().child(session.id.toString()).setValue(session).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteSession(id: Long) {
        chatDao.deleteSession(id)
        try {
            getSessionsRef().child(id.toString()).removeValue().await()
            getMessagesRef().child(id.toString()).removeValue().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun insertMessage(message: ChatMessage): Long {
        val id = chatDao.insertMessage(message)
        val messageWithId = message.copy(id = id)
        try {
            getMessagesRef().child(message.sessionId.toString()).child(id.toString()).setValue(messageWithId).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return id
    }

    suspend fun updateMessage(message: ChatMessage) {
        chatDao.updateMessage(message)
        try {
            getMessagesRef().child(message.sessionId.toString()).child(message.id.toString()).setValue(message).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateMessageContent(messageId: Long, content: String, sessionId: Long) {
        chatDao.updateMessageContent(messageId, content)
        try {
            getMessagesRef().child(sessionId.toString()).child(messageId.toString()).child("content").setValue(content).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteMessage(message: ChatMessage) {
        chatDao.deleteMessageById(message.id)
        try {
            getMessagesRef().child(message.sessionId.toString()).child(message.id.toString()).removeValue().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchSessionsAndMessages() {
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