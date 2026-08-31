# Execution Standards

> Written from the conventions **actually present in this codebase** on 2026-08-20 by
> `/brain-onboard`. Where the code disagrees with a general best practice, the code wins and
> the gap is recorded rather than silently "fixed".

## General Principles

- Keep work small, focused, and verifiable.
- Fix root causes instead of layering workarounds.
- Do not mix unrelated concerns in one change.
- Prefer clarity over cleverness.
- Do not create generic AI-sounding output.
- Preserve existing decisions unless a new decision is explicitly made.
- **Match the surrounding code.** Naming, file layout, error handling, and comment density
  follow what is already there.

## Commands

Run from `web/`. Baseline results are from a clean checkout on 2026-08-20, Node 24.15.0,
npm 11.12.1.

| Purpose | Command | Baseline |
| --- | --- | --- |
| Install | `npm install` | Passes — 378 packages |
| Dev server | `npm run dev` | Vite on port 8080 (`vite.config.ts:10`) |
| Build | `npm run build` | **Passes** — 1,009.54 kB JS (287 kB gzip) |
| Typecheck | `npm run typecheck` | **Passes** — 0 errors, under full `strict` |
| Lint | `npm run lint` | **Passes** — 0 errors, 9 warnings |
| Unit tests | `npx vitest run` | **Passes** — 4 files, 74 tests |
| Browser tests | `npx vitest run --config vitest.browser.config.ts` | **FAILS** — see below |
| Both suites | `npm test` | **FAILS** — because of the browser suite |

**Pre-existing failure — do not attribute it to your unit.** The browser suite cannot start:
Playwright's browser binaries are not installed on this machine. It needs a one-time
`npx playwright install` (a large download, so run it deliberately). Until then
`web/src/test/calendar.browser.test.tsx` never executes and `npm test` exits 1.

**Lint warnings (9):** all are `react-refresh/only-export-components`, seven inside generated
`web/src/components/ui/**`. They are HMR hints, not defects. The tenth was real — a `useCallback`
in `ScanSheet.tsx` missing `language.englishName`, which froze the scan language — and was fixed
in unit 08.

**Test coverage** now stands at 74 tests across `nutrition.ts`, `dates.ts` and the AI response
parser (units 03, 04, 06). Still uncovered: `image.ts` (DOM canvas), `summaryCard.ts`, and
`store/AppStore.tsx`, which needs a React testing setup.

CI runs typecheck, lint, unit tests and build on every push touching `web/`
(`.github/workflows/web-ci.yml`). Note that `npm run build` alone does **not** typecheck — Vite
does not — so `npm run typecheck` is the gate, not the build.

There are no native build commands verified here — see Known Debt in `architecture.md`, the
iOS and Android projects cannot build from a fresh clone.

## TypeScript

**The compiler is configured loosely, but the code is written strictly.** Follow the code, not
the config.

Actual `tsconfig.app.json` settings: `strict: false`, `noImplicitAny: false`,
`noUnusedLocals: false`, `noUnusedParameters: false`, `noFallthroughCasesInSwitch: false`.
ESLint additionally turns `@typescript-eslint/no-unused-vars` off (`eslint.config.js:23`).

Despite that, the hand-written source contains **zero uses of `any`** and annotates return
types everywhere. Hold that line:

- Annotate every exported function's return type, including components (`): JSX.Element`).
- Never introduce `any`. The config will not catch you; the review will.
- Use `import type { ... }` for type-only imports — used consistently throughout.
- Model closed sets as string-literal unions, not enums (`MealSlot`, `EntrySource`,
  `ActivityLevel` in `lib/types.ts`).
- Do not tighten `tsconfig.json` as a side effect of a feature unit. Turning `strict` on is its
  own unit with its own spec, because it will surface errors across the whole tree.

## Import Order

Observed consistently in `store/AppStore.tsx`, `pages/Index.tsx`, `lib/nutrition.ts`:

1. `import type` declarations
2. blank line
3. external packages
4. blank line
5. internal `@/` aliased imports, alphabetized by path

The `@/` alias maps to `web/src/` (`vite.config.ts:18`, `tsconfig.app.json:21`).

## React Conventions

- **State lives in one place.** `AppStoreProvider` owns all app data; components consume it via
  `useAppStore()`. Do not add a second global store or reach into `localStorage` directly.
- The context throws if consumed outside the provider (`AppStore.tsx:469`) — keep that guard
  pattern on any new context.
- Every store method is wrapped in `useCallback`, and the context value in `useMemo`, with full
  dependency arrays. Follow this; the file is large and re-render churn is the reason.
