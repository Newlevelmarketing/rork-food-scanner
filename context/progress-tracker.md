# Progress Tracker

Update this file after every meaningful implementation or project change.

## Current Phase

- **De-Rork in progress (units 11–16). Unit 11 done; unit 12 next, gated on a Mac build.**
- Apple's Guideline 2.1 reply (unit 01) is still open, blocked on a physical iPhone.

## Current Goal

Make the project independent of Rork. The human scoped this as **"everything"** — runtime gateway,
app identity, config generation and cosmetic traces — with **Google Gemini** chosen as the direct
provider.

Planned sequence:

| Unit | Work | Status |
| --- | --- | --- |
| 11 | Gemini proxy; web client off the gateway | **Done** |
| 12 | iOS and Android clients onto the proxy | Next — needs the Mac build first |
| 13 | Replace Rork-generated `Config.swift` / `Config.kt` | Pending |
| 14 | Rename the Android package off `com.rork.calzyandroid` | Pending |
| 15 | Rename the iOS bundle id off `app.rork.…` | **Blocked** — see below |
| 16 | `rork.json` and remaining cosmetic traces | Pending |

**Unit 15 is blocked on a question that was answered "not sure":** whether
`app.rork.kffuebxmbishdc4eli446` is already registered in App Store Connect. If it is, it is
permanent and unit 15 must be cancelled — renaming would create a separate app and lose the review
history. If it is not, renaming must happen before any submission. This needs settling before the
Apple reply goes out, not after.

Note the deliberate ordering: the identity renames (14, 15) come last, because they are mechanical
and permanent, while the proxy work is reversible and unblocks the security fix.

## Completed

- Project Brain scaffolded and reconciled against the original template
  (`Desktop\ralp\sometemplate-main.zip`).
- Application repository cloned and merged to the workspace root with `.git` intact. Zero
  collisions with the brain. Brain files are untracked and ready to commit.
- `/brain-onboard` Phases 0–2 and 5:
  - `context/architecture.md` — **filled from code**, with evidence citations.
  - `context/execution-standards.md` — **filled from code**, with verified baselines.
  - `context/ui-brand-context.md` — visual sections **filled from code**; voice sections
    inferred and awaiting ratification.
  - `context/project-overview.md` — **half filled**; evidence sections done, intent sections
    marked `[NEEDS HUMAN INPUT]`.
  - `context/decision-log.md` — four pre-existing decisions reconstructed, reasoning labelled
    INFERRED.
- Baseline checks run on a clean checkout (Node 24.15.0, npm 11.12.1). Results below.

## Baseline Check Results — 2026-08-20

Run from `web/`. **Any future unit is measured against this, not against zero.**

| Check | Command | Result |
| --- | --- | --- |
| Install | `npm install` | Pass — 378 packages |
| Typecheck | `npx tsc --noEmit -p tsconfig.app.json` | **Pass** — 0 errors |
| Lint | `npm run lint` | **Pass** — 0 errors, 10 warnings (pre-existing) |
| Build | `npm run build` | **Pass** — 33 s, 942.95 kB JS / 270 kB gzip |
| Unit tests | `npx vitest run` | **Pass** — 1 file, 1 test (a placeholder) |
| Browser tests | `npx vitest run --config vitest.browser.config.ts` | **FAIL** — Playwright browsers not installed |
| Both | `npm test` | **FAIL** — because of the browser suite |

The browser-suite failure is environmental, not a code defect: it needs a one-time
`npx playwright install` (a large download, left for the human to run deliberately). Until then
`web/src/test/calendar.browser.test.tsx` never executes.

Native apps were **not** built. They cannot be, from a clean clone — see Known Debt.

## Recently Completed

**Unit 17 — iOS Scan Defects** (`context/feature-specs/17-ios-scan-defects.md`) —
**COMPLETE, verified on Simulator at `beabdeb`.**

All five re-test steps passed on iPhone 17 Pro / iOS 26.5: fresh Debug build, "Camera unavailable"
with recovery guidance, the correct iOS empty-key message, a clean retry state, and re-selecting
the same photo triggering analysis. Existing onboarding state still loads. The Swift compiled first
time, which is worth noting given none of it could be compiled where it was written.

**Carried forward, not closed:**

1. **Live-camera restart is still unproven.** The Simulator has no camera, so the
   `.running` → `stop()` → `start()` path never ran — only the *unavailable* branch of defect 3 is
   verified. Needs a physical device.
