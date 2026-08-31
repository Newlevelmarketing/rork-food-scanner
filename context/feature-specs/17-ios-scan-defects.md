# Unit Spec: iOS Scan Defects Found in Simulator Testing

## Status

- Status: **Implemented, UNVERIFIED** — needs a rebuild on the Mac
- Owner: Claude Code (code) / Human (verification)
- Created: 2026-08-22
- Last Updated: 2026-08-22

## Goal

Fix the three defects the iPhone 17 Pro / iOS 26.5 Simulator run surfaced.

## Why This Matters

The test report was a clean baseline — build, all six onboarding screens, dashboard, calculated
targets and relaunch persistence all passed — with three real failures against it. Two of them
affect physical devices too, not just the Simulator.

## Defects

### 1. Empty key reports the wrong error — `NutritionAI.swift`

**Confirmed exactly as predicted in unit 04.** `isConfigured` was `endpoint != nil`, and `endpoint`
only checked that the URL was non-empty — which `Config.EXPO_PUBLIC_TOOLKIT_URL` defaults to the
public gateway. So a keyless build reported itself configured, POSTed a bearer token with nothing
after it, collected a 401, and surfaced *"Meal analysis is temporarily unavailable. Please try
again later."*

No amount of trying again supplies a build-time credential.

**Fix:** require a non-empty key in `isConfigured`, and bail with `.notConfigured` before the
request rather than spending one to learn there are no credentials.

> **Note on the expected string.** The report expected *"AI scanning isn't available in this build
> yet."* — that is the **web** copy. iOS has its own, better wording:
> *"Meal analysis is unavailable right now. You can still add a meal by searching the food
> database."* It names a working alternative. **That is the correct pass condition**, not the web
> string.

### 2. "Preparing camera…" forever — `ScanView.swift`

`placeholder` branched only on `.denied`, so **every other non-running state rendered as
"Preparing camera…"** — including `.unavailable`, which is terminal.

On the Simulator there is no camera, `configure()` fails, status becomes `.unavailable`, and the
user waits forever for something that will never happen. **This is not Simulator-only:** any
device whose camera cannot be configured hits the same dead end.

**Fix:** `.unavailable` gets its own icon, title, and a line naming the two things that still work
— the photo library and the food database — matching the app's established
state-the-problem-then-the-recovery copy pattern.

### 3. "Try again" leaves a frozen preview — `ScanView.swift`, `CameraService.swift`

Two independent causes, both real:

- `handle()` calls `camera.stop()`, but `stop()` never cleared `status`. The view still saw
  `.running` and kept rendering a preview layer over a **stopped session** — which is exactly what
  a frozen last frame looks like. `stop()` now drops to `.idle`.
- "Try again" cleared `errorMessage` and `stagedImage` but never restarted the session, and never
  cleared `camera.capturedImage` or `pickerItem`. Both are watched with `onChange`, so
  **re-picking the same photo or re-capturing an identical frame would not have fired at all.**

**Fix:** the retry button restarts the camera and clears both sources.

## Scope

### In Scope

- `ios-calzy/.../Services/NutritionAI.swift`
- `ios-calzy/.../Services/CameraService.swift`
- `ios-calzy/.../Views/ScanView.swift`

### Out of Scope

- **Moving iOS onto the Gemini proxy.** Still correct to defer: the proxy is not deployed, so iOS
  would point at a URL that does not exist. iOS stays on the Rork gateway until there is a live
  endpoint.
- **The Swift actor-isolation and camera-concurrency warnings.** Reported but not quoted. Fixing
  concurrency annotations blind, in a file that cannot be compiled here, is exactly the kind of
  guess that turns one warning into a build failure. **Paste the warnings and they become their
  own unit.**
- `AiService.kt` — Android has defect 1 too, but no Android build has been run yet. Same reasoning
  as iOS: get a green baseline first.

## Verification — REQUIRED, and not done here

**None of this Swift has been compiled.** There is no Xcode on this machine. What was done instead:

- Every change traced to the code path named in the report.
- Switch-expression syntax matches what `NutritionAI.swift:20` already uses, so it is not a new
  language feature for this target.
- The `placeholder` VStack stays within the ViewBuilder child limit — four top-level children.
- `camera.capturedImage = nil` and `pickerItem = nil` both flow into existing guard-else handlers,
  so the nil write is harmless.

### Re-test script

| Step | Expected |
| --- | --- |
| Empty key, pick a photo | *"Meal analysis is unavailable right now. You can still add a meal by searching the food database."* |
| Open scan on Simulator | **"Camera unavailable"**, with the library/database line — not "Preparing camera…" |
| Scan fails, then "Try again" | Placeholder returns cleanly, no frozen frame |
| "Try again", then pick the **same** photo again | Analysis runs again |
| Real key, pick a photo | A real estimate |

## Decision Log Update Required

None. All three restore intended behaviour.
