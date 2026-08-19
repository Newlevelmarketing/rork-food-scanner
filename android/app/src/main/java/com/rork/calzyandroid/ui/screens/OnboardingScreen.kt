package com.rork.calzyandroid.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.NordicWalking
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.calzyandroid.AppViewModel
import com.rork.calzyandroid.data.ActivityLevel
import com.rork.calzyandroid.data.GoalDirection
import com.rork.calzyandroid.data.Nutrition
import com.rork.calzyandroid.data.Sex
import com.rork.calzyandroid.data.UserProfile
import com.rork.calzyandroid.ui.components.CalzyBackdrop
import com.rork.calzyandroid.ui.components.CalzyCard
import com.rork.calzyandroid.ui.components.CalzySlider
import com.rork.calzyandroid.ui.components.MetricText
import com.rork.calzyandroid.ui.components.Pressable
import com.rork.calzyandroid.ui.components.PrimaryButton
import com.rork.calzyandroid.ui.components.SegmentedControl
import com.rork.calzyandroid.ui.theme.CalzyColors
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TOTAL_STEPS = 6

/** First-run flow that builds the user's profile and calculates their daily plan. */
@Composable
fun OnboardingScreen(viewModel: AppViewModel) {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf(Sex.male) }
    var birthYear by remember { mutableIntStateOf(1996) }
    var height by remember { mutableDoubleStateOf(176.0) }
    var weight by remember { mutableDoubleStateOf(80.0) }
    var goal by remember { mutableStateOf(GoalDirection.lose) }
    var goalWeight by remember { mutableDoubleStateOf(74.0) }
    var rate by remember { mutableDoubleStateOf(0.5) }
    var activity by remember { mutableStateOf(ActivityLevel.light) }

    val draft = UserProfile(
        name = name,
        sex = sex,
        birthYear = birthYear,
        heightCm = height,
        startWeightKg = weight,
        currentWeightKg = weight,
        goalWeightKg = goalWeight,
        goal = goal,
        weeklyRateKg = rate,
        activity = activity,
    )

    CalzyBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues()),
        ) {
            // Step progress
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(TOTAL_STEPS) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (index <= step) {
                                    CalzyColors.ink
                                } else {
                                    CalzyColors.inkFaint.copy(alpha = 0.25f)
                                },
                            ),
                    )
                }
            }

            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (slideInVertically(tween(320)) { it / 8 } + fadeIn(tween(320)))
                        .togetherWith(fadeOut(tween(150)))
                },
                label = "onboardingStep",
            ) { current ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    when (current) {
                        0 -> WelcomeStep()
                        1 -> StepShell(
                            title = "First, the basics",
                            subtitle = "We use these to calculate your daily energy needs.",
                        ) {
                            CalzyCard(radius = 22.dp) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "What should we call you?",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = CalzyColors.inkSoft,
                                        modifier = Modifier.padding(bottom = 8.dp),
                                    )
                                    TextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        placeholder = {
                                            Text("Your name", color = CalzyColors.inkFaint)
                                        },
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
                                        keyboardOptions = KeyboardOptions.Default,
                                    )
                                }
                            }

                            CalzyCard(radius = 22.dp) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Sex",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
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

                        2 -> StepShell(
                            title = "Your body today",
                            subtitle = "Be honest — this only ever lives on your device.",
                        ) {
                            BigSlider(
                                title = "Height",
                                value = height,
                                min = 130.0,
                                max = 220.0,
                                format = { "${it.roundToInt()}" },
                                unit = "cm",
                                onChange = { height = it },
                            )
                            BigSlider(
                                title = "Weight",
                                value = weight,
                                min = 35.0,
                                max = 200.0,
                                format = { String.format(Locale.US, "%.1f", it) },
                                unit = "kg",
                                onChange = { weight = it },
                            )
                            BmiPill(heightCm = height, weightKg = weight)
                        }

                        3 -> StepShell(
                            title = "What's the mission?",
                            subtitle = "You can change this at any time.",
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                Nutrition.goalOrder.forEach { option ->
                                    val active = goal == option
                                    Pressable(
                                        onClick = {
                                            goal = option
                                            goalWeight = when (option) {
                                                GoalDirection.lose -> maxOf(40.0, weight - 6)
                                                GoalDirection.gain -> weight + 5
                                                GoalDirection.maintain -> weight
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                                .padding(vertical = 18.dp),
                                        ) {
                                            Icon(
                                                imageVector = goalIcon(option),
                                                contentDescription = null,
                                                tint = if (active) Color.White else CalzyColors.ink,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Text(
                                                text = Nutrition.goalLabels[option] ?: "",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center,
                                                color = if (active) Color.White else CalzyColors.ink,
                                                modifier = Modifier.padding(horizontal = 4.dp),
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
                                    onChange = { goalWeight = it },
                                )
                                BigSlider(
                                    title = "Weekly pace",
                                    value = rate,
                                    min = 0.1,
                                    max = 1.2,
                                    format = { String.format(Locale.US, "%.1f", it) },
                                    unit = "kg / week",
                                    onChange = { rate = it },
                                )
                            }
                        }

                        4 -> StepShell(
                            title = "How active are you?",
                            subtitle = "Outside of what you'll log as exercise.",
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
                                                if (active) {
                                                    CalzyColors.ink
                                                } else {
                                                    Color.White.copy(alpha = 0.78f)
                                                },
                                            )
                                            .padding(15.dp),
                                    ) {
                                        Icon(
                                            imageVector = activityIcon(level),
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

                        else -> PlanStep(
                            draft = draft,
                            goal = goal,
                            weight = weight,
                            goalWeight = goalWeight,
                            rate = rate,
                        )
                    }
                }
            }

            // Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(top = 8.dp)
                    .padding(WindowInsets.navigationBars.asPaddingValues()),
            ) {
                PrimaryButton(
                    text = if (step == TOTAL_STEPS - 1) "Start tracking" else "Continue",
                    onClick = {
                        if (step == TOTAL_STEPS - 1) {
                            viewModel.completeOnboarding(draft)
                        } else {
                            step += 1
                        }
                    },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .padding(top = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (step > 0) {
                        Pressable(onClick = { step -= 1 }) {
                            Text(
                                text = "Back",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = CalzyColors.inkFaint,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun goalIcon(goal: GoalDirection): ImageVector = when (goal) {
    GoalDirection.lose -> Icons.AutoMirrored.Outlined.TrendingDown
    GoalDirection.maintain -> Icons.Outlined.DragHandle
    GoalDirection.gain -> Icons.AutoMirrored.Outlined.TrendingUp
}

private fun activityIcon(level: ActivityLevel): ImageVector = when (level) {
    ActivityLevel.sedentary -> Icons.Outlined.Chair
    ActivityLevel.light -> Icons.Outlined.NordicWalking
    ActivityLevel.moderate -> Icons.Outlined.SelfImprovement
    ActivityLevel.high -> Icons.Outlined.FitnessCenter
    ActivityLevel.athlete -> Icons.Outlined.LocalFireDepartment
}

@Composable
private fun StepShell(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = 36.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = CalzyColors.ink,
                lineHeight = 36.sp,
            )
            Text(text = subtitle, fontSize = 16.sp, color = CalzyColors.inkSoft)
        }
        content()
    }
}

@Composable
private fun WelcomeStep() {
    val transition = rememberInfiniteTransition(label = "breathe")
    val scale by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breatheScale",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Box(modifier = Modifier.size(260.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                CalzyColors.flame.copy(alpha = 0.28f),
                                Color.Transparent,
                            ),
                        ),
                        shape = CircleShape,
                    ),
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(CalzyColors.ink),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalFireDepartment,
                    contentDescription = "ModernBody",
                    tint = CalzyColors.flame,
                    modifier = Modifier.size(52.dp),
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricText(text = "ModernBody", size = 40)
            Text(
                text = "Point your camera at any meal.\nWe'll do the counting.",
                fontSize = 17.sp,
                color = CalzyColors.inkSoft,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
            )
        }
    }
}

