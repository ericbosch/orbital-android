package com.orbital.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbital.app.core.network.DiscoveredServer
import com.orbital.app.core.network.NsdDiscoveryService
import com.orbital.app.core.network.TailscaleDiscoveryService
import com.orbital.app.data.AppearanceStore
import com.orbital.app.data.OrbitalRepository
import com.orbital.app.domain.Agent
import com.orbital.app.domain.AgentStatus
import com.orbital.app.domain.AppearanceSettings
import com.orbital.app.domain.DiagnosticCheck
import com.orbital.app.domain.Project
import com.orbital.app.domain.SearchResult
import com.orbital.app.domain.Session
import com.orbital.app.domain.Skill
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis
import javax.inject.Inject

sealed class ConnectionState {
    data object Idle : ConnectionState()
    data object Scanning : ConnectionState()
    data class Connecting(val server: DiscoveredServer) : ConnectionState()
    data class Connected(val url: String, val name: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: OrbitalRepository,
    private val appearanceStore: AppearanceStore,
    private val nsdDiscovery: NsdDiscoveryService,
    private val tailscaleDiscovery: TailscaleDiscoveryService
) : ViewModel() {

    val appearance = appearanceStore.appearance.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppearanceSettings()
    )

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredServers = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    val discoveredServers: StateFlow<List<DiscoveredServer>> = _discoveredServers.asStateFlow()

    val projects = mutableStateListOf<Project>()
    val agents = mutableStateListOf<Agent>()
    val sessions = mutableStateListOf<Session>()
    val skills = mutableStateListOf<Skill>()
    val searchResults = mutableStateListOf<SearchResult>()
    val diagnostics = mutableStateListOf<DiagnosticCheck>()

    var serverName by mutableStateOf("")
    var serverHost by mutableStateOf("")
    var latencyMs by mutableStateOf(0)
    var authToken by mutableStateOf("")
    var isSearching by mutableStateOf(false)
    var diagnosticsRunning by mutableStateOf(false)

    private var scanJob: Job? = null

    init {
        loadMockData()
        checkStoredServer()
    }

    private fun checkStoredServer() {
        viewModelScope.launch {
            repository.getStoredServer().collect { stored ->
                if (stored != null && _connectionState.value is ConnectionState.Idle) {
                    repository.setServerUrl(stored.url)
                    repository.setAuthToken(stored.token)
                    val name = stored.url.removePrefix("http://").substringBefore(":")
                    serverHost = stored.url.removePrefix("http://").substringBefore(":")
                    serverName = name
                    authToken = stored.token
                    _connectionState.value = ConnectionState.Connected(stored.url, name)
                    refreshFromServer()
                }
            }
        }
    }

