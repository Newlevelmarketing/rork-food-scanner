package com.rork.calzyandroid.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rork.calzyandroid.AppViewModel
import com.rork.calzyandroid.data.Dates
import com.rork.calzyandroid.data.GoalDirection
import com.rork.calzyandroid.data.ImageUtils
import com.rork.calzyandroid.data.Nutrition
import com.rork.calzyandroid.data.WeightEntry
import com.rork.calzyandroid.data.averageCalories
import com.rork.calzyandroid.data.photosSorted
import com.rork.calzyandroid.data.streak
import com.rork.calzyandroid.data.weightEntriesSorted
import com.rork.calzyandroid.data.weightOn
import com.rork.calzyandroid.ui.components.CalzyBottomSheet
import com.rork.calzyandroid.ui.components.CalzyCard
import com.rork.calzyandroid.ui.components.CalzySlider
import com.rork.calzyandroid.ui.components.MetricText
import com.rork.calzyandroid.ui.components.Pressable
import com.rork.calzyandroid.ui.components.PrimaryButton
import com.rork.calzyandroid.ui.components.SegmentedControl
import com.rork.calzyandroid.ui.components.VHairline
import com.rork.calzyandroid.ui.theme.CalzyColors
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

private enum class TrendRange { week, month }

/** Progress tab: weight, BMI, streaks, weight journal, trend chart and photos. */
@Composable
fun ProgressScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val data by viewModel.data.collectAsStateWithLifecycle()
    val profile = data.profile

    var weightSheetDate by remember { mutableStateOf<LocalDate?>(null) }
    var showBodyMetrics by remember { mutableStateOf(false) }
    var journalMonth by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var trendRange by remember { mutableStateOf(TrendRange.week) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            ImageUtils.uriToDataUrl(context, uri, maxDimension = 620)?.let {
                viewModel.addProgressPhoto(it)
            }
        }
    }

    val weightDelta = profile.currentWeightKg - profile.startWeightKg
    val deltaColor = when {
        profile.goal == GoalDirection.maintain -> CalzyColors.inkSoft
        (if (profile.goal == GoalDirection.lose) weightDelta <= 0 else weightDelta >= 0) ->
            CalzyColors.mint
        else -> CalzyColors.flame
    }

    val bmi = Nutrition.bmiOf(profile.heightCm, profile.currentWeightKg)
    val category = Nutrition.bmiCategory(bmi)
    val bmiPosition = (((bmi - 15) / 25).coerceIn(0.0, 1.0)).toFloat()
    val categoryColor = when (category) {
        Nutrition.BmiCategory.Underweight -> CalzyColors.carbs
        Nutrition.BmiCategory.Healthy -> CalzyColors.mint
        Nutrition.BmiCategory.Overweight -> CalzyColors.fat
        Nutrition.BmiCategory.Obese -> CalzyColors.protein
    }

    val weightEntries = data.weightEntriesSorted()
    val trendEntries = remember(weightEntries, trendRange) {
        val cutoff = LocalDate.now().minusDays(if (trendRange == TrendRange.week) 7 else 30)
        val filtered = weightEntries.filter { !Dates.localDate(it.date).isBefore(cutoff) }
        filtered.ifEmpty { weightEntries.takeLast(2) }
    }
    val values = trendEntries.map { it.kilograms }
    val change = (values.lastOrNull() ?: 0.0) - (values.firstOrNull() ?: 0.0)

    val avg = data.averageCalories()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(bottom = 120.dp),
    ) {
        Text(
            text = "Progress",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = CalzyColors.ink,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )

        // Weight + BMI cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CalzyCard(modifier = Modifier.weight(1f), radius = 22.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Weight",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CalzyColors.inkSoft,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        MetricText(
                            text = String.format(Locale.US, "%.1f", profile.currentWeightKg),
                            size = 32,
                        )
                        Text(
                            text = " kg",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = CalzyColors.inkFaint,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (weightDelta >= 0) {
                                Icons.Outlined.KeyboardArrowUp
                            } else {
                                Icons.Outlined.KeyboardArrowDown
                            },
                            contentDescription = null,
                            tint = deltaColor,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "${if (weightDelta >= 0) "+" else ""}${
                                String.format(Locale.US, "%.1f", weightDelta)
                            }",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = deltaColor,
                        )
                    }
                    Pressable(onClick = { weightSheetDate = LocalDate.now() }) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CalzyColors.ink),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Log today's weight",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            CalzyCard(modifier = Modifier.weight(1f), radius = 22.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "BMI",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CalzyColors.inkSoft,
                            modifier = Modifier.weight(1f),
                        )
                        Pressable(onClick = { showBodyMetrics = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit body metrics",
                                tint = CalzyColors.inkFaint,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }
                    MetricText(text = String.format(Locale.US, "%.1f", bmi), size = 32)
                    Text(
                        text = category.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = categoryColor,
                    )
                    // BMI gradient scale
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(13.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            CalzyColors.carbs,
                                            CalzyColors.mint,
                                            CalzyColors.fat,
                                            CalzyColors.protein,
                                        ),
                                    ),
                                ),
                        )
                        androidx.compose.foundation.layout.BoxWithConstraints(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(start = (maxWidth - 13.dp) * bmiPosition)
                                    .size(13.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(0.5.dp, Color.Black.copy(alpha = 0.15f), CircleShape),
                            )
                        }
                    }
                    Text(
                        text = "Source:\nWHO BMI Classification",
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        color = CalzyColors.inkFaint,
                    )
                }
            }
        }

        // Streak + average calories
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(
                icon = Icons.Outlined.LocalFireDepartment,
                iconTint = CalzyColors.flame,
                title = "Day Streak",
                value = "${data.streak()}",
                unit = "days",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = Icons.Outlined.Bolt,
                iconTint = CalzyColors.fat,
                title = "Avg Calories",
                value = "${avg.average}",
                unit = null,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "${avg.logged}/7 days logged",
                    fontSize = 12.sp,
                    color = CalzyColors.inkFaint,
                )
            }
        }

        // Weight journal
        CalzyCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp),
            radius = 26.dp,
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                val monthStart = journalMonth
                val totalDays = monthStart.lengthOfMonth()
                val leadingBlanks = monthStart.dayOfWeek.value % 7 // Sunday-first grid.
                val loggedInMonth = weightEntries.count {
                    val day = Dates.localDate(it.date)
                    day.year == monthStart.year && day.month == monthStart.month
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Weight Journal",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = CalzyColors.ink,
                        )
                        Text(
                            text = "$loggedInMonth/$totalDays days logged",
                            fontSize = 13.sp,
                            color = CalzyColors.inkFaint,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MonthButton(label = "‹", description = "Previous month") {
                            journalMonth = journalMonth.minusMonths(1)
                        }
                        Text(
                            text = Dates.monthYear(monthStart),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CalzyColors.ink,
                            modifier = Modifier.width(74.dp),
                            textAlign = TextAlign.Center,
                        )
                        MonthButton(label = "›", description = "Next month") {
                            journalMonth = journalMonth.plusMonths(1)
                        }
                    }
                }

                // Weekday headers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Dates.weekdayInitials().forEach { symbol ->
                        Text(
                            text = symbol,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = CalzyColors.inkFaint,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Day grid
                val totalCells = leadingBlanks + totalDays
                val rows = (totalCells + 6) / 7
                Column(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    repeat(rows) { rowIndex ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(7) { columnIndex ->
                                val cell = rowIndex * 7 + columnIndex
                                val dayNumber = cell - leadingBlanks + 1
                                if (dayNumber in 1..totalDays) {
                                    val day = monthStart.withDayOfMonth(dayNumber)
                                    val entry = data.weightOn(day)
                                    val future = day.isAfter(LocalDate.now())
                                    JournalCell(
                                        dayNumber = dayNumber,
                                        entry = entry,
                                        isToday = Dates.isToday(day),
                                        future = future,
                                        onClick = { if (!future) weightSheetDate = day },
                                        modifier = Modifier.weight(1f),
                                    )
                                } else {
                                    Box(modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Weight trend
        CalzyCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp),
            radius = 26.dp,
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Weight Trend",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = CalzyColors.ink,
                        )
                        if (trendEntries.size > 1) {
                            val trendTint = if (change >= 0) CalzyColors.flame else CalzyColors.mint
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (change >= 0) {
                                        Icons.Outlined.KeyboardArrowUp
                                    } else {
                                        Icons.Outlined.KeyboardArrowDown
                                    },
                                    contentDescription = null,
                                    tint = trendTint,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = "${if (change >= 0) "+" else ""}${
                                        String.format(Locale.US, "%.1f", change)
                                    } kg",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = trendTint,
                                )
                                Text(
                                    text = " this ${trendRange.name}",
                                    fontSize = 13.sp,
                                    color = CalzyColors.inkFaint,
                                )
                            }
                        }
                    }
                    SegmentedControl(
                        options = listOf(TrendRange.week to "Week", TrendRange.month to "Month"),
                        selected = trendRange,
                        onSelect = { trendRange = it },
                        modifier = Modifier.width(150.dp),
                    )
                }

                if (trendEntries.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CalzyColors.well.copy(alpha = 0.7f))
                            .padding(vertical = 10.dp),
                    ) {
                        TrendStat(
                            icon = Icons.AutoMirrored.Outlined.List,
                            title = "Entries",
                            value = "${trendEntries.size}",
                            modifier = Modifier.weight(1f),
                        )
                        VHairline()
                        TrendStat(
                            icon = Icons.Outlined.KeyboardArrowDown,
                            title = "Lowest",
                            value = String.format(Locale.US, "%.1f", values.min()),
                            modifier = Modifier.weight(1f),
                        )
                        VHairline()
                        TrendStat(
                            icon = Icons.Outlined.KeyboardArrowUp,
                            title = "Highest",
                            value = String.format(Locale.US, "%.1f", values.max()),
                            modifier = Modifier.weight(1f),
                        )
                        VHairline()
                        TrendStat(
                            icon = Icons.Outlined.DragHandle,
                            title = "Average",
                            value = String.format(Locale.US, "%.1f", values.average()),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    WeightTrendChart(
                        entries = trendEntries,
                        goalWeight = profile.goalWeightKg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .padding(top = 16.dp),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ShowChart,
                            contentDescription = null,
                            tint = CalzyColors.inkFaint,
                            modifier = Modifier.size(26.dp),
                        )
                        Text(
                            text = "Log your weight twice to see a trend",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = CalzyColors.inkSoft,
                        )
                    }
                }
            }
        }

        // Progress photos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Progress Photos",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = CalzyColors.ink,
                modifier = Modifier.weight(1f),
            )
            Pressable(onClick = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = CalzyColors.ink,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = " Add",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CalzyColors.ink,
                    )
                }
            }
        }

        val photos = data.photosSorted()
        if (photos.isEmpty()) {
            CalzyCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                radius = 26.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        tint = CalzyColors.inkFaint,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = "Track your transformation",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CalzyColors.inkSoft,
                    )
                    Text(
                        text = "Add photos to see your progress over time",
                        fontSize = 13.sp,
                        color = CalzyColors.inkFaint,
                    )
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
            ) {
                items(count = photos.size, key = { photos[it].id }) { index ->
                    val photo = photos[index]
                    Box(
                        modifier = Modifier
                            .size(width = 140.dp, height = 190.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(CalzyColors.well),
                    ) {
                        val bitmap = remember(photo.photo) {
                            ImageUtils.dataUrlToBitmap(photo.photo)
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = Dates.monthDay(photo.date),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            photo.weightKg?.let {
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", it)} kg",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.85f),
                                )
                            }
                        }
                        Pressable(
                            onClick = { viewModel.deletePhoto(photo.id) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Weight logging sheet
    weightSheetDate?.let { sheetDate ->
        WeightSheet(
            date = sheetDate,
            initial = data.weightOn(sheetDate)?.kilograms ?: profile.currentWeightKg,
            onSave = { kg ->
                viewModel.logWeight(kg, sheetDate)
                weightSheetDate = null
            },
            onClose = { weightSheetDate = null },
        )
    }

    // Body metrics sheet
    if (showBodyMetrics) {
        BodyMetricsSheet(
            initialHeight = profile.heightCm,
            initialWeight = profile.currentWeightKg,
            onSave = { height, weight ->
                viewModel.setProfile { it.copy(heightCm = height, currentWeightKg = weight) }
                viewModel.logWeight(weight)
                showBodyMetrics = false
            },
            onClose = { showBodyMetrics = false },
        )
    }
}

@Composable
private fun JournalCell(
    dayNumber: Int,
    entry: WeightEntry?,
    isToday: Boolean,
    future: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Pressable(onClick = onClick, enabled = !future, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (entry != null) CalzyColors.ink else Color.Transparent)
                .then(
                    if (isToday) {
                        Modifier.border(1.5.dp, CalzyColors.ink, RoundedCornerShape(11.dp))
                    } else {
                        Modifier
                    },
                ),
        ) {
            Text(
                text = "$dayNumber",
                fontSize = 12.sp,
                fontWeight = if (entry != null) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    entry != null -> Color.White
                    future -> CalzyColors.inkFaint.copy(alpha = 0.4f)
                    else -> CalzyColors.inkSoft
                },
                lineHeight = 13.sp,
            )
            if (entry != null) {
                Text(
                    text = "${entry.kilograms.roundToInt()}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 10.sp,
                )
            } else if (!future) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = CalzyColors.inkFaint.copy(alpha = 0.6f),
                    modifier = Modifier.size(8.dp),
                )
            }
        }
    }
}

