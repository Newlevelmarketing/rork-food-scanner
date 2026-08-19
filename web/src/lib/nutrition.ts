import type {
  ActivityLevel,
  FoodItem,
  GoalDirection,
  MealEntry,
  MealSlot,
  NutritionTargets,
  Sex,
  UserProfile,
} from "./types";

export const activityMeta: Record<
  ActivityLevel,
  { label: string; detail: string; multiplier: number; icon: string }
> = {
  sedentary: { label: "Sedentary", detail: "Desk job, little exercise", multiplier: 1.2, icon: "Armchair" },
  light: { label: "Lightly active", detail: "1–2 workouts a week", multiplier: 1.375, icon: "Footprints" },
  moderate: { label: "Moderately active", detail: "3–4 workouts a week", multiplier: 1.55, icon: "PersonStanding" },
  high: { label: "Very active", detail: "5–6 workouts a week", multiplier: 1.725, icon: "Dumbbell" },
  athlete: { label: "Athlete", detail: "Twice-daily training", multiplier: 1.9, icon: "Flame" },
};

export const activityOrder: ActivityLevel[] = ["sedentary", "light", "moderate", "high", "athlete"];

export const goalMeta: Record<GoalDirection, { label: string; icon: string }> = {
  lose: { label: "Lose weight", icon: "TrendingDown" },
  maintain: { label: "Maintain", icon: "Equal" },
  gain: { label: "Build muscle", icon: "TrendingUp" },
};

export const goalOrder: GoalDirection[] = ["lose", "maintain", "gain"];

export const sexOrder: Sex[] = ["male", "female"];

export const slotOrder: MealSlot[] = ["breakfast", "lunch", "dinner", "snack"];

export const slotMeta: Record<MealSlot, { label: string; icon: string }> = {
  breakfast: { label: "Breakfast", icon: "Sunrise" },
  lunch: { label: "Lunch", icon: "Sun" },
  dinner: { label: "Dinner", icon: "MoonStar" },
  snack: { label: "Snack", icon: "CupSoda" },
};

export function currentSlot(at: Date = new Date()): MealSlot {
  const hour = at.getHours();
  if (hour >= 4 && hour < 11) return "breakfast";
  if (hour >= 11 && hour < 16) return "lunch";
  if (hour >= 16 && hour < 22) return "dinner";
  return "snack";
}

export function ageOf(profile: UserProfile): number {
  return Math.max(13, new Date().getFullYear() - profile.birthYear);
}

export function bmiOf(heightCm: number, weightKg: number): number {
  const m = heightCm / 100;
  return m > 0 ? weightKg / (m * m) : 0;
}

/** Mifflin–St Jeor basal metabolic rate. */
export function bmrOf(profile: UserProfile): number {
  const base = 10 * profile.currentWeightKg + 6.25 * profile.heightCm - 5 * ageOf(profile);
  return profile.sex === "male" ? base + 5 : base - 161;
}

export function maintenanceOf(profile: UserProfile): number {
  return bmrOf(profile) * activityMeta[profile.activity].multiplier;
}

export function targetsOf(profile: UserProfile): NutritionTargets {
  if (profile.usesCustomTargets) {
    return {
      calories: profile.customCalories,
      protein: profile.customProtein,
      carbs: profile.customCarbs,
      fat: profile.customFat,
    };
  }

  const maintenance = maintenanceOf(profile);
  const dailyDelta = (profile.weeklyRateKg * 7700) / 7;
  let calories: number;
  switch (profile.goal) {
    case "lose":
      calories = maintenance - dailyDelta;
      break;
    case "gain":
      calories = maintenance + dailyDelta * 0.6;
      break;
    default:
      calories = maintenance;
  }
  calories = Math.max(1200, Math.round(calories));

  const proteinPerKg = profile.goal === "gain" ? 2.0 : 1.8;
  const protein = Math.round(profile.currentWeightKg * proteinPerKg);
  const fat = Math.round((calories * 0.27) / 9);
  const carbs = Math.max(0, Math.round((calories - protein * 4 - fat * 9) / 4));

  return { calories, protein, carbs, fat };
}

export type BMICategory = "Underweight" | "Healthy" | "Overweight" | "Obese";

export function bmiCategory(bmi: number): BMICategory {
  if (bmi < 18.5) return "Underweight";
  if (bmi < 25) return "Healthy";
  if (bmi < 30) return "Overweight";
  return "Obese";
}

export function bmiCategoryColor(category: BMICategory): string {
  switch (category) {
    case "Underweight":
      return "hsl(var(--carbs))";
    case "Healthy":
      return "hsl(var(--mint))";
    case "Overweight":
      return "hsl(var(--fat))";
    default:
      return "hsl(var(--protein))";
  }
}

const round1 = (value: number): number => Math.round(value * 10) / 10;

export function itemsCalories(items: FoodItem[]): number {
  return items.reduce((sum, item) => sum + item.calories, 0);
}

export function mealCalories(meal: MealEntry): number {
  return Math.round(itemsCalories(meal.items) * meal.portions);
}

export function mealProtein(meal: MealEntry): number {
  return round1(meal.items.reduce((sum, item) => sum + item.protein, 0) * meal.portions);
}

export function mealCarbs(meal: MealEntry): number {
  return round1(meal.items.reduce((sum, item) => sum + item.carbs, 0) * meal.portions);
}

export function mealFat(meal: MealEntry): number {
  return round1(meal.items.reduce((sum, item) => sum + item.fat, 0) * meal.portions);
}

export function scaleItem(item: FoodItem, factor: number): FoodItem {
  return {
    ...item,
    calories: Math.round(item.calories * factor),
    protein: round1(item.protein * factor),
    carbs: round1(item.carbs * factor),
    fat: round1(item.fat * factor),
  };
}

/**
 * Returns a copy of `meal` whose total calories equal `target`.
 *
 * Calories are derived from the item list, so a manual correction scales every
 * item's energy and macros by the same factor — the macro split the user
 * already saw stays intact. Meals with no nutrition yet collapse to a single
 * manual item carrying the entered calories.
 */
export function mealWithCalories(meal: MealEntry, target: number): MealEntry {
  const clamped = Math.max(0, Math.min(Math.round(target), 20000));
  const current = mealCalories(meal);

  if (current <= 0 || meal.items.length === 0) {
    return {
      ...meal,
      portions: 1,
      items: [
        {
          id: crypto.randomUUID(),
          name: meal.title.trim() === "" ? "Meal" : meal.title,
          quantity: "1 serving",
          calories: clamped,
          protein: 0,
          carbs: 0,
          fat: 0,
        },
      ],
    };
  }

  const factor = clamped / current;
  const items = meal.items.map((item) => scaleItem(item, factor));
  const next: MealEntry = { ...meal, items };

  // Per-item rounding can drift a few kcal off the requested total; push the
  // remainder into the largest item so the row shows exactly what was typed.
  const drift = clamped - mealCalories(next);
  if (drift !== 0 && next.portions > 0) {
    let largest = 0;
    for (let i = 1; i < items.length; i += 1) {
      if (items[i].calories > items[largest].calories) largest = i;
    }
    const corrected = [...items];
    corrected[largest] = {
      ...corrected[largest],
      calories: Math.max(0, corrected[largest].calories + Math.round(drift / next.portions)),
    };
    return { ...next, items: corrected };
  }

  return next;
}
