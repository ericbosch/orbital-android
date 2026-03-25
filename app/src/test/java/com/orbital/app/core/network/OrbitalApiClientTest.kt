package com.orbital.app.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrbitalApiClientTest {

    @Test
    fun `parses projects from wrapped data payload`() = runBlocking {
        val payload = """
            {
              "data": [
                {"name":"orbital-android","path":"/workspace/orbital-android","sessionCount":3},
                {"name":"orbital-server","path":"/workspace/orbital-server","sessionCount":2}
              ]
            }
        """.trimIndent()

        val api = clientFor(pathToBody = mapOf("/api/projects" to payload))
        api.setBaseUrl("http://localhost:8080")

        val projects = api.getProjects()
        assertEquals(2, projects.size)
        assertEquals("orbital-android", projects.first().name)
        assertEquals(3, projects.first().sessionCount)
    }

    @Test
    fun `parses sessions with mixed id types and computes defaults`() = runBlocking {
        val payload = """
            [
              {"id":1,"agent":"Claude Code","name":"task-1","msgs":4,"status":"active","updatedAt":1710000000000},
              {"id":"abc-2","agentName":"Codex CLI","title":"task-2","messageCount":9,"status":"idle"}
            ]
        """.trimIndent()

        val api = clientFor(pathToBody = mapOf("/api/sessions" to payload))
        api.setBaseUrl("http://localhost:8080")

        val sessions = api.getSessions()
        assertEquals(2, sessions.size)
        assertEquals("1", sessions[0].id)
        assertEquals("abc-2", sessions[1].id)
        assertEquals("Codex CLI", sessions[1].agent)
        assertEquals(9, sessions[1].msgs)
        assertTrue(sessions[1].ago.isNotBlank())
    }

    @Test
    fun `parses agents with status mapping`() = runBlocking {
        val payload = """
            {
              "agents": [
                {"id":"claude","name":"Claude Code","model":"claude-opus-4-6","status":"active","sessions":7,"icon":"◎"},
                {"id":"cursor","name":"Cursor","model":"gpt-4.1","status":"offline","sessions":0,"icon":"◉"}
              ]
            }
        """.trimIndent()

        val api = clientFor(pathToBody = mapOf("/api/agents" to payload))
        api.setBaseUrl("http://localhost:8080")

        val agents = api.getAgents()
        assertEquals(2, agents.size)
        assertEquals("claude", agents[0].id)
        assertEquals("cursor", agents[1].id)
    }

    private fun clientFor(pathToBody: Map<String, String>): OrbitalApiClient {
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            val body = pathToBody[path] ?: "[]"
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString())
            )
        }

        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        return OrbitalApiClient(client)
    }
}
