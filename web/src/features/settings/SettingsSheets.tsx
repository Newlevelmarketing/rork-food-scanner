import {
  Check,
  CircleCheck,
  Coffee,
  Equal,
  Leaf,
  MoonStar,
  Sun,
  Sunrise,
  BedDouble,
  TrendingDown,
  TrendingUp,
  type LucideIcon,
} from "lucide-react";
import type { JSX } from "react";
import { useEffect, useState, type ReactNode } from "react";

import { Card, PrimaryButton, Segmented, Slider, Toggle } from "@/components/calzy/Primitives";
import { ActivityIcon } from "@/components/calzy/icons";
import { FullScreenSheet } from "@/components/calzy/Sheet";
import { addDays, longMonthDay } from "@/lib/dates";
import { haptics } from "@/lib/haptics";
import { activityMeta, activityOrder, goalOrder, targetsOf } from "@/lib/nutrition";
import type { ActivityLevel, GoalDirection, Sex } from "@/lib/types";
import { cn } from "@/lib/utils";
import { useAppStore } from "@/store/AppStore";

interface SheetProps {
  open: boolean;
  onClose: () => void;
}

/** Shared settings sub-screen chrome with a save button that commits on exit. */
function Scaffold({
  open,
  onClose,
  title,
  onSave,
  children,
}: SheetProps & { title: string; onSave: () => void; children: ReactNode }): JSX.Element {
  return (
    <FullScreenSheet
      open={open}
      onClose={onClose}
      title={title}
      leading={
        <button type="button" onClick={onClose} className="pressable text-[15px] font-medium text-ink-soft">
          Back
        </button>
      }
      footer={
        <PrimaryButton
          onClick={() => {
            onSave();
            haptics.success();
            onClose();
          }}
        >
          Save changes
        </PrimaryButton>
      }
    >
      <div className="flex flex-col gap-4 px-5 py-4">{children}</div>
    </FullScreenSheet>
  );
}

function Label({ children }: { children: ReactNode }): JSX.Element {
  return <span className="text-[15px] font-medium text-ink-soft">{children}</span>;
}

function SliderRow({
  title,
  value,
  min,
  max,
  step,
  display,
  tint = "hsl(var(--ink))",
  disabled,
  onChange,
}: {
  title: string;
  value: number;
  min: number;
  max: number;
  step: number;
  display: string;
  tint?: string;
  disabled?: boolean;
  onChange: (value: number) => void;
}): JSX.Element {
  return (
    <div className="flex flex-col gap-[7px]">
      <div className="flex items-baseline justify-between">
        <Label>{title}</Label>
        <span className="metric text-[16px]" style={{ color: tint }}>
          {display}
        </span>
      </div>
      <Slider
        value={value}
        min={min}
        max={max}
        step={step}
        tint={tint}
        disabled={disabled}
        onChange={onChange}
        label={title}
      />
    </div>
  );
}

// MARK: - Account

export function AccountSheet({ open, onClose }: SheetProps): JSX.Element {
  const store = useAppStore();
  const [name, setName] = useState<string>(store.profile.name);
  const [sex, setSex] = useState<Sex>(store.profile.sex);
  const [birthYear, setBirthYear] = useState<number>(store.profile.birthYear);

  useEffect(() => {
    if (!open) return;
    setName(store.profile.name);
    setSex(store.profile.sex);
    setBirthYear(store.profile.birthYear);
  }, [open, store.profile]);

  return (
    <Scaffold
      open={open}
      onClose={onClose}
      title="Account"
      onSave={() => store.setProfile({ name, sex, birthYear })}
    >
      <Card radius={22} padding={0}>
        <div className="flex items-center justify-between p-4">
          <Label>Name</Label>
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="Your name"
            className="min-w-0 flex-1 bg-transparent text-right text-[16px] font-medium text-ink outline-none placeholder:text-ink-faint"
          />
        </div>
        <div className="h-px calzy-hairline" />
        <div className="flex items-center justify-between gap-4 p-4">
          <Label>Sex</Label>
          <Segmented<Sex>
            className="w-[170px]"
            value={sex}
            onChange={setSex}
            options={[
              { value: "male", label: "Male" },
              { value: "female", label: "Female" },
            ]}
          />
        </div>
        <div className="h-px calzy-hairline" />
        <div className="p-4">
          <div className="mb-2 flex items-baseline justify-between">
            <Label>Birth year</Label>
            <span className="metric text-[16px] text-ink">{birthYear}</span>
          </div>
          <Slider
            value={birthYear}
            min={1940}
            max={2012}
            step={1}
            onChange={setBirthYear}
            label="Birth year"
          />
        </div>
      </Card>
    </Scaffold>
  );
}

