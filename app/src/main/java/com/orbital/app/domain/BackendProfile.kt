package com.orbital.app.domain

enum class BackendProfile {
    ORBITAL,
    ORBITDOCK,
    UNKNOWN;

    fun label(): String = when (this) {
        ORBITAL -> "Orbital Server"
        ORBITDOCK -> "OrbitDock Server"
        UNKNOWN -> "Unknown"
    }

    companion object {
        fun fromStored(value: String?): BackendProfile = when (value?.trim()?.lowercase()) {
            "orbital" -> ORBITAL
            "orbitdock" -> ORBITDOCK
            // OrbitDock is the primary backend contract for Orbital.
            else -> ORBITDOCK
        }
    }
}
