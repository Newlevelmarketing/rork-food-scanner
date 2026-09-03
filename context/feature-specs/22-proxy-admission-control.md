# Unit Spec: Admission Control for the Analysis Proxy

## Status

- Status: **Complete — verified against a live endpoint**
- Owner: Claude Code
- Created: 2026-09-03
- Last Updated: 2026-09-03

## Goal

Stop `/api/analyze` being an unauthenticated, unmetered relay to the operator's Gemini key.

## Why This Matters

**This is a gap in unit 11's own spec, not deferred work.** Unit 11 moved the model credential
server-side, which removed key *extraction*. It introduced an open endpoint in its place, and the
spec's Out of Scope list never mentions abuse control in either direction — so it was neither
solved nor knowingly postponed.

The audit's `security` lens found it: the handler's only gates were method, a missing key, and body
validation. No rate limit, no origin check, no auth, nothing in `vercel.json`, no middleware. Once
deployed, anyone who learned the URL could POST in a loop and spend the quota until real users
started receiving `rateLimited`.

Also fixed here, from the same lens: `content` for `kind: "text"` had **no upper bound**, while the
image path was capped. An unbounded prompt is a cost and latency problem.

## Implementation

All three checks live in `api/_core.ts` as pure functions so they are unit-testable, with the
handler owning the state and the clock.

1. **Origin allowlist** — `ALLOWED_ORIGINS`, comma-separated.
2. **Fixed-window rate limit** — 20 requests per minute per client.
3. **Text length cap** — `MAX_TEXT_LENGTH = 2000`, returning 413.

### Two deliberate design choices

**A null Origin is allowed.** Native apps send no `Origin` header and are intended callers of this
endpoint, especially once unit 12 points them here.

**An unset allowlist fails open, with a warning log.** Failing closed by default would break the
first deployment before anyone had a chance to configure it. The trade-off is recorded in
`.env.example` and the README as *set this before deploying*.

## The Honest Limit — this is a speed bump, not protection

**The rate-limit store is per-instance memory.** Serverless hosts run many instances and recycle
them, so a caller spreading requests across instances gets a fresh bucket each time. `clientKey`
reads `x-forwarded-for`, which is spoofable.

This stops a naive loop from one client — the common case — and nothing more. **Durable protection
needs a shared store (Vercel KV, Upstash) or a WAF rule**, and that is a real follow-up, not a
nicety. The limitation is documented in the code itself so nobody mistakes it for a solved problem.

## A Bug Found in Unit 11a While Verifying This

The first live test showed rate limiting working but **origin rejection silently doing nothing**.

The cause was in the dev middleware I wrote in unit 11a: it constructed a new `Request` with
`headers: { "Content-Type": "application/json" }` **hardcoded**, dropping `Origin` and
`x-forwarded-for`. The handler could not see them, so local development could not exercise its own
admission control — defeating the point of running the real handler locally.

Fixed: the middleware now forwards the incoming headers, skipping hop-by-hop and `content-length`
(the body is re-framed by `Request`, so a stale length is a mismatch).

**Worth noting how this surfaced.** Rate limiting appeared to work *because* headers were dropped —
every caller fell back to the same `"unknown"` key and shared one bucket. A passing test hid a
broken one.

## Scope

### In Scope

- `web/api/_core.ts`, `web/api/analyze.ts`
- `web/vite.config.ts` — header forwarding
- `web/src/test/analyze-core.test.ts`
- `web/.env.example`, `README.md`

### Out of Scope

- A shared-store rate limiter — needs a KV provider and an account decision.
- Authenticating the native clients. Unit 12 should give them a signed nonce rather than relying on
  a null Origin being permitted forever.
- Every other audit finding.

## Checks Run

- `npm run typecheck` — **0 errors**
- `npm run lint` — **0 errors, 9 warnings**, unchanged
- `npx vitest run` — **173 tests** (up from 158), all pass
- `npm run build` — **pass**; neither `GEMINI_API_KEY` nor `ALLOWED_ORIGINS` appears in `dist/`

### Live endpoint verification

With `ALLOWED_ORIGINS=https://app.example`:

| Request | Expected | Actual |
| --- | --- | --- |
| `Origin: https://evil.example` | 403 | **403** `forbiddenOrigin` |
| `Origin: https://app.example` | past the gate | **400** validation |
| No `Origin` (native app) | past the gate | **400** validation |
| `Origin: https://app.example.evil.com` | 403, no prefix match | **403** |
| 25 rapid requests, limit 20 | 429 from the 21st | **20 × 400, then 5 × 429** |

## Decision Log Update Required

Yes — the fail-open default and the per-instance limitation are both trade-offs a future reader
should not have to re-derive.