2. **Real AI estimation is still unproven.** Needs a valid Rork key; only `.notConfigured` is
   verified.
3. **Swift concurrency warnings remain**, still not quoted. To capture them:
   `xcodebuild -project ios-calzy/ModernBodyFoodScanner.xcodeproj -scheme ModernBodyFoodScanner
   -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build 2>&1 | grep -n 'warning:'`
4. **No automated regression tests.** A fair criticism, and bigger than this unit — see below.

### Test coverage is lopsided across the three platforms

**Web has 158 tests. iOS and Android have none.** The platforms are hand-mirrored and
`context/architecture.md` already records drift between them as a known risk; untested mirrors are
how that risk turns into a defect.

The highest-value Swift target is not unit 17's code but the **nutrition math** — `lib/nutrition.ts`
has 46 tests covering Mifflin–St Jeor, the calorie floor, macro splits and rounding drift, and its
Swift mirror has zero. Same equations, no proof they agree. That is its own unit and it needs a Mac
to run.

---

**Original entry, for the reasoning trail:**

An iPhone 17 Pro / iOS 26.5 Simulator test gave a **green baseline** — build, all six onboarding
screens, dashboard, calculated targets, relaunch persistence — with three failures against it, two
of which affect physical devices as well.

1. **Empty key reported the wrong error.** Confirmed exactly as predicted in unit 04 —
   `isConfigured` checked only the URL, which defaults to the public gateway, so a keyless build
   sent an empty bearer token and got a 401. Now requires a key and bails before the request.
2. **"Preparing camera…" forever.** `placeholder` branched only on `.denied`, so `.unavailable` —
   a *terminal* state — rendered as a transient one. Not Simulator-only: any device whose camera
   cannot be configured hits the same dead end. It now has its own copy naming the two things that
   still work.
3. **"Try again" left a frozen preview.** Two causes: `stop()` never cleared `status`, so the view
   rendered a preview layer over a stopped session; and retry never restarted the camera nor
   cleared `capturedImage`/`pickerItem`, both watched with `onChange` — meaning **re-picking the
   same photo would not have fired at all.**

**Correction to the test's pass condition:** the report expected *"AI scanning isn't available in
this build yet."* That is the **web** string. iOS has its own, better copy —
*"Meal analysis is unavailable right now. You can still add a meal by searching the food
database."* — which names a working alternative. That is what a pass looks like on iOS.

**No Swift was compiled.** There is no Xcode here. Changes were traced to the reported code paths,
and the switch-expression syntax matches what `NutritionAI.swift:20` already uses. **Rebuild and
run the script in the spec before trusting any of it.**

**Not done, deliberately:** the actor-isolation and camera-concurrency warnings. They were reported
but not quoted, and guessing at concurrency annotations in a file that cannot be compiled here is
how one warning becomes a build failure. **Paste them and they become their own unit.**

**Still deferred:** moving iOS onto the Gemini proxy. The proxy is not deployed, so iOS would point
at a URL that does not exist. iOS stays on the Rork gateway until there is a live endpoint — which
also means **the leaked Rork key cannot be rotated yet.**


**Unit 16 — Android Config Bootstrap and Cosmetic De-Rork**
(`context/feature-specs/16-cosmetic-derork.md`) — complete.

Everything in the de-Rork that needs neither a Mac, an Android SDK, nor a decision is now done.

- `android/setup-config.sh` + `Config.kt.example` — the Android half of the config bootstrap. It
  derives the package from `MainActivity.kt` rather than hardcoding it, so it survives unit 14's
  rename. Tested; output confirmed gitignored.
- `rork.json` deleted — checked first, and it was referenced **only in documentation**. No build,
  script or source reads it.
- `web/package.json` name: `rork-web-app` → `modernbody-web`.
- `.rork/` cache entries and `rork-eslint.config.js` removed from all three `.gitignore` files.
- `// Created by Rork on …` headers stripped from four Swift files. **Comment-only** —
  `git diff --stat` confirms two deleted lines per file. A removed comment cannot change
  compilation, which is why this was acceptable without a compiler, unlike unit 12.

Checks unchanged: typecheck 0, lint 9 warnings, 158 tests, build passes.

### Rork surface after this unit

