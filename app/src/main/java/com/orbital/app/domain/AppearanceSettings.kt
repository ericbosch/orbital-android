package com.orbital.app.domain

data class AppearanceSettings(
    val themeName: String = "dark",
    val accentName: String = "indigo",
    val fontName: String = "Syne / JetBrains",
    val fontSize: Int = 11,
    val bubbleStyle: String = "rounded",
    val density: String = "normal"
)
