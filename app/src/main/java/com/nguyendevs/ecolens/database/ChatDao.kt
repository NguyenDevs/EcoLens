package com.nguyendevs.ecolens.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nguyendevs.ecolens.model.ChatMessage
import com.nguyendevs.ecolens.model.ChatSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    // ==================== CHAT SESSION - INSERT & UPDATE ====================

    /**
     * Thêm hoặc thay thế một phiên chat mới
     * @return ID của phiên chat vừa tạo
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    /**
     * Cập nhật thông tin của một phiên chat
     */
    @Update
    suspend fun updateSession(session: ChatSession)

    // ==================== CHAT SESSION - QUERY ====================

    /**
     * Lấy tất cả phiên chat, sắp xếp từ mới nhất đến cũ nhất
     */
    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    /**
     * Lấy một phiên chat theo ID
     */
    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): ChatSession?

    /**
     * Lấy phiên chat mới nhất
     */
    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSession(): ChatSession?

    // ==================== CHAT SESSION - DELETE ====================

    /**
     * Xóa một phiên chat theo ID
     */
    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    /**
     * Xóa tất cả tin nhắn thuộc một phiên chat
     */
    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesBySession(sessionId: Long)

    // ==================== CHAT MESSAGE - INSERT & UPDATE ====================

    /**
     * Thêm hoặc thay thế một tin nhắn mới
     * @return ID của tin nhắn vừa tạo
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    /**
     * Cập nhật toàn bộ thông tin của một tin nhắn
     */
    @Update
    suspend fun updateMessage(message: ChatMessage)

    /**
     * Cập nhật nội dung của một tin nhắn cụ thể
     */
    @Query("UPDATE chat_messages SET content = :content WHERE id = :messageId")
    suspend fun updateMessageContent(messageId: Long, content: String)

    // ==================== CHAT MESSAGE - QUERY ====================

    /**
     * Lấy tất cả tin nhắn trong một phiên chat, sắp xếp từ cũ đến mới
     */
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesBySession(sessionId: Long): Flow<List<ChatMessage>>

    /**
     * Đếm số lượng tin nhắn của người dùng trong một phiên chat
     */
    @Query("SELECT COUNT(*) FROM chat_messages WHERE sessionId = :sessionId AND isUser = 1")
    suspend fun getUserMessageCount(sessionId: Long): Int

    // ==================== CHAT MESSAGE - DELETE ====================

    /**
     * Xóa một tin nhắn theo ID
     */
    @Query("DELETE FROM chat_messages WHERE id = :msgId")
    suspend fun deleteMessageById(msgId: Long)
}