| Area | Remaining | Blocked on |
| --- | --- | --- |
| `web/src`, `web/api` | **None real** | — (`AIErrorKind` matches "rorK" incidentally) |
| `ios-calzy` | 3 files | Unit 12 (proxy) and unit 15 (bundle id) |
| `android` | 36 files | Unit 14 — 35 are the package name |

**There is no unblocked de-Rork work left.** Everything remaining maps to:

- **Unit 12** — native clients onto the proxy. Needs a green Mac build first, so a Swift or Kotlin
  mistake is caught against a project known to compile. This is also what finally lets the leaked
  Rork key be rotated.
- **Unit 14** — Android package rename. Needs a target namespace nobody has chosen.
- **Unit 15** — iOS bundle id. Needs the App Store Connect answer, which is permanent either way.


**Unit 11a — Runnable From a Clean Clone** (`context/feature-specs/11a-runnable-from-a-clean-clone.md`)
— complete. Done so the human's Mac works on first pull.

Four traps stood between a clean clone and a running app, none discoverable from the repo:

1. **`vite dev` did not serve `/api/*`.** After unit 11 the scanner posts to `/api/analyze`, which
   only the *host* runs in production — locally the request fell through to the SPA fallback and
   failed to parse. **Scanning was dead in development.** A dev-server plugin now mounts the real
   handler via `ssrLoadModule`, so local dev exercises the same code the deployment does.
2. **iOS did not compile from a clean clone.** `ios-calzy/setup-config.sh` creates the gitignored
   `Config.swift` from a committed template, refuses to overwrite, and is one command.
3. **Node version.** `.nvmrc` pins 24 and `engines` records `>=20.12.0`, so the v18-via-nvm failure
   stops looking like a code fault.
4. **No README at all.** The repo now has one: run instructions per platform, the env-var rules,
   deploy settings, and a map of the brain.

**Verified against a live dev server**, exercising the real handler — not a stub:

| Request | Result |
| --- | --- |
| Valid body, no key | **503 `notConfigured`** |
| `kind: "audio"` | **400** with a specific message |
| Whitespace content | **400** |
| Image that is not a data URL | **400** |
| Valid body, bad key | **502** — the request genuinely reached Gemini |
| `GET` | **405** |

Also confirmed `GEMINI_API_KEY` never appears in `dist/`, and `setup-config.sh` output is ignored
by `git check-ignore`. 158 tests, typecheck 0, lint 9 warnings, build passes.

**Finding:** an invalid API key surfaces to users as "Something went wrong" rather than an auth
error, because Google returns 400 for a bad key and this proxy treats 400 as our fault. That is the
right user-facing choice — the alternative tells them to reload, which cannot supply a server key —
but it makes the likeliest deployment failure look generic. The server log now says
`check GEMINI_API_KEY and GEMINI_MODEL` on 400/403.


**Unit 11 — Gemini Proxy** (`context/feature-specs/11-gemini-proxy.md`) — complete for web.
**First unit of the de-Rork project**, which the human scoped as "everything", with Gemini chosen
as the direct provider.

`toolkit.rork.com` was the only runtime dependency on Rork, and the credential it required shipped
inside the web bundle and both app binaries. Independence and the top Known Debt item were the
same problem, so they were fixed together.

`web/api/analyze.ts` now calls Gemini directly with the key held server-side. The web client posts
`{ kind, content, jesterMode, language }` to `/api/analyze` and holds no credential at all.

**Verified:**

- Zero occurrences of "rork" in the built bundle; no `EXPO_PUBLIC` reference anywhere.
- **The system prompt no longer ships to users.** It lived in three clients, had to be edited
  three times, and was downloaded by every user. Now one server-side copy, changeable without
  releasing an app.
- Bundle got *smaller*: 1,009.54 kB → 1,007.63 kB.
- 158 tests across 7 files, up from 118. Typecheck 0, lint 9 warnings, build passes.
- `api/` is now typechecked too, so the proxy is not an unchecked blind spot.

**Not verified, and it matters:** no end-to-end call to Gemini was made. There is no API key here
and `vite dev` does not serve `/api/*`. **Deploy, then scan once, before trusting it.**

**Confirm the model id before deploying.** `GEMINI_MODEL` defaults to `gemini-2.5-flash`; check it
against Google's current documentation. It is env-configurable so a correction is config, not code.

**Invariant 1 was restated, not broken** — "no backend" became "no user data is stored off the
device". The proxy is stateless. The shipped privacy policy already described this accurately, so
no legal copy changed. Both decisions are in `context/decision-log.md`.