/** Custom-drawn weight line chart with area fill and dashed goal line. */
@Composable
private fun WeightTrendChart(
    entries: List<WeightEntry>,
    goalWeight: Double,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (entries.size < 2) return@Canvas
        val kgs = entries.map { it.kilograms }
        val minValue = minOf(kgs.min(), goalWeight) - 0.6
        val maxValue = maxOf(kgs.max(), goalWeight) + 0.6
        val span = (maxValue - minValue).coerceAtLeast(0.1)

        val times = entries.map { Dates.parse(it.date).toEpochMilli().toDouble() }
        val minTime = times.min()
        val timeSpan = (times.max() - minTime).coerceAtLeast(1.0)

        fun x(time: Double): Float = ((time - minTime) / timeSpan * size.width).toFloat()
        fun y(kg: Double): Float = (size.height * (1 - (kg - minValue) / span)).toFloat()

        // Grid lines
        val grid = Color.Black.copy(alpha = 0.06f)
        repeat(4) { index ->
            val gy = size.height * index / 3f
            drawLine(grid, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
        }

        // Goal reference line
        val goalY = y(goalWeight)
        drawLine(
            color = CalzyColors.mint.copy(alpha = 0.7f),
            start = Offset(0f, goalY),
            end = Offset(size.width, goalY),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
        )

        // Area fill
        val area = Path().apply {
            moveTo(x(times.first()), size.height)
            entries.forEachIndexed { index, entry ->
                lineTo(x(times[index]), y(entry.kilograms))
            }
            lineTo(x(times.last()), size.height)
            close()
        }
        drawPath(
            path = area,
            brush = Brush.verticalGradient(
                colors = listOf(
                    CalzyColors.ink.copy(alpha = 0.16f),
                    CalzyColors.ink.copy(alpha = 0.01f),
                ),
            ),
        )

        // Line
        val line = Path().apply {
            entries.forEachIndexed { index, entry ->
                val px = x(times[index])
                val py = y(entry.kilograms)
                if (index == 0) moveTo(px, py) else lineTo(px, py)
            }
        }
        drawPath(
            path = line,
            color = CalzyColors.ink,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
        )

        // Dots
        entries.forEachIndexed { index, entry ->
            drawCircle(
                color = CalzyColors.ink,
                radius = 3.dp.toPx(),
                center = Offset(x(times[index]), y(entry.kilograms)),
            )
        }
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    unit: String?,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null,
) {
    CalzyCard(modifier = modifier, radius = 22.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CalzyColors.inkSoft,
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                MetricText(text = value, size = 30)
                if (unit != null) {
                    Text(
                        text = " $unit",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = CalzyColors.inkFaint,
                    )
                }
            }
            content?.invoke()
        }
    }
}

