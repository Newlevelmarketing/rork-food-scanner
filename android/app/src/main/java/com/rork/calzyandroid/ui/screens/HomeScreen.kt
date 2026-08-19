package com.rork.calzyandroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.rork.calzyandroid.data.MealEntry
import com.rork.calzyandroid.data.Nutrition
import com.rork.calzyandroid.data.caloriesBurned
import com.rork.calzyandroid.data.caloriesEaten
import com.rork.calzyandroid.data.carbsOn
import com.rork.calzyandroid.data.exercisesOn
import com.rork.calzyandroid.data.fatOn
import com.rork.calzyandroid.data.hasLogs
import com.rork.calzyandroid.data.mealsOn
import com.rork.calzyandroid.data.proteinOn
import com.rork.calzyandroid.data.streak
import com.rork.calzyandroid.data.waterOn
import com.rork.calzyandroid.ui.components.CalzyCard
import com.rork.calzyandroid.ui.components.IconMaps
import com.rork.calzyandroid.ui.components.LocalT
import com.rork.calzyandroid.ui.components.MetricText
import com.rork.calzyandroid.ui.components.Pressable
import com.rork.calzyandroid.ui.components.RingProgress
import com.rork.calzyandroid.ui.navigation.HomeRoute
import com.rork.calzyandroid.ui.share.ShareSummary
import com.rork.calzyandroid.ui.theme.CalzyColors
import kotlin.math.roundToInt

