# App Review Information — ModernBody

Response to **Guideline 2.1 — Information Needed**, received 2026-08-10.

App: ModernBody · Bundle ID `app.rork.kffuebxmbishdc4eli446` · Version 1.0.0 (build 1)
iPhone only (`TARGETED_DEVICE_FAMILY = 1`) · Minimum iOS 18.0 · Portrait only

Everything below is drawn from the shipping source. Items marked **`[CONFIRM]`** need your
input before sending — they are facts only you have.

---

## Part 1 — Paste into App Store Connect → App Review Information → Notes

> Copy from here down. Keep the numbering; it maps to Apple's request.

### 1. Screen recording

Attached / provided separately. See Part 2 of this document for what to record.

### 2. Devices and operating systems tested

**`[CONFIRM]`** — Apple wants specific models and OS versions. For example:

> Tested on physical devices before submission:
> - iPhone 15 Pro — iOS 26.x
> - iPhone 13 — iOS 26.x
>
> The app requires iOS 18.0 or later and is built for iPhone only.

Fill in the devices you actually tested on. Do not list a device you did not run the build on —
this is exactly the claim Apple spot-checks.

### 3. Purpose and target audience

> ModernBody is a personal nutrition-awareness app for adults who want to understand what they
> eat without the friction of manual food logging.
>
> **The problem it solves:** conventional calorie trackers require the user to search a database
> and enter every item and portion by hand. Most people stop within a few days. ModernBody
> replaces that with a photo — the user takes a picture of a meal, and the app returns an
> itemised estimate of calories, protein, carbohydrates and fat, which the user can review and
> correct before saving.
>
> **Target audience:** adults tracking their nutrition for general wellness — weight
> management, higher protein intake, or simply awareness of what they eat. **`[CONFIRM]`**
>
> **Value provided:** during onboarding the user enters height, weight, activity level and
> goal. The app calculates daily calorie and macronutrient targets using the Mifflin–St Jeor
> equation and tracks progress against them, alongside water intake, exercise, body weight and
> optional progress photos.
>
> ModernBody is a general wellness app. It does not diagnose, treat, cure or prevent any medical
> condition, and it presents this disclaimer during onboarding and in its legal section.

### 4. Setting up and accessing the app's main features

> **No account, login or credentials are required.** ModernBody has no account system, no sign-up
> and no server-side user records. There is nothing to provision for the reviewer — the app is
> fully functional on first launch.
>
> **First launch** presents a short onboarding flow: name, sex, birth year, height, current
> weight, goal weight, activity level, goal direction and weekly rate. Completing it reveals the
> main app. No entry is validated against a remote service.
>
> **Reaching each core feature from the Home tab:**
>
> | Feature | How to reach it |
> | --- | --- |
> | Scan a meal by photo | Home → **Scan** → allow camera → capture a meal → review the estimate → Save |
> | Describe a meal in words | Home → **Type** → enter e.g. "two scrambled eggs and toast" → Save |
> | Search the built-in food database | Home → **Search** → search the 100-item offline list |
> | Re-log a saved meal | Home → **Saved** |
> | Log exercise | Home → **Exercise** |
> | Log water | Tap the water card on Home |
> | Log weight | Tap the weight card on Home |
> | Review or correct a meal | Tap any meal in the day's list → Edit |
> | Progress, streaks, weight history | **Progress** tab |
> | Language, reminders, legal documents, erase data | **Settings** tab |
>
> **To test the scanner without a physical meal**, photograph any picture of food on another
> screen — the estimator works from any image containing food. If the image contains no food the
> app returns "We couldn't find any food in that photo", which is expected behaviour rather than
> an error.
>
> **In-app purchases.** ModernBody Pro is an optional auto-renewing subscription, offered in
> weekly, monthly and yearly terms and sold through the App Store. Purchases are validated by
> RevenueCat. The subscription screen is reached from the **Settings** tab → **ModernBody Pro**,
> and shows the price, billing period and currency of each term in the reviewer's local currency
> before any purchase is confirmed. Auto-renewal, cancellation and refund handling are stated in
> section 7 of the in-app Terms of Use. Every feature listed in the walkthrough above works
> without a subscription.
>
> **No user-generated content platform.** There are no profiles, feeds, comments, messaging or
> any mechanism by which one user's content becomes visible to another. Meal names and notes are
> stored only on the user's device. This is stated in section 6 of the in-app Terms of Use.
>
> **Permission prompts the reviewer will encounter:**
>
> - **Camera** — when opening the meal scanner. Declining leaves every other feature usable;
>   meals can still be added from the photo library, by description, or by search.
> - **Photo Library** — when picking an existing photo, or saving a progress photo.
> - **Notifications** — only if the reviewer enables meal reminders in Settings. Off by default.
>
> The app requests no location, contacts, health data or App Tracking Transparency permission,
> because it uses none of them.

