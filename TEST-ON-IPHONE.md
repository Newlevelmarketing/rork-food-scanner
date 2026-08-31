# Test ModernBody on a real iPhone

Hand this to anyone with a Mac and an iPhone. It assumes no knowledge of the project.

**You need:** macOS with **Xcode 16 or newer**, an **iPhone on iOS 18+**, a USB cable, and any
Apple ID. A free Apple ID is enough — the paid Developer Program is not required to install on your
own phone.

---

## 1. Get the code

```sh
git clone https://github.com/Newlevelmarketing/rork-food-scanner.git modernbody
```

```sh
cd modernbody && git checkout chore/project-brain-and-review-prep
```

> **The branch matters.** `main` does not have this work.

<details>
<summary>Clone fails with "Repository not found"</summary>

The repo is private. Ask the owner to add your GitHub account as a collaborator, then retry. A
404 here means "no access", not "wrong URL".
</details>

## 2. Create the one missing file

```sh
./ios-calzy/setup-config.sh
```

The project **will not compile without this.** `Config.swift` holds API settings, is gitignored,
and two source files reference it. The script writes it from a template and never overwrites an
existing one.

<details>
<summary>"Permission denied"</summary>

```sh
sh ios-calzy/setup-config.sh
```
</details>

## 3. Open it

```sh
open ios-calzy/ModernBodyFoodScanner.xcodeproj
```

<details>
<summary>Xcode says the project is damaged, or won't open it</summary>

Your Xcode is too old. The project uses file-system synchronized groups, which need **Xcode 16+**.
Update from the App Store.
</details>

## 4. Set signing — and change the bundle ID

In Xcode's left sidebar click the blue **ModernBodyFoodScanner** icon → **Signing & Capabilities**.

1. Set **Team** to your Apple ID. Leave "Automatically manage signing" ticked.
2. **Change the Bundle Identifier** to something unique to you, e.g. `com.yourname.modernbody`.

> **Step 2 is not optional for a second machine.** The committed bundle ID is already registered to
> the project owner's team, and Apple will not let a different account register the same one. You
> will get *"Failed to register bundle identifier"* if you skip it.

Prefer the command line? From the repo root, before opening Xcode:

```sh
sed -i '' 's/app\.rork\.kffuebxmbishdc4eli446/com.yourname.modernbody/g' ios-calzy/ModernBodyFoodScanner.xcodeproj/project.pbxproj
```

**Do not commit that change.** To undo it afterwards:

```sh
git checkout ios-calzy/ModernBodyFoodScanner.xcodeproj/project.pbxproj
```

## 5. Run on the Simulator first

Pick any **iPhone 16/17, iOS 18+** simulator from the toolbar and press **⌘R**.

Do this before touching your phone — it separates "does it build" from "is signing right".

**Expected:** onboarding appears — *"Point your camera at any meal."*

## 6. Run on your iPhone

Plug the phone in, select it in the device dropdown, press **⌘R**.

<details>
<summary>"Untrusted Developer" on the phone</summary>

**Settings → General → VPN & Device Management** → your Apple ID → **Trust**. Then launch again.
</details>

<details>
<summary>Xcode cannot see the phone</summary>

Unlock the phone, tap **Trust This Computer**, and try a different cable — many charge-only cables
carry no data.
</details>

<details>
<summary>"Developer Mode disabled"</summary>

**Settings → Privacy & Security → Developer Mode** → on → reboot.

The toggle **only appears after a development build has been installed once**, so run from Xcode
first, then look for it.
</details>

<details>
<summary>"Failed to register bundle identifier"</summary>

You skipped step 4.2. Change the Bundle Identifier to something unique and run again.
</details>

<details>
<summary>Deployment target error</summary>

The phone is on iOS 17 or older. This app requires **iOS 18+**. Use a newer device.
</details>

---

## What a working install looks like

| Check | Expected |
| --- | --- |
| Launch | Onboarding: *"Point your camera at any meal."* |
| Complete onboarding | Home dashboard with calorie and macro rings |
| Force-quit and reopen | Your profile and any logged meals are still there |
| Tap **Scan** | Camera permission prompt, then a live preview |
| Settings tab | Language, reminders, Privacy Policy, Terms, Delete All Data |

## Two things that look broken but are not

**"Meal analysis is unavailable right now. You can still add a meal by searching the food
database."** — Correct. There is no API key in the template config. The app is working; AI
estimation just needs a key the project owner supplies. Everything else, including manual meal
entry via **Search**, works.

**"Camera unavailable" on the Simulator** — Correct. Simulators have no camera. On a real phone you
should get a live preview.

## What is genuinely worth reporting

- Any crash
- Onboarding not appearing, or not saving
- Data lost after force-quit
- Camera permission prompt never appearing **on a real device**
- Live preview staying black **on a real device**
- Wrong or obviously implausible calorie targets
- Anything visually broken — cut-off text, overlapping elements

Include your **iPhone model and iOS version**, and a screenshot or screen recording.

## Note on free Apple IDs

A free account signs builds for **7 days**, after which the app stops launching until you rebuild
from Xcode. That is normal and not a bug in the app.
