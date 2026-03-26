package com.orbital.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.orbital.app.core.network.OrbitalApiClient
import com.orbital.app.domain.Agent
import com.orbital.app.domain.BackendProfile
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

data class StoredServer(
    val url: String,
    val token: String,
    val name: String,
    val backendProfile: BackendProfile = BackendProfile.UNKNOWN
)

@Singleton
class OrbitalRepository @Inject constructor(
    private val apiClient: OrbitalApiClient,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val SERVER_URL  = stringPreferencesKey("server_url")
        private val AUTH_TOKEN  = stringPreferencesKey("auth_token")
        private val SERVER_NAME = stringPreferencesKey("server_name")
        private val BACKEND_PROFILE = stringPreferencesKey("backend_profile")
    }

    fun getStoredServer(): Flow<StoredServer?> = dataStore.data.map { prefs ->
        val url = prefs[SERVER_URL] ?: return@map null
        val token = prefs[AUTH_TOKEN] ?: return@map null
        val name = prefs[SERVER_NAME] ?: ""
        val profile = BackendProfile.fromStored(prefs[BACKEND_PROFILE])
        if (url.isNotBlank()) StoredServer(url, token, name, profile) else null
    }

    fun getStoredServerName(): Flow<String> = dataStore.data.map { prefs ->
        prefs[SERVER_NAME] ?: ""
    }

    suspend fun saveServer(
        url: String,
        token: String,
        name: String,
        backendProfile: BackendProfile = BackendProfile.UNKNOWN
    ) {
        dataStore.edit { prefs ->
            prefs[SERVER_URL] = url
            prefs[AUTH_TOKEN] = token
            prefs[SERVER_NAME] = name
            prefs[BACKEND_PROFILE] = backendProfile.name.lowercase()
        }
    }

    suspend fun clearServer() {
        dataStore.edit { prefs ->
            prefs.remove(SERVER_URL)
            prefs.remove(AUTH_TOKEN)
            prefs.remove(SERVER_NAME)
            prefs.remove(BACKEND_PROFILE)
        }
    }

    suspend fun ping(url: String, token: String): Boolean = apiClient.ping(url, token)
    suspend fun detectBackendProfile(url: String, token: String): BackendProfile =
        apiClient.detectBackendProfile(url, token)

    fun setServerUrl(url: String) = apiClient.setBaseUrl(url)
    fun setAuthToken(token: String) = apiClient.setAuthToken(token)
    fun setBackendProfile(profile: BackendProfile) = apiClient.setBackendProfile(profile)
    fun getBackendProfile(): BackendProfile = apiClient.getBackendProfile()

    suspend fun getProjects(): List<Project> = apiClient.getProjects()
    suspend fun getAgents(): List<Agent> = apiClient.getAgents()
    suspend fun getSessions(projectName: String? = null): List<Session> = apiClient.getSessions(projectName)
    suspend fun getCodexSessions(projectPath: String): List<Session> = apiClient.getCodexSessions(projectPath)
    suspend fun getCursorSessions(projectPath: String): List<Session> = apiClient.getCursorSessions(projectPath)
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
