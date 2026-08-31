# Unit Spec: Runnable From a Clean Clone

## Status

- Status: **Complete**
- Owner: Claude Code
- Created: 2026-08-22
- Last Updated: 2026-08-22

## Goal

Make `git clone` → run work on a fresh machine — specifically the Mac the human is setting up —
without anyone having to know the tribal knowledge this project had accumulated.

## Why This Matters

Four traps stood between a clean clone and a running app, none of them discoverable from the
repository itself:

1. **`vite dev` does not serve `/api/*`.** After unit 11 the scanner posts to `/api/analyze`, a
   function the *host* runs in production. Locally that request fell through to the SPA fallback,
   returned `index.html`, and failed to parse. Scanning was simply dead in development.
2. **The iOS project does not compile from a clean clone.** `Config.swift` is Rork-generated and
   gitignored, and two source files reference it. It presents as a broken project, not a missing
   file.
3. **Node version.** Git Bash on this machine resolves v18.20.4 via nvm while PowerShell uses
   v24.15.0. Vite 8 needs 20.12+, and under 18 the test runner dies with a `styleText` import
   error that reads like a code fault.
4. **No README.** The repo had none at all — no entry point describing how to run anything.

## Scope

### In Scope

- `web/vite.config.ts` — a dev-server plugin mounting `api/analyze.ts` at `/api/analyze`
- `web/.env.example`, `web/package.json` (`engines`), `.nvmrc`
- `ios-calzy/Config.swift.example` and `ios-calzy/setup-config.sh`
- `README.md` at the repo root

### Out of Scope

- Android's equivalent `Config.kt` bootstrap — same problem, but it cannot be verified here and
  belongs with the Android work in units 13/14.
- `"name": "rork-web-app"` in `package.json` — a cosmetic Rork trace, batched into unit 16.
- Anything requiring the Mac.

## Implementation Notes

**The dev middleware runs the real handler**, via `ssrLoadModule`, rather than a stub. Local
development therefore exercises the same code the deployment does, so a bug found locally is a
real bug.

Env is read with `loadEnv(mode, cwd, "")` — an empty prefix, so `GEMINI_API_KEY` is visible to the
middleware. That is safe because it runs in the dev-server process and is never passed to `define`
or to client code. Verified: the key name does not appear in `dist/`.

**The template is `Config.swift.example`, not `Config.swift.example.swift`.** `ModernBodyFoodScanner/`
is a `PBXFileSystemSynchronizedRootGroup`, so Xcode compiles every `.swift` file it finds there
automatically — a second file declaring `enum Config` would be a duplicate-symbol error. The
non-`.swift` extension makes that impossible.

`setup-config.sh` refuses to overwrite an existing `Config.swift`, so it is safe to re-run.

## Acceptance Criteria

1. ~~`POST /api/analyze` works under `vite dev`.~~ **Verified against a running server.**
2. ~~The iOS setup script produces a compiling `Config.swift` that is gitignored.~~ **Verified.**
3. ~~No secret name reaches the bundle.~~ **Verified.**
4. ~~Checks stay at baseline.~~ **Verified.**

## Checks Run

Against a live dev server on port 8098/8099, exercising the real handler:

| Request | Expected | Actual |
| --- | --- | --- |
| Valid body, no key | 503 `notConfigured` | **503 `notConfigured`** |
| `kind: "audio"` | 400 | **400**, `kind must be "image" or "text".` |
| Whitespace content | 400 | **400**, `content is required.` |
| `kind: "image"` with an http URL | 400 | **400**, `content must be a base64 data URL.` |
| Valid body, bad key | mapped upstream failure | **502 `upstream`** — the request genuinely reached Gemini |
| `GET` | 405 | **405** |

Plus: `npm run typecheck` 0 errors, lint 9 warnings, **158 tests**, build passes, and
`setup-config.sh` output confirmed ignored by `git check-ignore`.

## A Finding Worth Recording

An **invalid API key surfaces to the user as "Something went wrong"**, not as an auth error.
Google returns 400 for a malformed key, and this proxy maps 400 to a server error on the grounds
that a bad request is our fault rather than the user's.

That is the right *user-facing* choice — the alternative message tells them to reload, which
cannot supply a server key. But it makes the single most likely deployment failure look generic.
The server log now says `check GEMINI_API_KEY and GEMINI_MODEL` on 400 and 403 so the operator is
not left guessing.

## Decision Log Update Required

None. This adds no behaviour; it removes friction.
