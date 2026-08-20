---
name: brain-resume
description: Resume work on a Project Brain project after a break. Use when the user says "where were we", "resume the project", "pick up where we left off", "what's next", or returns to a project after time away. Reports status, what is done, what is in progress, blockers, the next three units, and the safest next prompt. Read-only — it never implements.
---

# Resume the Project

Reconstruct the state of the project and hand back a safe next move.

## Rules

- **Implement nothing** unless the user explicitly asks afterwards.
- **Trust the code over the tracker.** The progress tracker is a claim; the repo is a fact. If
  they disagree, say so — a stale tracker is a finding, not a detail.
- **Do not re-plan the project.** Resume means continue, not redesign.

## Read

1. `CLAUDE.md` or `AGENTS.md`
2. `context/project-overview.md`
3. `context/strategy-context.md`
4. `context/architecture.md`
5. `context/execution-standards.md`
6. `context/ai-workflow-rules.md`
7. `context/ui-brand-context.md`
8. `context/progress-tracker.md`
9. `context/decision-log.md`
10. Every file in `context/feature-specs/`, noting each one's Status field

Then check reality: uncommitted changes, recent commits, the current branch, and whether any
spec marked In Progress has matching work in the tree.

## Report

1. **Current status** — phase, goal, and how much of it is real versus claimed.
2. **Completed** — what is genuinely done, per the tracker *and* the repo.
3. **In progress** — including any unit left marked In Progress with no work to show for it,
   or work in the tree with no spec.
4. **Open questions and blockers** — everything that would stall the next unit.
5. **Drift** — where the tracker, the context files, and the code disagree. Say it plainly.
6. **Next 3 units** — in order, each with a one-line reason and rough size.
7. **The safest next prompt** — the exact prompt to send next, written out ready to copy.

Prefer the smallest next unit that unblocks the most. If the project was left mid-unit, the
safest next move is almost always to finish or explicitly abandon that unit, not to start a
new one.
