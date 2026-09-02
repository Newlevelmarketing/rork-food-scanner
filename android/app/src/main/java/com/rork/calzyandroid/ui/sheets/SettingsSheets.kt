package com.rork.calzyandroid.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.NordicWalking
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rork.calzyandroid.AppViewModel
import com.rork.calzyandroid.data.ActivityLevel
import com.rork.calzyandroid.data.GoalDirection
import com.rork.calzyandroid.data.Nutrition
import com.rork.calzyandroid.data.Sex
import com.rork.calzyandroid.ui.components.CalzyCard
import com.rork.calzyandroid.ui.components.CalzyToggle
import com.rork.calzyandroid.ui.components.FullScreenSheet
import com.rork.calzyandroid.ui.components.LocalT
import com.rork.calzyandroid.ui.components.MetricText
import com.rork.calzyandroid.ui.components.Pressable
import com.rork.calzyandroid.ui.components.PrimaryButton
import com.rork.calzyandroid.ui.components.SegmentedControl
import com.rork.calzyandroid.ui.screens.BigSlider
import com.rork.calzyandroid.ui.theme.CalzyColors
import java.util.Locale
import kotlin.math.roundToInt

/** Account sheet: name, sex and birth year. */
@Composable
fun AccountSheet(open: Boolean, viewModel: AppViewModel, onClose: () -> Unit) {
    val data by viewModel.data.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf(Sex.male) }
    var birthYear by remember { mutableIntStateOf(1996) }

    LaunchedEffect(open) {
        if (open) {
            name = data.profile.name
            sex = data.profile.sex
            birthYear = data.profile.birthYear
        }
    }

    FullScreenSheet(
        open = open,
        onClose = onClose,
        title = "Account",
        footer = {
            PrimaryButton(
                text = "Save",
                onClick = {
                    viewModel.setProfile {
                        it.copy(name = name.trim(), sex = sex, birthYear = birthYear)
                    }
                    onClose()
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CalzyCard(radius = 22.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Name",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CalzyColors.inkSoft,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Your name", color = CalzyColors.inkFaint) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CalzyColors.well,
                            unfocusedContainerColor = CalzyColors.well,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        singleLine = true,
                    )
                }
            }

            CalzyCard(radius = 22.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Sex",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CalzyColors.inkSoft,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    SegmentedControl(
                        options = listOf(Sex.male to "Male", Sex.female to "Female"),
                        selected = sex,
                        onSelect = { sex = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            BigSlider(
                title = "Birth year",
                value = birthYear.toDouble(),
                min = 1940.0,
                max = 2012.0,
                format = { "${it.roundToInt()}" },
                unit = "",
                onChange = { birthYear = it.roundToInt() },
            )
        }
    }
}

/** Nutrition goals sheet: auto plan vs custom calorie/macro targets. */
@Composable
fun NutritionGoalsSheet(open: Boolean, viewModel: AppViewModel, onClose: () -> Unit) {
    val t = LocalT.current
    val data by viewModel.data.collectAsStateWithLifecycle()

    var custom by remember { mutableStateOf(false) }
    var calories by remember { mutableIntStateOf(2200) }
    var protein by remember { mutableIntStateOf(140) }
    var carbs by remember { mutableIntStateOf(240) }
    var fat by remember { mutableIntStateOf(70) }
    var waterGoal by remember { mutableIntStateOf(2500) }

    LaunchedEffect(open) {
        if (open) {
            custom = data.profile.usesCustomTargets
            calories = data.profile.customCalories
            protein = data.profile.customProtein
            carbs = data.profile.customCarbs
            fat = data.profile.customFat
            waterGoal = data.profile.waterGoalMl
        }
    }

    val auto = Nutrition.targetsOf(data.profile.copy(usesCustomTargets = false))

    FullScreenSheet(
        open = open,
        onClose = onClose,
        title = t("s.goals"),
        footer = {
            PrimaryButton(
                text = "Save",
                onClick = {
                    viewModel.setProfile {
                        it.copy(
                            usesCustomTargets = custom,
                            customCalories = calories,
                            customProtein = protein,
                            customCarbs = carbs,
                            customFat = fat,
                            waterGoalMl = waterGoal,
                        )
                    }
                    onClose()
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CalzyCard(radius = 22.dp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Custom targets",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CalzyColors.ink,
                        )
                        Text(
                            text = "Off = calculated from your profile" +
                                " (${auto.calories} kcal)",
                            fontSize = 12.sp,
                            color = CalzyColors.inkFaint,
                        )
                    }
                    CalzyToggle(checked = custom, onChange = { custom = it })
                }
            }

            if (custom) {
                BigSlider(
                    title = "Calories",
                    value = calories.toDouble(),
                    min = 1000.0,
                    max = 5000.0,
                    format = { "${(it / 10).roundToInt() * 10}" },
                    unit = "kcal",
                    onChange = { calories = (it / 10).roundToInt() * 10 },
                )
                BigSlider(
                    title = "Protein",
                    value = protein.toDouble(),
                    min = 40.0,
                    max = 350.0,
                    format = { "${it.roundToInt()}" },
                    unit = "g",
                    onChange = { protein = it.roundToInt() },
                    tint = CalzyColors.protein,
                )
                BigSlider(
                    title = "Carbs",
                    value = carbs.toDouble(),
                    min = 40.0,
                    max = 600.0,
                    format = { "${it.roundToInt()}" },
                    unit = "g",
                    onChange = { carbs = it.roundToInt() },
                    tint = CalzyColors.carbs,
                )
                BigSlider(
                    title = "Fat",
                    value = fat.toDouble(),
                    min = 20.0,
                    max = 250.0,
                    format = { "${it.roundToInt()}" },
                    unit = "g",
                    onChange = { fat = it.roundToInt() },
                    tint = CalzyColors.fat,
                )
            } else {
                CalzyCard(radius = 22.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "YOUR CALCULATED PLAN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = CalzyColors.inkFaint,
                        )
                        MetricText(text = "${auto.calories} kcal", size = 34)
                        Text(
                            text = "${auto.protein}g protein · ${auto.carbs}g carbs · ${auto.fat}g fat",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = CalzyColors.inkSoft,
                        )
                    }
                }
            }

            BigSlider(
                title = "Water goal",
                value = waterGoal.toDouble(),
                min = 1000.0,
                max = 5000.0,
                format = { "${(it / 50).roundToInt() * 50}" },
                unit = "ml",
                onChange = { waterGoal = (it / 50).roundToInt() * 50 },
                tint = CalzyColors.water,
            )
        }
    }
}

