package com.orbital.app.ui.screens.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbital.app.domain.SearchResult
import com.orbital.app.domain.Skill
import com.orbital.app.ui.theme.OrbitalTheme

private val GREEN = Color(0xFF22C55E)
private val BLUE = Color(0xFF0066FF)

@Composable
fun ExploreScreen(
    skills: List<Skill>,
    searchResults: List<SearchResult>,
    isSearching: Boolean,
    onSearch: (String) -> Unit
) {
    val th = OrbitalTheme.colors
    val mono = OrbitalTheme.fonts.mono
    val ui = OrbitalTheme.fonts.ui
    val typ = OrbitalTheme.typography

    var tab by remember { mutableStateOf("skills") }
    var query by remember { mutableStateOf("") }

    val news = listOf(
        Triple("Claude claude-opus-4-6 supports 200k context", "2h", "update"),
        Triple("New: parallel tool use in Claude Code", "1d", "feature"),
        Triple("Codex CLI v2.1 — faster file edits", "3d", "release")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 10.dp)
        ) {
            Text(
                text = "Explore",
                style = typ.headlineMedium.copy(
                    color = th.text,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    fontFamily = ui
                )
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(th.raised)
                    .border(1.dp, th.border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (query.isBlank()) {
                    Text(
                        text = "Search projects/sessions...",
                        style = typ.labelSmall.copy(color = th.muted, fontSize = 8.5.sp, fontFamily = mono)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onSearch(it)
                    },
                    textStyle = typ.bodySmall.copy(color = th.text, fontSize = 9.sp, fontFamily = mono),
                    cursorBrush = SolidColor(th.accentP),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("skills", "news", "search").forEach { t ->
                    val active = tab == t
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (active) th.accentI else th.raised)
                            .border(1.dp, if (active) th.accentP else th.border, RoundedCornerShape(20.dp))
                            .clickable { tab = t }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = t.replaceFirstChar { it.uppercase() },
                            style = typ.labelSmall.copy(
                                color = if (active) th.text else th.muted,
                                fontSize = 9.sp,
                                fontFamily = mono
                            )
                        )
                    }
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (tab) {
                "skills" -> items(skills) { skill -> SkillCard(skill = skill) }
                "news" -> items(news) { (title, time, tag) ->
                    val tagColor = when (tag) {
                        "update" -> GREEN
                        "feature" -> th.accentP
                        else -> BLUE
                    }
                    NewsCard(title = title, time = time, tag = tag, tagColor = tagColor)
                }
                else -> {
                    if (isSearching) {
                        item {
                            Text(
                                text = "Searching...",
                                style = typ.labelSmall.copy(color = th.muted, fontSize = 8.5.sp, fontFamily = mono)
                            )
                        }
                    }
                    items(searchResults) { item -> SearchCard(item) }
                }
            }
        }
    }
}

@Composable
private fun SearchCard(item: SearchResult) {
    val th = OrbitalTheme.colors
    val mono = OrbitalTheme.fonts.mono
    val ui = OrbitalTheme.fonts.ui
    val typ = OrbitalTheme.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(th.raised)
            .border(1.dp, th.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = item.title,
            style = typ.bodySmall.copy(
                color = th.text,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = ui
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${item.type.uppercase()} · ${item.subtitle}",
            style = typ.labelSmall.copy(color = th.muted, fontSize = 8.sp, fontFamily = mono)
        )
    }
}

@Composable
private fun SkillCard(skill: Skill) {
    val th = OrbitalTheme.colors
    val mono = OrbitalTheme.fonts.mono
    val ui = OrbitalTheme.fonts.ui
    val typ = OrbitalTheme.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(th.raised)
            .border(1.dp, th.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = skill.name,
                style = typ.bodySmall.copy(
                    color = th.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = ui
                )
            )
            Box(
                modifier = Modifier
                    .background(th.accentP.copy(alpha = 0.13f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = skill.tag,
                    style = typ.labelSmall.copy(color = th.accentP, fontSize = 7.5.sp, fontFamily = mono)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(text = "by community", style = typ.labelSmall.copy(color = th.muted, fontSize = 8.sp, fontFamily = mono))
    }
}

@Composable
private fun NewsCard(title: String, time: String, tag: String, tagColor: Color) {
    val th = OrbitalTheme.colors
    val mono = OrbitalTheme.fonts.mono
    val ui = OrbitalTheme.fonts.ui
    val typ = OrbitalTheme.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(th.raised)
            .border(1.dp, th.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(tagColor.copy(alpha = 0.13f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(text = tag, style = typ.labelSmall.copy(color = tagColor, fontSize = 7.5.sp, fontFamily = mono))
            }
            Text(text = "$time ago", style = typ.labelSmall.copy(color = th.muted, fontSize = 8.sp, fontFamily = mono))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            style = typ.bodySmall.copy(color = th.text, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = ui)
        )
    }
}
