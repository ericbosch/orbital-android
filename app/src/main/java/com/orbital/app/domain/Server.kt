package com.orbital.app.domain

data class Server(
    val name: String,
    val host: String,
    val via: String,       // "LAN" | "Tailscale"
    val latencyMs: Int
)
