package com.orbital.app.ui.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbital.app.ui.components.Mark
import com.orbital.app.ui.components.OrbitalButton
import com.orbital.app.ui.theme.OrbitalTheme
import com.orbital.app.ui.theme.StatusGreen
import kotlinx.coroutines.delay

@Composable
fun ScanScreen(onNext: () -> Unit) {
    val th = OrbitalTheme.colors
    var found by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1200)
        found = true
        delay(1000)
        done = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "step 1 / 3",
            style = OrbitalTheme.typography.labelSmall.copy(color = th.muted)
        )
        Text(
            text = "Discover servers",
            style = OrbitalTheme.typography.headlineMedium.copy(color = th.text, fontSize = 22.sp),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "Scanning LAN and Tailscale for Orbital servers…",
            style = OrbitalTheme.typography.labelSmall.copy(color = th.sub, fontSize = 10.sp, lineHeight = 16.sp),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Radar Animation
        Box(
            modifier = Modifier
                .size(130.dp)
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "radar")
            
            for (i in 1..3) {
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 2.5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200 + i * 300, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "scale"
                )
                val opacity by infiniteTransition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200 + i * 300, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "opacity"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .scale(scale)
                        .background(th.accentP.copy(alpha = opacity), CircleShape)
                        .border(1.dp, th.accentP.copy(alpha = opacity), CircleShape)
                )
            }
            
            if (done) {
                Mark(sz = 34.dp, col = th.accentP, glow = true)
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(2.dp, th.accentP, CircleShape)
                        .clip(CircleShape)
                )
            }
        }

        // Discovery Results
        if (found) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ServerItem(name = "elitebook", host = "192.168.1.130", via = "LAN")
                if (done) {
                    ServerItem(name = "elitebook", host = "100.75.22.51", via = "Tailscale")
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 20.dp)) {
            if (done) {
                OrbitalButton(text = "Connect to elitebook", onClick = onNext)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(th.raised)
                    .border(1.dp, th.border, RoundedCornerShape(12.dp))
                    .clickable { /* Manual IP */ }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Enter IP manually",
                    style = OrbitalTheme.typography.bodyLarge.copy(color = th.text, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun ServerItem(name: String, host: String, via: String) {
    val th = OrbitalTheme.colors
    val viaColor = if (via == "LAN") StatusGreen else Color(0xFF0066FF)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(th.raised, RoundedCornerShape(14.dp))
            .border(1.dp, th.border, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = name, style = OrbitalTheme.typography.headlineMedium.copy(fontSize = 12.sp, color = th.text))
                Box(modifier = Modifier
                    .background(viaColor.copy(alpha = 0.13f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(text = via, style = OrbitalTheme.typography.labelSmall.copy(color = viaColor, fontSize = 8.sp))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = host, style = OrbitalTheme.typography.labelSmall.copy(color = th.muted))
                Text(text = "4ms", style = OrbitalTheme.typography.labelSmall.copy(color = th.muted))
            }
        }
    }
}
