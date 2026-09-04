import { CameraOff, Images, Sparkles, UtensilsCrossed } from "lucide-react";
import type { JSX } from "react";
import { useCallback, useEffect, useRef, useState } from "react";

import { CloseButton, FullScreenSheet } from "@/components/calzy/Sheet";
import { analyzeImage, messageForError } from "@/lib/ai";
import { haptics } from "@/lib/haptics";
import { useLanguage } from "@/lib/i18n";
import { captureVideoFrame, toBudgetedDataURL, toThumbnail } from "@/lib/image";
import { useAppStore } from "@/store/AppStore";

import type { MealDraft } from "./MealResult";

type CameraStatus = "idle" | "starting" | "running" | "denied" | "unavailable";

/** Full-screen meal scanner: live camera, library import and AI analysis. */
export function ScanSheet({
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
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  const [status, setStatus] = useState<CameraStatus>("idle");
  /**
   * Bumped by "Try again" to re-run the camera effect.
   *
   * Retry used to call getUserMedia itself, which duplicated the acquisition
   * without the effect's `cancelled` guard and mapped every failure to "denied".
   * Going through the effect means one code path owns the stream's lifetime.
   */
  const [retryNonce, setRetryNonce] = useState<number>(0);
  const [staged, setStaged] = useState<string | null>(null);
  const [analyzing, setAnalyzing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Invalidates an in-flight analysis. Same problem as DescribeSheet: Escape
   * still reaches the sheet while the analysing overlay covers the close button,
   * so a request could outlive the sheet and write a stale error and staged photo
   * into it - both of which then greeted the user on the next open, over a live
   * camera.
   */
  const runRef = useRef<number>(0);
  const abortRef = useRef<AbortController | null>(null);

  const stopCamera = useCallback(() => {
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
  }, []);

  useEffect(() => {
    if (!open) {
      runRef.current += 1;
      abortRef.current?.abort();
      abortRef.current = null;
      stopCamera();
      setStaged(null);
      setAnalyzing(false);
      setError(null);
      setStatus("idle");
      return;
    }

    let cancelled = false;
    setStatus("starting");

    const start = async (): Promise<void> => {
      if (!navigator.mediaDevices?.getUserMedia) {
        if (!cancelled) setStatus("unavailable");
        return;
      }
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: { ideal: "environment" } },
          audio: false,
        });
        if (cancelled) {
          stream.getTracks().forEach((track) => track.stop());
          return;
        }
        streamRef.current = stream;
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          await videoRef.current.play().catch(() => undefined);
        }
        setStatus("running");
      } catch (cameraError) {
        if (cancelled) return;
        const name = (cameraError as DOMException)?.name;
        setStatus(name === "NotAllowedError" || name === "SecurityError" ? "denied" : "unavailable");
      }
    };

    void start();
    return () => {
      cancelled = true;
      stopCamera();
    };
  }, [open, retryNonce, stopCamera]);

  const handle = useCallback(
    async (source: string | File): Promise<void> => {
      runRef.current += 1;
      const run = runRef.current;
      const isCurrent = (): boolean => runRef.current === run;
      const controller = new AbortController();
      abortRef.current = controller;

      setError(null);
      setAnalyzing(true);
      stopCamera();

      try {
        // toThumbnail and toBudgetedDataURL are not abortable, so each await
        // needs the run check too - not just the fetch.
        const preview = await toThumbnail(source, 520);
        if (!isCurrent()) return;
        if (preview) setStaged(preview);

        const payload = await toBudgetedDataURL(source);
        if (!isCurrent()) return;
        if (!payload) {
          setAnalyzing(false);
          setError("That photo is too large. Try taking a new one.");
          return;
        }

        const result = await analyzeImage(
          payload,
          store.profile.jesterMode,
          language.englishName,
          controller.signal,
        );
        if (!isCurrent()) return;
        haptics.success();
        setAnalyzing(false);
        onResult({ result, photo: preview ?? undefined, source: "photo" });
      } catch (analysisError) {
        if (!isCurrent()) return;
        haptics.warning();
        setAnalyzing(false);
        setError(messageForError(analysisError));
      }
    },
    // language.englishName is passed to the model to choose the reply language.
    // Omitting it here froze the value captured when the sheet first mounted, so
    // switching language in Settings and then scanning returned item names in the
    // previous language.
    [onResult, stopCamera, store.profile.jesterMode, language.englishName],
  );

  const shoot = (): void => {
    const video = videoRef.current;
    if (!video) return;
    const frame = captureVideoFrame(video);
    if (!frame) return;
    haptics.rigid();
    void handle(frame);
  };

  return (
    <FullScreenSheet open={open} onClose={onClose} dark bare label="Meal scanner">
      <div className="relative h-full w-full bg-black">
        <video
          ref={videoRef}
          playsInline
          muted
          className="absolute inset-0 h-full w-full object-cover"
          style={{ opacity: status === "running" && !staged ? 1 : 0 }}
        />

        {staged && (
          <>
            <img src={staged} alt="" className="absolute inset-0 h-full w-full object-cover" />
            <div className="absolute inset-0 bg-black/35" />
          </>
        )}

        {status !== "running" && !staged && (
          <div className="absolute inset-0 flex flex-col items-center justify-center gap-4 px-11 text-center">
            <CameraOff size={42} className="text-white/65" strokeWidth={1.4} />
            <p className="text-[17px] font-semibold text-white">
              {status === "denied"
                ? "Camera access is off"
                : status === "unavailable"
                  ? "Camera unavailable"
                  : "Preparing camera…"}
            </p>
            {/* "unavailable" is terminal, not transient: there is no camera, or the
                page is not on a secure origin. Telling those users to allow access
                is advice that can never work, because no prompt will ever appear. */}
            {status === "denied" && (
              <p className="text-[14px] text-white/70">
                Allow camera access in your browser, or pick a photo from your library instead.
              </p>
            )}
            {status === "unavailable" && (
              <p className="text-[14px] text-white/70">
                You can still pick a photo from your library, or add a meal by searching the food
                database.
              </p>
            )}
          </div>
        )}

        {/* Framing guide */}
        <div className="pointer-events-none absolute inset-0 grid place-items-center">
          <div className="relative aspect-square w-[min(calc(100%-56px),320px)] overflow-hidden rounded-[32px] border-[2.5px] border-white/85">
            {analyzing && (
              <div
                className="animate-scan-sweep absolute inset-x-0 h-[60px]"
                style={{
                  background:
                    "linear-gradient(to bottom, transparent, hsl(var(--flame) / 0.85), transparent)",
                }}
              />
            )}
          </div>
        </div>

        {/* Controls */}
        <div className="camera-top absolute inset-x-0 top-0 flex items-center gap-3 px-5">
          <CloseButton onClick={onClose} />
          <span className="mx-auto rounded-full bg-black/35 px-[14px] py-2 text-[14px] font-semibold text-white backdrop-blur">
            Point at your meal
          </span>
          <span className="h-10 w-10" />
        </div>

        <div className="camera-bottom absolute inset-x-0 bottom-0 flex items-center justify-center gap-[34px]">
          <button
            type="button"
            onClick={() => fileRef.current?.click()}
            aria-label="Choose a meal photo from your library to scan"
            className="pressable grid h-[52px] w-[52px] place-items-center rounded-full bg-white/[0.16] text-white backdrop-blur"
          >
            <Images size={20} strokeWidth={2.3} />
          </button>
          <input
            ref={fileRef}
            type="file"
            accept="image/*"
            className="hidden"
            onChange={(event) => {
              const file = event.target.files?.[0];
              event.target.value = "";
              if (file) void handle(file);
            }}
          />

          <button
            type="button"
            onClick={shoot}
            disabled={status !== "running" || analyzing}
            aria-label="Scan meal — take a photo and analyze its calories and macros"
            className="pressable relative grid h-[78px] w-[78px] place-items-center rounded-full border-4 border-white disabled:opacity-40"
          >
            <span className="grid h-16 w-16 place-items-center rounded-full bg-white">
              <Sparkles size={22} className="text-black" strokeWidth={2.6} />
            </span>
          </button>

          <span className="h-[52px] w-[52px]" />
        </div>

        {analyzing && (
          <div className="absolute inset-0 grid place-items-center bg-black/55 backdrop-blur-[2px]">
            <div className="flex flex-col items-center gap-4">
              <div className="relative grid h-[68px] w-[68px] place-items-center">
                <span className="absolute inset-0 rounded-full border-4 border-white/20" />
                <span
                  className="absolute inset-0 animate-spin rounded-full border-4 border-transparent"
                  style={{ borderTopColor: "hsl(var(--flame))" }}
                />
                <UtensilsCrossed size={22} className="text-white" strokeWidth={2.2} />
              </div>
              <p className="text-[17px] font-semibold text-white">Reading your plate…</p>
              <p className="text-[13px] text-white/70">Identifying ingredients and portion sizes</p>
            </div>
          </div>
        )}

        {error && (
          <div className="absolute inset-0 grid place-items-center bg-black/60 px-8 backdrop-blur-sm">
            <div className="calzy-card w-full max-w-[320px] rounded-[24px] p-5 text-center">
              <p className="text-[17px] font-bold text-ink">Scan failed</p>
              <p className="mt-2 text-[14px] text-ink-soft">{error}</p>
              <div className="mt-5 flex gap-2">
                <button
                  type="button"
                  onClick={onClose}
                  className="pressable flex-1 rounded-full bg-black/[0.06] py-3 text-[15px] font-semibold text-ink"
                >
                  Close
                </button>
                <button
                  type="button"
                  onClick={() => {
                    // Re-run the camera effect rather than acquiring a second stream
                    // here. The old inline version short-circuited to `undefined` when
                    // navigator.mediaDevices was missing - so neither .then nor .catch
                    // ran and the sheet pinned on "Preparing camera…" - assigned
                    // streamRef with no cancellation guard, and mapped every failure to
                    // "denied" including the ones that are really "unavailable".
                    setError(null);
                    setStaged(null);
                    setRetryNonce((n) => n + 1);
                  }}
                  className="pressable flex-1 rounded-full bg-ink py-3 text-[15px] font-semibold text-white"
                >
                  Try again
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </FullScreenSheet>
  );
}
