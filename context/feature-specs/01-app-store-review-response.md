# Unit Spec: App Store Review Response — Guideline 2.1

## Status

- Status: **In Progress** — draft complete, blocked on human-only inputs
- Owner: Claude Code (drafting) / Human (device testing, recording, submission)
- Created: 2026-08-20
- Last Updated: 2026-08-20

## Goal

Produce everything needed to reply to Apple's Guideline 2.1 "Information Needed" message of
2026-08-10 for ModernBody 1.0.0, so the iOS submission can continue review.

## Why This Matters

This is a **documentation and submission unit, not a code unit.** That matters more than usual
here: the unresolved question of whether Rork re-exports overwrite hand edits
(`decision-log.md`) blocks code work, but does not block this. It is the one piece of valuable
work that can proceed with that question still open.

## Scope

### In Scope

- Drafting the seven App Review Information answers Apple requested
- A screen-recording shot list covering Apple's item 1
- Identifying submission risks that would cause a second rejection
- Listing the facts only the human can supply

### Out of Scope

- **Any change to application code.** Nothing in `ios-calzy/`, `android/` or `web/` is touched.
- Recording the video — requires a physical device
- Editing App Store Connect — the human submits
- Fixing the client-side AI key, the paywall stub, or any other Known Debt item
- Implementing StoreKit. iOS ships with no IAP and that is correct for this submission.

## Relevant Context Files

- `CLAUDE.md`
- `context/architecture.md` — external services, permissions, data model
- `context/project-overview.md` — feature inventory
- `context/ui-brand-context.md` — copy voice
- `context/decision-log.md` — the no-backend and client-key decisions

## Implementation Details

### Files Created

- `submission/app-review-information.md` — the deliverable. Three parts: paste-ready notes,
  recording shot list, pre-send risk checklist.

### Evidence Base

Every factual claim traces to source:

| Claim | Evidence |
| --- | --- |
| iPhone only, iOS 18.0+, portrait, v1.0.0 | `project.pbxproj:325,411,417,426` |
| Bundle `app.rork.kffuebxmbishdc4eli446` | `project.pbxproj:418` |
| Camera / photo-library purpose strings present and specific | `project.pbxproj:405-407` |
| No IAP, no subscriptions | `Legal.swift:177-180`; no StoreKit import anywhere in `ios-calzy/` |
| No user-generated content platform | `Legal.swift:170-175` |
| No account system | `Legal.swift:54`; no auth code in repo |
| AI chain: Rork gateway → Gemini 3 Flash, Claude Haiku 4.5, GPT-5 Mini | `web/src/lib/ai.ts:53-63`, `NutritionAI.swift:90`, `AiService.kt:74-81` |
| Only photo/text/language transmitted | `Legal.swift:74-84`, `lib/ai.ts:127-136` |
| No analytics, ads, tracking SDKs | `Legal.swift:90-95`; dependency list in `package.json` |
| 32 locales incl. RTL | `lib/i18n.ts`, `strings.json` |
| Wellness disclaimers | `Legal.swift:32-44,156-162` |
| Notification permission is opt-in | `ReminderService.swift:33-39` |
| `s.subscription` string unused on iOS | `strings.json` has the key; `SettingsView.swift` never references it |

### Findings That Changed the Answer

1. **iOS has no paywall.** Web and Android ship `Paywall.tsx` / `PaywallSheet.kt`; iOS has no
   equivalent view. Apple's item 1 asks for purchase flows — the correct answer is that none
   exist, and the recording should demonstrate that rather than assert it.
2. **The purpose strings already satisfy Guideline 5.1.1** — they state both the reason and how
   the data is used, which is what Apple's "common issues" note asks for. No change needed.
3. **`Legal.swift` ships with empty `privacyPolicyURL`, `termsOfUseURL` and `supportEmail`.**
   Safe in-app, but App Store Connect requires Privacy Policy and Support URLs as metadata. This
   is the highest-probability cause of a second rejection.
4. **The privacy policy asserts photos are not used to train models.** That is a claim about
   Rork's gateway and its downstream providers, not about code in this repo. It needs written
   backing from Rork.

## Acceptance Criteria

1. ~~Every one of Apple's seven items has a drafted answer, or an explicit `[CONFIRM]` where
   only the human can answer.~~ **Done.**
2. ~~Each factual claim traces to a file in the repository.~~ **Done.**
3. ~~A shot list exists covering launch, core flows, and every permission prompt.~~ **Done.**
4. The seven `[CONFIRM]` items are answered by the human. **Blocked.**
5. The recording is captured on a physical device. **Blocked.**
6. The reply is sent in App Store Connect. **Blocked.**

## Checks to Run

- [x] No tracked application file modified — `git status` shows only additions
- [x] Every claim in the deliverable verified against source
- [ ] Human review of the `[CONFIRM]` items
- [ ] Recording captured

No build, lint or test check applies — this unit changes no code.

## Progress Tracker Update Required

Done — see `context/progress-tracker.md`.

## Decision Log Update Required

None yet. If the human decides to soften the no-training privacy claim, or to add web-hosted
legal documents, those are decisions and get logged.
