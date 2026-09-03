import { Bookmark, Plus, Search, Trash2 } from "lucide-react";
import type { JSX } from "react";
import { useEffect, useMemo, useState } from "react";

import { Card } from "@/components/calzy/Primitives";
import { FullScreenSheet } from "@/components/calzy/Sheet";
import { stampOnDay } from "@/lib/dates";
import { foodToItem, searchFoods, type FoodRecord } from "@/lib/foods";
import { haptics } from "@/lib/haptics";
import { currentSlot, itemsCalories, slotMeta } from "@/lib/nutrition";
import type { SavedFood } from "@/lib/types";
import { useAppStore } from "@/store/AppStore";

function loggedDate(selected: Date): string {
  return stampOnDay(selected).toISOString();
}

/** Offline food search backed by the bundled 100-food table. */
export function SearchSheet({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}): JSX.Element {
  const store = useAppStore();
  const [query, setQuery] = useState<string>("");
  const [justAdded, setJustAdded] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      setQuery("");
      setJustAdded(null);
    }
  }, [open]);

  const results = useMemo(() => searchFoods(query), [query]);

  const log = (record: FoodRecord): void => {
    const item = foodToItem(record);
    store.addMeal({
      title: record.name,
      date: loggedDate(store.selectedDate),
      slot: currentSlot(),
      source: "search",
      items: [item],
      portions: 1,
      healthScore: 6,
    });
    haptics.success();
    setJustAdded(record.name);
    window.setTimeout(() => setJustAdded(null), 900);
  };

  return (
    <FullScreenSheet open={open} onClose={onClose} title="Search foods">
      <div className="sticky top-0 z-10 bg-gradient-to-b from-white/70 to-transparent px-5 pb-3 pt-4 backdrop-blur-sm">
        <div className="calzy-card flex items-center gap-[10px] px-4 py-[13px]" style={{ borderRadius: 18 }}>
          <Search size={17} className="shrink-0 text-ink-faint" strokeWidth={2.5} />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            autoFocus
            placeholder="Chicken, oats, latte…"
            className="min-w-0 flex-1 bg-transparent text-[16px] text-ink outline-none placeholder:text-ink-faint"
          />
          {query && (
            <button
              type="button"
              onClick={() => setQuery("")}
              className="pressable text-[13px] font-semibold text-ink-faint"
            >
              Clear
            </button>
          )}
        </div>
      </div>

      <div className="flex flex-col gap-[10px] px-5 pb-8">
        {results.length === 0 ? (
          <div className="flex flex-col items-center gap-2 py-14 text-center">
            <Search size={30} className="text-ink-faint" strokeWidth={1.8} />
            <p className="text-[16px] font-semibold text-ink-soft">No matches for “{query}”</p>
            <p className="text-[13px] text-ink-faint">
              Try the Type flow instead — the AI can estimate anything.
            </p>
          </div>
        ) : (
          results.map((record) => (
            <button
              key={record.name}
              type="button"
              onClick={() => log(record)}
              className="pressable calzy-card flex items-center gap-3 p-[14px] text-left"
              style={{ borderRadius: 20 }}
            >
              <div className="min-w-0 flex-1">
                <p className="truncate text-[15px] font-semibold text-ink">{record.name}</p>
                <p className="text-[12px] text-ink-faint">
                  {record.serving} · {record.p}P {record.c}C {record.f}F
                </p>
              </div>
              <span className="metric shrink-0 text-[17px] text-ink">{record.kcal}</span>
              <span
                className={`grid h-8 w-8 shrink-0 place-items-center rounded-full transition-colors ${
                  justAdded === record.name ? "bg-mint text-white" : "bg-well text-ink"
                }`}
              >
                <Plus size={15} strokeWidth={3} />
              </span>
            </button>
          ))
        )}
      </div>
    </FullScreenSheet>
  );
}

/** Bookmarked meals, ready for one-tap re-logging. */
export function SavedSheet({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}): JSX.Element {
  const store = useAppStore();
  const saved = store.data.saved;

  const relog = (food: SavedFood): void => {
    store.addMeal({
      title: food.title,
      date: loggedDate(store.selectedDate),
      slot: food.slot,
      source: "saved",
      items: food.items,
      portions: 1,
      healthScore: 7,
    });
    haptics.success();
    onClose();
  };

  return (
    <FullScreenSheet open={open} onClose={onClose} title="Saved meals">
      <div className="flex flex-col gap-[10px] px-5 py-4">
        {saved.length === 0 ? (
          <Card radius={26} padding={0}>
            <div className="flex flex-col items-center gap-2 px-8 py-14 text-center">
              <Bookmark size={32} className="text-ink-faint" strokeWidth={1.8} />
              <p className="text-[17px] font-bold text-ink">Nothing saved yet</p>
              <p className="text-[13px] text-ink-soft">
                Tap the bookmark on any meal you review and it lands here for one-tap logging.
              </p>
            </div>
          </Card>
        ) : (
          saved.map((food) => (
            <div
              key={food.id}
              className="calzy-card flex items-center gap-3 p-[14px]"
              style={{ borderRadius: 20 }}
            >
              <button
                type="button"
                onClick={() => relog(food)}
                className="pressable min-w-0 flex-1 text-left"
              >
                <p className="truncate text-[16px] font-semibold text-ink">{food.title}</p>
                <p className="text-[12px] text-ink-faint">
                  {slotMeta[food.slot].label} · {food.items.length} item
                  {food.items.length === 1 ? "" : "s"}
                </p>
              </button>
              <span className="metric shrink-0 text-[17px] text-ink">
                {itemsCalories(food.items)}
              </span>
              <button
                type="button"
                onClick={() => {
                  haptics.tap();
                  store.deleteSaved(food.id);
                }}
                aria-label={`Delete ${food.title}`}
                className="pressable grid h-8 w-8 shrink-0 place-items-center rounded-full text-ink-faint hover:bg-black/[0.05]"
              >
                <Trash2 size={15} strokeWidth={2.3} />
              </button>
            </div>
          ))
        )}
      </div>
    </FullScreenSheet>
  );
}