@Composable
private fun TrendStat(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CalzyColors.inkFaint,
                modifier = Modifier.size(10.dp),
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = CalzyColors.inkFaint,
            )
        }
        MetricText(text = value, size = 16)
    }
}

@Composable
private fun MonthButton(label: String, description: String, onClick: () -> Unit) {
    Pressable(onClick = onClick) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(CalzyColors.well)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CalzyColors.ink,
            )
        }
    }
}

@Composable
private fun WeightSheet(
    date: LocalDate,
    initial: Double,
    onSave: (Double) -> Unit,
    onClose: () -> Unit,
) {
    var value by remember(date) { mutableDoubleStateOf(initial) }

    CalzyBottomSheet(open = true, onClose = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (Dates.isToday(date)) {
                        "Today's weight"
                    } else {
                        Dates.abbreviatedDate(date)
                    },
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = CalzyColors.ink,
                )
                Text(
                    text = "Small daily swings are normal — trends matter.",
                    fontSize = 13.sp,
                    color = CalzyColors.inkFaint,
                )
            }

            Row(verticalAlignment = Alignment.Bottom) {
                MetricText(text = String.format(Locale.US, "%.1f", value), size = 52)
                Text(
                    text = " kg",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = CalzyColors.inkFaint,
                )
            }

            CalzySlider(
                value = value.toFloat(),
                onChange = { value = (it * 10).roundToInt() / 10.0 },
                range = 35f..200f,
                modifier = Modifier.fillMaxWidth(),
            )

            PrimaryButton(text = "Save weight", onClick = { onSave(value) })
        }
    }
}

