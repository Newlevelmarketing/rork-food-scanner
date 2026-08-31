# Unit Spec: Test the iOS Nutrition Maths

## Status

- Status: **Implemented, UNVERIFIED** — needs `⌘U` on the Mac
- Owner: Claude Code (code) / Human (verification)
- Created: 2026-08-22
- Last Updated: 2026-08-22

## Goal

Cover the iOS nutrition maths with tests that mirror the web suite, so the two hand-written
implementations can be shown to agree rather than assumed to.

## Why This Matters

The Simulator report closed with a fair criticism: *"No automated regression tests were added."*
That gap is wider than unit 17 — **web has 158 tests, iOS and Android have none.**

It matters most here because `context/architecture.md` records that the three platforms are
**deliberate hand-written mirrors**, and lists drift between them as a known risk. Untested mirrors
are how that risk becomes a defect, and the nutrition maths is where a defect would hurt most:
`lib/nutrition.ts` has 46 tests covering Mifflin–St Jeor, the 1200 kcal floor, macro splits and
rounding drift. Its Swift twin had zero. Same equations, no proof they agree.

If they had drifted, a user would see different calorie targets on iPhone than in the browser, and
nothing in the project would have caught it.

## Scope

### In Scope

- `ios-calzy/ModernBodyFoodScannerTests/NutritionMathTests.swift`

### Out of Scope — not touched

- **Any file under `ModernBodyFoodScanner/`.** This unit adds tests. Where a test surfaced a
  divergence, it is recorded below as a finding, not silently corrected.
- `NutritionAI`, `CameraService`, `ScanView` — unit 17's code. Testing them needs network stubbing
  and a camera, which is its own problem.
- Android's mirror, which has the same gap.
- The Swift concurrency warnings — still unquoted.

## Method

Expected values are **the same hand-computed numbers the web suite asserts**, not values read back
from the Swift. That distinction is the whole point: a test written from the implementation only
proves the code does what it does. These prove both platforms agree with the arithmetic.

Written against **Swift Testing** (`import Testing`, `@Test`, `#expect`), matching the existing
placeholder in the target rather than introducing XCTest alongside it.

Coverage mirrors `web/src/test/nutrition.test.ts`: age and its floor, BMR for both sexes, activity
multipliers, all three goal branches, the 1200 kcal floor, macro consistency, BMI and its category
boundaries, meal totals with portions, item scaling, and `settingCalories` including the
empty-items collapse, drift correction and clamping. Plus `MealSlot.current`, which mirrors web's
`currentSlot`.

## Drift Found

Comparing the implementations line by line surfaced **two real divergences.** Neither is fixed
here — a test-only unit should report drift, not quietly pick a winner.

### 1. Whitespace-only meal titles

`settingCalories` falls back to a generic name when a meal has no title:

- **Web** (`nutrition.ts:177`): `meal.title.trim() === "" ? "Meal" : meal.title` — **trims**.
- **iOS** (`MealEntry.swift`): `title.isEmpty ? "Meal" : title` — **does not trim**.

A title of `"   "` becomes `"Meal"` on web and stays `"   "` on iOS, producing a blank-looking row.

The web suite asserts the trimming behaviour. The iOS test deliberately asserts **only** the
genuinely-empty case, so nothing here pins the divergence as correct. **Needs a decision:** trim on
iOS to match, or drop trimming on web.

### 2. Tie-breaking when pushing rounding drift

Both platforms push leftover kcal into the largest item, but disagree on ties:

- **Web** keeps the **first** maximum (`items[i].calories > items[largest].calories`).
- **iOS** `indices.max(by:)` returns the **last** maximum.

With three equal items, web corrects the first row and iOS the last. Cosmetic — the totals match
either way — but it is a divergence, and both suites are written tie-agnostically so neither fails
spuriously.

### Not drift, though it looks like it

JavaScript's `Math.round` rounds half toward `+∞`; Swift's `.rounded()` rounds half **away from
zero**. These differ only for negative halves, and every rounded quantity here is non-negative, so
the two agree on all realistic inputs. Checked rather than assumed, because it would have been an
easy false alarm.

## Acceptance Criteria

1. The file compiles and `⌘U` runs it.
2. Every test passes — which is the actual claim being made: **the platforms agree**.
3. No file outside the test target is modified.

## Verification — REQUIRED, not done here

**No Swift was compiled.** There is no Xcode on this machine. Confidence rests on:

- Every initializer checked against the real declarations in `UserProfile.swift` and
  `MealEntry.swift`.
- Swift Testing syntax matching the existing placeholder in the same target.
- The test target being a `PBXFileSystemSynchronizedRootGroup`, so a new file needs no project
  edit.

**A failure is a result, not a mistake to smooth over.** If an expected value fails, the platforms
have drifted and that is exactly what this unit was built to detect — report the numbers rather
than adjusting the expectation.

## Decision Log Update Required

Only once the human decides what to do about the two drifts above.
