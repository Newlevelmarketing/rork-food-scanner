---
name: brain-build
description: Implement exactly one unit spec from `context/feature-specs/` end to end, with the Project Brain guardrails. Use when the user says "build this unit", "implement the spec", "execute unit NN", or names a spec file to be built. Loads the full brain, marks the unit in progress, implements only what the spec defines, runs the spec's checks, and updates the tracker and decision log. Refuses to work without a spec.
---

# Build One Unit

Implement one written spec. Nothing more.

## Rules

These are not suggestions. They are why the system works.

1. **No spec, no build.** If the user asks for work with no spec, offer `/brain-spec` first.
2. **Stay in scope.** Out-of-scope areas are not touched, not improved, not tidied, not
   reformatted.
3. **Invent nothing.** Behavior not in the spec or the context files does not get built.
4. **Never break an invariant** from `context/architecture.md` — not even temporarily to make
   a check pass.
5. **Ambiguity stops you.** Log it as an Open Question in `context/progress-tracker.md` and
   ask, before making a risky change.
6. **Report checks honestly.** If a check fails, say so and show the output. Never claim a
   pass you did not observe.

## Procedure

### Before implementing

1. Read `CLAUDE.md` and every `context/*.md` in the required order.
2. Read the unit spec.
3. Summarize the unit goal in one sentence and confirm it matches what the user expects.
4. Mark the unit **In Progress** in `context/progress-tracker.md`.
5. List the files you expect to create or modify. If that list extends past the spec's Files
   section, the spec is wrong — stop and fix the spec first, do not widen the build.
6. Establish the baseline: run the spec's checks *before* changing anything, so a
   pre-existing failure is not later attributed to this unit.

### While implementing

- Match the surrounding code — naming, layout, error handling, comment density. Consistency
  with what exists beats a better style imported from elsewhere.
- Follow `context/execution-standards.md` for code, `context/ui-brand-context.md` for any
  user-facing copy or UI.
- Handle the error, empty, and edge states the spec names. They are requirements, not polish.
- Do not upgrade dependencies, rename unrelated symbols, or reformat untouched files.
- If you find a real problem outside this unit, note it for Known Debt in
  `context/architecture.md`. Do not fix it here.

### After implementing

1. Run every check listed in the spec. Report real output, including failures.
2. Verify each acceptance criterion one by one, and say how you verified it. A criterion you
   could not verify is not met.
3. Update `context/progress-tracker.md`: completed work, files changed, checks run with real
   results, open questions, next recommended unit.
4. Update `context/decision-log.md` if any structural, product, strategy, or design decision
   was made — with its reversal condition.
5. Update `context/architecture.md`, `context/execution-standards.md`, or
   `context/ui-brand-context.md` if this unit changed anything they describe.
6. Set the spec's Status to `Complete`.

### Report

- What changed, file by file
- Which checks ran and what they actually returned
- Which acceptance criteria are met, and how each was verified
- Anything you deliberately did not do, and why
- The recommended next unit

## Stop Conditions

Stop and ask rather than proceeding if:

- The spec conflicts with an invariant or with another context file
- Implementing it correctly requires touching an out-of-scope area
- A required detail is missing and any assumption would be a guess about product behavior
- A check was already failing before you started and the spec does not account for it
