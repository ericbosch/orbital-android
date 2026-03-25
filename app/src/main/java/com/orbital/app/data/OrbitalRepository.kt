package com.orbital.app.data

import com.orbital.app.core.network.OrbitalApiClient
import com.orbital.app.domain.Agent
import com.orbital.app.domain.Session
import com.orbital.app.domain.Skill
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrbitalRepository @Inject constructor(private val apiClient: OrbitalApiClient) {
    suspend fun getAgents(): List<Agent>     = apiClient.getAgents()
    suspend fun getSessions(): List<Session> = apiClient.getSessions()
    suspend fun getSkills(): List<Skill>     = apiClient.getSkills()
    fun setServerUrl(url: String)            = apiClient.setBaseUrl(url)
}
