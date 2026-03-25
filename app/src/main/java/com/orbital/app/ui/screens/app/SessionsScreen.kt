package com.orbital.app.ui.screens.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.orbital.app.domain.Session
import com.orbital.app.ui.components.Dot
import com.orbital.app.ui.theme.OrbitalTheme

private val GREEN = Color(0xFF22C55E)

@Composable
fun SessionsScreen(sessions: List<Session>, onSession: (Session) -> Unit) {
    val th   = OrbitalTheme.colors
    val mono = OrbitalTheme.fonts.mono
    val ui   = OrbitalTheme.fonts.ui
    val typ  = OrbitalTheme.typography

    var filter by remember { mutableStateOf("all") }
    val filtered = when (filter) {
        "claude" -> sessions.filter { it.agent.lowercase().contains("claude") }
        "codex"  -> sessions.filter { it.agent.lowercase().contains("codex") }
        else     -> sessions
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header + filters
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sessions",
                    style = typ.headlineMedium.copy(
                        color = th.text, fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp, fontFamily = ui
                    )
                )
                Box(
                    modifier = Modifier
                        .background(th.accentP.copy(alpha = 0.13f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "+ new",
                        style = typ.labelSmall.copy(
                            color = th.accentP, fontSize = 7.5.sp, fontFamily = mono
                        )
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("all" to "All", "claude" to "Claude Code", "codex" to "Codex CLI")
                    .forEach { (id, label) ->
                        val active = filter == id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (active) th.accentI else th.raised)
                                .border(1.dp, if (active) th.accentP else th.border, RoundedCornerShape(20.dp))
                                .clickable { filter = id }
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = label,
                                style = typ.labelSmall.copy(
                                    color = if (active) th.text else th.muted,
                                    fontSize = 8.sp, fontFamily = mono
                                )
                            )
                        }
                    }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered) { s ->
                SessionCard(session = s, onClick = { onSession(s) })
            }
        }
    }
}
