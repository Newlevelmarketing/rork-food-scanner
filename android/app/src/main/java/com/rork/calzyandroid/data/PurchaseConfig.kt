package com.rork.calzyandroid.data

/**
 * RevenueCat client configuration.
 *
 * [GOOGLE_API_KEY] is a RevenueCat **public** SDK key (it begins with `goog_`).
 * Public SDK keys are designed to ship inside the client binary, so keeping it
 * here is not a secret leak — it is the documented RevenueCat integration.
 * The secret key, which must never appear in an app, is a different credential.
 *
 * Until a key is filled in, [isConfigured] stays false: the SDK is never
 * configured, the Pro entry point is hidden, and no purchase UI can be reached
 * in a release build.
 *
 * [ENTITLEMENT_ID] and [OFFERING_ID] must match the identifiers configured in
 * the RevenueCat dashboard.
 */
object PurchaseConfig {

    /** RevenueCat public SDK key for the Play Store app (`goog_...`). */
    const val GOOGLE_API_KEY: String = ""

    /**
     * Entitlement that unlocks ModernBody Pro.
     *
     * Must match the dashboard entitlement identifier exactly. A mismatch does
     * not fail loudly: the purchase succeeds, then reads back as "not
     * subscribed", so this string is load-bearing.
     */
    const val ENTITLEMENT_ID: String = "premium"

    /**
     * Offering queried for the paywall; blank uses the dashboard's current offering.
     *
     * Named explicitly so the paywall still resolves if `default` is ever not the
     * dashboard's "current" offering.
     */
    const val OFFERING_ID: String = "default"

    val isConfigured: Boolean
        get() = GOOGLE_API_KEY.isNotBlank()
}
