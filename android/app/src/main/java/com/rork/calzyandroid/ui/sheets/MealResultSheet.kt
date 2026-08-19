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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rork.calzyandroid.AppViewModel
import com.rork.calzyandroid.data.AiService
import com.rork.calzyandroid.data.Dates
import com.rork.calzyandroid.data.FoodItem
import com.rork.calzyandroid.data.ImageUtils
import com.rork.calzyandroid.data.MealEntry
import com.rork.calzyandroid.data.MealSlot
import com.rork.calzyandroid.data.Nutrition
import com.rork.calzyandroid.data.isSaved
import com.rork.calzyandroid.ui.components.CalzyCard
import com.rork.calzyandroid.ui.components.FullScreenSheet
import com.rork.calzyandroid.ui.components.Hairline
import com.rork.calzyandroid.ui.components.MetricText
import com.rork.calzyandroid.ui.components.Pressable
import com.rork.calzyandroid.ui.components.PrimaryButton
import com.rork.calzyandroid.ui.navigation.MealDraft
import com.rork.calzyandroid.ui.theme.CalzyColors
import java.util.Locale
import kotlin.math.roundToInt

/** Review + confirm screen shown after a scan, description or search pick. */
@Composable
fun MealResultSheet(
    draft: MealDraft?,
    viewModel: AppViewModel,
    onClose: () -> Unit,
) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val date by viewModel.selectedDate.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var portions by remember { mutableDoubleStateOf(1.0) }
    var slot by remember { mutableStateOf(Nutrition.currentSlot()) }

    LaunchedEffect(draft) {
        if (draft != null) {
            title = draft.result.title
            items = AiService.resultToItems(draft.result)
            portions = 1.0
            slot = Nutrition.currentSlot()
        }
    }

    val totalCalories = (items.sumOf { it.calories } * portions).roundToInt()
    val totalProtein = items.sumOf { it.protein } * portions
    val totalCarbs = items.sumOf { it.carbs } * portions
    val totalFat = items.sumOf { it.fat } * portions

    val score = draft?.result?.healthScore ?: 0
    val scoreTint = when {
        score >= 8 -> CalzyColors.mint
        score >= 5 -> CalzyColors.fat
        else -> CalzyColors.protein
    }
    val saved = data.isSaved(title)

    FullScreenSheet(
        open = draft != null,
        onClose = onClose,
        title = "Review meal",
        trailing = {
            if (draft != null) {
                Pressable(onClick = { viewModel.toggleSaved(title, items, slot) }) {
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
        },
        footer = {
            if (draft != null) {
                PrimaryButton(
                    text = "Log $totalCalories kcal",
                    enabled = items.isNotEmpty(),
                    onClick = {
                        viewModel.addMeal(
                            MealEntry(
                                title = title.trim().ifEmpty { "Meal" },
                                date = Dates.mergedTimestamp(date),
                                slot = slot,
                                source = draft.source,
                                items = items,
                                portions = portions,
                                photo = draft.photo,
                                healthScore = score,
                                quip = draft.result.quip,
                            ),
                        )
                        onClose()
                    },
                )
            }
        },
    ) {
        if (draft == null) return@FullScreenSheet
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            draft.photo?.let { photo ->
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(14.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.42f))
                                .padding(horizontal = 11.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp),
                            )
                            Text(
                                text = "AI estimate",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            CalzyCard(radius = 26.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = {
                            Text(
                                text = "Meal name",
                                color = CalzyColors.inkFaint,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        },
                        textStyle = TextStyle(
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = CalzyColors.ink,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        singleLine = true,
                    )

                    Row(verticalAlignment = Alignment.Bottom) {
                        MetricText(text = "$totalCalories", size = 46)
                        Text(
                            text = " kcal",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = CalzyColors.inkFaint,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MacroPill(
                            name = "Protein",
                            value = totalProtein,
                            tint = CalzyColors.protein,
                            modifier = Modifier.weight(1f),
                        )
                        MacroPill(
                            name = "Carbs",
                            value = totalCarbs,
                            tint = CalzyColors.carbs,
                            modifier = Modifier.weight(1f),
                        )
                        MacroPill(
                            name = "Fat",
                            value = totalFat,
                            tint = CalzyColors.fat,
                            modifier = Modifier.weight(1f),
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
                        StepButton(
                            label = "Decrease portions",
                            icon = Icons.Outlined.Remove,
                        ) {
                            portions = maxOf(0.25, ((portions - 0.25) * 100).roundToInt() / 100.0)
                        }
                        MetricText(
                            text = formatPortions(portions),
                            size = 19,
                            modifier = Modifier.width(56.dp),
                            align = TextAlign.Center,
                        )
                        StepButton(
                            label = "Increase portions",
                            icon = Icons.Outlined.Add,
                        ) {
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

            if (items.isNotEmpty()) {
                CalzyCard(radius = 22.dp) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        items.forEachIndexed { index, item ->
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
                                    text = "${item.calories}",
                                    size = 16,
                                    color = CalzyColors.inkSoft,
                                )
                                Pressable(onClick = {
                                    items = items.filter { it.id != item.id }
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Remove ${item.name}",
                                        tint = CalzyColors.inkFaint.copy(alpha = 0.6f),
                                        modifier = Modifier.size(17.dp),
                                    )
                                }
                            }
                            if (index < items.size - 1) {
                                Hairline(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }

            draft.result.quip?.let { quip ->
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
fun MacroPill(name: String, value: Double, tint: Color, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.1f))
            .padding(vertical = 10.dp),
    ) {
        MetricText(text = "${value.roundToInt()}g", size = 17)
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = CalzyColors.inkSoft,
        )
    }
}

@Composable
fun SlotChip(label: String, active: Boolean, onClick: () -> Unit) {
    Pressable(onClick = onClick) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (active) CalzyColors.ink else CalzyColors.ink.copy(alpha = 0.05f),
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (active) Color.White else CalzyColors.inkSoft,
            )
        }
    }
}

@Composable
private fun StepButton(
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
