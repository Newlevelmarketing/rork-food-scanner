# Unit Spec: Android Audit Fixes — Third-Party Legal Links and Debug-Signed Releases

## Status

- Status: **Implemented, UNVERIFIED** — needs a Gradle sync and build on a machine with the SDK
- Owner: Claude Code (code) / Human (verification)
- Created: 2026-09-03
- Last Updated: 2026-09-03

## Goal

Fix the two Android findings from the 2026-09-03 audit that block a Play release.

## Defect 1 — Settings links to another company's legal documents

`SettingsScreen.kt:357`. **Audit severity: high.** A **follow-up to unit 02**, which fixed exactly
this on web; unit 16 never carried it to Android.

The Support section hardcoded `https://rork.app/terms`, `https://rork.app/privacy` and
`mailto:support@calzy.app` — the scaffolding vendor's documents, which describe a different
product, and a domain this project does not own. An Android user tapping **Privacy** read someone
else's policy.

Both other platforms already apply a documented rule: an empty value **hides the row** rather than
shipping a dead or wrong link (`legal.ts:40` + `Settings.tsx:198`, `Legal.swift:24` +
`LegalViews.swift:157`). Android bypassed it with literals.

**Fix:** a `data/Legal.kt` mirroring the other two platforms, with each row gated on a configured
value. All three are empty by default, so the rows are hidden until the web build is deployed and
its real `/privacy` and `/terms` URLs are filled in.

**This is deliberately incomplete, and the trade-off is worth stating.** Hiding the privacy row is
worse UX than showing one — but showing a link to *another company's* privacy policy is worse than
showing none. The fuller fix is to render the documents natively as iOS does, which means porting
~200 lines of legal text to Kotlin; that is a separate unit, and it should not be done blind.

## Defect 2 — Release builds are signed with the public debug keystore

`build.gradle.kts:23`. **Audit severity: medium**, but it is a hard release blocker.

`release { signingConfig = signingConfigs.getByName("debug") }` signs with
`~/.android/debug.keystore`, whose password, alias and key password are published constants
(`android` / `androiddebugkey` / `android`). Play rejects a debug-signed upload outright, and a
same-identity APK could update an installed ModernBody in place and inherit its data directory.

**Fix:** a real `release` signing config reading from a gitignored `android/keystore.properties`.
When that file is absent the release build is left **unsigned** — deliberately, so the problem
surfaces at build or upload time instead of silently shipping a debug identity.

`keystore.properties`, `*.jks` and `*.keystore` are now gitignored, with a committed
`keystore.properties.example`.

## Scope

### In Scope

- `android/.../data/Legal.kt` (new)
- `android/.../ui/screens/SettingsScreen.kt` — the Support section
- `android/app/build.gradle.kts` — signing
- `android/.gitignore`, `android/keystore.properties.example`

### Out of Scope

- **`isMinifyEnabled = true`.** The audit suggested enabling it. Left `false`: it is untested here,
  and `kotlinx.serialization` needs keep rules — turning it on blind risks a release-only runtime
  failure that would not appear in debug. Its own change, with a release build to verify against.
- Rendering legal documents natively, as above.
- The other seven Android findings — config-change state loss in onboarding and the scan result,
  main-thread image work, the inert reminders screen, the debounced-save flush, decode-failure
  reset, and `streak()` cost. All medium or low; none blocks a release.
- The Android empty-key AI bug, still queued behind a green Android baseline build.

## Acceptance Criteria

1. Gradle syncs and `assembleDebug` still builds.
2. With no `keystore.properties`, the Support section is **absent** from Settings, and
   `assembleRelease` produces an unsigned artifact rather than a debug-signed one.
3. With `keystore.properties` present and populated, `assembleRelease` produces a properly signed
   artifact.
4. Filling in `Legal.PRIVACY_POLICY_URL` makes the Privacy row reappear and open that URL.

## Verification — REQUIRED, not done here

**No Kotlin or Gradle was compiled.** There is no Android SDK on this machine.

Confidence rests on: the `Properties` import placement being valid at the top of a Kotlin DSL
script; `rootProject.file` resolving to `android/` since `settings.gradle.kts` lives there;
`signingConfigs.getByName("release")` only being reached inside the same `exists()` guard that
creates it; and `Legal` being a plain `object` in a package `SettingsScreen.kt` now imports.

**The most likely failure is a Gradle DSL slip, not a logic error.** If sync fails, paste the
error.

## Decision Log Update Required

Yes — hiding the legal rows until a deployment exists is a visible product trade-off, not just a
bug fix.
