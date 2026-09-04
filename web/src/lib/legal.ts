/**
 * Canonical legal copy for the web build.
 *
 * Ported from `ios-calzy/ModernBodyFoodScanner/Utilities/Legal.swift` so the same
 * documents back every platform. The iOS build renders its own copy from Swift;
 * this module is the source for `/privacy` and `/terms`, which double as the
 * public URLs App Store Connect and Google Play require.
 *
 * Two deliberate deviations from the Swift original, both for accuracy rather
 * than preference — see `context/decision-log.md`:
 *   1. "your iPhone" becomes "your device", because these pages serve the web
 *      and Android builds too.
 *   2. A sentence naming browser storage was added to the storage section,
 *      because on web the data lives in `localStorage` rather than an app
 *      sandbox.
 */

export type LegalBlock =
  | { kind: "heading"; text: string }
  | { kind: "paragraph"; text: string }
  | { kind: "bullets"; items: string[] };

export interface LegalDocument {
  title: string;
  lastUpdated: string;
  blocks: LegalBlock[];
}

export const appName = "ModernBody";
export const appVersion = "1.0.0";

/**
 * Support inbox, e.g. "support@example.com".
 *
 * Empty hides the support row rather than shipping a dead link — the same rule
 * `Legal.swift` applies on iOS. Set this only to an address on a domain that is
 * actually owned and monitored; App Store Connect and the Play Console both
 * check that support contacts resolve.
 */
export const supportEmail = "";

/** Shown wherever an AI estimate is presented as a number. */
export const estimateDisclaimer =
  "Nutrition values are estimates and may vary based on ingredients, portion sizes and preparation methods.";

/** Shown during onboarding and in the legal section. */
export const wellnessDisclaimer =
  "ModernBody is a general wellness and nutrition-awareness app. It does not diagnose, treat, cure or prevent any medical condition, and it is not a substitute for advice from a qualified healthcare professional. Always consult your doctor or a registered dietitian before making significant changes to your diet, especially if you are pregnant, managing a medical condition or taking medication.";

export const privacyPolicy: LegalDocument = {
  title: "Privacy Policy",
  lastUpdated: "5 September 2026",
  blocks: [
    { kind: "heading", text: "Summary" },
    {
      kind: "paragraph",
      text: "ModernBody does not require an account. Everything you log stays on your device. We do not use analytics, advertising or tracking technologies of any kind, and we never sell your data.",
    },

    { kind: "heading", text: "1. Information stored on your device" },
    {
      kind: "paragraph",
      text: "The following is written only to ModernBody's private storage on your device and is never uploaded to us:",
    },
    {
      kind: "bullets",
      items: [
        "Profile details you enter (name, sex, birth year, height, weight, activity level, goal and daily targets)",
        "Meals, food items, portions and meal times",
        "Exercise entries, water intake and weight history",
        "Meal photos and progress photos",
        "Saved meals, reminder preferences and your chosen language",
      ],
    },
    {
      kind: "paragraph",
      text: "In the mobile apps this is the app's own private storage area. In the web app it is your browser's local storage for this site, which means clearing your browser's site data also erases everything ModernBody has saved.",
    },
    {
      kind: "paragraph",
      text: "Because this data is stored locally, it is included in your device backups if you use iCloud Backup or a computer backup. Those backups are governed by Apple's privacy policy, not ours.",
    },

    { kind: "heading", text: "2. Information sent for AI analysis" },
    {
      kind: "paragraph",
      text: "When you scan a meal photo or describe a meal in words, that single photo or text description is transmitted over an encrypted connection to our AI processing provider so that nutrition can be estimated. The request contains only the photo or description, the language you selected and the instruction set for the estimate.",
    },
    {
      kind: "paragraph",
      text: "The request does not include your name, email address, device identifier, profile, weight, meal history or any account information, because the app has no account system.",
    },
    {
      kind: "paragraph",
      text: "Photos submitted for analysis are processed to generate the estimate and are not used by us to build a user profile or to train models. The photo that appears in your meal history is a separate copy stored on your device.",
    },

    { kind: "heading", text: "3. Subscriptions and purchases" },
    {
      kind: "paragraph",
      text: "ModernBody Pro is optional. If you buy it, the transaction is handled by the app store you bought it from and by our subscription provider, RevenueCat, which validates the purchase and tells the app whether your subscription is active.",
    },
    {
      kind: "paragraph",
      text: "RevenueCat receives a randomly generated, anonymous app user identifier together with the store purchase receipt. It does not receive your name, email address, meals, photos, weight or any other content from the app. We never see or store your payment card details; those remain with the app store.",
    },
    {
      kind: "paragraph",
      text: "If you never subscribe, no purchase data is created and nothing is sent to RevenueCat. The web version of ModernBody has no purchase flow, so nothing you do on this site can create purchase data.",
    },

    { kind: "heading", text: "4. Information we do not collect" },
    {
      kind: "paragraph",
      text: "We do not collect or process: contacts, precise or coarse location, health records, advertising identifiers, browsing history, crash telemetry or usage analytics. Apart from the subscription provider described in section 3, ModernBody contains no third-party analytics, attribution or advertising SDKs, and performs no tracking across apps or websites.",
    },

    { kind: "heading", text: "5. Permissions" },
    {
      kind: "bullets",
      items: [
        "Camera — used only while the meal scanner is open, so you can photograph a meal. Declining leaves every other feature usable; you can still pick a photo from your library or type a description.",
        "Photo library — used only when you choose a photo for a meal or a progress photo. ModernBody reads only the item you select.",
        "Notifications — used only if you switch on tracking reminders, to post local reminders at the times you choose. No notification content leaves your device.",
      ],
    },
    {
      kind: "paragraph",
      text: "Each permission is requested at the moment the feature is first used, never at launch.",
    },

    { kind: "heading", text: "6. Your control and deletion" },
    {
      kind: "paragraph",
      text: "You can delete individual meals, exercises, weights and photos at any time. Settings › Delete All Data permanently removes every entry, photo and profile detail from the device and returns the app to its first-run state. Deleting the app from your device also destroys all of its stored data. Because we hold no server-side copy and no account, there is nothing for us to delete on your behalf.",
    },
    {
      kind: "paragraph",
      text: "Deleting your data, or deleting the app, does not cancel a subscription. Manage or cancel a subscription in your app store account settings.",
    },

    { kind: "heading", text: "7. Children" },
    {
      kind: "paragraph",
      text: "ModernBody is intended for general adult consumers and is not directed at children under 13.",
    },

    { kind: "heading", text: "8. Changes" },
    {
      kind: "paragraph",
      text: "If this policy changes materially, the updated text will ship with a new version of the app and the date above will be revised.",
    },
  ],
};