// MARK: - Nutrition goals

export function NutritionGoalsSheet({ open, onClose }: SheetProps): JSX.Element {
  const store = useAppStore();
  const [custom, setCustom] = useState<boolean>(false);
  const [calories, setCalories] = useState<number>(2200);
  const [protein, setProtein] = useState<number>(140);
  const [carbs, setCarbs] = useState<number>(240);
  const [fat, setFat] = useState<number>(70);
  const [water, setWater] = useState<number>(2500);

  useEffect(() => {
    if (!open) return;
    const targets = targetsOf(store.profile);
    setCustom(store.profile.usesCustomTargets);
    setCalories(targets.calories);
    setProtein(targets.protein);
    setCarbs(targets.carbs);
    setFat(targets.fat);
    setWater(store.profile.waterGoalMl);
  }, [open, store.profile]);

  const recommended = targetsOf({ ...store.profile, usesCustomTargets: false });

  return (
    <Scaffold
      open={open}
      onClose={onClose}
      title="Nutrition Goals"
      onSave={() =>
        store.setProfile({
          usesCustomTargets: custom,
          customCalories: Math.round(calories),
          customProtein: Math.round(protein),
          customCarbs: Math.round(carbs),
          customFat: Math.round(fat),
          waterGoalMl: Math.round(water),
        })
      }
    >
      <Card radius={22} padding={16}>
        <div className="flex items-center gap-3">
          <div className="min-w-0 flex-1">
            <p className="text-[16px] font-semibold text-ink">Custom targets</p>
            <p className="text-[12px] text-ink-faint">Off = calculated from your body and goal</p>
          </div>
          <Toggle checked={custom} onChange={setCustom} label="Custom targets" />
        </div>
      </Card>

      <Card radius={22} padding={16} className={cn(!custom && "pointer-events-none opacity-45")}>
        <div className="flex flex-col gap-[18px]">
          <SliderRow
            title="Calories"
            value={calories}
            min={1200}
            max={5000}
            step={10}
            display={`${Math.round(calories)} kcal`}
            onChange={setCalories}
            disabled={!custom}
          />
          <SliderRow
            title="Protein"
            value={protein}
            min={40}
            max={300}
            step={5}
            display={`${Math.round(protein)} g`}
            tint="hsl(var(--protein))"
            onChange={setProtein}
            disabled={!custom}
          />
          <SliderRow
            title="Carbs"
            value={carbs}
            min={40}
            max={600}
            step={5}
            display={`${Math.round(carbs)} g`}
            tint="hsl(var(--carbs))"
            onChange={setCarbs}
            disabled={!custom}
          />
          <SliderRow
            title="Fat"
            value={fat}
            min={20}
            max={200}
            step={5}
            display={`${Math.round(fat)} g`}
            tint="hsl(var(--fat))"
            onChange={setFat}
            disabled={!custom}
          />
        </div>
      </Card>

      <Card radius={22} padding={16}>
        <SliderRow
          title="Water"
          value={water}
          min={500}
          max={6000}
          step={250}
          display={`${Math.round(water)} ml`}
          tint="hsl(var(--water))"
          onChange={setWater}
        />
      </Card>

      {!custom && (
        <Card radius={20} padding={14}>
          <div className="flex flex-col items-center gap-[6px]">
            <span className="text-[11px] font-bold tracking-wider text-ink-faint">
              RECOMMENDED FOR YOU
            </span>
            <span className="text-[14px] font-semibold text-ink">
              {recommended.calories} kcal · {recommended.protein}P · {recommended.carbs}C ·{" "}
              {recommended.fat}F
            </span>
          </div>
        </Card>
      )}
    </Scaffold>
  );
}

