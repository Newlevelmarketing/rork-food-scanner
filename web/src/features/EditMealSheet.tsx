import { CheckCircle2, Minus, Plus, X } from "lucide-react";
import type { JSX } from "react";
import { useEffect, useMemo, useState } from "react";

import { Card, PrimaryButton } from "@/components/calzy/Primitives";
import { FullScreenSheet } from "@/components/calzy/Sheet";
import { haptics } from "@/lib/haptics";
import {
  mealCalories,
  mealCarbs,
  mealFat,
  mealProtein,
  mealWithCalories,
} from "@/lib/nutrition";
import type { MealEntry } from "@/lib/types";
import { cn } from "@/lib/utils";
import { useAppStore } from "@/store/AppStore";

const MAX_CALORIES = 20000;

/**
 * Quick-correction form for an already-logged meal.
 *
 * AI estimates and database matches are close but rarely exact, so this lets a
 * user fix the two things they actually notice on the timeline: what the meal
 * is called and how many calories it cost them. Macros scale with the calorie
 * edit so the split they already saw stays believable.
 */
export function EditMealSheet({
  meal,
  onClose,
  onSaved,
}: {
  meal: MealEntry | null;
  onClose: () => void;
  onSaved?: (meal: MealEntry) => void;
}): JSX.Element | null {
  const store = useAppStore();
  const [title, setTitle] = useState<string>("");
  const [calories, setCalories] = useState<number>(0);

  useEffect(() => {
    if (!meal) return;
    setTitle(meal.title);
    setCalories(mealCalories(meal));
  }, [meal]);

  const originalCalories = meal ? mealCalories(meal) : 0;

  const preview = useMemo<MealEntry | null>(
    () => (meal ? mealWithCalories(meal, calories) : null),
    [meal, calories],
  );

  if (!meal || !preview) return null;

  const trimmed = title.trim();
  const canSave = trimmed !== "" && (trimmed !== meal.title || calories !== originalCalories);
  const delta = calories - originalCalories;

  const step = (amount: number): void => {
    haptics.selection();
    setCalories((current) => Math.max(0, Math.min(MAX_CALORIES, current + amount)));
  };

  const save = (): void => {
    if (!canSave) return;
    const updated: MealEntry = { ...mealWithCalories(meal, calories), title: trimmed };
    store.updateMeal(updated);
    haptics.success();
    onSaved?.(updated);
    onClose();
  };

  return (
    <FullScreenSheet
      open
      onClose={onClose}
      title="Edit meal"
      footer={
        <PrimaryButton onClick={save} disabled={!canSave}>
          <CheckCircle2 size={18} strokeWidth={2.4} />
          Save changes
        </PrimaryButton>
      }
    >
      <div className="flex flex-col gap-[14px] px-5 pb-8 pt-[6px]">
        {/* Name */}
        <Card radius={22} padding={16}>
          <label
            htmlFor="edit-meal-name"
            className="mb-[9px] block text-[13px] font-semibold text-ink-soft"
          >
            Meal name
          </label>
          <div className="flex items-center gap-[10px] rounded-[16px] bg-well px-[14px] py-[13px]">
            <input
              id="edit-meal-name"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="e.g. Chicken burrito bowl"
              autoComplete="off"
              className="min-w-0 flex-1 bg-transparent text-[17px] font-semibold text-ink outline-none placeholder:font-medium placeholder:text-ink-faint"
            />
            {title !== "" && (
              <button
                type="button"
                onClick={() => {
                  haptics.selection();
                  setTitle("");
                }}
                aria-label="Clear meal name"
                className="pressable shrink-0 text-ink-faint"
              >
                <X size={16} strokeWidth={2.6} />
              </button>
            )}
          </div>
        </Card>

        {/* Calories */}
        <Card radius={22} padding={16}>
          <div className="flex items-center justify-between">
            <span className="text-[13px] font-semibold text-ink-soft">Calories</span>
            {delta !== 0 && (
              <span
                className={cn(
                  "animate-pop-in rounded-full px-2 py-1 text-[12px] font-semibold",
                  delta > 0 ? "bg-flame/[0.12] text-flame" : "bg-mint/[0.12] text-mint",
                )}
              >
                {delta > 0 ? `+${delta}` : delta} kcal
              </span>
            )}
          </div>

          <div className="mt-[14px] flex items-center justify-center gap-[18px]">
            <StepButton
              icon={Minus}
              label="Decrease calories"
              disabled={calories <= 0}
              onClick={() => step(-10)}
            />

            <div className="flex flex-col items-center gap-[2px]">
              <input
                inputMode="numeric"
                pattern="[0-9]*"
                aria-label="Calories"
                value={`${calories}`}
                onChange={(event) => {
                  const digits = event.target.value.replace(/[^0-9]/g, "");
                  setCalories(Math.min(MAX_CALORIES, Number(digits === "" ? 0 : digits)));
                }}
                className="metric w-[140px] bg-transparent text-center text-[40px] text-ink outline-none"
              />
              <span className="text-[12px] font-medium text-ink-faint">kcal</span>
            </div>

            <StepButton
              icon={Plus}
              label="Increase calories"
              disabled={calories >= MAX_CALORIES}
              onClick={() => step(10)}
            />
          </div>

          <div className="mt-[14px] flex gap-2">
            {[-100, -50, 50, 100].map((amount) => (
              <button
                key={amount}
                type="button"
                onClick={() => step(amount)}
                className="pressable flex-1 rounded-full bg-well py-[9px] text-[13px] font-semibold text-ink"
              >
                {amount > 0 ? `+${amount}` : amount}
              </button>
            ))}
          </div>
        </Card>

        {/* Macro preview */}
        <Card radius={22} padding={16}>
          <span className="text-[13px] font-semibold text-ink-soft">Macros scale to match</span>
          <div className="mt-3 flex gap-[10px]">
            <MacroPill name="Protein" value={mealProtein(preview)} token="--protein" />
            <MacroPill name="Carbs" value={mealCarbs(preview)} token="--carbs" />
            <MacroPill name="Fat" value={mealFat(preview)} token="--fat" />
          </div>
        </Card>
      </div>
    </FullScreenSheet>
  );
}

function StepButton({
  icon: Icon,
  label,
  disabled,
  onClick,
}: {
  icon: typeof Plus;
  label: string;
  disabled: boolean;
  onClick: () => void;
}): JSX.Element {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      className="pressable grid h-10 w-10 shrink-0 place-items-center rounded-full bg-well text-ink disabled:text-ink-faint"
    >
      <Icon size={15} strokeWidth={3} />
    </button>
  );
}

function MacroPill({
  name,
  value,
  token,
}: {
  name: string;
  value: number;
  token: string;
}): JSX.Element {
  return (
    <div
      className="flex flex-1 flex-col items-center gap-1 rounded-[16px] py-[11px]"
      style={{ backgroundColor: `hsl(var(${token}) / 0.09)` }}
    >
      <span className="metric text-[18px] text-ink">{Math.round(value)}g</span>
      <span className="text-[11px] font-medium text-ink-soft">{name}</span>
    </div>
  );
}
