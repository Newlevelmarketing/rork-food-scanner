package com.rork.calzyandroid.ui.screens

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.NordicWalking
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rork.calzyandroid.AppViewModel
import com.rork.calzyandroid.BuildConfig
import com.rork.calzyandroid.data.Legal
import com.rork.calzyandroid.data.PurchaseManager
import com.rork.calzyandroid.ui.components.CalzyCard
import com.rork.calzyandroid.ui.components.CalzyToggle
import com.rork.calzyandroid.ui.components.Hairline
import com.rork.calzyandroid.ui.components.LocalAppLanguage
import com.rork.calzyandroid.ui.components.LocalT
import com.rork.calzyandroid.ui.components.MetricText
import com.rork.calzyandroid.ui.components.Pressable
import com.rork.calzyandroid.ui.sheets.AccountSheet
import com.rork.calzyandroid.ui.sheets.ActivitySheet
import com.rork.calzyandroid.ui.sheets.GoalsWeightSheet
import com.rork.calzyandroid.ui.sheets.LanguageSheet
import com.rork.calzyandroid.ui.sheets.NutritionGoalsSheet
import com.rork.calzyandroid.ui.sheets.PaywallSheet
import com.rork.calzyandroid.ui.sheets.PrivacyPolicySheet
import com.rork.calzyandroid.ui.sheets.RemindersSheet
import com.rork.calzyandroid.ui.sheets.TermsOfUseSheet
import com.rork.calzyandroid.ui.theme.CalzyColors

private enum class SettingsRoute { account, nutrition, goals, reminders, activity, language }

