# AGENTS.md

Instructions for any AI coding agent working in this repository (Claude Code, Codex,
Cursor, Copilot, Gemini, or anything else).

**`CLAUDE.md` is the canonical entry point. Read it first.** This file exists so agents that
do not look for `CLAUDE.md` still land on the same rules.

---

## What This Project Uses

A spec-driven **Project Brain**. The Markdown files in `context/` are the source of truth.
Code changes are made against a written unit spec in `context/feature-specs/`, one unit at
a time.

---

## Prime Directive

Stay inside the current unit of work. Do not invent features, redesign the whole system, or
expand scope unless instructed.

---

## Context Files

- `context/project-overview.md` — what the project is and what success means
- `context/strategy-context.md` — positioning, users, market, adoption, and leverage
- `context/architecture.md` — technical/system/workflow architecture
- `context/execution-standards.md` — coding, writing, operational, and quality standards
- `context/ai-workflow-rules.md` — how AI should work inside this project
- `context/ui-brand-context.md` — visual design, brand, tone, and content style
- `context/progress-tracker.md` — current status and next work
- `context/decision-log.md` — important decisions and reasoning
- `context/feature-specs/` — one scoped unit of work per file

---

## Current State

This is an **existing codebase** being brought under the Project Brain. The context files
are still unfilled templates.

**Do not implement product features or refactor application code until the brain is
filled.** Check `context/progress-tracker.md` for the current phase.

---

## Read Order

1. `CLAUDE.md`
2. `context/project-overview.md`
3. `context/strategy-context.md`
4. `context/architecture.md`
5. `context/execution-standards.md`
6. `context/ai-workflow-rules.md`
7. `context/ui-brand-context.md`
8. `context/progress-tracker.md`
9. `context/decision-log.md`
10. The relevant `context/feature-specs/*.md`

---

## Core Rules

- One unit of work at a time. No bundling unrelated changes.
- Never invent product behavior that is not in the context files or the current spec.
- Never violate an invariant listed in `context/architecture.md`.
- Never touch anything the spec marks out of scope.
- Log ambiguity as an Open Question in `context/progress-tracker.md` rather than guessing.
- Record meaningful decisions in `context/decision-log.md`.
- Update the matching context file whenever architecture, storage, conventions, scope, or
  brand direction changes.
- Never read, print, or commit secrets. Treat `.env*` as write-only-by-human.
- Report check results honestly, including failures.

---

## Protected Paths

`node_modules/`, vendored code, lockfiles (unless the unit is a dependency change), `.env*`,
generated clients and types, applied database migrations, and anything marked protected in
`context/architecture.md`.

---

## Definition of Done

A unit is done when it works end to end within its scope, breaks no invariant, passes the
checks named in its spec, and has its results written into `context/progress-tracker.md`.
