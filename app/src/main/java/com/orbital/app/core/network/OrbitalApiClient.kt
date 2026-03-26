package com.orbital.app.core.network

import android.util.Log
import com.orbital.app.domain.Agent
import com.orbital.app.domain.AgentStatus
import com.orbital.app.domain.ChatMessage
import com.orbital.app.domain.ChatStreamEvent
import com.orbital.app.domain.Project
import com.orbital.app.domain.SearchResult
import com.orbital.app.domain.Session
import com.orbital.app.domain.Skill
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

class OrbitalApiClient @Inject constructor(private val client: HttpClient) {
    private val logTag = "OrbitalApiClient"

    private val json = Json { ignoreUnknownKeys = true }

    private var baseUrl: String = ""
    private var authToken: String = ""

    fun setBaseUrl(url: String) { baseUrl = url.trimEnd('/') }
    fun setAuthToken(token: String) { authToken = token }
    fun getBaseUrl(): String = baseUrl

    suspend fun ping(url: String, token: String): Boolean {
        val normalized = url.trimEnd('/')
        return try {
            client.get("$normalized/api/health") {
                if (token.isNotBlank()) header(HttpHeaders.Authorization, "Bearer $token")
            }.status.isSuccess()
        } catch (_: Exception) {
            try {
                client.get("$normalized/api/projects") {
                    if (token.isNotBlank()) header(HttpHeaders.Authorization, "Bearer $token")
                }.status.isSuccess()
            } catch (_: Exception) {
                false
            }
        }
    }

