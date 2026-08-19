package com.rork.calzyandroid.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rork.calzyandroid.AppViewModel
import com.rork.calzyandroid.ui.components.CalzyCard
import com.rork.calzyandroid.ui.components.FullScreenSheet
import com.rork.calzyandroid.ui.components.MetricText
import com.rork.calzyandroid.ui.components.Pressable
import com.rork.calzyandroid.ui.components.PrimaryButton
import com.rork.calzyandroid.ui.theme.CalzyColors

private enum class Plan(val title: String, val price: String, val caption: String) {
    yearly("Yearly", "$29.99/yr", "2 months free"),
    monthly("Monthly", "$4.99/mo", "Cancel anytime"),
}

/** ModernBody Pro paywall (local mock — flips the Pro flag). */
@Composable
fun PaywallSheet(open: Boolean, viewModel: AppViewModel, onClose: () -> Unit) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    var plan by remember { mutableStateOf(Plan.yearly) }

    LaunchedEffect(open) {
        if (open) plan = Plan.yearly
    }

    FullScreenSheet(
        open = open,
        onClose = onClose,
        title = "ModernBody Pro",
        footer = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    text = if (data.profile.isPro) {
                        "You're a Pro member"
                    } else {
                        "Start with ${plan.title}"
                    },
                    enabled = !data.profile.isPro,
                    onClick = {
                        viewModel.setProfile { it.copy(isPro = true) }
                        onClose()
                    },
                )
                Text(
                    text = "Recurring billing · Cancel anytime",
                    fontSize = 11.sp,
                    color = CalzyColors.inkFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Hero
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CalzyColors.plum, CalzyColors.protein),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
                MetricText(text = "Go Pro", size = 30)
                Text(
                    text = "Unlimited AI scans, deeper insights and\nevery future feature — day one.",
                    fontSize = 14.sp,
                    color = CalzyColors.inkSoft,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
            }

            // Features
            CalzyCard(radius = 22.dp) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Feature(
                        icon = Icons.Outlined.CameraAlt,
                        title = "Unlimited scans",
                        caption = "Photograph every plate, no daily cap",
                    )
                    Feature(
                        icon = Icons.Outlined.Insights,
                        title = "Deeper insights",
                        caption = "Weekly trends, macro balance coaching",
                    )
                    Feature(
                        icon = Icons.Outlined.Language,
                        title = "All 32 languages",
                        caption = "AI answers in the language you choose",
                    )
                }
            }

            // Plans
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Plan.entries.forEach { option ->
                    val active = plan == option
                    Pressable(onClick = { plan = option }, modifier = Modifier.weight(1f)) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.85f))
                                .border(
                                    width = if (active) 2.dp else 1.dp,
                                    color = if (active) {
                                        CalzyColors.ink
                                    } else {
                                        CalzyColors.cardBorder
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                )
                                .padding(vertical = 16.dp),
                        ) {
                            Text(
                                text = option.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CalzyColors.ink,
                            )
                            MetricText(text = option.price, size = 17)
                            Text(
                                text = option.caption,
                                fontSize = 11.sp,
                                color = if (option == Plan.yearly) {
                                    CalzyColors.mint
                                } else {
                                    CalzyColors.inkFaint
                                },
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, caption: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(CalzyColors.ink.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CalzyColors.ink,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CalzyColors.ink,
            )
            Text(text = caption, fontSize = 12.sp, color = CalzyColors.inkFaint)
        }
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = CalzyColors.mint,
            modifier = Modifier.size(16.dp),
        )
    }
}
