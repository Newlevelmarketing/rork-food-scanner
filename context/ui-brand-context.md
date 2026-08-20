# UI / Brand Context

> Visual sections written from the theme files on 2026-08-20 by `/brain-onboard`. Voice
> sections are inferred from shipped copy and marked where they need human ratification.
> This file describes **what the app is today**. A redesign is a unit of work with its own spec,
> not an edit to this file.

## Product Personality

ModernBody (codename Calzy) is a **calm, tactile, phone-first food tracker**. It reads as a
polished consumer iOS app rather than a clinical health tool: soft pastel mist backdrop,
rounded cards, spring animations, haptics on interaction.

The one sharp edge is deliberate. **Jester mode** (`profile.jesterMode`, off by default) flips
the AI's closing line from an encouraging note to "one savage, funny one-line roast of this
meal, max 14 words" (`web/src/lib/ai.ts:86-88`). Warmth is the default; the roast is opt-in.

## Voice

> Inferred from `web/src/data/strings.json`, error copy in `lib/ai.ts`, and
> `features/Paywall.tsx`. **Needs human ratification.**

The product should sound:

- **Plain and short.** "Tap Scan to add your first meal of the day." No preamble.
- **Practical.** Every error says what happened and what to do next.
- **Warm without cheerleading.** Encouraging, not motivational-poster.

The product should not sound:

- Clinical or medical
- Shaming about food or weight — the numbers are neutral, the copy stays neutral
- Salesy outside the paywall

### Error copy pattern

State the problem, then the recovery, in that order:

> "That photo is too large. Try taking a new one."
> "We couldn't find any food in that photo. Try again with better lighting."
> "Too many scans at once. Wait a moment and try again."

Never surface a status code, a stack, or a provider name to the user.

## Content Rules

- UI labels are **sentence case**. Settings section headers are **ALL CAPS** (`s.account = ACCOUNT`).
- Strings live in `web/src/data/strings.json` under a flat dotted namespace: `tab.*`, `h.*`
  (home), `s.*` (settings). Never hardcode a user-facing string in a component.
- **32 locales**, including RTL. Any shipped copy is added to all of them, or the unit spec
  explicitly scopes it English-only.
- The AI system prompt is itself brand copy (`lib/ai.ts:67-89`) and is mirrored on iOS and
  Android. Changing it is a three-platform change.
- `[UNKNOWN — needs human input]` — the AI prompt uses British spelling ("appetising",
  "Analyse") while code identifiers use American ("analyzeImage"). Pick one for user-facing
  copy and record it.

## Visual Theme

Light, airy, high-contrast type on near-white. An ambient pastel mist sits behind the app: four
radial gradients in lavender, peach, sky, and apricot over an off-white base, painted on a
pinned pseudo-element (`.calzy-backdrop`, `index.css:124-160`).

The comment there records a real constraint worth preserving: `background-attachment: fixed` is
not honoured by iOS Safari and forces a full-gradient repaint every frame.

The app is pinned to the visual viewport — `position: fixed`, `overflow: hidden`,
`overscroll-behavior: none`, `user-select: none` — so it behaves like a native app rather than a
scrolling web page (`index.css:83-96`). Text inputs opt selection back in.

Layout is a single centered column capped at `max-w-[520px]` (`pages/Index.tsx:42`). This is a
phone-shaped app that happens to run in a browser.

## Colors

All colors are HSL custom properties on `:root` in `web/src/index.css`, mirroring
`ios-calzy/.../Utilities/Theme.swift`. **Never write a raw hex or `rgb()` value.**

