# Unit Spec: Test the Domain Layer

## Status

- Status: **Complete**
- Owner: Claude Code
- Created: 2026-08-20
- Last Updated: 2026-08-20

## Result

**46 tests across 3 files, all passing on the first run. No defect found.**

Every hand-computed expectation — the Mifflin–St Jeor values (1780 male / 1614 female), the
three goal branches (1898 / 2778 / 2448 kcal), the 1200 floor, the macro splits, and the
rounding-drift correction landing 1 kcal on the largest item — matched the implementation
exactly. The domain layer was already correct; it simply had nothing proving it.

That is a meaningful result in itself: the arithmetic behind every number the user sees is now
pinned, and any future change that shifts it will fail loudly.

## Goal

Cover `web/src/lib/nutrition.ts` and `web/src/lib/dates.ts` with real unit tests, so the
calculations behind every number the user sees are protected against regression.

## Why This Matters

`context/execution-standards.md` records the baseline plainly: **test coverage is effectively
zero.** The only test in the repo is `src/test/example.test.ts`, which asserts `true === true`.

Meanwhile `lib/nutrition.ts` owns the Mifflin–St Jeor BMR calculation, activity multipliers,
the calorie floor, macro splits, BMI categorisation, portion scaling, and the rounding-drift
correction in `mealWithCalories`. If any of that silently breaks, the app shows wrong numbers to
someone making decisions about what they eat. Nothing currently catches it.

This layer is the ideal first test target: pure functions, no React, no network, no storage, no
mocking required.

It is also safe work under the open Rork question — tests are new files that no re-export would
have any reason to overwrite, and they change no application behaviour.

## Scope

### In Scope

- `web/src/test/nutrition.test.ts` — cover `lib/nutrition.ts`
- `web/src/test/dates.test.ts` — cover `lib/dates.ts`

### Out of Scope — do not touch

- **Any file in `web/src/lib/`.** This unit adds tests; it does not change behaviour. If a test
  reveals a bug, report it and stop — the fix is a separate unit with its own spec.
- `lib/ai.ts`, `lib/image.ts`, `lib/summaryCard.ts` — network and DOM dependent, later units
- `store/AppStore.tsx` — needs a React testing setup, later unit
- The browser test suite and its Playwright dependency
- `src/test/example.test.ts` — leave the placeholder alone; removing it is not this unit's job

### Explicitly Not Allowed

Adjusting a test to make it pass when it has found a real defect. A failing test that describes
correct behaviour is a **result**, not a problem to tune away.

## Implementation Details

### Conventions to Follow

- Vitest runs with `globals: true` (`vitest.config.ts:9`), so `describe`/`it`/`expect` need no
  import — match `src/test/example.test.ts`.
- Environment is node; there is no DOM. Keep to pure functions.
- Files must match `src/**/*.{test,spec}.{ts,tsx}` and must **not** be named `*.browser.test.*`,
  which the unit config excludes and the browser config claims.
- Import through the `@/` alias.
- No locale-dependent assertions — `lib/dates.ts` formatting helpers call `toLocaleDateString`
  and would make tests machine-dependent. Cover the arithmetic helpers, not the formatters.
- Derive age-dependent expectations from `new Date().getFullYear()` so tests do not rot at the
  next new year.

### Behaviour to Cover — `nutrition.ts`

1. `currentSlot` at each boundary hour: 04:00, 10:59, 11:00, 15:59, 16:00, 21:59, 22:00, 03:00
2. `ageOf` — normal arithmetic, and the 13-year floor for a future birth year
3. `bmrOf` — the male `+5` and female `−161` branches, against hand-computed values
4. `maintenanceOf` — activity multiplier applied to BMR
5. `targetsOf` — custom-target bypass; `lose`, `gain` and `maintain` branches; the 1200 kcal
   floor; protein at 1.8 g/kg and 2.0 g/kg for `gain`; fat at 27% of calories; carbs as the
   remainder
6. `bmiOf` — normal case, and the zero-height guard returning 0
7. `bmiCategory` — the 18.5 / 25 / 30 boundaries
8. `mealCalories`, `mealProtein`, `mealCarbs`, `mealFat` — portion multiplication and 1-decimal
   rounding
9. `scaleItem` — calories rounded to integer, macros to one decimal
10. `mealWithCalories` — the empty-items path producing one manual item; macro split preserved
    under scaling; the drift correction landing on the largest item; clamping to 0 and 20000

### Behaviour to Cover — `dates.ts`

1. `startOfDay` zeroes the time and does not mutate its argument
2. `isSameDay` for `Date` and ISO-string inputs, and across a midnight boundary
3. `addDays` across a month boundary and with a negative offset, without mutating its argument
4. `addMonths` — specifically that it sets the day to 1 first, so 31 January plus one month is
   February rather than overflowing into March
5. `daysInMonth` — February in a leap year and a non-leap year
6. `startOfMonth`
7. `weekdayInitials` has seven entries

## Acceptance Criteria

1. Both test files exist and are picked up by `npx vitest run`.
2. Every function listed above has at least one assertion covering it.
3. No file outside `web/src/test/` is modified.
4. `npx vitest run` reports 3 test files passing — or, if a test fails, the failure is reported
   as a defect found, with the failing expectation quoted, and no source file is edited.
5. Typecheck, lint and build stay at their recorded baselines.

## Checks Run

- [x] `npx vitest run` — **3 files, 46 tests, all passed**
- [x] `npx tsc --noEmit -p tsconfig.app.json` — **0 errors**
- [x] `npm run lint` — **0 errors, 10 warnings**, the same 10 as baseline, none new
- [x] `npm run build` — **pass**, bundle byte-identical at 951.81 kB, confirming tests do not ship
- [x] `git status` — only two additions, both under `web/src/test/`. No source file touched.

## Progress Tracker Update Required

Record the new coverage, the real test count, and any defect the tests surface.

## Decision Log Update Required

Only if a test reveals a behaviour that turns out to be intentional and undocumented — that
becomes a recorded decision rather than a bug.
