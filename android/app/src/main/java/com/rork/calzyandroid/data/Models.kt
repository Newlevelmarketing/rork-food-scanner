package com.rork.calzyandroid.data

import java.util.UUID
import kotlinx.serialization.Serializable

/** Domain models — mirror `web/src/lib/types.ts` and the iOS models. */

fun uid(): String = UUID.randomUUID().toString()

@Serializable
enum class Sex { male, female }

@Serializable
enum class ActivityLevel { sedentary, light, moderate, high, athlete }

@Serializable
enum class GoalDirection { lose, maintain, gain }

@Serializable
enum class UnitSystem { metric, imperial }

@Serializable
enum class MealSlot { breakfast, lunch, dinner, snack }

@Serializable
enum class EntrySource { photo, text, search, saved, manual }

@Serializable
data class NutritionTargets(
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
)

/** Everything the app knows about the person using it, plus their daily targets. */
@Serializable
data class UserProfile(
    val name: String = "",
    val sex: Sex = Sex.male,
    val birthYear: Int = 1996,
    val heightCm: Double = 178.0,
    val startWeightKg: Double = 82.0,
    val currentWeightKg: Double = 82.0,
    val goalWeightKg: Double = 76.0,
    val activity: ActivityLevel = ActivityLevel.light,
    val goal: GoalDirection = GoalDirection.lose,
    val weeklyRateKg: Double = 0.5,
    val units: UnitSystem = UnitSystem.metric,
    val usesCustomTargets: Boolean = false,
    val customCalories: Int = 2200,
    val customProtein: Int = 140,
    val customCarbs: Int = 240,
    val customFat: Int = 70,
    val waterGoalMl: Int = 2500,
    val jesterMode: Boolean = false,
    val remindersEnabled: Boolean = true,
    val reminderTimes: List<Int> = listOf(9, 13, 19),
    val healthSynced: Boolean = false,
    val isPro: Boolean = false,
    val hasOnboarded: Boolean = false,
    /** Language code from the catalogue in I18n.kt; null means "follow the system". */
    val languageCode: String? = null,
)

/** One food item inside a logged meal. */
@Serializable
data class FoodItem(
    val id: String = uid(),
    val name: String,
    val quantity: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
)

/** A logged meal, optionally backed by a photo the user scanned (JPEG data URL). */
@Serializable
data class MealEntry(
    val id: String = uid(),
    val title: String,
    /** ISO-8601 timestamp. */
    val date: String,
    val slot: MealSlot,
    val source: EntrySource,
    val items: List<FoodItem>,
    val portions: Double = 1.0,
    val photo: String? = null,
    val healthScore: Int = 6,
    val note: String? = null,
    val quip: String? = null,
)

@Serializable
data class ExerciseEntry(
    val id: String = uid(),
    val name: String,
    val date: String,
    val minutes: Int,
    val calories: Int,
    val icon: String,
)

@Serializable
data class WaterEntry(
    val id: String = uid(),
    val date: String,
    val milliliters: Int,
)

@Serializable
data class WeightEntry(
    val id: String = uid(),
    val date: String,
    val kilograms: Double,
)

@Serializable
data class ProgressPhoto(
    val id: String = uid(),
    val date: String,
    val photo: String,
    val weightKg: Double? = null,
)

/** A food the user bookmarked for one-tap re-logging. */
@Serializable
data class SavedFood(
    val id: String = uid(),
    val title: String,
    val items: List<FoodItem>,
    val slot: MealSlot,
)

@Serializable
data class AppData(
    val profile: UserProfile = UserProfile(),
    val meals: List<MealEntry> = emptyList(),
    val exercises: List<ExerciseEntry> = emptyList(),
    val water: List<WaterEntry> = emptyList(),
    val weights: List<WeightEntry> = emptyList(),
    val photos: List<ProgressPhoto> = emptyList(),
    val saved: List<SavedFood> = emptyList(),
)
