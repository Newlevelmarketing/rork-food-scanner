# AI Workflow Rules

> **STATUS: ACTIVE.** This file is complete as written and applies immediately. It is not a
> fill-in template.

## Roles

- **Human**: final decision-maker, taste, judgment, priorities
- **ChatGPT**: prompt engineer, product architect, strategist, spec writer, reviewer
- **Claude Code**: implementation agent, coder, file editor, test runner
- **Context files**: project memory and rules

## Approach

Build this project incrementally using a spec-driven workflow. Context files define what to
build, why it matters, how to build it, and where the project currently stands.

AI must implement against the context and current unit spec. It must not infer, invent, or
expand behavior without permission.

## Scoping Rules

- Work on one feature unit, section, asset, or subsystem at a time.
- Prefer small, verifiable increments over large speculative changes.
- Do not combine unrelated system boundaries in one implementation step.
- Do not redesign the project while implementing a small unit.
- Do not add features just because they seem useful.

## When to Split Work

Split the work if it combines:

- UI changes and backend changes that can be separated
- Database changes and unrelated design changes
- Strategy changes and implementation changes
- Multiple unrelated API routes or modules
- Content writing and automation setup
- Proposal writing and budget modeling
- Anything that cannot be verified quickly

If a change cannot be reviewed end to end quickly, the scope is too broad.

## Handling Missing Requirements

- Do not invent product behavior not defined in the context files.
- If a requirement is ambiguous, mark it as an open question in `progress-tracker.md`.
- If implementation requires a decision, add it to `decision-log.md` after the human approves.
- If a requested change affects architecture, update `architecture.md`.
- If a requested change affects brand or tone, update `ui-brand-context.md`.

## Protected Files / Areas

Do not modify unless explicitly instructed:

- Third-party library internals
- Generated UI library files, unless the project standard allows it
- Environment files containing secrets
- Database migrations that have already been applied in production
- Any folder marked protected in `architecture.md`

## Rules for Working in an Existing Codebase

The brain was designed for greenfield work. On an existing app, these additions apply:

1. **The code is ground truth about what is.** Where the brain and the code disagree about
   current behavior, the code is right and the brain is stale — fix the brain.
2. **The human is ground truth about what should be.** Never infer product intent, strategy,
   or priorities from source files.
3. **Write from evidence.** Claims in `architecture.md` and `execution-standards.md` cite the
   file, config, or dependency that proves them.
4. **Unknown beats plausible.** `[UNKNOWN — needs human input]` is a correct answer. A
   confident guess that reads as fact will be trusted later and cause real damage.
5. **No opportunistic cleanup.** Existing violations of the standards get logged as Known
   Debt in `architecture.md`, not fixed inside an unrelated unit.
6. **Never surface secrets.** Read `.env*` for variable *names* only. Never print, copy, log,
   or commit a value.

## Keeping Docs in Sync

Update the relevant context file whenever implementation changes:

- System architecture or boundaries
- Storage model decisions
- Code conventions or standards
- Feature scope
- Brand voice or UI direction
- Current project state

## Before Moving to the Next Unit

1. The current unit works end to end within its defined scope.
2. No invariant in `architecture.md` was violated.
3. `progress-tracker.md` reflects the completed work.
4. `decision-log.md` includes any new decisions.
5. Required checks pass.

## Prompt Discipline

Bad prompt:

> Build the app.

Good prompt:

> Read `CLAUDE.md`, all context files, and `context/feature-specs/01-auth.md`. Mark the unit
> in progress. Implement only the authentication flow described in the spec. Do not touch
> billing, dashboard, or AI generation. Run checks and update the progress tracker.
