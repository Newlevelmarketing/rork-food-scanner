# Decision Log

Record important decisions here so the project does not lose its reasoning over time.

## How to Use

Every important decision should include:

- Date
- Decision
- Context
- Options considered
- Why this decision was made
- Impact
- Reversal condition, if any

---

## Decisions

### 2026-08-20 — Adopt the Project Brain for this existing app

**Decision:** Manage this existing application through a spec-driven Project Brain: context
files in `context/` as the source of truth, one unit spec per change, and Claude Code
implementing only against a written spec.

**Context:** The application already exists and was built without this system. Work on it
needs to become reviewable and resumable, and AI agents need a stable memory of what the
project is, how it is built, and what must never break.

**Options Considered:**

1. Keep working ad hoc, prompting per task with no persistent context.
2. Rely on a single `CLAUDE.md` with no separate context files.
3. Adopt the full Project Brain, filled from the existing codebase.

**Reasoning:** Option 1 loses reasoning between sessions and produces scope creep. Option 2
collapses under its own weight once architecture, strategy, brand, and progress all compete
for the same file. Option 3 separates concerns, keeps the always-loaded entry point small,
and makes each unit of work independently reviewable.

**Impact:** All feature work now requires a unit spec in `context/feature-specs/`. Context
files must be updated in the same session as any change that alters architecture, storage,
conventions, scope, or brand.

**Reversal Condition:** If the overhead of maintaining the brain exceeds the cost of the
rework it prevents — for example on a project that becomes a short-lived throwaway — drop
back to a single `CLAUDE.md`.

---

### 2026-08-20 — Validate the AI response at runtime with zod

**Decision:** Parse the model reply through a zod schema before it reaches the store, replacing
the `JSON.parse(json) as AnalysisResult` assertion in `web/src/lib/ai.ts`.

**Context:** `as` is a promise to the compiler, not a check. The model reply is the app's least
trusted input — generated text from a third-party service over the network — and nothing verified
its shape. An item missing `calories` produced `Math.max(0, Math.round(undefined))` → **NaN**,
which was written into `MealEntry.items`, persisted to `localStorage`, and propagated through
`mealCalories` into the Home tab's calorie ring. `healthScore` was worse: nothing clamped or
rounded it, and `Home.tsx:68` averages it across the day, so a single string value turned the
day's health average into concatenated garbage. The corruption survived reload.

The NaN mechanism was confirmed directly: `Math.round(undefined)` is `NaN`, `Math.max(0, NaN)` is
`NaN`, and `500 + NaN` is `NaN`.

`zod` had been a declared dependency since before this brain existed, imported nowhere.

**Options Considered:**

1. Leave the assertion and rely on `resultToItems`' clamping — which does not help, since
   `Math.max(0, NaN)` is NaN.
2. Hand-roll a validator — roughly 40 lines, no bundle cost.
3. Use zod, already declared in `package.json`.

**Reasoning:** Option 3 is idiomatic, declarative, and keeps the schema readable next to the
system prompt that produces the payload. The dependency was already committed to.

**Impact — with an honest cost:** the bundle grew from **951.81 kB to 1,009.54 kB** raw, and
**273.20 kB to 287.00 kB gzipped: +13.80 kB gzip** for validating one payload shape. The build
already warns that the chunk exceeds 500 kB. Option 2 would have cost nothing.

**Reversal Condition:** If bundle size becomes a priority — a plausible outcome given the
existing chunk warning — replace the schema with a hand-written validator and drop zod. The test
suite in `web/src/test/ai.test.ts` pins the behaviour, so the swap is safe to make.

**Two sub-decisions worth recording:**

- Numeric strings are **coerced**. Models return `"200"` for `200` routinely, and rejecting that
  would fail responses that are semantically fine.
- `isFood` is **not** run through `z.coerce.boolean()`, which maps the string `"false"` to `true`
  and would log a photo of a bicycle as a meal. It accepts a real boolean or an explicit
  `"true"`/`"false"` mapping.

---

### 2026-09-03 — Admission control on the analysis proxy: fail open, and accept a speed bump

**Decision:** Gate `/api/analyze` on an `ALLOWED_ORIGINS` allowlist and a 20-per-minute in-memory
rate limit, with an unset allowlist **allowing** the request and logging a warning.

