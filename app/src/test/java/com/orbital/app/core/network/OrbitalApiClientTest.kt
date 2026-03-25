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
import com.orbital.app.domain.ChatStreamEvent

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

    @Test
    fun `parses session message history payload`() = runBlocking {
        val payload = """
            {
              "messages": [
                {"role":"user","content":"hola"},
                {"role":"assistant","content":"que tal"}
              ]
            }
        """.trimIndent()

        val api = clientFor(pathToBody = mapOf("/api/sessions/s1/messages" to payload))
        api.setBaseUrl("http://localhost:8080")

        val messages = api.getSessionMessages("s1", "claude", "orbital-android", "/workspace/orbital-android")
        assertEquals(2, messages.size)
        assertEquals("u", messages[0].role)
        assertEquals("a", messages[1].role)
    }

    @Test
    fun `parses search results`() = runBlocking {
        val payload = """
            event: result
            data: {"projectResult":{"projectName":"orbital-android","projectDisplayName":"Orbital Android","sessions":[{"sessionId":"s-1","provider":"claude","sessionSummary":"Refactor auth","matches":[{"snippet":"jwt middleware"}]}]}}
            
            event: done
            data: {}
            
        """.trimIndent()

        val api = clientFor(pathToBody = mapOf("/api/search/conversations" to payload))
        api.setBaseUrl("http://localhost:8080")

        val results = api.search("orbital")
        assertEquals(1, results.size)
        assertEquals("claude", results[0].type)
        assertEquals("Refactor auth", results[0].title)
    }

    @Test
    fun `stream parser returns output for assistant text event`() {
        val api = clientFor(emptyMap())
        val event = api.parseStreamEvent("""{"type":"output","text":"hola"}""")
        assertEquals(ChatStreamEvent.Output("hola"), event)
    }

    @Test
    fun `stream parser ignores unknown json envelopes`() {
        val api = clientFor(emptyMap())
        val event = api.parseStreamEvent("""{"type":"session_state","state":"running","sessionId":"abc"}""")
        assertEquals(ChatStreamEvent.Noop, event)
    }

    @Test
    fun `stream parser ignores sse metadata lines`() {
        val api = clientFor(emptyMap())
        val event = api.parseStreamEvent("event: result")
        assertEquals(ChatStreamEvent.Noop, event)
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
