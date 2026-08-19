package com.rork.calzyandroid.data

import java.time.LocalDate
import kotlin.math.roundToInt

/** Derived, read-only views over [AppData] — mirrors the web store selectors. */

private fun <T> newestFirst(
    entries: List<T>,
    day: LocalDate,
    dateOf: (T) -> String,
): List<T> = entries
    .withIndex()
    .filter { Dates.isSameDay(dateOf(it.value), day) }
    .sortedWith(
        compareByDescending<IndexedValue<T>> { Dates.parse(dateOf(it.value)) }
            .thenByDescending { it.index },
    )
    .map { it.value }

fun AppData.mealsOn(day: LocalDate): List<MealEntry> = newestFirst(meals, day) { it.date }

fun AppData.exercisesOn(day: LocalDate): List<ExerciseEntry> =
    newestFirst(exercises, day) { it.date }

fun AppData.caloriesEaten(day: LocalDate): Int =
    mealsOn(day).sumOf { Nutrition.mealCalories(it) }

fun AppData.caloriesBurned(day: LocalDate): Int = exercisesOn(day).sumOf { it.calories }

fun AppData.proteinOn(day: LocalDate): Double = mealsOn(day).sumOf { Nutrition.mealProtein(it) }

fun AppData.carbsOn(day: LocalDate): Double = mealsOn(day).sumOf { Nutrition.mealCarbs(it) }

fun AppData.fatOn(day: LocalDate): Double = mealsOn(day).sumOf { Nutrition.mealFat(it) }

fun AppData.waterOn(day: LocalDate): Int =
    water.filter { Dates.isSameDay(it.date, day) }.sumOf { it.milliliters }

fun AppData.hasLogs(day: LocalDate): Boolean =
    meals.any { Dates.isSameDay(it.date, day) } || exercises.any { Dates.isSameDay(it.date, day) }

/** Consecutive days (ending today or yesterday) with at least one logged meal. */
fun AppData.streak(): Int {
    var cursor = LocalDate.now()
    if (mealsOn(cursor).isEmpty()) {
        val yesterday = cursor.minusDays(1)
        if (mealsOn(yesterday).isEmpty()) return 0
        cursor = yesterday
    }
    var count = 0
    while (mealsOn(cursor).isNotEmpty()) {
        count += 1
        cursor = cursor.minusDays(1)
    }
    return count
}

data class AverageCalories(val average: Int, val logged: Int, val total: Int)

/** Average calories over the last 7 days, counting only days with logs. */
fun AppData.averageCalories(): AverageCalories {
    val today = LocalDate.now()
    var total = 0
    var logged = 0
    for (offset in 0 until 7) {
        val value = caloriesEaten(today.minusDays(offset.toLong()))
        if (value > 0) {
            total += value
            logged += 1
        }
    }
    return AverageCalories(
        average = if (logged == 0) 0 else (total.toDouble() / logged).roundToInt(),
        logged = logged,
        total = 7,
    )
}

fun AppData.weightEntriesSorted(): List<WeightEntry> =
    weights.sortedBy { Dates.parse(it.date) }

fun AppData.weightOn(day: LocalDate): WeightEntry? =
    weights.find { Dates.isSameDay(it.date, day) }

fun AppData.photosSorted(): List<ProgressPhoto> =
    photos.sortedByDescending { Dates.parse(it.date) }

fun AppData.isSaved(title: String): Boolean =
    saved.any { it.title.equals(title, ignoreCase = true) }