### 5. External services, tools and platforms

> ModernBody uses two external services: one for meal analysis, and one to validate subscription
> purchases.
>
> **AI nutrition estimation.** When the user scans a photo or describes a meal, that single
> image or text string is sent over HTTPS to an AI gateway operated by Rork
> (`toolkit.rork.com`), which routes it to a vision-capable large language model. The primary
> model is Google Gemini 3 Flash, with Anthropic Claude Haiku 4.5 and OpenAI GPT-5 Mini as
> fallbacks. The model returns a structured JSON estimate — item names, portion descriptions,
> calories, macronutrients and a 1–10 health score — which the app renders for the user to
> review and correct before saving.
>
> **What is transmitted:** only the photo or text description, the user's selected language, and
> the fixed instruction set for the estimate.
>
> **What is not transmitted:** name, email, device identifier, profile, weight, meal history, or
> any account information — the app has no account system and no other network calls.
>
> **Subscription validation.** If the user buys ModernBody Pro, the transaction is handled by
> Apple and validated by RevenueCat, which tells the app whether the subscription is active.
> RevenueCat receives a randomly generated, anonymous app user identifier together with the App
> Store purchase receipt. It does not receive the user's name, email address, meals, photos,
> weight or any other content from the app. Payment card details remain with Apple and are never
> seen by the app. If the user never subscribes, no purchase data is created and nothing is sent
> to RevenueCat.
>
> The app integrates **no** analytics, attribution, advertising, crash-reporting or tracking
> SDKs, and no authentication provider.
>
> The only other network request is a Google Fonts stylesheet used by the web build; the iOS app
> bundles its fonts. **`[CONFIRM]`** — verify this before sending if the iOS build differs.

### 6. Regional differences

> The app functions identically in every region. There are no region-gated features, no
> geographically restricted content, and no location detection of any kind.
>
> The interface is localised into 32 languages, including right-to-left layouts for Arabic,
> Hebrew and Persian. Language follows the device setting by default and can be changed manually
> in Settings. Localisation affects only the interface language and the language the AI returns
> item names in — the underlying functionality is the same everywhere.
>
> Units can be switched between metric and imperial in Settings; this is a user preference, not
> a regional behaviour.

### 7. Regulated industry and third-party material

> ModernBody is a **general wellness and nutrition-awareness app**, not a medical device and not
> a healthcare service. It makes no diagnostic claims and provides no clinical advice.
>
> The app presents these disclaimers in the product itself:
>
> - Wherever an estimate is displayed: "Nutrition values are estimates and may vary based on
>   ingredients, portion sizes and preparation methods."
> - During onboarding and in the legal section: the app "does not diagnose, treat, cure or
>   prevent any medical condition, and it is not a substitute for advice from a qualified
>   healthcare professional", with an explicit instruction to consult a doctor or registered
>   dietitian before significant dietary changes, particularly during pregnancy, when managing a
>   medical condition, or when taking medication.
> - In the Terms of Use: users are told not to rely on the app where accurate nutritional
>   information is medically necessary — specifically naming allergen avoidance, diabetes
>   management and clinical diets.
>
> The Privacy Policy and Terms of Use are rendered **inside the app** (Settings → Privacy Policy
> / Terms of Use) so they are always reachable and version-locked to the binary under review.
>
> **Third-party material:** the app contains no licensed third-party content. The built-in
> 100-item food reference list contains generic food names with typical nutritional values and
> is not reproduced from a proprietary database. **`[CONFIRM]`** — confirm the provenance of
> `foods.json` before sending this line.

