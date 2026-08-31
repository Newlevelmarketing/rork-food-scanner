# Architecture Context

> Written from evidence in the repository on 2026-08-20 by `/brain-onboard`. Every non-obvious
> claim cites the file that proves it. Sections marked `[UNKNOWN — needs human input]` were not
> established by the code and must not be guessed.

## Architecture Type

- Type: **Software** — a consumer nutrition-tracking app shipped as three parallel native/web
  implementations of one product.

## Product Identity

The repo is a [Rork](https://rork.com) multi-app export. `rork.json` declares three apps:

| Rork app | Path | Framework | Display name |
| --- | --- | --- | --- |
| Calzy | `ios-calzy` | swift | ModernBody |
| Calzy Web | `web` | web | — |
| Calzy Android | `android` | kotlin | — |

`Calzy` is the internal codename; `ModernBody` is the user-facing name (`rork.json`,
`ios-calzy/ModernBodyFoodScanner/`). The AI system prompt introduces itself as "ModernBody"
(`web/src/lib/ai.ts:69`).

## Stack

| Layer | Technology | Role | Evidence |
| --- | --- | --- | --- |
| Web app | React 19.2, TypeScript 5.9, Vite 8 | Browser surface | `web/package.json` |
| Web UI | Tailwind 3.4 + shadcn/ui on Radix | Component system | `web/tailwind.config.ts`, `web/components.json` |
| Web state | React Context (`AppStoreProvider`) | Single app store | `web/src/store/AppStore.tsx` |
| Web routing | react-router-dom 6, one real route | Shell only | `web/src/App.tsx:19-23` |
| Web charts | recharts | Progress dashboard | `web/package.json` |
| Web tests | Vitest 4 + Playwright browser mode | Unit + browser | `web/vitest.config.ts`, `web/vitest.browser.config.ts` |
| iOS | Swift / SwiftUI | Native iOS app | `ios-calzy/ModernBodyFoodScanner.xcodeproj` |
| Android | Kotlin / Jetpack Compose / Gradle KTS | Native Android app | `android/app/build.gradle.kts` |
| AI (web) | Own proxy → Google Gemini, direct | Nutrition estimation | `web/api/analyze.ts` |
| AI (native) | Rork toolkit gateway, **until unit 12** | Nutrition estimation | `NutritionAI.swift:90`, `AiService.kt:74-77` |
| Persistence | Device-local only | All user data | `web/src/store/AppStore.tsx:37` |

The native clients still use the gateway's fallback chain (`anthropic/claude-haiku-4.5`, then
`openai/gpt-5-mini`). The web proxy calls a single Gemini model, set by `GEMINI_MODEL`. Adding a
fallback chain to the proxy is possible but was not in unit 11's scope.

## System Boundaries

- `web/src/lib/` — pure domain logic (nutrition math, dates, i18n, image, types). No React
  imports. This is the layer the native apps mirror.
- `web/src/store/AppStore.tsx` — the single source of application state and the only writer to
  persistent storage.
- `web/src/pages/` — tab-level screens: Home, Progress, Settings, Onboarding.
- `web/src/features/` — modal sheets and flows: scan, describe, search, saved, exercise, meal
  result/detail/edit, paywall, share, settings sheets.
- `web/src/components/ui/` — **generated shadcn primitives. Do not hand-edit.** 47 files.
- `web/src/components/calzy/` — project-specific primitives (TabBar, Sheet, icons, HomeParts).
- `ios-calzy/ModernBodyFoodScanner/` — native iOS, same structure: `Models/`, `Store/`,
  `Services/`, `Views/`, `Utilities/`.
- `android/app/src/main/java/com/rork/calzyandroid/` — native Android, same structure:
  `data/`, `ui/screens/`, `ui/sheets/`, `ui/components/`.

**The three implementations are deliberate mirrors, not shared code.** `web/src/lib/types.ts:1`
declares "mirrors ios-calzy/Calzy/Models"; `web/src/index.css:7` declares "mirrors
ios-calzy/Calzy/Utilities/Theme.swift". iOS is the reference implementation.

## Data / Knowledge / Asset Model

**There is no account system, and no user data is stored server-side.** All user data is
device-local. Since unit 11 there *is* one server component — a stateless analysis proxy
(`web/api/analyze.ts`) that holds the model credential and persists nothing.

- **Web storage**: a single `localStorage` key, `calzy-data-v1`, holding the whole `AppData`
  object, written on a 220 ms debounce (`web/src/store/AppStore.tsx:37,139-151`).
