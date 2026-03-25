package com.orbital.app.ui.screens.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbital.app.domain.DiagnosticCheck
import com.orbital.app.ui.theme.OrbitalTheme

private val GREEN = Color(0xFF22C55E)
private val RED = Color(0xFFEF4444)

@Composable
fun TroubleshootScreen(
    serverName: String,
    checks: List<DiagnosticCheck>,
    isRunning: Boolean,
    onRun: () -> Unit,
    onBack: () -> Unit
) {
    val th = OrbitalTheme.colors
    val mono = OrbitalTheme.fonts.mono
    val ui = OrbitalTheme.fonts.ui
    val typ = OrbitalTheme.typography

    LaunchedEffect(Unit) {
        if (checks.isEmpty()) onRun()
    }

    val allDone = checks.isNotEmpty() && checks.all { it.ok }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = th.border, shape = RoundedCornerShape(0.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                color = th.accentP,
                fontSize = 18.sp,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(8.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Troubleshoot",
                style = typ.bodySmall.copy(
                    color = th.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = ui
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
                text = "Running diagnostics on $serverName...",
                style = typ.labelSmall.copy(color = th.muted, fontSize = 9.sp, fontFamily = mono),
                modifier = Modifier.padding(bottom = 18.dp)
            )

            if (checks.isEmpty() && isRunning) {
                Text(
                    text = "Collecting telemetry...",
                    style = typ.labelSmall.copy(color = th.sub, fontSize = 8.5.sp, fontFamily = mono),
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            checks.forEach { check ->
                val color = if (check.ok) GREEN else RED
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(th.raised, RoundedCornerShape(10.dp))
                        .border(1.dp, th.border, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = check.label,
                            style = typ.bodySmall.copy(color = th.text, fontSize = 9.5.sp, fontFamily = mono)
                        )
                        if (check.details.isNotBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = check.details,
                                style = typ.labelSmall.copy(color = th.muted, fontSize = 8.sp, fontFamily = mono)
                            )
                        }
                    }
                    Text(
                        text = if (check.ok) "OK" else "FAIL",
                        style = typ.labelSmall.copy(color = color, fontSize = 8.5.sp, fontFamily = mono)
                    )
                }
            }

            if (allDone) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "All systems nominal",
                    style = typ.bodySmall.copy(color = GREEN, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = ui)
                )
            }

            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(th.accentI, RoundedCornerShape(12.dp))
                    .border(1.dp, th.accentP, RoundedCornerShape(12.dp))
                    .clickable(enabled = !isRunning) { onRun() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRunning) "Running..." else "Run diagnostics again",
                    style = typ.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = ui)
                )
            }
        }
    }
}
