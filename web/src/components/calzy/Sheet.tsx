import { X } from "lucide-react";
import type { JSX } from "react";
import { useEffect, type ReactNode } from "react";

import { haptics } from "@/lib/haptics";
import { cn } from "@/lib/utils";

/** Full-screen modal that slides up like a SwiftUI fullScreenCover. */
export function FullScreenSheet({
  open,
  onClose,
  title,
  leading,
  trailing,
  footer,
  children,
  dark = false,
  bare = false,
}: {
  open: boolean;
  onClose: () => void;
  title?: string;
  leading?: ReactNode;
  trailing?: ReactNode;
  footer?: ReactNode;
  children: ReactNode;
  dark?: boolean;
  bare?: boolean;
}): JSX.Element | null {
  useEffect(() => {
    if (!open) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const onKey = (event: KeyboardEvent): void => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => {
      document.body.style.overflow = previous;
      window.removeEventListener("keydown", onKey);
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex justify-center bg-black/25 backdrop-blur-[2px]">
      <div
        className={cn(
          "animate-sheet-up relative flex h-full w-full max-w-[520px] flex-col overflow-hidden shadow-[0_-8px_60px_rgba(0,0,0,0.28)]",
          dark ? "bg-black" : "calzy-backdrop",
        )}
      >
        {!bare && (
          <header className="sheet-top relative z-10 flex shrink-0 items-center gap-3 border-b border-black/[0.05] bg-white/60 px-4 pb-3 backdrop-blur-xl">
            <div className="flex min-w-[72px] justify-start">
              {leading ?? (
                <button
                  type="button"
                  onClick={() => {
                    haptics.tap();
                    onClose();
                  }}
                  className="pressable text-[15px] font-medium text-ink-soft"
                >
                  Cancel
                </button>
              )}
            </div>
            <h2 className="flex-1 text-center text-[17px] font-semibold text-ink">{title}</h2>
            <div className="flex min-w-[72px] justify-end">{trailing}</div>
          </header>
        )}

        <div className="no-scrollbar relative flex-1 overflow-y-auto overscroll-contain">
          {children}
        </div>

        {footer && (
          <div className="sheet-bottom relative z-10 shrink-0 border-t border-black/[0.05] bg-white/70 px-5 pt-3 backdrop-blur-xl">
            {footer}
          </div>
        )}
      </div>
    </div>
  );
}

/** Bottom sheet with a grabber, for compact editors. */
export function BottomSheet({
  open,
  onClose,
  children,
}: {
  open: boolean;
  onClose: () => void;
  children: ReactNode;
}): JSX.Element | null {
  useEffect(() => {
    if (!open) return;
    const onKey = (event: KeyboardEvent): void => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-[60] flex items-end justify-center bg-black/35 backdrop-blur-[2px]"
      onClick={onClose}
    >
      <div
        className="animate-sheet-up calzy-backdrop sheet-bottom max-h-[92%] w-full max-w-[520px] overflow-y-auto overscroll-contain rounded-t-[28px] shadow-[0_-10px_50px_rgba(0,0,0,0.3)]"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex justify-center pb-1 pt-[10px]">
          <span className="h-[5px] w-[38px] rounded-full bg-ink-faint/35" />
        </div>
        {children}
      </div>
    </div>
  );
}

export function CloseButton({ onClick }: { onClick: () => void }): JSX.Element {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label="Close"
      className="pressable grid h-10 w-10 place-items-center rounded-full bg-black/35 text-white backdrop-blur"
    >
      <X size={17} strokeWidth={2.8} />
    </button>
  );
}
