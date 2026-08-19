import type { JSX } from "react";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";

import { addDays, isSameDay, startOfDay } from "@/lib/dates";
import {
  mealCalories,
  mealCarbs,
  mealFat,
  mealProtein,
  targetsOf,
} from "@/lib/nutrition";
import {
  emptyData,
  type AppData,
  type ExerciseEntry,
  type FoodItem,
  type MealEntry,
  type MealSlot,
  type NutritionTargets,
  type ProgressPhoto,
  type SavedFood,
  type UserProfile,
  type WeightEntry,
} from "@/lib/types";
import { uid } from "@/lib/uid";

const STORAGE_KEY = "calzy-data-v1";

/**
 * Filters entries to a single day and orders them newest first.
 *
 * Entries sharing a timestamp fall back to insertion order (latest logged
 * wins), so the list stays fixed across re-renders instead of leaning on the
 * original array order, which would surface the oldest of a tie at the top.
 */
function newestFirst<T extends { date: string }>(entries: T[], date: Date): T[] {
  return entries
    .map((entry, index) => ({ entry, index }))
    .filter(({ entry }) => isSameDay(entry.date, date))
    .sort((a, b) => {
      const delta = new Date(b.entry.date).getTime() - new Date(a.entry.date).getTime();
      return delta !== 0 ? delta : b.index - a.index;
    })
    .map(({ entry }) => entry);
}

function load(): AppData {
  if (typeof window === "undefined") return emptyData;
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return emptyData;
    const parsed = JSON.parse(raw) as Partial<AppData>;
    return {
      ...emptyData,
      ...parsed,
      profile: { ...emptyData.profile, ...(parsed.profile ?? {}) },
      meals: parsed.meals ?? [],
      exercises: parsed.exercises ?? [],
      water: parsed.water ?? [],
      weights: parsed.weights ?? [],
      photos: parsed.photos ?? [],
      saved: parsed.saved ?? [],
    };
  } catch (error) {
    console.warn("[Calzy] could not restore saved data", error);
    return emptyData;
  }
}

export interface AverageCalories {
  average: number;
  logged: number;
  total: number;
}

export interface AppStoreValue {
  data: AppData;
  profile: UserProfile;
  targets: NutritionTargets;
  selectedDate: Date;
  setSelectedDate: (date: Date) => void;
  setProfile: (update: Partial<UserProfile>) => void;
  completeOnboarding: (profile: UserProfile) => void;

  mealsOn: (date: Date) => MealEntry[];
  exercisesOn: (date: Date) => ExerciseEntry[];
  caloriesEaten: (date: Date) => number;
  caloriesBurned: (date: Date) => number;
  proteinOn: (date: Date) => number;
  carbsOn: (date: Date) => number;
  fatOn: (date: Date) => number;
  waterOn: (date: Date) => number;
  hasLogs: (date: Date) => boolean;
  streak: number;
  averageCalories: AverageCalories;

  addMeal: (meal: Omit<MealEntry, "id">) => void;
  updateMeal: (meal: MealEntry) => void;
  deleteMeal: (id: string) => void;
  addExercise: (entry: Omit<ExerciseEntry, "id">) => void;
  deleteExercise: (id: string) => void;
  addWater: (ml: number, date: Date) => void;
  undoWater: (date: Date) => void;

  weightEntries: WeightEntry[];
  weightOn: (date: Date) => WeightEntry | undefined;
  logWeight: (kg: number, date?: Date) => void;
  deleteWeight: (id: string) => void;

  toggleSaved: (title: string, items: FoodItem[], slot: MealSlot) => void;
  isSaved: (title: string) => boolean;
  deleteSaved: (id: string) => void;

  photos: ProgressPhoto[];
  addProgressPhoto: (photo: string) => void;
  deletePhoto: (id: string) => void;

  eraseAll: () => void;
}

const AppStoreContext = createContext<AppStoreValue | null>(null);