**Context:** The 2026-09-03 audit found the proxy was an unauthenticated, unmetered relay to the
operator's Gemini key. This was a **gap in unit 11's own spec** rather than deferred work — its Out
of Scope list never mentioned abuse control either way.

**Options Considered:**

1. Leave it and rely on the URL being unpublished.
2. Shared-store rate limiting (Vercel KV / Upstash) — durable, but needs a provider account.
3. In-memory limiting plus an origin allowlist, shipped now.

**Reasoning:** Option 3. Option 1 is not a position once the endpoint is deployed. Option 2 is the
right end state but needs a decision this project has not made, and shipping nothing while waiting
leaves the endpoint open.

**Two trade-offs a future reader should not have to re-derive:**

- **Fail open when `ALLOWED_ORIGINS` is unset.** Failing closed by default would break the first
  deployment before it could be configured. The cost is that an unconfigured deployment is
  unprotected, so `.env.example` and the README both say to set it before deploying.
- **A null `Origin` is always allowed**, because native apps send none and are intended callers.
  Unit 12 should give them a signed nonce rather than leaving that hole open permanently.

**Impact — stated plainly:** the rate-limit store is **per-instance memory**. Serverless hosts run
many instances, and `x-forwarded-for` is spoofable, so this stops a naive loop from one client and
nothing more. It is a speed bump. **Durable protection still needs a shared store or a WAF rule**,
and that remains open.

**Reversal Condition:** Any evidence of real abuse, or the first bill that looks wrong, should
promote this to option 2 immediately.

---

### 2026-08-22 — Leave Rork: call Gemini through our own proxy

**Decision:** Stand up `web/api/analyze.ts`, a stateless serverless proxy that calls Google Gemini
directly, and point the web client at it. The native clients follow in unit 12.

**Context:** The human asked to remove Rork so the project could be customised independently. Two
problems turned out to be the same problem. `toolkit.rork.com` was the only runtime dependency on
Rork, and the credential it required — `EXPO_PUBLIC_RORK_TOOLKIT_SECRET_KEY` — was sent as a
`Bearer` token from the client on all three platforms. `vite.config.ts` deliberately exposed
`EXPO_PUBLIC_*` to the bundle, so it shipped inside the web JS and both app binaries. That has been
the top Known Debt item since onboarding, and it could not be fixed without a server.

Independence and fixing the key were one job. Doing them separately would have meant building the
proxy twice.

**Options Considered:**

1. Keep the gateway and accept the exposed key.
2. Keep the gateway but proxy it, staying dependent on Rork for the model call.
3. Call Gemini directly through our own proxy.

**Reasoning:** Option 3. Gemini was chosen because it was already the primary model
(`google/gemini-3-flash`), so the prompt is tuned for it and it is the cheapest per vision call —
the smallest behavioural change available.

**Impact:**

- Verified: zero occurrences of "rork" in the built bundle, no `EXPO_PUBLIC` references, no
  credential of any kind client-side.
- **The system prompt moved server-side.** It previously existed in three clients, had to be
  edited three times, and was shipped to every user. Now one copy, changeable without releasing an
  app. Confirmed absent from `dist/`.
- The client's error taxonomy is unchanged; the proxy maps failures onto the status codes it
  already understood, so no user-facing copy moved.
- The bundle got marginally smaller.

**Cost the human should weigh:** billing moves in-house. Rork was presumably absorbing model cost;
a direct Gemini key is billed per scan.

**Reversal Condition:** None expected. If Gemini proves unsuitable, the proxy is the only file that
changes — the clients neither know nor care which model answers.

---

### 2026-08-22 — Restate the "no backend" invariant rather than break it

**Decision:** Reword invariant 1 in `context/architecture.md` from "all user data stays on the
device, no backend" to "**no user data is stored off the device**", and record that a stateless
analysis proxy now exists.

**Context:** The proxy introduces the project's first server component, which contradicts an
invariant proposed at onboarding. An invariant that is quietly broken is worse than one that never
existed, so it needed restating in the open.

