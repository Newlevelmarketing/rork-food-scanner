package com.rork.calzyandroid.ui.sheets

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.TheaterComedy
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rork.calzyandroid.AppViewModel
import com.rork.calzyandroid.data.Dates
import com.rork.calzyandroid.data.ImageUtils
import com.rork.calzyandroid.data.MealEntry
import com.rork.calzyandroid.data.Nutrition
import com.rork.calzyandroid.data.isSaved
import com.rork.calzyandroid.ui.components.CalzyCard
import com.rork.calzyandroid.ui.components.FullScreenSheet
import com.rork.calzyandroid.ui.components.Hairline
import com.rork.calzyandroid.ui.components.MetricText
import com.rork.calzyandroid.ui.components.Pressable
import com.rork.calzyandroid.ui.components.PrimaryButton
import com.rork.calzyandroid.ui.theme.CalzyColors
import java.util.Locale
import kotlin.math.roundToInt

/** Detail sheet for an already-logged meal: retune portions, slot or delete it. */
@Composable
fun MealDetailSheet(
    meal: MealEntry?,
    viewModel: AppViewModel,
    onClose: () -> Unit,
    onEdit: (MealEntry) -> Unit,
) {
    val data by viewModel.data.collectAsStateWithLifecycle()

    var portions by remember { mutableDoubleStateOf(1.0) }
    var slot by remember { mutableStateOf(Nutrition.currentSlot()) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(meal?.id) {
        if (meal != null) {
            portions = meal.portions
            slot = meal.slot
            confirmDelete = false
        }
    }

    val preview = meal?.copy(portions = portions, slot = slot)
    val targets = Nutrition.targetsOf(data.profile)
    val score = meal?.healthScore ?: 0
    val scoreTint = when {
        score >= 8 -> CalzyColors.mint
        score >= 5 -> CalzyColors.fat
        else -> CalzyColors.protein
    }
    val dirty = meal != null && (portions != meal.portions || slot != meal.slot)
    val saved = meal != null && data.isSaved(meal.title)

    FullScreenSheet(
        open = meal != null,
        onClose = onClose,
        title = Nutrition.slotLabels[slot] ?: "",
        trailing = {
            if (meal != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Pressable(onClick = { onEdit(meal) }) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit meal name or calories",
                            tint = CalzyColors.ink,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Pressable(onClick = { viewModel.toggleSaved(meal.title, meal.items, slot) }) {
                        Icon(
                            imageVector = if (saved) {
                                Icons.Filled.Bookmark
                            } else {
                                Icons.Outlined.BookmarkBorder
                            },
                            contentDescription = if (saved) "Remove bookmark" else "Bookmark meal",
                            tint = CalzyColors.ink,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }
        },
        footer = {
            if (meal == null) return@FullScreenSheet
            when {
                confirmDelete -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Pressable(onClick = { confirmDelete = false }, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .background(CalzyColors.ink.copy(alpha = 0.06f))
                                .padding(vertical = 15.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Keep it",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CalzyColors.ink,
                            )
                        }
                    }
                    Pressable(
                        onClick = {
                            viewModel.deleteMeal(meal.id)
                            onClose()
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
                                text = "Delete meal",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                        }
                    }
                }
                dirty -> PrimaryButton(
                    text = "Save changes",
                    onClick = {
                        viewModel.updateMeal(meal.copy(portions = portions, slot = slot))
                        onClose()
                    },
                )
                else -> Pressable(onClick = { confirmDelete = true }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(CalzyColors.protein.copy(alpha = 0.1f))
                            .padding(vertical = 15.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = CalzyColors.protein,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Delete meal",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CalzyColors.protein,
                        )
                    }
                }
            }
        },
    ) {
        if (meal == null || preview == null) return@FullScreenSheet
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            meal.photo?.let { photo ->
                val bitmap = remember(photo) { ImageUtils.dataUrlToBitmap(photo) }
                if (bitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(CalzyColors.well),
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            CalzyCard(radius = 26.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = meal.title,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = CalzyColors.ink,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Logged at ${Dates.shortTime(meal.date)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = CalzyColors.inkFaint,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        MetricText(text = "${Nutrition.mealCalories(preview)}", size = 46)
                        Text(
                            text = " kcal",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = CalzyColors.inkFaint,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(scoreTint.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Eco,
                            contentDescription = null,
                            tint = scoreTint,
                            modifier = Modifier.size(11.dp),
                        )
                        Text(
                            text = "Health score $score/10",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = scoreTint,
                        )
                    }
                }
            }

            CalzyCard(radius = 22.dp) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    MacroLine(
                        name = "Protein",
                        value = Nutrition.mealProtein(preview),
                        goal = targets.protein,
                        tint = CalzyColors.protein,
                    )
                    MacroLine(
                        name = "Carbs",
                        value = Nutrition.mealCarbs(preview),
                        goal = targets.carbs,
                        tint = CalzyColors.carbs,
                    )
                    MacroLine(
                        name = "Fat",
                        value = Nutrition.mealFat(preview),
                        goal = targets.fat,
                        tint = CalzyColors.fat,
                    )
                }
            }

            CalzyCard(radius = 22.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Portions",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CalzyColors.ink,
                            modifier = Modifier.weight(1f),
                        )
                        StepCircle(label = "Decrease portions", icon = Icons.Outlined.Remove) {
                            portions = maxOf(0.25, ((portions - 0.25) * 100).roundToInt() / 100.0)
                        }
                        MetricText(
                            text = formatPortions(portions),
                            size = 19,
                            modifier = Modifier.width(56.dp),
                            align = TextAlign.Center,
                        )
                        StepCircle(label = "Increase portions", icon = Icons.Outlined.Add) {
                            portions = minOf(10.0, ((portions + 0.25) * 100).roundToInt() / 100.0)
                        }
                    }

                    Hairline(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Meal",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CalzyColors.ink,
                            modifier = Modifier.weight(1f),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Nutrition.slotOrder.forEach { option ->
                                SlotChip(
                                    label = Nutrition.slotLabels[option] ?: "",
                                    active = slot == option,
                                    onClick = { slot = option },
                                )
                            }
                        }
                    }
                }
            }

            CalzyCard(radius = 22.dp) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    meal.items.forEachIndexed { index, item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CalzyColors.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = item.quantity,
                                    fontSize = 12.sp,
                                    color = CalzyColors.inkFaint,
                                )
                            }
                            MetricText(
                                text = "${(item.calories * portions).roundToInt()}",
                                size = 16,
                                color = CalzyColors.inkSoft,
                            )
                        }
                        if (index < meal.items.size - 1) {
                            Hairline(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            meal.quip?.let { quip ->
                CalzyCard(radius = 20.dp) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(14.dp),
                    ) {
                        Icon(
                            imageVector = if (data.profile.jesterMode) {
                                Icons.Outlined.TheaterComedy
                            } else {
                                Icons.Outlined.AutoAwesome
                            },
                            contentDescription = null,
                            tint = if (data.profile.jesterMode) {
                                CalzyColors.fat
                            } else {
                                CalzyColors.plum
                            },
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(15.dp),
                        )
                        Text(
                            text = quip,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = CalzyColors.inkSoft,
                            lineHeight = 20.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun formatPortions(portions: Double): String =
    if (portions % 1.0 == 0.0) {
        "${portions.toInt()}"
    } else {
        String.format(Locale.US, "%.2f", portions).trimEnd('0')
    }

@Composable
private fun MacroLine(name: String, value: Double, goal: Int, tint: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                text = name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = CalzyColors.inkSoft,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${value.roundToInt()}g",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CalzyColors.ink,
            )
            Text(
                text = " of ${goal}g daily",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = CalzyColors.inkFaint,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.14f)),
        ) {
            val ratio = if (goal > 0) (value / goal).toFloat().coerceIn(0f, 1f) else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(tint),
            )
        }
    }
}

@Composable
private fun StepCircle(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Pressable(onClick = onClick) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(CalzyColors.well),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = CalzyColors.ink,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