export const termsOfUse: LegalDocument = {
  title: "Terms of Use",
  lastUpdated: "5 September 2026",
  blocks: [
    { kind: "heading", text: "1. Acceptance" },
    {
      kind: "paragraph",
      text: "By using ModernBody you agree to these terms. If you do not agree, please stop using the app and delete it from your device.",
    },

    { kind: "heading", text: "2. What ModernBody is" },
    {
      kind: "paragraph",
      text: "ModernBody is a nutrition-awareness and meal-logging tool for general adult consumers. It estimates the calorie and macronutrient content of meals from a photo or a written description, and tracks those estimates against daily targets calculated from the details you enter.",
    },

    { kind: "heading", text: "3. Not medical advice" },
    { kind: "paragraph", text: wellnessDisclaimer },

    { kind: "heading", text: "4. Accuracy of estimates" },
    {
      kind: "paragraph",
      text: `${estimateDisclaimer} Estimates are produced by automated image and text analysis and are inherently approximate. Calorie targets are derived from the Mifflin–St Jeor equation and standard activity multipliers, which are population averages and will not fit every individual. You are responsible for reviewing and correcting any estimate before relying on it, and the app provides editing controls for exactly that purpose. Do not rely on ModernBody where accurate nutritional information is medically necessary — for example for allergen avoidance, diabetes management or clinical diets.`,
    },

    { kind: "heading", text: "5. Acceptable use" },
    {
      kind: "paragraph",
      text: "Use ModernBody only for its intended purpose. Do not attempt to interfere with, overload or reverse engineer the app or the services it relies on, and do not submit content you have no right to submit.",
    },

    { kind: "heading", text: "6. No user-generated content platform" },
    {
      kind: "paragraph",
      text: "ModernBody has no social features. There are no public profiles, feeds, comments, messaging, sharing between users or any other mechanism by which content you create becomes visible to another person inside the app. Meal names and descriptions you type are stored on your device only.",
    },

    { kind: "heading", text: "7. Subscriptions and billing" },
    {
      kind: "paragraph",
      text: "ModernBody Pro is an optional auto-renewing subscription, offered in weekly, monthly and yearly terms. It is sold inside the ModernBody mobile app. This website has no purchase flow; nothing can be bought here.",
    },
    {
      kind: "paragraph",
      text: "The exact price, billing period and currency of each term are shown in your local currency on the subscription screen inside the app before you confirm any purchase. Prices may differ by region and may change over time; any change is shown to you before it applies.",
    },
    {
      kind: "paragraph",
      text: "Payment is charged to your app store account when you confirm the purchase. A subscription renews automatically for the same term unless you turn off auto-renew at least 24 hours before the end of the current period. Your account is charged for the renewal within 24 hours before the end of the current period.",
    },
    {
      kind: "paragraph",
      text: "You can view, manage and cancel your subscription, including turning off auto-renew, in your app store account settings. Deleting ModernBody does not cancel a subscription.",
    },
    {
      kind: "paragraph",
      text: "Where a free trial or introductory offer is provided, any unused portion is forfeited when you purchase a subscription. Refunds are handled by the app store you purchased from, under its refund policy.",
    },

    { kind: "heading", text: "8. Disclaimer of warranties" },
    {
      kind: "paragraph",
      text: 'ModernBody is provided "as is", without warranty of any kind, express or implied, including fitness for a particular purpose. We do not warrant that estimates will be accurate or that the app will be uninterrupted or error-free.',
    },

    { kind: "heading", text: "9. Limitation of liability" },
    {
      kind: "paragraph",
      text: "To the fullest extent permitted by law, we are not liable for any indirect, incidental or consequential loss arising from your use of ModernBody, including any decision made in reliance on an estimate it produced.",
    },

    { kind: "heading", text: "10. Changes" },
    {
      kind: "paragraph",
      text: "These terms may be updated in a future version of the app, with the date above revised accordingly.",
    },
  ],
};
