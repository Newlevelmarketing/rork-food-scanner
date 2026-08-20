# CLAUDE.md — Project Brain Entry Point

This project runs on a spec-driven **Project Brain** workflow. The Markdown files in
`context/` are the source of truth for what gets built, why it matters, how it must be
built, and where the project currently stands.

Read this file first, every session.

---

## Project Mode

- **Mode:** Existing codebase being brought under the Project Brain.
- The application was built before this system existed. The code is the ground truth about
  *what is*; the brain must be written to match it, not the other way around.
- **The brain is not filled yet.** Every file in `context/` is still a template.

> **Hard gate:** while the brain is unfilled, do not implement product features and do not
> refactor application code. Run `/brain-onboard` first. Check
> `context/progress-tracker.md` for the current phase before doing anything else.

---

## Roles

| Role | Owns |
| --- | --- |
| **Human** | Final decisions, taste, judgment, priorities |
| **ChatGPT** | Prompt engineering, product architecture, strategy, spec writing, review |
| **Claude Code** | Implementation, file edits, test runs, evidence gathering |
| **`context/` files** | Project memory and rules |

Claude Code implements against the context files and the current unit spec. It does not
infer, invent, or expand behavior without permission.

---

## Required Read Order

Read these in order at the start of any real work:

1. `CLAUDE.md` (this file)
2. `context/project-overview.md`
3. `context/strategy-context.md`
4. `context/architecture.md`
5. `context/execution-standards.md`
6. `context/ai-workflow-rules.md`
7. `context/ui-brand-context.md`
8. `context/progress-tracker.md`
9. `context/decision-log.md`
10. The relevant spec in `context/feature-specs/`

---

## Core Rule

> **Do not build from vibes. Build from specs.**

If a requirement is not defined, do not invent it. Add it as an open question in
`context/progress-tracker.md` or ask for clarification.

---

## Non-Negotiable Rules

1. **One unit at a time.** One feature, section, asset, or subsystem per implementation step.
2. **Never invent behavior.** If it is not in the context files or the unit spec, it is not
   in scope. Ambiguity goes to Open Questions in `context/progress-tracker.md`.
3. **Never violate an invariant** listed in `context/architecture.md`.
4. **Stay inside the spec's scope.** Out-of-scope areas are not touched, not "improved,"
   not tidied.
5. **Docs follow code.** Any change to architecture, storage, conventions, scope, or brand
   updates the matching context file in the same session.
6. **Decisions get recorded.** Structural, product, strategy, or design decisions go in
   `context/decision-log.md` after the human approves.
7. **Stop instead of guessing.** If a requirement is ambiguous and the change is risky,
   log the question and stop.

---

## Protected — Do Not Modify Without Explicit Instruction

- Third-party library internals, `node_modules/`, vendored code
- Lockfiles, unless the unit is explicitly a dependency change
- `.env*` and any file containing secrets — never read values into output, never commit them
- Generated clients, generated types, generated UI library files
- Database migrations that have already been applied
- Any path marked protected in `context/architecture.md`

---

## Skills

Invoke these by name. They encode the workflow above.

| Skill | Use when |
| --- | --- |
| `/brain-onboard` | The repo is in place and the brain needs to be filled from real code. **Start here.** |
| `/brain-bootstrap` | The brain is filled and you need a comprehension check before building. |
| `/brain-resume` | Returning after a break; need status, blockers, and the next 3 units. |
| `/brain-spec` | Writing the next unit spec into `context/feature-specs/`. |
| `/brain-build` | Implementing one existing unit spec end to end. |
| `/brain-review` | Reviewing work against the brain for scope creep and violations. |
| `/brain-sync` | Code and docs have drifted; bring the context files back in line. |

---

## Before Moving to the Next Unit

1. The current unit works end to end within its defined scope.
2. No invariant in `context/architecture.md` was violated.
3. `context/progress-tracker.md` reflects the completed work.
4. `context/decision-log.md` includes any new decisions.
5. The checks listed in the unit spec pass, and the real output is reported — including
   failures.

---

## Output Format After Implementation

When done, respond with:

- What was implemented
- Files changed
- Checks run, with real results including failures
- Any issues found
- Any open questions
- What should be built next

---

## Prompt Discipline

Bad:

> Build the app.

Good:

> Read `CLAUDE.md`, all context files, and `context/feature-specs/01-auth.md`. Mark the unit
> in progress. Implement only the authentication flow described in the spec. Do not touch
> billing, dashboard, or AI generation. Run checks and update the progress tracker.
