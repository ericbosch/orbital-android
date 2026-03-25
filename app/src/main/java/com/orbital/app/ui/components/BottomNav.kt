package com.orbital.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbital.app.ui.theme.OrbitalTheme

private val NAV_ITEMS = listOf(
    Triple("home",     "⌂", "Home"),
    Triple("agents",   "◈", "Agents"),
    Triple("sessions", "☰", "Sessions"),
    Triple("explore",  "⊞", "Explore"),
    Triple("settings", "⚙", "Settings"),
)

@Composable
fun BottomNav(active: String, onChange: (String) -> Unit) {
    val th = OrbitalTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(th.surface)
            .padding(top = 1.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        NAV_ITEMS.forEach { (id, icon, label) ->
            val on = active == id
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onChange(id) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 17.sp,
                    color = if (on) th.accentP else th.muted
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = label,
                    style = OrbitalTheme.typography.labelSmall.copy(
                        color = if (on) th.accentP else th.muted,
                        fontSize = 7.sp,
                        fontFamily = OrbitalTheme.fonts.mono
                    )
                )
                if (on) {
                    Spacer(Modifier.height(1.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 16.dp, height = 2.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(th.accentP)
                    )
                }
            }
        }
    }
}