@Composable
private fun PlanStep(
    draft: UserProfile,
    goal: GoalDirection,
    weight: Double,
    goalWeight: Double,
    rate: Double,
) {
    val targets = Nutrition.targetsOf(draft)
    val eta = remember(goalWeight, weight, rate) {
        val weeks = Nutrition.etaWeeks(weight, goalWeight, rate)
        val date = LocalDate.now().plusDays((weeks * 7).roundToInt().toLong())
        "Reach ${String.format(Locale.US, "%.1f", goalWeight)} kg around " +
            com.rork.calzyandroid.data.Dates.longMonthDay(date)
    }

    StepShell(
        title = "Your daily plan",
        subtitle = "Built from your body, goal and activity level.",
    ) {
        CalzyCard(radius = 26.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "DAILY CALORIES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = CalzyColors.inkFaint,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    MetricText(text = "${targets.calories}", size = 56)
                    Text(
                        text = " kcal",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = CalzyColors.inkFaint,
                    )
                }
                Text(
                    text = "Maintenance is about ${Nutrition.maintenanceOf(draft).roundToInt()} kcal",
                    fontSize = 13.sp,
                    color = CalzyColors.inkSoft,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PlanTile(
                title = "Protein",
                value = "${targets.protein}g",
                tint = CalzyColors.protein,
                emoji = "\uD83C\uDF57",
                modifier = Modifier.weight(1f),
            )
            PlanTile(
                title = "Carbs",
                value = "${targets.carbs}g",
                tint = CalzyColors.carbs,
                emoji = "\uD83C\uDF5E",
                modifier = Modifier.weight(1f),
            )
            PlanTile(
                title = "Fat",
                value = "${targets.fat}g",
                tint = CalzyColors.fat,
                emoji = "\uD83E\uDD51",
                modifier = Modifier.weight(1f),
            )
        }

        if (goal != GoalDirection.maintain && abs(goalWeight - weight) > 0.1) {
            CalzyCard(radius = 20.dp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier.padding(15.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = CalzyColors.mint,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = eta,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = CalzyColors.inkSoft,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanTile(
    title: String,
    value: String,
    tint: Color,
    emoji: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(tint.copy(alpha = 0.12f))
            .padding(vertical = 16.dp),
    ) {
        Text(text = emoji, fontSize = 24.sp)
        MetricText(text = value, size = 19)
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = CalzyColors.inkSoft,
        )
    }
}

@Composable
private fun BmiPill(heightCm: Double, weightKg: Double) {
    val bmi = Nutrition.bmiOf(heightCm, weightKg)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(CalzyColors.well)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = "BMI",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = CalzyColors.inkSoft,
        )
        MetricText(text = String.format(Locale.US, "%.1f", bmi), size = 17)
        Text(
            text = Nutrition.bmiCategory(bmi).label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = CalzyColors.inkSoft,
        )
    }
}

/** Slider card with a live metric readout — shared with settings sheets. */
@Composable
fun BigSlider(
    title: String,
    value: Double,
    min: Double,
    max: Double,
    format: (Double) -> String,
    unit: String,
    onChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = CalzyColors.ink,
) {
    CalzyCard(modifier = modifier, radius = 22.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = CalzyColors.inkSoft,
                    modifier = Modifier.weight(1f),
                )
                MetricText(text = format(value), size = 22)
                if (unit.isNotEmpty()) {
                    Text(
                        text = " $unit",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = CalzyColors.inkFaint,
                    )
                }
            }
            CalzySlider(
                value = value.toFloat(),
                onChange = { onChange(it.toDouble()) },
                range = min.toFloat()..max.toFloat(),
                tint = tint,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}
