package com.orbital.app.domain

data class Session(
    val id: String,
    val agent: String,
    val name: String,
    val msgs: Int,
    val ago: String,
    val status: String, // active | idle
    val projectName: String? = null,
    val projectPath: String? = null,
    val provider: String = "claude",
    val updatedAtMs: Long? = null
)

data class ChatMessage(
    val role: String,  // u | a
    val text: String,
    val attachments: List<String> = emptyList()
)