**Still on the Rork gateway: iOS and Android.** Nothing is broken mid-migration — both keep working
until unit 12. **Rotate the Rork key once all three clients are off it**; anything already shipped
has leaked it.

**Toolchain trap found:** Git Bash resolves Node **v18.20.4** via nvm while PowerShell uses
**v24.15.0**. Vite 8 needs 20.12+, so the test runner dies under the nvm default with a
`styleText` import error that looks like a code fault. A `.nvmrc` would prevent this; not added
yet, since it is unrelated to this unit.


**Unit 10 — i18n and Food-Search Tests** (`context/feature-specs/10-i18n-and-food-search-tests.md`)
— complete.

Localisation is one of this product's largest surfaces — 32 languages, three right-to-left — and
none of it was tested. Unit 08 had already found a shipped language bug, so the area warranted
attention rather than trust.

**118 tests across 6 files, up from 74. All pass, no defect found.**

- `i18n.test.ts` — the language table, `languageFor`, `translate` and its two-step fallback, and
  `browserLanguage` across every branch: `zh-TW` and `zh-Hant-HK` → Traditional, `zh-CN` and bare
  `zh` → Simplified, `no` → `nb`, unsupported tags falling through to the next preference, and
  English for an empty list or absent `navigator`.
- `foods.test.ts` — the bundled table's integrity, search matching and ranking, `foodToItem`, and
  `presetCalories` weight scaling.

**Catalogue integrity is now enforced, not assumed.** Measured first: 32 tables, 52 keys each,
identical key sets, no empty values. `context/execution-standards.md` requires shipped copy to
reach all 32 locales — that rule was honour-system and is now a failing test.

**A note on test quality.** The ranking test was first written with an early return for the case
where no query exercises both match kinds, which would have let it pass while asserting nothing.
Checked rather than assumed: the table yields 6 rankable queries, the first being `"protein"` with
2 name matches against 9 tag-only. The escape hatch was replaced with an explicit assertion, so a
future data change that makes ranking untestable fails loudly instead of going quietly green.

No new dependencies. `navigator` is stubbed with `vi.stubGlobal` rather than pulling in a DOM.

**Still untested and now the highest-value target:** `store/AppStore.tsx` — streaks, 7-day
averages, the weight `isLatest` rule, water undo, case-insensitive saved-food matching. It needs
`@testing-library/react` plus `jsdom` or `happy-dom`, and adding devDependencies carries a
wrinkle worth deciding separately: the lockfile is `bun.lock` with no `package-lock.json`, so an
npm-added dependency would not appear in the lockfile CI installs from.


**Unit 09 — Continuous Integration** (`context/feature-specs/09-continuous-integration.md`) —
complete, with a stated verification limit.

Unit 07 found that nothing enforced any check: `vite build` does not typecheck, and there was no
CI. Strict TypeScript and 74 tests only protect the codebase if something runs them.
`.github/workflows/web-ci.yml` now runs typecheck, lint, unit tests and build on every push and PR
touching `web/`, path-filtered so documentation commits do not burn minutes.

**The browser suite is deliberately excluded.** Adding `npx playwright install` would have looked
like fixing the one failing check, but that suite **has never been observed to pass** — it could
not start locally. Its result is unknown, not green, and asserting it in CI would be a claim
nobody has verified. Enabling it is a follow-up once someone runs `npx playwright install` and
confirms.

**Verification limit:** the workflow itself **has not been observed to run.** GitHub Actions
cannot be executed from here and the repo is private, so its status cannot be read back. Every
command in it passes locally and the YAML parses — but **check the first run before trusting it.**
A failure there will be environmental, not a code defect.

**Unit 08 — ScanSheet Stale Language** (`context/feature-specs/08-scansheet-stale-language.md`) —
complete.

Recorded at onboarding as "the one real warning among the ten." On reading, it was a **user-visible
bug in a 32-language app.** `handle` passes `language.englishName` to the model to choose the
reply language, but omitted it from its `useCallback` deps. `ScanSheet` is mounted permanently by
`Index.tsx:61` with an `open` prop, so the stale closure survived the whole session: change
language in Settings, scan a meal, and item names came back in the *previous* language while the
interface was translated.

One dependency added, with a comment recording the failure so nobody tidies it away.

**Baseline changed: lint is now 9 warnings, not 10.** All nine are
`react-refresh/only-export-components`, seven in generated `components/ui/**`. They are HMR hints,
not defects. `context/execution-standards.md` updated so future units are not measured against a
stale number.


