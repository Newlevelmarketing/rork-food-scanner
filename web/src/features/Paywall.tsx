import {
  Circle,
  CircleDot,
  HeartPulse,
  Scan,
  Sparkles,
  TrendingUp,
  type LucideIcon,
} from "lucide-react";
import type { JSX } from "react";
import { useState } from "react";

import { Card, PrimaryButton } from "@/components/calzy/Primitives";
import { FullScreenSheet } from "@/components/calzy/Sheet";
import { haptics } from "@/lib/haptics";
import { cn } from "@/lib/utils";
import { useAppStore } from "@/store/AppStore";

type Plan = "monthly" | "yearly";

const planMeta: Record<Plan, { title: string; price: string; caption: string }> = {
  monthly: { title: "Monthly", price: "$9.99", caption: "Billed every month" },
  yearly: { title: "Yearly", price: "$59.99", caption: "Just $5.00 / month" },
};

const perks: Array<{ icon: LucideIcon; title: string; detail: string }> = [
  { icon: Scan, title: "Unlimited AI scans", detail: "Photograph any plate, any time" },
  { icon: TrendingUp, title: "Deep progress insights", detail: "Trends, streaks and weekly reviews" },
  { icon: Sparkles, title: "Jester Mode", detail: "Let the AI roast your late-night snacks" },
  { icon: HeartPulse, title: "Apple Health sync", detail: "Everything stays in one place" },
];

/** ModernBody Pro upgrade screen. */
export function Paywall({ open, onClose }: { open: boolean; onClose: () => void }): JSX.Element {
  const store = useAppStore();
  const [plan, setPlan] = useState<Plan>("yearly");

  return (
    <FullScreenSheet
      open={open}
      onClose={onClose}
      title="ModernBody Pro"
      leading={
        <button type="button" onClick={onClose} className="pressable text-[15px] font-medium text-ink-soft">
          Close
        </button>
      }
      footer={
        <div className="flex flex-col gap-2">
          <PrimaryButton
            onClick={() => {
              store.setProfile({ isPro: !store.profile.isPro });
              haptics.success();
              onClose();
            }}
          >
            {store.profile.isPro ? "Manage subscription" : "Start 3-day free trial"}
          </PrimaryButton>
          <button
            type="button"
            onClick={onClose}
            className="text-center text-[14px] font-medium text-ink-faint"
          >
            Not now
          </button>
        </div>
      }
    >
      <div className="flex flex-col gap-[22px] px-5 pb-8">
        <div className="relative grid h-[180px] place-items-center">
          <div
            className="animate-breathe absolute h-[220px] w-[220px] rounded-full"
            style={{
              background: "radial-gradient(circle, hsl(var(--plum) / 0.4) 0%, transparent 62%)",
            }}
          />
          <img
            src="/icon.png"
            alt="ModernBody app icon"
            draggable={false}
            className="relative h-[84px] w-[84px] rounded-[24px] object-cover"
            style={{ boxShadow: "0 10px 34px hsl(var(--plum) / 0.35)" }}
          />
        </div>

        <div className="flex flex-col items-center gap-2">
          <h1 className="metric text-[32px] leading-none text-ink">ModernBody Pro</h1>
          <p className="text-[16px] text-ink-soft">Effortless tracking, sharper results.</p>
        </div>

        <Card radius={24} padding={16}>
          <div className="flex flex-col gap-3">
            {perks.map((perk) => {
              const Icon = perk.icon;
              return (
                <div key={perk.title} className="flex items-center gap-[13px]">
                  <span className="grid h-10 w-10 shrink-0 place-items-center rounded-[13px] bg-well">
                    <Icon size={16} className="text-ink" strokeWidth={2.5} />
                  </span>
                  <div className="min-w-0">
                    <p className="text-[15px] font-semibold text-ink">{perk.title}</p>
                    <p className="text-[12px] text-ink-faint">{perk.detail}</p>
                  </div>
                </div>
              );
            })}
          </div>
        </Card>

        <div className="flex flex-col gap-[10px]">
          {(["monthly", "yearly"] as Plan[]).map((option) => {
            const active = plan === option;
            const meta = planMeta[option];
            const Icon = active ? CircleDot : Circle;
            return (
              <button
                key={option}
                type="button"
                onClick={() => {
                  haptics.selection();
                  setPlan(option);
                }}
                className={cn(
                  "pressable flex items-center gap-[13px] rounded-[22px] bg-white/[0.78] p-4 text-left transition-all duration-200",
                  active ? "ring-2 ring-inset ring-ink" : "ring-0",
                )}
              >
                <Icon
                  size={20}
                  className={cn("shrink-0", active ? "text-ink" : "text-ink-faint/50")}
                  strokeWidth={active ? 3 : 2}
                />
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-[7px]">
                    <span className="text-[16px] font-bold text-ink">{meta.title}</span>
                    {option === "yearly" && (
                      <span className="rounded-full bg-mint px-[7px] py-[3px] text-[10px] font-bold text-white">
                        SAVE 50%
                      </span>
                    )}
                  </div>
                  <p className="text-[12px] text-ink-faint">{meta.caption}</p>
                </div>
                <span className="metric shrink-0 text-[19px] text-ink">{meta.price}</span>
              </button>
            );
          })}
        </div>

        <p className="px-6 text-center text-[11px] text-ink-faint">
          Cancel anytime. Prices shown are illustrative in this preview build.
        </p>
      </div>
    </FullScreenSheet>
  );
}
