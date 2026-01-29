package com.nguyendevs.ecolens.models.chat

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.database.PropertyName

/**
 * Entity đại diện cho một tin nhắn trong chat
 * Có foreign key relationship với ChatSession (cascade delete)
 *
 * @property id ID tự động tăng của tin nhắn
 * @property sessionId ID của chat session chứa tin nhắn này
 * @property content Nội dung tin nhắn (hỗ trợ Markdown)
 * @property isUser True nếu là tin nhắn từ user, false nếu từ AI
 * @property timestamp Thời gian tạo tin nhắn (milliseconds)
 * @property isLoading True khi đang hiển thị loading animation
 * @property isStreaming True khi đang nhận streaming response từ AI
 */
@Entity(
    tableName = "chat_messages",
    foreignKeys = [ForeignKey(
        entity = ChatSession::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val sessionId: Long = 0,

    val content: String = "",

    @get:PropertyName("isUser")
    @set:PropertyName("isUser")
    var isUser: Boolean = false,

    val timestamp: Long = System.currentTimeMillis(),

    @get:PropertyName("loading")
    @set:PropertyName("loading")
    var isLoading: Boolean = false,

    @get:PropertyName("streaming")
    @set:PropertyName("streaming")
    var isStreaming: Boolean = false
)