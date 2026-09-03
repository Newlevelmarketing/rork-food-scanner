import type { JSX } from "react";
import { useEffect, useState } from "react";

import { Card, PrimaryButton, Slider } from "@/components/calzy/Primitives";
import { ExerciseIcon } from "@/components/calzy/icons";
import { FullScreenSheet } from "@/components/calzy/Sheet";
import { stampOnDay } from "@/lib/dates";
import { exercisePresets, presetCalories, type ExercisePreset } from "@/lib/foods";
import { haptics } from "@/lib/haptics";
import { cn } from "@/lib/utils";
import { useAppStore } from "@/store/AppStore";

/** Log a workout to add calories back to the day's budget. */
export function ExerciseSheet({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}): JSX.Element {
  const store = useAppStore();
  const [selected, setSelected] = useState<ExercisePreset>(exercisePresets[0]);
  const [minutes, setMinutes] = useState<number>(30);

  useEffect(() => {
    if (open) return;
    setSelected(exercisePresets[0]);
    setMinutes(30);
  }, [open]);

  const burned = presetCalories(selected, minutes, store.profile.currentWeightKg);

  return (
    <FullScreenSheet
      open={open}
      onClose={onClose}
      title="Log exercise"
      footer={
        <PrimaryButton
          onClick={() => {
            store.addExercise({
              name: selected.name,
              // Every other write path honours the day the user is viewing. This one
              // stamped `new Date()`, so a workout logged while looking at a past day
              // landed on today: invisible in the list on screen, and silently
              // inflating today's budget instead.
              date: stampOnDay(store.selectedDate).toISOString(),
              minutes,
              calories: burned,
              icon: selected.icon,
            });
            haptics.success();
            onClose();
          }}
        >
          Add {burned} kcal back
        </PrimaryButton>
      }
    >
      <div className="flex flex-col gap-[18px] px-5 py-4">
        <Card radius={26} padding={20}>
          <div className="flex flex-col items-center gap-[6px] py-2">
            <ExerciseIcon name={selected.icon} size={34} className="text-flame" />
            <div className="flex items-baseline gap-1">
              <span className="metric text-[52px] leading-none text-ink">{burned}</span>
              <span className="text-[17px] font-medium text-ink-faint">kcal</span>
            </div>
            <span className="text-[14px] font-medium text-ink-soft">
              {minutes} minutes of {selected.name.toLowerCase()}
            </span>
          </div>
        </Card>

        <Card radius={22} padding={16}>
          <div className="mb-3 flex items-baseline justify-between">
            <span className="text-[15px] font-semibold text-ink">Duration</span>
            <span className="metric text-[16px] text-ink-soft">{minutes} min</span>
          </div>
          <Slider
            value={minutes}
            min={5}
            max={180}
            step={5}
            label="Duration"
            tint="hsl(var(--flame))"
            onChange={(value) => {
              haptics.selection();
              setMinutes(value);
            }}
          />
        </Card>

        <div className="grid grid-cols-3 gap-[10px]">
          {exercisePresets.map((preset) => {
            const active = preset.name === selected.name;
            return (
              <button
                key={preset.name}
                type="button"
                onClick={() => {
                  haptics.tap();
                  setSelected(preset);
                }}
                className={cn(
                  "pressable flex flex-col items-center gap-2 rounded-[20px] py-[15px] transition-colors duration-200",
                  active ? "bg-ink" : "bg-white/[0.78]",
                )}
              >
                <ExerciseIcon
                  name={preset.icon}
                  size={18}
                  className={active ? "text-white" : "text-ink"}
                />
                <span
                  className={cn("text-[12px] font-medium", active ? "text-white" : "text-ink-soft")}
                >
                  {preset.name}
                </span>
              </button>
            );
          })}
        </div>
      </div>
    </FullScreenSheet>
  );
}
