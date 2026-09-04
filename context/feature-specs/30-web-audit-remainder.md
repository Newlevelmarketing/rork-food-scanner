# Unit Spec: The Rest of the Web Audit List

## Status

- Status: **Complete — four of five verified in the running app**
- Owner: Claude Code
- Created: 2026-09-03
- Last Updated: 2026-09-03

## Goal

Clear the last five web findings from `context/audit-2026-09-03.md`.

## The Findings

### 1. Carousel dots never moved — `Home.tsx:247`

`setPage` had exactly two references: the `useState`, and an `onFocus` on the **first** panel — the
only value `page` could already hold. There was no scroll listener and no observer anywhere, so the
indicator permanently showed page 0 regardless of which panel was on screen.

**Fix:** derive the index from `scrollLeft / clientWidth` on the snap container, and drop the
vestigial `onFocus`. The dots are also now `aria-hidden` — they duplicate position that the scroll
container already conveys.

### 2. Tab bar signalled selection by colour alone — `TabBar.tsx:34`

The active tab differed only in text colour, icon weight and a faint background. Nothing conveyed
it non-visually, while `Index.tsx` already claimed `role="tabpanel"` for the content — a tabpanel
with no tablist anywhere.

**Fix:** `role="tablist"`, `role="tab"` and `aria-selected` on each.

### 3. Erase confirmation announced nothing — `Settings.tsx:211`

The destructive confirmation swapped in inline with no role and no focus move. A screen-reader user
got **no announcement that a destructive choice had appeared**, and a keyboard user was left
focused on a button that no longer existed.

**Fix:** `role="alertdialog"` with a label, and focus moves to **Cancel** — the safe option, not
"Erase everything".

### 4. Back control was an 18 px target — `Onboarding.tsx:289`

The row was `h-[18px]`, clamping the only way back through onboarding to under half the 44 pt
minimum.

**Fix:** `min-h-[44px]` on the row and padding on the control itself. This adds roughly 26 px to
the footer — a deliberate visual change, since the alternative is a control most thumbs miss.

### 5. Reminder slots were operable while disabled — `SettingsSheets.tsx:481`

The card used `pointer-events-none opacity-45` when reminders were off. That hides the slots from a
mouse but leaves them **tabbable and activatable by keyboard**, so a keyboard user could toggle
reminder times for a feature that was switched off.

**Fix:** a real `disabled` attribute, plus `aria-pressed` so the on/off state of each slot is
conveyed at all.

## Checks Run

- `npm run typecheck` — **0 errors**
- `npm run lint` — **0 errors, 9 warnings**, unchanged
- `npx vitest run` — **183 tests**, all pass
- `npm run build` — **pass**

### Verified in the running app

| Check | Result |
| --- | --- |
| Tab bar | `role="tablist"`, 3 tabs, `aria-selected` `["true","false","false"]` |
| Carousel dots | Active dot **0 → 1** on scroll, **1 → 0** scrolling back |
| Erase confirmation | `role="alertdialog"`, labelled "Erase all data", focus lands on **Cancel** |
| Reminder slots | 6 slots, `disabled: false` when on, **`true` when off** |

The carousel needed care: the browser pane has zero layout size here, so a first attempt using a
faked 400 px page width proved nothing — `scrollLeft` clamped to 80 against a `scrollWidth` of 236.
Re-run with a page width derived from the container's real `scrollWidth`, the dot tracked correctly.
**The initial "failure" was a bad test, not a bad fix**, and it was worth chasing rather than
recording as a pass.

### Not verified by measurement

The **44 pt hit area** is verified by the applied classes, not by measuring a rendered rectangle —
the pane has no layout, so every element measures 0. `min-h-[44px]` plus `py-[11px]` on a 14 px
control is arithmetic rather than observation.

## Remaining Web Findings: none

Every web item from the 2026-09-03 audit is now addressed, **except two deliberately deferred**:

1. **`/privacy` and `/terms` hotlink a Google-hosted webfont**, so Google sees the IP of anyone
   reading the privacy policy. Fixing it means committing font files — an assets decision.
2. **Large parts of the UI bypass the 32-locale catalogue.** Not an audit finding, but a standing
   violation of `context/execution-standards.md`, and larger than anything on the list.

What remains open overall is Android, and the native verification backlog.