// MARK: - Goals & weight

const goalIcons: Record<GoalDirection, LucideIcon> = {
  lose: TrendingDown,
  maintain: Equal,
  gain: TrendingUp,
};

const goalLabels: Record<GoalDirection, string> = {
  lose: "Lose weight",
  maintain: "Maintain",
  gain: "Build muscle",
};

export function GoalsWeightSheet({ open, onClose }: SheetProps): JSX.Element {
  const store = useAppStore();
  const [goal, setGoal] = useState<GoalDirection>("lose");
  const [current, setCurrent] = useState<number>(80);
  const [target, setTarget] = useState<number>(75);
  const [rate, setRate] = useState<number>(0.5);

  useEffect(() => {
    if (!open) return;
    setGoal(store.profile.goal);
    setCurrent(store.profile.currentWeightKg);
    setTarget(store.profile.goalWeightKg);
    setRate(store.profile.weeklyRateKg);
  }, [open, store.profile]);

  const projected = targetsOf({
    ...store.profile,
    goal,
    currentWeightKg: current,
    weeklyRateKg: rate,
    usesCustomTargets: false,
  }).calories;

  const weeks = Math.abs(target - current) / Math.max(rate, 0.1);
  const eta = `On track for ${longMonthDay(addDays(new Date(), Math.round(weeks * 7)))}`;

  return (
    <Scaffold
      open={open}
      onClose={onClose}
      title="Goals & Weight"
      onSave={() => {
        store.setProfile({
          goal,
          currentWeightKg: current,
          goalWeightKg: target,
          weeklyRateKg: rate,
        });
        store.logWeight(current);
      }}
    >
      <div className="flex gap-[9px]">
        {goalOrder.map((option) => {
          const Icon = goalIcons[option];
          const active = goal === option;
          return (
            <button
              key={option}
              type="button"
              onClick={() => {
                haptics.selection();
                setGoal(option);
              }}
              className={cn(
                "pressable flex flex-1 flex-col items-center gap-[7px] rounded-[20px] py-4 transition-colors",
                active ? "bg-ink text-white" : "bg-white/[0.78] text-ink",
              )}
            >
              <Icon size={17} strokeWidth={2.8} />
              <span className="text-center text-[12px] font-semibold leading-tight">
                {goalLabels[option]}
              </span>
            </button>
          );
        })}
      </div>

      <Card radius={22} padding={16}>
        <div className="flex flex-col gap-[18px]">
          <SliderRow
            title="Current weight"
            value={current}
            min={35}
            max={200}
            step={0.1}
            display={`${current.toFixed(1)} kg`}
            onChange={setCurrent}
          />
          <SliderRow
            title="Goal weight"
            value={target}
            min={35}
            max={200}
            step={0.1}
            display={`${target.toFixed(1)} kg`}
            onChange={setTarget}
          />
          {goal !== "maintain" && (
            <SliderRow
              title="Weekly pace"
              value={rate}
              min={0.1}
              max={1.2}
              step={0.1}
              display={`${rate.toFixed(1)} kg / week`}
              onChange={setRate}
            />
          )}
        </div>
      </Card>

      <Card radius={22} padding={18}>
        <div className="flex flex-col items-center gap-[5px]">
          <span className="text-[11px] font-bold tracking-wider text-ink-faint">
            PROJECTED TARGET
          </span>
          <span className="metric text-[24px] text-ink">{projected} kcal / day</span>
          {goal !== "maintain" && Math.abs(target - current) > 0.1 && (
            <span className="text-[13px] text-ink-soft">{eta}</span>
          )}
        </div>
      </Card>
    </Scaffold>
  );
}

// MARK: - Reminders

const reminderSlots: Array<{ hour: number; label: string; icon: LucideIcon }> = [
  { hour: 8, label: "Breakfast", icon: Sunrise },
  { hour: 9, label: "Mid-morning", icon: Coffee },
  { hour: 13, label: "Lunch", icon: Sun },
  { hour: 16, label: "Afternoon", icon: Leaf },
  { hour: 19, label: "Dinner", icon: MoonStar },
  { hour: 21, label: "Evening check-in", icon: BedDouble },
];

