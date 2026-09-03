# Unit Spec: Android — Losing Work, and the Empty-Key Error

## Status

- Status: **Implemented, UNVERIFIED** — needs a Gradle build
- Owner: Claude Code (code) / Human (verification)
- Created: 2026-09-03
- Last Updated: 2026-09-03

## Goal

Fix the Android findings where the app **loses the user's work**, plus the AI error it inherited
from the same bug iOS had.

## Defects

### 1. A single decode failure destroys everything — `AppViewModel.kt:54`

`load()` caught every exception and returned a fresh `AppData()`. The next `mutate` then wrote that
empty document **over the file**. One bad decode — a corrupt write, a schema change, a partially
flushed file — silently destroyed the entire meal history with no way back.

**Fix:** copy the unreadable file aside before continuing. Only the *first* failure is preserved:
after that the app is running on an empty document, so a later backup would overwrite the one copy
that still holds real data.

### 2. The last entry is lost if the ViewModel dies — `AppViewModel.kt:62`

Persistence is debounced 260 ms on `viewModelScope`. That scope is **cancelled as part of
clearing**, taking the pending write with it, so anything logged inside that window never reached
disk.

**Fix:** `onCleared()` flushes a pending write synchronously.

This is main-thread I/O, which the audit separately flags as a concern elsewhere in this file. The
trade-off is deliberate: it is one small file, and it is the last chance to keep the user's most
recent entry. Losing the entry is worse than a brief write.

### 3. Onboarding restarts at step 0 on any configuration change — `OnboardingScreen.kt:89`

All nine answers were held in `remember`, so a rotation — or the system recreating the activity
after time in the background — restarted the **mandatory six-step flow** from scratch with
defaults.

**Fix:** `rememberSaveable` throughout, with an explicit `enumSaver` for the three enums.

The default saver would probably have handled enums, since they are `Serializable` — naming the
saver explicitly means the behaviour does not depend on that inference holding.

### 4. Empty key reports the wrong error — `AiService.kt:80`

Identical to the bug iOS carried, fixed and **verified on Simulator** in unit 17. `toolkitUrl()`
falls back to the public gateway, so a keyless build looked configured, sent an empty bearer token,
collected a 401, and told the user analysis was temporarily unavailable and to try again later.

**Fix:** bail with `NotConfigured` before the request.

### 5. Cancellation was reported as a server error — `AiService.kt:174`

`catch (error: Exception)` swallowed `CancellationException` and rethrew it as `ServerError`. That
breaks structured concurrency — the parent scope believes the child completed normally — and tells
the user something went wrong when they simply navigated away.

**Fix:** rethrow `CancellationException` before the general catch.

## Scope

### Deliberately NOT fixed — the scan draft

`AppNavigation.kt:89` holds a completed `MealDraft` in `remember`, so a configuration change
discards a **finished AI analysis** the user has already paid for in time and quota.

`rememberSaveable` is the wrong tool here: `MealDraft` carries a **base64 photo**, and
saved-instance-state goes through a Bundle with a practical size ceiling. Putting a photo there
risks `TransactionTooLargeException` — **a crash, which is worse than the loss it would prevent.**

The right fix is to hoist the draft into the ViewModel, which survives configuration changes with
no Bundle involved. That means moving `MealDraft` out of `ui.navigation` into `data`, since a
ViewModel holding a UI-package type is the wrong dependency direction — a multi-file refactor with
import churn across the sheets.

**Not attempted without a compiler.** There are already several unverified native units queued;
adding a cross-package refactor whose failure mode is a broken build is a poor trade. It stays on
the list.

### Also out of scope

Main-thread image work in `ScanSheet.kt`, the synchronous store read at first composition, the
inert reminders screen, `streak()`'s cost, and the remaining drift findings.

## Verification — REQUIRED, not done here

**No Kotlin was compiled.** There is no Android SDK on this machine.

Reasoning behind each change: `File.copyTo` and `runCatching` are stdlib; `onCleared` is the
standard flush point and `super.onCleared()` is called last so the flush happens before teardown;
`rememberSaveable(stateSaver = ...)` is the documented overload for a `MutableState`;
`CancellationException` is imported from `kotlin.coroutines.cancellation`, which is the correct
package for common code. The `remember` import was checked and **kept** — `remember(goalWeight,
weight, rate)` at `OnboardingScreen.kt:561` still uses it.

### Re-test script

| Step | Expected |
| --- | --- |
| Corrupt `calzy-data-v1.json` by hand, relaunch | App starts empty, and `calzy-data-v1.json.unreadable` exists beside it holding the original |
| Log a meal, immediately background-and-kill the app | The meal is still there on relaunch |
| Start onboarding, reach step 3, rotate the device | Still on step 3, answers intact |
| Empty key in `Config.kt`, scan | *"AI scanning isn't available in this build yet."* — not "temporarily unavailable, try again later" |
| Start a scan, navigate away mid-request | No error toast; nothing reported as a failure |

## Decision Log Update Required

Yes — declining to make the draft saveable is a deliberate trade, and the reasoning should not have
to be re-derived.
