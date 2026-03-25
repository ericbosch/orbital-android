package com.orbital.app.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbital.app.ui.theme.OrbitalTheme

// ─── Mark ─────────────────────────────────────────────────────
// Replicates the portfolio SVG (shared.jsx): orbital ring + planet + satellites
@Composable
fun Mark(sz: Dp, col: Color, glow: Boolean = false) {
    Canvas(modifier = Modifier.size(sz)) {
        val w = size.width
        val s = w / 64f          // scale from 64×64 source viewBox
        val cx = w / 2f
        val cy = w / 2f

        // Orbital ring (rotated -18°), matching: ellipse rx=21 ry=9.5 rotate(-18)
        rotate(degrees = -18f, pivot = Offset(cx, cy)) {
            val rx = 21f * s
            val ry = 9.5f * s
            if (glow) {
                drawOval(
                    color = col.copy(alpha = 0.18f),
                    topLeft = Offset(cx - rx - 3 * s, cy - ry - 3 * s),
                    size = Size((rx + 3 * s) * 2f, (ry + 3 * s) * 2f),
                    style = Stroke(width = 6f * s)
                )
            }
            drawOval(
                color = col.copy(alpha = 0.9f),
                topLeft = Offset(cx - rx, cy - ry),
                size = Size(rx * 2f, ry * 2f),
                style = Stroke(width = 1.6f * s)
            )
        }

        // Center planet glow
        if (glow) {
            drawCircle(color = col.copy(alpha = 0.10f), radius = 14f * s, center = Offset(cx, cy))
            drawCircle(color = col.copy(alpha = 0.22f), radius =  9f * s, center = Offset(cx, cy))
        }

        // Center planet: circle r=5.5 at (32,32)
        drawCircle(color = col, radius = 5.5f * s, center = Offset(cx, cy))

        // Large satellite: circle r=2.8 at (51.5, 27.5)
        drawCircle(color = col, radius = 2.8f * s, center = Offset(51.5f * s, 27.5f * s))

        // Small satellite: circle r=2.1 at (14, 37.5), opacity=0.7
        drawCircle(color = col.copy(alpha = 0.7f), radius = 2.1f * s, center = Offset(14f * s, 37.5f * s))

        // Tiny dot: circle r=1.7 at (34, 19.5), opacity=0.45
        drawCircle(color = col.copy(alpha = 0.45f), radius = 1.7f * s, center = Offset(34f * s, 19.5f * s))
    }
}

// ─── Dot ──────────────────────────────────────────────────────
@Composable
fun Dot(col: Color, ping: Boolean = false) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(8.dp)) {
        if (ping) {
            val transition = rememberInfiniteTransition(label = "ping")
            val scale by transition.animateFloat(
                initialValue = 1f, targetValue = 2.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1300, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "pingScale"
            )
            val alpha by transition.animateFloat(
                initialValue = 0.55f, targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1300, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "pingAlpha"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(scale)
                    .background(col.copy(alpha = alpha), CircleShape)
            )
        }
        Box(modifier = Modifier.size(8.dp).background(col, CircleShape))
    }
}

// ─── Pill ─────────────────────────────────────────────────────
@Composable
fun Pill(text: String, col: Color) {
    Text(
        text = text,
        style = OrbitalTheme.typography.labelSmall.copy(
            color = col,
            fontSize = 7.5.sp,
            fontFamily = OrbitalTheme.fonts.mono
        ),
        modifier = Modifier
            .background(col.copy(alpha = 0.13f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

// ─── SectionLabel ─────────────────────────────────────────────
@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = OrbitalTheme.typography.labelSmall.copy(
            color = OrbitalTheme.colors.muted,
            fontSize = 8.5.sp,
            fontFamily = OrbitalTheme.fonts.mono,
            letterSpacing = 0.06.sp
        ),
        modifier = Modifier.padding(bottom = 10.dp, top = 4.dp)
    )
}

// ─── Toggle ───────────────────────────────────────────────────
@Composable
fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    val th = OrbitalTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!value) }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = OrbitalTheme.typography.bodySmall.copy(
                color = th.sub,
                fontSize = 9.5.sp,
                fontFamily = OrbitalTheme.fonts.mono
            )
        )
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (value) th.accentI else th.border)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(if (value) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(if (value) PaddingValues(end = 2.dp) else PaddingValues(start = 2.dp))
                    .background(Color.White, CircleShape)
            )
        }
    }
}

// ─── OrbitalButton ────────────────────────────────────────────
@Composable
fun OrbitalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false
) {
    val th = OrbitalTheme.colors
    val bg = if (danger) Color(0xFFEF4444) else th.accentI
    val border = if (danger) Color(0xFFEF4444) else th.accentP

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = OrbitalTheme.typography.bodyLarge.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        )
    }
}
