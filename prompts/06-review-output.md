# Prompt 06 — Review Output Against Spec

Use this after Claude Code implements something.

> `/brain-review` runs an expanded version of this inside Claude Code, with the context files
> and the real diff already loaded.

```txt
Act as my senior reviewer and prompt engineer.

Review the latest output against the current unit spec and project context.

I will provide:
1. The unit spec
2. Claude Code's summary
3. The changed files or diff, if available
4. Any errors or issues

Review for:
- Scope creep
- Missing requirements
- Violated architecture rules
- Broken invariants
- Inconsistent naming or file organization
- UI/brand mismatch
- Unnecessary complexity
- Risky assumptions
- Missing progress tracker or decision log updates

Then give me:
1. Pass/fail judgment
2. What is correct
3. What needs fixing
4. A focused corrective prompt I can send to Claude Code
5. Any context files that should be updated

Do not rewrite the whole project. Keep the correction focused.
```
