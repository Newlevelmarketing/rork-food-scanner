# Unit Spec: Web Camera Terminal State and Retry Path

## Status

- Status: **Complete — verified in the running app**
- Owner: Claude Code
- Created: 2026-09-03
- Last Updated: 2026-09-03

## Goal

Fix the three audit findings clustered in the web scanner's camera handling. All three share one
root cause, so one change closes them.

## Why This Matters

**This is a follow-up to unit 17**, which fixed exactly this bug on iOS and never checked web. The
audit's `ux-a11y` lens found the web mirror still carried it, and the `web-logic` lens independently
found a stream leak in the same retry path.

### The three findings

1. **`ScanSheet.tsx:154`** (medium) — the placeholder branched only on `denied`, so `unavailable`
   fell through to *"Preparing camera…"* under a `CameraOff` icon, with a subtitle telling the user
   to *"Allow camera access in your browser"*. That advice **can never work**: `unavailable` means
   there is no camera or the page is not on a secure origin, so no prompt will ever appear. The
   user waits forever for a transient state that is actually terminal.

2. **Retry silently no-ops** — `navigator.mediaDevices?.getUserMedia(...)` short-circuits the
   **entire chain** to `undefined` when `mediaDevices` is missing, so neither `.then` nor `.catch`
   ran while `setStatus("starting")` had already fired. The sheet pinned on "Preparing camera…"
   with nothing in flight.

3. **`ScanSheet.tsx:263`** (medium) — retry assigned `streamRef.current` with **no cancellation
   guard**, bypassing the `cancelled` check the effect's `start()` uses. Close the sheet inside the
   `getUserMedia` window and the effect cleanup stops a stream that has not arrived yet; the stream
   then lands in a closed sheet with **the device camera indicator still lit**, and reopening
   overwrites the ref so it can never be stopped.

Retry also mapped *every* failure to `denied`, including ones that are really `unavailable`.

## The Fix

**Retry now bumps a `retryNonce` in the effect's dependency list instead of acquiring its own
stream.** One code path owns the camera's lifetime, which resolves findings 2 and 3 together — the
effect's existing `cancelled` guard and cleanup take over — and fixes the failure mis-mapping for
free, since the effect already distinguishes `NotAllowedError`/`SecurityError` from everything else.

The placeholder gains a third branch so `unavailable` reads *"Camera unavailable"* with guidance
naming the two things that still work: the photo library and the food database. That mirrors the
iOS copy unit 17 introduced.

## Scope

### In Scope

- `web/src/features/ScanSheet.tsx`

### Out of Scope

- The two **sibling** post-await state bugs the audit found in `DescribeSheet.tsx:53` and
  `ScanSheet.tsx:113` — analysis results landing after the sheet closes. Same family, but the fix
  is a run-token discipline across both sheets plus an `AbortController` in `lib/ai.ts`, which is
  its own unit.
- Every other audit finding.

## Acceptance Criteria

1. ~~`unavailable` shows terminal copy, not "Preparing camera…".~~ **Verified.**
2. ~~`denied` still shows its own copy and permission guidance.~~ **Verified.**
3. ~~Retry no longer acquires a stream outside the guarded effect.~~ **Verified by code; see the
   limit below.**
4. ~~Checks stay at baseline.~~ **Verified.**

## Checks Run

- `npm run typecheck` — **0 errors**
- `npm run lint` — **0 errors, 9 warnings**, unchanged
- `npx vitest run` — **7 files, 158 tests, all pass**
- `npm run build` — **pass**

### Manual verification in the running app

Seeded `localStorage` with `hasOnboarded: true` to reach Home without walking onboarding, then:

| Condition | Result |
| --- | --- |
| Browser blocks camera → `NotAllowedError` | **"Camera access is off"** + permission guidance |
| `navigator.mediaDevices` forced to `undefined` | **"Camera unavailable"** + library/database guidance |

Before this change the second case rendered "Preparing camera…" with advice that could not work.

**Incidental cross-check:** the dashboard rendered 1898 kcal / 144 g / 202 g / 57 g — exactly the
hand-computed values asserted in `nutrition.test.ts`. The maths is right end to end, not just in
isolation.

### Honest limit

**The retry button was not exercised end to end.** Reaching it needs a failed analysis, which needs
an uploaded photo and a configured key. The structural fix — retry bumping the nonce rather than
calling `getUserMedia` — is verified by reading and by typecheck, and the effect it now delegates
to is the same one verified above. The stream-leak scenario itself remains unreproduced.

## Decision Log Update Required

None. Restores intended behaviour and aligns web with iOS.
