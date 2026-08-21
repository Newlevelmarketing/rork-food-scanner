# Unit Spec: Enable Strict TypeScript

## Status

- Status: **Complete**
- Owner: Claude Code
- Created: 2026-08-21
- Last Updated: 2026-08-21

## Result

`strict`, `strictNullChecks`, `noImplicitAny` and `noFallthroughCasesInSwitch` all resolve true.
`npm run typecheck` exits 0. Lint, tests and build unchanged.

**The unit paid for itself immediately.** With `strict` on, zod's inference is correct, so the
documented assertion unit 06 needed in `lib/ai.ts` is no longer required. Removed — `parseAnalysis`
now returns `validated.data` directly, with the compiler verifying assignability to
`AnalysisResult` rather than being told to trust it. Typecheck 0 errors, 74 tests still pass.

### Scope deviation, recorded

The spec above put `lib/ai.ts` out of scope, saying the assertion should stay "with its comment
updated" and that removal was a follow-up.

One source line was changed instead. The reasoning: the comment asserted "the compiler therefore
cannot see that `safeParse` has already guaranteed this shape" — which became **false** the moment
`strict` was enabled. Leaving it would have shipped a comment that lies about the code beside it,
breaching the docs-follow-code rule in `CLAUDE.md`. Determining the correct replacement comment
required testing whether the assertion was still needed; it was not, so the honest form of
"comment updated" was deleting both.

Acceptance criterion 4 ("no file under `src/` is modified") is therefore **not met as written**,
by one line, deliberately, for the reason above.

## Goal

Turn on `strict` in `tsconfig.app.json` so the compiler enforces what the codebase already does,
and add a `typecheck` script so it can actually be run.

## Why This Matters

`context/execution-standards.md` recorded the gap at onboarding: **the compiler is configured
loosely while the code is written strictly.** Unit 06 turned that from an observation into a
concrete cost — zod's type inference silently degrades to all-properties-optional without
`strictNullChecks`, so a validated payload failed to typecheck and needed a documented assertion
to work around a blindness the compiler didn't have to have.

`tsconfig.node.json` already sets `strict: true`. The build tooling is strict; only the
application is not.

## Measured First — the prediction was wrong

I predicted this would "surface errors across the whole tree." **It surfaces none.**

| Configuration | Errors |
| --- | --- |
| `--strict` | **0** |
| `--strict --noImplicitAny` | **0** |
| `--strict --noImplicitAny --noFallthroughCasesInSwitch` | **0** |
| the above plus `--noUnusedLocals --noUnusedParameters` | **0** |

Verified rather than trusted, because a zero from a typechecker is exactly the result a
misconfigured command also produces:

- `--showConfig` confirms the flag applies: `"strict": true`, `"strictNullChecks": true`.
- 94 files under `src/` are actually typechecked, generated `components/ui/**` included.
- A temporary probe file with a deliberate null-deref and an implicit `any` produced exactly
  `TS18047` and `TS7006`, then was removed. The check catches what it should.

One trap found along the way: the first `--strict` run left `noImplicitAny` **false**, because an
explicit setting in `tsconfig.app.json` overrides the `strict` umbrella. Simply flipping `strict`
without removing that line would have delivered less than it appeared to.

## Scope

### In Scope

- `web/tsconfig.app.json` — enable `strict`, drop the `noImplicitAny: false` override, and set
  `noFallthroughCasesInSwitch: true` to match `tsconfig.node.json`
- `web/package.json` — add a `typecheck` script

### Out of Scope — not touched

- **Any application source file.** The measurement says none is needed. If enabling strict turns
  out to require a source change, the measurement was wrong and this unit stops for a re-spec.
- `noUnusedLocals` and `noUnusedParameters` — left `false`. Both sibling configs set them false
  and `eslint.config.js:23` deliberately disables `@typescript-eslint/no-unused-vars`. That is a
  consistent existing stance, and overturning it is a separate decision even though both measure
  clean today.
- `web/tsconfig.json` — the solution file. Its `compilerOptions` are inert: it has `files: []`
  and project references do not inherit them. Documented below rather than changed.
- The assertion in `lib/ai.ts` added by unit 06. It stays, with its comment updated, because it
  documents a real historical constraint. Removing it is a follow-up once strict has settled.
- Adding CI. See the finding below.

## Implementation Details

1. `strict: false` → `strict: true`.
2. Remove `"noImplicitAny": false`. Leaving it would silently defeat part of `strict`.
3. `noFallthroughCasesInSwitch: false` → `true`, matching `tsconfig.node.json:15`. Measured free,
   and it catches a real class of bug.
4. Add `"typecheck": "tsc --noEmit -p tsconfig.app.json"` to `package.json` scripts.

### Finding: nothing enforces typechecking

`npm run build` runs `vite build`, which **does not typecheck**. There is no CI in the repo. A
type error can therefore reach a production bundle without anything objecting — enabling `strict`
improves editor feedback and the manual check, but nothing makes it run.

Not fixed here: adding CI is its own unit, and it interacts with the unresolved question of
whether this repo or Rork is the editing surface. Recorded so it is not mistaken for solved.

## Acceptance Criteria

1. `tsconfig.app.json` has `strict: true` and no `noImplicitAny` override.
2. `npm run typecheck` exists and exits 0.
3. `--showConfig` confirms `strict`, `strictNullChecks` and `noImplicitAny` are all true.
4. **No file under `src/` is modified.**
5. Lint, tests and build stay at their recorded baselines.

## Checks Run

- [x] `npm run typecheck` — **exit 0**
- [x] `--showConfig` — `strict`, `strictNullChecks`, `noImplicitAny`,
      `noFallthroughCasesInSwitch` all **true**
- [x] `npm run lint` — **0 errors, 10 warnings**, the same 10 as baseline
- [x] `npx vitest run` — **4 files, 74 tests, all pass**
- [x] `npm run build` — **pass**
- [x] `git status` — `tsconfig.app.json`, `package.json`, and `web/src/lib/ai.ts` (see the
      recorded deviation)

## Progress Tracker Update Required

Record the measurement, the flags enabled, and the no-CI finding.

## Decision Log Update Required

Yes — enabling strict, and the deliberate choice to leave the unused-variable checks off.
