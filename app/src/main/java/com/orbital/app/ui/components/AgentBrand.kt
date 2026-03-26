package com.orbital.app.ui.components

import com.orbital.app.R

fun agentLogoRes(agentId: String): Int? = when (agentId.lowercase()) {
    "claude" -> R.drawable.ic_agent_claude
    "codex" -> R.drawable.ic_agent_codex
    "gemini" -> R.drawable.ic_agent_gemini
    "aider" -> R.drawable.ic_agent_aider
    "cursor" -> R.drawable.ic_agent_cursor
    else -> null
}
