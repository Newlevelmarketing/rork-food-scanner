import {
  Bell,
  ChevronRight,
  Footprints,
  Heart,
  Lock,
  Mail,
  Scale,
  Sparkles,
  Target,
  FileText,
  BadgeCheck,
  ArrowUpRight,
  Globe,
} from "lucide-react";
import type { JSX } from "react";
import { useState, type ReactNode } from "react";

import { Card, Toggle } from "@/components/calzy/Primitives";
import { Paywall } from "@/features/Paywall";
import { LanguageSheet } from "@/features/settings/LanguageSheet";
import {
  AccountSheet,
  ActivitySheet,
  GoalsWeightSheet,
  NutritionGoalsSheet,
  RemindersSheet,
} from "@/features/settings/SettingsSheets";
import { haptics } from "@/lib/haptics";
import { useLanguage } from "@/lib/i18n";
import { useAppStore } from "@/store/AppStore";

type SettingsRoute =
  | "account"
  | "nutrition"
  | "goals"
  | "reminders"
  | "activity"
  | "language"
  | null;

/** Settings tab: grouped cards with section captions. */
export function Settings(): JSX.Element {
  const store = useAppStore();
  const profile = store.profile;
  const { language, t } = useLanguage();

  const [route, setRoute] = useState<SettingsRoute>(null);
  const [showPaywall, setShowPaywall] = useState<boolean>(false);
  const [confirmErase, setConfirmErase] = useState<boolean>(false);

  const displayName = profile.name.trim() === "" ? "Your profile" : profile.name;
  const initials =
    displayName
      .split(" ")
      .map((part) => part[0])
      .filter(Boolean)
      .slice(0, 2)
      .join("")
      .toUpperCase() || "C";

  return (
    <div className="no-scrollbar page-bottom h-full overflow-y-auto overscroll-contain">
      <h1 className="page-top-lg px-5 pb-3 text-center text-[17px] font-semibold text-ink">
        {t("s.title")}
      </h1>

      <div className="flex flex-col gap-[22px]">
        <Section title={t("s.account")}>
          <button
            type="button"
            onClick={() => {
              haptics.tap();
              setRoute("account");
            }}
            className="pressable flex w-full items-center gap-[13px] p-[14px] text-left"
          >
            <span className="metric grid h-12 w-12 shrink-0 place-items-center rounded-full bg-ink text-[17px] text-white">
              {initials}
            </span>
            <span className="min-w-0 flex-1">
              <span className="flex items-center gap-[5px]">
                <span className="truncate text-[17px] font-semibold text-ink">{displayName}</span>
                <BadgeCheck size={14} className="shrink-0 text-mint" strokeWidth={2.6} />
              </span>
              <span className="block text-[13px] text-ink-faint">
                {profile.isPro ? t("s.pro") : t("s.free")}
              </span>
            </span>
            <ChevronRight size={16} className="shrink-0 text-ink-faint" strokeWidth={2.6} />
          </button>
        </Section>

        <Section title={t("s.personal")}>
          <NavRow icon={<Target size={17} />} title={t("s.goals")} onClick={() => setRoute("nutrition")} />
          <Divider />
          <NavRow icon={<Scale size={17} />} title={t("s.weight")} onClick={() => setRoute("goals")} />
          <Divider />
          <NavRow icon={<Bell size={17} />} title={t("s.reminders")} onClick={() => setRoute("reminders")} />
          <Divider />
          <NavRow icon={<Footprints size={17} />} title={t("s.activity")} onClick={() => setRoute("activity")} />
        </Section>

        <Section title={t("s.app")}>
          <NavRow
            icon={<Globe size={17} />}
            title={t("s.language")}
            onClick={() => setRoute("language")}
            value={
              <span dir="ltr" className="flex items-center gap-[6px]">
                <span className="text-[16px] leading-none">{language.flag}</span>
                <span className="truncate text-[15px] text-ink-faint">{language.nativeName}</span>
              </span>
            }
          />
        </Section>

        <Section title={t("s.integrations")}>
          <button
            type="button"
            onClick={() => {
              haptics.tap();
              store.setProfile({ healthSynced: !profile.healthSynced });
            }}
            className="pressable flex w-full items-center gap-[13px] p-[14px] text-left"
          >
            <Heart size={19} className="w-7 shrink-0 text-protein" fill="currentColor" strokeWidth={0} />
            <span className="min-w-0 flex-1">
              <span className="block text-[16px] font-semibold text-ink">{t("s.health")}</span>
              <span className="block text-[12px] text-ink-faint">{t("s.healthSub")}</span>
            </span>
            <span
              className={`shrink-0 text-[14px] font-semibold ${profile.healthSynced ? "text-mint" : "text-ink-faint"}`}
            >
              {profile.healthSynced ? t("s.connected") : t("s.connect")}
            </span>
          </button>
        </Section>

        <Section title={t("s.preferences")}>
          <div className="flex items-center gap-[13px] p-[14px]">
            <span className="w-7 shrink-0 text-[22px] leading-none">🎭</span>
            <div className="min-w-0 flex-1">
              <p className="text-[16px] font-semibold text-ink">{t("s.jester")}</p>
              <p className="text-[12px] text-ink-faint">{t("s.jesterSub")}</p>
            </div>
            <Toggle
              checked={profile.jesterMode}
              onChange={(value) => {
                haptics.selection();
                store.setProfile({ jesterMode: value });
              }}
            />
          </div>
        </Section>

        <Section title={t("s.subscription")}>
          <button
            type="button"
            onClick={() => {
              haptics.tap();
              setShowPaywall(true);
            }}
            className="pressable flex w-full items-center gap-[13px] p-[14px] text-left"
          >
            <span
              className="grid h-11 w-11 shrink-0 place-items-center rounded-full"
              style={{
                background: "linear-gradient(135deg, hsl(var(--plum)), hsl(var(--protein)))",
              }}
            >
              <Sparkles size={17} className="text-white" strokeWidth={2.6} />
            </span>
            <span className="min-w-0 flex-1">
              <span className="flex items-center gap-[7px]">
                <span className="text-[16px] font-bold text-ink">ModernBody Pro</span>
                <span
                  className={`rounded-full px-[7px] py-[3px] text-[10px] font-bold text-white ${profile.isPro ? "bg-mint" : "bg-ink"}`}
                >
                  {profile.isPro ? t("s.active") : t("s.upgrade")}
                </span>
              </span>
              <span className="block text-[12px] text-ink-faint">
                {profile.isPro ? "Unlimited scans and insights" : "Unlimited scans, deeper insights"}
              </span>
            </span>
            <ChevronRight size={16} className="shrink-0 text-ink-faint" strokeWidth={2.6} />
          </button>
        </Section>

        <Section title={t("s.support")}>
          <LinkRow icon={<FileText size={17} />} title={t("s.terms")} href="https://rork.app/terms" />
          <Divider />
          <LinkRow icon={<Lock size={17} />} title={t("s.privacy")} href="https://rork.app/privacy" />
          <Divider />
          <LinkRow icon={<Mail size={17} />} title={t("s.email")} href="mailto:support@calzy.app" />
        </Section>

        <div className="px-5">
          {confirmErase ? (
            <div className="flex flex-col gap-3">
              <p className="text-center text-[13px] text-ink-soft">
                This removes every meal, weight and photo from this browser.
              </p>
              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => setConfirmErase(false)}
                  className="pressable flex-1 rounded-full bg-black/[0.06] py-[15px] text-[15px] font-semibold text-ink"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={() => {
                    store.eraseAll();
                    haptics.warning();
                    setConfirmErase(false);
                  }}
                  className="pressable flex-1 rounded-full bg-protein py-[15px] text-[15px] font-semibold text-white"
                >
                  Erase everything
                </button>
              </div>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => setConfirmErase(true)}
              className="pressable w-full rounded-full bg-protein/10 py-[15px] text-[15px] font-semibold text-protein"
            >
              {t("s.erase")}
            </button>
          )}
        </div>

        <p className="pb-2 text-center text-[12px] text-ink-faint">{t("s.version")} 1.0.0</p>
      </div>

      <AccountSheet open={route === "account"} onClose={() => setRoute(null)} />
      <NutritionGoalsSheet open={route === "nutrition"} onClose={() => setRoute(null)} />
      <GoalsWeightSheet open={route === "goals"} onClose={() => setRoute(null)} />
      <RemindersSheet open={route === "reminders"} onClose={() => setRoute(null)} />
      <ActivitySheet open={route === "activity"} onClose={() => setRoute(null)} />
      <LanguageSheet open={route === "language"} onClose={() => setRoute(null)} />
      <Paywall open={showPaywall} onClose={() => setShowPaywall(false)} />
    </div>
  );
}

