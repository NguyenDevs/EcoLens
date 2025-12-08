package com.nguyendevs.ecolens.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity đại diện cho một phiên chat
 */
@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val lastMessage: String,
    val timestamp: Long
)