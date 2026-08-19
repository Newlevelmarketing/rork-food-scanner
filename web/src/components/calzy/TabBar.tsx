import type { JSX } from "react";

import { Home, LineChart, Settings2, type LucideIcon } from "lucide-react";

import { haptics } from "@/lib/haptics";
import { useT } from "@/lib/i18n";
import { cn } from "@/lib/utils";

export type AppTab = "home" | "progress" | "settings";

const tabs: Array<{ id: AppTab; labelKey: string; icon: LucideIcon }> = [
  { id: "home", labelKey: "tab.home", icon: Home },
  { id: "progress", labelKey: "tab.progress", icon: LineChart },
  { id: "settings", labelKey: "tab.settings", icon: Settings2 },
];

/** Floating pill tab bar pinned to the bottom of the shell. */
export function TabBar({
  active,
  onChange,
}: {
  active: AppTab;
  onChange: (tab: AppTab) => void;
}): JSX.Element {
  const t = useT();

  return (
    <nav className="tabbar-bottom pointer-events-none absolute inset-x-0 bottom-0 z-30 flex justify-center px-6 sm:px-10">
      <div className="calzy-glass pointer-events-auto flex w-full max-w-[340px] gap-1 rounded-full p-[6px]">
        {tabs.map((tab) => {
          const isActive = tab.id === active;
          const Icon = tab.icon;
          return (
            <button
              key={tab.id}
              type="button"
              onClick={() => {
                haptics.tap();
                onChange(tab.id);
              }}
              className={cn(
                "pressable relative flex flex-1 flex-col items-center gap-1 rounded-full py-[11px] transition-colors duration-200",
                isActive ? "text-ink" : "text-ink-faint",
              )}
            >
              {isActive && (
                <span className="animate-pop-in absolute inset-0 rounded-full bg-ink/[0.07]" />
              )}
              <Icon
                size={19}
                strokeWidth={isActive ? 2.6 : 2.1}
                className="relative"
                fill={isActive ? "currentColor" : "none"}
                fillOpacity={isActive ? 0.12 : 0}
              />
              <span className="relative truncate px-1 text-[11px] font-semibold">
                {t(tab.labelKey)}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
}