- Screens under `pages/` and sheets under `features/` use **named** exports.
  `pages/Index.tsx` and `pages/NotFound.tsx` use default exports because the router imports them
  that way. Do not "harmonize" this inside an unrelated unit.
- Navigation is component state, not URLs: a `tab` and a `route` in `pages/Index.tsx`. There is
  one real router route. Adding a URL-addressable screen is an architectural change, not a
  detail.

## Error Handling

The established pattern, mirrored on all three platforms:

1. A closed union of error kinds (`NutritionAIErrorKind` in `lib/ai.ts:22-30`).
2. A message map from kind to user-facing copy (`lib/ai.ts:32-41`).
3. A typed error carrying the kind (`NutritionAIError`).
4. A helper that turns an unknown thrown value into display copy
   (`messageForError`, `lib/ai.ts:224`).

New failure modes get a new kind and a new message, not an inline string. User-facing messages
say what happened and what to do next — never a raw status code or stack.

Storage failures are caught and warned, never thrown (`AppStore.tsx:74-77,143-147`). Preserve
that: the app must stay usable when persistence fails.

## Data and External Input

- Numbers coming back from the model are clamped and rounded at the boundary before entering
  the store (`Math.max(0, Math.round(...))`, `lib/ai.ts:212-222`).
- Model replies are parsed by extracting the first balanced JSON object, tolerating markdown
  fences (`lib/ai.ts:92-105`).
- **Gap worth knowing:** `zod` is a declared dependency but is imported nowhere in `src/`. The
  AI response — the single most untrusted input in the app — is cast with
  `as AnalysisResult` (`lib/ai.ts:174`) with only shape checks on `isFood` and `items`. A
  malformed-but-parseable reply reaches the store. Validating it is a good candidate unit; do
  not do it opportunistically inside unrelated work.
- Domain math belongs in `lib/`, never in components. `lib/nutrition.ts` owns every calorie,
  macro, BMR, and BMI calculation.

## Comments

The codebase comments **why**, not what, and does it well. Match this density and voice:

> `// Debounced persistence so slider drags don't thrash localStorage.`
> `// Per-item rounding can drift a few kcal off the requested total; push the remainder into
> the largest item so the row shows exactly what was typed.`

Exported domain functions carry a one-line JSDoc. Do not add comments that restate the code.

## Cross-Platform Parity

The web, iOS, and Android apps are hand-mirrored implementations of one product. A change to
shared behavior has three homes:

| Concern | Web | iOS | Android |
| --- | --- | --- | --- |
| Domain models | `web/src/lib/types.ts` | `ios-calzy/.../Models/` | `android/.../data/Models.kt` |
| Store | `web/src/store/AppStore.tsx` | `.../Store/AppStore.swift` | `.../AppViewModel.kt` |
| AI service | `web/src/lib/ai.ts` | `.../Services/NutritionAI.swift` | `.../data/AiService.kt` |
| Theme | `web/src/index.css` | `.../Utilities/Theme.swift` | `.../ui/theme/Theme.kt` |
| Nutrition math | `web/src/lib/nutrition.ts` | — | `.../data/Nutrition.kt` |

A unit that changes one platform states explicitly whether the others follow, and when.

## Existing-Codebase Rules

- Do not reformat files you are not otherwise changing.
- Do not rename existing symbols as a side effect of a feature unit.
- Do not upgrade dependencies inside a feature unit.
- Never hand-edit `web/src/components/ui/**` — regenerate via shadcn instead.
- If existing code violates a standard above, note it in Known Debt in
  `context/architecture.md` rather than fixing it opportunistically.

## Design Standards

- Follow the visual language in `ui-brand-context.md`.
- Use the existing token set. Never introduce a raw hex or `rgb()` value.
- Reuse `components/calzy/Primitives.tsx` and the shadcn primitives before writing new ones.
- Make designs readable first, beautiful second.

## Content / Communication Standards

Applies to UI copy, AI prompt text, and anything user-facing.

- Match the voice defined in `ui-brand-context.md`.
- Keep claims grounded. Never invent nutrition facts or health guidance.
- User-facing copy that ships must be added to all 32 locales in
  `web/src/data/strings.json`, or explicitly scoped as English-only in the unit spec.

## Quality Checklist

Before marking work as complete:

1. Does it match the current spec?
2. Does it preserve project scope?
3. Does it follow the project voice, design, and architecture?
4. Is it clear what changed?
5. Are open questions documented?
6. Is the progress tracker updated?
7. Did the named checks actually run, and is the real result reported — measured against the
   baseline above, not against zero?
