package com.orbital.app.ui.screens.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbital.app.ui.theme.OrbitalTheme
import kotlinx.coroutines.delay

private val GREEN = Color(0xFF22C55E)

private val CHECKS = listOf(
    "Pinging host",
    "Checking port 8420",
    "Auth handshake",
    "Agent API",
    "Session store"
)

@Composable
fun TroubleshootScreen(serverName: String, onBack: () -> Unit) {
    val th   = OrbitalTheme.colors
    val mono = OrbitalTheme.fonts.mono
    val ui   = OrbitalTheme.fonts.ui
    val typ  = OrbitalTheme.typography

    var step by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        for (i in 1..CHECKS.size) {
            delay(500)
            step = i
        }
    }

    val done = step >= CHECKS.size

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = th.border, shape = RoundedCornerShape(0.dp))
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
            Text(
                text = "Troubleshoot",
                style = typ.bodySmall.copy(
                    color = th.text, fontWeight = FontWeight.Bold,
                    fontSize = 14.sp, fontFamily = ui
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Running diagnostics on $serverName…",
                style = typ.labelSmall.copy(
                    color = th.muted, fontSize = 9.sp, fontFamily = mono
                ),
                modifier = Modifier.padding(bottom = 18.dp)
            )

            CHECKS.forEachIndexed { i, label ->
                val isActive  = i < step
                val isRunning = i == step && !done

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isActive) th.raised else th.surface)
                        .border(
                            1.dp,
                            if (isActive) th.border else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.alpha(if (i > step) 0.3f else 1f)
                    ) {
                        when {
                            isRunning -> SpinnerIcon(color = th.accentP)
                            isActive  -> Text(text = "✓", color = GREEN, fontSize = 13.sp)
                            else      -> Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .border(1.5.dp, th.muted, CircleShape)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = typ.bodySmall.copy(
                                color = if (isActive) th.text else th.muted,
                                fontSize = 9.5.sp, fontFamily = mono
                            )
                        )
                    }
                    if (isActive) {
                        Text(
                            text = "OK",
                            style = typ.labelSmall.copy(color = GREEN, fontSize = 8.5.sp, fontFamily = mono)
                        )
                    }
                }
            }

            if (done) {
                Spacer(Modifier.height(24.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GREEN.copy(alpha = 0.067f))
                        .border(1.dp, GREEN.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "✓", fontSize = 24.sp, color = GREEN)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "All systems nominal",
                        style = typ.bodySmall.copy(
                            color = GREEN, fontWeight = FontWeight.Bold,
                            fontSize = 13.sp, fontFamily = ui
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Connection to $serverName is healthy",
                        style = typ.labelSmall.copy(
                            color = th.sub, fontSize = 9.sp, fontFamily = mono
                        )
                    )
                }
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(th.raised)
                        .border(1.dp, th.border, RoundedCornerShape(12.dp))
                        .clickable { onBack() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Back to Settings",
                        style = typ.bodySmall.copy(
                            color = th.text, fontWeight = FontWeight.Bold,
                            fontSize = 12.sp, fontFamily = ui
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SpinnerIcon(color: Color) {
    val transition = rememberInfiniteTransition(label = "spin")
    val rotation by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )
    Box(
        modifier = Modifier
            .size(13.dp)
            .rotate(rotation)
            .border(1.5.dp, color, CircleShape)
            .clip(CircleShape)
    ) {
        Box(
            modifier = Modifier
                .size(13.dp)
                .clip(CircleShape)
                .background(
                    Color.Transparent
                )
        )
    }
}

