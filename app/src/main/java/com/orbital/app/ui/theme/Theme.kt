package com.orbital.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

// ─── Orbital Custom Color System ─────────────────────────────
data class OrbitalColors(
    val void: Color,
    val surface: Color,
    val raised: Color,
    val border: Color,
    val text: Color,
    val sub: Color,
    val muted: Color,
    val accentI: Color,
    val accentP: Color,
    val isDark: Boolean
)

data class OrbitalFonts(
    val ui: FontFamily,
    val mono: FontFamily
)

val LocalOrbitalColors = staticCompositionLocalOf {
    OrbitalColors(
        void = OrbitalThemeColors.DarkVoid,
        surface = OrbitalThemeColors.DarkSurface,
        raised = OrbitalThemeColors.DarkRaised,
        border = OrbitalThemeColors.DarkBorder,
        text = OrbitalThemeColors.DarkText,
        sub = OrbitalThemeColors.DarkSub,
        muted = OrbitalThemeColors.DarkMuted,
        accentI = OrbitalAccents.IndigoI,
        accentP = OrbitalAccents.IndigoP,
        isDark = true
    )
}

val LocalOrbitalFonts = staticCompositionLocalOf {
    OrbitalFonts(ui = Syne, mono = JetBrainsMono)
}

object OrbitalTheme {
    val colors: OrbitalColors
        @Composable @ReadOnlyComposable
        get() = LocalOrbitalColors.current

    val typography: Typography
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.typography

    val fonts: OrbitalFonts
        @Composable @ReadOnlyComposable
        get() = LocalOrbitalFonts.current
}

@Composable
fun OrbitalTheme(
    themeName: String = "dark",
    accentName: String = "indigo",
    fontName: String = "Syne / JetBrains",
    content: @Composable () -> Unit
) {
    val th = when (themeName) {
        "amoled"    -> OrbitalThemeColors.run { listOf(AmoledVoid, AmoledSurface, AmoledRaised, AmoledBorder, AmoledText, AmoledSub, AmoledMuted, true) }
        "nord"      -> OrbitalThemeColors.run { listOf(NordVoid, NordSurface, NordRaised, NordBorder, NordText, NordSub, NordMuted, true) }
        "solarized" -> OrbitalThemeColors.run { listOf(SolarizedVoid, SolarizedSurface, SolarizedRaised, SolarizedBorder, SolarizedText, SolarizedSub, SolarizedMuted, true) }
        "light"     -> OrbitalThemeColors.run { listOf(LightVoid, LightSurface, LightRaised, LightBorder, LightText, LightSub, LightMuted, false) }
        else        -> OrbitalThemeColors.run { listOf(DarkVoid, DarkSurface, DarkRaised, DarkBorder, DarkText, DarkSub, DarkMuted, true) }
    }

    val ac = when (accentName) {
        "blue"  -> listOf(OrbitalAccents.BlueI, OrbitalAccents.BlueP)
        "teal"  -> listOf(OrbitalAccents.TealI, OrbitalAccents.TealP)
        "rose"  -> listOf(OrbitalAccents.RoseI, OrbitalAccents.RoseP)
        "amber" -> listOf(OrbitalAccents.AmberI, OrbitalAccents.AmberP)
        "green" -> listOf(OrbitalAccents.GreenI, OrbitalAccents.GreenP)
        else    -> listOf(OrbitalAccents.IndigoI, OrbitalAccents.IndigoP)
    }

    val orbitalFonts = when (fontName) {
        "DM Sans / Fira"   -> OrbitalFonts(ui = DMSans,       mono = JetBrainsMono)
        "Space / IBM Plex" -> OrbitalFonts(ui = SpaceGrotesk, mono = JetBrainsMono)
        "Mono everywhere"  -> OrbitalFonts(ui = JetBrainsMono, mono = JetBrainsMono)
        else               -> OrbitalFonts(ui = Syne,         mono = JetBrainsMono)
    }

    val orbitalColors = OrbitalColors(
        void = th[0] as Color,
        surface = th[1] as Color,
        raised = th[2] as Color,
        border = th[3] as Color,
        text = th[4] as Color,
        sub = th[5] as Color,
        muted = th[6] as Color,
        accentI = ac[0],
        accentP = ac[1],
        isDark = th[7] as Boolean
    )

    val materialColorScheme = if (orbitalColors.isDark) {
        darkColorScheme(
            primary = orbitalColors.accentP,
            background = orbitalColors.void,
            surface = orbitalColors.surface,
            onBackground = orbitalColors.text,
            onSurface = orbitalColors.text
        )
    } else {
        lightColorScheme(
            primary = orbitalColors.accentP,
            background = orbitalColors.void,
            surface = orbitalColors.surface,
            onBackground = orbitalColors.text,
            onSurface = orbitalColors.text
        )
    }

    CompositionLocalProvider(
        LocalOrbitalColors provides orbitalColors,
        LocalOrbitalFonts provides orbitalFonts,
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = Typography,
            content = content
        )
    }
}
