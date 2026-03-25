package com.orbital.app.ui.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbital.app.ui.components.Mark
import com.orbital.app.ui.components.OrbitalButton
import com.orbital.app.ui.theme.OrbitalTheme
import kotlinx.coroutines.delay

@Composable
fun Splash(onNext: () -> Unit) {
    val th = OrbitalTheme.colors
    var show by remember { mutableStateOf(false) }
    
    val opacity by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = tween(600), label = "opacity"
    )
    
    val translateY by animateDpAsState(
        targetValue = if (show) 0.dp else 16.dp,
        animationSpec = tween(600), label = "translate"
    )

    LaunchedEffect(Unit) {
        delay(300)
        show = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    0.0f to th.accentI.copy(alpha = 0.13f),
                    0.7f to Color.Transparent,
                    center = androidx.compose.ui.geometry.Offset(0.5f, 0.55f)
                )
            )
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.graphicsLayer(alpha = opacity, translationY = translateY.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.padding(bottom = 20.dp)) {
                Mark(sz = 72.dp, col = th.accentP, glow = true)
            }
            Text(
                text = "orbital",
                style = OrbitalTheme.typography.displayLarge.copy(color = th.text),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "local AI agent dispatcher",
                style = OrbitalTheme.typography.labelSmall.copy(color = th.muted),
                modifier = Modifier.padding(bottom = 44.dp)
            )
            OrbitalButton(
                text = "Get started",
                onClick = onNext
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row {
                Text(
                    text = "Already have a server? ",
                    style = OrbitalTheme.typography.labelSmall.copy(color = th.muted, fontSize = 8.sp)
                )
                Text(
                    text = "Connect manually →",
                    style = OrbitalTheme.typography.labelSmall.copy(color = th.accentP, fontSize = 8.sp),
                    modifier = Modifier.clickable { onNext() }
                )
            }
        }
    }
}
