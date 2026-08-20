---
name: brain-onboard
description: Fill the Project Brain context files from an existing codebase. Use when the repo is present but `context/` is still unfilled templates, or when the user says "onboard the repo", "fill the brain", "set up the context files from the code", or drops an existing project into a workspace that already has the brain scaffolding. Gathers evidence from real source files first, writes only what the evidence supports, then asks the human for the product and strategy answers code cannot provide.
---

# Onboard an Existing Codebase Into the Project Brain

Turn an existing application into a filled Project Brain, without inventing anything.

## The One Rule

**Code is ground truth for what exists. The human is ground truth for what it is for.
Anything established by neither is `[UNKNOWN — needs human input]`.**

A confidently wrong brain is worse than an empty one, because every future unit of work will
be built on top of it. Never write a plausible-sounding architecture claim you did not verify
in a file.

## Constraints for the Whole Run

- **Do not modify application code.** This skill writes only to `context/*.md`.
- **Never surface secrets.** Read `.env*` for variable *names* only. Never print, copy, log,
  or write a value into any context file.
- **Cite evidence.** Non-obvious claims in `architecture.md` and `execution-standards.md`
  name the file that proves them.
- **Describe, do not prescribe.** No rewrites, migrations, or redesigns. Those are units of
  work with their own specs.

---

## Phase 0 — Preflight

1. Confirm the application code is actually present. If the workspace only contains the
   brain, stop and tell the user to add the repo.
2. Locate the app root. It may be the workspace root or a subfolder. If there are several
   candidate projects (a monorepo), ask which one the brain covers before proceeding.
3. Check whether any `context/*.md` file has already been filled — the STATUS banner is
   absent, or real content replaced the placeholders. If so, **stop and ask** whether to
   update in place or overwrite. Never silently discard filled context.
4. Check whether the incoming repo brought its own `CLAUDE.md`, `AGENTS.md`, `README.md`, or
   `context/` folder. Report collisions and ask how to merge before touching either copy.

## Phase 1 — Evidence Sweep

Read, do not assume. Work through these and record what you find with file paths.

**Stack** — package manifests, lockfiles, runtime version files, framework config, build
config, `tsconfig`, compiler options. Note versions.

**Structure** — top-level directory map and the responsibility of each folder. Entry points
for dev and production. How the app boots.

**Surfaces** — routes, pages, screens, CLI commands. API endpoints with methods, and which
ones mutate state.

**Data** — database and ORM, schema location, main tables and relationships, migration
directory and which migrations are applied, blob storage, caches, queues.

**External integrations** — third-party SDKs and services. Environment variable *names* from
`.env.example` or from code references. Never values.

**Auth** — how a user authenticates, where session state lives, how ownership is enforced on
mutations.

**Conventions in use** — naming, file layout, error handling, state management, data
fetching, testing patterns. Linter, formatter, and type settings that constrain future code.
Record what the code *does*, not what it *should* do.

**Design system** — theme file, design tokens, real color values, fonts, component library,
icon set, spacing scale.

**Checks** — every runnable script. Then actually run build, typecheck, lint, and test, and
record the **real** result. A check that already fails is a critical finding: it becomes the
baseline, so no future unit gets blamed for it.

**Risk surface** — fragile areas, dead code, duplicated logic, TODO/FIXME clusters, anything
that will bite whoever changes it.

**History** — if git is available, recent commit subjects and the most-churned files. This
reveals where work is actually happening.

## Phase 2 — Write What the Code Proves

Fill these three files now, removing their STATUS banners as you complete them:

- `context/architecture.md` — stack, boundaries, data model, integrations, critical flows,
  protected paths, Known Debt. Invariants go here, but only ones you can *verify the code
  currently upholds*. An invariant must be checkable against a diff: "no database query may
  run inside a React render path" is an invariant; "should be performant" is not. Propose
  invariants you are unsure about as Open Architecture Questions instead.
- `context/execution-standards.md` — the conventions actually present, plus the real commands
  table with baseline pass/fail noted.
- `context/ui-brand-context.md` — real tokens, real fonts, real component library. Voice
  sections stay unfilled until Phase 3.

## Phase 3 — Ask the Human

Now stop and ask. Keep it to the questions that genuinely block the remaining files —
roughly 6 to 10, grouped, with your best inference offered as a default so the user can
confirm rather than compose. Cover:

- What the product is and who it is for
- What it is deliberately *not* trying to do
- What counts as success
- What must never break — candidate invariants, framed from what you observed
- What the immediate goal is: new feature, bug backlog, refactor, or redesign
- Anything from Phase 1 that the code left genuinely ambiguous

Do not proceed past this phase without answers. Product intent is not inferable, and guessing
it poisons every downstream spec.

## Phase 4 — Write What the Human Proves

Fill `context/project-overview.md` and `context/strategy-context.md` from the answers, and
complete the voice sections of `context/ui-brand-context.md`. Remove their STATUS banners.
Leave `[UNKNOWN — needs human input]` wherever an answer was not given.

## Phase 5 — Seed the Working Files

- `context/decision-log.md` — decisions clearly visible in the code (framework choice, data
  layer, auth approach, hosting). Record the reasoning as **inferred** where you are
  reconstructing it, and say so in the entry.
- `context/progress-tracker.md` — current phase, what exists today, open questions,
  blockers, baseline check results, and session notes written for an agent with no memory.

## Phase 6 — Propose the First Three Units

Recommend three unit specs in order, each with a one-line reason and a rough size. Prefer a
small, low-risk first unit that exercises the whole loop — spec, build, check, sync — over an
ambitious one. Then stop. Do not write the specs unless asked; that is `/brain-spec`.

---

## Report Back

End with:

1. Which context files are now filled, and which remain partly `[UNKNOWN]`
2. The baseline check results, stated plainly including failures
3. The invariants you propose, for the human to confirm or reject
4. The top risks you found in the codebase
5. The three recommended units
