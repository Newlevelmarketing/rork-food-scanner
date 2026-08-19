package com.rork.calzyandroid.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.calzyandroid.ui.components.LocalT
import com.rork.calzyandroid.ui.components.Pressable
import com.rork.calzyandroid.ui.theme.CalzyColors

/** Floating pill tab bar hovering above the bottom safe area. */
@Composable
fun TabBar(
    active: AppTab,
    onChange: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = LocalT.current
    val tabs: List<Triple<AppTab, ImageVector, String>> = listOf(
        Triple(AppTab.home, Icons.Outlined.Home, t("tab.home")),
        Triple(AppTab.progress, Icons.AutoMirrored.Outlined.ShowChart, t("tab.progress")),
        Triple(AppTab.settings, Icons.Outlined.Settings, t("tab.settings")),
    )

    Row(
        modifier = modifier
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .padding(bottom = 14.dp)
            .shadow(elevation = 18.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.94f))
            .border(1.dp, CalzyColors.cardBorder, CircleShape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { (tab, icon, label) ->
            val selected = tab == active
            val tint by animateColorAsState(
                targetValue = if (selected) CalzyColors.ink else CalzyColors.inkFaint,
                label = "tabTint",
            )
            Pressable(onClick = { onChange(tab) }) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .width(72.dp)
                        .padding(vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = tint,
                    )
                }
            }
        }
    }
}
