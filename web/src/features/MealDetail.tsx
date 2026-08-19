import { Bookmark, Leaf, Minus, Pencil, Plus, Sparkles, Theater, Trash2 } from "lucide-react";
import type { JSX } from "react";
import { useEffect, useState } from "react";

import { Card, MacroBar, PrimaryButton } from "@/components/calzy/Primitives";
import { FullScreenSheet } from "@/components/calzy/Sheet";
import { shortTime } from "@/lib/dates";
import { haptics } from "@/lib/haptics";
import { mealCalories, mealCarbs, mealFat, mealProtein, slotMeta, slotOrder } from "@/lib/nutrition";
import type { MealEntry, MealSlot } from "@/lib/types";
import { cn } from "@/lib/utils";
import { useAppStore } from "@/store/AppStore";

/** Detail sheet for an already-logged meal: retune portions, slot or delete it. */
export function MealDetail({
  meal,
  onClose,
  onEdit,
}: {
  meal: MealEntry | null;
  onClose: () => void;
  onEdit: (meal: MealEntry) => void;
}): JSX.Element {
  const store = useAppStore();
  const [portions, setPortions] = useState<number>(1);
  const [slot, setSlot] = useState<MealSlot>("lunch");
  const [confirmDelete, setConfirmDelete] = useState<boolean>(false);

  useEffect(() => {
    if (!meal) return;
    setPortions(meal.portions);
    setSlot(meal.slot);
    setConfirmDelete(false);
  }, [meal]);

  if (!meal) return <FullScreenSheet open={false} onClose={onClose}>{null}</FullScreenSheet>;

  const preview: MealEntry = { ...meal, portions, slot };
  const calories = mealCalories(preview);
  const protein = mealProtein(preview);
  const carbs = mealCarbs(preview);
  const fat = mealFat(preview);
  const targets = store.targets;
  const score = meal.healthScore;
  const scoreToken = score >= 8 ? "--mint" : score >= 5 ? "--fat" : "--protein";
  const dirty = portions !== meal.portions || slot !== meal.slot;
  const saved = store.isSaved(meal.title);

  return (
    <FullScreenSheet
      open
      onClose={onClose}
      title={slotMeta[slot].label}
      leading={
        <button type="button" onClick={onClose} className="pressable text-[15px] font-medium text-ink-soft">
          Done
        </button>
      }
      trailing={
        <div className="flex items-center gap-4">
          <button
            type="button"
            onClick={() => {
              haptics.tap();
              onEdit(meal);
            }}
            aria-label="Edit meal name or calories"
            className="pressable text-ink"
          >
            <Pencil size={18} strokeWidth={2.4} />
          </button>
          <button
            type="button"
            onClick={() => {
              store.toggleSaved(meal.title, meal.items, slot);
              haptics.tap();
            }}
            aria-label={saved ? "Remove bookmark" : "Bookmark meal"}
            className="pressable text-ink"
          >
            <Bookmark size={19} fill={saved ? "currentColor" : "none"} strokeWidth={2.2} />
          </button>
        </div>
      }
      footer={
        confirmDelete ? (
          <div className="flex gap-3">
            <button
              type="button"
              onClick={() => setConfirmDelete(false)}
              className="pressable flex-1 rounded-full bg-black/[0.06] py-[15px] text-[15px] font-semibold text-ink"
            >
              Keep it
            </button>
            <button
              type="button"
              onClick={() => {
                store.deleteMeal(meal.id);
                haptics.warning();
                onClose();
              }}
              className="pressable flex-1 rounded-full bg-protein py-[15px] text-[15px] font-semibold text-white"
            >
              Delete meal
            </button>
          </div>
        ) : dirty ? (
          <PrimaryButton
            onClick={() => {
              store.updateMeal({ ...meal, portions, slot });
              haptics.success();
              onClose();
            }}
          >
            Save changes
          </PrimaryButton>
        ) : (
          <button
            type="button"
            onClick={() => setConfirmDelete(true)}
            className="pressable flex w-full items-center justify-center gap-2 rounded-full bg-protein/10 py-[15px] text-[15px] font-semibold text-protein"
          >
            <Trash2 size={16} strokeWidth={2.4} />
            Delete meal
          </button>
        )
      }
    >
      <div className="animate-rise-in flex flex-col gap-4 px-5 py-4">
        {meal.photo && (
          <div className="h-[210px] overflow-hidden rounded-[26px] bg-well">
            <img src={meal.photo} alt="" className="h-full w-full object-cover" />
          </div>
        )}

        <Card radius={26} padding={20}>
          <div className="flex flex-col items-center gap-3">
            <h2 className="text-center text-[21px] font-bold text-ink">{meal.title}</h2>
            <span className="text-[12px] font-medium text-ink-faint">
              Logged at {shortTime(meal.date)}
            </span>
            <div className="flex items-baseline gap-1">
              <span className="metric text-[46px] leading-none text-ink">{calories}</span>
              <span className="text-[16px] font-medium text-ink-faint">kcal</span>
            </div>
            <span
              className="flex items-center gap-[6px] rounded-full px-3 py-[6px] text-[12px] font-semibold"
              style={{
                color: `hsl(var(${scoreToken}))`,
                backgroundColor: `hsl(var(${scoreToken}) / 0.12)`,
              }}
            >
              <Leaf size={11} strokeWidth={2.8} />
              Health score {score}/10
            </span>
          </div>
        </Card>

        <Card radius={22} padding={16}>
          <div className="flex flex-col gap-[14px]">
            <MacroLine
              name="Protein"
              value={protein}
              goal={targets.protein}
              color="hsl(var(--protein))"
            />
            <MacroLine name="Carbs" value={carbs} goal={targets.carbs} color="hsl(var(--carbs))" />
            <MacroLine name="Fat" value={fat} goal={targets.fat} color="hsl(var(--fat))" />
          </div>
        </Card>

        <Card radius={22} padding={16}>
          <div className="flex items-center justify-between">
            <span className="text-[15px] font-semibold text-ink">Portions</span>
            <div className="flex items-center gap-4">
              <StepButton
                label="Decrease portions"
                onClick={() => setPortions((v) => Math.max(0.25, Math.round((v - 0.25) * 100) / 100))}
              >
                <Minus size={14} strokeWidth={3} />
              </StepButton>
              <span className="metric min-w-[42px] text-center text-[19px] text-ink">
                {portions % 1 === 0 ? portions.toFixed(0) : portions.toFixed(2).replace(/0$/, "")}
              </span>
              <StepButton
                label="Increase portions"
                onClick={() => setPortions((v) => Math.min(10, Math.round((v + 0.25) * 100) / 100))}
              >
                <Plus size={14} strokeWidth={3} />
              </StepButton>
            </div>
          </div>

          <div className="my-[14px] h-px calzy-hairline" />

          <div className="flex items-center justify-between gap-3">
            <span className="text-[15px] font-semibold text-ink">Meal</span>
            <div className="flex gap-[6px]">
              {slotOrder.map((option) => (
                <button
                  key={option}
                  type="button"
                  onClick={() => {
                    haptics.selection();
                    setSlot(option);
                  }}
                  className={cn(
                    "pressable rounded-full px-[10px] py-[6px] text-[12px] font-semibold transition-colors",
                    slot === option ? "bg-ink text-white" : "bg-black/[0.05] text-ink-soft",
                  )}
                >
                  {slotMeta[option].label}
                </button>
              ))}
            </div>
          </div>
        </Card>

        <Card radius={22} padding={16}>
          {meal.items.map((item, index) => (
            <div key={item.id}>
              <div className="flex items-center gap-3 py-3">
                <div className="min-w-0 flex-1">
                  <p className="truncate text-[15px] font-semibold text-ink">{item.name}</p>
                  <p className="text-[12px] text-ink-faint">{item.quantity}</p>
                </div>
                <span className="metric shrink-0 text-[16px] text-ink-soft">
                  {Math.round(item.calories * portions)}
                </span>
              </div>
              {index < meal.items.length - 1 && <div className="h-px calzy-hairline" />}
            </div>
          ))}
        </Card>

        {meal.quip && (
          <Card radius={20} padding={14}>
            <div className="flex items-start gap-[10px]">
              {store.profile.jesterMode ? (
                <Theater size={15} className="mt-[2px] shrink-0 text-fat" strokeWidth={2.4} />
              ) : (
                <Sparkles size={15} className="mt-[2px] shrink-0 text-plum" strokeWidth={2.4} />
              )}
              <p className="text-[14px] font-medium text-ink-soft">{meal.quip}</p>
            </div>
          </Card>
        )}
      </div>
    </FullScreenSheet>
  );
}

function MacroLine({
  name,
  value,
  goal,
  color,
}: {
  name: string;
  value: number;
  goal: number;
  color: string;
}): JSX.Element {
  return (
    <div className="flex flex-col gap-[6px]">
      <div className="flex items-baseline justify-between">
        <span className="text-[13px] font-medium text-ink-soft">{name}</span>
        <span className="text-[13px] font-semibold text-ink">
          {Math.round(value)}g
          <span className="font-medium text-ink-faint"> of {goal}g daily</span>
        </span>
      </div>
      <MacroBar progress={goal > 0 ? value / goal : 0} color={color} />
    </div>
  );
}

function StepButton({
  children,
  label,
  onClick,
}: {
  children: React.ReactNode;
  label: string;
  onClick: () => void;
}): JSX.Element {
  return (
    <button
      type="button"
      aria-label={label}
      onClick={() => {
        haptics.selection();
        onClick();
      }}
      className="pressable grid h-[34px] w-[34px] place-items-center rounded-full bg-well text-ink"
    >
      {children}
    </button>
  );
}
