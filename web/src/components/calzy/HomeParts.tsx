import {
  Bookmark,
  Camera,
  CupSoda,
  Flame,
  MoonStar,
  Pencil,
  Search,
  Sun,
  Sunrise,
  TextCursorInput,
  type LucideIcon,
} from "lucide-react";
import type { JSX } from "react";
import { Fragment, useEffect, useMemo, useRef } from "react";

import { Card } from "@/components/calzy/Primitives";
import { addDays, isSameDay, isToday, shortTime, shortWeekday, startOfDay } from "@/lib/dates";
import { haptics } from "@/lib/haptics";
import { useT } from "@/lib/i18n";
import { mealCalories, mealCarbs, mealFat, mealProtein } from "@/lib/nutrition";
import type { EntrySource, MealEntry, MealSlot } from "@/lib/types";
import { cn } from "@/lib/utils";

/** Salad emoji used in the empty-state ghost row (escaped to keep the file ASCII). */
const SALAD_EMOJI = String.fromCodePoint(0x1f957);

const sourceIcons: Record<EntrySource, LucideIcon> = {
  photo: Camera,
  text: TextCursorInput,
  search: Search,
  saved: Bookmark,
  manual: Pencil,
};

const slotIcons: Record<MealSlot, LucideIcon> = {
  breakfast: Sunrise,
  lunch: Sun,
  dinner: MoonStar,
  snack: CupSoda,
};

/** Horizontal week selector pinned under the app title. */
export function DateStrip({
  selected,
  onSelect,
  hasLogs,
}: {
  selected: Date;
  onSelect: (date: Date) => void;
  hasLogs: (date: Date) => boolean;
}): JSX.Element {
  const scrollerRef = useRef<HTMLDivElement>(null);

  const days = useMemo(() => {
    const today = startOfDay(new Date());
    return Array.from({ length: 21 }, (_, index) => addDays(today, index - 20));
  }, []);

  useEffect(() => {
    const node = scrollerRef.current;
    if (node) node.scrollLeft = node.scrollWidth;
  }, []);

  return (
    <div ref={scrollerRef} className="no-scrollbar overflow-x-auto px-5 py-1">
      <div className="flex gap-[10px]">
        {days.map((day) => {
          const active = isSameDay(day, selected);
          const today = isToday(day);
          return (
            <button
              key={day.toISOString()}
              type="button"
              onClick={() => {
                haptics.selection();
                onSelect(day);
              }}
              className="pressable flex shrink-0 flex-col items-center gap-[6px]"
            >
              <span
                className={cn(
                  "text-[11px] font-semibold tracking-wide transition-colors",
                  active ? "text-ink" : "text-ink-faint",
                )}
              >
                {shortWeekday(day)}
              </span>
              <span
                className={cn(
                  "relative grid h-[50px] w-[46px] place-items-center rounded-[14px] transition-all duration-300",
                  active
                    ? "metric bg-white text-[19px] text-ink shadow-[0_4px_14px_rgba(0,0,0,0.09)]"
                    : "metric text-[19px] font-medium text-ink-faint",
                )}
              >
                {day.getDate()}
                {today && !active && (
                  <span className="absolute right-[6px] top-[6px] h-[5px] w-[5px] rounded-full bg-flame" />
                )}
              </span>
              <span
                className={cn(
                  "h-[5px] w-[5px] rounded-full transition-colors",
                  hasLogs(day) ? "bg-mint" : "bg-transparent",
                )}
              />
            </button>
          );
        })}
      </div>
    </div>
  );
}

