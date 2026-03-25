package com.orbital.app.domain

enum class AgentStatus { ACTIVE, IDLE, OFFLINE }

data class Agent(
    val id: String,
    val name: String,
    val model: String,
    val status: AgentStatus,
    val sessions: Int,
    val icon: String
)
