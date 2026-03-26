package com.orbital.app.domain

sealed class ChatStreamEvent {
    data class Output(val text: String) : ChatStreamEvent()
    data class ToolUse(val tool: String, val inputSummary: String = "") : ChatStreamEvent()
    data class Status(val message: String) : ChatStreamEvent()
    data class ActionRequired(val message: String) : ChatStreamEvent()
    data object Done : ChatStreamEvent()
    data class Error(val message: String) : ChatStreamEvent()
    data object Noop : ChatStreamEvent()
}
