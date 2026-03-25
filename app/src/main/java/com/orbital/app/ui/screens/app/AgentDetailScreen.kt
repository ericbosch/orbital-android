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
import com.orbital.app.ui.theme.OrbitalTheme

private val GREEN  = Color(0xFF22C55E)
private val AMBER  = Color(0xFFF59E0B)
private val RED    = Color(0xFFEF4444)
private val IDLE_BLUE = Color(0xFF0066FF)

@Composable
fun AgentDetailScreen(agent: Agent, onBack: () -> Unit) {
    val th   = OrbitalTheme.colors
    val mono = OrbitalTheme.fonts.mono
    val ui   = OrbitalTheme.fonts.ui
    val typ  = OrbitalTheme.typography
    val off  = agent.status == AgentStatus.OFFLINE
    val dc   = when (agent.status) {
        AgentStatus.ACTIVE  -> GREEN
        AgentStatus.IDLE    -> IDLE_BLUE
        AgentStatus.OFFLINE -> th.muted
    }
    val statusColor = if (off) AMBER else GREEN

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = th.border,
                    shape = RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                color = th.accentP, fontSize = 18.sp,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(8.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(text = agent.icon, fontSize = 20.sp, color = dc)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = agent.name,
                    style = typ.bodySmall.copy(
                        color = th.text, fontWeight = FontWeight.Bold,
                        fontSize = 14.sp, fontFamily = ui
                    )
                )
                Text(
                    text = agent.model,
                    style = typ.labelSmall.copy(color = th.muted, fontSize = 8.5.sp, fontFamily = mono)
                )
            }
            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.13f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = agent.status.name.lowercase(),
                    style = typ.labelSmall.copy(
                        color = statusColor, fontSize = 7.5.sp, fontFamily = mono
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            // Not installed banner
            if (off) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AMBER.copy(alpha = 0.067f))
                        .border(1.dp, AMBER.copy(alpha = 0.267f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column {
                        Text(
                            text = "⚠ Not installed",
                            style = typ.bodySmall.copy(
                                color = AMBER, fontWeight = FontWeight.Bold,
                                fontSize = 11.sp, fontFamily = ui
                            )
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Run on ${agent.id} via SSH:",
                            style = typ.labelSmall.copy(
                                color = th.sub, fontSize = 8.5.sp, fontFamily = mono
                            )
                        )
                        Spacer(Modifier.height(6.dp))
                        val installCmd = when (agent.id) {
                            "gemini" -> "npm i -g @google/gemini-cli"
                            else     -> "npm i -g @aider-ai/aider"
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(th.void)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = installCmd,
                                style = typ.labelSmall.copy(
                                    color = th.text, fontSize = 8.5.sp, fontFamily = mono
                                )
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(th.accentI)
                                .border(1.dp, th.accentP, RoundedCornerShape(10.dp))
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Open SSH session to install",
                                style = typ.bodySmall.copy(
                                    color = Color.White, fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp, fontFamily = ui
                                )
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // Configuration section
            Text(
                text = "CONFIGURATION",
                style = typ.labelSmall.copy(
                    color = th.muted, fontSize = 8.5.sp, fontFamily = mono, letterSpacing = 0.06.sp
                ),
                modifier = Modifier.padding(bottom = 10.dp)
            )
            val configRows = listOf(
                "Model"       to agent.model,
                "Max tokens"  to "8192",
                "Working dir" to "~/projects",
                "Auto-approve" to "OFF",
                "Log level"   to "info"
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(th.raised)
                    .border(1.dp, th.border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp)
            ) {
                configRows.forEachIndexed { i, (key, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = key,
                            style = typ.labelSmall.copy(color = th.sub, fontSize = 9.sp, fontFamily = mono)
                        )
                        Text(
                            text = value,
                            style = typ.labelSmall.copy(color = th.text, fontSize = 9.sp, fontFamily = mono)
                        )
                    }
                    if (i < configRows.lastIndex) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(th.border))
                    }
                }
            }

            if (!off) {
                Spacer(Modifier.height(20.dp))
                listOf("Edit configuration" to false, "View logs" to false, "Stop agent" to true)
                    .forEach { (label, danger) ->
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (danger) RED else th.raised)
                                .border(
                                    1.dp,
                                    if (danger) RED else th.border,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = typ.bodySmall.copy(
                                    color = if (danger) Color.White else th.text,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp, fontFamily = ui
                                )
                            )
                        }
                    }
            }
        }
    }
}
