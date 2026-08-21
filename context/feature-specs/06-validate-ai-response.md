# Unit Spec: Validate the AI Response Before It Reaches the Store

## Status

- Status: **Complete**
- Owner: Claude Code
- Created: 2026-08-20
- Last Updated: 2026-08-20

## Result

**74 tests passing, up from 56 — 18 new cases covering the untrusted-input path.**

The NaN mechanism was confirmed directly rather than assumed: `Math.round(undefined)` is `NaN`,
`Math.max(0, NaN)` is `NaN`, and `500 + NaN` is `NaN`. `resultToItems` still has no guard of its
own, so the schema is now the only thing standing between the model and a corrupted meal history.

**One finding worth more than the unit itself:** the first typecheck failed with TS2322. Under
`strict: false`, zod's inferred type degrades to all-properties-optional, so the compiler could
not see that `safeParse` had guaranteed the shape. Worked around with a single documented
assertion placed *after* runtime verification. Logged in `context/decision-log.md` as evidence
for scheduling a "turn on `strict`" unit.

**Bundle cost, reported rather than estimated:** 951.81 kB → **1,009.54 kB** raw,
273.20 kB → **287.00 kB gzipped — +13.80 kB gzip**. A hand-rolled validator would have cost
nothing. The decision and its reversal condition are recorded.

## Goal

Parse the model's reply instead of asserting its shape, so a malformed response fails with the
existing `badResponse` message rather than writing broken numbers into the user's meal history.

## Why This Matters

The model reply is the single most untrusted input in the app — it is generated text from a
third-party service, arriving over the network. It is currently handled with a type assertion:

```ts
parsed = JSON.parse(json) as AnalysisResult;   // lib/ai.ts:186
```

`as` is a promise to the compiler, not a check. Nothing verifies the shape at runtime. The only
guard is `!parsed.isFood || !Array.isArray(parsed.items) || parsed.items.length === 0`.

`zod` has been a declared dependency since before this brain existed and is imported nowhere in
`src/`. The tool for this job was already installed and unused.

### The concrete failure

An LLM omitting one numeric field is ordinary behaviour, not an exotic edge case. If `calories`
is missing from an item:

1. `resultToItems` computes `Math.max(0, Math.round(undefined))` → `Math.max(0, NaN)` → **NaN**
   (`lib/ai.ts:217`).
2. That NaN is saved into `MealEntry.items` and persisted to `localStorage`.
3. `mealCalories` sums it, so the meal's calories become NaN.
4. The Home tab's calorie ring and every daily total involving that meal render NaN.

`healthScore` is worse, because nothing clamps or rounds it at all — `MealResult.tsx:82` passes
it straight through to the store. A string `"8"` survives into `MealEntry.healthScore`, and
`Home.tsx:68` averages health across the day with `sum + meal.healthScore`, so a single string
turns the day's health average into concatenated garbage.

**The corruption is persistent.** It is written to `localStorage`, so it survives reload and
poisons that day's history until the user deletes the meal.

## Scope

### In Scope

- `web/src/lib/ai.ts` — a zod schema for the analysis payload, and an exported
  `parseAnalysis(text)` covering extract → parse → validate
- `web/src/test/ai.test.ts` — extend with malformed-payload cases

### Out of Scope — not touched

- The client-side credential exposure — still the top Known Debt item, still needs a backend
- The network layer, the error taxonomy, and every existing error message. This unit reuses
  `badResponse` and `notFood`; it introduces no new user-facing copy.
- `resultToItems`' existing clamping — it stays as a second line of defence
- `MealResult.tsx` and any other consumer
- The native platforms, which have the same weakness. `NutritionAI.swift` decodes into a
  `Codable` struct, so Swift already rejects wrong types — the gap there is narrower and is a
  separate assessment.

## Implementation Details

### Behaviour

1. Numbers arriving as numeric strings are coerced. Models return `"200"` for `200` routinely,
   and rejecting that would fail a response that is semantically fine.
2. A value that cannot become a finite number — `undefined`, `"lots"`, `NaN`, `Infinity` — fails
   validation and raises `badResponse`. It must never reach the store.
3. `isFood` accepts a real boolean, or the strings `"true"` / `"false"` mapped explicitly. A
   blanket `z.coerce.boolean()` is **not** acceptable: it turns the string `"false"` into `true`.
4. `healthScore` is rounded and clamped to 1–10. The system prompt already specifies that range
   and the UI renders it as a score out of ten; enforcing it at the boundary is upholding the
   documented contract, not inventing behaviour.
5. `name` and `quantity` must be non-empty after trimming.
6. `quip` stays optional.
7. Validation happens **before** the `isFood` / empty-items check, so shape errors report
   `badResponse` and semantic ones report `notFood`. The distinction already exists in the error
   taxonomy and should be preserved.
8. `parseAnalysis` is exported so the untrusted-input path is testable without touching the
   network.

### Type Safety

Keep the existing exported `AnalysisResult` and `AnalysisItem` interfaces — `MealResult.tsx`
imports `AnalysisResult`. Annotating `parseAnalysis`'s return type as `AnalysisResult` makes the
compiler verify the schema output stays assignable to the interface, so the two cannot drift
silently.

### Expected Cost

Importing zod pulls a previously tree-shaken dependency into the bundle. Report the real
before/after size rather than estimating.

## Acceptance Criteria

1. A payload with a missing numeric field raises `badResponse` and does not return a value.
2. A payload with a non-numeric string in a numeric field raises `badResponse`.
3. Numeric strings are accepted and coerced.
4. `isFood: "false"` is treated as false, not true.
5. `healthScore` outside 1–10 is clamped; a non-integer is rounded.
6. A valid payload still parses, including one wrapped in markdown fences.
7. `isFood: false` or an empty item list still raises `notFood`, not `badResponse`.
8. No value that would produce NaN can reach `resultToItems`.
9. Typecheck, lint and build stay at baseline; the bundle delta is reported honestly.

## Checks Run

- [x] `npx vitest run` — **4 files, 74 tests, all pass** (was 56)
- [x] `npx tsc --noEmit -p tsconfig.app.json` — **0 errors** (failed first with TS2322, see Result)
- [x] `npm run lint` — **0 errors, 10 warnings**, the same 10 as baseline
- [x] `npm run build` — **pass**, 1,009.54 kB / 287.00 kB gzip (+13.80 kB gzip)

## Progress Tracker Update Required

Record the coverage added and the bundle cost.

## Decision Log Update Required

Yes — adopting runtime validation at the AI boundary, and the choice to coerce numeric strings
while refusing blanket boolean coercion, is a standard worth recording for the native platforms
to follow.
