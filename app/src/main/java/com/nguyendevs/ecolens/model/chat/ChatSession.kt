package com.nguyendevs.ecolens.model.chat

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity đại diện cho một phiên chat trong cơ sở dữ liệu
 *
 * @property id ID tự động tăng của phiên chat
 * @property title Tiêu đề của phiên chat
 * @property lastMessage Tin nhắn cuối cùng trong phiên
 * @property timestamp Thời gian của phiên chat (milliseconds)
 */
@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0
)