**Unit 07 — Enable Strict TypeScript** (`context/feature-specs/07-enable-strict-typescript.md`) —
complete.

I predicted this would "surface errors across the whole tree." **It surfaced none.** Full
`--strict`, with `noImplicitAny`, `noFallthroughCasesInSwitch`, `noUnusedLocals` and
`noUnusedParameters` all added, produces **0 errors** across 94 files including generated
`components/ui/**`.

Because a zero from a typechecker is also what a misconfigured command produces, it was verified
three ways: `--showConfig` confirmed the flags applied, `--listFiles` confirmed 94 `src/` files
were checked, and a temporary probe with a deliberate null-deref and implicit `any` produced
exactly `TS18047` and `TS7006` before being deleted.

Trap worth remembering: an explicit `noImplicitAny: false` **overrides** the `strict` umbrella.
Flipping `strict` alone would have quietly delivered less than it looked like.

Enabled `strict` and `noFallthroughCasesInSwitch` (matching `tsconfig.node.json`, which was
already strict), and added `npm run typecheck`. Left `noUnusedLocals`/`noUnusedParameters` off —
both sibling configs and `eslint.config.js:23` deliberately disable that class of check.

**The unit paid for itself:** with strict on, zod's inference is correct, so the assertion unit 06
needed in `lib/ai.ts` was removed. The compiler now verifies what it previously had to be told.

**Scope deviation, recorded:** the spec put `lib/ai.ts` out of scope. One line was changed anyway,
because the comment there asserted the compiler "cannot see" something it now can — leaving it
would have shipped a comment that lies about adjacent code. Documented in the spec rather than
absorbed silently.

**Finding — nothing enforces typechecking.** `npm run build` runs `vite build`, which does not
typecheck, and there is no CI in the repo. A type error can still reach a production bundle.
Adding CI is its own unit and interacts with the unresolved Rork question, so it is recorded, not
solved.

| Check | Baseline | After |
| --- | --- | --- |
| Typecheck | 0 errors, non-strict | **0 errors, full strict** |
| Lint | 0 errors, 10 warnings | **0 errors, 10 warnings** — same 10 |
| Unit tests | 1 test | **74 tests, all pass** |
| Build | Pass | **Pass** |


**Unit 06 — Validate the AI Response** (`context/feature-specs/06-validate-ai-response.md`) —
complete.

`JSON.parse(json) as AnalysisResult` checked nothing. An item missing `calories` became NaN via
`Math.max(0, Math.round(undefined))`, was persisted to `localStorage`, and propagated into the
Home tab's calorie ring — surviving reload. `healthScore` had no clamping at all, and
`Home.tsx:68` averages it across the day, so one string value corrupted the day's health average.

Replaced with a zod schema and an exported `parseAnalysis(text)` covering extract → parse →
validate, so the untrusted-input path is testable without the network. **74 tests, up from 56.**

Numeric strings are coerced, because models return `"200"` for `200` routinely. `isFood` is
deliberately **not** `z.coerce.boolean()`, which maps `"false"` to `true`.

**Two findings beyond the fix:**

1. **Typecheck failed first, with TS2322.** Under `strict: false`, zod's inference degrades to
   all-properties-optional, so the compiler could not see that `safeParse` had guaranteed the
   shape. This is concrete evidence for the standing observation that this codebase is written
   strictly while configured loosely. **A "turn on `strict`" unit is now worth scheduling.**
2. **The bundle cost is real:** +13.80 kB gzip (273.20 → 287.00), on a chunk the build already
   warns is over 500 kB. A hand-rolled validator would have cost nothing. Recorded with a
   reversal condition — the test suite pins the behaviour, so swapping it out later is safe.

| Check | Baseline | After |
| --- | --- | --- |
| Unit tests | 1 test | **74 tests, 4 files, all pass** |
| Typecheck | 0 errors | **0 errors** |
| Lint | 0 errors, 10 warnings | **0 errors, 10 warnings** — same 10 |
| Build | 942.95 kB | **1,009.54 kB** (+13.80 kB gzip, zod) |


**Unit 05 — Deployable Legal URLs** (`context/feature-specs/05-deployable-legal-urls.md`) —
complete.

