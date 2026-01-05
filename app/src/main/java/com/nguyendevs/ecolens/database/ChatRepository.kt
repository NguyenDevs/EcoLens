package com.nguyendevs.ecolens.database

import com.google.firebase.database.FirebaseDatabase
import com.nguyendevs.ecolens.model.ChatMessage
import com.nguyendevs.ecolens.model.ChatSession
import kotlinx.coroutines.tasks.await

class ChatRepository(private val chatDao: ChatDao) {
    // Sử dụng URL cụ thể do người dùng cung cấp để đảm bảo kết nối đúng region
    private val database = FirebaseDatabase.getInstance("https://ecolens-658ae-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private val sessionsRef = database.getReference("chat_sessions")
    private val messagesRef = database.getReference("chat_messages")

    suspend fun insertSession(session: ChatSession): Long {
        val id = chatDao.insertSession(session)
        val sessionWithId = session.copy(id = id)
        try {
            sessionsRef.child(id.toString()).setValue(sessionWithId).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return id
    }

    suspend fun updateSession(session: ChatSession) {
        chatDao.updateSession(session)
        try {
            sessionsRef.child(session.id.toString()).setValue(session).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteSession(id: Long) {
        chatDao.deleteSession(id)
        try {
            sessionsRef.child(id.toString()).removeValue().await()
            messagesRef.child(id.toString()).removeValue().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun insertMessage(message: ChatMessage): Long {
        val id = chatDao.insertMessage(message)
        val messageWithId = message.copy(id = id)
        try {
            messagesRef.child(message.sessionId.toString()).child(id.toString()).setValue(messageWithId).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return id
    }

    suspend fun updateMessage(message: ChatMessage) {
        chatDao.updateMessage(message)
        try {
            messagesRef.child(message.sessionId.toString()).child(message.id.toString()).setValue(message).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateMessageContent(messageId: Long, content: String, sessionId: Long) {
        chatDao.updateMessageContent(messageId, content)
        try {
            messagesRef.child(sessionId.toString()).child(messageId.toString()).child("content").setValue(content).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteMessage(message: ChatMessage) {
        chatDao.deleteMessageById(message.id)
        try {
            messagesRef.child(message.sessionId.toString()).child(message.id.toString()).removeValue().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}