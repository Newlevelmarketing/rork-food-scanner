/** Calendar helpers shared across the app. */

export function startOfDay(date: Date): Date {
  const copy = new Date(date);
  copy.setHours(0, 0, 0, 0);
  return copy;
}

export function isSameDay(a: Date | string, b: Date | string): boolean {
  const left = new Date(a);
  const right = new Date(b);
  return (
    left.getFullYear() === right.getFullYear() &&
    left.getMonth() === right.getMonth() &&
    left.getDate() === right.getDate()
  );
}

export function isToday(date: Date | string): boolean {
  return isSameDay(date, new Date());
}

/**
 * A timestamp on `day`, carrying the current clock time.
 *
 * Logging onto a past day should record *that* day with the time of writing, not
 * midnight - otherwise entries bunch at 00:00 and lose their order. Seconds and
 * milliseconds carry over so two entries written into the same minute still sort
 * correctly.
 *
 * This rule previously existed twice, in MealResult and SearchSheet, and the two
 * copies had already drifted: one preserved seconds, the other zeroed them.
 */
export function stampOnDay(day: Date, now: Date = new Date()): Date {
  if (isSameDay(day, now)) return now;
  const target = new Date(day);
  target.setHours(now.getHours(), now.getMinutes(), now.getSeconds(), now.getMilliseconds());
  return target;
}

export function addDays(date: Date, days: number): Date {
  const copy = new Date(date);
  copy.setDate(copy.getDate() + days);
  return copy;
}

export function addMonths(date: Date, months: number): Date {
  const copy = new Date(date);
  copy.setDate(1);
  copy.setMonth(copy.getMonth() + months);
  return copy;
}

export function shortWeekday(date: Date): string {
  return date.toLocaleDateString(undefined, { weekday: "short" }).toUpperCase();
}

export function shortTime(date: Date | string): string {
  return new Date(date).toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" });
}

export function monthDay(date: Date | string): string {
  return new Date(date).toLocaleDateString(undefined, { month: "short", day: "numeric" });
}

export function longMonthDay(date: Date | string): string {
  return new Date(date).toLocaleDateString(undefined, { month: "long", day: "numeric" });
}

export function monthYear(date: Date): string {
  return date.toLocaleDateString(undefined, { month: "short", year: "numeric" });
}

export function abbreviatedDate(date: Date | string): string {
  return new Date(date).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

/** Localised single-letter weekday headers, ordered from Sunday. */
export const weekdayInitials: string[] = (() => {
  const base = new Date(2024, 0, 7); // A Sunday.
  return Array.from({ length: 7 }, (_, index) =>
    addDays(base, index).toLocaleDateString(undefined, { weekday: "narrow" }),
  );
})();

export function daysInMonth(date: Date): number {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
}

export function startOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}
