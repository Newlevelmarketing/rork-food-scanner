import {
  Bookmark,
  Droplet,
  Flame,
  Heart,
  History,
  Scale,
  Scan,
  Search,
  Share,
  TextCursorInput,
  TrendingUp,
  Trash2,
  type LucideIcon,
} from "lucide-react";
import type { JSX } from "react";
import { useMemo, useRef, useState } from "react";

import {
  DateStrip,
  EmptyMealsState,
  MacroTile,
  MealRow,
  QuickActionBar,
} from "@/components/calzy/HomeParts";
import { AnimatedNumber, Card, RingProgress, SectionHeader } from "@/components/calzy/Primitives";
import { ExerciseIcon } from "@/components/calzy/icons";
import { ShareSummary } from "@/features/ShareSummary";
import { haptics } from "@/lib/haptics";
import { useT } from "@/lib/i18n";
import { mealCalories } from "@/lib/nutrition";
import type { DailySummary } from "@/lib/summaryCard";
import type { MealEntry } from "@/lib/types";
import { cn } from "@/lib/utils";
import { useAppStore } from "@/store/AppStore";

export type HomeRoute = "scan" | "describe" | "search" | "saved" | "exercise";

/** Main dashboard: date strip, calorie + water rings, macros and the day's meals. */
export function Home({
  onRoute,
  onOpenMeal,
  onEditMeal,
}: {
  onRoute: (route: HomeRoute) => void;
  onOpenMeal: (meal: MealEntry) => void;
  onEditMeal: (meal: MealEntry) => void;
}): JSX.Element {
  const store = useAppStore();
  const t = useT();
  const [page, setPage] = useState<0 | 1>(0);
  const carouselRef = useRef<HTMLDivElement>(null);
  const [waterPulse, setWaterPulse] = useState<boolean>(false);
  const [isSharing, setIsSharing] = useState<boolean>(false);

  const date = store.selectedDate;
  const targets = store.targets;
  const eaten = store.caloriesEaten(date);
  const burned = store.caloriesBurned(date);
  const budget = targets.calories + burned;
  const remaining = budget - eaten;
  const meals = store.mealsOn(date);
  const workouts = store.exercisesOn(date);
  const water = store.waterOn(date);
  const waterGoal = Math.max(store.profile.waterGoalMl, 1);

  const healthScore = useMemo(() => {
    if (meals.length === 0) return "—";
    return `${Math.round(meals.reduce((sum, meal) => sum + meal.healthScore, 0) / meals.length)}`;
  }, [meals]);

  const coachLine = useMemo(() => {
    if (meals.length === 0) return "Log your first meal to unlock today's insights.";
    if (remaining < 0) return `You're ${-remaining} kcal over — a walk would even it out.`;
    const proteinLeft = targets.protein - Math.round(store.proteinOn(date));
    if (proteinLeft > 30) return `${proteinLeft}g of protein still to go today.`;
    return `Great balance so far — ${remaining} kcal left in the tank.`;
  }, [meals.length, remaining, targets.protein, store, date]);

  /** Snapshot of the selected day handed to the shareable summary card. */
  const summary = useMemo<DailySummary>(
    () => ({
      date,
      eaten,
      target: targets.calories,
      burned,
      protein: store.proteinOn(date),
      carbs: store.carbsOn(date),
      fat: store.fatOn(date),
      proteinTarget: targets.protein,
      carbsTarget: targets.carbs,
      fatTarget: targets.fat,
      mealCount: meals.length,
      water,
      streak: store.streak,
    }),
    [date, eaten, burned, targets, store, meals.length, water],
  );

  const pourWater = (ml: number): void => {
    haptics.soft();
    store.addWater(ml, date);
    setWaterPulse(true);
    window.setTimeout(() => setWaterPulse(false), 450);
  };

  return (
    <div className="no-scrollbar page-bottom h-full overflow-y-auto overscroll-contain">
      <header className="page-top flex items-center gap-[11px] px-5">
        <img
          src="/icon.png"
          alt=""
          aria-hidden
          draggable={false}
          className="h-[38px] w-[38px] rounded-[11px] object-cover"
        />
        <h1 className="metric text-[26px] leading-none text-ink">ModernBody</h1>

        {/* Streak stays visible at zero so it reads as a goal to chase
            rather than a badge that only appears once you already have one. */}
        <span
          aria-label={`${store.streak} day streak`}
          className={cn(
            "ml-auto flex h-[34px] items-center gap-[5px] rounded-full border border-white/70 px-[11px] transition-colors duration-300",
            store.streak > 0 ? "bg-flame/[0.12] text-flame" : "bg-white/60 text-ink-faint",
          )}
        >
          <Flame size={13} fill="currentColor" strokeWidth={0} />
          <span className="metric text-[15px]">{store.streak}</span>
        </span>

        <button
          type="button"
          aria-label="Share your day"
          onClick={() => {
            haptics.tap();
            setIsSharing(true);
          }}
          className="pressable grid h-[34px] w-[34px] place-items-center rounded-full border border-white/70 bg-white/[0.72] text-ink shadow-[0_3px_8px_rgba(0,0,0,0.05)]"
        >
          <Share size={15} strokeWidth={2.2} />
        </button>
      </header>

      <ShareSummary open={isSharing} summary={summary} onClose={() => setIsSharing(false)} />

      <div className="mt-4">
        <DateStrip
          selected={date}
          onSelect={store.setSelectedDate}
          hasLogs={store.hasLogs}
        />
      </div>

      {/* Energy card */}
      <div className="mt-4 px-5">
        <Card radius={28} padding={0}>
          <div className="flex items-stretch py-[15px]">
            <div className="relative flex flex-1 flex-col items-center gap-[8px]">
              <RingProgress
                progress={budget > 0 ? eaten / budget : 0}
                size={104}
                lineWidth={10}
                gradient={["hsl(var(--ink))", "hsl(var(--ink) / 0.72)"]}
              >
                <AnimatedNumber value={eaten} className="text-[27px] text-ink" />
                <span className="text-[12px] font-medium text-ink-faint">/{budget}</span>
              </RingProgress>
              {burned > 0 && (
                <span className="absolute left-[10px] top-[33px] flex items-center gap-[3px] text-flame">
                  <Flame size={10} fill="currentColor" strokeWidth={0} />
                  <span className="metric text-[13px]">+{burned}</span>
                </span>
              )}
              <div className="flex flex-col items-center gap-[2px]">
                <span className="truncate text-[12px] font-medium text-ink-soft">{t("h.eaten")}</span>
                <span
                  className={cn(
                    "text-[11px] font-semibold",
                    remaining >= 0 ? "text-mint" : "text-protein",
                  )}
                >
                  {remaining >= 0 ? `${remaining} ${t("h.left")}` : `${-remaining} ${t("h.over")}`}
                </span>
              </div>
            </div>

            <div className="w-px shrink-0 self-center calzy-hairline" style={{ height: 96 }} />

            <div className="flex flex-1 flex-col items-center gap-[8px]">
              <RingProgress
                progress={water / waterGoal}
                size={104}
                lineWidth={10}
                gradient={["hsl(var(--water))", "hsl(var(--water) / 0.55)"]}
                trackColor="hsl(var(--water) / 0.12)"
              >
                <Droplet
                  size={15}
                  className={cn("text-water", waterPulse && "animate-drop-pulse")}
                  fill="currentColor"
                  strokeWidth={0}
                />
                <AnimatedNumber value={water} className="text-[22px] text-ink" />
                <span className="text-[10px] font-medium text-ink-faint">ml</span>
              </RingProgress>

              <div className="flex items-center gap-[6px]">
                <button
                  type="button"
                  onClick={() => pourWater(250)}
                  className="pressable rounded-full bg-water/[0.14] px-[12px] py-[6px] text-[12px] font-semibold text-water"
                >
                  + 250ml
                </button>
                <button
                  type="button"
                  onClick={() => pourWater(500)}
                  aria-label="Add 500 millilitres"
                  className="pressable grid h-[28px] w-[28px] place-items-center rounded-full bg-water/[0.14] text-[11px] font-bold text-water"
                >
                  ½L
                </button>
                {water > 0 && (
                  <button
                    type="button"
                    onClick={() => {
                      haptics.tap();
                      store.undoWater(date);
                    }}
                    aria-label="Undo last water entry"
                    className="pressable grid h-[28px] w-[28px] place-items-center rounded-full bg-black/[0.05] text-ink-faint"
                  >
                    <Trash2 size={13} strokeWidth={2.4} />
                  </button>
                )}
              </div>
            </div>
          </div>
        </Card>
      </div>

      {/* Macro / insights carousel */}
      <div className="mt-4 flex flex-col gap-[10px]">
        {/* The dots used to be driven by an onFocus on the first panel - the only
            value `page` could already hold - so they never moved off page 0 no
            matter which panel was on screen. Derive it from the scroll instead. */}
        <div
          ref={carouselRef}
          onScroll={() => {
            const node = carouselRef.current;
            if (node === null || node.clientWidth === 0) return;
            const index = Math.round(node.scrollLeft / node.clientWidth);
            setPage(index <= 0 ? 0 : 1);
          }}
          className="no-scrollbar flex snap-x snap-mandatory overflow-x-auto"
        >
          <div className="w-full shrink-0 snap-center px-5">
            <div className="flex gap-3">
              <MacroTile
                title={t("h.protein")}
                emoji="🍗"
                eaten={store.proteinOn(date)}
                goal={targets.protein}
                tint="hsl(var(--protein))"
              />
              <MacroTile
                title={t("h.carbs")}
                emoji="🍞"
                eaten={store.carbsOn(date)}
                goal={targets.carbs}
                tint="hsl(var(--carbs))"
              />
              <MacroTile
                title={t("h.fat")}
                emoji="🥑"
                eaten={store.fatOn(date)}
                goal={targets.fat}
                tint="hsl(var(--fat))"
              />
            </div>
          </div>

          <div className="w-full shrink-0 snap-center px-5">
            <div className="flex flex-col gap-3">
              <div className="flex gap-3">
                <InsightTile
                  icon={TrendingUp}
                  token="--plum"
                  title={t("h.burned")}
                  value={`${burned}`}
                  unit="kcal"
                />
                <InsightTile
                  icon={Scale}
                  token="--mint"
                  title={t("h.weight")}
                  value={store.profile.currentWeightKg.toFixed(1)}
                  unit="kg"
                />
                <InsightTile
                  icon={Heart}
                  token="--protein"
                  title={t("h.health")}
                  value={healthScore}
                  unit="/10"
                />
              </div>
              <p className="text-center text-[13px] font-medium text-ink-soft">{coachLine}</p>
            </div>
          </div>
        </div>

        {/* Decorative: the position is already conveyed by the scroll container. */}
        <div aria-hidden="true" className="flex justify-center gap-[6px]">
          {[0, 1].map((index) => (
            <span
              key={index}
              className={cn(
                "h-[7px] rounded-full transition-all duration-300",
                page === index ? "w-[22px] bg-ink" : "w-[7px] bg-ink-faint/40",
              )}
            />
          ))}
        </div>
      </div>

      {/* Quick actions */}
      <div className="mt-1 px-5">
        <QuickActionBar
          actions={[
            { icon: Scan, title: t("h.scan"), tint: "--ink", onClick: () => onRoute("scan") },
            { icon: TextCursorInput, title: t("h.type"), tint: "--ink", onClick: () => onRoute("describe") },
            { icon: Search, title: t("h.search"), tint: "--ink", onClick: () => onRoute("search") },
            { icon: Bookmark, title: t("h.saved"), tint: "--ink", onClick: () => onRoute("saved") },
            { icon: Flame, title: t("h.exercise"), tint: "--ink", onClick: () => onRoute("exercise") },
          ]}
        />
      </div>

      {/* Meals */}
      <div className="mt-6 flex flex-col gap-3">
        <SectionHeader
          icon={History}
          title={t("h.meals")}
          className="px-5"
          trailing={
            meals.length > 0 ? (
              <span className="text-[13px] font-semibold text-ink-faint">{eaten} kcal</span>
            ) : null
          }
        />

        {meals.length === 0 ? (
          <EmptyMealsState />
        ) : (
          <div className="flex flex-col gap-[10px] px-5">
            {meals.map((meal) => (
              <MealRow
                key={meal.id}
                meal={meal}
                onClick={() => onOpenMeal(meal)}
                onEdit={() => onEditMeal(meal)}
              />
            ))}
          </div>
        )}

        {workouts.length > 0 && (
          <>
            <SectionHeader icon={Flame} title={t("h.exercise")} className="mt-3 px-5" />
            <div className="flex flex-col gap-[10px] px-5">
              {workouts.map((entry) => (
                <div
                  key={entry.id}
                  className="calzy-card flex items-center gap-[13px] p-3"
                  style={{ borderRadius: 22 }}
                >
                  <div className="grid h-[46px] w-[46px] shrink-0 place-items-center rounded-[14px] bg-flame/[0.12]">
                    <ExerciseIcon name={entry.icon} size={18} className="text-flame" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-[16px] font-semibold text-ink">{entry.name}</p>
                    <p className="text-[13px] text-ink-soft">{entry.minutes} min</p>
                  </div>
                  <span className="metric text-[17px] text-flame">−{entry.calories}</span>
                  <button
                    type="button"
                    onClick={() => {
                      haptics.tap();
                      store.deleteExercise(entry.id);
                    }}
                    aria-label={`Delete ${entry.name}`}
                    className="pressable grid h-8 w-8 place-items-center rounded-full text-ink-faint hover:bg-black/[0.05]"
                  >
                    <Trash2 size={15} strokeWidth={2.2} />
                  </button>
                </div>
              ))}
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function InsightTile({
  icon: Icon,
  token,
  title,
  value,
  unit,
}: {
  icon: LucideIcon;
  token: string;
  title: string;
  value: string;
  unit: string;
}): JSX.Element {
  return (
    <Card radius={22} padding={0} className="flex-1">
      <div className="flex flex-col items-center gap-2 py-[14px]">
        <span
          className="grid h-[42px] w-[42px] place-items-center rounded-full"
          style={{ backgroundColor: `hsl(var(${token}) / 0.13)` }}
        >
          <Icon size={17} strokeWidth={2.5} style={{ color: `hsl(var(${token}))` }} />
        </span>
        <span className="text-[12px] font-medium text-ink-soft">{title}</span>
        <span className="flex items-baseline gap-[2px]">
          <span className="metric text-[19px] text-ink">{value}</span>
          <span className="text-[11px] font-medium text-ink-faint">{unit}</span>
        </span>
      </div>
    </Card>
  );
}

/** Meal totals helper re-exported for the detail sheet. */
export { mealCalories };
