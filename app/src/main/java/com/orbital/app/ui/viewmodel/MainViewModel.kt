package com.orbital.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbital.app.data.AppearanceStore
import com.orbital.app.data.OrbitalRepository
import com.orbital.app.domain.Agent
import com.orbital.app.domain.AgentStatus
import com.orbital.app.domain.AppearanceSettings
import com.orbital.app.domain.Session
import com.orbital.app.domain.Skill
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: OrbitalRepository,
    private val appearanceStore: AppearanceStore
) : ViewModel() {

    // ─── Appearance (persisted) ───────────────────────────────────
    val appearance = appearanceStore.appearance.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppearanceSettings()
    )

    // ─── Live lists ───────────────────────────────────────────────
    val agents   = mutableStateListOf<Agent>()
    val sessions = mutableStateListOf<Session>()
    val skills   = mutableStateListOf<Skill>()

    // ─── Server info ──────────────────────────────────────────────
    var serverName by mutableStateOf("elitebook")
        private set
    var serverHost by mutableStateOf("")
        private set
    var latencyMs  by mutableStateOf(0)
        private set

    init { loadMockData() }

    private fun loadMockData() {
        agents.addAll(listOf(
            Agent("claude", "Claude Code", "claude-opus-4-6",   AgentStatus.ACTIVE,  8, "◎"),
            Agent("codex",  "Codex CLI",   "codex-mini-latest", AgentStatus.IDLE,    3, "◈"),
            Agent("gemini", "Gemini CLI",  "gemini-2.5-pro",    AgentStatus.OFFLINE, 0, "◇"),
            Agent("aider",  "Aider",       "gpt-4o",            AgentStatus.OFFLINE, 0, "◉"),
        ))
        sessions.addAll(listOf(
            Session(1, "Claude Code", "refactor-auth-middleware",  14, "2m",  "active"),
            Session(2, "Codex CLI",   "orbital-api-schema",         8, "1h",  "idle"),
            Session(3, "Claude Code", "fix-nightshift-flock",      32, "3h",  "idle"),
            Session(4, "Claude Code", "northstar-vector-research", 47, "1d",  "idle"),
            Session(5, "Codex CLI",   "krinekk-os-schema-review",  19, "2d",  "idle"),
        ))
        skills.addAll(listOf(
            Skill("Sequential Thinking", "reasoning", true),
            Skill("Memory Summarizer",   "memory",    false),
            Skill("Git Reviewer",        "git",       true),
            Skill("Test Generator",      "testing",   false),
            Skill("Doc Writer",          "docs",      false),
        ))
    }

    fun setServer(name: String, host: String, latency: Int) {
        serverName = name
        serverHost = host
        latencyMs  = latency
        repository.setServerUrl("http://$host:8420")
        refreshFromServer()
    }

    private fun refreshFromServer() {
        viewModelScope.launch {
            val remoteAgents   = repository.getAgents()
            val remoteSessions = repository.getSessions()
            val remoteSkills   = repository.getSkills()
            if (remoteAgents.isNotEmpty())   { agents.clear();   agents.addAll(remoteAgents)     }
            if (remoteSessions.isNotEmpty()) { sessions.clear(); sessions.addAll(remoteSessions) }
            if (remoteSkills.isNotEmpty())   { skills.clear();   skills.addAll(remoteSkills)     }
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
}
