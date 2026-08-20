---
name: brain-bootstrap
description: Load the whole Project Brain and prove comprehension before any implementation begins. Use at the start of a session on a filled brain, when the user says "read the context files", "load the project brain", "get up to speed", or "what do you understand about this project". Read-only — it never implements. For returning to a project after a break with a focus on status and next actions, use brain-resume instead.
---

# Bootstrap the Project Brain

Load the project into working memory and demonstrate that you actually understood it, before
touching anything.

## Rules

- **Implement nothing.** This is a comprehension pass. No edits, no fixes, no "small
  improvements while I'm here."
- **Read every file.** Do not skim, and do not rely on what a filename implies.
- **Report gaps honestly.** If a file is still an unfilled template, say so — do not
  reconstruct its contents from the code and present that as the brain.

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
10. Everything in `context/feature-specs/`

## Then Report

1. **Understanding** — what this project is, who it is for, and what it does. In your own
   words; do not paste the overview back.
2. **Current phase and goal** — from the progress tracker.
3. **Invariants** — the rules you must never violate, quoted exactly.
4. **Protected paths** — what you may not modify.
5. **Open questions** — everything unresolved, including any `[UNKNOWN]` markers still in the
   context files.
6. **Next recommended unit** — with a one-line reason.
7. **Risks** — anything you see that could go wrong before implementation begins.
8. **Contradictions** — any place where two context files disagree, or where a context file
   appears to contradict the code. This is the most valuable thing this pass produces; look
   for it deliberately rather than reporting "none found" by default.

## If the Brain Is Unfilled

Stop and say so. Point the user to `/brain-onboard`. Do not begin feature work against
placeholder context — an unfilled brain will make you invent behavior that does not exist.
