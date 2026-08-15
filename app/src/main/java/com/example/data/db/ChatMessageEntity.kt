package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_history")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // SYSTEM, JARVIS, USER, SUCCESS, WARNING, STARK_DIRECTIVE
    val tag: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
