package com.rork.calzyandroid.ui.sheets

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rork.calzyandroid.data.PlanTerm
import com.rork.calzyandroid.data.PurchaseManager
import com.rork.calzyandroid.data.SubscriptionPlan
import com.rork.calzyandroid.ui.components.CalzyCard
import com.rork.calzyandroid.ui.components.FullScreenSheet
import com.rork.calzyandroid.ui.components.MetricText
import com.rork.calzyandroid.ui.components.Pressable
import com.rork.calzyandroid.ui.theme.CalzyColors

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * ModernBody Pro paywall backed by the live RevenueCat offering.
 *
 * Every price on screen comes from the store product, never from a constant, so
 * the figure shown is the figure charged in the user's own currency. When no
 * offering can be loaded the screen says so instead of inventing prices.
 */
@Composable
fun PaywallSheet(
    open: Boolean,
    onClose: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val state by PurchaseManager.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(open) {
        if (open) {
            PurchaseManager.clearMessage()
            PurchaseManager.refreshOfferings()
        }
    }

    // Purchase and restore both end with an active entitlement; close on success.
    LaunchedEffect(state.isSubscribed) {
        if (open && state.isSubscribed) onClose()
    }

    FullScreenSheet(
        open = open,
        onClose = onClose,
        title = "ModernBody Pro",
        footer = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 10.dp),
            ) {
                Pressable(
                    onClick = { PurchaseManager.restore() },
                    enabled = !state.isRestoring,
                ) {
                    Text(
                        text = if (state.isRestoring) "Restoring…" else "Restore Purchases",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CalzyColors.ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Pressable(onClick = onOpenTerms) {
                            Text(
                                text = "Terms of Use",
                                fontSize = 12.sp,
                                color = CalzyColors.inkFaint,
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Pressable(onClick = onOpenPrivacy) {
                            Text(
                                text = "Privacy Policy",
                                fontSize = 12.sp,
                                color = CalzyColors.inkFaint,
                            )
                        }
                    }
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
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

            when {
                state.isLoading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = CalzyColors.ink)
                }

                state.plans.isEmpty() -> Notice(
                    text = state.message
                        ?: "Subscription plans can't be loaded right now. " +
                        "ModernBody stays fully usable in the meantime.",
                )

                else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.plans.forEach { plan ->
                        PlanCard(
                            plan = plan,
                            purchasing = state.purchasingId == plan.id,
                            disabled = state.purchasingId != null || plan.rcPackage == null,
                            onSubscribe = {
                                context.findActivity()?.let { activity ->
                                    PurchaseManager.purchase(activity, plan)
                                }
                            },
                        )
                    }
                }
            }

            state.message?.let { message ->
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = CalzyColors.inkSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Guideline 3.1.2 / Play billing disclosure.
            Text(
                text = "Subscriptions renew automatically for the same period at the " +
                    "price shown above, charged to your Google Play account, unless " +
                    "auto-renew is turned off at least 24 hours before the current " +
                    "period ends. Manage or cancel in the Google Play Store under " +
                    "Payments & subscriptions. Uninstalling the app does not cancel " +
                    "a subscription.",
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = CalzyColors.inkFaint,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Notice(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CalzyColors.fat.copy(alpha = 0.12f))
            .padding(14.dp),
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = CalzyColors.inkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    purchasing: Boolean,
    disabled: Boolean,
    onSubscribe: () -> Unit,
) {
    val highlighted = plan.term == PlanTerm.YEARLY
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.85f))
            .border(
                width = if (highlighted) 2.dp else 1.dp,
                color = if (highlighted) CalzyColors.ink else CalzyColors.cardBorder,
                shape = shape,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        text = plan.term.label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CalzyColors.ink,
                    )
                    plan.badge?.let { badge ->
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(CalzyColors.mint)
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = badge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }
                Text(
                    text = "Billed every ${plan.term.unitNoun} · auto-renews",
                    fontSize = 12.sp,
                    color = CalzyColors.inkFaint,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                MetricText(text = plan.price, size = 20)
                plan.perUnit?.let {
                    Text(text = it, fontSize = 11.sp, color = CalzyColors.inkFaint)
                }
            }
        }

        Pressable(
            onClick = onSubscribe,
            enabled = !disabled && !purchasing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(
                        if (disabled) CalzyColors.ink.copy(alpha = 0.3f) else CalzyColors.ink,
                    )
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when {
                        purchasing -> "Processing…"
                        else -> "Subscribe · ${plan.price}"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
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