---

## Part 2 — Screen recording shot list (Apple item 1)

Record on a **physical iPhone** running the latest iOS, in one continuous take, starting from
the Home screen. Do not use the Simulator — Apple asks for a physical device and can tell.

**Before you record:** delete and reinstall the app so onboarding runs, and so the permission
prompts actually appear. A recording that skips the permission dialogs will not satisfy the
request.

1. **Launch from the Home screen** — tap the icon, show the launch and first frame.
2. **Onboarding** — walk through every step: name, sex, birth year, height, weight, goal weight,
   activity, goal, weekly rate. Show the calculated daily targets at the end.
3. **Home tab** — pause on the calorie ring, macro rings, water, exercise and weight cards, and
   the empty-meals state.
4. **Scan a meal** — tap Scan, and **let the camera permission dialog appear and be accepted on
   camera**. Photograph a meal. Show the analysing state, then the result with items, calories,
   macros and health score. Save it.
5. **Correct a meal** — open the saved meal, tap Edit, change the portion or calories, save. This
   demonstrates that estimates are user-correctable, which matters for item 7.
6. **Describe a meal** — Type → "two scrambled eggs and toast" → show the result → save.
7. **Search** — open Search, find a food in the offline list, add it.
8. **Photo library permission** — add a progress photo or pick a meal photo from the library so
   **the photo library dialog appears on camera**.
9. **Log water and exercise** — tap through both.
10. **Progress tab** — show weight history, streak and averages.
11. **Settings tab** — show language switching (change to one non-English language and back),
    reminders (**let the notification permission dialog appear** if you enable them), then open
    **Privacy Policy** and **Terms of Use** and scroll each one visibly.
12. **Show there is no paywall** — scroll the full Settings list so it is evident no purchase or
    subscription flow exists. This is the clearest way to answer Apple's purchase-flow question.
13. **Erase all data** — show the destructive action and its confirmation.

Narration is not required, but the recording must be long enough to read each screen.

---

## Part 3 — Fix or confirm before you reply

Ordered by how likely each is to cause a second rejection.

**1. Privacy Policy URL — ~~blocked~~ now solvable locally. Action: deploy the web build.**

`Legal.swift:18,21,24` ships with `privacyPolicyURL`, `termsOfUseURL` and `supportEmail` empty.
That is safe *inside* the app — the in-app documents are canonical and no dead links render. But
App Store Connect requires a **Privacy Policy URL** and a **Support URL** as app metadata
regardless, and there was nowhere to point them.

**Fixed on 2026-08-20.** The privacy policy and terms are now served by the web build at
`/privacy` and `/terms` (unit 02). Separately, the web Settings rows had been linking to
`https://rork.app/privacy` and `https://rork.app/terms` — the **toolchain vendor's** documents,
not ModernBody's. Those now point at ModernBody's own pages.

Remaining steps, in order:

1. Deploy the web build anywhere static (Vercel, Netlify, Cloudflare Pages — `npm run build`
   outputs `web/dist/`).
2. Paste `https://<your-domain>/privacy` into App Store Connect as the Privacy Policy URL.
3. Paste the same URL into `Legal.swift:18` and `/terms` into `Legal.swift:21` so the iOS app
   offers web mirrors. Until then the in-app documents remain canonical and no dead link shows,
   which is still valid.

**Do not change the `/privacy` and `/terms` paths** once submitted.

