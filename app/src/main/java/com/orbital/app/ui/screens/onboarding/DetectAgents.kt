package com.orbital.app.ui.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbital.app.ui.components.OrbitalButton
import com.orbital.app.ui.theme.OrbitalTheme
import com.orbital.app.ui.theme.StatusAmber
import com.orbital.app.ui.theme.StatusGreen
import kotlinx.coroutines.delay

data class AgentDetection(val id: String, val name: String, val model: String, val icon: String, val res: String)

val AGENTS = listOf(
    AgentDetection("claude", "Claude Code", "claude-opus-4-5", "◎", "installed"),
    AgentDetection("codex", "Codex CLI", "codex-mini-latest", "◈", "installed"),
    AgentDetection("gemini", "Gemini CLI", "gemini-2.5-pro", "◇", "missing"),
    AgentDetection("aider", "Aider", "gpt-4o", "◉", "missing")
)

@Composable
fun DetectAgents(onNext: () -> Unit) {
    val th = OrbitalTheme.colors
    var detectedStates by remember { mutableStateOf(mapOf<String, String>()) }

    LaunchedEffect(Unit) {
        AGENTS.forEachIndexed { index, agent ->
            delay(500 + index * 600L)
            detectedStates = detectedStates + (agent.id to agent.res)
        }
    }

    val allDone = AGENTS.all { detectedStates.containsKey(it.id) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(text = "step 2 / 3", style = OrbitalTheme.typography.labelSmall.copy(color = th.muted))
        Text(
            text = "Detecting agents",
            style = OrbitalTheme.typography.headlineMedium.copy(color = th.text, fontSize = 22.sp),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "Checking elitebook for installed AI coding agents…",
            style = OrbitalTheme.typography.labelSmall.copy(color = th.sub, fontSize = 9.5.sp, lineHeight = 16.sp),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AGENTS.forEach { agent ->
                val status = detectedStates[agent.id]
                val dotCol = when (status) {
                    "installed" -> StatusGreen
                    "missing" -> StatusAmber
                    else -> th.muted
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(th.raised, RoundedCornerShape(14.dp))
                        .border(1.dp, th.border, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(text = agent.icon, style = OrbitalTheme.typography.headlineMedium.copy(fontSize = 20.sp, color = dotCol))
                                Column {
                                    Text(text = agent.name, style = OrbitalTheme.typography.headlineMedium.copy(fontSize = 12.sp, color = th.text))
                                    Text(text = agent.model, style = OrbitalTheme.typography.labelSmall.copy(color = th.muted, fontSize = 8.sp))
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (status == null) {
                                    LoadingSpinner(th.muted)
                                } else {
                                    Box(modifier = Modifier.size(6.dp).background(dotCol, CircleShape))
                                }
                                Text(text = status ?: "…", style = OrbitalTheme.typography.labelSmall.copy(color = dotCol, fontSize = 8.sp))
                            }
                        }
                        
                        if (allDone && status == "missing") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(th.border))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "Install on elitebook:", style = OrbitalTheme.typography.labelSmall.copy(color = th.muted, fontSize = 8.sp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(modifier = Modifier.fillMaxWidth().background(th.void, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 7.dp)) {
                                Text(
                                    text = if (agent.id == "gemini") "npm i -g @google/gemini-cli" else "npm i -g @aider-ai/aider",
                                    style = OrbitalTheme.typography.labelSmall.copy(color = th.text, fontSize = 8.5.sp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (allDone) {
            Column(modifier = Modifier.padding(top = 20.dp)) {
                OrbitalButton(text = "Continue with 2 agents", onClick = onNext)
            }
        }
    }
}

@Composable
fun LoadingSpinner(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(800, easing = LinearEasing)),
        label = "angle"
    )
    Box(
        modifier = Modifier
            .size(13.dp)
            .border(1.5.dp, color.copy(alpha = 0.3f), CircleShape)
            .border(1.5.dp, color, CircleShape) // TODO: Specific arc for spinner
    )
}
