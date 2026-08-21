# Unit Spec: Continuous Integration for the Web App

## Status

- Status: **Complete** — with an honest limit on verification, below
- Owner: Claude Code
- Created: 2026-08-22
- Last Updated: 2026-08-22

## Goal

Make the checks run automatically, so the standards this project has adopted are enforced rather
than merely documented.

## Why This Matters

Unit 07 surfaced the gap: **nothing enforces typechecking.** `npm run build` runs `vite build`,
which does not typecheck, and the repo has no CI at all. Strict TypeScript was switched on and a
`typecheck` script added — but nothing makes either run. A type error could still reach a
production bundle unopposed.

The same applies to the 74 tests written in units 03, 04 and 06. They only protect the codebase if
something runs them.

## Scope

### In Scope

- `.github/workflows/web-ci.yml` — typecheck, lint, unit tests and build for `web/`

### Out of Scope — not touched

- **The native apps.** They cannot be built in CI: `Config.swift` and `Config.kt` are
  Rork-generated and gitignored, so a clean checkout has nothing to compile against. iOS would
  also need a macOS runner, a Team ID, and signing certificates as secrets. Recorded in
  `context/architecture.md` Known Debt.
- **The browser test suite.** See below.
- Deployment. This workflow verifies; it does not ship.
- Any application source file.

### The browser suite is deliberately excluded

It would be easy to add `npx playwright install` and claim the pre-existing failing check is
fixed. That would be dishonest: **the browser suite has never been observed to pass.** It could
not start locally — Playwright's browsers are not installed on this machine, and installing them
is a large download that is the human's call to make.

Its result is therefore *unknown*, not *green*. Adding it to CI would assert a check nobody has
seen succeed, and if `calendar.browser.test.tsx` fails for an unrelated reason the first red build
would be this workflow's fault rather than a real regression.

Enabling it is a small follow-up once someone has run `npx playwright install` locally and
confirmed the suite passes.

## Implementation Details

- Triggers on push to any branch and on pull requests, filtered to `web/**` and the workflow file
  itself. Documentation-only commits — of which this project has many — do not burn CI minutes.
- `defaults.run.working-directory: web`, since the app is not at the repo root.
- Node 24, matching the local toolchain the baseline was measured on (24.15.0).
- `npm install`, not `npm ci`. `npm ci` requires a `package-lock.json`; this repo's lockfile is
  `bun.lock`. Installs therefore resolve unpinned — a real reproducibility gap, already recorded
  in `context/architecture.md`, and called out in a comment in the workflow so the next reader
  does not assume it is pinned.
- 15-minute timeout so a hung job cannot run indefinitely.

## Honest Limit of Verification

**This workflow has not been observed to run.** GitHub Actions cannot be executed from this
machine, and the repository is private so its status cannot be read back without authentication.

What *is* verified:

- Every command in it passes locally: `npm run typecheck` (0 errors), `npm run lint`
  (0 errors, 9 warnings), `npx vitest run` (74 tests), `npm run build`.
- The YAML parses.

What is **not** verified: that GitHub schedules it, that the runner installs cleanly, or that it
goes green.

**Check the first run** on the next push before trusting the badge. If it fails, it will be
environmental — a runner or install issue — rather than a code defect, because the same commands
pass locally.

## Acceptance Criteria

1. ~~A workflow exists covering typecheck, lint, unit tests and build.~~ **Done.**
2. ~~It runs only for changes affecting the web app.~~ **Done.**
3. ~~Every command in it is one already verified to pass locally.~~ **Done.**
4. ~~No application source file is modified.~~ **Done.**
5. The first CI run goes green. **Unverifiable from here — human to confirm.**

## Checks Run

- [x] `npm run typecheck` — **0 errors**
- [x] `npm run lint` — **0 errors, 9 warnings**
- [x] `npx vitest run` — **4 files, 74 tests, all pass**
- [x] `npm run build` — **pass**
- [ ] The workflow itself — **cannot be run from this machine**

## Decision Log Update Required

Yes — adopting CI, and the deliberate exclusion of the browser suite.