/** The macro tile shown in the home carousel (protein / carbs / fat). */
export function MacroTile({
  title,
  emoji,
  eaten,
  goal,
  tint,
}: {
  title: string;
  emoji: string;
  eaten: number;
  goal: number;
  tint: string;
}): JSX.Element {
  const progress = goal > 0 ? Math.min(eaten / goal, 1) : 0;
  const radius = 28.5;
  const circumference = 2 * Math.PI * radius;

  return (
    <Card radius={22} padding={0} className="flex-1">
      <div className="flex flex-col items-center gap-[10px] py-4">
        <div className="relative h-[62px] w-[62px]">
          <svg width={62} height={62} className="-rotate-90">
            <circle
              cx={31}
              cy={31}
              r={radius}
              fill="none"
              stroke={tint}
              strokeOpacity={0.16}
              strokeWidth={5}
            />
            <circle
              cx={31}
              cy={31}
              r={radius}
              fill="none"
              stroke={tint}
              strokeWidth={5}
              strokeLinecap="round"
              strokeDasharray={circumference}
              strokeDashoffset={circumference * (1 - progress)}
              style={{ transition: "stroke-dashoffset 750ms cubic-bezier(0.22,1,0.36,1)" }}
            />
          </svg>
          <span className="absolute inset-0 grid place-items-center text-[26px] leading-none">
            {emoji}
          </span>
        </div>
        <span className="text-[13px] font-medium text-ink-soft">{title}</span>
        <div className="flex items-baseline gap-[2px]">
          <span className="metric text-[21px] text-ink">{Math.round(eaten)}</span>
          <span className="text-[13px] font-medium text-ink-faint">/{goal}g</span>
        </div>
      </div>
    </Card>
  );
}

/** Small stat tile used on the Progress screen. */
export function StatTile({
  icon: Icon,
  iconColor,
  title,
  value,
  unit,
  children,
}: {
  icon: LucideIcon;
  iconColor: string;
  title: string;
  value: string;
  unit?: string;
  children?: React.ReactNode;
}): JSX.Element {
  return (
    <Card radius={22} padding={16} className="flex-1">
      <div className="flex h-full flex-col gap-[10px]">
        <div className="flex items-center gap-[7px]">
          <Icon size={15} color={iconColor} strokeWidth={2.5} />
          <span className="text-[14px] font-semibold text-ink-soft">{title}</span>
        </div>
        <div className="flex items-baseline gap-1">
          <span className="metric text-[30px] text-ink">{value}</span>
          {unit && <span className="text-[14px] font-medium text-ink-faint">{unit}</span>}
        </div>
        {children}
      </div>
    </Card>
  );
}

/** A single entry in the home quick-action strip. */
export type QuickAction = {
  icon: LucideIcon;
  title: string;
  /** CSS custom property name for the accent, e.g. `--flame`. */
  tint: string;
  onClick: () => void;
};

/**
 * Compact control strip for the home quick actions.
 *
 * The five actions share one card and are separated by hairlines so the row
 * reads as a single deliberate control rather than five competing tiles.
 */
export function QuickActionBar({ actions }: { actions: QuickAction[] }): JSX.Element {
  return (
    <div
      className="calzy-card flex items-stretch overflow-hidden"
      style={{ borderRadius: 22 }}
    >
      {actions.map((action, index) => (
        <Fragment key={action.title}>
          {index > 0 && <span className="calzy-hairline my-auto h-[24px] w-px shrink-0" />}
          <QuickActionCell action={action} />
        </Fragment>
      ))}
    </div>
  );
}

function QuickActionCell({ action }: { action: QuickAction }): JSX.Element {
  const Icon = action.icon;

  return (
    <button
      type="button"
      onClick={() => {
        haptics.tap();
        action.onClick();
      }}
      className="quick-cell flex flex-1 flex-col items-center gap-[5px] py-[11px]"
      style={{ ["--cell-tint" as string]: `hsl(var(${action.tint}))` }}
    >
      <Icon
        size={15}
        strokeWidth={2.4}
        style={{ color: `hsl(var(${action.tint}))` }}
      />
      <span className="text-[10px] font-semibold tracking-[0.1px] text-ink-soft">
        {action.title}
      </span>
    </button>
  );
}

function MacroChip({ text, token }: { text: string; token: string }): JSX.Element {
  return (
    <span
      className="rounded-full px-[7px] py-[3px] text-[11px] font-semibold"
      style={{ color: `hsl(var(${token}))`, backgroundColor: `hsl(var(${token}) / 0.12)` }}
    >
      {text}
    </span>
  );
}

