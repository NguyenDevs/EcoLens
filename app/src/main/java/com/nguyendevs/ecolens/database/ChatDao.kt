package com.nguyendevs.ecolens.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nguyendevs.ecolens.model.ChatMessage
import com.nguyendevs.ecolens.model.ChatSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    @Update
    suspend fun updateSession(session: ChatSession)

    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): ChatSession?

    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSession(): ChatSession?

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesBySession(sessionId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Update
    suspend fun updateMessage(message: ChatMessage)

    @Query("UPDATE chat_messages SET content = :content WHERE id = :messageId")
    suspend fun updateMessageContent(messageId: Long, content: String)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesBySession(sessionId: Long): Flow<List<ChatMessage>>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE sessionId = :sessionId AND isUser = 1")
    suspend fun getUserMessageCount(sessionId: Long): Int

    @Query("DELETE FROM chat_messages WHERE id = :msgId")
    suspend fun deleteMessageById(msgId: Long)
}