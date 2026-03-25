package com.orbital.app.core.network

import com.orbital.app.domain.Agent
import com.orbital.app.domain.AgentStatus
import com.orbital.app.domain.Session
import com.orbital.app.domain.Skill
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class AgentDto(
    val id: String,
    val name: String,
    val model: String,
    val status: String,
    val sessions: Int,
    val icon: String
)

@Serializable
data class SessionDto(
    val id: Int,
    val agent: String,
    val name: String,
    val msgs: Int,
    val ago: String,
    val status: String
)

@Serializable
data class SkillDto(
    val name: String,
    val tag: String,
    @SerialName("on") val enabled: Boolean
)

class OrbitalApiClient @Inject constructor(private val client: HttpClient) {

    private var baseUrl: String = "http://192.168.1.1:8420"

    fun setBaseUrl(url: String) { baseUrl = url }

    suspend fun getAgents(): List<Agent> = try {
        client.get("$baseUrl/api/agents").body<List<AgentDto>>().map { it.toDomain() }
    } catch (_: Exception) { emptyList() }

    suspend fun getSessions(): List<Session> = try {
        client.get("$baseUrl/api/sessions").body<List<SessionDto>>().map { it.toDomain() }
    } catch (_: Exception) { emptyList() }

    suspend fun getSkills(): List<Skill> = try {
        client.get("$baseUrl/api/skills").body<List<SkillDto>>().map { it.toDomain() }
    } catch (_: Exception) { emptyList() }

    private fun AgentDto.toDomain() = Agent(
        id = id, name = name, model = model,
        status = when (status) {
            "active" -> AgentStatus.ACTIVE
            "idle"   -> AgentStatus.IDLE
            else     -> AgentStatus.OFFLINE
        },
        sessions = sessions, icon = icon
    )

    private fun SessionDto.toDomain() =
        Session(id = id, agent = agent, name = name, msgs = msgs, ago = ago, status = status)

    private fun SkillDto.toDomain() = Skill(name = name, tag = tag, enabled = enabled)
}
