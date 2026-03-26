package com.orbital.app.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import com.orbital.app.domain.BackendProfile
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.orbital.app.domain.ChatStreamEvent

class OrbitalApiClientTest {

    @Test
    fun `ping succeeds against orbitdock health endpoint`() = runBlocking {
        val api = clientForResponses(
            mapOf(
                "/api/health" to (HttpStatusCode.NotFound to ""),
                "/health" to (HttpStatusCode.OK to """{"status":"ok","version":"0.4.0"}""")
            )
        )

        val ok = api.ping("http://localhost:4000", "token")
        assertTrue(ok)
    }

    @Test
    fun `detect backend profile recognizes orbitdock via health`() = runBlocking {
        val api = clientForResponses(
            mapOf(
                "/api/server/meta" to (HttpStatusCode.NotFound to ""),
                "/api/health" to (HttpStatusCode.NotFound to ""),
                "/health" to (HttpStatusCode.OK to """{"status":"ok","version":"0.4.0"}""")
            )
        )

        val profile = api.detectBackendProfile("http://localhost:4000", "token")
        assertEquals(BackendProfile.ORBITDOCK, profile)
    }

    @Test
    fun `detect backend profile falls back to orbitdock when probes fail`() = runBlocking {
        val api = clientForResponses(
            mapOf(
                "/api/server/meta" to (HttpStatusCode.NotFound to ""),
                "/health" to (HttpStatusCode.NotFound to ""),
                "/api/health" to (HttpStatusCode.NotFound to ""),
                "/api/sessions" to (HttpStatusCode.InternalServerError to "")
            )
        )

        val profile = api.detectBackendProfile("http://localhost:4000", "token")
        assertEquals(BackendProfile.ORBITDOCK, profile)
    }

    @Test
    fun `detect backend profile recognizes explicit orbital marker`() = runBlocking {
        val api = clientForResponses(
            mapOf(
                "/api/server/meta" to (HttpStatusCode.OK to """{"name":"orbital-server"}""")
            )
        )

        val profile = api.detectBackendProfile("http://localhost:4000", "token")
        assertEquals(BackendProfile.ORBITAL, profile)
    }

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
        api.setBackendProfile(BackendProfile.ORBITAL)

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
        api.setBackendProfile(BackendProfile.ORBITAL)

        val sessions = api.getSessions()
        assertEquals(2, sessions.size)
        assertEquals("1", sessions[0].id)
        assertEquals("abc-2", sessions[1].id)
        assertEquals("Codex CLI", sessions[1].agent)
        assertEquals(9, sessions[1].msgs)
        assertTrue(sessions[1].ago.isNotBlank())
    }

    @Test
    fun `parses orbitdock sessions when backend profile is orbitdock`() = runBlocking {
        val payload = """
            {
              "sessions": [
                {"id":"od-1","provider":"codex","project_path":"/workspace/repo-a","status":"active"}
              ]
            }
        """.trimIndent()

        val api = clientFor(pathToBody = mapOf("/api/sessions" to payload))
        api.setBaseUrl("http://localhost:8080")
        api.setBackendProfile(BackendProfile.ORBITDOCK)

        val sessions = api.getSessions()
        assertEquals(1, sessions.size)
        assertEquals("od-1", sessions[0].id)
        assertEquals("/workspace/repo-a", sessions[0].projectPath)
        assertEquals("repo-a", sessions[0].projectName)
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
        api.setBackendProfile(BackendProfile.ORBITAL)

        val agents = api.getAgents()
        assertEquals(2, agents.size)
        assertEquals("claude", agents[0].id)
        assertEquals("cursor", agents[1].id)
    }

    @Test
    fun `builds agents from orbitdock sessions when agents endpoint is absent`() = runBlocking {
        val payload = """
            {
              "sessions": [
                {"id":"s1","provider":"codex","project_path":"/workspace/a","status":"active"},
                {"id":"s2","provider":"codex","project_path":"/workspace/b","status":"idle"},
                {"id":"s3","provider":"claude","project_path":"/workspace/a","status":"idle"}
              ]
            }
        """.trimIndent()

        val api = clientFor(pathToBody = mapOf("/api/sessions" to payload))
        api.setBaseUrl("http://localhost:8080")
        api.setBackendProfile(BackendProfile.ORBITDOCK)

        val agents = api.getAgents()
        assertEquals(2, agents.size)
        assertEquals("claude", agents[0].id)
        assertEquals("codex", agents[1].id)
        assertEquals(2, agents[1].sessions)
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
        api.setBackendProfile(BackendProfile.ORBITAL)

        val messages = api.getSessionMessages("s1", "claude", "orbital-android", "/workspace/orbital-android")
        assertEquals(2, messages.size)
        assertEquals("u", messages[0].role)
        assertEquals("a", messages[1].role)
    }

    @Test
    fun `parses orbitdock message rows payload`() = runBlocking {
        val payload = """
            {
              "rows": [
                {"row":{"row_type":"assistant","id":"m1","content":"hola"}},
                {"row":{"row_type":"tool","id":"t1","title":"Bash","summary":"ls -la"}}
              ]
            }
        """.trimIndent()

        val api = clientFor(pathToBody = mapOf("/api/sessions/s1/messages" to payload))
        api.setBaseUrl("http://localhost:8080")
        api.setBackendProfile(BackendProfile.ORBITDOCK)

        val messages = api.getSessionMessages("s1", "codex", null, null)
        assertEquals(2, messages.size)
        assertEquals("a", messages[0].role)
        assertEquals("hola", messages[0].text)
        assertEquals("[tool] Bash: ls -la", messages[1].text)
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
    fun `stream parser ignores escaped json envelope inside string`() {
        val api = clientFor(emptyMap())
        val event = api.parseStreamEvent("\"{\\\"id\\\":\\\"1\\\",\\\"event\\\":\\\"message\\\",\\\"topic\\\":\\\"krinekk-dev\\\",\\\"message\\\":\\\"ok\\\"}\"")
        assertEquals(ChatStreamEvent.Noop, event)
    }

    @Test
    fun `stream parser ignores sse metadata lines`() {
        val api = clientFor(emptyMap())
        val event = api.parseStreamEvent("event: result")
        assertEquals(ChatStreamEvent.Noop, event)
    }

    @Test
    fun `stream parser ignores marker lines`() {
        val api = clientFor(emptyMap())
        val event = api.parseStreamEvent("token_budget")
        assertEquals(ChatStreamEvent.Noop, event)
    }

    @Test
    fun `stream parser maps permission request to action required`() {
        val api = clientFor(emptyMap())
        val event = api.parseStreamEvent("""{"kind":"permission_request","toolName":"Bash","input":{"command":"git push"}}""")
        assertTrue(event is ChatStreamEvent.ActionRequired)
    }

    @Test
    fun `stream parser maps interactive prompt to action required`() {
        val api = clientFor(emptyMap())
        val event = api.parseStreamEvent("""{"kind":"interactive_prompt","content":"Approve this action?"}""")
        assertEquals(ChatStreamEvent.ActionRequired("Approve this action?"), event)
    }

    private fun clientFor(pathToBody: Map<String, String>): OrbitalApiClient {
        val responses = pathToBody.mapValues { HttpStatusCode.OK to it.value }
        return clientForResponses(responses)
    }

    private fun clientForResponses(pathToResponse: Map<String, Pair<HttpStatusCode, String>>): OrbitalApiClient {
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            val (status, body) = pathToResponse[path] ?: (HttpStatusCode.OK to "[]")
            respond(
                content = body,
                status = status,
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
