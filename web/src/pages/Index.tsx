import type { JSX } from "react";
import { useState } from "react";

import { TabBar, type AppTab } from "@/components/calzy/TabBar";
import { DescribeSheet } from "@/features/DescribeSheet";
import { ExerciseSheet } from "@/features/ExerciseSheet";
import { EditMealSheet } from "@/features/EditMealSheet";
import { MealDetail } from "@/features/MealDetail";
import { MealResult, type MealDraft } from "@/features/MealResult";
import { ScanSheet } from "@/features/ScanSheet";
import { SavedSheet, SearchSheet } from "@/features/SearchSheet";
import { Home, type HomeRoute } from "@/pages/Home";
import { Onboarding } from "@/pages/Onboarding";
import { Progress } from "@/pages/Progress";
import { Settings } from "@/pages/Settings";
import { useLanguage } from "@/lib/i18n";
import type { MealEntry } from "@/lib/types";
import { useAppStore } from "@/store/AppStore";

/** Root shell: onboarding gate, tab content, floating tab bar and sheet routing. */
const Index = (): JSX.Element => {
  const store = useAppStore();
  const { language } = useLanguage();

  const [tab, setTab] = useState<AppTab>("home");
  const [route, setRoute] = useState<HomeRoute | null>(null);
  const [draft, setDraft] = useState<MealDraft | null>(null);
  const [openMeal, setOpenMeal] = useState<MealEntry | null>(null);
  const [editingMeal, setEditingMeal] = useState<MealEntry | null>(null);

  const onResult = (next: MealDraft): void => {
    setRoute(null);
    setDraft(next);
  };

  return (
    <div
      lang={language.code}
      dir={language.isRTL === true ? "rtl" : "ltr"}
      className="calzy-backdrop flex h-full w-full justify-center overflow-hidden"
    >
      <main className="relative h-full w-full max-w-[520px] overflow-hidden bg-transparent sm:shadow-[0_0_80px_rgba(0,0,0,0.06)]">
        {!store.profile.hasOnboarded ? (
          <Onboarding />
        ) : (
          <>
            <div key={tab} className="animate-rise-in h-full" role="tabpanel">
              {tab === "home" && (
                <Home
                  onRoute={setRoute}
                  onOpenMeal={setOpenMeal}
                  onEditMeal={setEditingMeal}
                />
              )}
              {tab === "progress" && <Progress />}
              {tab === "settings" && <Settings />}
            </div>

            <TabBar active={tab} onChange={setTab} />

            <ScanSheet
              open={route === "scan"}
              onClose={() => setRoute(null)}
              onResult={onResult}
            />
            <DescribeSheet
              open={route === "describe"}
              onClose={() => setRoute(null)}
              onResult={onResult}
            />
            <SearchSheet open={route === "search"} onClose={() => setRoute(null)} />
            <SavedSheet open={route === "saved"} onClose={() => setRoute(null)} />
            <ExerciseSheet open={route === "exercise"} onClose={() => setRoute(null)} />

            <MealResult draft={draft} onClose={() => setDraft(null)} />
            <MealDetail
              meal={openMeal}
              onClose={() => setOpenMeal(null)}
              onEdit={setEditingMeal}
            />
            <EditMealSheet
              meal={editingMeal}
              onClose={() => setEditingMeal(null)}
              onSaved={(updated) => {
                // Keep an open detail sheet in sync with the correction.
                setOpenMeal((current) => (current?.id === updated.id ? updated : current));
              }}
            />
          </>
        )}
      </main>
    </div>
  );
};

export default Index;
