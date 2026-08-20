---
name: brain-sync
description: Detect and repair drift between the Project Brain context files and the actual codebase. Use when the user says "update the docs", "sync the context files", "the brain is out of date", after work was done outside the spec workflow, or when a context file appears to contradict the code. Updates `context/*.md` to match reality — it never changes application code to match the docs.
---

# Sync the Brain to Reality

The brain is only useful while it is true. This finds where it stopped being true and fixes
the documents.

## The Direction of Truth

**Code is ground truth for what exists. Docs get corrected to match it — never the reverse.**

If the code violates something the brain says it must do, that is not a doc bug. That is a
finding: report it, and let the human decide whether to fix the code or amend the rule. Do not
quietly rewrite an invariant to match code that breaks it.

## Load

`CLAUDE.md`, every `context/*.md`, every file in `context/feature-specs/`, and the current
state of the repo — recent commits, uncommitted changes, and the areas each context file
claims to describe.

## Check for Drift

**`architecture.md`** — Do the stack, versions, and dependencies still match the manifest? Do
the listed boundaries match the real folder structure? Do the data model and integrations
match the schema and the environment variable names in use? Are the critical flows still
accurate? Are the protected paths still the right ones?

**`execution-standards.md`** — Do the documented conventions match what the code now does? Are
the commands still correct, and does the baseline pass/fail note still hold? Run them.

**`ui-brand-context.md`** — Do the tokens, colors, fonts, and component library match the
theme files?

**`project-overview.md`** — Does the Already Built list match what actually ships? Has anything
listed as out of scope quietly been built?

**`progress-tracker.md`** — Does Completed match the repo? Is anything still marked In Progress
with nothing to show? Is there work in the tree with no spec? Are resolved open questions still
listed, or new ones unlisted?

**`decision-log.md`** — Were decisions made recently that were never recorded? Has any recorded
decision's reversal condition actually been met?

**`feature-specs/`** — Does each spec's Status match reality?

## Repair

Update the context files to match the code. For each edit, state what changed and what
evidence drove it.

Where a decision was clearly made but never logged, add it to `context/decision-log.md` and
label any reconstructed reasoning as **inferred**.

Where the code and an invariant genuinely conflict, do **not** edit either. Report it as a
required decision for the human.

## Report

1. **Drift found** — file by file, with evidence
2. **Corrections made** — what you changed in the docs
3. **Conflicts requiring a decision** — code versus invariant, or contradictions between
   context files that only the human can settle
4. **Undocumented decisions** — added to the log, flagged for confirmation
5. **Still accurate** — one line, so the user knows what you verified rather than skipped
