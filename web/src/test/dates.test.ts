import {
  addDays,
  addMonths,
  daysInMonth,
  isSameDay,
  startOfDay,
  stampOnDay,
  startOfMonth,
  weekdayInitials,
} from "@/lib/dates";

/**
 * Locale-dependent formatters (`shortWeekday`, `monthDay`, `shortTime`, ...) are
 * deliberately not covered — their output varies by machine locale, so asserting
 * on it would make the suite environment-dependent rather than correct.
 */

describe("startOfDay", () => {
  it("zeroes the time component", () => {
    const result = startOfDay(new Date(2026, 5, 15, 14, 37, 22, 500));
    expect(result.getHours()).toBe(0);
    expect(result.getMinutes()).toBe(0);
    expect(result.getSeconds()).toBe(0);
    expect(result.getMilliseconds()).toBe(0);
    expect(result.getDate()).toBe(15);
  });

  it("does not mutate its argument", () => {
    const original = new Date(2026, 5, 15, 14, 37);
    startOfDay(original);
    expect(original.getHours()).toBe(14);
  });
});

describe("isSameDay", () => {
  it("matches two times on the same calendar day", () => {
    expect(isSameDay(new Date(2026, 5, 15, 1), new Date(2026, 5, 15, 23))).toBe(true);
  });

  it("separates times across midnight", () => {
    expect(isSameDay(new Date(2026, 5, 15, 23, 59), new Date(2026, 5, 16, 0, 1))).toBe(false);
  });

  it("accepts ISO strings on either side", () => {
    const iso = new Date(2026, 5, 15, 9).toISOString();
    expect(isSameDay(iso, new Date(2026, 5, 15, 18))).toBe(true);
    expect(isSameDay(iso, iso)).toBe(true);
  });

  it("does not match the same day in a different year", () => {
    expect(isSameDay(new Date(2025, 5, 15), new Date(2026, 5, 15))).toBe(false);
  });
});

describe("addDays", () => {
  it("crosses a month boundary", () => {
    const result = addDays(new Date(2026, 0, 31), 1);
    expect(result.getMonth()).toBe(1);
    expect(result.getDate()).toBe(1);
  });

  it("goes backwards with a negative offset", () => {
    const result = addDays(new Date(2026, 0, 1), -1);
    expect(result.getFullYear()).toBe(2025);
    expect(result.getMonth()).toBe(11);
    expect(result.getDate()).toBe(31);
  });

  it("does not mutate its argument", () => {
    const original = new Date(2026, 0, 31);
    addDays(original, 5);
    expect(original.getDate()).toBe(31);
  });
});

describe("addMonths", () => {
  it("lands on the following month rather than overflowing", () => {
    // The implementation sets the day to 1 before shifting the month, which is
    // what stops 31 January + 1 month from spilling into March.
    const result = addMonths(new Date(2026, 0, 31), 1);
    expect(result.getMonth()).toBe(1);
    expect(result.getFullYear()).toBe(2026);
  });

  it("crosses a year boundary in both directions", () => {
    expect(addMonths(new Date(2026, 11, 15), 1).getFullYear()).toBe(2027);
    expect(addMonths(new Date(2026, 0, 15), -1).getFullYear()).toBe(2025);
  });
});

describe("daysInMonth", () => {
  it("handles February in a leap year", () => {
    expect(daysInMonth(new Date(2024, 1, 1))).toBe(29);
  });

  it("handles February in a non-leap year", () => {
    expect(daysInMonth(new Date(2026, 1, 1))).toBe(28);
  });

  it("handles 30- and 31-day months", () => {
    expect(daysInMonth(new Date(2026, 3, 1))).toBe(30);
    expect(daysInMonth(new Date(2026, 0, 1))).toBe(31);
  });
});

describe("startOfMonth", () => {
  it("returns the first day at midnight", () => {
    const result = startOfMonth(new Date(2026, 5, 15, 14, 30));
    expect(result.getDate()).toBe(1);
    expect(result.getMonth()).toBe(5);
    expect(result.getHours()).toBe(0);
  });
});

describe("weekdayInitials", () => {
  it("provides seven entries, one per weekday", () => {
    expect(weekdayInitials).toHaveLength(7);
    expect(weekdayInitials.every((initial) => initial.length > 0)).toBe(true);
  });
});


describe("stampOnDay", () => {
  it("returns the clock time unchanged when the day is today", () => {
    const now = new Date(2026, 5, 15, 14, 37, 22, 500);
    expect(stampOnDay(now, now)).toBe(now);
  });

  it("moves the date to the target day but keeps the clock time", () => {
    const now = new Date(2026, 5, 15, 14, 37, 22, 500);
    const past = new Date(2026, 5, 10, 0, 0, 0, 0);
    const result = stampOnDay(past, now);
    expect(result.getFullYear()).toBe(2026);
    expect(result.getMonth()).toBe(5);
    expect(result.getDate()).toBe(10);
    expect(result.getHours()).toBe(14);
    expect(result.getMinutes()).toBe(37);
  });

  it("carries seconds and milliseconds, so same-minute entries still order", () => {
    // SearchSheet's old copy zeroed these, so two foods logged into a past day
    // within one minute got identical timestamps and an arbitrary order.
    const now = new Date(2026, 5, 15, 14, 37, 22, 500);
    const past = new Date(2026, 5, 10);
    const result = stampOnDay(past, now);
    expect(result.getSeconds()).toBe(22);
    expect(result.getMilliseconds()).toBe(500);
  });

  it("orders two entries written into the same past minute", () => {
    const past = new Date(2026, 5, 10);
    const first = stampOnDay(past, new Date(2026, 5, 15, 14, 37, 10, 0));
    const second = stampOnDay(past, new Date(2026, 5, 15, 14, 37, 40, 0));
    expect(second.getTime()).toBeGreaterThan(first.getTime());
  });

  it("does not mutate the day it is given", () => {
    const past = new Date(2026, 5, 10, 0, 0, 0, 0);
    stampOnDay(past, new Date(2026, 5, 15, 14, 37));
    expect(past.getHours()).toBe(0);
    expect(past.getDate()).toBe(10);
  });

  it("handles a future day the same way", () => {
    const now = new Date(2026, 5, 15, 9, 5);
    const future = new Date(2026, 5, 20);
    const result = stampOnDay(future, now);
    expect(result.getDate()).toBe(20);
    expect(result.getHours()).toBe(9);
  });
});
