package com.orbital.app.domain

data class Session(
    val id: Int,
    val agent: String,
    val name: String,
    val msgs: Int,
    val ago: String,
    val status: String  // "active" | "idle"
)

data class ChatMessage(
    val role: String,  // "u" | "a"
    val text: String,
    val attachments: List<String> = emptyList()
)
