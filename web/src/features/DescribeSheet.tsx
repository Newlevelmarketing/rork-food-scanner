import { Sparkles, Wand2 } from "lucide-react";
import type { JSX } from "react";
import { useEffect, useState } from "react";

import { Card, PrimaryButton } from "@/components/calzy/Primitives";
import { FullScreenSheet } from "@/components/calzy/Sheet";
import { analyzeText, messageForError } from "@/lib/ai";
import { haptics } from "@/lib/haptics";
import { useLanguage } from "@/lib/i18n";
import { useAppStore } from "@/store/AppStore";

import type { MealDraft } from "./MealResult";

const examples: string[] = [
  "Two scrambled eggs, sourdough toast and a flat white",
  "Chicken caesar salad with extra parmesan",
  "Large bowl of ramen with pork belly and an egg",
  "Protein shake and a banana after the gym",
];

/** Type-a-meal flow — the same model, no camera required. */
export function DescribeSheet({
  open,
  onClose,
  onResult,
}: {
  open: boolean;
  onClose: () => void;
  onResult: (draft: MealDraft) => void;
}): JSX.Element {
  const store = useAppStore();
  const { language } = useLanguage();
  const [text, setText] = useState<string>("");
  const [busy, setBusy] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open) return;
    setText("");
    setBusy(false);
    setError(null);
  }, [open]);

  const submit = async (): Promise<void> => {
    const trimmed = text.trim();
    if (trimmed.length < 3 || busy) return;
    setBusy(true);
    setError(null);
    try {
      const result = await analyzeText(trimmed, store.profile.jesterMode, language.englishName);
      haptics.success();
      setBusy(false);
      onResult({ result, source: "text" });
    } catch (analysisError) {
      haptics.warning();
      setBusy(false);
      setError(messageForError(analysisError));
    }
  };

  return (
    <FullScreenSheet
      open={open}
      onClose={onClose}
      title="Describe your meal"
      footer={
        <PrimaryButton onClick={() => void submit()} disabled={text.trim().length < 3 || busy}>
          {busy ? (
            <>
              <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />
              Estimating…
            </>
          ) : (
            <>
              <Wand2 size={18} strokeWidth={2.5} />
              Estimate nutrition
            </>
          )}
        </PrimaryButton>
      }
    >
      <div className="flex flex-col gap-4 px-5 py-4">
        <Card radius={26} padding={18}>
          <div className="flex items-center gap-2 pb-3">
            <Sparkles size={15} className="text-plum" strokeWidth={2.5} />
            <span className="text-[13px] font-semibold text-ink-soft">
              Say it how you&apos;d say it out loud
            </span>
          </div>
          <textarea
            value={text}
            onChange={(event) => setText(event.target.value)}
            rows={5}
            autoFocus
            placeholder="e.g. Grilled salmon with roast potatoes and a side salad"
            className="w-full resize-none rounded-[18px] bg-well p-4 text-[16px] leading-relaxed text-ink outline-none placeholder:text-ink-faint"
          />
          <p className="pt-2 text-right text-[12px] text-ink-faint">{text.length} characters</p>
        </Card>

        <div className="flex flex-col gap-[10px]">
          <span className="pl-1 text-[12px] font-semibold tracking-wide text-ink-faint">
            NEED A NUDGE?
          </span>
          {examples.map((example) => (
            <button
              key={example}
              type="button"
              onClick={() => {
                haptics.tap();
                setText(example);
              }}
              className="pressable calzy-card px-4 py-[13px] text-left text-[14px] font-medium text-ink-soft"
              style={{ borderRadius: 18 }}
            >
              {example}
            </button>
          ))}
        </div>

        {error && (
          <div className="rounded-[18px] bg-protein/10 px-4 py-3 text-[14px] font-medium text-protein">
            {error}
          </div>
        )}
      </div>
    </FullScreenSheet>
  );
}
