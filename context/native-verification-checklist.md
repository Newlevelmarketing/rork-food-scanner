# Native Verification Checklist

Five units of native code have been written but **never compiled** — there is no Xcode, no Android
SDK and no Java on the Windows machine they were authored on (verified, not assumed:
`ANDROID_HOME` unset, no SDK in any standard location, `java` not on PATH).

One pass through this confirms all five. Work top to bottom; the build steps come first so a
compile error surfaces before you spend time on behaviour.

| Unit | Platform | What it changed |
| --- | --- | --- |
| 17 | iOS | Empty-key error, camera unavailable state, clean retry — **already verified on Simulator** |
| 19 | iOS | `eraseAll()` deletes photos; camera start race |
| 20 | Android | Legal links; release signing |
| 25 | Android | Backup rules |
| 27 | Android | Decode-failure backup, `onCleared` flush, onboarding rotation, empty key, cancellation |

```sh
git pull
```

---

## Part 1 — iOS

```sh
./ios-calzy/setup-config.sh
open ios-calzy/ModernBodyFoodScanner.xcodeproj
```

**Build first (⌘B).** If it fails, stop and send the error — everything below assumes it compiles.

### 1.1 Photos are actually deleted (unit 19) — the one that matters most

This is the finding behind a **false claim in the shipped privacy policy**, so it is worth doing
carefully.

1. Log a meal **with a photo**, and add a progress photo in Progress.
2. Xcode → **Window → Devices and Simulators** → select the device → the app → **Download
   Container**. Open the `.xcappdata` package and look in `AppData/Documents/images/`.
3. Confirm the JPEGs are there.
4. In the app: **Settings → Delete everything**.
5. Download the container again and look at `Documents/images/`.

**Pass:** the directory is **empty** and still exists. Before this fix every JPEG remained.

### 1.2 The shutter no longer races the session (unit 19)

Needs a **physical device** — the Simulator never reaches `.running` at all, which is why unit 17's
Simulator pass could not have caught this.

Open Scan and watch the shutter button as the preview comes up.

**Pass:** the shutter stays **dimmed and untappable** until the live preview appears, then enables.

> Tapping fast and not crashing is weak evidence — the window is sub-second. The visible dimming is
> the real check.

### 1.3 Unit 17 regression check

- Empty key → *"Meal analysis is unavailable right now. You can still add a meal by searching the
  food database."*
- Simulator scan → **"Camera unavailable"** with library/database guidance
- Fail a scan → **Try again** → clean state, no frozen frame
- Try again → pick the **same** photo → analysis runs

### 1.4 Still unproven, if you have a moment

**Live-camera restart.** Fail a scan on a physical device with a working camera, tap Try again, and
confirm the preview comes back live. The Simulator cannot exercise this path, so it has never been
tested on any build.

---

## Part 2 — Android

```sh
./android/setup-config.sh
```

Then open `android/` in Android Studio, or:

```sh
cd android && ./gradlew assembleDebug
```

**Build first.** If Gradle sync or assembly fails, stop and send the error — the most likely
failure is a DSL slip in `build.gradle.kts`, not a logic error.

### 2.1 Release signing (unit 20)

```sh
cd android && ./gradlew assembleRelease
```

**Pass:** the artifact is named **`app-release-unsigned.apk`**. That is the point — previously it
was signed with the public debug keystore, which Play rejects and which lets any same-identity APK
update an installed app in place.

To sign for real, copy `keystore.properties.example` to `android/keystore.properties` and fill it
in. That file is gitignored.

### 2.2 Legal rows are hidden, not wrong (unit 20)

Open **Settings** and scroll to the bottom.

**Pass:** there is **no Support section at all**. It previously linked to `rork.app` — the
scaffolding vendor's documents, describing a different product — and to an unowned support address.

Filling in `Legal.PRIVACY_POLICY_URL` in `data/Legal.kt` makes the row reappear pointing at that
URL. Do that once the web build is deployed.

### 2.3 Backup rules (unit 25)

Hard to observe directly. The meaningful check is that **the build accepts the manifest** —
`android:dataExtractionRules` and `android:fullBackupContent` both resolve.

If you want to confirm behaviour: `adb shell bmgr backupnow com.rork.calzyandroid` should report
nothing transferred for the app's data.

### 2.4 Data-loss fixes (unit 27) — the important ones

**Decode failure preserves the original:**

```sh
adb shell "run-as com.rork.calzyandroid sh -c 'echo not-json > files/calzy-data-v1.json'"
```

Relaunch the app.

**Pass:** it starts empty **and** `files/calzy-data-v1.json.unreadable` exists beside it containing
`not-json`. Before this fix the original was simply overwritten on the next mutation.

```sh
adb shell "run-as com.rork.calzyandroid ls files/"
```

**Pending write survives the ViewModel dying:**

Log a meal, then **immediately** background and force-stop the app (within a second). Relaunch.

**Pass:** the meal is still there. The 260 ms debounce previously died with `viewModelScope`.

**Onboarding survives rotation:**

Fresh install, start onboarding, reach step 3 or 4, fill some answers, **rotate the device**.

**Pass:** still on the same step with the answers intact. It previously restarted at step 0 with
defaults.

### 2.5 AI error honesty (unit 27)

- **Empty key** in `Config.kt` → scan → *"AI scanning isn't available in this build yet."* — **not**
  "temporarily unavailable, try again later".
- **Navigate away mid-scan** → no error toast, nothing reported as a failure.

---

## What to send back

For each numbered check: pass, or the actual behaviour. For a build failure, the full error — a
Gradle DSL or Swift syntax slip is the likeliest failure mode and is usually a one-line fix.

## Known-unfixed, so do not report these as regressions

- **Android rotation during a scan result** still discards the draft. Deliberate — `MealDraft`
  carries a base64 photo and `rememberSaveable` would risk `TransactionTooLargeException`. See
  `context/decision-log.md`.
- **Android reminders** write a setting nothing reads; no notification is ever scheduled.
- **Android image work** runs on the main thread.
- Several **cross-platform drift** findings remain open.
