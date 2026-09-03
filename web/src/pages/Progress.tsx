import {
  ArrowDown,
  ArrowDownRight,
  ArrowUp,
  ArrowUpRight,
  Camera,
  Equal,
  Flame,
  LineChart as LineChartIcon,
  List,
  Pencil,
  Plus,
  Trash2,
  Zap,
} from "lucide-react";
import type { JSX } from "react";
import { useMemo, useRef, useState } from "react";
import {
  Area,
  CartesianGrid,
  ComposedChart,
  Line,
  ReferenceLine,
  ResponsiveContainer,
  XAxis,
  YAxis,
} from "recharts";

import { Card, PrimaryButton, Segmented, Slider } from "@/components/calzy/Primitives";
import { StatTile } from "@/components/calzy/HomeParts";
import { BottomSheet } from "@/components/calzy/Sheet";
import {
  abbreviatedDate,
  addDays,
  addMonths,
  daysInMonth,
  isToday,
  monthDay,
  monthYear,
  startOfMonth,
  weekdayInitials,
} from "@/lib/dates";
import { haptics } from "@/lib/haptics";
import { bmiCategory, bmiCategoryColor, bmiOf } from "@/lib/nutrition";
import { toThumbnail } from "@/lib/image";
import { cn } from "@/lib/utils";
import { useAppStore } from "@/store/AppStore";

type TrendRange = "week" | "month";

