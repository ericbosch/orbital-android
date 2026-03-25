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
import com.orbital.app.ui.components.Dot
import com.orbital.app.ui.theme.OrbitalTheme

private val GREEN  = Color(0xFF22C55E)
private val IDLE_BLUE = Color(0xFF0066FF)

@Composable
fun AgentsScreen(
    agents: List<Agent>,
    onAgent: (Agent) -> Unit
) {
    val th   = OrbitalTheme.colors
    val mono = OrbitalTheme.fonts.mono
    val ui   = OrbitalTheme.fonts.ui
    val typ  = OrbitalTheme.typography

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Agents",
                style = typ.headlineMedium.copy(
                    color = th.text, fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp, fontFamily = ui
                )
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(th.accentI, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", color = Color.White, fontSize = 18.sp)
            }
        }

        Spacer(Modifier.height(20.dp))

        agents.forEach { agent ->
            val dc = when (agent.status) {
                AgentStatus.ACTIVE  -> GREEN
                AgentStatus.IDLE    -> IDLE_BLUE
                AgentStatus.OFFLINE -> th.muted
            }
            AgentCard(agent = agent, dotCol = dc, onClick = { onAgent(agent) })
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun AgentCard(agent: Agent, dotCol: Color, onClick: () -> Unit) {
    val th   = OrbitalTheme.colors
    val mono = OrbitalTheme.fonts.mono
    val ui   = OrbitalTheme.fonts.ui
    val typ  = OrbitalTheme.typography
    val off  = agent.status == AgentStatus.OFFLINE

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = agent.icon, fontSize = 22.sp, color = dotCol)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = agent.name,
                        style = typ.bodySmall.copy(
                            color = th.text, fontWeight = FontWeight.Bold,
                            fontSize = 13.sp, fontFamily = ui
                        )
                    )
                    Text(
                        text = agent.model,
                        style = typ.labelSmall.copy(color = th.muted, fontSize = 8.5.sp, fontFamily = mono)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(col = dotCol)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = agent.status.name.lowercase(),
                        style = typ.labelSmall.copy(color = dotCol, fontSize = 8.sp, fontFamily = mono)
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${agent.sessions} sessions",
                    style = typ.labelSmall.copy(color = th.muted, fontSize = 8.sp, fontFamily = mono)
                )
            }
        }
        if (off) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(th.void)
                    .border(
                        width = 1.dp,
                        color = th.border,
                        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Not installed · tap for install guide",
                    style = typ.labelSmall.copy(color = th.muted, fontSize = 8.sp, fontFamily = mono)
                )
            }
        }
    }
}
