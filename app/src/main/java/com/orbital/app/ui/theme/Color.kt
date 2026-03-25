package com.orbital.app.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Design Tokens (Ported from src/constants.js) ────────────

// THEME PALETTES
object OrbitalThemeColors {
    // Dark Theme
    val DarkVoid    = Color(0xFF07070F)
    val DarkSurface = Color(0xFF0D0D1C)
    val DarkRaised  = Color(0xFF12122A)
    val DarkBorder  = Color(0xFF1C1C38)
    val DarkText    = Color(0xFFEEEEFF)
    val DarkSub     = Color(0xFF9090B8)
    val DarkMuted   = Color(0xFF3A3A60)

    // Amoled Theme
    val AmoledVoid    = Color(0xFF000000)
    val AmoledSurface = Color(0xFF080808)
    val AmoledRaised  = Color(0xFF101010)
    val AmoledBorder  = Color(0xFF1A1A1A)
    val AmoledText    = Color(0xFFFFFFFF)
    val AmoledSub     = Color(0xFF888888)
    val AmoledMuted   = Color(0xFF333333)

    // Nord Theme
    val NordVoid    = Color(0xFF1A1E2E)
    val NordSurface = Color(0xFF242938)
    val NordRaised  = Color(0xFF2E3347)
    val NordBorder  = Color(0xFF3B4261)
    val NordText    = Color(0xFFCAD3F5)
    val NordSub     = Color(0xFFA5ADCB)
    val NordMuted   = Color(0xFF494D64)

    // Solarized Theme
    val SolarizedVoid    = Color(0xFF001E26)
    val SolarizedSurface = Color(0xFF002B36)
    val SolarizedRaised  = Color(0xFF073642)
    val SolarizedBorder  = Color(0xFF0D4A59)
    val SolarizedText    = Color(0xFF839496)
    val SolarizedSub     = Color(0xFF657B83)
    val SolarizedMuted   = Color(0xFF304A52)

    // Light Theme
    val LightVoid    = Color(0xFFF0F0F8)
    val LightSurface = Color(0xFFFFFFFF)
    val LightRaised  = Color(0xFFF8F8FF)
    val LightBorder  = Color(0xFFDDDDF0)
    val LightText    = Color(0xFF1A1A2E)
    val LightSub     = Color(0xFF555577)
    val LightMuted   = Color(0xFF9999BB)
}

// ACCENT COLORS
object OrbitalAccents {
    val IndigoI = Color(0xFF5B21B6)
    val IndigoP = Color(0xFF7C3AED)
    
    val BlueI   = Color(0xFF1D4ED8)
    val BlueP   = Color(0xFF3B82F6)
    
    val TealI   = Color(0xFF0F766E)
    val TealP   = Color(0xFF14B8A6)
    
    val RoseI   = Color(0xFFBE123C)
    val RoseP   = Color(0xFFF43F5E)
    
    val AmberI  = Color(0xFFB45309)
    val AmberP  = Color(0xFFF59E0B)
    
    val GreenI  = Color(0xFF15803D)
    val GreenP  = Color(0xFF22C55E)
}

// STATIC COLORS
val StatusGreen = Color(0xFF22C55E)
val StatusAmber = Color(0xFFF59E0B)
val StatusRed   = Color(0xFFEF4444)