export function AppStoreProvider({ children }: { children: ReactNode }): JSX.Element {
  const [data, setData] = useState<AppData>(load);
  const [selectedDate, setSelectedDate] = useState<Date>(() => startOfDay(new Date()));
  const saveTimer = useRef<number | null>(null);

  // Debounced persistence so slider drags don't thrash localStorage.
  useEffect(() => {
    if (saveTimer.current !== null) window.clearTimeout(saveTimer.current);
    saveTimer.current = window.setTimeout(() => {
      try {
        window.localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
      } catch (error) {
        console.warn("[Calzy] could not save data — storage may be full", error);
      }
    }, 220);
    return () => {
      if (saveTimer.current !== null) window.clearTimeout(saveTimer.current);
    };
  }, [data]);

  const setProfile = useCallback((update: Partial<UserProfile>) => {
    setData((current) => ({ ...current, profile: { ...current.profile, ...update } }));
  }, []);

  const completeOnboarding = useCallback((profile: UserProfile) => {
    setData((current) => ({
      ...current,
      profile: { ...profile, hasOnboarded: true },
      weights:
        current.weights.length > 0
          ? current.weights
          : [{ id: uid(), date: new Date().toISOString(), kilograms: profile.currentWeightKg }],
    }));
  }, []);

  const mealsOn = useCallback(
    (date: Date): MealEntry[] => newestFirst(data.meals, date),
    [data.meals],
  );

  const exercisesOn = useCallback(
    (date: Date): ExerciseEntry[] => newestFirst(data.exercises, date),
    [data.exercises],
  );

  const caloriesEaten = useCallback(
    (date: Date): number => mealsOn(date).reduce((sum, meal) => sum + mealCalories(meal), 0),
    [mealsOn],
  );

  const caloriesBurned = useCallback(
    (date: Date): number => exercisesOn(date).reduce((sum, entry) => sum + entry.calories, 0),
    [exercisesOn],
  );

  const proteinOn = useCallback(
    (date: Date): number => mealsOn(date).reduce((sum, meal) => sum + mealProtein(meal), 0),
    [mealsOn],
  );

  const carbsOn = useCallback(
    (date: Date): number => mealsOn(date).reduce((sum, meal) => sum + mealCarbs(meal), 0),
    [mealsOn],
  );

  const fatOn = useCallback(
    (date: Date): number => mealsOn(date).reduce((sum, meal) => sum + mealFat(meal), 0),
    [mealsOn],
  );

  const waterOn = useCallback(
    (date: Date): number =>
      data.water
        .filter((entry) => isSameDay(entry.date, date))
        .reduce((sum, entry) => sum + entry.milliliters, 0),
    [data.water],
  );

  const hasLogs = useCallback(
    (date: Date): boolean => mealsOn(date).length > 0 || exercisesOn(date).length > 0,
    [mealsOn, exercisesOn],
  );

  /** Consecutive days (ending today or yesterday) with at least one logged meal. */
  const streak = useMemo(() => {
    let cursor = startOfDay(new Date());
    if (mealsOn(cursor).length === 0) {
      const yesterday = addDays(cursor, -1);
      if (mealsOn(yesterday).length === 0) return 0;
      cursor = yesterday;
    }
    let count = 0;
    while (mealsOn(cursor).length > 0) {
      count += 1;
      cursor = addDays(cursor, -1);
    }
    return count;
  }, [mealsOn]);

  /** Average calories over the last 7 days, counting only days with logs. */
  const averageCalories = useMemo<AverageCalories>(() => {
    const today = startOfDay(new Date());
    let total = 0;
    let logged = 0;
    for (let offset = 0; offset < 7; offset += 1) {
      const value = caloriesEaten(addDays(today, -offset));
      if (value > 0) {
        total += value;
        logged += 1;
      }
    }
    return { average: logged === 0 ? 0 : Math.round(total / logged), logged, total: 7 };
  }, [caloriesEaten]);

  const addMeal = useCallback((meal: Omit<MealEntry, "id">) => {
    setData((current) => ({ ...current, meals: [...current.meals, { ...meal, id: uid() }] }));
  }, []);

  const updateMeal = useCallback((meal: MealEntry) => {
    setData((current) => ({
      ...current,
      meals: current.meals.map((existing) => (existing.id === meal.id ? meal : existing)),
    }));
  }, []);

  const deleteMeal = useCallback((id: string) => {
    setData((current) => ({ ...current, meals: current.meals.filter((meal) => meal.id !== id) }));
  }, []);

  const addExercise = useCallback((entry: Omit<ExerciseEntry, "id">) => {
    setData((current) => ({
      ...current,
      exercises: [...current.exercises, { ...entry, id: uid() }],
    }));
  }, []);

  const deleteExercise = useCallback((id: string) => {
    setData((current) => ({
      ...current,
      exercises: current.exercises.filter((entry) => entry.id !== id),
    }));
  }, []);

  const addWater = useCallback((ml: number, date: Date) => {
    setData((current) => ({
      ...current,
      water: [...current.water, { id: uid(), date: date.toISOString(), milliliters: ml }],
    }));
  }, []);

  const undoWater = useCallback((date: Date) => {
    setData((current) => {
      const index = current.water.map((entry) => isSameDay(entry.date, date)).lastIndexOf(true);
      if (index === -1) return current;
      const water = [...current.water];
      water.splice(index, 1);
      return { ...current, water };
    });
  }, []);

  const weightEntries = useMemo(
    () => [...data.weights].sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime()),
    [data.weights],
  );

  const weightOn = useCallback(
    (date: Date): WeightEntry | undefined =>
      data.weights.find((entry) => isSameDay(entry.date, date)),
    [data.weights],
  );

  const logWeight = useCallback((kg: number, date: Date = new Date()) => {
    const rounded = Math.round(kg * 10) / 10;
    setData((current) => {
      const index = current.weights.findIndex((entry) => isSameDay(entry.date, date));
      const weights = [...current.weights];
      if (index >= 0) {
        weights[index] = { ...weights[index], kilograms: rounded };
      } else {
        weights.push({ id: uid(), date: date.toISOString(), kilograms: rounded });
      }
      const latest = weights.reduce<number>(
        (max, entry) => Math.max(max, new Date(entry.date).getTime()),
        0,
      );
      const isLatest = new Date(date).getTime() >= latest;
      return {
        ...current,
        weights,
        profile: isLatest ? { ...current.profile, currentWeightKg: rounded } : current.profile,
      };
    });
  }, []);

  const deleteWeight = useCallback((id: string) => {
    setData((current) => ({
      ...current,
      weights: current.weights.filter((entry) => entry.id !== id),
    }));
  }, []);

  const toggleSaved = useCallback((title: string, items: FoodItem[], slot: MealSlot) => {
    setData((current) => {
      const index = current.saved.findIndex(
        (food) => food.title.toLowerCase() === title.toLowerCase(),
      );
      if (index >= 0) {
        return { ...current, saved: current.saved.filter((_, i) => i !== index) };
      }
      return { ...current, saved: [...current.saved, { id: uid(), title, items, slot }] };
    });
  }, []);

  const isSaved = useCallback(
    (title: string): boolean =>
      data.saved.some((food) => food.title.toLowerCase() === title.toLowerCase()),
    [data.saved],
  );

  const deleteSaved = useCallback((id: string) => {
    setData((current) => ({ ...current, saved: current.saved.filter((food) => food.id !== id) }));
  }, []);

  const photos = useMemo(
    () => [...data.photos].sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()),
    [data.photos],
  );

  const addProgressPhoto = useCallback((photo: string) => {
    setData((current) => ({
      ...current,
      photos: [
        ...current.photos,
        {
          id: uid(),
          date: new Date().toISOString(),
          photo,
          weightKg: current.profile.currentWeightKg,
        },
      ],
    }));
  }, []);

  const deletePhoto = useCallback((id: string) => {
    setData((current) => ({ ...current, photos: current.photos.filter((p) => p.id !== id) }));
  }, []);

  const eraseAll = useCallback(() => {
    setData(emptyData);
    setSelectedDate(startOfDay(new Date()));
  }, []);

  const targets = useMemo(() => targetsOf(data.profile), [data.profile]);

  const value = useMemo<AppStoreValue>(
    () => ({
      data,
      profile: data.profile,
      targets,
      selectedDate,
      setSelectedDate,
      setProfile,
      completeOnboarding,
      mealsOn,
      exercisesOn,
      caloriesEaten,
      caloriesBurned,
      proteinOn,
      carbsOn,
      fatOn,
      waterOn,
      hasLogs,
      streak,
      averageCalories,
      addMeal,
      updateMeal,
      deleteMeal,
      addExercise,
      deleteExercise,
      addWater,
      undoWater,
      weightEntries,
      weightOn,
      logWeight,
      deleteWeight,
      toggleSaved,
      isSaved,
      deleteSaved,
      photos,
      addProgressPhoto,
      deletePhoto,
      eraseAll,
    }),
    [
      data,
      targets,
      selectedDate,
      setProfile,
      completeOnboarding,
      mealsOn,
      exercisesOn,
      caloriesEaten,
      caloriesBurned,
      proteinOn,
      carbsOn,
      fatOn,
      waterOn,
      hasLogs,
      streak,
      averageCalories,
      addMeal,
      updateMeal,
      deleteMeal,
      addExercise,
      deleteExercise,
      addWater,
      undoWater,
      weightEntries,
      weightOn,
      logWeight,
      deleteWeight,
      toggleSaved,
      isSaved,
      deleteSaved,
      photos,
      addProgressPhoto,
      deletePhoto,
      eraseAll,
    ],
  );

  return <AppStoreContext.Provider value={value}>{children}</AppStoreContext.Provider>;
}

export function useAppStore(): AppStoreValue {
  const context = useContext(AppStoreContext);
  if (!context) throw new Error("useAppStore must be used inside AppStoreProvider");
  return context;
}
