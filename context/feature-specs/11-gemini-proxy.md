# Unit Spec: Replace the Rork Gateway with a Gemini Proxy

## Status

- Status: **Complete** — web only; native follows in unit 12
- Owner: Claude Code
- Created: 2026-08-22
- Last Updated: 2026-08-22

## Result

**158 tests across 7 files, up from 118. Typecheck 0 errors, lint unchanged at 9 warnings, build passes.**

Verified against the acceptance criteria:

- `toolkit.rork.com` appears nowhere in `web/src` or `web/api`.
- The built bundle contains **zero** occurrences of "rork" and no `EXPO_PUBLIC` reference.
- The bundle calls `/api/analyze`.
- **The system prompt is no longer shipped to users** — 0 occurrences of it in `dist/`. It now
  exists once, server-side, instead of three times across three clients.
- Bundle got slightly *smaller*: 1,009.54 kB → 1,007.63 kB.

Not verified, as stated up front: no end-to-end call to Gemini. See the limit below.

## Goal

Stand up a serverless proxy that calls Google Gemini directly, and switch the **web** client to
it, so nothing at runtime depends on `toolkit.rork.com` and no API key ships to a client.

## Why This Matters

Two problems collapse into one fix.

**Independence.** `toolkit.rork.com` is the only runtime dependency on Rork. Three files call it:
`web/src/lib/ai.ts`, `NutritionAI.swift`, `AiService.kt`. Nothing else in the product needs Rork
to be running.

**The exposed credential.** `EXPO_PUBLIC_RORK_TOOLKIT_SECRET_KEY` is sent as a `Bearer` token from
the client on all three platforms, and `vite.config.ts:23` deliberately exposes `EXPO_PUBLIC_*` to
the bundle. It ships inside the web JS and both app binaries, extractable by anyone. This has been
the top Known Debt item since onboarding, and it cannot be fixed without a server.

Going independent and fixing the key are the **same work**. Doing them separately would mean
building the proxy twice.

## Scope

### In Scope

- `web/api/analyze.ts` — the proxy, calling Gemini directly
- `web/src/lib/ai.ts` — the web client posts to the proxy; no gateway URL, no key
- `web/src/vite-env.d.ts` — drop the Rork env vars
- `web/src/test/ai.test.ts` — updated for the new contract

### Out of Scope — separate units

- **`NutritionAI.swift` and `AiService.kt`.** They keep calling the Rork gateway until unit 12.
  Both platforms stay working throughout; nothing breaks mid-migration.
- Bundle ID and Android package rename — units 14 and 15.
- `Config.swift` / `Config.kt` replacement — unit 13.
- `rork.json` and cosmetic traces — unit 16.
- The paywall, localStorage quota, and every other open item.

## Design

### The system prompt moves server-side

Today the prompt lives in all three clients and must be edited three times, kept in sync by hand,
and is shipped to every user. Moving it into the proxy means **one copy**, changeable without
releasing an app.

### Contract

`POST /api/analyze`

```
{ "kind": "image" | "text", "content": string, "jesterMode": boolean, "language": string }
```

For `kind: "image"`, `content` is the data URL the client already produces. The proxy splits off
the base64 payload, because Gemini wants `inline_data`, not a data URL.

**The 200 response is the exact JSON shape the clients already parse**, so `parseAnalysis`, the
zod schema and every downstream consumer are untouched.

### Error mapping is preserved

The existing `NutritionAIErrorKind` taxonomy and its user-facing copy stay exactly as they are.
The proxy maps Gemini failures onto the status codes the client already understands:

| Condition | Status | Client kind |
| --- | --- | --- |
| `GEMINI_API_KEY` unset | 503 | `notConfigured` |
| Gemini 401/403 | 401 | `authError` |
| Gemini quota exhausted | 402 | `insufficientBalance` |
| Body or image too large | 413 | `imageTooLarge` |
| Gemini 429 | 429 | `rateLimited` |
| Anything else | 502 | `serverError` |

This is why unit 04's behaviour survives: an unconfigured deployment still reports
"AI scanning isn't available in this build yet", but the **server** now decides that, which is the
only place that can honestly know.

### `isAIConfigured` changes meaning

The client no longer holds a credential, so it cannot know whether AI works — only the server can.
The flag becomes "an endpoint is configured", which is true by default since the proxy is
same-origin. Unit 04's tests are updated to assert the new contract: an unconfigured **server**
yields `notConfigured`.

This is a deliberate behaviour change, not a test being bent to fit.

## Implementation Details

1. `web/api/analyze.ts` — a Web-standard `Request`/`Response` handler. Vercel treats `api/` at the
   project root as functions and ignores files prefixed `_`. Netlify Edge uses the same signature;
   Cloudflare Pages needs a three-line adapter, documented in the file.
2. Model id from `GEMINI_MODEL`, defaulting to a documented value. **The exact current model id
   must be confirmed against Google's docs before deploying** — it is env-configurable precisely
   so that is a config change, not a code change.
3. `generationConfig.responseMimeType: "application/json"` so Gemini returns JSON natively rather
   than prose that needs unfencing. `extractJSON` stays as a belt-and-braces fallback.
4. Only `GEMINI_API_KEY` is read server-side. It must **never** carry a `VITE_` or `EXPO_PUBLIC_`
   prefix, or `vite.config.ts:23` would inline it into the bundle and recreate the exact bug this
   unit exists to fix.

## Acceptance Criteria

1. No `toolkit.rork.com` reference remains in `web/`.
2. No API key is readable from the built bundle. Verified by grepping `dist/`.
3. The client posts to the proxy with the documented contract.
4. Error kinds still map correctly from status codes.
5. `parseAnalysis` and the zod schema are unchanged.
6. Typecheck, lint and build stay at baseline; tests pass.

## Honest Limit of Verification

**No end-to-end call to Gemini can be made here.** There is no API key on this machine, and
`vite dev` does not serve `/api/*` — that needs `vercel dev` or a deploy.

What will be verified: the pure logic (data-URL splitting, error mapping, prompt building) by
unit test; that the client sends the right request; that the bundle contains no secret; and that
typecheck, lint and build pass.

What will not: that Gemini accepts the request body. **First deploy, then scan once**, before
trusting it.

## Progress Tracker Update Required

Record the new architecture, the env var, and the model-id caveat.

## Decision Log Update Required

Yes, and it is significant: this introduces the project's **first backend**, which contradicts the
proposed invariant that all user data stays on the device with no server. That invariant needs
restating rather than quietly breaking.
