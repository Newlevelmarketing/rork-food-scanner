# Unit Spec: Sheet Dialog Semantics and Focus Management

## Status

- Status: **Complete — verified in the running app**
- Owner: Claude Code
- Created: 2026-09-03
- Last Updated: 2026-09-03

## Goal

Make the modal sheets behave like modals for a keyboard or screen-reader user. They are the app's
primary navigation surface — scan, describe, search, saved, exercise, meal detail, edit, paywall,
share, and every settings screen — so this is most of the product.

## The Findings

Two audit items, one component.

### `Sheet.tsx:48` (medium) — not actually a dialog

`FullScreenSheet` rendered a plain `<div>`. No `role="dialog"`, no `aria-modal`, no accessible
name. To assistive tech it was an anonymous container that happened to be on top.

Worse: **background UI stayed mounted and tabbable.** Nothing moved focus into the sheet on open,
so the first Tab landed behind the overlay — the user could tab through a screen they could not
see, activating controls hidden under a modal. And nothing restored focus on close, so dismissing
a sheet dropped focus to the top of the document.

### `Sheet.tsx:38` (low) — one Escape closed two sheets

Each open sheet registered its **own window `keydown` listener** with no top-most check and no
`stopPropagation`. Sheets stack: `MealDetail`'s pencil opens `EditMealSheet` **without clearing the
detail**, and `Index` renders both as siblings, so both listeners were live.

One Escape ran both `onClose` callbacks — dropping the user from the calorie edit, past the meal
detail, all the way back to the Home timeline.

## The Fix

1. **`role="dialog"`, `aria-modal="true"`, and an accessible name** from `title`, with a new
   optional `label` prop for `bare` sheets that have no visible title. `ScanSheet` passes
   `label="Meal scanner"`.
2. **A module-level stack** of open sheets. Each instance takes a `Symbol` identity, pushes on
   open, removes on close, and only acts on Escape when it is last.
3. **Focus trap on Tab.** `aria-modal` tells assistive tech the background is inert but does
   nothing for keyboard order, so Tab and Shift+Tab wrap within the sheet's focusable elements.
4. **Focus moved in on open, restored on close** to whatever was focused before.

## Scope

### In Scope

- `web/src/components/calzy/Sheet.tsx`
- `web/src/features/ScanSheet.tsx` — one `label` prop

### Out of Scope

- **`inert` on the background.** The correct modern primitive, but it needs a wrapper around all
  non-sheet content and its own layout change. The focus trap achieves the keyboard half today.
- `TabBar.tsx:34` — colour-only tab selection and missing tab semantics.
- `Settings.tsx:211` — the erase confirmation has no alert role and no focus move.
- Remaining low-severity a11y items.

## Checks Run

- `npm run typecheck` — **0 errors** (caught a missing destructure of the new prop first)
- `npm run lint` — **0 errors, 9 warnings**, unchanged
- `npx vitest run` — **183 tests**, all pass
- `npm run build` — **pass**

### Verified in the running app

Seeded a meal, opened the detail sheet, and stacked the edit sheet on top of it:

| Check | Result |
| --- | --- |
| `role="dialog"` and `aria-modal="true"` | **Present** |
| Accessible name | **"Lunch"**, then **"Edit meal"** for the stacked one |
| Focus moved into the sheet on open | **Yes** |
| Two sheets open simultaneously | **Confirmed** — `["Lunch", "Edit meal"]` |
| One Escape | **Closes only "Edit meal"**, leaving "Lunch" open |
| Second Escape | Closes the remaining sheet |
| Tab from the last focusable | **Wraps to the first**, stays inside |
| Shift+Tab from the first | **Wraps to the last** |
| Focus after close | **Restored to the exact trigger button** |

The sheet had 10 focusable elements, so the trap was exercised against a real list rather than a
trivial one.

The only console errors were `navigator.vibrate` being blocked for want of a real user gesture — an
artifact of driving the page programmatically, not a defect.

## Decision Log Update Required

None. This adds the semantics the component always implied.