- **Domain model** (`web/src/lib/types.ts`): `AppData` = `profile`, `meals`, `exercises`,
  `water`, `weights`, `photos`, `saved`.
- **Photos**: meal and progress photos are stored as base64 JPEG data URLs inside that same
  key. Full-resolution images (≤2.6 MB) are only sent for analysis; a 420 px thumbnail at
  quality 0.72 is what gets persisted (`web/src/lib/image.ts:40-67`).
- **Bundled reference data**: `web/src/data/foods.json` — 100 foods for offline search.
  `web/src/data/strings.json` — 66 KB of UI translations.
- **Localization**: 32 languages including RTL (`web/src/lib/i18n.ts`, `Index.tsx:38-39`).

## Access / Permissions / Ownership Model

- Repo owner: `Newlevelmarketing` (private GitHub repo).
- No user accounts, no roles, no server-side authorization — the app has no concept of a
  remote user.
- `[UNKNOWN — needs human input]` — who besides the owner may approve changes.

## Integration Points

| System | Connected To | Purpose | Evidence |
| --- | --- | --- | --- |
| **Own proxy → Google Gemini** | web | Vision + text nutrition analysis | `web/api/analyze.ts` |
| Rork toolkit gateway | iOS, Android **(until unit 12)** | Vision + text nutrition analysis | `ios-calzy/.../NutritionAI.swift:90`, `android/.../AiService.kt:74-77` |
| Google Fonts | web | Nunito webfont | `web/src/index.css:1` |

Environment variable **names** in use (values are never committed):

- **Server-side only:** `GEMINI_API_KEY`, `GEMINI_MODEL` — read in `web/api/analyze.ts`. These must
  never be given a `VITE_` or `EXPO_PUBLIC_` prefix; Vite inlines both into the client bundle,
  which is precisely how the previous key leaked.
- **Client-side:** `VITE_ANALYZE_ENDPOINT` (optional, a URL, not a credential).
- **Native, until unit 12:** `EXPO_PUBLIC_TOOLKIT_URL`, `EXPO_PUBLIC_RORK_TOOLKIT_SECRET_KEY` in
  the Rork-generated `Config.swift` / `Config.kt`.

## Critical Flows

### Flow 1: Scan a meal

1. User opens the scan sheet from the Home tab (`Index.tsx:61-65`).
2. Camera frame or picked image is downscaled down a resize/quality ladder until it fits the
   2.6 MB budget (`lib/image.ts:40-61`).
3. The data URL is POSTed to `/api/analyze` (`lib/ai.ts`). The proxy attaches the system prompt
   — which lives server-side, so it is neither shipped to users nor duplicated per platform — and
   calls Gemini with `responseMimeType: "application/json"`.
4. The first balanced JSON object is extracted from the reply, tolerating markdown fences
   (`lib/ai.ts:92-105`).
5. `isFood === false` or an empty item list raises `notFood` (`lib/ai.ts:179-181`).
6. The result becomes a `MealDraft`; the user confirms or corrects it, and it is written to the
   store as a `MealEntry` with a persisted thumbnail.

### Flow 2: Onboarding to daily targets

1. `hasOnboarded === false` gates the whole shell to the onboarding screen (`Index.tsx:43`).
2. The profile is collected, and `completeOnboarding` seeds the first weight entry
   (`AppStore.tsx:157-166`).
3. Targets derive from Mifflin–St Jeor BMR × activity multiplier, adjusted by goal direction at
   7700 kcal per kg, floored at 1200 kcal (`lib/nutrition.ts:62-102`).
4. Macros: protein 1.8 g/kg (2.0 for `gain`), fat at 27% of calories, carbs as the remainder.

## Invariants

> **Proposed from observed behavior — pending human ratification.** These are the rules the code
> currently upholds. Confirm or strike each one before treating it as binding.

1. **No user data is stored off the device.** The only outbound request carrying user content is
   the meal-analysis call, which since unit 11 goes to this project's own proxy
   (`web/api/analyze.ts`) and on to Google Gemini. The proxy is stateless: it holds the model
   credential, forwards one photo or description, and stores nothing. No accounts, no analytics
   upload, no backend persistence.

   > Restated in unit 11. The original wording said "no backend", which the proxy would have
   > broken. The property that actually matters — nothing about a user is *retained* anywhere but
   > their device — is preserved. See `context/decision-log.md`.
2. **Full-resolution photos are never persisted.** Only `toThumbnail` output enters storage;
   full-size images exist in memory long enough to reach the AI gateway.
