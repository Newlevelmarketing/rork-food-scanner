import raw from "@/data/foods.json";

import type { FoodItem } from "./types";
import { uid } from "./uid";

export interface FoodRecord {
  name: string;
  serving: string;
  kcal: number;
  p: number;
  c: number;
  f: number;
  tags: string;
}

/** Bundled offline food table used by the Search flow. */
export const allFoods: FoodRecord[] = raw as FoodRecord[];

export function foodToItem(record: FoodRecord): FoodItem {
  return {
    id: uid(),
    name: record.name,
    quantity: record.serving,
    calories: record.kcal,
    protein: record.p,
    carbs: record.c,
    fat: record.f,
  };
}

export function searchFoods(query: string): FoodRecord[] {
  const trimmed = query.trim().toLowerCase();
  if (!trimmed) return allFoods.slice(0, 24);

  return allFoods
    .filter(
      (food) =>
        food.name.toLowerCase().includes(trimmed) || food.tags.toLowerCase().includes(trimmed),
    )
    .sort((lhs, rhs) => {
      const l = lhs.name.toLowerCase().includes(trimmed) ? 0 : 1;
      const r = rhs.name.toLowerCase().includes(trimmed) ? 0 : 1;
      return l === r ? lhs.name.localeCompare(rhs.name) : l - r;
    });
}

export interface ExercisePreset {
  name: string;
  icon: string;
  /** Calories burned per minute for an 80 kg person. */
  perMinute: number;
}

export const exercisePresets: ExercisePreset[] = [
  { name: "Walking", icon: "Footprints", perMinute: 4.5 },
  { name: "Running", icon: "PersonStanding", perMinute: 11.5 },
  { name: "Cycling", icon: "Bike", perMinute: 9.0 },
  { name: "Weights", icon: "Dumbbell", perMinute: 6.5 },
  { name: "HIIT", icon: "Zap", perMinute: 12.5 },
  { name: "Swimming", icon: "Waves", perMinute: 10.0 },
  { name: "Yoga", icon: "Flower2", perMinute: 3.5 },
  { name: "Football", icon: "Goal", perMinute: 9.5 },
  { name: "Tennis", icon: "Trophy", perMinute: 8.0 },
  { name: "Rowing", icon: "Sailboat", perMinute: 10.5 },
  { name: "Boxing", icon: "Swords", perMinute: 11.0 },
  { name: "Hiking", icon: "Mountain", perMinute: 7.0 },
];

export function presetCalories(preset: ExercisePreset, minutes: number, weightKg: number): number {
  return Math.round(preset.perMinute * minutes * (weightKg / 80));
}
