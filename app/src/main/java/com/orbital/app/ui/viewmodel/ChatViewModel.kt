package com.orbital.app.ui.viewmodel

import android.util.Log
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: OrbitalRepository
) : ViewModel() {
    private val logTag = "ChatViewModel"

    val messages = mutableStateListOf<ChatMessage>()
    var isStreaming by mutableStateOf(false)
    var streamStatusMessage by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)
    var hasOlderMessages by mutableStateOf(false)
        private set
    var isLoadingOlder by mutableStateOf(false)
        private set

    private var activeSessionId: String? = null
    private var activeSession: Session? = null
    private var streamJob: Job? = null
    private val fullHistory = mutableListOf<ChatMessage>()
    private var loadedStartIndex: Int = 0

    fun bindSession(session: Session) {
        val sessionId = session.id
        if (activeSessionId == sessionId && messages.isNotEmpty()) return
        activeSessionId = sessionId
        activeSession = session
        messages.clear()
        fullHistory.clear()
        loadedStartIndex = 0
        hasOlderMessages = false
        isLoadingOlder = false
        viewModelScope.launch {
            val history = repository.getSessionMessages(
                sessionId = sessionId,
                provider = session.provider,
                projectName = session.projectName,
                projectPath = session.projectPath
            )
            fullHistory.addAll(history)
            loadedStartIndex = (fullHistory.size - PAGE_SIZE).coerceAtLeast(0)
            if (fullHistory.isNotEmpty()) messages.addAll(fullHistory.subList(loadedStartIndex, fullHistory.size))
            hasOlderMessages = loadedStartIndex > 0
        }
    }

    fun loadOlderMessages() {
        if (isLoadingOlder || !hasOlderMessages) return
        isLoadingOlder = true
        viewModelScope.launch {
            val nextStart = (loadedStartIndex - PAGE_SIZE).coerceAtLeast(0)
            if (nextStart < loadedStartIndex) {
                val olderChunk = fullHistory.subList(nextStart, loadedStartIndex)
                messages.addAll(0, olderChunk)
                loadedStartIndex = nextStart
                hasOlderMessages = loadedStartIndex > 0
            }
            isLoadingOlder = false
        }
    }

    fun sendMessage(content: String) {
        val sessionId = activeSessionId ?: return
        val session = activeSession ?: return
        if (content.isBlank() || isStreaming) return

        val text = content.trim()
        val userMessage = ChatMessage(role = "u", text = text)
        messages.add(userMessage)
        fullHistory.add(userMessage)
        Log.d(logTag, "sendMessage session=$sessionId len=${text.length}")
        isStreaming = true
        streamStatusMessage = "Preparando envío..."
        errorMessage = null

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            var assistantIndex = -1
            var assistantFullIndex = -1
            var lastEventAtMs = System.currentTimeMillis()
            var receivedAnyStreamEvent = false

            val streamWorker = launch {
                repository.sendMessageAndStream(session, text) { event ->
                    when (event) {
                        is ChatStreamEvent.Output -> {
                            if (event.text.isBlank()) return@sendMessageAndStream
                            receivedAnyStreamEvent = true
                            lastEventAtMs = System.currentTimeMillis()
                        }
                        is ChatStreamEvent.ToolUse -> {
                            receivedAnyStreamEvent = true
                            lastEventAtMs = System.currentTimeMillis()
                        }
                        is ChatStreamEvent.Status -> {
                            lastEventAtMs = System.currentTimeMillis()
                        }
                        is ChatStreamEvent.Done, is ChatStreamEvent.Error -> {
                            lastEventAtMs = System.currentTimeMillis()
                        }
                        is ChatStreamEvent.Noop -> Unit
                    }
                    when (event) {
                        is ChatStreamEvent.Output -> {
                            if (event.text.isLikelyRawJsonBlob()) return@sendMessageAndStream
                            if (assistantIndex < 0) {
                                val assistantMessage = ChatMessage(role = "a", text = event.text)
                                messages.add(assistantMessage)
                                assistantIndex = messages.lastIndex
                                fullHistory.add(assistantMessage)
                                assistantFullIndex = fullHistory.lastIndex
                            } else {
                                val prev = messages[assistantIndex]
                                messages[assistantIndex] = prev.copy(text = prev.text + event.text)
                                if (assistantFullIndex >= 0) {
                                    val fullPrev = fullHistory[assistantFullIndex]
                                    fullHistory[assistantFullIndex] = fullPrev.copy(text = fullPrev.text + event.text)
                                }
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
                            val toolMessage = ChatMessage(role = "a", text = toolText)
                            messages.add(toolMessage)
                            fullHistory.add(toolMessage)
                        }

                        is ChatStreamEvent.Status -> {
                            streamStatusMessage = event.message
                        }

                        is ChatStreamEvent.Done -> {
                            Log.d(logTag, "stream done session=$sessionId")
                            isStreaming = false
                            streamStatusMessage = null
                        }

                        is ChatStreamEvent.Error -> {
                            Log.e(logTag, "stream error session=$sessionId msg=${event.message}")
                            errorMessage = event.message
                            isStreaming = false
                            streamStatusMessage = null
                        }

                        is ChatStreamEvent.Noop -> Unit
                    }
                }
            }

            while (isActive && streamWorker.isActive) {
                delay(500)
                val timeout = if (receivedAnyStreamEvent) STREAM_IDLE_TIMEOUT_MS else STREAM_FIRST_CHUNK_TIMEOUT_MS
                val elapsed = System.currentTimeMillis() - lastEventAtMs
                if (elapsed > timeout) {
                    Log.e(logTag, "stream timeout session=$sessionId elapsed=${elapsed}ms received=$receivedAnyStreamEvent")
                    errorMessage = if (receivedAnyStreamEvent) {
                        "Stream interrumpido por inactividad"
                    } else {
                        "Timeout esperando respuesta de Orbital"
                    }
                    isStreaming = false
                    streamStatusMessage = null
                    streamWorker.cancel()
                    break
                }
            }

            if (streamWorker.isActive) streamWorker.cancel()
            streamWorker.join()

            if (isStreaming) {
                // fallback in case backend closes stream without explicit done event
                isStreaming = false
                streamStatusMessage = null
            }
        }
    }

    override fun onCleared() {
        streamJob?.cancel()
        super.onCleared()
    }

    private fun String.isLikelyRawJsonBlob(): Boolean {
        val value = trim()
        if (value.length < 180) return false
        if (!(value.startsWith("{") && value.endsWith("}"))) return false
        return value.contains("\"type\"") || value.contains("\"kind\"") || value.contains("\"event\"")
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val STREAM_FIRST_CHUNK_TIMEOUT_MS = 120_000L
        const val STREAM_IDLE_TIMEOUT_MS = 180_000L
    }
}
