# Unit Spec: Make the Legal URLs Survive a Static Deploy

## Status

- Status: **Complete** — deployable; the rewrite itself is confirmable only after deploying
- Owner: Claude Code
- Created: 2026-08-20
- Last Updated: 2026-08-20

## Result

The 404 was real and confirmed by inspecting the build output: `dist/` contained
`index.html`, `favicon.png`, `icon.png`, `placeholder.svg`, `robots.txt` and `assets/` — and
**no `privacy/` directory**. A static host had nothing to serve for `/privacy`.

Fallback config now ships. Verified in the running app: `/privacy` renders with the tab reading
"Privacy Policy — ModernBody", the page scrolls (1929 px of content in a 694 px viewport,
scrolled to 400 to prove it), and the title reverts to the app's on returning to `/`. No console
errors.

The scroll check mattered: the global stylesheet sets `body { overflow: hidden }` for the app
shell, so a document page that relied on document scroll would have rendered fully and been
unreadable past the first screen.

## Goal

Ensure `https://<domain>/privacy` and `https://<domain>/terms` return the documents on a **direct
request**, not a 404, once the web build is deployed to a static host.

## Why This Matters

Unit 02 added the routes and they work in development. They would still have failed in
production, in the one way that matters most.

`npm run build` emits a single `index.html` plus hashed assets. There is no `dist/privacy/`
directory and no `dist/privacy/index.html`. A static host resolving a request for `/privacy`
looks for a file at that path, does not find one, and returns **404**.

That is the exact request App Store Connect and the Play Console make when validating a Privacy
Policy URL, and the exact link a reviewer clicks. The URL would have been dead on arrival.

`vite preview` masks the problem — it has SPA fallback built in, so `/privacy` resolves locally
and looks fine. The failure only appears on the real host, after submission.

Client-side routing needs the host to rewrite unknown paths to `index.html` so React Router can
resolve them. That rewrite is host-specific configuration, and it does not exist in this repo.

## Scope

### In Scope

- `web/public/_redirects` — SPA fallback for Netlify and Cloudflare Pages
- `web/vercel.json` — SPA rewrite for Vercel
- `web/src/pages/Legal.tsx` — set the document title, since these are standalone public pages
- Verify the emitted `dist/` actually carries the fallback file

### Out of Scope — not touched

- Choosing or creating a hosting account. That needs the human's credentials and is theirs to do.
- Any deployment. This unit makes the build deployable; it does not deploy it.
- A custom domain, DNS, or TLS.
- `Legal.swift`'s empty URL constants — they cannot be filled until a real domain exists.
- The app shell, the paywall, and every other Known Debt item.

## Implementation Details

### Behaviour

1. A direct request to `/privacy` or `/terms` on a static host serves `index.html` with HTTP 200,
   letting React Router render the document.
2. Hashed asset requests under `/assets/*` must **not** be rewritten — they resolve to real files.
   A blanket rewrite that catches them would break the app, so the fallback must apply only to
   paths with no matching file. Both `_redirects` and Vercel `rewrites` behave this way by
   default: a real file always wins over a rewrite.
3. Unknown paths still render the app's `NotFound` route. They return HTTP 200 rather than 404,
   which is the normal and accepted trade-off for a client-routed SPA.
4. The browser tab reads the document name rather than the app's marketing title, because these
   pages are public documents that a reviewer will open directly. The previous title is restored
   on unmount so returning to the app shell does not keep a stale title.

### Host Coverage

| File | Covers |
| --- | --- |
| `web/public/_redirects` | Netlify, Cloudflare Pages |
| `web/vercel.json` | Vercel |

Anything in `web/public/` is copied verbatim to `dist/`, so `_redirects` ships with the build.
`vercel.json` is read from the project root by Vercel and is not part of the bundle.

Three hosts is deliberate over-coverage: the human has not chosen one, all three have generous
free tiers, and each config is a few lines. Choosing for them would be the wrong call.

## Acceptance Criteria

1. `dist/_redirects` exists after a build.
2. `web/vercel.json` exists and contains a rewrite to `/index.html`.
3. `/privacy` and `/terms` still render in the running app.
4. The tab title reads the document name on those routes, and reverts on returning to `/`.
5. The legal page scrolls — it must, since the global stylesheet pins `body`.
6. Typecheck, lint, tests and build stay at their recorded baselines.

## Checks Run

- [x] `npm run build` — **pass**; `dist/_redirects` present in the output
- [x] `npx tsc --noEmit -p tsconfig.app.json` — **0 errors**
- [x] `npm run lint` — **0 errors, 10 warnings**, the same 10 as baseline
- [x] `npx vitest run` — **4 files, 56 tests, all pass**
- [x] Manual — `/privacy` renders, title "Privacy Policy — ModernBody", scrolls, reverts on `/`,
      no console errors

## Honest Limit of Verification

**The rewrite rules themselves cannot be verified on this machine.** They are instructions to a
host that does not exist yet. What is verified here is that the fallback file ships in `dist/`,
that the config is syntactically valid, and that the routes render. Whether the host honours the
rewrite can only be confirmed by loading `/privacy` on the deployed URL — which is the first
thing to check after deploying, and before pasting the URL into App Store Connect.

## Progress Tracker Update Required

Record the deployment-readiness state and the post-deploy check the human must perform.

## Decision Log Update Required

None. This implements the existing decision to host the documents on the web build; it decides
nothing new.
