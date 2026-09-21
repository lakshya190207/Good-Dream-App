package com.example.data.gemini

import java.util.UUID

enum class MessageRole {
    USER,
    MODEL
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)
