/** Light haptic feedback where the browser supports it (Android / some PWAs). */

function buzz(pattern: number | number[]): void {
  if (typeof navigator === "undefined") return;
  if (typeof navigator.vibrate !== "function") return;
  try {
    navigator.vibrate(pattern);
  } catch {
    // Vibration can be blocked by user settings — ignore.
  }
}

export const haptics = {
  tap: (): void => buzz(8),
  selection: (): void => buzz(4),
  soft: (): void => buzz(6),
  rigid: (): void => buzz(14),
  success: (): void => buzz([10, 40, 16]),
  warning: (): void => buzz([18, 60, 18]),
};