**Reasoning:** The original wording conflated two things: *no server* and *no user data retained
elsewhere*. Only the second is a property anyone actually cares about, and it is the one the
privacy policy promises. The proxy holds a credential, forwards one photo or description, and
persists nothing — so the meaningful guarantee survives intact.

The shipped privacy policy already says user content "is transmitted over an encrypted connection
to our AI processing provider", so **no legal copy needed changing** — it described this
architecture accurately before it existed.

**Impact:** Invariant 1 now describes what the system actually guarantees. Still pending human
ratification, along with the other five.

**Reversal Condition:** If the proxy ever logs, caches or stores request content, this invariant is
broken for real and must be rewritten again — or the behaviour reverted.

---

### 2026-08-22 — Adopt CI for the web app, and exclude the browser suite from it

**Decision:** Add `.github/workflows/web-ci.yml` running typecheck, lint, unit tests and build on
every push and pull request touching `web/`. Deliberately exclude the Playwright browser suite.

**Context:** Unit 07 surfaced that nothing enforced any check. `npm run build` runs `vite build`,
which does not typecheck, and the repo had no CI. Strict TypeScript and 74 tests only protect the
codebase if something runs them.

**Options Considered:**

1. No CI — rely on discipline.
2. CI including the browser suite, adding `npx playwright install`.
3. CI covering only checks already observed to pass.

**Reasoning:** Option 3. Option 2 is tempting because it would appear to fix the one failing
check, but **the browser suite has never been observed to pass** — it could not start locally, so
its result is unknown rather than green. Putting it in CI would assert a check nobody has seen
succeed, and a failure in `calendar.browser.test.tsx` for an unrelated reason would look like this
workflow's fault. Enabling it is a small follow-up once someone runs `npx playwright install` and
confirms it passes.

**Impact:** Every push touching `web/` is now gated on four checks that pass locally today.
Documentation-only commits are path-filtered out.

**Known gap carried forward:** `npm ci` is unavailable because the lockfile is `bun.lock` with no
`package-lock.json`, so CI installs resolve unpinned. Commented in the workflow so it is not
mistaken for reproducible.

**Reversal Condition:** If CI minutes become a cost concern on a private repo, narrow the triggers
to pull requests only.

---

### 2026-08-21 — Enable strict TypeScript

**Decision:** Set `strict: true` and `noFallthroughCasesInSwitch: true` in
`web/tsconfig.app.json`, remove the `noImplicitAny: false` override, and add an `npm run typecheck`
script.

**Context:** Recorded at onboarding as a gap — the compiler configured loosely while the code was
written strictly. Unit 06 turned it into a real cost when zod's inference degraded without
`strictNullChecks`. `tsconfig.node.json` already set `strict: true`, so the build tooling was
strict and only the application was not.

**Options Considered:**

1. Leave it and keep working around the consequences.
2. Enable `strictNullChecks` alone — the minimum that fixes zod.
3. Enable full `strict`, plus the fallthrough check to match the sibling config.

**Reasoning:** Measured before deciding. Full `strict` produces **zero errors** across 94 files,
generated `components/ui/**` included — verified with `--showConfig`, a file count, and a probe
file whose deliberate violations produced `TS18047` and `TS7006` before being removed. Option 3
costs nothing and option 2 would have left the weaker setting in place for no gain.

One trap: an explicit `noImplicitAny: false` overrides the `strict` umbrella. Flipping `strict`
alone would have delivered less than it appeared to.

**Impact:** No source change was required. The assertion unit 06 needed in `lib/ai.ts` became
unnecessary and was removed — the compiler now verifies what it previously had to be told.

**Deliberately left off:** `noUnusedLocals` and `noUnusedParameters`, though both measure clean.
Both sibling configs set them false and `eslint.config.js:23` disables the ESLint equivalent. That
is a consistent existing stance and overturning it is a separate decision.

**Reversal Condition:** None foreseen. If a future dependency cannot satisfy strict, prefer typing
around it over relaxing the project.

---

### 2026-08-20 — Finding: `strict: false` is degrading zod's type inference

> **Resolved 2026-08-21** by the decision above. Kept for the reasoning trail.

**Status:** **Noted, not fixed.** Recorded because it will recur with any schema library.

