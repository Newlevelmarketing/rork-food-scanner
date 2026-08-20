---
name: brain-review
description: Review completed work against the Project Brain — spec compliance, invariants, standards, correctness, and doc sync. Use when the user says "review this", "check the work", "did that follow the spec", or after a unit is built and before it is accepted. Produces a verdict and an ordered list of required fixes. Reports only; it does not apply fixes unless asked.
---

# Review Against the Brain

Judge the work against what was written, not against what would have been nice.

## Rules

- **Review only.** Do not fix anything unless the user asks afterwards.
- **Cite everything.** File paths and line numbers, not impressions.
- **No padding.** Skip the praise. The user wants the problems.
- **No redesign.** Proposing a better architecture in a unit review is scope creep with a
  clipboard.
- **Do not assume a check passed.** If you cannot verify it, run it; if you cannot run it, say
  it is unverified.

## Load

`CLAUDE.md`, all `context/*.md`, the relevant unit spec, and the actual change — diff,
untracked files, or the files the user named.

## Review Order

**1. Spec compliance**
Does it do everything the spec required? Does it do anything the spec did not ask for? Was any
out-of-scope area touched — including reformatting, renames, and dependency changes?

**2. Invariants**
Does anything violate an invariant in `context/architecture.md`? Quote the invariant and the
line that breaks it. This is the highest-severity category; nothing else outranks it.

**3. Standards**
Does it follow `context/execution-standards.md`? Does it match the conventions of the
surrounding code, or import a foreign style? Was strict typing preserved? Is external input
validated at the boundary? Is auth and ownership enforced before mutations?

**4. Correctness**
Give concrete inputs or state that produce a wrong result or a crash. Check the error, empty,
and edge states the spec named. A finding without a concrete failure scenario is a hunch —
either make it concrete or drop it.

**5. Voice and design**
Does user-facing copy and UI match `context/ui-brand-context.md`? Hardcoded colors instead of
tokens? Fonts or spacing outside the system?

**6. Verification**
Were the spec's checks actually run? Were real results reported? Is any claimed pass
unverified?

**7. Documentation sync**
Was `context/progress-tracker.md` updated accurately — no overclaiming? Was
`context/decision-log.md` updated if a decision was made? Does any context file now contradict
the code?

## Output

- **Verdict:** Accept / Accept with fixes / Reject
- **Required fixes:** numbered, most severe first, each with file, line, and the concrete
  failure it causes
- **Optional improvements:** separate list, explicitly marked out of scope for this unit
- **Unverified claims:** anything you could not confirm, stated as such

If nothing is wrong, say so in one line and stop. An empty findings list is a legitimate
result; manufacturing a finding to look thorough wastes the user's time.