/**
 * One logged meal in the home timeline.
 *
 * The card holds two separate hit targets: the body opens the meal detail, and
 * the trailing pencil jumps straight to the quick-edit form.
 */
export function MealRow({
  meal,
  onClick,
  onEdit,
}: {
  meal: MealEntry;
  onClick: () => void;
  onEdit: () => void;
}): JSX.Element {
  const SourceIcon = sourceIcons[meal.source];
  const SlotIcon = slotIcons[meal.slot];

  return (
    <div
      className="calzy-card animate-pop-in flex w-full items-center gap-2 p-3"
      style={{ borderRadius: 22 }}
    >
      <button
        type="button"
        onClick={() => {
          haptics.tap();
          onClick();
        }}
        aria-label={`${meal.title}, ${mealCalories(meal)} calories. Opens meal details.`}
        className="pressable flex min-w-0 flex-1 items-center gap-[13px] text-left"
      >
        <div className="grid h-[58px] w-[58px] shrink-0 place-items-center overflow-hidden rounded-[16px] bg-well">
          {meal.photo ? (
            <img src={meal.photo} alt="" className="h-full w-full object-cover" />
          ) : (
            <SourceIcon size={20} className="text-ink-faint" strokeWidth={2.3} />
          )}
        </div>

        <div className="min-w-0 flex-1">
          <p className="truncate text-[16px] font-semibold text-ink">{meal.title}</p>
          <div className="mt-[5px] flex flex-wrap items-center gap-2">
            <span className="flex items-center gap-[3px] text-flame">
              <Flame size={11} fill="currentColor" strokeWidth={0} />
              <span className="text-[13px] font-bold">{mealCalories(meal)}</span>
            </span>
            <MacroChip text={`${Math.round(mealProtein(meal))}P`} token="--protein" />
            <MacroChip text={`${Math.round(mealCarbs(meal))}C`} token="--carbs" />
            <MacroChip text={`${Math.round(mealFat(meal))}F`} token="--fat" />
          </div>
        </div>

        <div className="flex shrink-0 items-center gap-1 text-ink-faint">
          <SlotIcon size={11} strokeWidth={2.2} />
          <span className="text-[12px] font-medium">{shortTime(meal.date)}</span>
        </div>
      </button>

      <button
        type="button"
        onClick={() => {
          haptics.tap();
          onEdit();
        }}
        aria-label={`Edit ${meal.title}`}
        className="pressable grid h-[34px] w-[34px] shrink-0 place-items-center rounded-full bg-well text-ink-soft"
      >
        <Pencil size={13} strokeWidth={2.6} />
      </button>
    </div>
  );
}

/**
 * Empty state shown when a day has no meals yet.
 *
 * Renders a ghosted meal row on a small stack of cards so a blank day previews
 * the shape of what a logged meal will look like.
 */
export function EmptyMealsState(): JSX.Element {
  const t = useT();
  const caption = t("h.empty");

  return (
    <div className="px-5">
      <div
        className="rounded-[26px] border border-ink/[0.06] bg-white/40 px-[18px] py-[22px]"
        aria-label={caption}
      >
        <div className="calzy-float relative mb-[13px]">
          <div className="absolute inset-x-[34px] top-[13px] h-[76px] rounded-[16px] bg-white/40" />
          <div className="absolute inset-x-[20px] top-[7px] h-[76px] rounded-[17px] bg-white/70" />
          <div className="relative mx-[8px] flex h-[76px] items-center gap-[14px] rounded-[18px] bg-white px-4 shadow-[0_4px_10px_rgba(0,0,0,0.05)]">
            <span className="text-[32px] leading-none" aria-hidden="true">
              {SALAD_EMOJI}
            </span>
            <div className="flex flex-1 flex-col gap-[10px]">
              <div className="h-[10px] rounded-full bg-ink/[0.07]" />
              <div className="mr-[58px] h-[10px] rounded-full bg-ink/[0.07]" />
            </div>
          </div>
        </div>

        <p className="text-center text-[14px] font-medium text-ink-soft">{caption}</p>
      </div>
    </div>
  );
}
