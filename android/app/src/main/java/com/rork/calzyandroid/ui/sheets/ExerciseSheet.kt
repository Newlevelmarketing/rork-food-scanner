package com.rork.calzyandroid.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rork.calzyandroid.AppViewModel
import com.rork.calzyandroid.data.Dates
import com.rork.calzyandroid.data.ExerciseEntry
import com.rork.calzyandroid.data.exercisePresets
import com.rork.calzyandroid.data.presetCalories
import com.rork.calzyandroid.ui.components.CalzyCard
import com.rork.calzyandroid.ui.components.CalzySlider
import com.rork.calzyandroid.ui.components.FullScreenSheet
import com.rork.calzyandroid.ui.components.IconMaps
import com.rork.calzyandroid.ui.components.LocalT
import com.rork.calzyandroid.ui.components.MetricText
import com.rork.calzyandroid.ui.components.Pressable
import com.rork.calzyandroid.ui.components.PrimaryButton
import com.rork.calzyandroid.ui.theme.CalzyColors

/** Log a workout to add calories back to the day's budget. */
@Composable
fun ExerciseSheet(
    open: Boolean,
    viewModel: AppViewModel,
    onClose: () -> Unit,
) {
    val t = LocalT.current
    val data by viewModel.data.collectAsStateWithLifecycle()

    var selected by remember { mutableStateOf(exercisePresets.first()) }
    var minutes by remember { mutableIntStateOf(30) }

    LaunchedEffect(open) {
        if (!open) {
            selected = exercisePresets.first()
            minutes = 30
        }
    }

    val burned = presetCalories(selected, minutes, data.profile.currentWeightKg)

    FullScreenSheet(
        open = open,
        onClose = onClose,
        title = t("h.exercise"),
        footer = {
            PrimaryButton(
                text = "Add $burned kcal back",
                onClick = {
                    viewModel.addExercise(
                        ExerciseEntry(
                            name = selected.name,
                            date = Dates.nowIso(),
                            minutes = minutes,
                            calories = burned,
                            icon = selected.icon,
                        ),
                    )
                    onClose()
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            CalzyCard(radius = 26.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = IconMaps.exercise(selected.icon),
                        contentDescription = null,
                        tint = CalzyColors.flame,
                        modifier = Modifier.size(34.dp),
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        MetricText(text = "$burned", size = 52)
                        Text(
                            text = " kcal",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = CalzyColors.inkFaint,
                        )
                    }
                    Text(
                        text = "$minutes minutes of ${selected.name.lowercase()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = CalzyColors.inkSoft,
                    )
                }
            }

            CalzyCard(radius = 22.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "Duration",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CalzyColors.ink,
                            modifier = Modifier.weight(1f),
                        )
                        MetricText(text = "$minutes min", size = 16, color = CalzyColors.inkSoft)
                    }
                    CalzySlider(
                        value = minutes.toFloat(),
                        onChange = { minutes = (it / 5).toInt() * 5 },
                        range = 5f..180f,
                        tint = CalzyColors.flame,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(370.dp),
                userScrollEnabled = false,
            ) {
                items(exercisePresets, key = { it.name }) { preset ->
                    val active = preset.name == selected.name
                    Pressable(onClick = { selected = preset }) {
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
                                .padding(vertical = 15.dp),
                        ) {
                            Icon(
                                imageVector = IconMaps.exercise(preset.icon),
                                contentDescription = null,
                                tint = if (active) Color.White else CalzyColors.ink,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = preset.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (active) Color.White else CalzyColors.inkSoft,
                            )
                        }
                    }
                }
            }
        }
    }
}
