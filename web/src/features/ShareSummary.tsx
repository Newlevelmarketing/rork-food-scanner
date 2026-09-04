import { Download, Share2 } from "lucide-react";
import type { JSX } from "react";
import { useEffect, useRef, useState } from "react";

import { FullScreenSheet } from "@/components/calzy/Sheet";
import { haptics } from "@/lib/haptics";
import {
  canvasToBlob,
  drawSummaryCard,
  type DailySummary,
} from "@/lib/summaryCard";

/**
 * Preview-then-share sheet for the day's summary card.
 *
 * Falls back to a download when the browser has no Web Share target for files,
 * which is the case on most desktop browsers.
 */
export function ShareSummary({
  open,
  summary,
  onClose,
}: {
  open: boolean;
  summary: DailySummary;
  onClose: () => void;
}): JSX.Element | null {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [ready, setReady] = useState<boolean>(false);
  const [busy, setBusy] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      // The component stays mounted, so anything left here survives a close.
      // Only `ready` was reset, which meant a one-time failure greeted the user
      // on every later open, above a card that had rendered perfectly well.
      setReady(false);
      setBusy(false);
      setError(null);
      return;
    }

    let cancelled = false;
    const canvas = canvasRef.current;
    if (!canvas) return;

    drawSummaryCard(canvas, summary)
      .then(() => {
        if (!cancelled) setReady(true);
      })
      .catch(() => {
        if (!cancelled) setError("Couldn't build your summary card.");
      });

    return () => {
      cancelled = true;
    };
  }, [open, summary]);

  const share = async (): Promise<void> => {
    const canvas = canvasRef.current;
    if (!canvas || busy) return;

    haptics.tap();
    setBusy(true);
    setError(null);

    try {
      const blob = await canvasToBlob(canvas);
      const file = new File([blob], "calzy-daily-summary.png", { type: "image/png" });

      if (navigator.canShare?.({ files: [file] })) {
        await navigator.share({ files: [file], title: "Daily Summary" });
      } else {
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = file.name;
        link.click();
        URL.revokeObjectURL(url);
      }
    } catch (cause) {
      // A user dismissing the native share sheet surfaces as AbortError.
      if (!(cause instanceof DOMException && cause.name === "AbortError")) {
        setError("Sharing failed. Try again.");
      }
    } finally {
      setBusy(false);
    }
  };

  const canShareFiles =
    typeof navigator !== "undefined" && typeof navigator.canShare === "function";

  return (
    <FullScreenSheet
      open={open}
      onClose={onClose}
      title="Share your day"
      footer={
        <div className="flex flex-col gap-2">
          {error && (
            <p className="text-center text-[13px] font-medium text-flame">{error}</p>
          )}
          <button
            type="button"
            onClick={share}
            disabled={!ready || busy}
            className="pressable flex w-full items-center justify-center gap-2 rounded-full bg-ink py-4 text-[17px] font-semibold text-white disabled:opacity-45"
          >
            {canShareFiles ? <Share2 size={17} /> : <Download size={17} />}
            {busy ? "Preparing…" : canShareFiles ? "Share summary" : "Download summary"}
          </button>
        </div>
      }
    >
      <div className="flex justify-center px-6 py-6">
        <canvas
          ref={canvasRef}
          aria-label="Daily summary card"
          className="w-full max-w-[340px] rounded-[26px] shadow-[0_14px_40px_rgba(0,0,0,0.12)] transition-opacity duration-300"
          style={{ opacity: ready ? 1 : 0 }}
        />
      </div>
    </FullScreenSheet>
  );
}