    fun startScan() {
        _connectionState.value = ConnectionState.Scanning
        _discoveredServers.value = emptyList()
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            launch {
                nsdDiscovery.discoverServers().collect { lan ->
                    _discoveredServers.value = _discoveredServers.value.filter { it.via != "LAN" } + lan
                }
            }
            launch {
                val savedName = repository.getStoredServerName().firstOrNull() ?: ""
                val ts = tailscaleDiscovery.discoverServers(savedHostnames = listOfNotNull(savedName.ifBlank { null }))
                if (ts.isNotEmpty()) {
                    _discoveredServers.value = _discoveredServers.value.filter { it.via != "Tailscale" } + ts
                }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        if (_connectionState.value is ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Idle
        }
    }

    fun connect(server: DiscoveredServer, token: String) {
        viewModelScope.launch {
            stopScan()
            _connectionState.value = ConnectionState.Connecting(server)
            val url = "http://${server.host}:${server.port}"
            var ok = false
            latencyMs = measureTimeMillis {
                ok = repository.ping(url, token)
            }.toInt()

            if (ok) {
                repository.saveServer(url, token, server.name)
                repository.setServerUrl(url)
                repository.setAuthToken(token)
                authToken = token
                serverName = server.name
                serverHost = server.host
                _connectionState.value = ConnectionState.Connected(url, server.name)
                refreshFromServer()
            } else {
                _connectionState.value = ConnectionState.Error("No se pudo conectar a ${server.name}")
            }
        }
    }

    fun connectManual(host: String, port: Int, token: String) {
        val server = DiscoveredServer(name = host, host = host, port = port, via = "Manual")
        connect(server, token)
    }

    fun updateServerToken(newToken: String) {
        val connected = _connectionState.value as? ConnectionState.Connected ?: return
        viewModelScope.launch {
            val ok = repository.ping(connected.url, newToken)
            if (ok) {
                repository.saveServer(connected.url, newToken, connected.name)
                repository.setAuthToken(newToken)
                authToken = newToken
                clearError()
                refreshFromServer()
            } else {
                _connectionState.value = ConnectionState.Error("Token inválido o expirado")
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            repository.clearServer()
            serverName = ""
            serverHost = ""
            authToken = ""
            projects.clear()
            searchResults.clear()
            diagnostics.clear()
            _connectionState.value = ConnectionState.Idle
        }
    }

    fun clearError() {
        if (_connectionState.value is ConnectionState.Error) {
            _connectionState.value = ConnectionState.Idle
        }
    }

    fun refreshFromServer() {
        viewModelScope.launch {
            val remoteProjects = repository.getProjects()
            if (remoteProjects.isNotEmpty()) {
                projects.clear()
                projects.addAll(remoteProjects)
            }

            val remoteAgents = repository.getAgents()
            if (remoteAgents.isNotEmpty()) {
                agents.clear()
                agents.addAll(remoteAgents)
            }

            val loadedSessions = if (remoteProjects.isNotEmpty()) {
                remoteProjects.flatMap { project ->
                    val claude = repository.getSessions(project.name).map { session ->
                        session.copy(
                            projectName = session.projectName ?: project.name,
                            projectPath = session.projectPath ?: project.path,
                            provider = if (session.provider.isBlank()) "claude" else session.provider
                        )
                    }
                    val codex = repository.getCodexSessions(project.path).map { session ->
                        session.copy(
                            projectName = session.projectName ?: project.name,
                            projectPath = session.projectPath ?: project.path,
                            provider = "codex"
                        )
                    }
                    val cursor = repository.getCursorSessions(project.path).map { session ->
                        session.copy(
                            projectName = session.projectName ?: project.name,
                            projectPath = session.projectPath ?: project.path,
                            provider = "cursor"
                        )
                    }
                    claude + codex + cursor
                }
            } else {
                repository.getSessions()
            }

            if (loadedSessions.isNotEmpty()) {
                sessions.clear()
                sessions.addAll(
                    loadedSessions
                        .distinctBy { it.id }
                        .sortedWith(compareByDescending<Session> { it.updatedAtMs ?: 0L }.thenByDescending { it.msgs })
                )
            }

            val remoteSkills = repository.getSkills()
            if (remoteSkills.isNotEmpty()) {
                skills.clear()
                skills.addAll(remoteSkills)
            }
        }
    }

    fun runDiagnostics() {
        val connected = _connectionState.value as? ConnectionState.Connected ?: return
        viewModelScope.launch {
            diagnosticsRunning = true
            diagnostics.clear()

            val pingOk = repository.ping(connected.url, authToken)
            diagnostics.add(DiagnosticCheck("Pinging host", pingOk, connected.url))

            val projectsOk = repository.getProjects().isNotEmpty()
            diagnostics.add(DiagnosticCheck("Projects endpoint", projectsOk, "/api/projects"))

            val agentsOk = repository.getAgents().isNotEmpty()
            diagnostics.add(DiagnosticCheck("Agents endpoint", agentsOk, "/api/agents"))

            val sessionsOk = projects
                .ifEmpty { repository.getProjects() }
                .any { repository.getSessions(it.name).isNotEmpty() }
            diagnostics.add(DiagnosticCheck("Sessions endpoint", sessionsOk, "/api/projects/:projectName/sessions"))

            val skillsOk = repository.getSkills().isNotEmpty()
            diagnostics.add(DiagnosticCheck("Skills endpoint", skillsOk, "/api/skills"))

            diagnosticsRunning = false
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            searchResults.clear()
            return
        }
        viewModelScope.launch {
            isSearching = true
            val results = repository.search(query)
            searchResults.clear()
            searchResults.addAll(results)
            isSearching = false
        }
    }

    fun updateAppearance(settings: AppearanceSettings) {
        viewModelScope.launch { appearanceStore.save(settings) }
    }

    fun toggleSkill(index: Int) {
        if (index in skills.indices) {
            skills[index] = skills[index].copy(enabled = !skills[index].enabled)
        }
    }

    private fun loadMockData() {
        agents.addAll(
            listOf(
                Agent("claude", "Claude Code", "claude-opus-4-6", AgentStatus.ACTIVE, 8, "◎"),
                Agent("codex", "Codex CLI", "codex-mini-latest", AgentStatus.IDLE, 3, "◈"),
                Agent("gemini", "Gemini CLI", "gemini-2.5-pro", AgentStatus.OFFLINE, 0, "◇"),
                Agent("aider", "Aider", "gpt-4o", AgentStatus.OFFLINE, 0, "◉")
            )
        )
        projects.addAll(
            listOf(
                Project("orbital-android", "~/dev/orbital-android", 3),
                Project("orbital-server", "~/dev/orbital/server", 2)
            )
        )
        sessions.addAll(
            listOf(
                Session("1", "Claude Code", "refactor-auth-middleware", 14, "2m", "active", "orbital-server"),
                Session(
                    "2",
                    "Codex CLI",
                    "orbital-api-schema",
                    8,
                    "1h",
                    "idle",
                    "orbital-server",
                    "~/dev/orbital/server",
                    "codex"
                ),
                Session("3", "Claude Code", "fix-nightshift-flock", 32, "3h", "idle", "orbital-android", "~/dev/orbital-android", "claude"),
                Session("4", "Claude Code", "northstar-vector-research", 47, "1d", "idle", "orbital-android", "~/dev/orbital-android", "claude"),
                Session("5", "Codex CLI", "krinekk-os-schema-review", 19, "2d", "idle", "orbital-android", "~/dev/orbital-android", "codex")
            )
        )
        skills.addAll(
            listOf(
                Skill("Sequential Thinking", "reasoning", true),
                Skill("Memory Summarizer", "memory", false),
                Skill("Git Reviewer", "git", true),
                Skill("Test Generator", "testing", false),
                Skill("Doc Writer", "docs", false)
            )
        )
    }
}