3. **The three platforms stay in sync as one product.** A change to the domain model in
   `web/src/lib/types.ts` has a matching change in `ios-calzy/.../Models/` and
   `android/.../data/Models.kt`, or an explicit decision recording why not.
4. **`web/src/components/ui/**` is generated and not hand-edited.** Project-specific components
   go in `web/src/components/calzy/`.
5. **No hardcoded colors.** Every color resolves through the token set in `web/src/index.css`,
   `Theme.swift`, or `Theme.kt`.
6. **Nutrition math lives in the domain layer.** `lib/nutrition.ts` and its native mirrors own
   every calorie, macro, BMR, and BMI calculation. Components read, they do not compute.

## Protected Paths

- `web/node_modules/`, `web/dist/`, `android/build/`, `android/.gradle/`, Xcode `DerivedData/`
- `web/bun.lock` — unless the unit is explicitly a dependency change
- `.env`, `Config.swift`, `android/app/src/main/java/**/Config.kt` — generated, gitignored,
  secret-bearing. Never read values, never commit.
- `web/src/components/ui/**` — generated shadcn primitives
- `ios-calzy/ModernBodyFoodScanner.xcodeproj/project.pbxproj` — regenerate, do not hand-merge
- `web/src/data/strings.json` — 66 KB of machine-generated translations across 32 locales

## Known Tradeoffs

| Decision | Benefit | Tradeoff |
| --- | --- | --- |
| Three hand-mirrored implementations instead of shared code | Each platform is fully native, no bridge overhead | Every feature is built three times and can drift |
| Device-local storage, no backend | No accounts, no privacy surface, no hosting cost | No sync, no backup; clearing site data destroys everything |
| Whole `AppData` in one storage key | Trivial load/save | Quota-bound, and one corrupt write loses all history |

## Known Debt / Landmines

| Area | Problem | Risk If Touched |
| --- | --- | --- |
| ~~`web/src/lib/ai.ts`~~ **resolved for web (unit 11)**; still live in `NutritionAI.swift:184` and `AiService.kt:80-81` | The toolkit API key was a **client-side credential** on all three platforms, sent as a `Bearer` token straight from the client. The web client now posts to `web/api/analyze.ts`, holds no credential, and `envPrefix` no longer exposes `EXPO_PUBLIC_*`. **The two native apps still send the key** until unit 12. | Until unit 12 ships, the key remains extractable from both app binaries. Rotate it once all three clients are migrated — anything already shipped has leaked it. |
| `ios-calzy/.gitignore`, `android/.gitignore` | `Config.swift` and `Config.kt` are generated by Rork and gitignored. | **The native apps cannot be built from a fresh clone.** Only `web` is reproducible from git alone. |
| `web/src/lib/ai.ts:56,65` | `toolkitURL` defaults to the public gateway, so `isAIConfigured` is `true` even with no key set. A missing key produces a 401 → `authError` ("please reload the page") instead of the honest `notConfigured` message. | A fresh clone silently presents AI as available, then fails misleadingly. |
| `web/src/store/AppStore.tsx:143-147` | A quota overflow is caught and `console.warn`ed. Photos are base64 in the same key as everything else. | Once the quota is hit, persistence stops silently — the user keeps logging and loses it all on reload. No user-facing signal. |
| `web/src/features/Paywall.tsx:52,151` | The paywall toggles `isPro` locally and labels its prices "illustrative in this preview build". | There is no billing integration. Any work assuming real entitlements is building on a stub. |
| `web/package.json` + `web/bun.lock` | The lockfile is bun's; bun is not installed on this machine and there is no `package-lock.json`. | npm installs resolve unpinned, so local builds are not reproducible against CI or against Rork. |
| Repo history (2 commits: "Initial commit", "New version from Rork") | The tree appears to be regenerated wholesale by Rork rather than hand-edited. | **A future Rork export may overwrite hand-written changes.** See Open Questions — this decides whether hand-editing this repo is safe at all. |

## Open Architecture Questions

- Is Rork still the source of truth? If the app is re-exported from Rork, hand edits made here
  are lost. This must be settled before any unit is built.
- Should the AI key move behind a server proxy, accepting that it introduces the project's first
  backend?
- Do all three platforms need to stay in lockstep, or is one the primary shipping target and the
  others secondary?
- Is `localStorage` the intended permanent storage for web, or a placeholder? IndexedDB would
  remove the quota ceiling for photos.
- `[UNKNOWN — needs human input]` — is there a CI pipeline, a release process, or an app-store
  presence? Nothing in the repo describes one.
