package com.rork.calzyandroid.data

/**
 * Canonical legal, support and disclaimer copy for the Android build.
 *
 * The documents are rendered **inside the app** so they are always reachable and
 * version-locked to the APK under review, rather than pointing at a generic
 * hosted page that describes someone else's product.
 *
 * This is the Android twin of the iOS `Legal.swift`. Wording differs only where
 * the platform differs (Google Play instead of the App Store, Android Settings
 * instead of iOS Settings).
 */
import com.rork.calzyandroid.BuildConfig

object Legal {

    /** Public web mirror of the privacy policy. Empty hides the "view online" row. */
    const val PRIVACY_POLICY_URL: String = ""

    /** Public web mirror of the terms of use. Empty hides the "view online" row. */
    const val TERMS_OF_USE_URL: String = ""

    /** Support inbox. Empty hides the email row. */
    const val SUPPORT_EMAIL: String = ""

    const val APP_NAME: String = "ModernBody"

    /** Always the version of the binary the reader is holding. */
    val VERSION: String = BuildConfig.VERSION_NAME

    /** Shown wherever an AI estimate is presented as a number. */
    const val ESTIMATE_DISCLAIMER: String =
        "Nutrition values are estimates and may vary based on ingredients, portion " +
            "sizes and preparation methods."

    const val WELLNESS_DISCLAIMER: String =
        "ModernBody is a general wellness and nutrition-awareness app. It does not " +
            "diagnose, treat, cure or prevent any medical condition, and it is not a " +
            "substitute for advice from a qualified healthcare professional. Always " +
            "consult your doctor or a registered dietitian before making significant " +
            "changes to your diet, especially if you are pregnant, managing a medical " +
            "condition or taking medication."

    val PRIVACY_POLICY: String = """
        Last updated: 31 August 2026

        SUMMARY

        ModernBody does not require an account. Everything you log stays on your device. We do not use analytics, advertising or tracking technologies of any kind, and we never sell your data.

        1. INFORMATION STORED ON YOUR DEVICE

        The following is written only to ModernBody's private storage area on your device and is never uploaded to us:

        • Profile details you enter (name, sex, birth year, height, weight, activity level, goal and daily targets)
        • Meals, food items, portions and meal times
        • Exercise entries, water intake and weight history
        • Meal photos and progress photos
        • Saved meals and your chosen language

        Because this data is stored locally, it is included in your device backups if you use Android Backup or a computer backup. Those backups are governed by Google's privacy policy, not ours.

        2. INFORMATION SENT FOR AI ANALYSIS

        When you scan a meal photo or describe a meal in words, that single photo or text description is transmitted over an encrypted connection to our AI processing provider so that nutrition can be estimated. The request contains only the photo or description, the language you selected and the instruction set for the estimate.

        The request does not include your name, email address, device identifier, profile, weight, meal history or any account information, because the app has no account system.

        Photos submitted for analysis are processed to generate the estimate and are not used by us to build a user profile or to train models. The photo that appears in your meal history is a separate copy stored on your device.

        3. PURCHASES

        This version of ModernBody is free. It contains no subscriptions, no in-app purchases and no billing code of any kind, so no purchase or payment information is created, collected or shared.

        4. INFORMATION WE DO NOT COLLECT

        We do not collect or process: contacts, precise or coarse location, health records, advertising identifiers, browsing history, crash telemetry or usage analytics. ModernBody contains no analytics, attribution or advertising SDKs, and performs no tracking across apps or websites.

        5. PERMISSIONS

        • Camera — used only while the meal scanner is open, so you can photograph a meal. Declining leaves every other feature usable; you can still pick a photo from your library or type a description.
        • Photos and media — used only when you choose a photo for a meal or a progress photo. ModernBody reads only the item you select.

        ModernBody requests no other permissions. It does not send notifications, and it does not read or write any health, fitness or activity data held by your device. Each permission is requested at the moment the feature is first used, never at launch.

        6. YOUR CONTROL AND DELETION

        You can delete individual meals, exercises, weights and photos at any time. Settings › Erase all data permanently removes every entry, photo and profile detail from the device and returns the app to its first-run state. Uninstalling the app also destroys all of its stored data. Because we hold no server-side copy and no account, there is nothing for us to delete on your behalf.

        7. CHILDREN

        ModernBody is intended for general adult consumers and is not directed at children under 13.

        8. CHANGES

        If this policy changes materially, the updated text will ship with a new version of the app and the date above will be revised.
    """.trimIndent()

    val TERMS_OF_USE: String = """
        Last updated: 31 August 2026

        1. ACCEPTANCE

        By using ModernBody you agree to these terms. If you do not agree, please stop using the app and uninstall it from your device.

        2. WHAT MODERNBODY IS

        ModernBody is a nutrition-awareness and meal-logging tool for general adult consumers. It estimates the calorie and macronutrient content of meals from a photo or a written description, and tracks those estimates against daily targets calculated from the details you enter.

        3. NOT MEDICAL ADVICE

        $WELLNESS_DISCLAIMER

        4. ACCURACY OF ESTIMATES

        $ESTIMATE_DISCLAIMER Estimates are produced by automated image and text analysis and are inherently approximate. Calorie targets are derived from the Mifflin–St Jeor equation and standard activity multipliers, which are population averages and will not fit every individual. You are responsible for reviewing and correcting any estimate before relying on it, and the app provides editing controls for exactly that purpose. Do not rely on ModernBody where accurate nutritional information is medically necessary — for example for allergen avoidance, diabetes management or clinical diets.

        5. ACCEPTABLE USE

        Use ModernBody only for its intended purpose. Do not attempt to interfere with, overload or reverse engineer the app or the services it relies on, and do not submit content you have no right to submit.

        6. NO USER-GENERATED CONTENT PLATFORM

        ModernBody has no social features. There are no public profiles, feeds, comments, messaging, sharing between users or any other mechanism by which content you create becomes visible to another person inside the app. Meal names and descriptions you type are stored on your device only.

        7. PRICE

        This version of ModernBody is free to use in full. Every feature — meal scanning, the food database, manual entry, exercise and water logging, weight and photo tracking — is available without payment. There is no subscription, no in-app purchase and no locked or trial-limited functionality.

        8. DISCLAIMER OF WARRANTIES

        ModernBody is provided "as is", without warranty of any kind, express or implied, including fitness for a particular purpose. We do not warrant that estimates will be accurate or that the app will be uninterrupted or error-free.

        9. LIMITATION OF LIABILITY

        To the fullest extent permitted by law, we are not liable for any indirect, incidental or consequential loss arising from your use of ModernBody, including any decision made in reliance on an estimate it produced.

        10. CHANGES

        These terms may be updated in a future version of the app, with the date above revised accordingly.
    """.trimIndent()
}
