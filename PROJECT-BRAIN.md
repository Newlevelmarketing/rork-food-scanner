# Project Brain

A spec-driven system for building software with AI without losing the plot.

This file explains the system. It is deliberately **not** named `README.md` so it will not
collide with the application repo's own README.

---

## The Idea

AI agents forget. Chats end, context windows fill, and the reasoning behind decisions
evaporates. The Project Brain keeps that reasoning in version-controlled Markdown, so any
agent — or any human — can pick the project up cold and be productive in one read.

Three roles, clearly separated:

- **Human** — decisions, taste, priorities. The only source of authority.
- **ChatGPT** — architect and spec writer. Turns intent into precise, small unit specs.
- **Claude Code** — implementer. Executes one spec at a time against the brain.

The `context/` folder is the memory that connects them.

---

## Layout

```
CLAUDE.md                       Entry point and hard rules for Claude Code
AGENTS.md                       Same rules, for any other AI agent
PROJECT-BRAIN.md                This file

context/
  project-overview.md           What this is, who it's for, scope, success criteria
  strategy-context.md           Why it should exist, positioning, adoption, metrics
  architecture.md               Stack, boundaries, data model, flows, invariants
  execution-standards.md        How code, content, and design must be written
  ai-workflow-rules.md          Scoping rules, splitting rules, protected areas
  ui-brand-context.md           Voice, tone, colors, typography, content style
  progress-tracker.md           Current phase, what's done, what's next, blockers
  decision-log.md               Decisions with context, reasoning, reversal conditions
  feature-specs/
    00-unit-spec-template.md    Template for every unit of work

prompts/                        Copy-paste prompts for each stage of the workflow
examples/                       A filled-in example showing the level of specificity

.claude/skills/                 The workflow as invocable Claude Code skills
```

---

## The Loop

1. **Onboard** — fill the brain from the existing codebase (`/brain-onboard`).
2. **Spec** — write one small unit spec (`/brain-spec`, or prompt `02` in ChatGPT).
3. **Build** — implement exactly that spec (`/brain-build`).
4. **Review** — check the work against the brain (`/brain-review`).
5. **Sync** — update the tracker, decision log, and any context file that changed
   (`/brain-sync`).
6. Repeat.

Returning after a break? `/brain-resume`.

---

## How to Use It From ChatGPT

The skills above run the loop inside Claude Code. The original workflow runs the same loop
with ChatGPT as the author, and both are supported.

1. **Start in ChatGPT.** Paste `prompts/01-chatgpt-fill-project-brain.md` along with your
   rough project idea. For an existing app, use `prompts/07-onboard-existing-repo.md` instead.
2. **Save the filled files** into `context/`.
3. **Open the project in Claude Code.** It reads `CLAUDE.md` first, which sends it to the
   context files before any code is written.
4. **Build one unit at a time.** Ask ChatGPT for a spec with
   `prompts/02-chatgpt-create-feature-spec.md`, save it as
   `context/feature-specs/01-[unit-name].md`, then hand it to Claude Code with
   `prompts/04-claude-code-execute-unit.md`.
5. **Keep project memory updated.** After every meaningful change, update
   `context/progress-tracker.md`, `context/decision-log.md`, and any context file the change
   affected.

---

## Project Types This Works For

SaaS apps, AI tools, hackathon projects, Web3 products, civic and smart-city projects,
content systems, community projects, local businesses, internal workflows, proposals, and
campaigns.

For software projects, `architecture.md` means technical architecture. For non-software
projects, it means operating structure, stakeholder structure, or workflow architecture.

---

## Rules That Make It Work

- **One unit at a time.** If a change cannot be reviewed end to end quickly, it is too big.
- **Never invent behavior.** Unspecified is out of scope. Ambiguity becomes an Open
  Question, not a guess.
- **Invariants are absolute.** `context/architecture.md` lists rules the project must never
  violate. They are not negotiable by an agent.
- **Docs move with code.** A change that alters architecture, storage, conventions, scope,
  or brand updates the matching context file in the same session.
- **Decisions are recorded, with reversal conditions.** A decision you cannot explain in six
  months is a decision you will relitigate.

---

## Working With an Existing Codebase

The brain was designed for greenfield projects. Using it on an app that already exists
changes one thing fundamentally: **the code is the ground truth.**

- `architecture.md`, `execution-standards.md`, and `ui-brand-context.md` are written from
  evidence in the repo — real file paths, real config, real conventions — not from
  preference.
- `project-overview.md` and `strategy-context.md` are written from the human. Code cannot
  tell you what a project is *for*.
- Anything that cannot be established from either source is marked
  `[UNKNOWN — needs human input]`. It is never filled with a plausible guess. A confidently
  wrong brain is worse than an empty one.

`/brain-onboard` runs this process.

---

## Prompts

The `prompts/` folder holds the copy-paste prompt for each stage:

| File | Where | Purpose |
| --- | --- | --- |
| `01-chatgpt-fill-project-brain.md` | ChatGPT | Generate the brain for a new project |
| `02-chatgpt-create-unit-spec.md` | ChatGPT | Write the next unit spec |
| `03-claude-code-bootstrap.md` | Claude Code | Load the brain, prove comprehension |
| `04-claude-code-execute-unit.md` | Claude Code | Implement one unit spec |
| `05-resume-project.md` | Either | Resume after a break |
| `06-review-output.md` | Either | Review work against the brain |
| `07-onboard-existing-repo.md` | Claude Code → ChatGPT | Fill the brain from existing code |

The `.claude/skills/` folder makes the same workflows invocable directly as `/brain-*`.
