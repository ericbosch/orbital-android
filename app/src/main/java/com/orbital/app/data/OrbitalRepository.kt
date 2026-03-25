package com.orbital.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.orbital.app.core.network.OrbitalApiClient
import com.orbital.app.domain.Agent
import com.orbital.app.domain.ChatMessage
import com.orbital.app.domain.ChatStreamEvent
import com.orbital.app.domain.Project
import com.orbital.app.domain.SearchResult
import com.orbital.app.domain.Session
import com.orbital.app.domain.Skill
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class StoredServer(val url: String, val token: String)

@Singleton
class OrbitalRepository @Inject constructor(
    private val apiClient: OrbitalApiClient,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val SERVER_URL  = stringPreferencesKey("server_url")
        private val AUTH_TOKEN  = stringPreferencesKey("auth_token")
        private val SERVER_NAME = stringPreferencesKey("server_name")
    }

    fun getStoredServer(): Flow<StoredServer?> = dataStore.data.map { prefs ->
        val url = prefs[SERVER_URL] ?: return@map null
        val token = prefs[AUTH_TOKEN] ?: return@map null
        if (url.isNotBlank()) StoredServer(url, token) else null
    }

    fun getStoredServerName(): Flow<String> = dataStore.data.map { prefs ->
        prefs[SERVER_NAME] ?: ""
    }

    suspend fun saveServer(url: String, token: String, name: String) {
        dataStore.edit { prefs ->
            prefs[SERVER_URL] = url
            prefs[AUTH_TOKEN] = token
            prefs[SERVER_NAME] = name
        }
    }

    suspend fun clearServer() {
        dataStore.edit { prefs ->
            prefs.remove(SERVER_URL)
            prefs.remove(AUTH_TOKEN)
            prefs.remove(SERVER_NAME)
        }
    }

    suspend fun ping(url: String, token: String): Boolean = apiClient.ping(url, token)

    fun setServerUrl(url: String) = apiClient.setBaseUrl(url)
    fun setAuthToken(token: String) = apiClient.setAuthToken(token)

    suspend fun getProjects(): List<Project> = apiClient.getProjects()
    suspend fun getAgents(): List<Agent> = apiClient.getAgents()
    suspend fun getSessions(projectName: String? = null): List<Session> = apiClient.getSessions(projectName)
    suspend fun getSessionMessages(
        sessionId: String,
        provider: String,
        projectName: String?,
        projectPath: String?
    ): List<ChatMessage> = apiClient.getSessionMessages(sessionId, provider, projectName, projectPath)
    suspend fun sendMessageAndStream(
        session: Session,
        content: String,
        onEvent: (ChatStreamEvent) -> Unit
    ) = apiClient.sendMessageAndStream(session, content, onEvent)
    suspend fun search(query: String): List<SearchResult> = apiClient.search(query)
    suspend fun getSkills(): List<Skill> = apiClient.getSkills()
}