Unit 02's routes worked in development and would still have **404'd in production**. The build
emits one `index.html`; `dist/` had no `privacy/` directory, so a static host had nothing to
serve for a direct request to `/privacy` — which is exactly the request App Store Connect and the
Play Console make when validating a Privacy Policy URL. `vite preview` hides this, because it has
SPA fallback built in.

Added `web/public/_redirects` (Netlify, Cloudflare Pages) and `web/vercel.json` (Vercel), both
scoped so real files still win and hashed assets are untouched. Three hosts covered deliberately
— the human has not picked one and choosing for them would be the wrong call.

Also set the document title on the legal routes, since these are public pages a reviewer opens
directly, with the previous title restored on unmount.

Verified in the running app: `/privacy` renders, tab reads "Privacy Policy — ModernBody", the
page scrolls (1929 px of content in a 694 px viewport), the title reverts on returning to `/`, no
console errors. The scroll check mattered — the global stylesheet pins `body { overflow: hidden }`
for the app shell, so a page relying on document scroll would have been unreadable past the first
screen.

**Honest limit:** the rewrite rules are instructions to a host that does not exist yet and
**cannot be verified here.** What is verified is that the fallback ships in `dist/`, the config is
valid, and the routes render. **First thing to do after deploying: load `/privacy` on the live
URL and confirm it is not a 404 — before pasting it into App Store Connect.**


**Unit 04 — Honest AI Unconfigured Error** (`context/feature-specs/04-honest-ai-unconfigured-error.md`)
— complete, web only.

`endpoint()` in `web/src/lib/ai.ts` could never return `null`, because `EXPO_PUBLIC_TOOLKIT_URL`
falls back to the public gateway. So `isAIConfigured` was unconditionally `true`, and a build with
no key POSTed `Authorization: Bearer ` (empty), collected a 401, and told the user *"AI features
are currently unavailable. Please reload the page."* — for a missing build-time credential no
reload can supply. The correct message, `notConfigured`, already existed and was unreachable.

Anyone cloning this repo hits it immediately, since `.env` is gitignored.

Fixed by requiring a key before AI counts as configured, and bailing before `fetch`.
`web/src/test/ai.test.ts` pins it, and also covers `resultToItems` clamping and `messageForError`.

**Verified the tests actually catch the bug** by running them against `HEAD:web/src/lib/ai.ts`:
3 failures — `expected true to be false`, and `expected 'authError' to be 'notConfigured'` twice.
Suite runtime for that file also fell from 1986 ms to 24 ms, the difference being real HTTP
requests to `toolkit.rork.com` that the pre-fix code was making *during tests*.

Found along the way: `isAIConfigured` is exported but read nowhere. The scan and describe sheets
call the analyzers directly. It is now correct, but whether the UI should use it to hide Scan and
Type in an unconfigured build is an open UX question.

**Native follow-up deliberately not done.** The same bug exists at
`NutritionAI.swift:89-92` and `AiService.kt:74-81`. Both need the same one-line condition, but
neither can be compiled or run on this machine, and shipping an unverified edit to two platforms
would breach the rule against claiming checks that did not run. Queued as its own unit.

| Check | Baseline | After |
| --- | --- | --- |
| Unit tests | 1 test | **56 tests, 4 files, all pass** |
| Typecheck | 0 errors | **0 errors** |
| Lint | 0 errors, 10 warnings | **0 errors, 10 warnings** — same 10 |
| Build | Pass | **Pass** |


**Unit 03 — Domain-Layer Tests** (`context/feature-specs/03-domain-layer-tests.md`) — complete.

The baseline recorded test coverage as effectively zero: one placeholder asserting `true === true`,
against a domain layer that owns every number the user sees. That is now covered.

- `web/src/test/nutrition.test.ts` — BMR, activity multipliers, all three goal branches, the
  1200 kcal floor, macro splits, BMI boundaries, portion arithmetic, and `mealWithCalories`
  including its empty-items path, drift correction and clamping.
- `web/src/test/dates.test.ts` — the arithmetic helpers. Locale-dependent formatters are
  deliberately excluded; asserting on `toLocaleDateString` output would make the suite
  machine-dependent rather than correct.

**Result: 46 tests, 3 files, all passing on the first run. No defect found.** Every hand-computed
expectation matched the implementation — the domain layer was already correct, it just had
nothing proving it. Two source files were read closely to derive those expectations and neither
was modified.

