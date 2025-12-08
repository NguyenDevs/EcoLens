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
    // Thêm phiên chat mới
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    // Cập nhật thông tin phiên chat
    @Update
    suspend fun updateSession(session: ChatSession)

    // Lấy tất cả phiên chat sắp xếp theo thời gian mới nhất
    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    // Lấy phiên chat theo ID
    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): ChatSession?

    //Lấy phiên chat mới nhất
    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSession(): ChatSession?

    // Xóa phiên chat theo ID
    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    // Thêm tin nhắn mới
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    // Lấy tất cả tin nhắn của một phiên chat
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesBySession(sessionId: Long): Flow<List<ChatMessage>>

    //Đếm số tin nhắn CỦA NGƯỜI DÙNG trong một phiên (dùng để check xem user đã chat chưa)
    @Query("SELECT COUNT(*) FROM chat_messages WHERE sessionId = :sessionId AND isUser = 1")
    suspend fun getUserMessageCount(sessionId: Long): Int
}