@Composable
private fun BodyMetricsSheet(
    initialHeight: Double,
    initialWeight: Double,
    onSave: (Double, Double) -> Unit,
    onClose: () -> Unit,
) {
    var height by remember { mutableDoubleStateOf(initialHeight) }
    var weight by remember { mutableDoubleStateOf(initialWeight) }
    val bmi = Nutrition.bmiOf(height, weight)

    CalzyBottomSheet(open = true, onClose = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Body metrics",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = CalzyColors.ink,
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "Height",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CalzyColors.ink,
                        modifier = Modifier.weight(1f),
                    )
                    MetricText(
                        text = "${height.roundToInt()} cm",
                        size = 15,
                        color = CalzyColors.inkSoft,
                    )
                }
                CalzySlider(
                    value = height.toFloat(),
                    onChange = { height = it.toDouble() },
                    range = 130f..220f,
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "Weight",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CalzyColors.ink,
                        modifier = Modifier.weight(1f),
                    )
                    MetricText(
                        text = "${String.format(Locale.US, "%.1f", weight)} kg",
                        size = 15,
                        color = CalzyColors.inkSoft,
                    )
                }
                CalzySlider(
                    value = weight.toFloat(),
                    onChange = { weight = (it * 10).roundToInt() / 10.0 },
                    range = 35f..200f,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CalzyColors.well)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
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

            PrimaryButton(text = "Save", onClick = { onSave(height, weight) })
        }
    }
}