    suspend fun getProjects(): List<Project> = try {
        parseArrayBody(client.get("$baseUrl/api/projects") { authHeader() }.body())
            .mapNotNull { it.toProject() }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun getAgents(): List<Agent> = try {
        parseArrayBody(client.get("$baseUrl/api/agents") { authHeader() }.body())
            .mapNotNull { it.toAgent() }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun getSessions(projectName: String? = null): List<Session> = try {
        val url = if (projectName.isNullOrBlank()) {
            "$baseUrl/api/sessions"
        } else {
            "$baseUrl/api/projects/$projectName/sessions"
        }
        parseArrayBody(
            client.get(url) { authHeader() }.body()
        ).mapNotNull { it.toSession(defaultProjectName = projectName, defaultProvider = "claude") }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun getCodexSessions(projectPath: String): List<Session> = try {
        parseArrayBody(
            client.get("$baseUrl/api/codex/sessions") {
                authHeader()
                parameter("projectPath", projectPath)
            }.body()
        ).mapNotNull { it.toSession(defaultProvider = "codex", defaultProjectPath = projectPath) }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun getCursorSessions(projectPath: String): List<Session> = try {
        parseArrayBody(
            client.get("$baseUrl/api/cursor/sessions") {
                authHeader()
                parameter("projectPath", projectPath)
            }.body()
        ).mapNotNull { it.toSession(defaultProvider = "cursor", defaultProjectPath = projectPath) }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun getSessionMessages(
        sessionId: String,
        provider: String,
        projectName: String?,
        projectPath: String?
    ): List<ChatMessage> = try {
        parseArrayBody(client.get("$baseUrl/api/sessions/$sessionId/messages") {
            authHeader()
            parameter("provider", provider)
            if (!projectName.isNullOrBlank()) parameter("projectName", projectName)
            if (!projectPath.isNullOrBlank()) parameter("projectPath", projectPath)
        }.body())
            .mapNotNull { it.toChatMessage() }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun getSkills(): List<Skill> = try {
        parseArrayBody(client.get("$baseUrl/api/skills") { authHeader() }.body())
            .mapNotNull { it.toSkill() }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun search(query: String): List<SearchResult> = try {
        val sseBody = client.get("$baseUrl/api/search/conversations") {
            authHeader()
            parameter("q", query)
            parameter("limit", 50)
        }.bodyAsText()
        parseSearchSse(sseBody)
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun sendMessageAndStream(
        session: Session,
        content: String,
        onEvent: (ChatStreamEvent) -> Unit
    ) {
        val sessionId = session.id
        val wsBase = baseUrl
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")

        val tokenParam = authToken.takeIf { it.isNotBlank() }
        val wsUrl = buildString {
            append("$wsBase/ws?sessionId=")
            append(sessionId)
            if (!tokenParam.isNullOrBlank()) {
                append("&token=")
                append(tokenParam)
            }
        }

        onEvent(ChatStreamEvent.Status("Comprobando estado de sesión..."))
        val isBusy = checkSessionBusy(wsUrl, session)
        if (isBusy) {
            val msg = "La sesión está activa en otro cliente. Abre una sesión nueva en Orbital para enviar mensajes."
            Log.e(logTag, "session busy session=$sessionId provider=${session.provider}")
            onEvent(ChatStreamEvent.Error(msg))
            return
        }

        var wsError: Exception? = null
        repeat(MAX_WS_ATTEMPTS) { attempt ->
            try {
                onEvent(ChatStreamEvent.Status(if (attempt == 0) "Conectando..." else "Reconectando... intento ${attempt + 1}/$MAX_WS_ATTEMPTS"))
                streamViaWebSocket(
                    wsUrl = wsUrl,
                    session = session,
                    content = content,
                    onEvent = onEvent
                )
                return
            } catch (e: Exception) {
                wsError = e
                Log.e(logTag, "ws attempt=${attempt + 1} failed session=${session.id}: ${e.message}")
                if (attempt < MAX_WS_ATTEMPTS - 1) {
                    val backoff = BASE_RETRY_DELAY_MS * (attempt + 1)
                    onEvent(ChatStreamEvent.Status("Reintentando conexión en ${backoff}ms..."))
                    delay(backoff)
                }
            }
        }

        // Fallback to REST send + synthetic completion if WS is not available.
        try {
            onEvent(ChatStreamEvent.Status("Usando fallback HTTP..."))
            client.post("$baseUrl/api/sessions/$sessionId/messages") {
                authHeader()
                parameter("provider", session.provider)
                if (!session.projectName.isNullOrBlank()) parameter("projectName", session.projectName)
                if (!session.projectPath.isNullOrBlank()) parameter("projectPath", session.projectPath)
                contentType(ContentType.Application.Json)
                setBody(
                    mapOf(
                        "content" to content,
                        "provider" to session.provider,
                        "sessionId" to sessionId
                    )
                )
            }
            Log.d(logTag, "fallback REST send ok session=$sessionId")
            onEvent(ChatStreamEvent.Done)
        } catch (fallbackErr: Exception) {
            Log.e(logTag, "fallback REST failed session=$sessionId: ${fallbackErr.message}")
            onEvent(ChatStreamEvent.Error((wsError?.message ?: fallbackErr.message ?: "stream failed").toFriendlyNetworkError()))
        }
    }

    private suspend fun checkSessionBusy(wsUrl: String, session: Session): Boolean {
        // Codex/Cursor/Gemini sessions can be occupied by another connected client.
        if (session.provider.lowercase() !in setOf("codex", "cursor", "gemini", "claude")) return false

        var isBusy = false
        return runCatching {
            client.webSocket(urlString = wsUrl) {
                val payload = JsonObject(
                    mapOf(
                        "type" to JsonPrimitive("check-session-status"),
                        "provider" to JsonPrimitive(session.provider),
                        "sessionId" to JsonPrimitive(session.id)
                    )
                )
                send(Frame.Text(json.encodeToString(JsonObject.serializer(), payload)))

                val statusObj = withTimeoutOrNull(1500L) {
                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        val lines = frame.readText().split('\n').map { it.trim() }.filter { it.isNotBlank() }
                        for (line in lines) {
                            val obj = runCatching { json.parseToJsonElement(line) as? JsonObject }.getOrNull() ?: continue
                            val type = obj.string("type")?.lowercase() ?: continue
                            if (type == "session-status") return@withTimeoutOrNull obj
                        }
                    }
                    null
                } as? JsonObject

                isBusy = statusObj?.get("isProcessing")
                    ?.let { (it as? JsonPrimitive)?.content?.toBooleanStrictOrNull() }
                    ?: false
            }
            isBusy
        }.getOrDefault(false)
    }

    private suspend fun streamViaWebSocket(
        wsUrl: String,
        session: Session,
        content: String,
        onEvent: (ChatStreamEvent) -> Unit
    ) {
        Log.d(logTag, "ws connect session=${session.id} provider=${session.provider}")
        client.webSocket(urlString = wsUrl) {
            val commandType = when (session.provider.lowercase()) {
                "codex" -> "codex-command"
                "cursor" -> "cursor-command"
                "gemini" -> "gemini-command"
                else -> "claude-command"
            }
            val payload = JsonObject(
                mapOf(
                    "type" to JsonPrimitive(commandType),
                    "command" to JsonPrimitive(content),
                    "options" to JsonObject(
                        buildMap {
                            put("sessionId", JsonPrimitive(session.id))
                            put("resume", JsonPrimitive(false))
                            if (!session.projectPath.isNullOrBlank()) {
                                put("projectPath", JsonPrimitive(session.projectPath))
                                put("cwd", JsonPrimitive(session.projectPath))
                            }
                        }
                    )
                )
            )
            send(Frame.Text(json.encodeToString(JsonObject.serializer(), payload)))
            Log.d(logTag, "ws sent command type=$commandType len=${content.length}")

            var pendingLine = ""
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val payloadText = pendingLine + frame.readText()
                val lines = payloadText.split('\n')
                pendingLine = lines.lastOrNull().orEmpty()
                lines.dropLast(1).forEach { line ->
                    val event = parseStreamEvent(line)
                    if (event !is ChatStreamEvent.Noop) {
                        Log.d(logTag, "ws event=${event::class.simpleName} lineLen=${line.length}")
                    }
                    onEvent(event)
                }
            }
            if (pendingLine.isNotBlank()) {
                val event = parseStreamEvent(pendingLine)
                if (event !is ChatStreamEvent.Noop) {
                    Log.d(logTag, "ws tail event=${event::class.simpleName} lineLen=${pendingLine.length}")
                }
                onEvent(event)
            }
        }
    }

    internal fun parseStreamEvent(line: String): ChatStreamEvent {
        val trimmed = line.trim()
        if (trimmed.isBlank()) return ChatStreamEvent.Noop
        if (trimmed.startsWith("event:") || trimmed.startsWith("data:")) return ChatStreamEvent.Noop
        if (trimmed in IGNORED_STREAM_MARKERS) return ChatStreamEvent.Noop

        return try {
            val element = json.parseToJsonElement(trimmed)
            if (element is JsonPrimitive && element.isString) {
                val decoded = element.content.trim()
                if (decoded.isBlank()) return ChatStreamEvent.Noop
                // Some backends wrap JSON payloads as escaped strings.
                if (decoded.startsWith("{") || decoded.startsWith("[")) return parseStreamEvent(decoded)
                return ChatStreamEvent.Output(decoded)
            }
            val obj = element as? JsonObject ?: return ChatStreamEvent.Noop
            val kind = (obj.string("kind") ?: obj.string("type") ?: obj.string("event") ?: "").lowercase()
            when (kind) {
                "output", "delta", "stream_delta", "text_delta" -> {
                    val text = obj.extractAssistantText()
                    if (text.isNotBlank()) ChatStreamEvent.Output(text) else ChatStreamEvent.Noop
                }
                "text" -> {
                    val role = (obj.string("role") ?: "assistant").lowercase()
                    if (role == "assistant") {
                        val text = obj.extractAssistantText()
                        if (text.isNotBlank()) ChatStreamEvent.Output(text) else ChatStreamEvent.Noop
                    } else {
                        ChatStreamEvent.Noop
                    }
                }
                "tool_use" -> ChatStreamEvent.ToolUse(
                    tool = obj.string("tool") ?: obj.string("name") ?: "tool",
                    inputSummary = obj.string("toolInput") ?: obj.string("input") ?: ""
                )
                "permission_request" -> {
                    val tool = obj.string("toolName") ?: obj.string("tool") ?: "tool"
                    val inputSummary = obj["input"]?.compactJsonSummary().orEmpty()
                    val prompt = buildString {
                        append("Aprobación requerida para ")
                        append(tool)
                        if (inputSummary.isNotBlank()) {
                            append(": ")
                            append(inputSummary)
                        }
                    }
                    ChatStreamEvent.ActionRequired(prompt)
                }
                "interactive_prompt" -> {
                    val content = obj.string("content") ?: obj.string("text") ?: "El agente requiere una respuesta."
                    ChatStreamEvent.ActionRequired(content)
                }
                "permission_cancelled" -> ChatStreamEvent.Status("Solicitud de aprobación cancelada")
                "status" -> {
                    val text = obj.string("text") ?: obj.string("message") ?: ""
                    if (text.isBlank() || text.lowercase() in IGNORED_STREAM_MARKERS) ChatStreamEvent.Noop else ChatStreamEvent.Status(text)
                }
                "done", "complete", "stream_end" -> ChatStreamEvent.Done
                "error" -> ChatStreamEvent.Error(obj.string("message") ?: obj.string("content") ?: "unknown error")
                else -> {
                    val text = obj.extractAssistantText()
                    if (text.isNotBlank()) ChatStreamEvent.Output(text) else ChatStreamEvent.Noop
                }
            }
        } catch (_: Exception) {
            // Keep genuine plain-text chunks, but ignore malformed JSON-ish payloads.
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) ChatStreamEvent.Noop else ChatStreamEvent.Output(trimmed)
        }
    }

    private fun JsonObject.extractAssistantText(): String {
        val explicitRole = string("role")?.lowercase()
        if (!explicitRole.isNullOrBlank() && explicitRole != "assistant") return ""

        string("text")?.takeIf { it.isNotBlank() }?.let { return it }
        string("content")?.takeIf { it.isNotBlank() }?.let { return it }
        string("delta")?.takeIf { it.isNotBlank() }?.let { return it }
        string("data")?.takeIf { it.isNotBlank() }?.let { return it }

        val deltaObj = this["delta"] as? JsonObject
        deltaObj?.string("text")?.takeIf { it.isNotBlank() }?.let { return it }
        deltaObj?.string("content")?.takeIf { it.isNotBlank() }?.let { return it }

        val payloadObj = this["payload"] as? JsonObject
        payloadObj?.string("text")?.takeIf { it.isNotBlank() }?.let { return it }
        payloadObj?.string("content")?.takeIf { it.isNotBlank() }?.let { return it }

        val contentArray = this["content"] as? JsonArray
        if (contentArray != null) {
            val chunk = contentArray.asSequence()
                .mapNotNull { it as? JsonObject }
                .mapNotNull { part ->
                    val type = part.string("type")?.lowercase()
                    when (type) {
                        null, "", "text", "output_text", "text_delta" -> {
                            part.string("text")
                                ?: (part["text"] as? JsonObject)?.string("value")
                                ?: part.string("content")
                        }
                        else -> null
                    }
                }
                .firstOrNull { it.isNotBlank() }
            if (!chunk.isNullOrBlank()) return chunk
        }

        return ""
    }

    companion object {
        private const val MAX_WS_ATTEMPTS = 3
        private const val BASE_RETRY_DELAY_MS = 400L
        val IGNORED_STREAM_MARKERS = setOf(
            "token_budget",
            "usage",
            "metrics",
            "heartbeat",
            "ping",
            "pong"
        )
    }

    private fun String.toFriendlyNetworkError(): String {
        val lower = lowercase()
        return when {
            "timeout" in lower || "failed to connect" in lower || "connect_timeout" in lower ->
                "No se pudo conectar con Orbital server. Revisa red/Tailscale y que el backend esté activo."
            "connection abort" in lower || "socket" in lower ->
                "La conexión se interrumpió durante el stream. Intenta reenviar el mensaje."
            else -> this
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.authHeader() {
        if (authToken.isNotBlank()) header(HttpHeaders.Authorization, "Bearer $authToken")
    }

    private fun JsonElement.compactJsonSummary(maxLen: Int = 120): String {
        val raw = when (this) {
            is JsonPrimitive -> content
            else -> toString()
        }.replace("\\s+".toRegex(), " ").trim()
        if (raw.length <= maxLen) return raw
        return raw.take(maxLen - 1) + "…"
    }

    private fun parseArrayBody(body: JsonElement): List<JsonObject> = when (body) {
        is JsonArray -> body.mapNotNull { it as? JsonObject }
        is JsonObject -> {
            val nestedArray = listOf(
                "data",
                "items",
                "projects",
                "sessions",
                "agents",
                "skills",
                "messages"
            ).firstNotNullOfOrNull { key -> body[key] as? JsonArray }
            nestedArray?.mapNotNull { it as? JsonObject } ?: emptyList()
        }
        else -> emptyList()
    }

    private fun JsonObject.toProject(): Project? {
        val name = string("name") ?: string("projectName") ?: return null
        val path = string("path") ?: string("dir") ?: ""
        val sessionCount = int("sessionCount") ?: int("sessions") ?: 0
        return Project(name = name, path = path, sessionCount = sessionCount)
    }

    private fun JsonObject.toAgent(): Agent? {
        val id = string("id") ?: string("slug") ?: return null
        val name = string("name") ?: id.replaceFirstChar { it.uppercase() }
        val model = string("model") ?: "unknown"
        val status = when ((string("status") ?: "offline").lowercase()) {
            "active" -> AgentStatus.ACTIVE
            "idle" -> AgentStatus.IDLE
            else -> AgentStatus.OFFLINE
        }
        val sessions = int("sessions") ?: int("sessionCount") ?: 0
        val icon = string("icon") ?: "◎"
        return Agent(id = id, name = name, model = model, status = status, sessions = sessions, icon = icon)
    }

    private fun JsonObject.toSession(
        defaultProjectName: String? = null,
        defaultProvider: String = "claude",
        defaultProjectPath: String? = null
    ): Session? {
        val id = string("id") ?: int("id")?.toString() ?: string("sessionId") ?: return null
        val provider = string("provider") ?: defaultProvider
        val agent = string("agent") ?: string("agentName") ?: providerLabel(provider)
        val name = string("name") ?: string("title") ?: string("summary") ?: id
        val msgs = int("msgs") ?: int("messageCount") ?: int("messages") ?: 0
        val status = (string("status") ?: "idle").lowercase()
        val updatedAt = long("updatedAt") ?: long("updatedAtMs") ?: long("lastUpdated")
            ?: parseDateMillis(string("lastActivity"))
        val ago = string("ago") ?: updatedAt?.let(::formatAgo) ?: "now"
        val projectName = string("projectName") ?: string("project") ?: defaultProjectName
        val projectPath = string("cwd") ?: string("projectPath") ?: defaultProjectPath
        return Session(
            id = id,
            agent = agent,
            name = name,
            msgs = msgs,
            ago = ago,
            status = status,
            projectName = projectName,
            projectPath = projectPath,
            provider = provider,
            updatedAtMs = updatedAt
        )
    }

    private fun JsonObject.toChatMessage(): ChatMessage? {
        val kind = string("kind")?.lowercase()
        if (kind == "tool_use") {
            val tool = string("toolName") ?: "tool"
            return ChatMessage(role = "a", text = "[tool] $tool")
        }
        val roleRaw = (string("role") ?: string("from") ?: "assistant").lowercase()
        val role = if (roleRaw.startsWith("u")) "u" else "a"
        val text = string("text") ?: string("content") ?: string("message") ?: return null
        return ChatMessage(role = role, text = text)
    }

    private fun JsonObject.toSkill(): Skill? {
        val name = string("name") ?: return null
        val tag = string("tag") ?: "general"
        val enabled = bool("on") ?: bool("enabled") ?: false
        return Skill(name = name, tag = tag, enabled = enabled)
    }

    private fun JsonObject.toSearchResult(): SearchResult? {
        val id = string("id") ?: string("sessionId") ?: string("projectName") ?: return null
        val type = string("type") ?: if (string("projectName") != null) "project" else "session"
        val title = string("title") ?: string("name") ?: id
        val subtitle = string("subtitle") ?: string("path") ?: string("agent") ?: ""
        return SearchResult(id = id, type = type, title = title, subtitle = subtitle)
    }

    private fun parseSearchSse(body: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val lines = body.lineSequence().toList()
        var currentEvent: String? = null
        var currentData: String? = null

        fun flushEvent() {
            if (currentEvent != "result" || currentData.isNullOrBlank()) return
            val eventObj = runCatching { json.parseToJsonElement(currentData!!) as? JsonObject }.getOrNull() ?: return
            val projectResult = eventObj["projectResult"] as? JsonObject ?: return
            val projectName = (projectResult["projectDisplayName"] as? JsonPrimitive)?.content
                ?: (projectResult["projectName"] as? JsonPrimitive)?.content
                ?: ""
            val sessions = projectResult["sessions"] as? JsonArray ?: JsonArray(emptyList())
            sessions.mapNotNull { it as? JsonObject }.forEach { session ->
                val sessionId = (session["sessionId"] as? JsonPrimitive)?.content ?: return@forEach
                val provider = (session["provider"] as? JsonPrimitive)?.content ?: "session"
                val summary = (session["sessionSummary"] as? JsonPrimitive)?.content ?: sessionId
                val matches = session["matches"] as? JsonArray
                val firstSnippet = matches
                    ?.mapNotNull { it as? JsonObject }
                    ?.firstOrNull()
                    ?.get("snippet")
                    ?.let { it as? JsonPrimitive }
                    ?.content
                    ?: ""
                results += SearchResult(
                    id = "$provider:$sessionId",
                    type = provider,
                    title = summary,
                    subtitle = listOf(projectName, firstSnippet).filter { it.isNotBlank() }.joinToString(" · ")
                )
            }
        }

        lines.forEach { line ->
            when {
                line.startsWith("event:") -> currentEvent = line.removePrefix("event:").trim()
                line.startsWith("data:") -> currentData = line.removePrefix("data:").trim()
                line.isBlank() -> {
                    flushEvent()
                    currentEvent = null
                    currentData = null
                }
            }
        }
        flushEvent()
        return results.distinctBy { it.id }
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }

    private fun JsonObject.int(key: String): Int? =
        (this[key] as? JsonPrimitive)?.content?.toIntOrNull()

    private fun JsonObject.long(key: String): Long? =
        (this[key] as? JsonPrimitive)?.content?.toLongOrNull()

    private fun JsonObject.bool(key: String): Boolean? =
        (this[key] as? JsonPrimitive)?.content?.toBooleanStrictOrNull()

    private fun inferProvider(agent: String): String {
        val lower = agent.lowercase()
        return when {
            "codex" in lower -> "codex"
            "cursor" in lower -> "cursor"
            "gemini" in lower -> "gemini"
            else -> "claude"
        }
    }

    private fun providerLabel(provider: String): String = when (provider.lowercase()) {
        "codex" -> "Codex CLI"
        "cursor" -> "Cursor"
        "gemini" -> "Gemini CLI"
        else -> "Claude Code"
    }

    private fun parseDateMillis(value: String?): Long? =
        try { if (value.isNullOrBlank()) null else java.time.Instant.parse(value).toEpochMilli() }
        catch (_: Exception) { null }

    private fun formatAgo(updatedAtMs: Long): String {
        val seconds = ((System.currentTimeMillis() - updatedAtMs).coerceAtLeast(0L)) / 1000L
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m"
            seconds < 86400 -> "${seconds / 3600}h"
            else -> "${seconds / 86400}d"
        }
    }
}
