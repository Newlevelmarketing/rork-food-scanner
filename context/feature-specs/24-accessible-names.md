# Unit Spec: Accessible Names, and Un-blocking Zoom

## Status

- Status: **Complete — verified in the running app**
- Owner: Claude Code
- Created: 2026-09-03
- Last Updated: 2026-09-03

## Goal

Close the audit's last remaining **high**-severity finding, plus the zoom block from the same lens.

## Defect 1 — every Toggle and Slider was anonymous

`Primitives.tsx:281`. **The only `high` left in the audit**, and a straight WCAG 4.1.2 (Name, Role,
Value) failure.

`Toggle` declared `role="switch"` and `aria-checked` but took **no label prop**, and its only child
was a decorative `<span>` — so it announced as *"switch, off"* with no subject. `Slider` rendered a
bare `<input type="range">` with no `aria-label`, no `id`, and no wrapping `<label>`.

The call sites all render their visible title as a **plain `<span>` with no `id`**, so nothing
associated the two. `Label` in `SettingsSheets.tsx:68` is itself a bare `<span>`.

**The onboarding flow is six mandatory screens of these.** To a screen-reader user it was a column
of identical unnamed "slider" controls — no way to tell weight from goal weight from weekly pace.

### Why `label` is required, not optional

An optional prop would have been omitted again at the next call site, which is how this happened
in the first place. Making it required turns the compiler into the enforcement mechanism: adding a
`Toggle` or `Slider` without a name is now a build error.

That surfaced **six** unlabelled call sites immediately, which is roughly the point.

`SliderRow` and `BigSlider` already receive a `title`, so they forward it and their nine call sites
needed no change at all.

## Defect 2 — the name field had no associated label

`Onboarding.tsx:106` wrapped a `<label>` with no `htmlFor` around an `<input>` with no `id` —
proximity, not association. On the app's mandatory first screen it announced as an unnamed text
field. Now linked by `htmlFor`/`id`.

## Defect 3 — pinch zoom was disabled app-wide

`index.html:5` carried `maximum-scale=1.0, user-scalable=no`, which fails WCAG 1.4.4.

**Checked before removing it:** the reason apps usually add this is to stop double-tap zoom, and
this project already handles that properly — `touch-action: manipulation` appears three times in
`index.css`. So the attributes were redundant as well as harmful.

## Scope

### In Scope

- `web/src/components/calzy/Primitives.tsx` — `Toggle`, `Slider`
- The six unlabelled call sites, plus `SliderRow` and `BigSlider` forwarding their titles
- `web/src/pages/Onboarding.tsx` — the name field
- `web/index.html` — viewport

### Out of Scope — the remaining a11y findings

- **`Sheet.tsx:48`** — sheets have no dialog semantics, no focus trap, no focus restoration, and
  background UI stays mounted and tabbable. Real, and bigger: it needs a focus-management strategy
  across every sheet, not a prop. Its own unit.
- `TabBar.tsx:34` — selection by colour alone, no tab semantics.
- `Settings.tsx:211` — erase confirmation has no alert role and no focus move.
- `Onboarding.tsx:289` — 18 px hit area on the Back control.
- `SettingsSheets.tsx:481` — reminder slots keyboard-operable while visually disabled.

## Checks Run

- `npm run typecheck` — **0 errors** (after fixing the six sites the compiler flagged)
- `npm run lint` — **0 errors, 9 warnings**, unchanged
- `npx vitest run` — **179 tests**, all pass
- `npm run build` — **pass**

### Verified in the running app

Walked the real onboarding flow and read the accessibility properties directly:

| Check | Result |
| --- | --- |
| Onboarding sliders | **"Birth year", "Height", "Weight", "Goal weight", "Weekly pace"** — every one distinct |
| Any anonymous control | **None** |
| Name field | `label[for=onboarding-name]` associated, reading "What should we call you?" |
| Settings switch | **"Jester Mode"**, and localised via `t("s.jester")` |
| Viewport meta | `width=device-width, initial-scale=1.0, viewport-fit=cover` — no zoom block |

"Weight" versus "Goal weight" versus "Weekly pace" is exactly the distinction the audit said was
impossible to make.

## Decision Log Update Required

None. This restores names the UI already displayed visually.