**2. Substantiate the model-training claim — `[CONFIRM]`**
The Privacy Policy states that photos sent for analysis "are not used by us to build a user
profile or to train models." User meal photos travel through Rork's gateway to Google, Anthropic
or OpenAI. Get written confirmation from Rork that their gateway terms — and the downstream
provider terms they use — actually support that statement. Apple item 7 asks for documentation
where relevant, and this is a claim you would need to defend. If Rork cannot confirm it, soften
the wording before resubmitting rather than after.

**3. Nutrition Facts in App Privacy — `[CONFIRM]`**
Your App Privacy answers in App Store Connect should reflect that meal photos and descriptions
are transmitted to a third-party processor. "Data Not Collected" is the natural reading of your
privacy policy, but photos *do* leave the device for processing. Make sure the App Privacy
questionnaire and the Privacy Policy tell the same story — a mismatch between them is its own
rejection reason.

**4. Age rating — `[CONFIRM]`**
Weight-management features and BMI categories often warrant a 12+ rating. Confirm what you
selected. Apps that set 4+ on weight-loss functionality do get flagged.

**5. Screenshots — `[CONFIRM]`**
Apple's message specifically calls out Guideline 2.3.3. Confirm your App Store screenshots show
the app actually in use — Home with logged meals, a scan result, the Progress tab — and not title
art or a splash screen.

**6. The `s.subscription` string**
The localisation catalogue contains an `s.subscription` ("SUBSCRIPTION") key in all 32 languages,
but the iOS `SettingsView.swift` never renders it — it is a leftover shared with the web and
Android builds, which do have a paywall UI. **No action needed for this submission**; the iOS
binary shows no subscription section. Noting it so nobody "fixes" it into visibility later. If
you ever surface it on iOS without real StoreKit products behind it, that is an immediate
Guideline 3.1.1 rejection.

---

## What I still need from you

| # | Question | Why it matters | Status |
| --- | --- | --- | --- |
| 1 | Exact device models and iOS versions tested on | Apple item 2 — cannot be invented | **Blocked** — needs a physical iPhone |
| 2 | Confirm the target-audience wording in item 3 | Only you know who this is for | Open |
| 3 | Privacy Policy URL | Most likely cause of a second rejection | **Solved locally** — deploy the web build, see Part 3 §1 |
| 4 | Where did `foods.json` come from — generic values, or a licensed source? | Apple item 7, third-party material | Open |
| 5 | Can Rork confirm no-training / no-retention on the gateway? | Backs a claim already shipping in your privacy policy | Open |
| 6 | Age rating and screenshots | Both called out in Apple's message | Open |
| 7 | Does the iOS build load a webfont, or bundle its own? | One line in item 5 | Open |
| 8 | Do you own `calzy.app`? | `Settings.tsx:196` links `mailto:support@calzy.app`; App Store Connect also needs a Support URL | New — left unchanged deliberately |
| 9 | Sign off on two legal-copy edits | "your iPhone" → "your device", plus a browser-storage sentence | New — see `context/decision-log.md` |

## Answered without you

These were open questions; the code answered them.

- **Purpose strings (Guideline 5.1.1)** — already correct. `project.pbxproj:405-407` states both
  the reason and how the data is used for camera, photo library and photo-library-add. No change
  needed.
- **Purchase flows (Apple item 1, Guideline 3.1.2)** — iOS has no paywall and no StoreKit. The
  correct answer to Apple is that no purchase flow exists, and the recording should demonstrate
  that by scrolling Settings.
- **iPad** — not a risk. `TARGETED_DEVICE_FAMILY = 1`, iPhone only, portrait only.
- **External services (item 5)** — fully traced: one gateway, three models, no analytics or
  tracking SDKs of any kind.
- **Regional differences (item 6)** — 32 locales including RTL, no region gating, no location
  access. Answer written.
- **Regulated industry (item 7)** — disclaimers already ship in three places. Answer written and
  quoted from `Legal.swift`.