`tsconfig.app.json` sets `strict: false`, and therefore `strictNullChecks: false`. Under that
setting zod's inferred output type silently degrades to **all properties optional**, so
`parseAnalysis` failed to typecheck against the `AnalysisResult` interface with TS2322.

Worked around with a single documented assertion after `safeParse` has already verified the shape
at runtime. That assertion is sound — unlike the one this unit removed, the check really has
happened by that line — but it exists only because the compiler cannot see it.

This is concrete evidence for the observation already in `context/execution-standards.md`: the
codebase is written strictly while the compiler is configured loosely. Turning `strict` on is its
own unit with its own spec, because it will surface errors across the whole tree. It is now worth
scheduling.

---

### 2026-08-20 — Host the legal documents on the web build at `/privacy` and `/terms`

**Decision:** Port the privacy policy and terms from `ios-calzy/.../Utilities/Legal.swift` into
`web/src/lib/legal.ts`, render them at the public routes `/privacy` and `/terms`, and point the
web Settings rows at those routes.

**Context:** Two problems, one fix. First, `web/src/pages/Settings.tsx:192-194` linked to
`https://rork.app/terms` and `https://rork.app/privacy` — the **toolchain vendor's** legal pages,
which do not describe ModernBody's data handling. Second, App Store Connect and the Play Console
both require a reachable Privacy Policy URL as app metadata, and `Legal.swift:18,21` ships with
`privacyPolicyURL` and `termsOfUseURL` empty. The iOS app already contains excellent policy text;
it just had nowhere public to live.

**Options Considered:**

1. Write standalone static HTML pages and host them separately.
2. Publish the policy on a marketing site that does not exist yet.
3. Serve the documents from the web build, which already deploys, at stable routes.

**Reasoning:** Option 3 needs no new infrastructure and no new domain, and it keeps one source of
truth: the in-app Settings rows and the public URL render the same module, so they cannot drift.
Deploying the web build produces the URL the stores require as a side effect.

**Impact:** `/privacy` and `/terms` are now public, stable URLs. **Their paths must not change** —
they get submitted to Apple and Google. Once the web build is deployed, paste the deployed URLs
into `Legal.swift:18,21` so the iOS app offers web mirrors too.

**Two deliberate deviations from the Swift original,** both for accuracy, both needing the
human's sign-off as legal copy:

1. "your iPhone" → "your device", since these pages now serve web and Android as well.
2. One sentence added to section 1 stating that web data lives in browser local storage and is
   erased by clearing site data. The Swift text describes an app sandbox, which is not accurate
   for the web build.

**Reversal Condition:** A dedicated marketing site takes over as the canonical home for the
documents.

---

### 2026-08-20 — Flagged, not decided: the web and Android paywall contradicts the Terms

**Status:** **Open — needs a human decision.** Recorded here because publishing `/terms` made the
contradiction visible inside a single build.

**The contradiction:** `/terms` section 7 now states ModernBody "contains no in-app purchases and
no subscriptions" — true on iOS. But `web/src/pages/Settings.tsx:157-189` renders a SUBSCRIPTION
section opening `features/Paywall.tsx`, which advertises "$9.99 Monthly", "$59.99 Yearly",
"SAVE 50%" and "Start 3-day free trial". Android ships the equivalent `PaywallSheet.kt`.

**What the paywall actually does:** `Paywall.tsx:52` flips `store.profile.isPro`. Nothing else.
`isPro` is read in exactly three places, all of them cosmetic badge labels — it gates no feature
anywhere in the codebase.

**All four advertised perks are already free:**

| Advertised perk | Reality |
| --- | --- |
| "Unlimited AI scans" | No scan metering exists anywhere |
| "Deep progress insights" | The Progress tab is identical regardless of `isPro` |
| "Jester Mode" | Has its own free toggle in Settings, directly above the upgrade row |
| "Apple Health sync" | `profile.healthSynced` is never written by any code |

**Why it matters:** Apple's 2026-08-10 message explicitly cites Guideline 3.1.2 on subscription
information. iOS has no paywall, so this does not block the current submission. But if this UI
ever reaches iOS without real StoreKit products behind it, it is an immediate Guideline 3.1.1
rejection — and on the shipping web build it is already misleading regardless of any store rule.