| Check | Baseline | After |
| --- | --- | --- |
| Unit tests | 1 test (placeholder) | **46 tests, 3 files, all pass** |
| Typecheck | 0 errors | **0 errors** |
| Lint | 0 errors, 10 warnings | **0 errors, 10 warnings** — same 10 |
| Build | 951.81 kB | **951.81 kB** — byte-identical, tests do not ship |

Still uncovered and worth a later unit: `lib/ai.ts` (network), `lib/image.ts` (DOM canvas),
`store/AppStore.tsx` (needs a React testing setup), and the browser suite, which remains blocked
on `npx playwright install`.


**Unit 02 — Public Legal Routes** (`context/feature-specs/02-public-legal-routes.md`) — code
complete, pending human sign-off on two legal-copy deviations.

The web build linked "Privacy Policy" and "Terms of Use" to `https://rork.app/...` — the
**toolchain vendor's** documents, which say nothing about ModernBody's data handling. The iOS app
already contained excellent policy text with nowhere public to live, while `Legal.swift:18,21`
shipped with its URL constants empty and App Store Connect requires a reachable Privacy Policy
URL as metadata.

Ported `Legal.swift` into `web/src/lib/legal.ts`, rendered it via `web/src/pages/Legal.tsx` at
`/privacy` and `/terms`, and repointed the Settings rows. Deploying the web build now produces
the store-required URL as a side effect, and the in-app rows plus the public page render the same
module so they cannot drift.

Files changed — four, no native code:

```
M  web/src/App.tsx           (+2 routes)
M  web/src/pages/Settings.tsx (2 hrefs, conditional support row)
?? web/src/lib/legal.ts       (new)
?? web/src/pages/Legal.tsx    (new)
```

**Follow-on, same unit (2026-08-20):** the human confirmed they do **not** own `calzy.app`, so
`Settings.tsx` was linking `mailto:support@calzy.app` — a domain outside their control. `Legal.swift:23-24`
already defines the correct rule for iOS: an empty support address hides the row rather than
shipping a dead link. The web build now follows the same rule via `supportEmail` in
`web/src/lib/legal.ts`, currently empty, so the row is hidden. Set it once a real, monitored
inbox exists. **Still outstanding:** App Store Connect requires a Support URL as metadata, and
there is no domain for it yet.

**Checks after the change, against the recorded baseline:**

| Check | Baseline | After |
| --- | --- | --- |
| Typecheck | 0 errors | **0 errors** |
| Lint | 0 errors, 10 warnings | **0 errors, 10 warnings** — same 10, none new |
| Unit tests | 1 passed | **1 passed** |
| Build | Pass, 942.95 kB | **Pass, 951.90 kB** (+9 kB, the document text) |
| Browser tests | Fail — Playwright missing | Unchanged, still pre-existing |

Also verified by hand in the running dev server: `/privacy` and `/terms` both render in full,
zero console errors.

**Needs sign-off:** two deviations from the Swift legal text — "your iPhone" became "your
device", and one sentence was added about browser local storage on web. Both are accuracy fixes,
but they are legal copy and a human should approve them. See `context/decision-log.md`.

## In Progress

**Unit 01 — App Store Review Response** (`context/feature-specs/01-app-store-review-response.md`)

- Drafted: `submission/app-review-information.md` — paste-ready answers to Apple's seven items,
  a physical-device recording shot list, and a pre-send risk checklist. Every claim traced to
  source.
- No application file was modified.
- **Blocked on seven human-only inputs** — see the deliverable's "What I still need from you".

Highest-risk finding: `ios-calzy/.../Utilities/Legal.swift:18,21,24` ships with
`privacyPolicyURL`, `termsOfUseURL` and `supportEmail` empty. That is deliberate and safe inside
the binary, but App Store Connect requires Privacy Policy and Support URLs as app metadata. If
those fields are not live, that alone causes a second rejection.

Second finding: the shipping privacy policy claims meal photos are "not used ... to train
models". That is a claim about Rork's gateway and its downstream providers, not about this
codebase, and needs written backing from Rork.

No other implementation is underway. The brain gate in `CLAUDE.md` still applies to code work
until Q1 below is answered.

## Phase 3 Questions — Blocking

**1. Is Rork still the source of truth?** *(This one gates everything.)*

The history is two commits, the second literally "New version from Rork". If the app gets
re-exported from Rork, every hand edit made in this repo is destroyed — and the entire
spec-driven build workflow does not apply. Which is it:

