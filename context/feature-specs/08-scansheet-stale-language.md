# Unit Spec: Fix the Stale Language Capture in ScanSheet

## Status

- Status: **Complete**
- Owner: Claude Code
- Created: 2026-08-22
- Last Updated: 2026-08-22

## Goal

Add the missing `language.englishName` dependency to `handle` in
`web/src/features/ScanSheet.tsx`, so a scan uses the language the user has actually selected.

## Why This Matters

This was recorded at onboarding as "the one real warning among the ten" — a lint warning. On
reading it, it is a **user-visible bug in a 32-language app**.

`handle` passes `language.englishName` to `analyzeImage` (`ScanSheet.tsx:106`) to tell the model
which language to return item names in. The `useCallback` dependency array omitted it
(`ScanSheet.tsx:116`), so the callback captured whatever language was selected when the sheet
first mounted.

`ScanSheet` is mounted permanently by `pages/Index.tsx:61` with an `open` prop rather than being
conditionally rendered, so the stale closure survives for the life of the session.

**Reproduction:** launch the app, change language in Settings, scan a meal. The model returns item
names in the *previous* language. The interface is translated, the food names are not.

## Scope

### In Scope

- `web/src/features/ScanSheet.tsx` — the dependency array on `handle`

### Out of Scope — not touched

- The other 9 lint warnings. All are `react-refresh/only-export-components`, 7 of them in
  generated `components/ui/**` which must not be hand-edited. They are cosmetic HMR hints, not
  defects.
- `DescribeSheet.tsx`, which takes the same language argument — it did not trigger the rule, so
  its dependencies are already correct. Verified by lint, not assumed.
- The i18n system itself.

## Implementation

One dependency added, with a comment recording the failure it caused so nobody "tidies" it away.

## Acceptance Criteria

1. ~~The `react-hooks/exhaustive-deps` warning for `ScanSheet.tsx:116` is gone.~~ **Verified.**
2. ~~Lint drops from 10 warnings to 9, with no new warnings.~~ **Verified.**
3. ~~Typecheck and tests stay clean.~~ **Verified.**

## Checks Run

- [x] `npm run lint` — **9 warnings, 0 errors**, down from 10; the `exhaustive-deps` entry is gone
- [x] `npm run typecheck` — **0 errors**
- [x] `npx vitest run` — **74 tests, all pass**

## Note on the Baseline

The recorded lint baseline changes from **10 warnings to 9**. `context/execution-standards.md` and
`context/progress-tracker.md` are updated to match, so future units are not measured against a
stale number.

## Decision Log Update Required

None. This restores intended behaviour; it decides nothing.
