package com.orbital.app.ui.screens.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbital.app.domain.Agent
import com.orbital.app.domain.AgentStatus
import com.orbital.app.domain.Session
import com.orbital.app.ui.components.Dot
import com.orbital.app.ui.components.Mark
import com.orbital.app.ui.theme.OrbitalTheme

private val GREEN  = Color(0xFF22C55E)
private val IDLE_BLUE = Color(0xFF0066FF)

@Composable
fun HomeScreen(
    serverName: String,
    serverLatency: Int,
    agents: List<Agent>,
    sessions: List<Session>,
    onNavAgents: () -> Unit,
    onNavSessions: () -> Unit,
    onSession: (Session) -> Unit
) {
    val th = OrbitalTheme.colors
    val mono = OrbitalTheme.fonts.mono
    val ui   = OrbitalTheme.fonts.ui
    val typ  = OrbitalTheme.typography

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "orbital",
                    style = typ.headlineMedium.copy(
                        color = th.text, fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp, fontFamily = ui
                    )
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(col = GREEN, ping = true)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "$serverName · LAN · ${serverLatency}ms",
                        style = typ.labelSmall.copy(
                            color = th.sub, fontSize = 8.5.sp, fontFamily = mono
                        )
                    )
                }
            }
            Mark(sz = 30.dp, col = th.accentP, glow = true)
        }

        Spacer(Modifier.height(22.dp))

        // Stats grid
        val activeCount  = agents.count { it.status == AgentStatus.ACTIVE }
        val agentCount   = agents.size
        val sessionCount = sessions.size
        val msgCount     = sessions.sumOf { it.msgs }
        val stats = listOf(
            Triple("$activeCount",  GREEN,   "Active"),
            Triple("$agentCount",   th.accentP, "Agents"),
            Triple("$sessionCount", th.sub,  "Sessions"),
            Triple("$msgCount",     th.sub,  "Messages"),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            stats.chunked(2).forEach { pair ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { (value, col, label) ->
                        StatCard(value = value, col = col, label = label)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Agents section
        Text(
            text = "AGENTS",
            style = typ.labelSmall.copy(
                color = th.muted, fontSize = 8.5.sp, fontFamily = mono, letterSpacing = 0.06.sp
            ),
            modifier = Modifier.padding(bottom = 10.dp)
        )
        agents.filter { it.status != AgentStatus.OFFLINE }.forEach { agent ->
            val col = if (agent.status == AgentStatus.ACTIVE) GREEN else IDLE_BLUE
            AgentRow(agent = agent, col = col, onClick = onNavAgents)
            Spacer(Modifier.height(7.dp))
        }

        Spacer(Modifier.height(13.dp))

        // Recent sessions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT",
                style = typ.labelSmall.copy(
                    color = th.muted, fontSize = 8.5.sp, fontFamily = mono, letterSpacing = 0.06.sp
                )
            )
            Text(
                text = "see all →",
                style = typ.labelSmall.copy(
                    color = th.accentP, fontSize = 8.sp, fontFamily = mono
                ),
                modifier = Modifier.clickable { onNavSessions() }
            )
        }
        Spacer(Modifier.height(10.dp))
        sessions.take(3).forEach { s ->
            SessionCard(session = s, onClick = { onSession(s) })
            Spacer(Modifier.height(7.dp))
        }
    }
}

@Composable
private fun StatCard(value: String, col: Color, label: String) {
    val th   = OrbitalTheme.colors
    val mono = OrbitalTheme.fonts.mono
    val typ  = OrbitalTheme.typography
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(th.raised)
            .border(1.dp, th.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column {
            Text(
                text = value,
                style = typ.headlineMedium.copy(
                    color = col, fontWeight = FontWeight.Bold,
                    fontSize = 22.sp, fontFamily = mono
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = typ.labelSmall.copy(color = th.muted, fontSize = 8.sp, fontFamily = mono)
            )
        }
    }
}

@Composable
private fun AgentRow(agent: Agent, col: Color, onClick: () -> Unit) {
    val th   = OrbitalTheme.colors
    val mono = OrbitalTheme.fonts.mono
    val ui   = OrbitalTheme.fonts.ui
    val typ  = OrbitalTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(th.raised)
            .border(1.dp, th.border, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(col = col, ping = agent.status == AgentStatus.ACTIVE)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = agent.name,
                    style = typ.bodySmall.copy(
                        color = th.text, fontWeight = FontWeight.Bold,
                        fontSize = 11.sp, fontFamily = ui
                    )
                )
                Text(
                    text = agent.model,
                    style = typ.labelSmall.copy(color = th.muted, fontSize = 8.sp, fontFamily = mono)
                )
            }
        }
        Box(
            modifier = Modifier
                .background(col.copy(alpha = 0.13f), RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = agent.status.name.lowercase(),
                style = typ.labelSmall.copy(color = col, fontSize = 7.5.sp, fontFamily = mono)
            )
        }
    }
}

@Composable
fun SessionCard(session: Session, onClick: () -> Unit) {
    val th   = OrbitalTheme.colors
    val mono = OrbitalTheme.fonts.mono
    val typ  = OrbitalTheme.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(th.raised)
            .border(1.dp, th.border, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = session.agent,
                style = typ.labelSmall.copy(
                    color = th.accentP, fontSize = 8.5.sp, fontFamily = mono
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (session.status == "active") {
                    Dot(col = GREEN, ping = true)
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    text = "${session.ago} ago",
                    style = typ.labelSmall.copy(color = th.muted, fontSize = 8.sp, fontFamily = mono)
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = session.name,
            style = typ.bodySmall.copy(
                color = th.text, fontSize = 10.sp, fontFamily = mono
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${session.msgs} msgs",
            style = typ.labelSmall.copy(color = th.muted, fontSize = 8.sp, fontFamily = mono)
        )
    }
}
