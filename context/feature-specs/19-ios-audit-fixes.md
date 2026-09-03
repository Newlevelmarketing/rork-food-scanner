# Unit Spec: iOS Audit Fixes — False Deletion Claim and Camera Start Race

## Status

- Status: **Implemented, UNVERIFIED** — needs a rebuild on the Mac
- Owner: Claude Code (code) / Human (verification)
- Created: 2026-09-03
- Last Updated: 2026-09-03

## Goal

Fix the two iOS findings from the 2026-09-03 audit that block shipping: a destructive action that
does not delete what it promises, and a race that can crash the app on a real device.

## Defect 1 — "Delete All Data" leaves every photo on disk

`AppStore.swift:299`. **Audit severity: high.** The single most consequential finding in the audit.

`eraseAll()` reset `data = AppData()` and persisted. But photos are **not in `AppData`** — they are
JPEGs written by `saveImage` into `Documents/images/`. The JSON is only an index.

So every meal photo and every **body progress photo** survived "Delete everything" as an orphan no
code path can reach, and continued into iCloud Backup.

`deleteMeal` and `deletePhoto` each remove their own file. `eraseAll` — the only caller of the
destructive Settings action — did not. The omission was inconsistent with the rest of the file.

**Three shipped strings promise otherwise:** the confirmation dialog (`SettingsView.swift:170`),
privacy policy section 5, and the FAQ. Web and Android keep photos inline in the persisted blob and
genuinely erase. **iOS was the only platform where the promise was false**, and that copy is
submitted to app stores.

**Fix:** remove and recreate `imagesDirectory` before persisting the empty `AppData`, using the
same call `init` already uses.

## Defect 2 — the shutter goes live against a session that has not started

`CameraService.swift:37`. **Audit severity: high.** This is a **follow-up to unit 17** — that unit
fixed `stop()` leaving `status` stale and missed the inverse race in `start()`.

`status = .running` was assigned **synchronously**, while `session.startRunning()` was only
**queued** on the serial queue. `ScanView` gates the shutter on that flag alone, so in the window
between the two the shutter renders enabled at full opacity over a black preview — and a tap
reaches `capturePhoto` with no active video connection, which is an uncatchable exception.

Device-only. The Simulator never reaches `.running` at all, which is exactly why unit 17's
Simulator pass could not have caught it.

**Fix, three parts:**

1. Claim `.running` from inside the queue block, after `startRunning()` returns, hopping back to
   the MainActor — the same pattern the photo delegate already uses in this file.
2. A `startGeneration` counter, bumped by `stop()` and by each new `start()`. Without it, moving
   the assignment later introduces a *new* race: a late completion could flip `.running` back on
   for a session `stop()` had just stopped.
3. Guard `capture()` on `output.connection(with: .video)?.isActive == true`, so the crash is
   impossible even if flag and session disagree.

## Scope

### In Scope

- `ios-calzy/.../Store/AppStore.swift` — `eraseAll()`
- `ios-calzy/.../Services/CameraService.swift` — `start()`, `stop()`, `capture()`

### Out of Scope

- **`AVCaptureSessionRuntimeError` / `WasInterrupted` observers.** The audit notes nothing ever
  drives `status` off `.running` when the session is interrupted — by a phone call, or another app
  taking the camera. That is real and worth fixing, but it is new lifecycle handling rather than a
  correction to existing logic, and it cannot be exercised here at all. Its own unit.
- The other four iOS findings (detached-write ordering, decode-failure reset, weekday `id: \.self`,
  `reschedule()` ordering) — all medium or low, none blocking.
- Android and web findings — separate units, this one is a single Swift build to verify.

## Acceptance Criteria

1. After "Delete everything", `Documents/images/` is empty and the directory still exists.
2. The shutter is not tappable until the preview is actually live.
3. No regression to unit 17's verified behaviour: `.unavailable` copy, clean retry, same-photo
   re-selection.

## Verification — REQUIRED, not done here

**No Swift was compiled.** Same arrangement as units 17 and 18.

### Re-test script

| Step | Expected |
| --- | --- |
| Log a meal with a photo, add a progress photo, then Settings → Delete everything | App returns to first-run state |
| Then, in Xcode: Window → Devices → the app container → `Documents/images/` | **Empty.** Previously every JPEG remained |
| Open Scan on a **physical device**, tap the shutter the instant the sheet appears | Nothing happens until the preview is live — no crash |
| Open Scan, wait for the preview, capture | Works as before |
| Fail a scan, "Try again" | Still returns to a clean state (unit 17 regression check) |

A note on defect 2: the window is sub-second, so "I tapped fast and it did not crash" is weak
evidence. The stronger check is that the shutter now visibly stays dimmed until the preview
appears.

## Decision Log Update Required

None. Both restore behaviour the code already claimed.
