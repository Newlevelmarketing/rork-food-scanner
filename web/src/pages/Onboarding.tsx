import { Calendar, Equal, TrendingDown, TrendingUp, type LucideIcon } from "lucide-react";
import type { JSX } from "react";
import { useMemo, useState } from "react";

import { Card, PrimaryButton, Segmented, Slider } from "@/components/calzy/Primitives";
import { ActivityIcon } from "@/components/calzy/icons";
import { addDays, longMonthDay } from "@/lib/dates";
import { haptics } from "@/lib/haptics";
import {
  activityMeta,
  activityOrder,
  bmiCategory,
  bmiOf,
  goalOrder,
  maintenanceOf,
  targetsOf,
} from "@/lib/nutrition";
import { defaultProfile, type ActivityLevel, type GoalDirection, type Sex, type UserProfile } from "@/lib/types";
import { cn } from "@/lib/utils";
import { useAppStore } from "@/store/AppStore";

const TOTAL_STEPS = 6;

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

/** First-run flow that builds the user's profile and calculates their daily plan. */
export function Onboarding(): JSX.Element {
  const { completeOnboarding } = useAppStore();

  const [step, setStep] = useState<number>(0);
  const [name, setName] = useState<string>("");
  const [sex, setSex] = useState<Sex>("male");
  const [birthYear, setBirthYear] = useState<number>(1996);
  const [height, setHeight] = useState<number>(176);
  const [weight, setWeight] = useState<number>(80);
  const [goal, setGoal] = useState<GoalDirection>("lose");
  const [goalWeight, setGoalWeight] = useState<number>(74);
  const [rate, setRate] = useState<number>(0.5);
  const [activity, setActivity] = useState<ActivityLevel>("light");

  const draft = useMemo<UserProfile>(
    () => ({
      ...defaultProfile,
      name,
      sex,
      birthYear,
      heightCm: height,
      startWeightKg: weight,
      currentWeightKg: weight,
      goalWeightKg: goalWeight,
      goal,
      weeklyRateKg: rate,
      activity,
    }),
    [name, sex, birthYear, height, weight, goalWeight, goal, rate, activity],
  );

  const bmi = bmiOf(height, weight);

  const etaText = useMemo(() => {
    const weeks = Math.abs(goalWeight - weight) / Math.max(rate, 0.1);
    const date = addDays(new Date(), Math.round(weeks * 7));
    return `Reach ${goalWeight.toFixed(1)} kg around ${longMonthDay(date)}`;
  }, [goalWeight, weight, rate]);

  const next = (): void => {
    haptics.tap();
    if (step === TOTAL_STEPS - 1) {
      completeOnboarding(draft);
      haptics.success();
      return;
    }
    setStep((current) => current + 1);
  };

  return (
    <div className="calzy-backdrop flex h-full flex-col">
      <div className="page-top-lg flex gap-[6px] px-5">
        {Array.from({ length: TOTAL_STEPS }, (_, index) => (
          <span
            key={index}
            className={cn(
              "h-1 flex-1 rounded-full transition-colors duration-400",
              index <= step ? "bg-ink" : "bg-ink-faint/25",
            )}
          />
        ))}
      </div>

      <div className="no-scrollbar flex-1 overflow-y-auto">
        <div key={step} className="animate-rise-in min-h-full">
          {step === 0 && <WelcomeStep />}
          {step === 1 && (
            <StepShell title="First, the basics" subtitle="We use these to calculate your daily energy needs.">
              <Card radius={22} padding={16}>
                {/* htmlFor/id, not just proximity: without the association this
                    announced as an unnamed text field on the app's mandatory
                    first screen. */}
                <label
                  htmlFor="onboarding-name"
                  className="mb-2 block text-[15px] font-medium text-ink-soft"
                >
                  What should we call you?
                </label>
                <input
                  id="onboarding-name"
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  placeholder="Your name"
                  className="w-full rounded-[16px] bg-well px-[14px] py-[14px] text-[17px] font-medium text-ink outline-none placeholder:text-ink-faint"
                />
              </Card>

              <Card radius={22} padding={16}>
                <p className="mb-[10px] text-[15px] font-medium text-ink-soft">Sex</p>
                <Segmented<Sex>
                  value={sex}
                  onChange={setSex}
                  options={[
                    { value: "male", label: "Male" },
                    { value: "female", label: "Female" },
                  ]}
                />
              </Card>

              <BigSlider
                title="Birth year"
                value={birthYear}
                min={1940}
                max={2012}
                step={1}
                format={(value) => `${value}`}
                unit=""
                onChange={setBirthYear}
              />
            </StepShell>
          )}

          {step === 2 && (
            <StepShell title="Your body today" subtitle="Be honest — this only ever lives on your device.">
              <BigSlider
                title="Height"
                value={height}
                min={130}
                max={220}
                step={1}
                format={(value) => value.toFixed(0)}
                unit="cm"
                onChange={setHeight}
              />
              <BigSlider
                title="Weight"
                value={weight}
                min={35}
                max={200}
                step={0.1}
                format={(value) => value.toFixed(1)}
                unit="kg"
                onChange={setWeight}
              />
              <div className="flex items-center gap-2 self-start rounded-full bg-well px-4 py-[10px]">
                <span className="text-[13px] font-medium text-ink-soft">BMI</span>
                <span className="metric text-[17px] text-ink">{bmi.toFixed(1)}</span>
                <span className="text-[12px] font-semibold text-ink-soft">{bmiCategory(bmi)}</span>
              </div>
            </StepShell>
          )}

          {step === 3 && (
            <StepShell title="What's the mission?" subtitle="You can change this at any time.">
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
                        if (option === "lose") setGoalWeight(Math.max(40, weight - 6));
                        if (option === "gain") setGoalWeight(weight + 5);
                        if (option === "maintain") setGoalWeight(weight);
                      }}
                      className={cn(
                        "pressable flex flex-1 flex-col items-center gap-2 rounded-[20px] py-[18px] transition-colors duration-200",
                        active ? "bg-ink text-white" : "bg-white/[0.78] text-ink",
                      )}
                    >
                      <Icon size={18} strokeWidth={2.6} />
                      <span className="px-1 text-center text-[12px] font-semibold leading-tight">
                        {goalLabels[option]}
                      </span>
                    </button>
                  );
                })}
              </div>

              {goal !== "maintain" && (
                <>
                  <BigSlider
                    title="Goal weight"
                    value={goalWeight}
                    min={35}
                    max={200}
                    step={0.1}
                    format={(value) => value.toFixed(1)}
                    unit="kg"
                    onChange={setGoalWeight}
                  />
                  <BigSlider
                    title="Weekly pace"
                    value={rate}
                    min={0.1}
                    max={1.2}
                    step={0.1}
                    format={(value) => value.toFixed(1)}
                    unit="kg / week"
                    onChange={setRate}
                  />
                </>
              )}
            </StepShell>
          )}

          {step === 4 && (
            <StepShell title="How active are you?" subtitle="Outside of what you'll log as exercise.">
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
                        "pressable flex items-center gap-[13px] rounded-[20px] p-[15px] text-left transition-colors duration-200",
                        active ? "bg-ink" : "bg-white/[0.78]",
                      )}
                    >
                      <ActivityIcon
                        level={level}
                        size={18}
                        className={active ? "text-white" : "text-ink"}
                      />
                      <div>
                        <p
                          className={cn(
                            "text-[16px] font-semibold",
                            active ? "text-white" : "text-ink",
                          )}
                        >
                          {meta.label}
                        </p>
                        <p
                          className={cn(
                            "text-[12px]",
                            active ? "text-white/75" : "text-ink-faint",
                          )}
                        >
                          {meta.detail}
                        </p>
                      </div>
                    </button>
                  );
                })}
              </div>
            </StepShell>
          )}

          {step === 5 && (
            <PlanStep draft={draft} goal={goal} weight={weight} goalWeight={goalWeight} eta={etaText} />
          )}
        </div>
      </div>

      <div className="sheet-bottom shrink-0 px-[22px] pt-2">
        <PrimaryButton onClick={next}>
          {step === TOTAL_STEPS - 1 ? "Start tracking" : "Continue"}
        </PrimaryButton>
        {/* min-h-[44px] and padding on the control itself: the row was 18px tall,
            so the only way back through onboarding was a target less than half
            the 44pt minimum. */}
        <div className="mt-[10px] flex min-h-[44px] items-center justify-center">
          {step > 0 && (
            <button
              type="button"
              onClick={() => setStep((current) => current - 1)}
              className="px-6 py-[11px] text-[14px] font-medium text-ink-faint"
            >
              Back
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

function StepShell({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: React.ReactNode;
}): JSX.Element {
  return (
    <div className="flex flex-col gap-[22px] px-[22px] pb-8 pt-9">
      <div className="flex flex-col gap-2">
        <h1 className="text-[30px] font-bold leading-tight text-ink">{title}</h1>
        <p className="text-[16px] text-ink-soft">{subtitle}</p>
      </div>
      <div className="flex flex-col gap-4">{children}</div>
    </div>
  );
}

function WelcomeStep(): JSX.Element {
  return (
    <div className="flex h-full min-h-[520px] flex-col items-center justify-center gap-7 px-8">
      <div className="relative grid h-[260px] w-[260px] place-items-center">
        <div
          className="animate-breathe absolute inset-0 rounded-full"
          style={{
            background:
              "radial-gradient(circle, hsl(var(--flame) / 0.28) 0%, transparent 65%)",
          }}
        />
        <img
          src="/icon.png"
          alt="ModernBody app icon"
          className="relative h-[100px] w-[100px] rounded-[28px] object-cover shadow-[0_12px_34px_rgba(0,0,0,0.22)]"
          draggable={false}
        />
      </div>
      <div className="flex flex-col items-center gap-3">
        <h1 className="metric text-[40px] leading-none text-ink">ModernBody</h1>
        <p className="text-center text-[17px] leading-relaxed text-ink-soft">
          Point your camera at any meal.
          <br />
          We&apos;ll do the counting.
        </p>
      </div>
    </div>
  );
}

function PlanStep({
  draft,
  goal,
  weight,
  goalWeight,
  eta,
}: {
  draft: UserProfile;
  goal: GoalDirection;
  weight: number;
  goalWeight: number;
  eta: string;
}): JSX.Element {
  const targets = targetsOf(draft);

  return (
    <StepShell title="Your daily plan" subtitle="Built from your body, goal and activity level.">
      <Card radius={26} padding={22}>
        <div className="flex flex-col items-center gap-[10px]">
          <span className="text-[11px] font-bold tracking-wider text-ink-faint">DAILY CALORIES</span>
          <div className="flex items-baseline gap-[5px]">
            <span className="metric text-[56px] leading-none text-ink">{targets.calories}</span>
            <span className="text-[17px] font-medium text-ink-faint">kcal</span>
          </div>
          <span className="text-[13px] text-ink-soft">
            Maintenance is about {Math.round(maintenanceOf(draft))} kcal
          </span>
        </div>
      </Card>

      <div className="flex gap-3">
        <PlanTile title="Protein" value={`${targets.protein}g`} token="--protein" emoji="🍗" />
        <PlanTile title="Carbs" value={`${targets.carbs}g`} token="--carbs" emoji="🍞" />
        <PlanTile title="Fat" value={`${targets.fat}g`} token="--fat" emoji="🥑" />
      </div>

      {goal !== "maintain" && Math.abs(goalWeight - weight) > 0.1 && (
        <Card radius={20} padding={15}>
          <div className="flex items-center gap-[9px]">
            <Calendar size={15} className="shrink-0 text-mint" strokeWidth={2.4} />
            <span className="text-[14px] font-medium text-ink-soft">{eta}</span>
          </div>
        </Card>
      )}
    </StepShell>
  );
}

function PlanTile({
  title,
  value,
  token,
  emoji,
}: {
  title: string;
  value: string;
  token: string;
  emoji: string;
}): JSX.Element {
  return (
    <div
      className="flex flex-1 flex-col items-center gap-[7px] rounded-[22px] py-4"
      style={{ backgroundColor: `hsl(var(${token}) / 0.12)` }}
    >
      <span className="text-[24px] leading-none">{emoji}</span>
      <span className="metric text-[19px] text-ink">{value}</span>
      <span className="text-[12px] font-medium text-ink-soft">{title}</span>
    </div>
  );
}

export function BigSlider({
  title,
  value,
  min,
  max,
  step,
  format,
  unit,
  onChange,
  tint = "hsl(var(--ink))",
}: {
  title: string;
  value: number;
  min: number;
  max: number;
  step: number;
  format: (value: number) => string;
  unit: string;
  onChange: (value: number) => void;
  tint?: string;
}): JSX.Element {
  return (
    <Card radius={22} padding={16}>
      <div className="mb-[10px] flex items-baseline justify-between">
        <span className="text-[15px] font-medium text-ink-soft">{title}</span>
        <span className="flex items-baseline gap-[3px]">
          <span className="metric text-[22px] text-ink">{format(value)}</span>
          {unit && <span className="text-[12px] font-medium text-ink-faint">{unit}</span>}
        </span>
      </div>
      <Slider
        value={value}
        min={min}
        max={max}
        step={step}
        label={title}
        tint={tint}
        onChange={(next) => {
          haptics.selection();
          onChange(next);
        }}
      />
    </Card>
  );
}
