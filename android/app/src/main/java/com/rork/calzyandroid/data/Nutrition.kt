package com.rork.calzyandroid.data

import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Nutrition math — mirrors `web/src/lib/nutrition.ts`. */
object Nutrition {

    data class ActivityMeta(val label: String, val detail: String, val multiplier: Double)

    val activityMeta: Map<ActivityLevel, ActivityMeta> = mapOf(
        ActivityLevel.sedentary to ActivityMeta("Sedentary", "Desk job, little exercise", 1.2),
        ActivityLevel.light to ActivityMeta("Lightly active", "1–2 workouts a week", 1.375),
        ActivityLevel.moderate to ActivityMeta("Moderately active", "3–4 workouts a week", 1.55),
        ActivityLevel.high to ActivityMeta("Very active", "5–6 workouts a week", 1.725),
        ActivityLevel.athlete to ActivityMeta("Athlete", "Twice-daily training", 1.9),
    )

    val activityOrder: List<ActivityLevel> = listOf(
        ActivityLevel.sedentary,
        ActivityLevel.light,
        ActivityLevel.moderate,
        ActivityLevel.high,
        ActivityLevel.athlete,
    )

    val goalOrder: List<GoalDirection> =
        listOf(GoalDirection.lose, GoalDirection.maintain, GoalDirection.gain)

    val goalLabels: Map<GoalDirection, String> = mapOf(
        GoalDirection.lose to "Lose weight",
        GoalDirection.maintain to "Maintain",
        GoalDirection.gain to "Build muscle",
    )

    val slotOrder: List<MealSlot> =
        listOf(MealSlot.breakfast, MealSlot.lunch, MealSlot.dinner, MealSlot.snack)

    val slotLabels: Map<MealSlot, String> = mapOf(
        MealSlot.breakfast to "Breakfast",
        MealSlot.lunch to "Lunch",
        MealSlot.dinner to "Dinner",
        MealSlot.snack to "Snack",
    )

    fun currentSlot(time: LocalTime = LocalTime.now()): MealSlot = when (time.hour) {
        in 4..10 -> MealSlot.breakfast
        in 11..15 -> MealSlot.lunch
        in 16..21 -> MealSlot.dinner
        else -> MealSlot.snack
    }

    fun ageOf(profile: UserProfile): Int =
        max(13, LocalDate.now().year - profile.birthYear)

    fun bmiOf(heightCm: Double, weightKg: Double): Double {
        val meters = heightCm / 100.0
        return if (meters > 0) weightKg / (meters * meters) else 0.0
    }

    /** Mifflin–St Jeor basal metabolic rate. */
    fun bmrOf(profile: UserProfile): Double {
        val base = 10 * profile.currentWeightKg + 6.25 * profile.heightCm - 5 * ageOf(profile)
        return if (profile.sex == Sex.male) base + 5 else base - 161
    }

    fun maintenanceOf(profile: UserProfile): Double =
        bmrOf(profile) * (activityMeta[profile.activity]?.multiplier ?: 1.375)

    fun targetsOf(profile: UserProfile): NutritionTargets {
        if (profile.usesCustomTargets) {
            return NutritionTargets(
                calories = profile.customCalories,
                protein = profile.customProtein,
                carbs = profile.customCarbs,
                fat = profile.customFat,
            )
        }

        val maintenance = maintenanceOf(profile)
        val dailyDelta = profile.weeklyRateKg * 7700 / 7
        val raw = when (profile.goal) {
            GoalDirection.lose -> maintenance - dailyDelta
            GoalDirection.gain -> maintenance + dailyDelta * 0.6
            GoalDirection.maintain -> maintenance
        }
        val calories = max(1200, raw.roundToInt())

        val proteinPerKg = if (profile.goal == GoalDirection.gain) 2.0 else 1.8
        val protein = (profile.currentWeightKg * proteinPerKg).roundToInt()
        val fat = (calories * 0.27 / 9).roundToInt()
        val carbs = max(0, ((calories - protein * 4 - fat * 9) / 4.0).roundToInt())

        return NutritionTargets(calories, protein, carbs, fat)
    }

    enum class BmiCategory(val label: String) {
        Underweight("Underweight"),
        Healthy("Healthy"),
        Overweight("Overweight"),
        Obese("Obese"),
    }

    fun bmiCategory(bmi: Double): BmiCategory = when {
        bmi < 18.5 -> BmiCategory.Underweight
        bmi < 25 -> BmiCategory.Healthy
        bmi < 30 -> BmiCategory.Overweight
        else -> BmiCategory.Obese
    }

    private fun round1(value: Double): Double = (value * 10).roundToInt() / 10.0

    fun itemsCalories(items: List<FoodItem>): Int = items.sumOf { it.calories }

    fun mealCalories(meal: MealEntry): Int =
        (itemsCalories(meal.items) * meal.portions).roundToInt()

    fun mealProtein(meal: MealEntry): Double =
        round1(meal.items.sumOf { it.protein } * meal.portions)

    fun mealCarbs(meal: MealEntry): Double =
        round1(meal.items.sumOf { it.carbs } * meal.portions)

    fun mealFat(meal: MealEntry): Double =
        round1(meal.items.sumOf { it.fat } * meal.portions)

    fun scaleItem(item: FoodItem, factor: Double): FoodItem = item.copy(
        calories = (item.calories * factor).roundToInt(),
        protein = round1(item.protein * factor),
        carbs = round1(item.carbs * factor),
        fat = round1(item.fat * factor),
    )

    /**
     * Returns a copy of [meal] whose total calories equal [target], scaling every
     * item so the macro split stays intact. Meals with no nutrition collapse to a
     * single manual item carrying the entered calories.
     */
    fun mealWithCalories(meal: MealEntry, target: Int): MealEntry {
        val clamped = max(0, min(target, 20000))
        val current = mealCalories(meal)

        if (current <= 0 || meal.items.isEmpty()) {
            return meal.copy(
                portions = 1.0,
                items = listOf(
                    FoodItem(
                        name = meal.title.trim().ifEmpty { "Meal" },
                        quantity = "1 serving",
                        calories = clamped,
                        protein = 0.0,
                        carbs = 0.0,
                        fat = 0.0,
                    ),
                ),
            )
        }

        val factor = clamped.toDouble() / current
        val items = meal.items.map { scaleItem(it, factor) }
        val next = meal.copy(items = items)

        // Per-item rounding can drift a few kcal; push the remainder into the
        // largest item so the row shows exactly what was typed.
        val drift = clamped - mealCalories(next)
        if (drift != 0 && next.portions > 0) {
            val largest = items.indices.maxByOrNull { items[it].calories } ?: 0
            val corrected = items.toMutableList()
            corrected[largest] = corrected[largest].copy(
                calories = max(
                    0,
                    corrected[largest].calories + (drift / next.portions).roundToInt(),
                ),
            )
            return next.copy(items = corrected)
        }
        return next
    }

    fun etaWeeks(fromKg: Double, toKg: Double, ratePerWeek: Double): Double =
        abs(toKg - fromKg) / max(ratePerWeek, 0.1)
}
