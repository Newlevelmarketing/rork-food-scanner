import Foundation

/// Canonical legal, support and disclaimer copy.
///
/// The policy text is rendered **inside the app** (see `LegalViews.swift`) so the
/// documents are always reachable and version-locked to the binary under review.
/// `privacyPolicyURL`, `termsOfUseURL` and `supportEmail` are optional mirrors —
/// when a value is empty the corresponding "open on the web" row is hidden rather
/// than shipping a dead link.
nonisolated enum Legal {
    // MARK: - Configuration
    //
    // ⚠️ Set these before submitting if you want web mirrors of the in-app
    // documents. Leaving them empty is safe: the in-app documents are canonical
    // and no dead links are shown.

    /// Public web mirror of the privacy policy, e.g. "https://example.com/privacy".
    static let privacyPolicyURL: String = ""

    /// Public web mirror of the terms of use, e.g. "https://example.com/terms".
    static let termsOfUseURL: String = ""

    /// Support inbox, e.g. "support@example.com". Empty hides the email row.
    static let supportEmail: String = ""

    static let appName: String = "ModernBody"
    static let version: String = "1.0.1"

    // MARK: - Disclaimers

    /// Shown wherever an AI estimate is presented as a number.
    static let estimateDisclaimer: String =
        "Nutrition values are estimates and may vary based on ingredients, portion sizes and preparation methods."

    /// Shown during onboarding and in the legal section.
    static let wellnessDisclaimer: String =
        """
        ModernBody is a general wellness and nutrition-awareness app. It does not \
        diagnose, treat, cure or prevent any medical condition, and it is not a \
        substitute for advice from a qualified healthcare professional. Always \
        consult your doctor or a registered dietitian before making significant \
        changes to your diet, especially if you are pregnant, managing a medical \
        condition or taking medication.
        """

    // MARK: - Privacy policy

    static let privacyPolicy: String =
        """
        Last updated: 31 August 2026

        SUMMARY

        ModernBody does not require an account. Everything you log stays on your \
        iPhone. We do not use analytics, advertising or tracking technologies of \
        any kind, and we never sell your data.

        1. INFORMATION STORED ON YOUR DEVICE

        The following is written only to ModernBody's private storage area on your \
        iPhone and is never uploaded to us:

        • Profile details you enter (name, sex, birth year, height, weight, \
        activity level, goal and daily targets)
        • Meals, food items, portions and meal times
        • Exercise entries, water intake and weight history
        • Meal photos and progress photos
        • Saved meals, reminder preferences and your chosen language

        Because this data is stored locally, it is included in your device backups \
        if you use iCloud Backup or a computer backup. Those backups are governed \
        by Apple's privacy policy, not ours.

        2. INFORMATION SENT FOR AI ANALYSIS

        When you scan a meal photo or describe a meal in words, that single photo \
        or text description is transmitted over an encrypted connection to our AI \
        processing provider so that nutrition can be estimated. The request \
        contains only the photo or description, the language you selected and the \
        instruction set for the estimate.

        The request does not include your name, email address, device identifier, \
        profile, weight, meal history or any account information, because the app \
        has no account system.

        Photos submitted for analysis are processed to generate the estimate and \
        are not used by us to build a user profile or to train models. The photo \
        that appears in your meal history is a separate copy stored on your device.

        3. SUBSCRIPTIONS AND PURCHASES

        ModernBody Pro is optional. If you buy it, the transaction is handled by \
        Apple and by our subscription provider, RevenueCat, which validates the \
        purchase and tells the app whether your subscription is active.

        RevenueCat receives a randomly generated, anonymous app user identifier \
        together with the App Store purchase receipt. It does not receive your \
        name, email address, meals, photos, weight or any other content from the \
        app. We never see or store your payment card details; those remain with \
        Apple.

        If you never subscribe, no purchase data is created and nothing is sent to \
        RevenueCat.

        4. INFORMATION WE DO NOT COLLECT

        We do not collect or process: contacts, precise or coarse location, health \
        records, advertising identifiers, browsing history, crash telemetry or \
        usage analytics. ModernBody contains no analytics, attribution or \
        advertising SDKs, and performs no tracking across apps or websites.

        5. PERMISSIONS

        • Camera — used only while the meal scanner is open, so you can photograph \
        a meal. Declining leaves every other feature usable; you can still pick a \
        photo from your library or type a description.
        • Photo library — used only when you choose a photo for a meal or a \
        progress photo. ModernBody reads only the item you select.
        • Notifications — used only if you switch on tracking reminders, to post \
        local reminders at the times you choose. No notification content leaves \
        your device.

        Each permission is requested at the moment the feature is first used, never \
        at launch.

        6. YOUR CONTROL AND DELETION

        You can delete individual meals, exercises, weights and photos at any time. \
        Settings › Delete All Data permanently removes every entry, photo and \
        profile detail from the device and returns the app to its first-run state. \
        Deleting the app from your iPhone also destroys all of its stored data. \
        Because we hold no server-side copy and no account, there is nothing for us \
        to delete on your behalf.

        Deleting your data does not cancel a subscription. Manage subscriptions in \
        Settings › your name › Subscriptions.

        7. CHILDREN

        ModernBody is intended for general adult consumers and is not directed at \
        children under 13.

        8. CHANGES

        If this policy changes materially, the updated text will ship with a new \
        version of the app and the date above will be revised.
        """

    // MARK: - Terms of use

    static let termsOfUse: String =
        """
        Last updated: 31 August 2026

        1. ACCEPTANCE

        By using ModernBody you agree to these terms. If you do not agree, please \
        stop using the app and delete it from your device.

        2. WHAT MODERNBODY IS

        ModernBody is a nutrition-awareness and meal-logging tool for general adult \
        consumers. It estimates the calorie and macronutrient content of meals from \
        a photo or a written description, and tracks those estimates against daily \
        targets calculated from the details you enter.

        3. NOT MEDICAL ADVICE

        \(wellnessDisclaimer)

        4. ACCURACY OF ESTIMATES

        \(estimateDisclaimer) Estimates are produced by automated image and text \
        analysis and are inherently approximate. Calorie targets are derived from \
        the Mifflin–St Jeor equation and standard activity multipliers, which are \
        population averages and will not fit every individual. You are responsible \
        for reviewing and correcting any estimate before relying on it, and the app \
        provides editing controls for exactly that purpose. Do not rely on \
        ModernBody where accurate nutritional information is medically necessary — \
        for example for allergen avoidance, diabetes management or clinical diets.

        5. ACCEPTABLE USE

        Use ModernBody only for its intended purpose. Do not attempt to interfere \
        with, overload or reverse engineer the app or the services it relies on, \
        and do not submit content you have no right to submit.

        6. NO USER-GENERATED CONTENT PLATFORM

        ModernBody has no social features. There are no public profiles, feeds, \
        comments, messaging, sharing between users or any other mechanism by which \
        content you create becomes visible to another person inside the app. Meal \
        names and descriptions you type are stored on your device only.

        7. SUBSCRIPTIONS AND BILLING

        Meal logging, the food database and manual entry are free to use. \
        ModernBody Pro is an optional auto-renewing subscription offered in \
        weekly, monthly and yearly terms.

        The exact price, billing period and currency of each term are shown in your \
        local currency on the subscription screen inside the app before you \
        confirm any purchase. Prices may differ by region and may change over \
        time; any change is shown to you before it applies.

        Payment is charged to your Apple Account when you confirm the purchase. A \
        subscription renews automatically for the same term unless you turn off \
        auto-renew at least 24 hours before the end of the current period. Your \
        account is charged for the renewal within 24 hours before the end of the \
        current period.

        You can view, manage and cancel your subscription, including turning off \
        auto-renew, in Settings › your name › Subscriptions. Deleting ModernBody \
        does not cancel a subscription.

        Where a free trial or introductory offer is provided, any unused portion is \
        forfeited when you purchase a subscription. Refunds are handled by Apple \
        under its refund policy.

        8. DISCLAIMER OF WARRANTIES

        ModernBody is provided "as is", without warranty of any kind, express or \
        implied, including fitness for a particular purpose. We do not warrant that \
        estimates will be accurate or that the app will be uninterrupted or \
        error-free.

        9. LIMITATION OF LIABILITY

        To the fullest extent permitted by law, we are not liable for any indirect, \
        incidental or consequential loss arising from your use of ModernBody, \
        including any decision made in reliance on an estimate it produced.

        10. CHANGES

        These terms may be updated in a future version of the app, with the date \
        above revised accordingly.
        """

    // MARK: - Help content

    static let faqs: [(question: String, answer: String)] = [
        (
            "How accurate are the calorie estimates?",
            "They are informed approximations, not measurements. The analysis reads visual cues such as plate size and utensils to judge portions, which works well for everyday meals and less well for mixed dishes, sauces and hidden fats. Treat every result as a starting point and correct it — tap a logged meal, then Edit, to adjust calories while keeping the macro balance."
        ),
        (
            "Do I need an account?",
            "No. ModernBody has no sign-up, no login and no password. Your data lives on this iPhone only, which is why there is nothing to log in to and nothing stored on a server."
        ),
        (
            "Why did my scan fail?",
            "The most common causes are no internet connection, a photo with no recognisable food in it, or very low light. The app will tell you which happened and let you retry. If a photo keeps failing, try the Type option instead and describe the meal in words."
        ),
        (
            "The camera is black or unavailable.",
            "ModernBody needs camera permission to show the viewfinder. Open iOS Settings › Privacy & Security › Camera › ModernBody and switch it on, or use the photo-library button in the scanner instead."
        ),
        (
            "How do I fix a wrong estimate?",
            "Open the meal from your day list, then use Edit to rename it or correct the calories in steps of 10, 50 or 100. Macros rescale proportionally. You can also change the portion count, which rescales everything at once."
        ),
        (
            "How are my daily targets calculated?",
            "Your basal metabolic rate comes from the Mifflin–St Jeor equation using your sex, age, height and weight. That is multiplied by an activity factor, then adjusted by your goal and weekly pace. You can override every target in Settings › Nutrition Goals."
        ),
        (
            "How do I delete everything?",
            "Settings › Delete All Data erases every meal, exercise, weight, photo and profile detail from the device and returns the app to its first-run state. This cannot be undone."
        ),
        (
            "Does ModernBody track me or show ads?",
            "No. There are no analytics, advertising or tracking SDKs in the app, and no cross-app or cross-site tracking of any kind."
        )
    ]
}
