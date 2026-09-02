package com.rork.calzyandroid.data

import com.rork.calzyandroid.BuildConfig
import com.rork.calzyandroid.Config

/**
 * RevenueCat client configuration.
 *
 * These are RevenueCat **public** SDK keys (`goog_...` / `test_...`). Public SDK
 * keys are designed to ship inside the client binary, so keeping them here is not
 * a secret leak — it is the documented RevenueCat integration. The secret key,
 * which must never appear in an app, is a different credential.
 *
 * Keys come from [Config], generated at build time from the project's public
 * environment variables. They are read through `Config.allValues` rather than as
 * named properties on purpose: `allValues` is the stable published surface of
 * that generated file, so this compiles whether or not the generator has emitted
 * a dedicated property for each key yet.
 *
 * Until a key is injected, [isConfigured] stays false: the SDK is never
 * configured, the Pro entry point is hidden, and no purchase UI can be reached
 * in a release build. A missing key therefore degrades to an honest "plans
 * unavailable" paywall, never to invented pricing.
 *
 * [ENTITLEMENT_ID] and [OFFERING_ID] must match the identifiers configured in
 * the RevenueCat dashboard.
 */
object PurchaseConfig {

    /** RevenueCat public SDK key for the Play Store app (`goog_...`). */
    val GOOGLE_API_KEY: String
        get() = Config.allValues["EXPO_PUBLIC_REVENUECAT_ANDROID_API_KEY"].orEmpty()

    /**
     * RevenueCat public SDK key for the Test Store app (`test_...`).
     *
     * Debug builds prefer this: the emulator has no Play Billing account, so the
     * Play Store key resolves zero packages there.
     */
    val TEST_API_KEY: String
        get() = Config.allValues["EXPO_PUBLIC_REVENUECAT_TEST_API_KEY"].orEmpty()

    /** The key this build should configure with. */
    val apiKey: String
        get() = if (BuildConfig.DEBUG && TEST_API_KEY.isNotBlank()) TEST_API_KEY else GOOGLE_API_KEY

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
        get() = apiKey.isNotBlank()
}
