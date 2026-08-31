# Unit Spec: Android Config Bootstrap and Cosmetic De-Rork

## Status

- Status: **Complete**
- Owner: Claude Code
- Created: 2026-08-22
- Last Updated: 2026-08-22

## Goal

Finish every part of the de-Rork that does not need a Mac, an Android SDK, or a decision from the
human — so what remains is only genuinely blocked work.

## Scope

### In Scope

- `android/Config.kt.example` and `android/setup-config.sh` — the Android half of unit 11a's iOS
  config bootstrap
- Delete `rork.json`
- `web/package.json` name: `rork-web-app` → `modernbody-web`
- `.gitignore` files: drop the `.rork/` cache entries and `rork-eslint.config.js`
- Strip `// Created by Rork on …` file headers from four Swift files

### Out of Scope — genuinely blocked

- **Unit 12**, the native clients onto the proxy. Needs a Mac build first, so a mistake is caught
  against a project known to compile.
- **Unit 14**, the Android package rename off `com.rork.calzyandroid` (35 files). Mechanical, but
  it needs a target namespace the human has not chosen, and it cannot be compiled here.
- **Unit 15**, the iOS bundle id. Blocked on whether `app.rork.kffuebxmbishdc4eli446` is already
  registered in App Store Connect — answered "not sure", and it is permanent either way.

## Verification Approach

Deleting `rork.json` was checked before doing it, not assumed: a repo-wide search found it
referenced **only in documentation**. Nothing in any build, script or source reads it.

The Swift edits are **comment-only**. `git diff --stat` confirms two deleted lines per file, all of
them `//` headers. A removed comment cannot change compilation, which is why this was acceptable
without a compiler — unlike unit 12, which changes behaviour.

`android/setup-config.sh` derives the package from `MainActivity.kt` rather than hardcoding it, so
it keeps working after unit 14's rename. Its output was tested and confirmed ignored by
`git check-ignore`.

## Acceptance Criteria

1. ~~`setup-config.sh` produces a `Config.kt` with the right package, and it is gitignored.~~
   **Verified** — created `com.rork.calzyandroid`, ignored by `android/.gitignore:38`.
2. ~~No Rork reference remains in `web/src`, `web/api`, or any `.gitignore`.~~ **Verified.**
3. ~~Checks stay at baseline.~~ **Verified.**

## Checks Run

- [x] `npm run typecheck` — **0 errors**
- [x] `npm run lint` — **0 errors, 9 warnings**
- [x] `npx vitest run` — **7 files, 158 tests, all pass**
- [x] `npm run build` — **pass**
- [x] `android/setup-config.sh` executed; output verified and gitignored
- [x] `git diff --stat ios-calzy` — comment lines only

## Rork Surface After This Unit

Audited case-insensitively across tracked files:

| Area | Remaining | Why |
| --- | --- | --- |
| `web/src`, `web/api` | **None real** | `AIErrorKind` matches "rorK" incidentally; one comment documents the gateway era on purpose |
| `ios-calzy` | 3 files | `Config.swift.example` and `NutritionAI.swift` (unit 12), `project.pbxproj` bundle id (unit 15) |
| `android` | 36 files | 35 are the package name (unit 14); 1 is the new template |

Everything left maps to a blocked unit. There is no unblocked de-Rork work remaining.

## Decision Log Update Required

None. No behaviour changed.
