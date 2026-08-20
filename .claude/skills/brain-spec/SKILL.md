---
name: brain-spec
description: Write the next unit spec into `context/feature-specs/` using the Project Brain template. Use when the user says "write a spec", "create the next unit", "spec out this feature", or describes a feature they want built before any code is written. Enforces small, verifiable scope and splits work that is too broad into an ordered sequence. Writes the spec only — it never implements it.
---

# Write a Unit Spec

Turn a request into one small, verifiable unit of work that another agent could execute
without asking a single follow-up question.

## Rules

- **Write the spec. Do not implement it.** Implementation is `/brain-build`.
- **One unit.** If the request spans unrelated boundaries, split it and propose an order.
- **No invented behavior.** Everything in the spec traces to the context files or to
  something the user said. If a detail is missing and matters, ask.
- **Every acceptance criterion must be checkable.** If you cannot describe how to verify it in
  one sentence, rewrite it.

## Procedure

### 1. Load context

Read `CLAUDE.md`, all `context/*.md`, and the existing `context/feature-specs/` so the new
spec fits the numbering and does not overlap a unit already written.

If the brain is still unfilled templates, stop and point the user to `/brain-onboard`. A spec
written against placeholder context will invent product behavior.

### 2. Test the scope

Split the work if it combines any of these:

- UI changes and backend changes that could ship separately
- Schema changes and unrelated design changes
- Strategy changes and implementation changes
- Multiple unrelated routes or modules
- Content writing and automation setup
- Anything that cannot be reviewed end to end quickly

The real test: **if a reviewer cannot hold the whole change in their head, the unit is too
big.** When you split, say so explicitly and give the recommended order with one line of
reasoning each — then write only the first spec unless asked for more.

### 3. Ask only what blocks you

Ask the questions that would change what gets built. Offer your best inference as a default so
the user can confirm rather than compose. Do not interrogate.

### 4. Write the spec

Copy `context/feature-specs/00-unit-spec-template.md` to
`context/feature-specs/NN-short-name.md` — next number, kebab-case name — and fill every
section. Status: `Ready`. Owner: `Claude Code`.

Give particular care to:

- **Out of Scope** — name the specific files, folders, and features that must not be touched.
  Vague out-of-scope sections are where scope creep enters.
- **Files to Create or Modify** — real paths from this repo, verified to exist.
- **Error / Empty / Edge States** — the section most often skipped and most often the source
  of rework.
- **Checks to Run** — the repo's real commands, with the current baseline noted if a check
  already fails.
- **Acceptance Criteria** — verifiable conditions, not intentions.

### 5. Update the tracker

Add the new unit to Next Up in `context/progress-tracker.md`. Leave its status as `Ready` —
it is not In Progress until `/brain-build` starts it.

### 6. Report

State the file you created, the unit's goal in one sentence, what you deliberately excluded,
and any assumption you made that the user should correct now rather than after it is built.
