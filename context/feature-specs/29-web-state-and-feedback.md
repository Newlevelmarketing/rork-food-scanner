# Unit Spec: Stale UI State and Silent Failures

## Status

- Status: **Complete — verified in the running app**
- Owner: Claude Code
- Created: 2026-09-03
- Last Updated: 2026-09-03

## Goal

Clear the audit's remaining web findings where the UI shows something stale, or fails without
saying so.

## The Findings

### 1. Importing a broken photo did nothing at all — `Progress.tsx:99` (medium)

`onPickPhoto` awaited `toThumbnail` and **returned silently** when it produced nothing. No error,
no pending state. And the caller invokes it with `void`, so a throw became an **unhandled
rejection**.

Picking an unreadable file produced no response whatsoever — which reads as the app being broken
rather than the file being bad.

**Fix:** try/catch with a `role="alert"` message, and the Add button disables and reads "Adding…"
while work is in flight.

### 2. A one-time share failure stuck forever — `ShareSummary.tsx:35` (low)

The `!open` branch reset `ready` but not `error` or `busy`, and the component is **mounted
persistently** by Home rather than conditionally. So after a single failure, every later open
showed *"Sharing failed. Try again."* above a card that had rendered perfectly well — before the
user had tried anything.

**Fix:** reset all three on close, matching every other sheet in the codebase.

### 3. Interrupted counters snapped — `Primitives.tsx:63` (low)

`AnimatedNumber`'s cleanup stored `fromRef.current = value` — the **target**, not what was on
screen. Interrupt a roll-up (a second edit mid-animation) and the next one started from a number
the user never saw, so the counter visibly jumped.

**Fix:** a `displayRef` tracks the rendered value each frame, and the cleanup resumes from that.

State alone cannot do this: the cleanup closure captures `display` from the render that created the
effect, which is already stale by the time the animation is interrupted.

### 4. `.env.production` was not gitignored — `web/.gitignore:9` (low)

The ignore list had `.env` and `*.local`, neither of which matches `.env.production` or
`.env.development`. A mode-specific file holding `GEMINI_API_KEY` would have been committed.

**Fix:** `.env.*` with `!.env.example`, so every variant is ignored and the committed example
survives. Both directions verified with `git check-ignore`.

## Scope

### Out of scope, with reasons

- **The Google Fonts hotlink** (`index.css:1`). `/privacy` and `/terms` load a Google-hosted
  webfont, so Google sees the IP of anyone reading the privacy policy — which sits oddly against
  what that policy says. Self-hosting means downloading and committing font files, which is a
  decision about assets rather than a code fix. **Left open deliberately.**
- `TabBar.tsx:34`, `Settings.tsx:211`, `Onboarding.tsx:289`,
  `SettingsSheets.tsx:481` — the remaining low a11y items.
- `Home.tsx:247`, the carousel dots stuck on page 0.

### A note on the new copy

`"We couldn't read that image. Try a different photo."` is hardcoded English, matching the rest of
`Progress.tsx` — which already hardcodes "Progress", "Progress Photos", "Body metrics" and more.

`context/execution-standards.md` says shipped copy reaches all 32 locales or is scoped English-only
in the spec. It is scoped English-only here, but the wider point stands: **large parts of the UI
bypass the localisation catalogue entirely.** That is a real gap, bigger than this unit, and not
currently on the audit's list.

## Checks Run

- `npm run typecheck` — **0 errors**
- `npm run lint` — **0 errors, 9 warnings**, unchanged
- `npx vitest run` — **183 tests**, all pass
- `npm run build` — **pass**

### Verified in the running app

Fed the photo input a `File` with a `.png` name and five bytes of junk — a plausible corrupt image.

**Result:** `role="alert"` appeared reading *"We couldn't read that image. Try a different photo."*
Before this change the picker silently did nothing.

`git check-ignore` confirms `.env.production` is now ignored and `.env.example` is not.

The only console error was `navigator.vibrate` blocked for want of a user gesture — an artifact of
driving the page programmatically.

### Honest limit

The **AnimatedNumber** fix is verified by reading and typecheck, not observation. Catching a
snap needs interrupting a 520 ms animation at a known frame and asserting on an intermediate
rendered value, which is not something this suite can currently express.

## Decision Log Update Required

None.