/** Goals & weight sheet: direction, goal weight and weekly pace. */
@Composable
fun GoalsWeightSheet(open: Boolean, viewModel: AppViewModel, onClose: () -> Unit) {
    val t = LocalT.current
    val data by viewModel.data.collectAsStateWithLifecycle()

    var goal by remember { mutableStateOf(GoalDirection.lose) }
    var goalWeight by remember { mutableDoubleStateOf(74.0) }
    var rate by remember { mutableDoubleStateOf(0.5) }

    LaunchedEffect(open) {
        if (open) {
            goal = data.profile.goal
            goalWeight = data.profile.goalWeightKg
            rate = data.profile.weeklyRateKg
        }
    }

    FullScreenSheet(
        open = open,
        onClose = onClose,
        title = t("s.weight"),
        footer = {
            PrimaryButton(
                text = "Save",
                onClick = {
                    viewModel.setProfile {
                        it.copy(goal = goal, goalWeightKg = goalWeight, weeklyRateKg = rate)
                    }
                    onClose()
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Nutrition.goalOrder.forEach { option ->
                    val active = goal == option
                    Pressable(onClick = { goal = option }, modifier = Modifier.weight(1f)) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (active) {
                                        CalzyColors.ink
                                    } else {
                                        Color.White.copy(alpha = 0.78f)
                                    },
                                )
                                .padding(vertical = 16.dp),
                        ) {
                            Text(
                                text = Nutrition.goalLabels[option] ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                color = if (active) Color.White else CalzyColors.ink,
                            )
                        }
                    }
                }
            }

            if (goal != GoalDirection.maintain) {
                BigSlider(
                    title = "Goal weight",
                    value = goalWeight,
                    min = 35.0,
                    max = 200.0,
                    format = { String.format(Locale.US, "%.1f", it) },
                    unit = "kg",
                    onChange = { goalWeight = (it * 10).roundToInt() / 10.0 },
                )
                BigSlider(
                    title = "Weekly pace",
                    value = rate,
                    min = 0.1,
                    max = 1.2,
                    format = { String.format(Locale.US, "%.1f", it) },
                    unit = "kg / week",
                    onChange = { rate = (it * 10).roundToInt() / 10.0 },
                )
            }

            CalzyCard(radius = 20.dp) {
                Text(
                    text = "Current weight: ${
                        String.format(Locale.US, "%.1f", data.profile.currentWeightKg)
                    } kg — log updates from the Progress tab.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = CalzyColors.inkSoft,
                    modifier = Modifier.padding(15.dp),
                    lineHeight = 19.sp,
                )
            }
        }
    }
}

/** Activity level sheet. */
@Composable
fun ActivitySheet(open: Boolean, viewModel: AppViewModel, onClose: () -> Unit) {
    val t = LocalT.current
    val data by viewModel.data.collectAsStateWithLifecycle()

    var activity by remember { mutableStateOf(ActivityLevel.light) }

    LaunchedEffect(open) {
        if (open) activity = data.profile.activity
    }

    FullScreenSheet(
        open = open,
        onClose = onClose,
        title = t("s.activity"),
        footer = {
            PrimaryButton(
                text = "Save",
                onClick = {
                    viewModel.setProfile { it.copy(activity = activity) }
                    onClose()
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Nutrition.activityOrder.forEach { level ->
                val active = activity == level
                val meta = Nutrition.activityMeta[level] ?: return@forEach
                Pressable(onClick = { activity = level }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(13.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (active) CalzyColors.ink else Color.White.copy(alpha = 0.78f),
                            )
                            .padding(15.dp),
                    ) {
                        Icon(
                            imageVector = activityLevelIcon(level),
                            contentDescription = null,
                            tint = if (active) Color.White else CalzyColors.ink,
                            modifier = Modifier.size(18.dp),
                        )
                        Column {
                            Text(
                                text = meta.label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) Color.White else CalzyColors.ink,
                            )
                            Text(
                                text = meta.detail,
                                fontSize = 12.sp,
                                color = if (active) {
                                    Color.White.copy(alpha = 0.75f)
                                } else {
                                    CalzyColors.inkFaint
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun activityLevelIcon(level: ActivityLevel): ImageVector = when (level) {
    ActivityLevel.sedentary -> Icons.Outlined.Chair
    ActivityLevel.light -> Icons.Outlined.NordicWalking
    ActivityLevel.moderate -> Icons.Outlined.SelfImprovement
    ActivityLevel.high -> Icons.Outlined.FitnessCenter
    ActivityLevel.athlete -> Icons.Outlined.LocalFireDepartment
}
