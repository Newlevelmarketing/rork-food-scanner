# Unit Spec: Test the i18n and Food-Search Layers

## Status

- Status: **Complete**
- Owner: Claude Code
- Created: 2026-08-22
- Last Updated: 2026-08-22

## Result

**118 tests across 6 files, up from 74. All pass. No defect found.**

The catalogue is complete: 32 tables, 52 keys each, identical key sets, no empty strings. The
integrity test now guards that rather than describing it.

`browserLanguage` behaves correctly on every case checked, including the ones most likely to be
wrong in a hurry — `zh-TW` and `zh-Hant-HK` resolving to Traditional while `zh-CN` and bare `zh`
go to Simplified, and `no` mapping to `nb`.

### One thing worth recording about test quality

The ranking test was written with an early return for the case where no query exercises both
match kinds. That would have let it **pass while asserting nothing** — coverage in appearance
only.

Checked rather than assumed: the bundled table yields **6 rankable queries**, the first being
`"protein"` with 2 name matches against 9 tag-only ones. The ranking really is exercised. The
silent escape was then replaced with an explicit `expect(query).not.toBeNull()`, so if the food
table ever changes such that ranking cannot be tested, the suite says so instead of going quietly
green.

## Goal

Cover `web/src/lib/i18n.ts` and `web/src/lib/foods.ts` with unit tests, and add a catalogue
integrity check that fails if a user-facing string ships untranslated.

## Why This Matters

Localisation is one of this product's largest surfaces — **32 languages, three of them
right-to-left** — and none of it is tested. Unit 08 already found one language bug that had shipped
(the scan language froze at mount), which suggests this area deserves attention rather than trust.

`browserLanguage()` in particular carries real subtlety that is invisible until it misfires:

- Chinese needs **script-level** matching, `zh-Hans` versus `zh-Hant`, so `zh-TW` and `zh-HK` must
  not resolve to Simplified.
- Everything else matches on the base subtag, so `es-ES` must find Spanish.
- Norwegian has a special case: the tag `no` maps to `nb`.
- An unrecognised tag must fall through to the next preference, not bail to English early.

`searchFoods` has ranking logic — name matches sort above tag-only matches — that nothing verifies.

### The catalogue check

Measured before writing the assertion: the catalogue has **32 tables of 52 keys each, with
identical key sets**. It is complete today.

That makes the integrity test a genuine regression guard rather than a documented failure: the
moment someone adds an English string without translating it, the test fails. Given that
`context/execution-standards.md` requires shipped copy to reach all 32 locales, this enforces a
rule that was previously honour-system.

## Scope

### In Scope

- `web/src/test/i18n.test.ts`
- `web/src/test/foods.test.ts`

### Out of Scope — not touched

- **Any file under `web/src/lib/`.** This unit adds tests. If one reveals a bug, report it and
  stop; the fix is a separate unit.
- `useLanguage` and `useT` — React hooks needing a testing library that is not installed. Adding
  one is its own unit; see the note below.
- `store/AppStore.tsx` — same reason, and it is the highest-value remaining test target.
- `strings.json` content. Translation quality is not assessed here, only structural completeness.

### No new dependencies

Both modules are pure and run in the existing node test environment. `navigator` is stubbed with
`vi.stubGlobal` rather than pulling in a DOM implementation.

This is deliberate: `AppStore.tsx` would need `@testing-library/react` plus `jsdom` or
`happy-dom`, and adding devDependencies has a complication worth deciding separately — the repo's
lockfile is `bun.lock` with no `package-lock.json`, so a dependency added via npm would not be
reflected in the lockfile CI installs from.

## Implementation Details

### `i18n.test.ts`

1. `languages` — 32 entries, unique codes, English first, `isRTL` on exactly Arabic, Persian and
   Hebrew.
2. **Catalogue integrity** — every code in `languages` has a table in `strings.json`, and every
   table's key set matches English exactly. Failure names the offending language and keys.
3. `languageFor` — a known code, an unknown code, and `undefined`.
4. `browserLanguage` with `navigator` stubbed: `es-ES` → Spanish; `zh-TW`, `zh-HK` and
   `zh-Hant-HK` → Traditional; `zh-CN` → Simplified; `no` → `nb`; an unknown tag falls through to
   the next preference; an empty list and an absent `navigator` both yield English.
5. `translate` — a known key; a key missing from a non-English table falling back to English; an
   unknown key returning the key itself rather than throwing.

### `foods.test.ts`

1. `allFoods` — 100 records, each with a non-empty name and serving and non-negative macros.
2. `searchFoods` — empty and whitespace-only queries return 24; case-insensitive name matching;
   tag matching; **name matches ranked above tag-only matches**; alphabetical within a rank; an
   unmatched query returns empty.
3. `foodToItem` — field mapping, and a distinct id per call.
4. `presetCalories` — exact at the 80 kg baseline, scales linearly with weight, rounds to a whole
   number.

## Acceptance Criteria

1. Both files run under `npx vitest run`.
2. Every exported non-hook function in both modules has at least one assertion.
3. The catalogue check passes today and would fail on an untranslated key.
4. No file outside `web/src/test/` is modified.
5. Typecheck, lint and build stay at baseline — 0 errors, 9 warnings.

## Checks Run

- [x] `npx vitest run` — **6 files, 118 tests, all pass**
- [x] `npm run typecheck` — **0 errors**
- [x] `npm run lint` — **0 errors, 9 warnings**, unchanged
- [x] `npm run build` — **pass**
- [x] `git status` — only additions; no source file modified

## Progress Tracker Update Required

Record the new coverage, the catalogue result, and any defect found.

## Decision Log Update Required

Only if a test reveals intentional-but-undocumented behaviour.