/** Main dashboard: date strip, calorie + water rings, macros and the day's meals. */
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onRoute: (HomeRoute) -> Unit,
    onOpenMeal: (MealEntry) -> Unit,
    onEditMeal: (MealEntry) -> Unit,
) {
    val t = LocalT.current
    val context = LocalContext.current
    val data by viewModel.data.collectAsStateWithLifecycle()
    val date by viewModel.selectedDate.collectAsStateWithLifecycle()

    val targets = Nutrition.targetsOf(data.profile)
    val eaten = data.caloriesEaten(date)
    val burned = data.caloriesBurned(date)
    val budget = targets.calories + burned
    val remaining = budget - eaten
    val meals = data.mealsOn(date)
    val workouts = data.exercisesOn(date)
    val water = data.waterOn(date)
    val waterGoal = maxOf(data.profile.waterGoalMl, 1)
    val streak = data.streak()

    val healthScore = if (meals.isEmpty()) {
        "—"
    } else {
        "${(meals.sumOf { it.healthScore }.toDouble() / meals.size).roundToInt()}"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(bottom = 120.dp),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            MetricText(text = "ModernBody", size = 26)
            Row(modifier = Modifier.weight(1f)) {}

            // Streak stays visible at zero so it reads as a goal to chase.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (streak > 0) {
                            CalzyColors.flame.copy(alpha = 0.12f)
                        } else {
                            Color.White.copy(alpha = 0.6f)
                        },
                    )
                    .padding(horizontal = 11.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = "$streak day streak",
                    tint = if (streak > 0) CalzyColors.flame else CalzyColors.inkFaint,
                    modifier = Modifier.size(14.dp),
                )
                MetricText(
                    text = "$streak",
                    size = 15,
                    color = if (streak > 0) CalzyColors.flame else CalzyColors.inkFaint,
                )
            }

            Pressable(onClick = {
                ShareSummary.share(
                    context = context,
                    data = data,
                    date = date,
                    targets = targets,
                )
            }) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share your day",
                        tint = CalzyColors.ink,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }

        DateStrip(
            selected = date,
            onSelect = { viewModel.setSelectedDate(it) },
            hasLogs = { data.hasLogs(it) },
            modifier = Modifier.padding(top = 6.dp),
        )

        // Energy card
        CalzyCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp),
            radius = 28.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 15.dp),
            ) {
                // Calories ring
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box {
                        RingProgress(
                            progress = if (budget > 0) eaten.toFloat() / budget else 0f,
                            size = 104.dp,
                            lineWidth = 10.dp,
                            color = CalzyColors.ink,
                            trackColor = CalzyColors.ink.copy(alpha = 0.08f),
                        ) {
                            MetricText(text = "$eaten", size = 27)
                            Text(
                                text = "/$budget",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = CalzyColors.inkFaint,
                            )
                        }
                        if (burned > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 0.dp, top = 0.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = CalzyColors.flame,
                                    modifier = Modifier.size(10.dp),
                                )
                                MetricText(text = "+$burned", size = 13, color = CalzyColors.flame)
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = t("h.eaten"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = CalzyColors.inkSoft,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (remaining >= 0) {
                                "$remaining ${t("h.left")}"
                            } else {
                                "${-remaining} ${t("h.over")}"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (remaining >= 0) CalzyColors.mint else CalzyColors.protein,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(width = 1.dp, height = 96.dp)
                        .background(CalzyColors.ink.copy(alpha = 0.06f)),
                )

                // Water ring
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RingProgress(
                        progress = water.toFloat() / waterGoal,
                        size = 104.dp,
                        lineWidth = 10.dp,
                        color = CalzyColors.water,
                        trackColor = CalzyColors.water.copy(alpha = 0.12f),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WaterDrop,
                            contentDescription = null,
                            tint = CalzyColors.water,
                            modifier = Modifier.size(15.dp),
                        )
                        MetricText(text = "$water", size = 22)
                        Text(
                            text = "ml",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = CalzyColors.inkFaint,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Pressable(onClick = { viewModel.addWater(250, date) }) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(CalzyColors.water.copy(alpha = 0.14f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = "+ 250ml",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CalzyColors.water,
                                )
                            }
                        }
                        Pressable(onClick = { viewModel.addWater(500, date) }) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(CalzyColors.water.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "½L",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CalzyColors.water,
                                )
                            }
                        }
                        if (water > 0) {
                            Pressable(onClick = { viewModel.undoWater(date) }) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(CalzyColors.ink.copy(alpha = 0.05f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Undo last water entry",
                                        tint = CalzyColors.inkFaint,
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Macro / insights carousel
        val pagerState = rememberPagerState(pageCount = { 2 })
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) { page ->
            if (page == 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MacroTile(
                        title = t("h.protein"),
                        emoji = "\uD83C\uDF57",
                        eaten = data.proteinOn(date),
                        goal = targets.protein,
                        tint = CalzyColors.protein,
                        modifier = Modifier.weight(1f),
                    )
                    MacroTile(
                        title = t("h.carbs"),
                        emoji = "\uD83C\uDF5E",
                        eaten = data.carbsOn(date),
                        goal = targets.carbs,
                        tint = CalzyColors.carbs,
                        modifier = Modifier.weight(1f),
                    )
                    MacroTile(
                        title = t("h.fat"),
                        emoji = "\uD83E\uDD51",
                        eaten = data.fatOn(date),
                        goal = targets.fat,
                        tint = CalzyColors.fat,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InsightTile(
                            icon = Icons.AutoMirrored.Outlined.TrendingUp,
                            tint = CalzyColors.plum,
                            title = t("h.burned"),
                            value = "$burned",
                            unit = "kcal",
                            modifier = Modifier.weight(1f),
                        )
                        InsightTile(
                            icon = Icons.Outlined.MonitorWeight,
                            tint = CalzyColors.mint,
                            title = t("h.weight"),
                            value = String.format(java.util.Locale.US, "%.1f", data.profile.currentWeightKg),
                            unit = "kg",
                            modifier = Modifier.weight(1f),
                        )
                        InsightTile(
                            icon = Icons.Outlined.FavoriteBorder,
                            tint = CalzyColors.protein,
                            title = t("h.health"),
                            value = healthScore,
                            unit = "/10",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        text = coachLine(
                            mealCount = meals.size,
                            remaining = remaining,
                            proteinLeft = targets.protein - data.proteinOn(date).roundToInt(),
                        ),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = CalzyColors.inkSoft,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            (0..1).forEach { index ->
                Box(
                    modifier = Modifier
                        .size(
                            width = if (pagerState.currentPage == index) 22.dp else 7.dp,
                            height = 7.dp,
                        )
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index) {
                                CalzyColors.ink
                            } else {
                                CalzyColors.inkFaint.copy(alpha = 0.4f)
                            },
                        ),
                )
            }
        }

        // Quick actions
        QuickActionBar(
            actions = listOf(
                QuickAction(Icons.Outlined.CenterFocusStrong, t("h.scan")) {
                    onRoute(HomeRoute.scan)
                },
                QuickAction(Icons.Outlined.Keyboard, t("h.type")) { onRoute(HomeRoute.describe) },
                QuickAction(Icons.Outlined.Search, t("h.search")) { onRoute(HomeRoute.search) },
                QuickAction(Icons.Outlined.Bookmark, t("h.saved")) { onRoute(HomeRoute.saved) },
                QuickAction(Icons.Outlined.LocalFireDepartment, t("h.exercise")) {
                    onRoute(HomeRoute.exercise)
                },
            ),
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp),
        )

        // Meals
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = CalzyColors.ink,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = t("h.meals"),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = CalzyColors.ink,
                modifier = Modifier.weight(1f),
            )
            if (meals.isNotEmpty()) {
                Text(
                    text = "$eaten kcal",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CalzyColors.inkFaint,
                )
            }
        }

        if (meals.isEmpty()) {
            EmptyMealsState(modifier = Modifier.padding(horizontal = 20.dp))
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                meals.forEach { meal ->
                    MealRow(
                        meal = meal,
                        onClick = { onOpenMeal(meal) },
                        onEdit = { onEditMeal(meal) },
                    )
                }
            }
        }

        // Exercise entries
        if (workouts.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = CalzyColors.ink,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = t("h.exercise"),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = CalzyColors.ink,
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                workouts.forEach { entry ->
                    CalzyCard(radius = 22.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(13.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                                    .background(CalzyColors.flame.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = IconMaps.exercise(entry.icon),
                                    contentDescription = null,
                                    tint = CalzyColors.flame,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CalzyColors.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${entry.minutes} min",
                                    fontSize = 13.sp,
                                    color = CalzyColors.inkSoft,
                                )
                            }
                            MetricText(
                                text = "−${entry.calories}",
                                size = 17,
                                color = CalzyColors.flame,
                            )
                            Pressable(onClick = { viewModel.deleteExercise(entry.id) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete ${entry.name}",
                                    tint = CalzyColors.inkFaint,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun coachLine(mealCount: Int, remaining: Int, proteinLeft: Int): String = when {
    mealCount == 0 -> "Log your first meal to unlock today's insights."
    remaining < 0 -> "You're ${-remaining} kcal over — a walk would even it out."
    proteinLeft > 30 -> "${proteinLeft}g of protein still to go today."
    else -> "Great balance so far — $remaining kcal left in the tank."
}

@Composable
private fun InsightTile(
    icon: ImageVector,
    tint: Color,
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    CalzyCard(modifier = modifier, radius = 22.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(17.dp),
                )
            }
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = CalzyColors.inkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                MetricText(text = value, size = 19)
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = CalzyColors.inkFaint,
                )
            }
        }
    }
}
