# Prompt 01 — ChatGPT Fill Project Brain

Use this in ChatGPT before building any project.

> For an **existing** codebase, use `07-onboard-existing-repo.md` instead — it gathers
> evidence from the real code first.

```txt
I want you to act as my senior prompt engineer, product architect, strategy partner, and project systems designer.

My workflow is:
- ChatGPT = prompt engineer, architect, strategist, and spec writer
- Claude Code = coder / builder / implementation agent
- Markdown context files = project memory

I am starting a new project and I want to use a spec-driven Project Brain system.

Here is my rough project idea:

[PASTE PROJECT IDEA HERE]

Your job:
1. First, study the idea and identify what is clear, what is weak, what is missing, and what decisions need to be made.
2. Ask only the most important clarifying questions if the project cannot be defined without them.
3. If enough information is available, make reasonable assumptions and label them clearly.
4. Then fill the following Markdown files with project-specific content.

Files to generate:

1. `context/project-overview.md`
2. `context/strategy-context.md`
3. `context/architecture.md`
4. `context/execution-standards.md`
5. `context/ai-workflow-rules.md`
6. `context/ui-brand-context.md`
7. `context/progress-tracker.md`
8. `context/decision-log.md`
9. `CLAUDE.md`
10. `AGENTS.md`

Important rules:
- Do not write generic startup filler.
- Make everything specific to this project.
- Keep version 1 realistic and small.
- Separate in-scope from out-of-scope clearly.
- Identify invariants Claude Code must never violate.
- Add unresolved items to Open Questions instead of inventing certainty.
- Optimize the files so Claude Code can build the project one unit at a time.

After generating the files, recommend the first 3 feature/unit specs I should create next.
```
