package com.orbital.app.ui.screens.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbital.app.domain.AppearanceSettings
import com.orbital.app.ui.components.Toggle
import com.orbital.app.ui.theme.OrbitalTheme

private val RED = Color(0xFFEF4444)

private val THEMES  = listOf("dark", "amoled", "nord", "solarized", "light")
private val ACCENTS = listOf(
    "indigo" to Color(0xFF7C3AED),
    "blue"   to Color(0xFF3B82F6),
    "teal"   to Color(0xFF14B8A6),
    "rose"   to Color(0xFFF43F5E),
    "amber"  to Color(0xFFF59E0B),
    "green"  to Color(0xFF22C55E),
)
private val FONTS = listOf("Syne / JetBrains", "DM Sans / Fira", "Space / IBM Plex", "Mono everywhere")

@Composable
fun SettingsScreen(
    serverHost: String,
    backendProfileLabel: String,
    authToken: String,
    connectionError: String?,
    appearance: AppearanceSettings,
    onAppearanceChange: (AppearanceSettings) -> Unit,
    onSaveToken: (String) -> Unit,
    onRefreshData: () -> Unit,
    onDisconnect: () -> Unit,
    onTroubleshoot: () -> Unit
) {
    val th   = OrbitalTheme.colors
    val mono = OrbitalTheme.fonts.mono
    val ui   = OrbitalTheme.fonts.ui
    val typ  = OrbitalTheme.typography

    var autoReconnect    by remember { mutableStateOf(true) }
    var tailscale        by remember { mutableStateOf(true) }
    var notifComplete    by remember { mutableStateOf(true) }
    var notifOffline     by remember { mutableStateOf(true) }
    var notifSkills      by remember { mutableStateOf(false) }
    var tokenInput       by remember(authToken) { mutableStateOf(authToken) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Settings",
            style = typ.headlineMedium.copy(
                color = th.text, fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp, fontFamily = ui
            ),
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // ─── Theme ─────────────────────────────────────────────
        SectionHeader("APPEARANCE — THEME")
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                THEMES.forEach { t ->
                    val active = appearance.themeName == t
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (active) th.accentI else th.void)
                            .border(1.dp, if (active) th.accentP else th.border, RoundedCornerShape(20.dp))
                            .clickable { onAppearanceChange(appearance.copy(themeName = t)) }
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = t.replaceFirstChar { it.uppercase() },
                            style = typ.labelSmall.copy(
                                color = if (active) th.text else th.muted,
                                fontSize = 8.sp, fontFamily = mono
                            )
                        )
                    }
                }
            }
        }

        // ─── Accent ────────────────────────────────────────────
        SectionHeader("APPEARANCE — ACCENT")
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ACCENTS.forEach { (name, col) ->
                    val active = appearance.accentName == name
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(col)
                            .border(
                                width = 2.dp,
                                color = if (active) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onAppearanceChange(appearance.copy(accentName = name)) }
                    )
                }
            }
        }

        // ─── Font ──────────────────────────────────────────────
        SectionHeader("APPEARANCE — FONT")
        SettingsCard {
            FONTS.forEachIndexed { i, f ->
                val active = appearance.fontName == f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAppearanceChange(appearance.copy(fontName = f)) }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = f,
                        style = typ.bodySmall.copy(
                            color = if (active) th.text else th.muted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp, fontFamily = ui
                        )
                    )
                    if (active) {
                        Text(text = "✓", color = th.accentP, fontSize = 14.sp)
                    }
                }
                if (i < FONTS.lastIndex) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(th.border))
                }
            }
        }

        // ─── Size & Style ───────────────────────────────────────
        SectionHeader("APPEARANCE — SIZE & STYLE")
        SettingsCard {
            // Font size
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Font size",
                    style = typ.labelSmall.copy(color = th.sub, fontSize = 9.5.sp, fontFamily = mono)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SizeButton("-") {
                        onAppearanceChange(appearance.copy(fontSize = (appearance.fontSize - 1).coerceAtLeast(10)))
                    }
                    Text(
                        text = "${appearance.fontSize}px",
                        style = typ.labelSmall.copy(color = th.text, fontSize = 9.sp, fontFamily = mono),
                        modifier = Modifier.widthIn(min = 24.dp)
                    )
                    SizeButton("+") {
                        onAppearanceChange(appearance.copy(fontSize = (appearance.fontSize + 1).coerceAtMost(16)))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(th.border))
            // Bubble style
            Column(modifier = Modifier.padding(vertical = 9.dp)) {
                Text(
                    text = "Bubble style",
                    style = typ.labelSmall.copy(color = th.sub, fontSize = 9.5.sp, fontFamily = mono),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SegmentedControl(
                    options = listOf("rounded", "sharp", "minimal"),
                    current = appearance.bubbleStyle,
                    onChange = { onAppearanceChange(appearance.copy(bubbleStyle = it)) }
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(th.border))
            // Density
            Column(modifier = Modifier.padding(vertical = 9.dp)) {
                Text(
                    text = "Density",
                    style = typ.labelSmall.copy(color = th.sub, fontSize = 9.5.sp, fontFamily = mono),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SegmentedControl(
                    options = listOf("compact", "normal", "spacious"),
                    current = appearance.density,
                    onChange = { onAppearanceChange(appearance.copy(density = it)) }
                )
            }
        }

        // ─── Connection ─────────────────────────────────────────
        SectionHeader("CONNECTION")
        SettingsCard {
            InfoRow("Host", serverHost.ifEmpty { "—" })
            Box(Modifier.fillMaxWidth().height(1.dp).background(th.border))
            InfoRow("Backend", backendProfileLabel)
            Box(Modifier.fillMaxWidth().height(1.dp).background(th.border))
            InfoRow("Via", "LAN (primary)")
            Box(Modifier.fillMaxWidth().height(1.dp).background(th.border))
            Column(modifier = Modifier.padding(vertical = 9.dp)) {
                Text(
                    text = "Auth token",
                    style = typ.labelSmall.copy(color = th.sub, fontSize = 9.5.sp, fontFamily = mono),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(th.void)
                        .border(1.dp, th.border, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    if (tokenInput.isBlank()) {
                        Text(
                            text = "Paste bearer token...",
                            style = typ.labelSmall.copy(color = th.muted, fontSize = 8.5.sp, fontFamily = mono)
                        )
                    }
                    BasicTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        textStyle = typ.labelSmall.copy(color = th.text, fontSize = 8.5.sp, fontFamily = mono),
                        cursorBrush = SolidColor(th.accentP),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(th.border))
            Toggle(label = "Auto-reconnect", value = autoReconnect, onChange = { autoReconnect = it })
            Box(Modifier.fillMaxWidth().height(1.dp).background(th.border))
            Toggle(label = "Tailscale", value = tailscale, onChange = { tailscale = it })
            Box(Modifier.fillMaxWidth().height(1.dp).background(th.border))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(th.accentI)
                        .border(1.dp, th.accentP, RoundedCornerShape(10.dp))
                        .clickable { onSaveToken(tokenInput) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Save token",
                        style = typ.labelSmall.copy(color = Color.White, fontSize = 8.5.sp, fontFamily = mono)
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(th.raised)
                        .border(1.dp, th.border, RoundedCornerShape(10.dp))
                        .clickable { onRefreshData() }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Refresh data",
                        style = typ.labelSmall.copy(color = th.text, fontSize = 8.5.sp, fontFamily = mono)
                    )
                }
            }
            if (!connectionError.isNullOrBlank()) {
                Text(
                    text = connectionError,
                    style = typ.labelSmall.copy(color = RED, fontSize = 8.5.sp, fontFamily = mono),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        // ─── Notifications ──────────────────────────────────────
        SectionHeader("NOTIFICATIONS")
        SettingsCard {
            Toggle(label = "Session complete", value = notifComplete, onChange = { notifComplete = it })
            Box(Modifier.fillMaxWidth().height(1.dp).background(th.border))
            Toggle(label = "Agent offline",    value = notifOffline,  onChange = { notifOffline  = it })
            Box(Modifier.fillMaxWidth().height(1.dp).background(th.border))
            Toggle(label = "New skills",       value = notifSkills,   onChange = { notifSkills   = it })
        }

        // ─── About ──────────────────────────────────────────────
        SectionHeader("ABOUT")
        SettingsCard {
            InfoRow("Version", "0.1.0-α")
            Box(Modifier.fillMaxWidth().height(1.dp).background(th.border))
            InfoRow("Plan", "Local")
            Box(Modifier.fillMaxWidth().height(1.dp).background(th.border))
            InfoRow("Build", "2026-03-24")
        }

        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(th.raised)
                .border(1.dp, th.border, RoundedCornerShape(12.dp))
                .clickable { onTroubleshoot() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Troubleshoot connection",
                style = typ.bodySmall.copy(
                    color = th.text, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = ui
                )
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RED)
                .border(1.dp, RED, RoundedCornerShape(12.dp))
                .clickable { onDisconnect() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Disconnect from server",
                style = typ.bodySmall.copy(
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = ui
                )
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    val th   = OrbitalTheme.colors
    val mono = OrbitalTheme.fonts.mono
    val typ  = OrbitalTheme.typography
    Text(
        text = text,
        style = typ.labelSmall.copy(
            color = th.muted, fontSize = 8.5.sp, fontFamily = mono, letterSpacing = 0.06.sp
        ),
        modifier = Modifier.padding(bottom = 10.dp, top = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    val th = OrbitalTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(th.raised)
            .border(1.dp, th.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp)
            .padding(bottom = 2.dp),
        content = content
    )
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun InfoRow(label: String, value: String) {
    val th   = OrbitalTheme.colors
    val mono = OrbitalTheme.fonts.mono
    val typ  = OrbitalTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = typ.labelSmall.copy(color = th.sub, fontSize = 9.5.sp, fontFamily = mono)
        )
        Text(
            text = value,
            style = typ.labelSmall.copy(color = th.text, fontSize = 9.5.sp, fontFamily = mono)
        )
    }
}

@Composable
private fun SegmentedControl(options: List<String>, current: String, onChange: (String) -> Unit) {
    val th   = OrbitalTheme.colors
    val mono = OrbitalTheme.fonts.mono
    val typ  = OrbitalTheme.typography
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        options.forEach { opt ->
            val active = current == opt
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (active) th.accentI else th.void)
                    .border(1.dp, if (active) th.accentP else th.border, RoundedCornerShape(16.dp))
                    .clickable { onChange(opt) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = opt.replaceFirstChar { it.uppercase() },
                    style = typ.labelSmall.copy(
                        color = if (active) th.text else th.muted,
                        fontSize = 8.sp, fontFamily = mono
                    )
                )
            }
        }
    }
}

@Composable
private fun SizeButton(text: String, onClick: () -> Unit) {
    val th = OrbitalTheme.colors
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(th.void)
            .border(1.dp, th.border, RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = th.text, fontSize = 14.sp)
    }
}
