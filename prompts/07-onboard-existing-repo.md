# Prompt 07 — Onboard an Existing Repo Into the Brain

Use this when the application already exists and the brain is empty.

Two stages: Claude Code gathers the evidence, then ChatGPT writes the brain from it.
`/brain-onboard` does both stages in one pass inside Claude Code — use these prompts if you
prefer ChatGPT to author the files.

---

## Stage 1 — Claude Code: produce the evidence report

```txt
Read `CLAUDE.md`, then survey this repository. Do not modify any file. Do not implement anything.

Produce an evidence report with these sections. Every claim must cite the file that proves it.
Where the code does not answer a question, write `[UNKNOWN]` — do not infer.

1. Stack
   - Language, runtime, framework, versions. Cite the manifest and lockfile.
   - Notable dependencies and what each is used for.

2. Structure
   - Top-level directory map with the responsibility of each folder.
   - Entry points: how the app starts in dev and in production.

3. Surfaces
   - Routes, pages, screens, or commands the app exposes.
   - Public API endpoints, their methods, and what they mutate.

4. Data
   - Database, ORM, schema location, main tables/collections and their relationships.
   - Migrations: where they live and which are applied.
   - File/blob storage, caches, queues.

5. External integrations
   - Third-party services. List required environment variable NAMES only.
   - Never print, copy, or summarize a secret value.

6. Auth and permissions
   - How a user is authenticated, where session state lives, how ownership is enforced.

7. Conventions actually in use
   - Naming, file layout, error handling, state management, testing patterns.
   - Formatter/linter/tsconfig settings that constrain future code.

8. Design system
   - Theme file, design tokens, color values, fonts, component library, icon set.

9. Checks
   - Every runnable script, and what it does.
   - Which ones currently pass on an untouched checkout. Run them and report real output.

10. Risk surface
    - Fragile areas, dead code, duplicated logic, TODO/FIXME clusters, anything that will
      bite someone who changes it. Cite paths.

Finish with the 10 questions about product intent, users, priorities, or strategy that the
code cannot answer and that a human must.
```

---

## Stage 2 — ChatGPT: write the brain from the evidence

```txt
Act as my product architect and project systems designer.

I have an existing application. Here is a factual evidence report generated from its source code:

[PASTE THE EVIDENCE REPORT]

Here are my answers about intent, users, and priorities:

[ANSWER THE QUESTIONS FROM STAGE 1]

Write these files, specific to this project:

1. `context/architecture.md`   — from the evidence only
2. `context/execution-standards.md` — from the conventions already in the code
3. `context/ui-brand-context.md` — from the theme and existing copy
4. `context/project-overview.md` — from my answers
5. `context/strategy-context.md` — from my answers
6. `context/progress-tracker.md` — current state and the next three units
7. `context/decision-log.md`    — decisions visible in the code, with your inferred reasoning clearly labelled as inferred

Rules:
- The code is ground truth for what exists. I am ground truth for what it is for.
- Never state something the evidence report does not support. Write `[UNKNOWN — needs human input]` instead.
- Do not propose a rewrite, a migration, or a redesign. Describe what is.
- Invariants must be checkable against a diff. "Should be performant" is not an invariant;
  "no database query may run inside a React render path" is.
- Record existing problems as Known Debt in `architecture.md`. Do not schedule fixes for them here.

Then recommend the first 3 unit specs, in order, with a one-line reason each.
```
