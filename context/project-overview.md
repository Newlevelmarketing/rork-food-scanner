# ModernBody — AI Food Scanner

> **STATUS: HALF FILLED.** The sections below marked *(from code)* were established from the
> repository on 2026-08-20 and are reliable. The sections marked
> **`[NEEDS HUMAN INPUT]`** cannot be recovered from source — code can say what an app does, not
> who it is for or what winning looks like. Answer the Phase 3 questions in
> `context/progress-tracker.md` and this file gets completed.

## One-Line Summary *(from code)*

ModernBody is a phone-first calorie and macro tracker that turns a photo or a sentence about a
meal into a logged nutrition entry using a vision model, and keeps every bit of that data on the
user's own device.

## Overview *(from code)*

The app is shipped as three parallel implementations of one product — native iOS (SwiftUI),
native Android (Jetpack Compose), and web (React) — exported from Rork. Internal codename
**Calzy**; user-facing name **ModernBody**.

A user onboards by entering their body stats and goal, which produce daily calorie and macro
targets via Mifflin–St Jeor. From then on the Home tab is a single day: rings for calories and
macros, cards for water, exercise, and weight, and a list of logged meals. Meals are added by
scanning a photo, describing the meal in text, searching a bundled 100-food database, or
re-logging a saved favourite. The Progress tab charts weight, streaks, and averages. There are
no accounts and no server — everything lives in device storage.

## Problem

**`[NEEDS HUMAN INPUT]`** — what pain this is meant to solve, and for whom, is not derivable
from the code. The code shows *a* solution (photo-based logging removes manual entry friction),
but not the insight it was built on.

## Target Users / Stakeholders

**`[NEEDS HUMAN INPUT]`**

What the code hints at, for confirmation rather than as fact: the default profile is a
1996-born, 178 cm, 82 kg male aiming to lose to 76 kg at 0.5 kg/week (`lib/types.ts:124-148`),
metric units, and 32 languages are shipped. That suggests an international consumer audience
with a weight-loss default, but a default profile is a placeholder, not a persona.

## Goals

**`[NEEDS HUMAN INPUT]`**

## Non-Goals *(inferred from code — confirm)*

Deliberately absent from the build today:

- User accounts, login, or cloud sync
- Any backend service
- Real payment processing — the paywall is a local toggle
- Social features, feeds, or sharing to a service (there is a local share-image generator)
- Barcode scanning
- Wearable or Health/Google Fit integration (`profile.healthSynced` exists but nothing writes it)

## Core User Flow *(from code)*

1. First launch gates on onboarding until `hasOnboarded` is true (`pages/Index.tsx:43`).
2. The user enters sex, birth year, height, weight, goal weight, activity level, goal direction
   and weekly rate; targets are computed and the first weight entry is seeded.
3. Home shows the selected day: calorie ring against target, macro rings, water, exercise,
   weight, and the day's meals.
4. To log a meal the user picks one of four paths — Scan (camera/photo), Type (describe it),
   Search (bundled food database), or Saved (one-tap re-log).
5. Photo and text paths call the AI gateway, which returns titled items with calories, macros,
   a 1–10 health score, and a one-line quip.
6. The user reviews the draft, corrects portions or calories if needed, and saves. A downscaled
   thumbnail is persisted with the entry.
7. Progress charts weight over time, streaks, and 7-day average calories.

## Features Already Built *(from code)*

| Feature | Where |
| --- | --- |
| Photo meal scanning | `features/ScanSheet.tsx`, `lib/ai.ts` |
| Text meal description | `features/DescribeSheet.tsx` |
| Bundled food search (100 foods) | `features/SearchSheet.tsx`, `data/foods.json` |
| Saved foods, one-tap re-log | `features/SearchSheet.tsx` (`SavedSheet`) |
| Meal detail, edit, portion and calorie correction | `features/MealDetail.tsx`, `EditMealSheet.tsx` |
| Exercise logging | `features/ExerciseSheet.tsx` |
| Water logging with undo | `store/AppStore.tsx:276-291` |
| Weight logging and history | `store/AppStore.tsx:304-332` |
| Progress photos | `store/AppStore.tsx:361-378` |
| Onboarding and target calculation | `pages/Onboarding.tsx`, `lib/nutrition.ts` |
| Progress dashboard, streaks, 7-day average | `pages/Progress.tsx`, `store/AppStore.tsx:217-245` |
| Shareable daily summary image | `features/ShareSummary.tsx`, `lib/summaryCard.ts` |
| 32-language localization incl. RTL | `lib/i18n.ts`, `data/strings.json` |
| Jester mode (roast instead of encouragement) | `lib/ai.ts:86-88` |
| Paywall UI (**stub — no billing**) | `features/Paywall.tsx` |
| Erase-all-data | `store/AppStore.tsx:380-383` |

## Scope

**`[NEEDS HUMAN INPUT]`** — what is in scope for the current version depends entirely on the
answer to "what is the first real goal for this app". See the Phase 3 questions.

## Success Criteria

**`[NEEDS HUMAN INPUT]`**

## Constraints *(partly from code)*

- Team: **`[NEEDS HUMAN INPUT]`**
- Tools: Rork as the generator/exporter; GitHub (private) as the remote.
- Platforms: iOS, Android, web — three hand-mirrored codebases.
- Technical: no backend exists today; adding one is an architectural change.
- Legal / compliance: **`[NEEDS HUMAN INPUT]`** — a calorie tracker giving BMI categories and
  weight-loss targets has real regulatory and duty-of-care surface, and the app ships legal
  views (`ios-calzy/.../Views/LegalViews.swift`, `Utilities/Legal.swift`) that were not
  reviewed here.

## Risks *(from code)*

| Risk | Why It Matters | Mitigation |
| --- | --- | --- |
| AI gateway key is client-side on all three platforms | Anyone can extract it and spend the account's credits | Needs a server proxy — see `architecture.md` Known Debt |
| A Rork re-export may overwrite hand edits | The whole workflow depends on this answer | Unresolved — first Phase 3 question |
| localStorage quota | Persistence fails silently once photos fill it; the user loses history | Not handled today |
| No real test coverage | Nothing catches a regression in the nutrition math | Domain layer is pure and easy to test |
| Native apps do not build from a clean clone | `Config.swift` / `Config.kt` are gitignored | Only `web` is currently reproducible |

## Open Questions

See the Phase 3 question list in `context/progress-tracker.md`. Nothing in this file should be
treated as settled product intent until those are answered.
