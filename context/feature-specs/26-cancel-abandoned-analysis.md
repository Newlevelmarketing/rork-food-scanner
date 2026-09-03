# Unit Spec: Cancel Analysis When Its Sheet Closes

## Status

- Status: **Complete — verified by test**
- Owner: Claude Code
- Created: 2026-09-03
- Last Updated: 2026-09-03

## Goal

Stop an abandoned analysis writing into a sheet the user has already closed.

## Why This Matters

Two audit findings — `DescribeSheet.tsx:53` and `ScanSheet.tsx:113` — with one root cause:
**`lib/ai.ts` had no cancellation of any kind.** No `AbortController`, no `signal`, nowhere.

Both sheets `await` an analysis and then write state unconditionally. Both are closable mid-flight:
the header **Cancel button is never disabled while busy**, and `Sheet.tsx:35-38` registers a
**window `keydown` handler**, so Escape reaches the sheet even when the analysing overlay covers
the on-screen close button. Only the footer button is disabled.

And the state reset lives in the **close** branch of each effect — `if (open) return;` — so it
fires on close and never on open. That combination is what made the writes stick:

- **Success path:** `onResult` fires → `Index` sets a draft → the MealResult review sheet pops open
  for a request the user abandoned.
- **Failure path:** a late `setError` lands *after* the reset, and because nothing resets on open,
  the next time the sheet opens the previous request's red banner is still there — over an empty
  form, or in ScanSheet's case over a live camera along with the stale staged photo.

## The Fix

**A run token per sheet, plus a real abort.**

1. `lib/ai.ts` — `analyzeImage` and `analyzeText` take an optional `AbortSignal`, passed to `fetch`.
2. An abort **propagates as-is** rather than being flattened to `serverError`. A cancel is the user
   walking away, not a failure, and reporting it as one would surface "Something went wrong" for
   their own action.
3. Each sheet bumps a `runRef` on close and on every new submit, and captures it. Every write after
   an `await` is gated on the token still matching.
4. Closing also aborts the in-flight controller, so the request actually stops rather than merely
   having its result ignored.

### Why both a token and a signal

`toThumbnail` and `toBudgetedDataURL` in ScanSheet are **not abortable** — they are canvas work, not
fetches. The signal only covers the network call, so each `await` needs the run check as well.
ScanSheet has four such guards; DescribeSheet has two.

The signal is what stops wasted work and quota; the token is what stops stale writes.

## Scope

### In Scope

- `web/src/lib/ai.ts`
- `web/src/features/DescribeSheet.tsx`, `web/src/features/ScanSheet.tsx`
- `web/src/test/ai.test.ts`

### Out of Scope

- **Resetting state on the open transition as well as close.** With the token in place, late writes
  cannot land, so the reset asymmetry no longer has an observable effect. Changing it is churn
  without a symptom.
- `ShareSummary.tsx:35`, which keeps its error across close and reopen. Same family, different
  mechanism — it has no async race, just a missing reset — and it is a separate low finding.
- Sheet focus management and dialog semantics.

## Checks Run

- `npm run typecheck` — **0 errors**
- `npm run lint` — **0 errors, 9 warnings**, unchanged
- `npx vitest run` — **183 tests** (up from 179), all pass
- `npm run build` — **pass**

Four new tests cover the contract: the signal reaches `fetch`; omitting it still works so existing
callers are unaffected; an abort propagates as something **other** than a `NutritionAIError`; and a
genuine network failure is still reported as `serverError`.

### Honest limit

**The end-to-end race was not reproduced.** Doing so means opening a sheet, starting an analysis
against a configured key, pressing Escape mid-flight, and asserting nothing lands — which needs the
React testing setup this project still does not have, plus a live key.

What is proven: the cancellation contract in `lib/ai.ts` is tested, and the guards are present at
every post-`await` write in both sheets (verified by reading, six sites). The wiring between them
is not covered by a test.

## Decision Log Update Required

None. Restores intended behaviour.
