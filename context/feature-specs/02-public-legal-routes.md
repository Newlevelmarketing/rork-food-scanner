# Unit Spec: Public Legal Routes on the Web Build

## Status

- Status: **Complete** — pending human sign-off on two legal-copy deviations
- Owner: Claude Code
- Created: 2026-08-20
- Last Updated: 2026-08-20

## Goal

Give ModernBody a reachable, self-owned Privacy Policy and Terms of Use at stable public URLs,
and stop the web build from linking users to the toolchain vendor's legal pages.

## Why This Matters

Two problems solved by one change:

1. **Correctness.** `web/src/pages/Settings.tsx:192-194` linked to `https://rork.app/terms` and
   `https://rork.app/privacy`. Those are **Rork's** documents — the vendor of the tool the app
   was built with. They do not describe ModernBody's data handling, and a user tapping "Privacy
   Policy" was being shown someone else's policy.
2. **Submission.** App Store Connect and the Play Console both require a reachable Privacy Policy
   URL as app metadata. `Legal.swift:18,21` ships with `privacyPolicyURL` and `termsOfUseURL`
   empty. The policy text already existed and was good; it had nowhere public to live.

This was also the only high-value work available: it changes no shared application logic and no
native code, so it proceeds cleanly with the Rork-overwrite question (Q1) still unresolved.

## Scope

### In Scope

- Port the privacy policy and terms from `Legal.swift` into a web module
- Render them at `/privacy` and `/terms`
- Repoint the web Settings rows at those routes

### Out of Scope — not touched

- `ios-calzy/` and `android/` — no native file was modified
- The paywall, `isPro`, and anything monetisation-related — see the open decision in
  `context/decision-log.md`
- `Legal.swift`'s empty URL constants — they cannot be filled until the web build is deployed
  and the real domain is known
- The `support@calzy.app` mailto in Settings — the domain is unverified; flagged, not changed
- The client-side AI key, the localStorage quota handling, and every other Known Debt item

## Implementation

### Files Created

- `web/src/lib/legal.ts` — typed document model (`LegalBlock`, `LegalDocument`) plus the
  privacy policy, terms, and the two shared disclaimers.
- `web/src/pages/Legal.tsx` — `LegalPage`, a standalone scrollable document page.

### Files Modified

- `web/src/App.tsx` — added `/privacy` and `/terms` above the catch-all route.
- `web/src/pages/Settings.tsx` — the two legal `LinkRow` hrefs now point at the internal routes.

### Notes on the Implementation

- `LinkRow` already renders `target="_blank"`, so the documents open in a new tab and the app
  shell is left intact. No component change was needed.
- The global stylesheet pins `body` to the visual viewport for the app shell, so `LegalPage`
  owns its own `overflow-y-auto` container rather than relying on document scroll.
- Only existing design tokens are used — no new colors, no hardcoded values.
- The prop is `doc`, not `document`, to avoid shadowing the global.
- Conventions followed: named export, explicit `JSX.Element` return type, `import type` for
  types, `@/` aliases, comments explaining why rather than what.

### Legal-Copy Deviations — need human sign-off

Both are accuracy fixes, not preferences, but they are changes to legal text and a human should
approve them:

1. "your iPhone" → "your device" throughout, because these pages now serve web and Android too.
2. One sentence added to privacy section 1: on web the data lives in browser local storage and is
   erased by clearing site data. The Swift text describes an app sandbox, which is untrue of the
   web build.

## Acceptance Criteria

1. ~~`/privacy` renders the full policy.~~ **Verified in the running app.**
2. ~~`/terms` renders the full terms.~~ **Verified in the running app.**
3. ~~Settings links point at ModernBody's own documents, not Rork's.~~ **Done.**
4. ~~No native file modified.~~ **Verified via `git status`.**
5. ~~Checks pass at or above the recorded baseline.~~ **See below.**
6. Human signs off on the two legal-copy deviations. **Open.**

## Checks Run

| Check | Baseline | After this unit |
| --- | --- | --- |
| Typecheck | Pass, 0 errors | **Pass, 0 errors** |
| Lint | Pass, 0 errors / 10 warnings | See progress tracker |
| Unit tests | Pass, 1 test | See progress tracker |
| Build | Pass | See progress tracker |
| Browser tests | **Fail** (Playwright not installed) | Unchanged — pre-existing |
| Manual | — | `/privacy` and `/terms` rendered in the running dev server, zero console errors |

## Follow-On Work Identified

- Once the web build is deployed, paste the live URLs into `Legal.swift:18,21` so the iOS app
  offers web mirrors, and into App Store Connect as app metadata.
- Verify the `support@calzy.app` domain is owned and the inbox is monitored, or replace it.
- Resolve the paywall/Terms contradiction — see `context/decision-log.md`.