/** Settings tab: grouped cards with section captions. */
@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val t = LocalT.current
    val language = LocalAppLanguage.current
    val context = LocalContext.current
    val data by viewModel.data.collectAsStateWithLifecycle()
    val profile = data.profile
    val store by PurchaseManager.state.collectAsStateWithLifecycle()

    var route by remember { mutableStateOf<SettingsRoute?>(null) }
    var showPaywall by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var confirmErase by remember { mutableStateOf(false) }

    val displayName = profile.name.trim().ifEmpty { "Your profile" }
    val initials = displayName
        .split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifEmpty { "C" }

    fun openLink(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Text(
            text = t("s.title"),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = CalzyColors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        )

        Section(title = t("s.account")) {
            Pressable(onClick = { route = SettingsRoute.account }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CalzyColors.ink),
                        contentAlignment = Alignment.Center,
                    ) {
                        MetricText(text = initials, size = 17, color = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(
                                text = displayName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CalzyColors.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = null,
                                tint = CalzyColors.mint,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Text(
                            text = if (store.isSubscribed) t("s.pro") else t("s.free"),
                            fontSize = 13.sp,
                            color = CalzyColors.inkFaint,
                        )
                    }
                    Chevron()
                }
            }
        }

        Section(title = t("s.personal")) {
            NavRow(icon = Icons.Outlined.TrackChanges, title = t("s.goals")) {
                route = SettingsRoute.nutrition
            }
            RowDivider()
            NavRow(icon = Icons.Outlined.MonitorWeight, title = t("s.weight")) {
                route = SettingsRoute.goals
            }
            RowDivider()
            NavRow(icon = Icons.Outlined.Notifications, title = t("s.reminders")) {
                route = SettingsRoute.reminders
            }
            RowDivider()
            NavRow(icon = Icons.Outlined.NordicWalking, title = t("s.activity")) {
                route = SettingsRoute.activity
            }
        }

        Section(title = t("s.app")) {
            NavRow(
                icon = Icons.Outlined.Language,
                title = t("s.language"),
                value = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(text = language.flag, fontSize = 16.sp)
                        Text(
                            text = language.nativeName,
                            fontSize = 15.sp,
                            color = CalzyColors.inkFaint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
            ) {
                route = SettingsRoute.language
            }
        }

        Section(title = t("s.integrations")) {
            Pressable(onClick = {
                viewModel.setProfile { it.copy(healthSynced = !it.healthSynced) }
            }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = CalzyColors.protein,
                        modifier = Modifier
                            .width(28.dp)
                            .size(19.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Health Connect",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CalzyColors.ink,
                        )
                        Text(
                            text = t("s.healthSub"),
                            fontSize = 12.sp,
                            color = CalzyColors.inkFaint,
                        )
                    }
                    Text(
                        text = if (profile.healthSynced) t("s.connected") else t("s.connect"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (profile.healthSynced) CalzyColors.mint else CalzyColors.inkFaint,
                    )
                }
            }
        }

        Section(title = t("s.preferences")) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            ) {
                Text(
                    text = "\uD83C\uDFAD",
                    fontSize = 22.sp,
                    modifier = Modifier.width(28.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = t("s.jester"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CalzyColors.ink,
                    )
                    Text(
                        text = t("s.jesterSub"),
                        fontSize = 12.sp,
                        color = CalzyColors.inkFaint,
                        lineHeight = 16.sp,
                    )
                }
                CalzyToggle(
                    checked = profile.jesterMode,
                    onChange = { value -> viewModel.setProfile { it.copy(jesterMode = value) } },
                )
            }
        }

        // Hidden entirely in a release build with no RevenueCat key, so an
        // unconfigured binary can never present an unpurchasable paywall.
        if (store.isConfigured || BuildConfig.DEBUG) {
            Section(title = t("s.subscription")) {
                Pressable(onClick = { showPaywall = true }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
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
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Text(
                                text = "ModernBody Pro",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CalzyColors.ink,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (store.isSubscribed) {
                                            CalzyColors.mint
                                        } else {
                                            CalzyColors.ink
                                        },
                                    )
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = if (store.isSubscribed) {
                                        t("s.active")
                                    } else {
                                        t("s.upgrade")
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }
                            Text(
                                text = if (store.isSubscribed) {
                                    "Unlimited scans and insights"
                                } else {
                                    "Unlimited scans, deeper insights"
                                },
                                fontSize = 12.sp,
                                color = CalzyColors.inkFaint,
                            )
                        }
                        Chevron()
                    }
                }
            }
        }

        // The documents ship inside the APK so they always describe this build,
        // rather than pointing at a generic hosted page about another product.
        Section(title = t("s.support")) {
            NavRow(icon = Icons.Outlined.Description, title = t("s.terms")) {
                showTerms = true
            }
            RowDivider()
            NavRow(icon = Icons.Outlined.Lock, title = t("s.privacy")) {
                showPrivacy = true
            }
            if (Legal.SUPPORT_EMAIL.isNotBlank()) {
                RowDivider()
                LinkRow(icon = Icons.Outlined.Mail, title = t("s.email")) {
                    openLink("mailto:${Legal.SUPPORT_EMAIL}")
                }
            }
        }

        // Erase all data
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (confirmErase) {
                Text(
                    text = "This removes every meal, weight and photo from this device.",
                    fontSize = 13.sp,
                    color = CalzyColors.inkSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Pressable(onClick = { confirmErase = false }, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .background(CalzyColors.ink.copy(alpha = 0.06f))
                                .padding(vertical = 15.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CalzyColors.ink,
                            )
                        }
                    }
                    Pressable(
                        onClick = {
                            viewModel.eraseAll()
                            confirmErase = false
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .background(CalzyColors.protein)
                                .padding(vertical = 15.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Erase everything",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                        }
                    }
                }
            } else {
                Pressable(onClick = { confirmErase = true }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(CalzyColors.protein.copy(alpha = 0.1f))
                            .padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = t("s.erase"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CalzyColors.protein,
                        )
                    }
                }
            }
        }

        Text(
            text = "${t("s.version")} 1.0.0",
            fontSize = 12.sp,
            color = CalzyColors.inkFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )
    }

    AccountSheet(
        open = route == SettingsRoute.account,
        viewModel = viewModel,
        onClose = { route = null },
    )
    NutritionGoalsSheet(
        open = route == SettingsRoute.nutrition,
        viewModel = viewModel,
        onClose = { route = null },
    )
    GoalsWeightSheet(
        open = route == SettingsRoute.goals,
        viewModel = viewModel,
        onClose = { route = null },
    )
    RemindersSheet(
        open = route == SettingsRoute.reminders,
        viewModel = viewModel,
        onClose = { route = null },
    )
    ActivitySheet(
        open = route == SettingsRoute.activity,
        viewModel = viewModel,
        onClose = { route = null },
    )
    LanguageSheet(
        open = route == SettingsRoute.language,
        viewModel = viewModel,
        onClose = { route = null },
    )
    PaywallSheet(
        open = showPaywall,
        onClose = { showPaywall = false },
        onOpenTerms = { showTerms = true },
        onOpenPrivacy = { showPrivacy = true },
    )
    // Declared after the paywall so the documents layer above it when opened
    // from the subscription screen's compliance links.
    TermsOfUseSheet(
        open = showTerms,
        onClose = { showTerms = false },
        title = t("s.terms"),
    )
    PrivacyPolicySheet(
        open = showPrivacy,
        onClose = { showPrivacy = false },
        title = t("s.privacy"),
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            color = CalzyColors.inkFaint,
            modifier = Modifier.padding(start = 24.dp),
        )
        CalzyCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            radius = 22.dp,
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun RowDivider() {
    Hairline(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 55.dp),
    )
}

@Composable
private fun Chevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
        contentDescription = null,
        tint = CalzyColors.inkFaint,
        modifier = Modifier.size(13.dp),
    )
}

@Composable
private fun NavRow(
    icon: ImageVector,
    title: String,
    value: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Pressable(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 15.dp),
        ) {
            Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CalzyColors.ink,
                    modifier = Modifier.size(17.dp),
                )
            }
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = CalzyColors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            value?.invoke()
            Chevron()
        }
    }
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Pressable(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 15.dp),
        ) {
            Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CalzyColors.ink,
                    modifier = Modifier.size(17.dp),
                )
            }
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = CalzyColors.ink,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
                tint = CalzyColors.inkFaint,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}
