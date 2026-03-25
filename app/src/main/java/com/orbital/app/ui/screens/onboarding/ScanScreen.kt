package com.orbital.app.ui.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbital.app.core.network.DiscoveredServer
import com.orbital.app.ui.components.Mark
import com.orbital.app.ui.theme.OrbitalTheme
import com.orbital.app.ui.theme.StatusGreen
import com.orbital.app.ui.viewmodel.ConnectionState

@Composable
fun ScanScreen(
    discoveredServers: List<DiscoveredServer>,
    connectionState: ConnectionState,
    onStartScan: () -> Unit,
    onConnect: (DiscoveredServer, String) -> Unit,
    onManual: (host: String, port: Int, token: String) -> Unit
) {
    val th = OrbitalTheme.colors
    var connectTarget by remember { mutableStateOf<DiscoveredServer?>(null) }
    var tokenInput    by remember { mutableStateOf("") }
    var showManual    by remember { mutableStateOf(false) }
    var manualHost    by remember { mutableStateOf("") }
    var manualPort    by remember { mutableStateOf("8080") }
    var manualToken   by remember { mutableStateOf("") }

    val isScanning   = connectionState is ConnectionState.Scanning
    val isConnecting = connectionState is ConnectionState.Connecting
    val errorMsg     = (connectionState as? ConnectionState.Error)?.message

    LaunchedEffect(Unit) { onStartScan() }

    // Token dialog — discovered server
    connectTarget?.let { server ->
        AlertDialog(
            onDismissRequest = { connectTarget = null; tokenInput = "" },
            title   = { Text("Connect to ${server.name}") },
            text    = {
                OutlinedTextField(
                    value              = tokenInput,
                    onValueChange      = { tokenInput = it },
                    label              = { Text("Auth token (leave blank if none)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine         = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onConnect(server, tokenInput)
                    connectTarget = null; tokenInput = ""
                }) { Text("Connect") }
            },
            dismissButton = {
                TextButton(onClick = { connectTarget = null; tokenInput = "" }) { Text("Cancel") }
            }
        )
    }

    // Manual IP dialog
    if (showManual) {
        AlertDialog(
            onDismissRequest = { showManual = false },
            title = { Text("Enter IP manually") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value         = manualHost,
                        onValueChange = { manualHost = it },
                        label         = { Text("Host / IP") },
                        singleLine    = true
                    )
                    OutlinedTextField(
                        value             = manualPort,
                        onValueChange     = { manualPort = it },
                        label             = { Text("Port") },
                        keyboardOptions   = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine        = true
                    )
                    OutlinedTextField(
                        value                = manualToken,
                        onValueChange        = { manualToken = it },
                        label                = { Text("Auth token (leave blank if none)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine           = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onManual(manualHost, manualPort.toIntOrNull() ?: 8080, manualToken)
                    showManual = false
                }) { Text("Connect") }
            },
            dismissButton = {
                TextButton(onClick = { showManual = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text  = "step 1 / 3",
            style = OrbitalTheme.typography.labelSmall.copy(color = th.muted)
        )
        Text(
            text     = "Discover servers",
            style    = OrbitalTheme.typography.headlineMedium.copy(color = th.text, fontSize = 22.sp),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text     = if (isScanning) "Scanning LAN for Orbital servers…" else "Scan complete",
            style    = OrbitalTheme.typography.labelSmall.copy(color = th.sub, fontSize = 10.sp, lineHeight = 16.sp),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Radar animation
        Box(
            modifier        = Modifier
                .size(130.dp)
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "radar")
            val ringColor = if (discoveredServers.isEmpty()) th.accentP else StatusGreen

            for (i in 1..3) {
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue  = 2.5f,
                    animationSpec = infiniteRepeatable(
                        animation  = tween(1200 + i * 300, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "scale$i"
                )
                val opacity by infiniteTransition.animateFloat(
                    initialValue = 0.5f,
                    targetValue  = 0f,
                    animationSpec = infiniteRepeatable(
                        animation  = tween(1200 + i * 300, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "opacity$i"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .scale(scale)
                        .background(ringColor.copy(alpha = opacity), CircleShape)
                        .border(1.dp, ringColor.copy(alpha = opacity), CircleShape)
                )
            }

            if (discoveredServers.isNotEmpty()) {
                Mark(sz = 34.dp, col = StatusGreen, glow = true)
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(2.dp, th.accentP, CircleShape)
                        .clip(CircleShape)
                )
            }
        }

        // Error
        errorMsg?.let {
            Text(
                text     = it,
                style    = OrbitalTheme.typography.labelSmall.copy(color = Color.Red, fontSize = 11.sp),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Results
        if (discoveredServers.isNotEmpty()) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                discoveredServers.forEach { server ->
                    val connecting = isConnecting &&
                        (connectionState as? ConnectionState.Connecting)?.server == server
                    ServerItem(
                        name         = server.name,
                        host         = "${server.host}:${server.port}",
                        via          = server.via,
                        isConnecting = connecting,
                        onClick      = { connectTarget = server }
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 20.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(th.raised)
                    .border(1.dp, th.border, RoundedCornerShape(12.dp))
                    .clickable { showManual = true }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "Enter IP manually",
                    style = OrbitalTheme.typography.bodyLarge.copy(color = th.text, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun ServerItem(
    name: String,
    host: String,
    via: String,
    isConnecting: Boolean = false,
    onClick: () -> Unit = {}
) {
    val th       = OrbitalTheme.colors
    val viaColor = when (via) {
        "LAN"      -> StatusGreen
        "Tailscale"-> Color(0xFF7B68EE)
        else       -> Color(0xFF888888)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(th.raised, RoundedCornerShape(14.dp))
            .border(1.dp, th.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text  = if (isConnecting) "Connecting…" else name,
                    style = OrbitalTheme.typography.headlineMedium.copy(fontSize = 12.sp, color = th.text)
                )
                Box(
                    modifier = Modifier
                        .background(viaColor.copy(alpha = 0.13f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(text = via, style = OrbitalTheme.typography.labelSmall.copy(color = viaColor, fontSize = 8.sp))
                }
            }
            Text(text = host, style = OrbitalTheme.typography.labelSmall.copy(color = th.muted))
        }
    }
}
