import { Check, Search, X } from "lucide-react";
import type { JSX } from "react";
import { Fragment, useMemo, useState } from "react";

import { Card } from "@/components/calzy/Primitives";
import { FullScreenSheet } from "@/components/calzy/Sheet";
import { haptics } from "@/lib/haptics";
import { languages, useLanguage } from "@/lib/i18n";
import { useAppStore } from "@/store/AppStore";

/** Full list of supported interface languages with live search. */
export function LanguageSheet({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}): JSX.Element {
  const store = useAppStore();
  const { language, t } = useLanguage();
  const [query, setQuery] = useState<string>("");

  const results = useMemo(() => {
    const trimmed = query.trim().toLowerCase();
    if (trimmed === "") return languages;
    return languages.filter(
      (item) =>
        item.englishName.toLowerCase().includes(trimmed) ||
        item.nativeName.toLowerCase().includes(trimmed) ||
        item.code.toLowerCase().includes(trimmed),
    );
  }, [query]);

  return (
    <FullScreenSheet open={open} onClose={onClose} title={t("l.title")}>
      <div className="flex flex-col gap-[14px] px-5 pb-10 pt-[6px]">
        <div className="flex items-center gap-[9px] rounded-[18px] bg-white/70 px-[14px] py-[13px] shadow-[0_1px_3px_rgba(0,0,0,0.05)]">
          <Search size={15} className="shrink-0 text-ink-faint" strokeWidth={2.6} />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder={t("l.search")}
            autoComplete="off"
            aria-label={t("l.search")}
            className="min-w-0 flex-1 bg-transparent text-[16px] text-ink outline-none placeholder:text-ink-faint"
          />
          {query !== "" && (
            <button
              type="button"
              onClick={() => setQuery("")}
              aria-label="Clear search"
              className="pressable shrink-0 text-ink-faint"
            >
              <X size={15} strokeWidth={2.8} />
            </button>
          )}
        </div>

        {results.length === 0 ? (
          <p className="pt-10 text-center text-[14px] text-ink-faint">
            No language matches “{query}”
          </p>
        ) : (
          <Card radius={22} padding={0}>
            <div className="overflow-hidden" style={{ borderRadius: 22 }}>
              {results.map((item, index) => {
                const isSelected = item.code === language.code;
                return (
                  <Fragment key={item.code}>
                    {index > 0 && <div className="ml-[56px] h-px calzy-hairline" />}
                    <button
                      type="button"
                      dir="ltr"
                      onClick={() => {
                        if (isSelected) return;
                        haptics.success();
                        store.setProfile({ languageCode: item.code });
                      }}
                      aria-pressed={isSelected}
                      className="pressable flex w-full items-center gap-[13px] px-[14px] py-[13px] text-left"
                    >
                      <span className="w-8 shrink-0 text-center text-[25px] leading-none">
                        {item.flag}
                      </span>
                      <span className="min-w-0 flex-1">
                        <span className="block truncate text-[16px] font-semibold text-ink">
                          {item.nativeName}
                        </span>
                        <span className="block truncate text-[12px] text-ink-faint">
                          {item.englishName}
                        </span>
                      </span>
                      {isSelected && (
                        <Check size={18} className="animate-pop-in shrink-0 text-mint" strokeWidth={3} />
                      )}
                    </button>
                  </Fragment>
                );
              })}
            </div>
          </Card>
        )}

        <p className="px-6 text-center text-[12px] text-ink-faint">{t("l.note")}</p>
      </div>
    </FullScreenSheet>
  );
}