function Section({ title, children }: { title: string; children: ReactNode }): JSX.Element {
  return (
    <section className="flex flex-col gap-[9px]">
      <h2 className="pl-6 text-[12px] font-semibold tracking-wide text-ink-faint">{title}</h2>
      <div className="px-5">
        <Card radius={22} padding={0}>
          <div className="overflow-hidden" style={{ borderRadius: 22 }}>
            {children}
          </div>
        </Card>
      </div>
    </section>
  );
}

function Divider(): JSX.Element {
  return <div className="ml-[55px] h-px calzy-hairline" />;
}

function NavRow({
  icon,
  title,
  onClick,
  value,
}: {
  icon: ReactNode;
  title: string;
  onClick: () => void;
  /** Optional trailing preview of the current setting. */
  value?: ReactNode;
}): JSX.Element {
  return (
    <button
      type="button"
      onClick={() => {
        haptics.tap();
        onClick();
      }}
      className="pressable flex w-full items-center gap-[13px] px-[14px] py-[15px] text-left"
    >
      <span className="grid w-7 shrink-0 place-items-center text-ink">{icon}</span>
      <span className="min-w-0 flex-1 truncate text-[16px] font-medium text-ink">{title}</span>
      {value !== undefined && <span className="min-w-0 shrink">{value}</span>}
      <ChevronRight size={16} className="shrink-0 text-ink-faint" strokeWidth={2.6} />
    </button>
  );
}

function LinkRow({
  icon,
  title,
  href,
}: {
  icon: ReactNode;
  title: string;
  href: string;
}): JSX.Element {
  return (
    <a
      href={href}
      target="_blank"
      rel="noreferrer"
      className="pressable flex w-full items-center gap-[13px] px-[14px] py-[15px]"
    >
      <span className="grid w-7 shrink-0 place-items-center text-ink">{icon}</span>
      <span className="flex-1 text-[16px] font-medium text-ink">{title}</span>
      <ArrowUpRight size={15} className="shrink-0 text-ink-faint" strokeWidth={2.6} />
    </a>
  );
}