- (a) Rork is retired; this repo is now the editing surface.
- (b) Rork still generates; this repo is read-only and the brain is for understanding only.
- (c) Mixed — Rork for scaffolding, hand edits for specific areas. If so, which areas?

**2. What is the first real goal?** — **ANSWERED 2026-08-20 by circumstance.** ModernBody 1.0.0
is in App Store review and was met with a Guideline 2.1 information request on 2026-08-10.
Launch is the goal; getting through review is the immediate work. Still open: what comes *after*
approval.

**3. Which platform actually ships first?** — **ANSWERED: iOS.** It is the platform in active
App Store review. Still open: whether Android and web are live targets behind it, or parked.
Note only `web` builds from a clean clone today.

**4. Ratify or strike the six proposed invariants** in `context/architecture.md`. They were
derived from what the code currently upholds, not from anyone's intent. The one most worth your
attention: *"all user data stays on the device"* — is that a product promise or just where the
code happens to be today?

**5. The AI key is client-side on all three platforms.** Extractable from the web bundle and
from both app binaries; anyone can spend your Rork credits. Fixing it properly means
introducing this project's first backend. Is that acceptable, or is the exposure a known and
accepted risk for now?

**6. Is the paywall meant to become real?** It currently toggles `isPro` locally and labels its
prices "illustrative in this preview build". Is billing in scope?

**7. Who is this for, and what does winning look like?** Needed to complete
`project-overview.md` and `strategy-context.md` — target user, the problem in your words, and
one or two measurable success criteria.

**8. Smaller, non-blocking:** Is dark mode planned or is the scaffolding vestigial? Is jester
mode a headline feature or an easter egg? British or American spelling for user-facing copy?

## Next Up

1. Answer the Phase 3 questions.
2. Complete `project-overview.md` and `strategy-context.md` from those answers (Phase 4).
3. Write the first unit spec with `/brain-spec`.

Candidate first units, pending the answer to Q1 and Q2 — **not yet recommended, just visible
from the evidence**:

- Test the domain layer. `lib/nutrition.ts` is pure, untested, and owns every number the user
  sees. Smallest possible unit that exercises the whole spec-build-check loop.
- Fix `isAIConfigured` so a missing key reports "not configured" instead of failing as a 401
  (`lib/ai.ts:56,65`). Tiny, self-contained, and removes a genuinely misleading error.
- Validate the AI response with zod, which is already a dependency but imported nowhere. The
  model reply is the app's most untrusted input and is currently cast, not parsed.
- Handle localStorage quota overflow visibly instead of a silent `console.warn`.

## Blockers

- **Phase 3 questions unanswered.** The brain cannot be completed and no unit should be built
  until Q1 in particular is resolved.

## Architecture / Strategy Decisions

- Adopted the spec-driven Project Brain for an existing codebase: code is ground truth for what
  is, the human is ground truth for what should be, and anything neither proves is marked
  `[UNKNOWN — needs human input]`.
- The brain's readme is `PROJECT-BRAIN.md`, not `README.md`, so it cannot clobber the app repo's
  own README.
- Four pre-existing architectural decisions were reconstructed into `decision-log.md` with their
  reasoning explicitly labelled INFERRED.

## Session Notes

Read this as a new agent with no memory.

The workspace root is now the application repo — a Rork export of **ModernBody** (codename
Calzy), an AI food scanner shipped as three hand-mirrored implementations: `web/` (React 19 +
Vite + Tailwind + shadcn), `ios-calzy/` (SwiftUI), `android/` (Compose). The Project Brain files
sit alongside as untracked files, ready to commit when the human decides to.

`architecture.md`, `execution-standards.md`, and `ui-brand-context.md` are filled from real
evidence and are trustworthy. `project-overview.md` is half filled — trust the *(from code)*
sections, do not trust anything marked `[NEEDS HUMAN INPUT]`. `strategy-context.md` is still an
untouched template.

**Do not start implementing.** The gate in `CLAUDE.md` is still closed, and for a specific
reason: it is not yet known whether hand edits to this repo survive. The repo has two commits,
the latest being "New version from Rork". If Rork re-exports, hand-written work is lost. That is
Q1 and it blocks everything else.

`web/node_modules/` was installed during onboarding (378 packages, gitignored, `--no-package-lock`
so no stray lockfile was created). Baseline check results are recorded above — measure against
them, and remember `npm test` was already failing before anyone touched anything.

## Last Updated

- Date: 2026-08-20
- Updated by: Claude Code
