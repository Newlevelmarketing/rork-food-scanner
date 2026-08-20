# Prompt 03 — Claude Code Bootstrap Project Context

Use this as the first prompt inside Claude Code after adding the Markdown files to your project.

> `/brain-bootstrap` runs this same routine.

```txt
Read `CLAUDE.md` first.

Then read every file inside the `context/` folder in this order:

1. `context/project-overview.md`
2. `context/strategy-context.md`
3. `context/architecture.md`
4. `context/execution-standards.md`
5. `context/ai-workflow-rules.md`
6. `context/ui-brand-context.md`
7. `context/progress-tracker.md`
8. `context/decision-log.md`

Do not implement anything yet.

After reading, give me:
1. Your understanding of the project
2. The current phase and goal
3. The key invariants you must not violate
4. The open questions
5. The next recommended unit of work
6. Any risks you see before implementation begins
```
