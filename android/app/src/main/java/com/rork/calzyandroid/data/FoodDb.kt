package com.rork.calzyandroid.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FoodRecord(
    val name: String,
    val serving: String,
    val kcal: Int,
    val p: Double,
    val c: Double,
    val f: Double,
    val tags: String,
)

/** Bundled offline food table used by the Search flow (assets/foods.json). */
object FoodDb {
    private var cache: List<FoodRecord> = emptyList()

    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context) {
        if (cache.isNotEmpty()) return
        cache = try {
            val raw = context.assets.open("foods.json").bufferedReader().use { it.readText() }
            json.decodeFromString<List<FoodRecord>>(raw)
        } catch (error: Exception) {
            emptyList()
        }
    }

    fun all(): List<FoodRecord> = cache

    fun search(query: String): List<FoodRecord> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return cache.take(24)
        return cache
            .filter {
                it.name.lowercase().contains(trimmed) || it.tags.lowercase().contains(trimmed)
            }
            .sortedWith(
                compareBy(
                    { if (it.name.lowercase().contains(trimmed)) 0 else 1 },
                    { it.name },
                ),
            )
    }

    fun toItem(record: FoodRecord): FoodItem = FoodItem(
        name = record.name,
        quantity = record.serving,
        calories = record.kcal,
        protein = record.p,
        carbs = record.c,
        fat = record.f,
    )
}

data class ExercisePreset(
    val name: String,
    val icon: String,
    /** Calories burned per minute for an 80 kg person. */
    val perMinute: Double,
)

val exercisePresets: List<ExercisePreset> = listOf(
    ExercisePreset("Walking", "walk", 4.5),
    ExercisePreset("Running", "run", 11.5),
    ExercisePreset("Cycling", "bike", 9.0),
    ExercisePreset("Weights", "weights", 6.5),
    ExercisePreset("HIIT", "hiit", 12.5),
    ExercisePreset("Swimming", "swim", 10.0),
    ExercisePreset("Yoga", "yoga", 3.5),
    ExercisePreset("Football", "football", 9.5),
    ExercisePreset("Tennis", "tennis", 8.0),
    ExercisePreset("Rowing", "row", 10.5),
    ExercisePreset("Boxing", "boxing", 11.0),
    ExercisePreset("Hiking", "hike", 7.0),
)

fun presetCalories(preset: ExercisePreset, minutes: Int, weightKg: Double): Int =
    (preset.perMinute * minutes * (weightKg / 80.0)).let { kotlin.math.round(it).toInt() }
