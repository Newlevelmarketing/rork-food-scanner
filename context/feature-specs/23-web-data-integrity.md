# Unit Spec: Web Data Integrity — Entries Landing on the Wrong Day, and Writes Being Lost

## Status

- Status: **Complete — verified**
- Owner: Claude Code
- Created: 2026-09-03
- Last Updated: 2026-09-03

## Goal

Fix the audit's `web-state` cluster: four ways an entry ends up on the wrong day, attached to the
wrong value, or silently erased.

These are grouped because they are one concern — **a write landing where the user meant it to** —
not four unrelated bugs.

## The Four Findings

### 1. A second tab erases what the first logged — `AppStore.tsx:139`

`load()` reads storage once per mount, and the whole document is written under a single key with no
`storage` listener anywhere. Two tabs on the same origin meant the second tab's **next mutation of
any kind** — a water tap, a slot change — rewrote everything from its mount-time snapshot and
permanently dropped whatever the first tab had written in between. No revision counter, no merge,
no way for a stale writer to notice.

**Fix:** a `storage` listener that adopts another tab's write. Last writer still wins — this is
convergence, not a merge — but tabs now converge instead of silently diverging.

A `lastPersisted` ref stops a tab re-adopting its own echo. Without it the two tabs would ping-pong
writes forever, since adopting sets state, which triggers the persistence effect, which fires the
other tab's listener.

### 2. An app left open past midnight files onto yesterday — `AppStore.tsx:135`

`selectedDate` was fixed at mount and nothing watched for the day rolling over. After midnight on
an open tab, meals, water and exercise were all stamped onto yesterday with today's clock time —
landing on the wrong ring, the wrong macro totals, the wrong 7-day average and the wrong streak.

**Fix:** re-base on `visibilitychange`, `focus`, and a 60-second interval. **Only if the user was
sitting on "today"** — a deliberately selected past day is left alone, which is why the check
compares against a `todayRef` rather than just assigning today unconditionally.

### 3. Correcting a weight never updates the profile — `AppStore.tsx:318`

The Weight Journal passes **midnight**; onboarding and the quick-log button store a real clock
time. When replacing a same-day entry the code kept the original timestamp but then computed
`latest` over the array **including that entry**, so midnight was compared against that entry's own
later time, lost, and left `profile.currentWeightKg` stale. The journal and trend chart showed the
new value while the Weight card, delta, BMI, Home tile and every target derived from
`currentWeightKg` kept the old one. `deleteWeight` never recomputes, so it did not self-heal.

**Fix:** compare against every **other** entry, and use the written entry's effective timestamp —
which after a replace is the existing entry's, not the passed `date`.

### 4. Exercise ignores the day being viewed — `ExerciseSheet.tsx:42`

`date: new Date().toISOString()`, with no reference to `selectedDate`. Logging a workout while
viewing a past day wrote it to today: invisible in the list on screen, that day's burn stayed 0,
and today's budget was silently inflated, since `budget = targets.calories + burned`.

**Fix:** stamp the selected day, as every other write path does.

## A Fifth Problem Found While Fixing the Fourth

The day-stamping rule already existed **twice** — `MealResult.mergedDate` and
`SearchSheet.loggedDate` — and **the two copies had drifted**:

- `MealResult` → `setHours(h, m, s, ms)`, with a comment explaining that seconds carry over so two
  meals logged into the same minute still order correctly.
- `SearchSheet` → `setHours(h, m, 0, 0)`, zeroing them. Two foods searched into a past day within
  one minute got **identical timestamps**, so their order fell to insertion index.

Adding a third copy for `ExerciseSheet` would have made it worse. Instead the rule is now one
tested helper, `stampOnDay` in `lib/dates.ts`, used by all three. `SearchSheet` picks up the
correct second-carrying behaviour as a side effect.

## Scope

### In Scope

- `web/src/store/AppStore.tsx`, `web/src/lib/dates.ts`
- `web/src/features/ExerciseSheet.tsx`, `MealResult.tsx`, `SearchSheet.tsx`
- `web/src/test/dates.test.ts`

### Out of Scope

- **`DateStrip`'s cells**, memoised with an empty dependency array at mount. After a midnight
  re-base the strip can lack today until remount. That is a display bug in `HomeParts.tsx`, not a
  data one, and it belongs with the other UI findings.
- A true merge or CRDT for multi-tab. Convergence is the proportionate fix for a single-user
  device-local app.
- The native mirrors, which have their own versions of some of these.

## Checks Run

- `npm run typecheck` — **0 errors**
- `npm run lint` — **0 errors, 9 warnings**, unchanged
- `npx vitest run` — **179 tests** (up from 173), all pass
- `npm run build` — **pass**

### Manual verification in the running app

Seeded a known state, then dispatched a real `StorageEvent` carrying a meal "written by another
tab": the app **adopted it and rendered it**, title and 321 kcal both on screen. No console errors,
and no write loop.

### Honest limits

- The **midnight re-base** was not observed firing. Doing so needs either a real day boundary or
  fake timers around a provider-level test, which needs the React testing setup this project does
  not have. The logic is straightforward and typechecked, but it is unproven.
- The **multi-tab fix was verified with a synthetic event**, not two real tabs. The listener and its
  echo guard are exercised; genuine cross-tab timing is not.
- `logWeight` is verified by reading, not by test — it lives inside the React context, which is the
  same untested-store gap recorded earlier.

## Decision Log Update Required

Yes — "last writer wins" for multi-tab is a deliberate ceiling, not an oversight.
