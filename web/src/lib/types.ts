/** Domain models — mirrors ios-calzy/Calzy/Models. */

export type Sex = "male" | "female";
export type ActivityLevel = "sedentary" | "light" | "moderate" | "high" | "athlete";
export type GoalDirection = "lose" | "maintain" | "gain";
export type UnitSystem = "metric" | "imperial";
export type MealSlot = "breakfast" | "lunch" | "dinner" | "snack";
export type EntrySource = "photo" | "text" | "search" | "saved" | "manual";

export interface NutritionTargets {
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
}

/** Everything Calzy knows about the person using it, plus their daily targets. */
export interface UserProfile {
  name: string;
  sex: Sex;
  birthYear: number;
  heightCm: number;
  startWeightKg: number;
  currentWeightKg: number;
  goalWeightKg: number;
  activity: ActivityLevel;
  goal: GoalDirection;
  weeklyRateKg: number;
  units: UnitSystem;
  usesCustomTargets: boolean;
  customCalories: number;
  customProtein: number;
  customCarbs: number;
  customFat: number;
  waterGoalMl: number;
  jesterMode: boolean;
  remindersEnabled: boolean;
  reminderTimes: number[];
  isPro: boolean;
  hasOnboarded: boolean;
  /**
   * Language code from the catalogue in `i18n.ts`. Optional so profiles saved
   * before the language picker shipped still load; `undefined` means "follow
   * the browser".
   */
  languageCode?: string;
}

/** One food item inside a logged meal. */
export interface FoodItem {
  id: string;
  name: string;
  quantity: string;
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
}

/** A logged meal, optionally backed by a photo the user scanned. */
export interface MealEntry {
  id: string;
  title: string;
  /** ISO-8601 timestamp. */
  date: string;
  slot: MealSlot;
  source: EntrySource;
  items: FoodItem[];
  portions: number;
  /** Downscaled JPEG data URL. */
  photo?: string;
  healthScore: number;
  note?: string;
  quip?: string;
}

export interface ExerciseEntry {
  id: string;
  name: string;
  date: string;
  minutes: number;
  calories: number;
  icon: string;
}

export interface WaterEntry {
  id: string;
  date: string;
  milliliters: number;
}

export interface WeightEntry {
  id: string;
  date: string;
  kilograms: number;
}

export interface ProgressPhoto {
  id: string;
  date: string;
  photo: string;
  weightKg?: number;
}

/** A food the user bookmarked for one-tap re-logging. */
export interface SavedFood {
  id: string;
  title: string;
  items: FoodItem[];
  slot: MealSlot;
}

export interface AppData {
  profile: UserProfile;
  meals: MealEntry[];
  exercises: ExerciseEntry[];
  water: WaterEntry[];
  weights: WeightEntry[];
  photos: ProgressPhoto[];
  saved: SavedFood[];
}

export const defaultProfile: UserProfile = {
  name: "",
  sex: "male",
  birthYear: 1996,
  heightCm: 178,
  startWeightKg: 82,
  currentWeightKg: 82,
  goalWeightKg: 76,
  activity: "light",
  goal: "lose",
  weeklyRateKg: 0.5,
  units: "metric",
  usesCustomTargets: false,
  customCalories: 2200,
  customProtein: 140,
  customCarbs: 240,
  customFat: 70,
  waterGoalMl: 2500,
  jesterMode: false,
  remindersEnabled: true,
  reminderTimes: [9, 13, 19],
  isPro: false,
  hasOnboarded: false,
};

export const emptyData: AppData = {
  profile: defaultProfile,
  meals: [],
  exercises: [],
  water: [],
  weights: [],
  photos: [],
  saved: [],
};
