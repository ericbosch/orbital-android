package com.orbital.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbital.app.data.OrbitalRepository
import com.orbital.app.domain.ChatMessage
import com.orbital.app.domain.ChatStreamEvent
import com.orbital.app.domain.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: OrbitalRepository
) : ViewModel() {

    val messages = mutableStateListOf<ChatMessage>()
    var isStreaming by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private var activeSessionId: String? = null
    private var activeSession: Session? = null
    private var streamJob: Job? = null

    fun bindSession(session: Session) {
        val sessionId = session.id
        if (activeSessionId == sessionId && messages.isNotEmpty()) return
        activeSessionId = sessionId
        activeSession = session
        messages.clear()
        viewModelScope.launch {
            val history = repository.getSessionMessages(
                sessionId = sessionId,
                provider = session.provider,
                projectName = session.projectName,
                projectPath = session.projectPath
            )
            if (history.isNotEmpty()) {
                messages.addAll(history)
            }
        }
    }

    fun sendMessage(content: String) {
        val sessionId = activeSessionId ?: return
        val session = activeSession ?: return
        if (content.isBlank() || isStreaming) return

        val text = content.trim()
        messages.add(ChatMessage(role = "u", text = text))
        isStreaming = true
        errorMessage = null

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            var assistantIndex = -1
            repository.sendMessageAndStream(session, text) { event ->
                when (event) {
                    is ChatStreamEvent.Output -> {
                        if (assistantIndex < 0) {
                            messages.add(ChatMessage(role = "a", text = event.text))
                            assistantIndex = messages.lastIndex
                        } else {
                            val prev = messages[assistantIndex]
                            messages[assistantIndex] = prev.copy(text = prev.text + event.text)
                        }
                    }

                    is ChatStreamEvent.ToolUse -> {
                        val toolText = buildString {
                            append("[tool] ")
                            append(event.tool)
                            if (event.inputSummary.isNotBlank()) {
                                append(": ")
                                append(event.inputSummary)
                            }
                        }
                        messages.add(ChatMessage(role = "a", text = toolText))
                    }

                    is ChatStreamEvent.Done -> {
                        isStreaming = false
                    }

                    is ChatStreamEvent.Error -> {
                        errorMessage = event.message
                        isStreaming = false
                    }
                }
            }
            if (isStreaming) {
                // fallback in case backend closes stream without explicit done event
                isStreaming = false
            }
        }
    }

    override fun onCleared() {
        streamJob?.cancel()
        super.onCleared()
    }
}
