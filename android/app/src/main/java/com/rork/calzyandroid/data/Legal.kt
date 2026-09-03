package com.rork.calzyandroid.data

/**
 * Links to ModernBody's own legal documents and support contact.
 *
 * Mirrors `web/src/lib/legal.ts` and `ios-calzy/.../Utilities/Legal.swift`, which
 * both apply the same rule: an empty value hides its row rather than shipping a
 * dead or wrong link.
 *
 * Android previously hardcoded `https://rork.app/terms`, `https://rork.app/privacy`
 * and `mailto:support@calzy.app` - the scaffolding vendor's documents, which
 * describe a different product, and a domain this project does not own.
 *
 * Fill these in once the web build is deployed; it serves the canonical documents
 * at `/privacy` and `/terms`, and those are the URLs submitted to the Play Console
 * and App Store Connect. Until then the rows stay hidden, which is incomplete but
 * honest - unlike pointing a user at another company's privacy policy.
 */
object Legal {
    /** e.g. "https://your-domain/privacy" */
    const val PRIVACY_POLICY_URL: String = ""

    /** e.g. "https://your-domain/terms" */
    const val TERMS_OF_USE_URL: String = ""

    /** Only set this to an address that is actually owned and monitored. */
    const val SUPPORT_EMAIL: String = ""

    val hasAnySupportLink: Boolean
        get() = PRIVACY_POLICY_URL.isNotEmpty() ||
            TERMS_OF_USE_URL.isNotEmpty() ||
            SUPPORT_EMAIL.isNotEmpty()
}
