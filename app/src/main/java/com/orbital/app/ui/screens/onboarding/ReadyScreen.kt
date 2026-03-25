package com.orbital.app.ui.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbital.app.ui.components.OrbitalButton
import com.orbital.app.ui.theme.OrbitalTheme
import com.orbital.app.ui.theme.StatusGreen

@Composable
fun ReadyScreen(onDone: () -> Unit) {
    val th = OrbitalTheme.colors
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    0.0f to th.accentI.copy(alpha = 0.1f),
                    0.65f to Color.Transparent,
                    center = androidx.compose.ui.geometry.Offset(0.5f, 0.4f)
                )
            )
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(StatusGreen.copy(alpha = 0.13f), CircleShape)
                    .border(1.5.dp, StatusGreen, CircleShape)
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✓", color = StatusGreen, fontSize = 26.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "You're all set",
                style = OrbitalTheme.typography.displayLarge.copy(color = th.text, fontSize = 24.sp),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Connected to elitebook via LAN",
                style = OrbitalTheme.typography.labelSmall.copy(color = th.sub, fontSize = 9.5.sp),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = "2 agents ready · sessions synced",
                style = OrbitalTheme.typography.labelSmall.copy(color = th.sub, fontSize = 9.5.sp),
                modifier = Modifier.padding(bottom = 32.dp)
            )
            OrbitalButton(
                text = "Open Orbital",
                onClick = onDone
            )
        }
    }
}
