# ModernBody

An AI food scanner. Photograph a meal or describe it, and it returns an itemised
estimate of calories and macros, tracked against daily targets. All user data
stays on the device — no account, no sync.

Shipped as three implementations of one product:

| Path | Platform | Stack |
| --- | --- | --- |
| `web/` | Browser | React 19, Vite, TypeScript, Tailwind, shadcn/ui |
| `ios-calzy/` | iOS 18+ | Swift, SwiftUI |
| `android/` | Android | Kotlin, Jetpack Compose |

`Calzy` is the internal codename; **ModernBody** is the user-facing name.

---

## Run the web app

Requires **Node 20.12 or newer**. `.nvmrc` pins 24, which is the verified version.

```sh
cd web
nvm use          # optional, but avoids a confusing failure on Node 18
npm install
npm run dev      # http://localhost:8080
```

Everything works without configuration except meal scanning, which needs a model
key:

```sh
cp .env.example .env
# put a Google Gemini key in GEMINI_API_KEY
```

Then restart `npm run dev`. Without a key the app says *"AI scanning isn't
available in this build yet"* — that message is correct, not a bug.

`vite dev` does not normally run the `api/` functions a host executes in
production, so a dev-server plugin mounts `api/analyze.ts` at `/api/analyze`.
Local development therefore exercises the same code the deployment does.

### Checks

```sh
npm run typecheck   # strict TypeScript
npm run lint
npx vitest run      # 158 unit tests
npm run build
```

`npm run build` does **not** typecheck — Vite does not. `npm run typecheck` is
the gate. CI runs all four on every push touching `web/`.

The Playwright browser suite (`npm test` runs it too) needs a one-time
`npx playwright install` and has never been observed to pass. It is excluded
from CI for that reason.

---

## Run the iOS app

Needs **Xcode 16+** — the project uses file-system synchronized groups, which
older versions cannot open.

```sh
./ios-calzy/setup-config.sh
open ios-calzy/ModernBodyFoodScanner.xcodeproj
```

The repo **does not build from a clean clone without that first step.**
`Config.swift` is generated and gitignored, and two source files reference it.
The script creates it from a template and never overwrites an existing one.

Then in Xcode: **Signing & Capabilities → Team**, pick a simulator, run. A free
Apple ID is enough to run on your own device; the paid programme is only needed
to archive or upload.

iPhone-only, portrait-only, iOS 18 minimum.

Full walkthrough: [`submission/mac-setup-runbook.md`](submission/mac-setup-runbook.md).

---

## Run the Android app

```sh
./android/setup-config.sh
```

Then open `android/` in Android Studio. As on iOS, `Config.kt` is generated and
gitignored, and the project will not compile without it. The script derives the
package from `MainActivity.kt`, so it keeps working if the package is renamed.

---

## Environment variables

| Name | Where | Notes |
| --- | --- | --- |
| `GEMINI_API_KEY` | **Server only** | Read by `web/api/analyze.ts`. Never give it a `VITE_` or `EXPO_PUBLIC_` prefix. |
| `GEMINI_MODEL` | Server only | Optional. Confirm the current id against Google's docs. |
| `VITE_ANALYZE_ENDPOINT` | Client | Optional URL, not a credential. Only for a split deployment. |

Vite inlines any `VITE_`-prefixed variable into the client bundle. That is
exactly how the previous gateway key ended up shipped to every user, so the
prefix rule above is load-bearing rather than stylistic.

---

## Deploying

Any static host, with these settings:

| Setting | Value |
| --- | --- |
| Root directory | `web` |
| Build command | `npm run build` |
| Output directory | `dist` |

SPA fallback config is committed for Vercel (`vercel.json`) and for Netlify and
Cloudflare Pages (`public/_redirects`). Without it, `/privacy` and `/terms`
return 404 on a direct request — which is how the app stores fetch a privacy
policy URL.

After the first deploy, **load `/privacy` and run one scan** before trusting
either.

---

## How this project is run

Changes are made one written unit spec at a time. The source of truth is
[`context/`](context/):

| File | Contents |
| --- | --- |
| [`CLAUDE.md`](CLAUDE.md) | Entry point and rules for AI agents |
| [`PROJECT-BRAIN.md`](PROJECT-BRAIN.md) | How the workflow itself works |
| [`context/architecture.md`](context/architecture.md) | Stack, boundaries, invariants, known debt |
| [`context/execution-standards.md`](context/execution-standards.md) | Conventions and the verified check baselines |
| [`context/progress-tracker.md`](context/progress-tracker.md) | Current state, blockers, what is next |
| [`context/decision-log.md`](context/decision-log.md) | Decisions, with reasoning and reversal conditions |
| [`context/feature-specs/`](context/feature-specs/) | One spec per unit of work |
| [`submission/`](submission/) | App Store review material and the Mac runbook |

Start with `context/progress-tracker.md` — it is written for someone arriving
with no memory of the project.
