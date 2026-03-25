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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbital.app.domain.ChatMessage
import com.orbital.app.domain.Session
import com.orbital.app.ui.components.Dot
import com.orbital.app.ui.theme.OrbitalTheme

private val GREEN = Color(0xFF22C55E)

private val MODELS = listOf(
    "opus" to "claude-opus-4-6",
    "sonnet" to "claude-sonnet-4-6",
    "haiku" to "claude-haiku-4-5",
    "codex" to "codex-mini-latest",
    "gpt4o" to "gpt-4o",
    "gemini" to "gemini-2.5-pro"
)
private val MODES = listOf("chat", "code", "plan", "ask", "review")

@Composable
fun ChatScreen(
    session: Session,
    messages: List<ChatMessage>,
    isStreaming: Boolean,
    hasOlderMessages: Boolean,
    isLoadingOlder: Boolean,
    errorMessage: String?,
    onLoadOlderMessages: () -> Unit,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    val th = OrbitalTheme.colors
    val mono = OrbitalTheme.fonts.mono
    val ui = OrbitalTheme.fonts.ui
    val typ = OrbitalTheme.typography

    var input by remember { mutableStateOf("") }
    var activeModel by remember { mutableStateOf("opus") }
    var activeMode by remember { mutableStateOf("code") }
    var showModel by remember { mutableStateOf(false) }
    var showSkills by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val isNearBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val lastIndex = messages.lastIndex
            lastIndex <= 0 || lastVisible >= lastIndex - 2
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && !isLoadingOlder && isNearBottom) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LaunchedEffect(listState, hasOlderMessages, isLoadingOlder) {
        if (!hasOlderMessages) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (!isLoadingOlder && hasOlderMessages && index == 0 && offset <= 8) {
                    onLoadOlderMessages()
                }
            }
    }

    fun send() {
        if (input.isBlank() || isStreaming) return
        onSendMessage(input)
        input = ""
    }

    val modelLabel = MODELS.find { it.first == activeModel }?.second?.split("-")?.first() ?: "model"

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = th.border, shape = RoundedCornerShape(0.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
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
                Spacer(Modifier.width(10.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Dot(col = GREEN, ping = isStreaming)
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            text = session.agent,
                            style = typ.bodySmall.copy(
                                color = th.text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = ui
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = session.name,
                            style = typ.labelSmall.copy(
                                color = th.muted,
                                fontSize = 7.5.sp,
                                fontFamily = mono
                            ),
                            maxLines = 1
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    ChipButton(
                        text = "skills",
                        active = showSkills,
                        onClick = { showSkills = !showSkills; showModel = false }
                    )
                    ChipButton(
                        text = modelLabel,
                        active = showModel,
                        onClick = { showModel = !showModel; showSkills = false },
                        maxWidth = 70.dp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = th.border, shape = RoundedCornerShape(0.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                MODES.forEach { mode ->
                    ChipButton(text = mode, active = activeMode == mode, onClick = { activeMode = mode })
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    style = typ.labelSmall.copy(color = Color.Red, fontSize = 8.5.sp, fontFamily = mono),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .clickable { showModel = false; showSkills = false }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isLoadingOlder) {
                    item {
                        Text(
                            text = "loading older messages...",
                            style = typ.labelSmall.copy(color = th.muted, fontSize = 8.sp, fontFamily = mono),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else if (hasOlderMessages) {
                    item {
                        Text(
                            text = "scroll up to load more",
                            style = typ.labelSmall.copy(color = th.muted, fontSize = 8.sp, fontFamily = mono),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                items(messages) { msg ->
                    val isUser = msg.role == "u"
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 14.dp,
                                        topEnd = 14.dp,
                                        bottomStart = if (isUser) 14.dp else 3.dp,
                                        bottomEnd = if (isUser) 3.dp else 14.dp
                                    )
                                )
                                .background(if (isUser) th.accentI else th.raised)
                                .then(
                                    if (!isUser) {
                                        Modifier.border(
                                            1.dp,
                                            th.border,
                                            RoundedCornerShape(
                                                topStart = 14.dp,
                                                topEnd = 14.dp,
                                                bottomStart = 3.dp,
                                                bottomEnd = 14.dp
                                            )
                                        )
                                    } else Modifier
                                )
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = msg.text,
                                style = typ.bodySmall.copy(
                                    color = th.text,
                                    fontSize = 8.5.sp,
                                    fontFamily = mono,
                                    lineHeight = 13.6.sp
                                )
                            )
                        }
                    }
                }
                if (isStreaming) {
                    item {
                        Text(
                            text = "streaming...",
                            style = typ.labelSmall.copy(color = th.muted, fontSize = 8.sp, fontFamily = mono)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = th.border, shape = RoundedCornerShape(0.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .padding(bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(th.raised)
                        .border(1.dp, th.border, RoundedCornerShape(24.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (input.isEmpty()) {
                        Text(
                            text = if (isStreaming) "waiting for response..." else "message...",
                            style = typ.bodySmall.copy(color = th.muted, fontSize = 9.sp, fontFamily = mono)
                        )
                    }
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        enabled = !isStreaming,
                        textStyle = typ.bodySmall.copy(color = th.text, fontSize = 9.sp, fontFamily = mono),
                        cursorBrush = SolidColor(th.accentP),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(if (isStreaming) th.border else th.accentI, CircleShape)
                        .clickable(enabled = !isStreaming) { send() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "↑", color = Color.White, fontSize = 16.sp)
                }
            }
        }

        if (showModel) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 88.dp, end = 10.dp)
                    .width(200.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(th.surface)
                    .border(1.dp, th.border, RoundedCornerShape(14.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "SELECT MODEL",
                        style = typ.labelSmall.copy(
                            color = th.muted,
                            fontSize = 8.sp,
                            fontFamily = mono,
                            letterSpacing = 0.06.sp
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    MODELS.forEach { (id, label) ->
                        val active = activeModel == id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 5.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) th.accentI.copy(alpha = 0.13f) else th.raised)
                                .border(1.dp, if (active) th.accentP else th.border, RoundedCornerShape(10.dp))
                                .clickable { activeModel = id; showModel = false }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = typ.labelSmall.copy(color = th.text, fontSize = 8.5.sp, fontFamily = mono)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipButton(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    maxWidth: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Unspecified
) {
    val th = OrbitalTheme.colors
    val mono = OrbitalTheme.fonts.mono
    val typ = OrbitalTheme.typography
    Box(
        modifier = Modifier
            .then(if (maxWidth != androidx.compose.ui.unit.Dp.Unspecified) Modifier.widthIn(max = maxWidth) else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) th.accentI else th.raised)
            .border(1.dp, if (active) th.accentP else th.border, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            style = typ.labelSmall.copy(
                color = if (active) th.text else th.muted,
                fontSize = 7.5.sp,
                fontFamily = mono
            ),
            maxLines = 1
        )
    }
}
