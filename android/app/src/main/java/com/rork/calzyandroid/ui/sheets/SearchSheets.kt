package com.rork.calzyandroid.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rork.calzyandroid.AppViewModel
import com.rork.calzyandroid.data.Dates
import com.rork.calzyandroid.data.EntrySource
import com.rork.calzyandroid.data.FoodDb
import com.rork.calzyandroid.data.FoodRecord
import com.rork.calzyandroid.data.MealEntry
import com.rork.calzyandroid.data.Nutrition
import com.rork.calzyandroid.data.SavedFood
import com.rork.calzyandroid.ui.components.CalzyCard
import com.rork.calzyandroid.ui.components.FullScreenSheet
import com.rork.calzyandroid.ui.components.MetricText
import com.rork.calzyandroid.ui.components.Pressable
import com.rork.calzyandroid.ui.theme.CalzyColors
import kotlinx.coroutines.delay

/** Offline food search backed by the bundled 100-food table. */
@Composable
fun SearchSheet(
    open: Boolean,
    viewModel: AppViewModel,
    onClose: () -> Unit,
) {
    val date by viewModel.selectedDate.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var justAdded by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(open) {
        if (!open) {
            query = ""
            justAdded = null
        }
    }

    LaunchedEffect(justAdded) {
        if (justAdded != null) {
            delay(900)
            justAdded = null
        }
    }

    val results = remember(query) { FoodDb.search(query) }

    FullScreenSheet(
        open = open,
        onClose = onClose,
        title = "Search foods",
        scrollable = false,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CalzyCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 12.dp),
                radius = 18.dp,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = CalzyColors.inkFaint,
                        modifier = Modifier.size(17.dp),
                    )
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text("Chicken, oats, latte…", color = CalzyColors.inkFaint)
                        },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        singleLine = true,
                    )
                    if (query.isNotEmpty()) {
                        Pressable(onClick = { query = "" }) {
                            Text(
                                text = "Clear",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CalzyColors.inkFaint,
                            )
                        }
                    }
                }
            }

            if (results.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = CalzyColors.inkFaint,
                        modifier = Modifier.size(30.dp),
                    )
                    Text(
                        text = "No matches for \u201C$query\u201D",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CalzyColors.inkSoft,
                    )
                    Text(
                        text = "Try the Type flow instead — the AI can estimate anything.",
                        fontSize = 13.sp,
                        color = CalzyColors.inkFaint,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 32.dp,
                    ),
                ) {
                    items(results, key = { it.name }) { record ->
                        FoodRow(
                            record = record,
                            highlighted = justAdded == record.name,
                            onAdd = {
                                viewModel.addMeal(
                                    MealEntry(
                                        title = record.name,
                                        date = Dates.mergedTimestamp(date),
                                        slot = Nutrition.currentSlot(),
                                        source = EntrySource.search,
                                        items = listOf(FoodDb.toItem(record)),
                                        portions = 1.0,
                                        healthScore = 6,
                                    ),
                                )
                                justAdded = record.name
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodRow(record: FoodRecord, highlighted: Boolean, onAdd: () -> Unit) {
    Pressable(onClick = onAdd) {
        CalzyCard(modifier = Modifier.fillMaxWidth(), radius = 20.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CalzyColors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${record.serving} · ${record.p}P ${record.c}C ${record.f}F",
                        fontSize = 12.sp,
                        color = CalzyColors.inkFaint,
                    )
                }
                MetricText(text = "${record.kcal}", size = 17)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (highlighted) CalzyColors.mint else CalzyColors.well),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Log ${record.name}",
                        tint = if (highlighted) Color.White else CalzyColors.ink,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}

/** Bookmarked meals, ready for one-tap re-logging. */
@Composable
fun SavedSheet(
    open: Boolean,
    viewModel: AppViewModel,
    onClose: () -> Unit,
) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val date by viewModel.selectedDate.collectAsStateWithLifecycle()

    FullScreenSheet(open = open, onClose = onClose, title = "Saved meals") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (data.saved.isEmpty()) {
                CalzyCard(radius = 26.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 56.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Bookmark,
                            contentDescription = null,
                            tint = CalzyColors.inkFaint,
                            modifier = Modifier.size(32.dp),
                        )
                        Text(
                            text = "Nothing saved yet",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = CalzyColors.ink,
                        )
                        Text(
                            text = "Tap the bookmark on any meal you review and it lands here for one-tap logging.",
                            fontSize = 13.sp,
                            color = CalzyColors.inkSoft,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                data.saved.forEach { food ->
                    SavedRow(
                        food = food,
                        onRelog = {
                            viewModel.addMeal(
                                MealEntry(
                                    title = food.title,
                                    date = Dates.mergedTimestamp(date),
                                    slot = food.slot,
                                    source = EntrySource.saved,
                                    items = food.items,
                                    portions = 1.0,
                                    healthScore = 7,
                                ),
                            )
                            onClose()
                        },
                        onDelete = { viewModel.deleteSaved(food.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedRow(food: SavedFood, onRelog: () -> Unit, onDelete: () -> Unit) {
    CalzyCard(modifier = Modifier.fillMaxWidth(), radius = 20.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Pressable(onClick = onRelog, modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = food.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CalzyColors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val itemsLabel = if (food.items.size == 1) "item" else "items"
                    Text(
                        text = "${Nutrition.slotLabels[food.slot]} · ${food.items.size} $itemsLabel",
                        fontSize = 12.sp,
                        color = CalzyColors.inkFaint,
                    )
                }
            }
            MetricText(text = "${Nutrition.itemsCalories(food.items)}", size = 17)
            Pressable(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete ${food.title}",
                    tint = CalzyColors.inkFaint,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}
