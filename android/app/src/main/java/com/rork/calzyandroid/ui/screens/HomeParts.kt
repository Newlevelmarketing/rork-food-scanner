package com.rork.calzyandroid.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.calzyandroid.data.Dates
import com.rork.calzyandroid.data.ImageUtils
import com.rork.calzyandroid.data.MealEntry
import com.rork.calzyandroid.data.Nutrition
import com.rork.calzyandroid.ui.components.CalzyCard
import com.rork.calzyandroid.ui.components.IconMaps
import com.rork.calzyandroid.ui.components.LocalT
import com.rork.calzyandroid.ui.components.MetricText
import com.rork.calzyandroid.ui.components.Pressable
import com.rork.calzyandroid.ui.theme.CalzyColors
import java.time.LocalDate
import kotlin.math.roundToInt

/** Horizontal 21-day selector pinned under the app title, scrolled to today. */
@Composable
fun DateStrip(
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    hasLogs: (LocalDate) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val days = remember { (20 downTo 0).map { LocalDate.now().minusDays(it.toLong()) } }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { listState.scrollToItem(days.lastIndex) }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
    ) {
        items(count = days.size, key = { days[it].toEpochDay() }) { index ->
            val day = days[index]
            val active = day == selected
            val today = Dates.isToday(day)
            Pressable(onClick = { onSelect(day) }) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = Dates.shortWeekday(day),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) CalzyColors.ink else CalzyColors.inkFaint,
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 46.dp, height = 50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) Color.White else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        MetricText(
                            text = "${day.dayOfMonth}",
                            size = 19,
                            color = if (active) CalzyColors.ink else CalzyColors.inkFaint,
                        )
                        if (today && !active) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 6.dp, end = 6.dp)
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(CalzyColors.flame),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (hasLogs(day)) CalzyColors.mint else Color.Transparent),
                    )
                }
            }
        }
    }
}

/** The macro tile shown in the home carousel (protein / carbs / fat). */
@Composable
fun MacroTile(
    title: String,
    emoji: String,
    eaten: Double,
    goal: Int,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val progress = if (goal > 0) (eaten / goal).toFloat().coerceIn(0f, 1f) else 0f
    CalzyCard(modifier = modifier, radius = 22.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(modifier = Modifier.size(62.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 5.dp.toPx()
                    val inset = stroke / 2
                    val arc = androidx.compose.ui.geometry.Size(
                        size.width - stroke,
                        size.height - stroke,
                    )
                    drawArc(
                        color = tint.copy(alpha = 0.16f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                        size = arc,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = tint,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                        size = arc,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                Text(text = emoji, fontSize = 26.sp)
            }
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = CalzyColors.inkSoft,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                MetricText(text = "${eaten.roundToInt()}", size = 21)
                Text(
                    text = "/${goal}g",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = CalzyColors.inkFaint,
                )
            }
        }
    }
}

/** One cell of the compact quick-action strip. */
data class QuickAction(val icon: ImageVector, val title: String, val onClick: () -> Unit)

@Composable
fun QuickActionBar(actions: List<QuickAction>, modifier: Modifier = Modifier) {
    CalzyCard(modifier = modifier.fillMaxWidth(), radius = 22.dp) {
        Row(modifier = Modifier.fillMaxWidth()) {
            actions.forEachIndexed { index, action ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .size(width = 1.dp, height = 24.dp)
                            .background(CalzyColors.ink.copy(alpha = 0.06f)),
                    )
                }
                Pressable(onClick = action.onClick, modifier = Modifier.weight(1f)) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(vertical = 11.dp),
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.title,
                            tint = CalzyColors.ink,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = action.title,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CalzyColors.inkSoft,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroChip(text: String, tint: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

/**
 * One logged meal in the home timeline: the body opens the meal detail, the
 * trailing pencil jumps straight to quick-edit.
 */
@Composable
fun MealRow(
    meal: MealEntry,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CalzyCard(modifier = modifier.fillMaxWidth(), radius = 22.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Pressable(onClick = onClick, modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CalzyColors.well),
                        contentAlignment = Alignment.Center,
                    ) {
                        val bitmap = remember(meal.photo) {
                            meal.photo?.let { ImageUtils.dataUrlToBitmap(it) }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                imageVector = IconMaps.source(meal.source),
                                contentDescription = null,
                                tint = CalzyColors.inkFaint,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = meal.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CalzyColors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = CalzyColors.flame,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = "${Nutrition.mealCalories(meal)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CalzyColors.flame,
                            )
                            MacroChip(
                                text = "${Nutrition.mealProtein(meal).roundToInt()}P",
                                tint = CalzyColors.protein,
                            )
                            MacroChip(
                                text = "${Nutrition.mealCarbs(meal).roundToInt()}C",
                                tint = CalzyColors.carbs,
                            )
                            MacroChip(
                                text = "${Nutrition.mealFat(meal).roundToInt()}F",
                                tint = CalzyColors.fat,
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = IconMaps.slot(meal.slot),
                            contentDescription = null,
                            tint = CalzyColors.inkFaint,
                            modifier = Modifier.size(11.dp),
                        )
                        Text(
                            text = Dates.shortTime(meal.date),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = CalzyColors.inkFaint,
                        )
                    }
                }
            }

            Pressable(onClick = onEdit) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(CalzyColors.well),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit ${meal.title}",
                        tint = CalzyColors.inkSoft,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
    }
}

/** Ghosted meal row previewing what a logged meal looks like on an empty day. */
@Composable
fun EmptyMealsState(modifier: Modifier = Modifier) {
    val t = LocalT.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White.copy(alpha = 0.4f))
            .padding(horizontal = 18.dp, vertical = 22.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(76.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Text(text = "\uD83E\uDD57", fontSize = 32.sp)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(CalzyColors.ink.copy(alpha = 0.07f)),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(CalzyColors.ink.copy(alpha = 0.07f)),
                        )
                    }
                }
            }
            Text(
                text = t("h.empty"),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = CalzyColors.inkSoft,
                modifier = Modifier.padding(top = 13.dp),
            )
        }
    }
}
