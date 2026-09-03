import { Bookmark, CheckCircle2, Leaf, Minus, Plus, Sparkles, Theater, X } from "lucide-react";
import type { JSX } from "react";
import { useEffect, useMemo, useState } from "react";

import { Card, PrimaryButton } from "@/components/calzy/Primitives";
import { FullScreenSheet } from "@/components/calzy/Sheet";
import { stampOnDay } from "@/lib/dates";
import { haptics } from "@/lib/haptics";
import { currentSlot, slotMeta, slotOrder } from "@/lib/nutrition";
import type { AnalysisResult } from "@/lib/ai";
import { resultToItems } from "@/lib/ai";
import type { EntrySource, FoodItem, MealSlot } from "@/lib/types";
import { cn } from "@/lib/utils";
import { useAppStore } from "@/store/AppStore";

export interface MealDraft {
  result: AnalysisResult;
  photo?: string;
  source: EntrySource;
}

/** Review + confirm screen shown after a scan, description or search pick. */
export function MealResult({
  draft,
  onClose,
}: {
  draft: MealDraft | null;
  onClose: () => void;
}): JSX.Element {
  const store = useAppStore();

  const [title, setTitle] = useState<string>("");
  const [items, setItems] = useState<FoodItem[]>([]);
  const [portions, setPortions] = useState<number>(1);
  const [slot, setSlot] = useState<MealSlot>(currentSlot());

  useEffect(() => {
    if (!draft) return;
    setTitle(draft.result.title);
    setItems(resultToItems(draft.result));
    setPortions(1);
    setSlot(currentSlot());
  }, [draft]);

  const totals = useMemo(() => {
    const calories = Math.round(items.reduce((sum, item) => sum + item.calories, 0) * portions);
    const protein = items.reduce((sum, item) => sum + item.protein, 0) * portions;
    const carbs = items.reduce((sum, item) => sum + item.carbs, 0) * portions;
    const fat = items.reduce((sum, item) => sum + item.fat, 0) * portions;
    return { calories, protein, carbs, fat };
  }, [items, portions]);

  if (!draft) return <FullScreenSheet open={false} onClose={onClose}>{null}</FullScreenSheet>;

  const score = draft.result.healthScore;
  const scoreToken = score >= 8 ? "--mint" : score >= 5 ? "--fat" : "--protein";
  const saved = store.isSaved(title);

  /**
   * Logs against the day the user is viewing, keeping the current clock time.
   * Seconds carry over so two meals logged onto a past day within the same
   * minute still order correctly.
   */
  const mergedDate = (): Date => stampOnDay(store.selectedDate);

  const save = (): void => {
    if (items.length === 0) return;
    store.addMeal({
      title: title.trim() === "" ? "Meal" : title,
      date: mergedDate().toISOString(),
      slot,
      source: draft.source,
      items,
      portions,
      photo: draft.photo,
      healthScore: score,
      quip: draft.result.quip,
    });
    haptics.success();
    onClose();
  };

  return (
    <FullScreenSheet
      open
      onClose={onClose}
      title="Review meal"
      trailing={
        <button
          type="button"
          onClick={() => {
            store.toggleSaved(title, items, slot);
            haptics.tap();
          }}
          aria-label={saved ? "Remove bookmark" : "Bookmark meal"}
          className="pressable text-ink"
        >
          <Bookmark size={19} fill={saved ? "currentColor" : "none"} strokeWidth={2.2} />
        </button>
      }
      footer={
        <PrimaryButton onClick={save} disabled={items.length === 0}>
          <CheckCircle2 size={18} strokeWidth={2.5} />
          Log {totals.calories} kcal
        </PrimaryButton>
      }
    >
      <div className="animate-rise-in flex flex-col gap-4 px-5 py-4">
        {draft.photo && (
          <div className="relative h-[210px] overflow-hidden rounded-[26px] bg-well">
            <img src={draft.photo} alt="" className="h-full w-full object-cover" />
            <span className="absolute bottom-[14px] left-[14px] flex items-center gap-[5px] rounded-full bg-black/[0.42] px-[11px] py-[6px] text-white backdrop-blur-sm">
              <Sparkles size={11} strokeWidth={2.8} />
              <span className="text-[12px] font-semibold">AI estimate</span>
            </span>
          </div>
        )}

        <Card radius={26} padding={20}>
          <div className="flex flex-col items-center gap-4">
            <input
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="Meal name"
              className="w-full bg-transparent text-center text-[21px] font-bold text-ink outline-none placeholder:text-ink-faint"
            />

            <div className="flex items-baseline gap-1">
              <span className="metric text-[46px] leading-none text-ink">{totals.calories}</span>
              <span className="text-[16px] font-medium text-ink-faint">kcal</span>
            </div>

            <div className="flex w-full gap-[10px]">
              <MacroPill name="Protein" value={totals.protein} token="--protein" />
              <MacroPill name="Carbs" value={totals.carbs} token="--carbs" />
              <MacroPill name="Fat" value={totals.fat} token="--fat" />
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
          <div className="flex items-center justify-between">
            <span className="text-[15px] font-semibold text-ink">Portions</span>
            <div className="flex items-center gap-4">
              <StepButton
                label="Decrease portions"
                onClick={() => setPortions((value) => Math.max(0.25, Math.round((value - 0.25) * 100) / 100))}
              >
                <Minus size={14} strokeWidth={3} />
              </StepButton>
              <span className="metric min-w-[42px] text-center text-[19px] text-ink">
                {portions % 1 === 0 ? portions.toFixed(0) : portions.toFixed(2).replace(/0$/, "")}
              </span>
              <StepButton
                label="Increase portions"
                onClick={() => setPortions((value) => Math.min(10, Math.round((value + 0.25) * 100) / 100))}
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

        {items.length > 0 && (
          <Card radius={22} padding={16}>
            {items.map((item, index) => (
              <div key={item.id}>
                <div className="flex items-center gap-3 py-3">
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-[15px] font-semibold text-ink">{item.name}</p>
                    <p className="text-[12px] text-ink-faint">{item.quantity}</p>
                  </div>
                  <span className="metric shrink-0 text-[16px] text-ink-soft">{item.calories}</span>
                  <button
                    type="button"
                    onClick={() => {
                      haptics.tap();
                      setItems((current) => current.filter((entry) => entry.id !== item.id));
                    }}
                    aria-label={`Remove ${item.name}`}
                    className="pressable shrink-0 text-ink-faint/60"
                  >
                    <X size={17} strokeWidth={2.6} />
                  </button>
                </div>
                {index < items.length - 1 && <div className="h-px calzy-hairline" />}
              </div>
            ))}
          </Card>
        )}

        {draft.result.quip && (
          <Card radius={20} padding={14}>
            <div className="flex items-start gap-[10px]">
              {store.profile.jesterMode ? (
                <Theater size={15} className="mt-[2px] shrink-0 text-fat" strokeWidth={2.4} />
              ) : (
                <Sparkles size={15} className="mt-[2px] shrink-0 text-plum" strokeWidth={2.4} />
              )}
              <p className="text-[14px] font-medium text-ink-soft">{draft.result.quip}</p>
            </div>
          </Card>
        )}
      </div>
    </FullScreenSheet>
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
      className="flex flex-1 flex-col items-center gap-[3px] rounded-[16px] py-[10px]"
      style={{ backgroundColor: `hsl(var(${token}) / 0.1)` }}
    >
      <span className="metric text-[17px] text-ink">{Math.round(value)}g</span>
      <span className="text-[11px] font-medium text-ink-soft">{name}</span>
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