**Options:** (a) remove the paywall from web and Android, matching iOS and the Terms;
(b) keep it but strip prices and the trial CTA, presenting it honestly as forthcoming;
(c) build real billing and make `isPro` gate something.

**Not actioned.** Removing or redesigning a monetisation surface is a product decision, not a
cleanup. Awaiting the human's choice.

---

## Decisions Reconstructed From the Code

> These were made before the brain existed. The **Decision** and **Impact** lines are facts read
> from the repository. The **Reasoning** lines are *inferred* — they are this agent's best
> reconstruction, not a record of what anyone actually thought. Correct or confirm them.

### [pre-2026-08-20] — Ship three hand-mirrored native implementations

**Decision:** Build the same product three times — SwiftUI, Jetpack Compose, and React — rather
than once in a cross-platform framework.

**Context:** `rork.json` declares three apps from one Rork project. `web/src/lib/types.ts:1` and
`web/src/index.css:7` both declare themselves mirrors of the iOS sources, making iOS the
reference implementation.

**Reasoning (INFERRED):** Rork generates native code per platform; fully native output avoids
bridge overhead and gives each platform its real UI toolkit.

**Impact:** Every feature is built three times. Domain models, theme tokens, AI prompts, and
nutrition math each exist in three places and can drift silently.

**Reversal Condition:** If parity cost exceeds the value of native feel — or if only one
platform actually ships — collapse to a single implementation.

---

### [pre-2026-08-20] — No backend, no accounts, device-local storage only

**Decision:** Persist all user data on the device. No server, no login, no sync.

**Context:** `web/src/store/AppStore.tsx:37` writes the entire `AppData` object to one
`localStorage` key, `calzy-data-v1`. There is no auth code and no API surface anywhere in the
repo.

**Reasoning (INFERRED):** Removes hosting cost, privacy exposure, and signup friction. A food
diary is single-user and single-device by nature.

**Impact:** No sync, no backup, no multi-device. Clearing site data destroys all history.
Storage is quota-bound and photos share the same key.

**Reversal Condition:** Users asking for multi-device or backup, or the storage quota becoming a
real support burden.

---

### [pre-2026-08-20] — Call the AI gateway directly from the client

**Decision:** Each app calls `toolkit.rork.com` directly, authenticating with
`EXPO_PUBLIC_RORK_TOOLKIT_SECRET_KEY` sent as a `Bearer` token from the client.

**Context:** `web/src/lib/ai.ts:56-57,121-136`; `ios-calzy/.../NutritionAI.swift:184`;
`android/.../AiService.kt:80-81`. `web/vite.config.ts:23` deliberately exposes `EXPO_PUBLIC_*`
to the bundle.

**Reasoning (INFERRED):** It is the only option consistent with having no backend — Rork's
toolkit is designed to be called from generated client apps.

**Impact:** The credential ships inside the web bundle and both app binaries and can be
extracted by any user. It is a follow-on consequence of the no-backend decision, not an
independent one.

**Reversal Condition:** Any evidence of key abuse, or the moment a backend exists for another
reason. **This decision is flagged for review — see Known Debt in `architecture.md`.**

---

### [pre-2026-08-20] — Rork as the generator of record

**Decision:** Author the app in Rork and export it to this repository.

**Context:** The entire history is two commits: `5a7810a Initial commit` and
`4db8802 New version from Rork`. `Config.swift` and `Config.kt` are Rork-generated and
gitignored, so the native apps do not build from a clean clone.

**Reasoning (INFERRED):** Rork produced the three implementations; the repo is an export target
rather than the primary editing surface.

**Impact:** **Unresolved and blocking.** If Rork remains the source of truth, hand edits made in
this repository are destroyed by the next export, and the entire Project Brain build workflow
does not apply. This is the first question the human must answer.

**Reversal Condition:** A decision that this repo becomes the editing surface and Rork is
retired or used only for scaffolding.

---

### [YYYY-MM-DD] — [Decision Title]

**Decision:** [What was decided]

**Context:** [What led to this decision]

**Options Considered:**

1. [Option A]
2. [Option B]
3. [Option C]

**Reasoning:** [Why this decision is best for now]

**Impact:** [What changes because of this decision]

**Reversal Condition:** [What would make us change this decision]
