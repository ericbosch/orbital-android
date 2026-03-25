package com.orbital.app.core.network

import com.orbital.app.domain.Agent
import com.orbital.app.domain.AgentStatus
import com.orbital.app.domain.ChatMessage
import com.orbital.app.domain.ChatStreamEvent
import com.orbital.app.domain.Project
import com.orbital.app.domain.Session
import com.orbital.app.domain.Skill
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

class OrbitalApiClient @Inject constructor(private val client: HttpClient) {

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
        parseArrayBody(
            client.get("$baseUrl/api/sessions") {
                authHeader()
                if (!projectName.isNullOrBlank()) parameter("projectName", projectName)
            }.body()
        ).mapNotNull { it.toSession() }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun getSessionMessages(sessionId: String): List<ChatMessage> = try {
        parseArrayBody(client.get("$baseUrl/api/sessions/$sessionId/messages") { authHeader() }.body())
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

    suspend fun sendMessageAndStream(
        sessionId: String,
        content: String,
        onEvent: (ChatStreamEvent) -> Unit
    ) {
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

        try {
            client.webSocket(urlString = wsUrl) {
                val payload = JsonObject(
                    mapOf(
                        "type" to JsonPrimitive("message"),
                        "sessionId" to JsonPrimitive(sessionId),
                        "content" to JsonPrimitive(content)
                    )
                )
                send(Frame.Text(json.encodeToString(JsonObject.serializer(), payload)))

                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val chunks = frame.readText().split('\n').filter { it.isNotBlank() }
                    chunks.forEach { line ->
                        onEvent(parseStreamEvent(line))
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to REST send + synthetic completion if WS is not available.
            try {
                client.post("$baseUrl/api/sessions/$sessionId/messages") {
                    authHeader()
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("content" to content))
                }
                onEvent(ChatStreamEvent.Done)
            } catch (_: Exception) {
                onEvent(ChatStreamEvent.Error(e.message ?: "stream failed"))
            }
        }
    }

    private fun parseStreamEvent(line: String): ChatStreamEvent {
        return try {
            val obj = json.parseToJsonElement(line) as? JsonObject ?: return ChatStreamEvent.Output(line)
            when ((obj.string("type") ?: "output").lowercase()) {
                "output", "delta" -> ChatStreamEvent.Output(
                    obj.string("text") ?: obj.string("content") ?: obj.string("delta") ?: ""
                )
                "tool_use" -> ChatStreamEvent.ToolUse(
                    tool = obj.string("tool") ?: obj.string("name") ?: "tool",
                    inputSummary = obj.string("input") ?: ""
                )
                "done", "complete" -> ChatStreamEvent.Done
                "error" -> ChatStreamEvent.Error(obj.string("message") ?: "unknown error")
                else -> ChatStreamEvent.Output(obj.string("text") ?: line)
            }
        } catch (_: Exception) {
            ChatStreamEvent.Output(line)
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.authHeader() {
        if (authToken.isNotBlank()) header(HttpHeaders.Authorization, "Bearer $authToken")
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

    private fun JsonObject.toSession(): Session? {
        val id = string("id") ?: int("id")?.toString() ?: string("sessionId") ?: return null
        val agent = string("agent") ?: string("agentName") ?: "Unknown"
        val name = string("name") ?: string("title") ?: id
        val msgs = int("msgs") ?: int("messageCount") ?: int("messages") ?: 0
        val status = (string("status") ?: "idle").lowercase()
        val updatedAt = long("updatedAt") ?: long("updatedAtMs") ?: long("lastUpdated")
        val ago = string("ago") ?: updatedAt?.let(::formatAgo) ?: "now"
        val projectName = string("projectName") ?: string("project")
        return Session(
            id = id,
            agent = agent,
            name = name,
            msgs = msgs,
            ago = ago,
            status = status,
            projectName = projectName,
            updatedAtMs = updatedAt
        )
    }

    private fun JsonObject.toChatMessage(): ChatMessage? {
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

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }

    private fun JsonObject.int(key: String): Int? =
        (this[key] as? JsonPrimitive)?.content?.toIntOrNull()

    private fun JsonObject.long(key: String): Long? =
        (this[key] as? JsonPrimitive)?.content?.toLongOrNull()

    private fun JsonObject.bool(key: String): Boolean? =
        (this[key] as? JsonPrimitive)?.content?.toBooleanStrictOrNull()

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
