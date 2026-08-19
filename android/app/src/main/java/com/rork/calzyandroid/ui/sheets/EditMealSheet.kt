package com.rork.calzyandroid.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.calzyandroid.AppViewModel
import com.rork.calzyandroid.data.MealEntry
import com.rork.calzyandroid.data.Nutrition
import com.rork.calzyandroid.ui.components.CalzyCard
import com.rork.calzyandroid.ui.components.FullScreenSheet
import com.rork.calzyandroid.ui.components.MetricText
import com.rork.calzyandroid.ui.components.Pressable
import com.rork.calzyandroid.ui.components.PrimaryButton
import com.rork.calzyandroid.ui.theme.CalzyColors
import com.rork.calzyandroid.ui.theme.MetricFontFamily

private const val MAX_CALORIES = 20000

/**
 * Quick-correction form for an already-logged meal: rename it or fix its
 * calories. Macros scale with the calorie edit so the split stays believable.
 */
@Composable
fun EditMealSheet(
    meal: MealEntry?,
    viewModel: AppViewModel,
    onClose: () -> Unit,
    onSaved: (MealEntry) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var calories by remember { mutableIntStateOf(0) }

    LaunchedEffect(meal?.id) {
        if (meal != null) {
            title = meal.title
            calories = Nutrition.mealCalories(meal)
        }
    }

    val originalCalories = meal?.let { Nutrition.mealCalories(it) } ?: 0
    val preview = meal?.let { Nutrition.mealWithCalories(it, calories) }
    val trimmed = title.trim()
    val canSave = meal != null && trimmed.isNotEmpty() &&
        (trimmed != meal.title || calories != originalCalories)
    val delta = calories - originalCalories

    fun step(amount: Int) {
        calories = (calories + amount).coerceIn(0, MAX_CALORIES)
    }

    FullScreenSheet(
        open = meal != null,
        onClose = onClose,
        title = "Edit meal",
        footer = {
            PrimaryButton(
                text = "Save changes",
                enabled = canSave,
                onClick = {
                    if (meal == null || !canSave) return@PrimaryButton
                    val updated = Nutrition.mealWithCalories(meal, calories).copy(title = trimmed)
                    viewModel.updateMeal(updated)
                    onSaved(updated)
                    onClose()
                },
            )
        },
    ) {
        if (meal == null || preview == null) return@FullScreenSheet
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 6.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Name
            CalzyCard(radius = 22.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Meal name",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CalzyColors.inkSoft,
                        modifier = Modifier.padding(bottom = 9.dp),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CalzyColors.well),
                    ) {
                        TextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = {
                                Text(
                                    text = "e.g. Chicken burrito bowl",
                                    color = CalzyColors.inkFaint,
                                )
                            },
                            textStyle = TextStyle(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CalzyColors.ink,
                            ),
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            singleLine = true,
                        )
                        if (title.isNotEmpty()) {
                            Pressable(onClick = { title = "" }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear meal name",
                                    tint = CalzyColors.inkFaint,
                                    modifier = Modifier
                                        .padding(end = 14.dp)
                                        .size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Calories
            CalzyCard(radius = 22.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Calories",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CalzyColors.inkSoft,
                            modifier = Modifier.weight(1f),
                        )
                        if (delta != 0) {
                            val deltaTint = if (delta > 0) CalzyColors.flame else CalzyColors.mint
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(deltaTint.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = "${if (delta > 0) "+$delta" else "$delta"} kcal",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = deltaTint,
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StepCircle(
                            label = "Decrease calories",
                            icon = Icons.Outlined.Remove,
                            enabled = calories > 0,
                        ) { step(-10) }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TextField(
                                value = "$calories",
                                onValueChange = { raw ->
                                    val digits = raw.filter { it.isDigit() }
                                    calories = (digits.toIntOrNull() ?: 0).coerceAtMost(MAX_CALORIES)
                                },
                                textStyle = TextStyle(
                                    fontSize = 40.sp,
                                    fontFamily = MetricFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CalzyColors.ink,
                                    textAlign = TextAlign.Center,
                                ),
                                modifier = Modifier.width(150.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                            )
                            Text(
                                text = "kcal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = CalzyColors.inkFaint,
                            )
                        }

                        StepCircle(
                            label = "Increase calories",
                            icon = Icons.Outlined.Add,
                            enabled = calories < MAX_CALORIES,
                        ) { step(10) }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(-100, -50, 50, 100).forEach { amount ->
                            Pressable(onClick = { step(amount) }, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(CircleShape)
                                        .background(CalzyColors.well)
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = if (amount > 0) "+$amount" else "$amount",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CalzyColors.ink,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Macro preview
            CalzyCard(radius = 22.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Macros scale to match",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CalzyColors.inkSoft,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MacroPill(
                            name = "Protein",
                            value = Nutrition.mealProtein(preview),
                            tint = CalzyColors.protein,
                            modifier = Modifier.weight(1f),
                        )
                        MacroPill(
                            name = "Carbs",
                            value = Nutrition.mealCarbs(preview),
                            tint = CalzyColors.carbs,
                            modifier = Modifier.weight(1f),
                        )
                        MacroPill(
                            name = "Fat",
                            value = Nutrition.mealFat(preview),
                            tint = CalzyColors.fat,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepCircle(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Pressable(onClick = onClick, enabled = enabled) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CalzyColors.well),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) CalzyColors.ink else CalzyColors.inkFaint,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}
