# Prompt 04 — Claude Code Execute One Unit

Use this in Claude Code when you already have a unit spec saved in `context/feature-specs/`.

> `/brain-build` runs this same routine.

```txt
Read `CLAUDE.md` first.

Then read all context files in the required order.

Then read this unit spec:

`context/feature-specs/[FILE-NAME].md`

Before implementing:
1. Summarize the unit goal.
2. Mark this unit as In Progress in `context/progress-tracker.md`.
3. Identify the files or areas you expect to modify.

Then implement exactly what is specified.

Rules:
- Do not go beyond the unit scope.
- Do not touch out-of-scope areas.
- Do not invent missing behavior.
- If a requirement is ambiguous, add it as an open question in `context/progress-tracker.md` and stop before making risky changes.
- Follow `context/architecture.md`, `context/execution-standards.md`, and `context/ui-brand-context.md`.

After implementation:
1. Run the checks listed in the spec.
2. Update `context/progress-tracker.md`.
3. Update `context/decision-log.md` if a meaningful decision was made.
4. Tell me what changed, what checks passed, and what should be done next.
```