| Role | Token | Value (HSL) |
| --- | --- | --- |
| Page background | `--background` | `260 17.6% 96.7%` |
| Card surface | `--card` | `0 0% 100%` |
| Primary text | `--ink` / `--foreground` | `240 4.3% 4.5%` |
| Muted text | `--ink-soft` | `240 3.2% 43.3%` |
| Faint text | `--ink-faint` | `240 4.2% 67.3%` |
| Well / inset surface | `--well` | `240 13% 95.5%` |
| Border | `--border` | `240 8% 90%` |
| Calories accent | `--flame` | `18 100% 58.6%` |
| Water | `--water` | `209 100% 61.2%` |
| Protein | `--protein` | `353 100% 67.6%` |
| Carbs | `--carbs` | `218 100% 64.9%` |
| Fat | `--fat` | `37 91.3% 55.1%` |
| Success / healthy | `--mint` | `147 60.5% 46.7%` |
| Accent | `--plum` | `271 81.7% 67.8%` |
| Destructive | `--destructive` | `353 100% 67.6%` |

Backdrop mist: `--mist-lavender`, `--mist-peach`, `--mist-sky`, `--mist-apricot`, `--mist-sage`.

**Macro colors are semantic, not decorative.** Protein is always `--protein`, carbs always
`--carbs`, fat always `--fat`, across charts, rings, and labels. BMI categories reuse them
deliberately (`lib/nutrition.ts:113-124`).

Corner radius: `--radius: 0.75rem`.

**Dark mode is scaffolded but not implemented.** `darkMode: ["class"]` is set
(`tailwind.config.ts:5`) and `next-themes` is installed, but `index.css` defines no `.dark`
palette and no `prefers-color-scheme` block. Shipping dark mode means authoring a full second
token set on all three platforms — a project, not a toggle.

## Typography

| Role | Font stack | Notes |
| --- | --- | --- |
| UI text | `-apple-system`, `BlinkMacSystemFont`, `SF Pro Text`, `Inter`, `Segoe UI`, `system-ui` | Native system face first |
| Numerals / metrics | **Nunito** (700–900), falling back to `SF Pro Rounded` | The `font-metric` utility; used for every large number |
| Mono | none defined | — |

Nunito loads from Google Fonts at the top of `index.css` — the app's only third-party asset
dependency at runtime.

The rounded numeric face carries most of the brand feel. Calorie counts, ring totals, and
metric cards use it; body copy does not.

## Motion

Defined in `tailwind.config.ts:83-130`. Reuse these rather than writing new keyframes:

| Animation | Use |
| --- | --- |
| `rise-in` | Tab content entering (0.5 s, spring easing) |
| `pop-in` | Cards and results appearing |
| `sheet-up` | Bottom sheets |
| `scan-sweep` | The scanning line during analysis |
| `breathe` | Pulsing idle state |
| `drop-pulse` | Water logged confirmation |
| `shimmer` | Loading skeletons |

The shared easing is `cubic-bezier(0.22, 1, 0.36, 1)` — a fast-out, settle-in spring. Match it.

## Components

- Primitive library: **shadcn/ui** on Radix, in `web/src/components/ui/` (47 files).
  **Generated — never hand-edit.** Configured in `web/components.json`.
- Project components: `web/src/components/calzy/` — `Primitives.tsx`, `Sheet.tsx`,
  `TabBar.tsx`, `HomeParts.tsx`, `icons.tsx`. New project-specific UI goes here.
- Icons: **Lucide** (`lucide-react`). Icon choices are data, stored as string names in
  `lib/nutrition.ts` metadata maps and resolved through `icons.tsx`.
- Toasts: `sonner`. Sheets: `vaul` plus the local `Sheet.tsx`.
- Charts: `recharts`, on the Progress tab.
- Haptics: `lib/haptics.ts`, mirrored by `Haptics.swift`. Interactions are expected to buzz.

## Example Copy

Good — states the problem and the recovery, no blame:

> "We couldn't find any food in that photo. Try again with better lighting."

Bad — vague, no recovery, exposes internals:

> "Error 502: analysis request failed."

Good — plain invitation, no exclamation marks:

> "Tap Scan to add your first meal of the day"

## Open Questions

- Is jester mode a headline feature or an easter egg? It changes how bold the brand can be.
- Is dark mode planned, or is the scaffolding vestigial from the shadcn template?
- British or American spelling for user-facing copy?
- `[UNKNOWN — needs human input]` — is there a logo, wordmark, or app-store creative set? The
  repo has only `icon.png` and `favicon.png`, no brand asset source.
