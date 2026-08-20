# Unit Spec: Report an Unconfigured AI Build Honestly

## Status

- Status: **Complete** — web only; native follow-up specified below
- Owner: Claude Code
- Created: 2026-08-20
- Last Updated: 2026-08-20

## Goal

Make a build with no AI gateway key say so, instead of firing a doomed request and blaming the
user's browser.

## Why This Matters

`web/src/lib/ai.ts` derives its endpoint from `EXPO_PUBLIC_TOOLKIT_URL`, which **falls back to
the public gateway** when unset. `endpoint()` therefore never returned `null`, so:

- `isAIConfigured` was unconditionally `true`, regardless of whether a key existed.
- `send()` proceeded to POST with `Authorization: Bearer ` (empty), got a 401, and mapped it to
  `authError` — *"AI features are currently unavailable. Please reload the page."*

Reloading cannot fix a missing build-time credential. The codebase already had the right message
for this case, `notConfigured` — *"AI scanning isn't available in this build yet."* — and it was
unreachable.

This is what anyone running the repo from a fresh clone hits immediately, because `.env` is
gitignored. It made the app look broken when it was merely unconfigured.

## Scope

### In Scope

- `web/src/lib/ai.ts` — require a key before considering AI configured, and fail before the
  network call
- `web/src/test/ai.test.ts` — pin the behaviour

### Out of Scope — not touched

- The **client-side credential exposure** itself. That the key ships in the bundle at all is the
  top item in Known Debt and needs a backend to fix properly. This unit changes only what happens
  when the key is *absent*.
- The `authError` path for a genuinely rejected key — still correct, still reachable.
- `NutritionAI.swift` and `AiService.kt` — see the native follow-up.
- Any UI file. `isAIConfigured` is exported but read nowhere in the app, so no component changed.

## Implementation

Two edits to `web/src/lib/ai.ts`:

1. `isAIConfigured` now requires both an endpoint and a non-empty key.
2. `send()` bails on `!isAIConfigured` before `fetch`, rather than spending a request to discover
   the build has no credentials.

### Note Found While Implementing

`isAIConfigured` is **exported but consumed nowhere** in the codebase. The scan and describe
sheets call `analyzeImage`/`analyzeText` directly and render whatever error comes back. The flag
was dead, and wrong. It is now correct; whether the UI should use it to hide the Scan and Type
buttons entirely in an unconfigured build is a UX question, not part of this unit.

## Acceptance Criteria

1. ~~With no key, `isAIConfigured` is `false`.~~ **Verified.**
2. ~~With no key, `analyzeText` and `analyzeImage` reject with kind `notConfigured`.~~ **Verified.**
3. ~~No network request is attempted in an unconfigured build.~~ **Verified** — suite runtime for
   this file dropped from 1986 ms to 24 ms, the difference being real HTTP calls to
   `toolkit.rork.com` that no longer happen.
4. ~~The tests fail against the pre-fix source.~~ **Verified by running them against
   `HEAD:web/src/lib/ai.ts`** — 3 failures: `expected true to be false`, and
   `expected 'authError' to be 'notConfigured'` twice.
5. ~~Checks stay at baseline.~~ **Verified.**

## Checks Run

- [x] `npx vitest run` — **4 files, 56 tests, all passed** (up from 46)
- [x] Regression check against pre-fix source — **3 tests failed, as intended**
- [x] `npx tsc --noEmit -p tsconfig.app.json` — **0 errors**
- [x] `npm run lint` — **0 errors, 10 warnings**, same 10 as baseline
- [x] `npm run build` — **pass**

## Native Follow-Up — NOT done, deliberately

The identical bug exists on both native platforms, and `context/execution-standards.md` requires
a unit changing one platform to state whether the others follow:

| Platform | File | Current behaviour |
| --- | --- | --- |
| iOS | `ios-calzy/.../Services/NutritionAI.swift:89-92` | `endpoint` checks only that the base URL is non-empty |
| Android | `android/.../data/AiService.kt:74-81` | `toolkitUrl()` falls back to `https://toolkit.rork.com`; the key is read separately and never gates the call |

Both need the same one-line condition: treat an empty key as unconfigured.

**Not applied here because it cannot be verified on this machine** — there is no Mac, no Xcode,
and no Android SDK, so neither would compile or run. Shipping an unverified edit to two platforms
would violate the rule against claiming a check that did not run. This is queued as its own unit
for whenever a build environment exists.

## Decision Log Update Required

None. This restores the behaviour the existing error taxonomy already described; it decides
nothing new.
