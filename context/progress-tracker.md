# Progress Tracker

Update this file after every meaningful implementation or project change.

## Current Phase

- **Unit 01 in progress — App Store review response. Brain onboarding Phase 3 still open.**

## Current Goal

Reply to Apple's Guideline 2.1 "Information Needed" message of 2026-08-10 so the ModernBody
1.0.0 iOS submission can continue review.

This unit was chosen by circumstance rather than by planning, and it is the right choice: it
changes **no application code**, so it proceeds cleanly even with the Rork-overwrite question
(Q1) still unresolved.

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