/** Progress tab: weight, BMI, streaks, weight journal, trend chart and photos. */
export function Progress(): JSX.Element {
  const store = useAppStore();
  const profile = store.profile;

  const [weightSheetDate, setWeightSheetDate] = useState<Date | null>(null);
  const [showBodyMetrics, setShowBodyMetrics] = useState<boolean>(false);
  const [journalMonth, setJournalMonth] = useState<Date>(() => new Date());
  const [trendRange, setTrendRange] = useState<TrendRange>("week");
  const fileRef = useRef<HTMLInputElement>(null);

  const weightDelta = profile.currentWeightKg - profile.startWeightKg;
  const deltaColor =
    profile.goal === "maintain"
      ? "text-ink-soft"
      : (profile.goal === "lose" ? weightDelta <= 0 : weightDelta >= 0)
        ? "text-mint"
        : "text-flame";

  const bmi = bmiOf(profile.heightCm, profile.currentWeightKg);
  const category = bmiCategory(bmi);
  const bmiPosition = Math.min(Math.max((bmi - 15) / 25, 0), 1);

  const monthStart = startOfMonth(journalMonth);
  const totalDays = daysInMonth(journalMonth);
  const leadingBlanks = monthStart.getDay();
  const loggedInMonth = store.weightEntries.filter((entry) => {
    const date = new Date(entry.date);
    return (
      date.getFullYear() === monthStart.getFullYear() && date.getMonth() === monthStart.getMonth()
    );
  }).length;

  const trendEntries = useMemo(() => {
    const cutoff = addDays(new Date(), trendRange === "week" ? -7 : -30);
    const filtered = store.weightEntries.filter((entry) => new Date(entry.date) >= cutoff);
    return filtered.length > 0 ? filtered : store.weightEntries.slice(-2);
  }, [store.weightEntries, trendRange]);

  const chartData = trendEntries.map((entry) => ({
    ts: new Date(entry.date).getTime(),
    kg: entry.kilograms,
  }));
  const values = trendEntries.map((entry) => entry.kilograms);
  const change = (values.length > 0 ? values[values.length - 1] : 0) - (values[0] ?? 0);

  const onPickPhoto = async (file: File | undefined): Promise<void> => {
    if (!file) return;
    const thumbnail = await toThumbnail(file, 620);
    if (!thumbnail) return;
    store.addProgressPhoto(thumbnail);
    haptics.success();
  };

  return (
    <div className="no-scrollbar page-bottom h-full overflow-y-auto overscroll-contain">
      <h1 className="page-top-lg px-5 pb-1 text-[32px] font-bold text-ink">Progress</h1>

      <div className="mt-2 flex items-stretch gap-3 px-5">
        {/* Weight */}
        <Card radius={22} padding={16} className="flex-1">
          <div className="flex h-full flex-col gap-[10px]">
            <span className="text-[14px] font-semibold text-ink-soft">Weight</span>
            <div className="flex items-baseline gap-1">
              <span className="metric text-[32px] text-ink">
                {profile.currentWeightKg.toFixed(1)}
              </span>
              <span className="text-[14px] font-medium text-ink-faint">kg</span>
            </div>
            <span className={cn("flex items-center gap-1 text-[13px] font-semibold", deltaColor)}>
              {weightDelta >= 0 ? <ArrowUpRight size={13} strokeWidth={3} /> : <ArrowDownRight size={13} strokeWidth={3} />}
              {weightDelta >= 0 ? "+" : ""}
              {weightDelta.toFixed(1)}
            </span>
            <button
              type="button"
              onClick={() => {
                haptics.tap();
                setWeightSheetDate(new Date());
              }}
              aria-label="Log today's weight"
              className="pressable mt-auto grid h-[38px] w-[38px] shrink-0 place-items-center rounded-full bg-ink text-white"
            >
              <Plus size={16} strokeWidth={3} />
            </button>
          </div>
        </Card>

        {/* BMI */}
        <Card radius={22} padding={16} className="flex-1">
          <div className="flex h-full flex-col gap-[10px]">
            <div className="flex items-center justify-between">
              <span className="text-[14px] font-semibold text-ink-soft">BMI</span>
              <button
                type="button"
                onClick={() => {
                  haptics.tap();
                  setShowBodyMetrics(true);
                }}
                aria-label="Edit body metrics"
                className="pressable text-ink-faint"
              >
                <Pencil size={13} strokeWidth={2.6} />
              </button>
            </div>
            <span className="metric text-[32px] text-ink">{bmi.toFixed(1)}</span>
            <span
              className="text-[14px] font-semibold"
              style={{ color: bmiCategoryColor(category) }}
            >
              {category}
            </span>
            <div className="relative h-[13px]">
              <div
                className="absolute inset-x-0 top-[3.5px] h-[6px] rounded-full"
                style={{
                  background:
                    "linear-gradient(to right, hsl(var(--carbs)), hsl(var(--mint)), hsl(var(--fat)), hsl(var(--protein)))",
                }}
              />
              <span
                className="absolute top-0 h-[13px] w-[13px] rounded-full bg-white shadow-[0_1px_4px_rgba(0,0,0,0.25)] transition-all duration-500"
                style={{ left: `calc(${bmiPosition * 100}% - 6.5px)` }}
              />
            </div>
            <span className="whitespace-pre-line text-[10px] leading-[1.35] text-ink-faint">
              {"Source:\nWHO BMI Classification"}
            </span>
          </div>
        </Card>
      </div>

      <div className="mt-3 flex items-stretch gap-3 px-5">
        <StatTile
          icon={Flame}
          iconColor="hsl(var(--flame))"
          title="Day Streak"
          value={`${store.streak}`}
          unit="days"
        />
        <StatTile
          icon={Zap}
          iconColor="hsl(var(--fat))"
          title="Avg Calories"
          value={`${store.averageCalories.average}`}
        >
          <span className="text-[12px] text-ink-faint">
            {store.averageCalories.logged}/7 days logged
          </span>
        </StatTile>
      </div>

      {/* Weight journal */}
      <div className="mt-3 px-5">
        <Card radius={26} padding={18}>
          <div className="flex items-start justify-between gap-3">
            <div>
              <h2 className="text-[19px] font-bold text-ink">Weight Journal</h2>
              <p className="text-[13px] text-ink-faint">
                {loggedInMonth}/{totalDays} days logged
              </p>
            </div>
            <div className="flex shrink-0 items-center gap-[10px]">
              <MonthButton direction="prev" onClick={() => setJournalMonth((m) => addMonths(m, -1))} />
              <span className="min-w-[74px] text-center text-[14px] font-semibold text-ink">
                {monthYear(monthStart)}
              </span>
              <MonthButton direction="next" onClick={() => setJournalMonth((m) => addMonths(m, 1))} />
            </div>
          </div>

          <div className="mt-4 grid grid-cols-7 gap-1">
            {weekdayInitials.map((symbol, index) => (
              <span
                key={`${symbol}-${index}`}
                className="text-center text-[11px] font-medium text-ink-faint"
              >
                {symbol}
              </span>
            ))}
          </div>

          <div className="mt-[6px] grid grid-cols-7 gap-1">
            {Array.from({ length: leadingBlanks }, (_, index) => (
              <span key={`blank-${index}`} className="h-[42px]" />
            ))}
            {Array.from({ length: totalDays }, (_, index) => {
              const day = new Date(monthStart.getFullYear(), monthStart.getMonth(), index + 1);
              const entry = store.weightOn(day);
              const future = day > new Date();
              return (
                <button
                  key={day.toISOString()}
                  type="button"
                  disabled={future}
                  onClick={() => {
                    haptics.tap();
                    setWeightSheetDate(day);
                  }}
                  className={cn(
                    "pressable flex h-[42px] flex-col items-center justify-center rounded-[11px] transition-colors",
                    entry ? "bg-ink" : "bg-transparent",
                    isToday(day) && !entry && "ring-[1.5px] ring-inset ring-ink",
                    entry && isToday(day) && "ring-[1.5px] ring-inset ring-ink",
                  )}
                >
                  <span
                    className={cn(
                      "text-[12px] leading-none",
                      entry
                        ? "font-bold text-white"
                        : future
                          ? "text-ink-faint/40"
                          : "text-ink-soft",
                    )}
                  >
                    {index + 1}
                  </span>
                  {entry ? (
                    <span className="mt-[2px] text-[9px] font-semibold leading-none text-white/85">
                      {Math.round(entry.kilograms)}
                    </span>
                  ) : (
                    !future && <Plus size={8} strokeWidth={3.4} className="mt-[2px] text-ink-faint/60" />
                  )}
                </button>
              );
            })}
          </div>
        </Card>
      </div>

      {/* Trend */}
      <div className="mt-3 px-5">
        <Card radius={26} padding={18}>
          <div className="flex items-start justify-between gap-3">
            <div>
              <h2 className="text-[19px] font-bold text-ink">Weight Trend</h2>
              {trendEntries.length > 1 && (
                <div
                  className={cn(
                    "flex items-center gap-1",
                    change >= 0 ? "text-flame" : "text-mint",
                  )}
                >
                  {change >= 0 ? <ArrowUpRight size={11} strokeWidth={3} /> : <ArrowDownRight size={11} strokeWidth={3} />}
                  <span className="text-[13px] font-bold">
                    {change >= 0 ? "+" : ""}
                    {change.toFixed(1)} kg
                  </span>
                  <span className="text-[13px] text-ink-faint">this {trendRange}</span>
                </div>
              )}
            </div>
            <Segmented<TrendRange>
              className="w-[150px] shrink-0"
              value={trendRange}
              onChange={setTrendRange}
              options={[
                { value: "week", label: "Week" },
                { value: "month", label: "Month" },
              ]}
            />
          </div>

          {trendEntries.length > 1 ? (
            <>
              <div className="mt-4 flex rounded-[16px] bg-well/70 py-[10px]">
                <TrendStat icon={List} title="Entries" value={`${trendEntries.length}`} />
                <Hairline />
                <TrendStat icon={ArrowDown} title="Lowest" value={Math.min(...values).toFixed(1)} />
                <Hairline />
                <TrendStat icon={ArrowUp} title="Highest" value={Math.max(...values).toFixed(1)} />
                <Hairline />
                <TrendStat
                  icon={Equal}
                  title="Average"
                  value={(values.reduce((a, b) => a + b, 0) / values.length).toFixed(1)}
                />
              </div>

              <div className="mt-4 h-[190px] w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <ComposedChart data={chartData} margin={{ top: 14, right: 6, bottom: 0, left: -18 }}>
                    <defs>
                      <linearGradient id="weightFill" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="hsl(var(--ink))" stopOpacity={0.16} />
                        <stop offset="100%" stopColor="hsl(var(--ink))" stopOpacity={0.01} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid stroke="rgba(0,0,0,0.06)" vertical={false} />
                    <XAxis
                      dataKey="ts"
                      type="number"
                      domain={["dataMin", "dataMax"]}
                      tickFormatter={(value: number) => monthDay(new Date(value))}
                      tick={{ fontSize: 10, fill: "hsl(var(--ink-faint))" }}
                      axisLine={false}
                      tickLine={false}
                      tickCount={4}
                    />
                    <YAxis
                      domain={["auto", "auto"]}
                      tick={{ fontSize: 10, fill: "hsl(var(--ink-faint))" }}
                      axisLine={false}
                      tickLine={false}
                      tickFormatter={(value: number) => value.toFixed(0)}
                      width={34}
                    />
                    <ReferenceLine
                      y={profile.goalWeightKg}
                      stroke="hsl(var(--mint))"
                      strokeOpacity={0.7}
                      strokeDasharray="4 4"
                      label={{
                        value: "Goal",
                        position: "insideTopRight",
                        fill: "hsl(var(--mint))",
                        fontSize: 10,
                        fontWeight: 600,
                      }}
                    />
                    <Area
                      type="monotone"
                      dataKey="kg"
                      stroke="none"
                      fill="url(#weightFill)"
                      isAnimationActive
                    />
                    <Line
                      type="monotone"
                      dataKey="kg"
                      stroke="hsl(var(--ink))"
                      strokeWidth={2.5}
                      strokeLinecap="round"
                      dot={{ r: 3, fill: "hsl(var(--ink))", strokeWidth: 0 }}
                      activeDot={{ r: 5 }}
                      isAnimationActive
                    />
                  </ComposedChart>
                </ResponsiveContainer>
              </div>
            </>
          ) : (
            <div className="flex flex-col items-center gap-2 py-10">
              <LineChartIcon size={26} className="text-ink-faint" strokeWidth={2} />
              <p className="text-[14px] font-medium text-ink-soft">
                Log your weight twice to see a trend
              </p>
            </div>
          )}
        </Card>
      </div>

      {/* Photos */}
      <div className="mt-5 flex flex-col gap-3">
        <div className="flex items-center justify-between px-5">
          <h2 className="text-[19px] font-bold text-ink">Progress Photos</h2>
          <button
            type="button"
            onClick={() => fileRef.current?.click()}
            className="pressable flex items-center gap-1 text-[14px] font-semibold text-ink"
          >
            <Plus size={15} strokeWidth={3} /> Add
          </button>
          <input
            ref={fileRef}
            type="file"
            accept="image/*"
            className="hidden"
            onChange={(event) => {
              void onPickPhoto(event.target.files?.[0]);
              event.target.value = "";
            }}
          />
        </div>

        {store.photos.length === 0 ? (
          <div className="px-5">
            <Card radius={26} padding={0}>
              <div className="flex flex-col items-center gap-2 py-10">
                <Camera size={28} className="text-ink-faint" strokeWidth={2} />
                <p className="text-[16px] font-semibold text-ink-soft">Track your transformation</p>
                <p className="text-[13px] text-ink-faint">Add photos to see your progress over time</p>
              </div>
            </Card>
          </div>
        ) : (
          <div className="no-scrollbar overflow-x-auto px-5">
            <div className="flex gap-3">
              {store.photos.map((photo) => (
                <div
                  key={photo.id}
                  className="relative h-[190px] w-[140px] shrink-0 overflow-hidden rounded-[20px] bg-well"
                >
                  <img src={photo.photo} alt="" className="h-full w-full object-cover" />
                  <span className="absolute inset-x-0 bottom-[10px] mx-auto flex w-fit flex-col items-center rounded-full bg-black/40 px-3 py-[6px] text-white backdrop-blur-sm">
                    <span className="text-[12px] font-bold leading-tight">{monthDay(photo.date)}</span>
                    {photo.weightKg !== undefined && (
                      <span className="text-[10px] font-medium opacity-85">
                        {photo.weightKg.toFixed(1)} kg
                      </span>
                    )}
                  </span>
                  <button
                    type="button"
                    onClick={() => store.deletePhoto(photo.id)}
                    aria-label="Delete photo"
                    className="pressable absolute right-2 top-2 grid h-8 w-8 place-items-center rounded-full bg-black/40 text-white backdrop-blur-sm"
                  >
                    <Trash2 size={14} strokeWidth={2.4} />
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      <WeightSheet date={weightSheetDate} onClose={() => setWeightSheetDate(null)} />
      <BodyMetricsSheet open={showBodyMetrics} onClose={() => setShowBodyMetrics(false)} />
    </div>
  );
}

function Hairline(): JSX.Element {
  return <span className="my-auto h-[30px] w-px calzy-hairline" />;
}

function TrendStat({
  icon: Icon,
  title,
  value,
}: {
  icon: typeof List;
  title: string;
  value: string;
}): JSX.Element {
  return (
    <div className="flex flex-1 flex-col items-center gap-1">
      <span className="flex items-center gap-[3px] text-ink-faint">
        <Icon size={9} strokeWidth={3} />
        <span className="text-[11px] font-medium">{title}</span>
      </span>
      <span className="metric text-[16px] text-ink">{value}</span>
    </div>
  );
}

function MonthButton({
  direction,
  onClick,
}: {
  direction: "prev" | "next";
  onClick: () => void;
}): JSX.Element {
  return (
    <button
      type="button"
      onClick={() => {
        haptics.selection();
        onClick();
      }}
      aria-label={direction === "prev" ? "Previous month" : "Next month"}
      className="pressable grid h-[30px] w-[30px] place-items-center rounded-full bg-well text-ink"
    >
      <span className="text-[13px] font-bold leading-none">
        {direction === "prev" ? "‹" : "›"}
      </span>
    </button>
  );
}

function WeightSheet({ date, onClose }: { date: Date | null; onClose: () => void }): JSX.Element {
  const store = useAppStore();
  const [value, setValue] = useState<number>(store.profile.currentWeightKg);
  const [seeded, setSeeded] = useState<string | null>(null);

  if (date && seeded !== date.toISOString()) {
    setSeeded(date.toISOString());
    setValue(store.weightOn(date)?.kilograms ?? store.profile.currentWeightKg);
  }

  return (
    <BottomSheet
      open={date !== null}
      onClose={() => {
        setSeeded(null);
        onClose();
      }}
    >
      <div className="flex flex-col items-center gap-5 px-[22px] pt-3">
        <div className="flex flex-col items-center gap-1 text-center">
          <h3 className="text-[19px] font-bold text-ink">
            {date && isToday(date) ? "Today's weight" : date ? abbreviatedDate(date) : ""}
          </h3>
          <p className="text-[13px] text-ink-faint">Small daily swings are normal — trends matter.</p>
        </div>

        <div className="flex items-baseline gap-1">
          <span className="metric text-[52px] leading-none text-ink">{value.toFixed(1)}</span>
          <span className="text-[18px] font-medium text-ink-faint">kg</span>
        </div>

        <div className="w-full px-1">
          <Slider
            value={value}
            min={35}
            max={200}
            step={0.1}
            label="Weight"

            onChange={(next) => {
              haptics.selection();
              setValue(next);
            }}
          />
        </div>

        <PrimaryButton
          onClick={() => {
            if (date) store.logWeight(value, date);
            haptics.success();
            setSeeded(null);
            onClose();
          }}
        >
          Save weight
        </PrimaryButton>
      </div>
    </BottomSheet>
  );
}

function BodyMetricsSheet({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}): JSX.Element {
  const store = useAppStore();
  const [height, setHeight] = useState<number>(store.profile.heightCm);
  const [weight, setWeight] = useState<number>(store.profile.currentWeightKg);
  const [seeded, setSeeded] = useState<boolean>(false);

  if (open && !seeded) {
    setSeeded(true);
    setHeight(store.profile.heightCm);
    setWeight(store.profile.currentWeightKg);
  }

  const bmi = bmiOf(height, weight);

  return (
    <BottomSheet
      open={open}
      onClose={() => {
        setSeeded(false);
        onClose();
      }}
    >
      <div className="flex flex-col items-center gap-4 px-[22px] pt-3">
        <h3 className="text-[19px] font-bold text-ink">Body metrics</h3>

        <div className="flex w-full flex-col gap-4">
          <div>
            <div className="flex items-baseline justify-between">
              <span className="text-[14px] font-semibold text-ink">Height</span>
              <span className="metric text-[15px] text-ink-soft">{height.toFixed(0)} cm</span>
            </div>
            <Slider
              value={height}
              min={130}
              max={220}
              step={1}
              onChange={setHeight}
              label="Height"
            />
          </div>
          <div>
            <div className="flex items-baseline justify-between">
              <span className="text-[14px] font-semibold text-ink">Weight</span>
              <span className="metric text-[15px] text-ink-soft">{weight.toFixed(1)} kg</span>
            </div>
            <Slider
              value={weight}
              min={35}
              max={200}
              step={0.1}
              onChange={setWeight}
              label="Weight"
            />
          </div>
        </div>

        <div className="flex items-center gap-[6px] rounded-full bg-well px-[14px] py-[9px]">
          <span className="text-[13px] font-medium text-ink-soft">BMI</span>
          <span className="metric text-[17px] text-ink">{bmi.toFixed(1)}</span>
          <span className="text-[12px] font-semibold text-ink-soft">{bmiCategory(bmi)}</span>
        </div>

        <PrimaryButton
          onClick={() => {
            store.setProfile({ heightCm: height, currentWeightKg: weight });
            store.logWeight(weight);
            haptics.success();
            setSeeded(false);
            onClose();
          }}
        >
          Save
        </PrimaryButton>
      </div>
    </BottomSheet>
  );
}
