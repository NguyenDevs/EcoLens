package com.nguyendevs.ecolens.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nguyendevs.ecolens.models.chat.ChatMessage
import com.nguyendevs.ecolens.models.chat.ChatSession
import kotlinx.coroutines.flow.Flow

/** DAO thao tác với bảng phiên chat và tin nhắn. */
@Dao
interface ChatDao {

    /** Thêm hoặc thay thế một phiên chat. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    /** Cập nhật thông tin phiên chat. */
    @Update suspend fun updateSession(session: ChatSession)

    /** Lấy tất cả phiên chat của user, mới nhất trước. */
    @Query("SELECT * FROM chat_sessions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllSessions(userId: String): Flow<List<ChatSession>>

    /** Lấy phiên chat theo ID. */
    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): ChatSession?

    /** Lấy phiên chat mới nhất. */
    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSession(): ChatSession?

    /** Xóa phiên chat theo ID. */
    @Query("DELETE FROM chat_sessions WHERE id = :id") suspend fun deleteSession(id: Long)

    /** Xóa tất cả tin nhắn thuộc một phiên chat. */
    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesBySession(sessionId: Long)

    /** Thêm hoặc thay thế một tin nhắn. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    /** Cập nhật toàn bộ thông tin tin nhắn. */
    @Update suspend fun updateMessage(message: ChatMessage)

    /** Cập nhật chỉ nội dung của một tin nhắn. */
    @Query("UPDATE chat_messages SET content = :content WHERE id = :messageId")
    suspend fun updateMessageContent(messageId: Long, content: String)

    /** Lấy tất cả tin nhắn trong một phiên, từ cũ đến mới. */
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesBySession(sessionId: Long): Flow<List<ChatMessage>>

    /** Đếm số tin nhắn của người dùng trong một phiên. */
    @Query("SELECT COUNT(*) FROM chat_messages WHERE sessionId = :sessionId AND isUser = 1")
    suspend fun getUserMessageCount(sessionId: Long): Int

    /** Xóa một tin nhắn theo ID. */
    @Query("DELETE FROM chat_messages WHERE id = :msgId") suspend fun deleteMessageById(msgId: Long)
}