export function RemindersSheet({ open, onClose }: SheetProps): JSX.Element {
  const store = useAppStore();
  const [enabled, setEnabled] = useState<boolean>(true);
  const [hours, setHours] = useState<number[]>([9, 13, 19]);

  useEffect(() => {
    if (!open) return;
    setEnabled(store.profile.remindersEnabled);
    setHours(store.profile.reminderTimes);
  }, [open, store.profile]);

  return (
    <Scaffold
      open={open}
      onClose={onClose}
      title="Tracking Reminders"
      onSave={() =>
        store.setProfile({
          remindersEnabled: enabled,
          reminderTimes: [...hours].sort((a, b) => a - b),
        })
      }
    >
      <Card radius={22} padding={16}>
        <div className="flex items-center gap-3">
          <div className="min-w-0 flex-1">
            <p className="text-[16px] font-semibold text-ink">Daily nudges</p>
            <p className="text-[12px] text-ink-faint">Gentle reminders so nothing goes unlogged</p>
          </div>
          <Toggle checked={enabled} onChange={setEnabled} label="Daily nudges" />
        </div>
      </Card>

      <Card radius={22} padding={0} className={cn(!enabled && "pointer-events-none opacity-45")}>
        <div className="overflow-hidden" style={{ borderRadius: 22 }}>
          {reminderSlots.map((slot, index) => {
            const active = hours.includes(slot.hour);
            const Icon = slot.icon;
            return (
              <div key={slot.hour}>
                <button
                  type="button"
                  onClick={() => {
                    haptics.selection();
                    setHours((current) =>
                      current.includes(slot.hour)
                        ? current.filter((hour) => hour !== slot.hour)
                        : [...current, slot.hour],
                    );
                  }}
                  className="pressable flex w-full items-center gap-[13px] px-4 py-[13px] text-left"
                >
                  <Icon size={16} className="w-[26px] shrink-0 text-ink" strokeWidth={2.3} />
                  <span className="min-w-0 flex-1">
                    <span className="block text-[16px] font-medium text-ink">{slot.label}</span>
                    <span className="block text-[12px] text-ink-faint">
                      {String(slot.hour).padStart(2, "0")}:00
                    </span>
                  </span>
                  <CircleCheck
                    size={20}
                    className={cn("shrink-0", active ? "text-ink" : "text-ink-faint/50")}
                    fill={active ? "currentColor" : "none"}
                    stroke={active ? "white" : "currentColor"}
                    strokeWidth={active ? 2.4 : 2}
                  />
                </button>
                {index < reminderSlots.length - 1 && <div className="ml-[55px] h-px calzy-hairline" />}
              </div>
            );
          })}
        </div>
      </Card>
    </Scaffold>
  );
}

// MARK: - Activity

export function ActivitySheet({ open, onClose }: SheetProps): JSX.Element {
  const store = useAppStore();
  const [activity, setActivity] = useState<ActivityLevel>("light");

  useEffect(() => {
    if (!open) return;
    setActivity(store.profile.activity);
  }, [open, store.profile]);

  return (
    <Scaffold
      open={open}
      onClose={onClose}
      title="Activity Settings"
      onSave={() => store.setProfile({ activity })}
    >
      <div className="flex flex-col gap-[10px]">
        {activityOrder.map((level) => {
          const active = activity === level;
          const meta = activityMeta[level];
          return (
            <button
              key={level}
              type="button"
              onClick={() => {
                haptics.selection();
                setActivity(level);
              }}
              className={cn(
                "pressable flex items-center gap-[13px] rounded-[22px] p-4 text-left transition-colors",
                active ? "bg-ink" : "bg-white/[0.78]",
              )}
            >
              <ActivityIcon level={level} className={active ? "text-white" : "text-ink"} />
              <div className="min-w-0 flex-1">
                <p className={cn("text-[16px] font-semibold", active ? "text-white" : "text-ink")}>
                  {meta.label}
                </p>
                <p className={cn("text-[12px]", active ? "text-white/75" : "text-ink-faint")}>
                  {meta.detail}
                </p>
              </div>
              {active && <Check size={15} className="shrink-0 text-white" strokeWidth={3} />}
            </button>
          );
        })}
      </div>
    </Scaffold>
  );
}
