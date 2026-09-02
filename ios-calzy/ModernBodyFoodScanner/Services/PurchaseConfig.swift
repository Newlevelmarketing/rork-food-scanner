import Foundation

/// RevenueCat client configuration.
///
/// These are RevenueCat **public** SDK keys (they begin with `appl_`). Public SDK
/// keys are designed to ship inside the client binary, so keeping them here is
/// not a secret leak — it is the documented RevenueCat integration. The secret
/// key, which must never appear in an app, is a different credential.
///
/// Keys come from `Config`, which is generated at build time from the project's
/// public environment variables. The literals in `Config.swift` read as empty in
/// source control and are injected during the iOS build, so an agent-time read of
/// this file will always show "" even when the keys are set.
///
/// Until a key is injected, `isConfigured` stays false: the SDK is never
/// configured, the Pro entry point is hidden, and no purchase UI is reachable in
/// a release build.
///
/// `entitlementID` and `offeringID` must match the identifiers configured in the
/// RevenueCat dashboard.
enum PurchaseConfig {

    /// Public SDK key for the RevenueCat **Test Store** app, used in debug builds.
    static let testAPIKey: String = Config.EXPO_PUBLIC_REVENUECAT_TEST_API_KEY

    /// Public SDK key for the RevenueCat **App Store** app, used in release builds.
    static let appStoreAPIKey: String = Config.EXPO_PUBLIC_REVENUECAT_IOS_API_KEY

    /// Entitlement that unlocks ModernBody Pro.
    ///
    /// Must match the dashboard entitlement identifier exactly. A mismatch does
    /// not fail loudly: the purchase succeeds, then reads back as "not
    /// subscribed", so this string is load-bearing.
    static let entitlementID: String = "premium"

    /// Offering queried for the paywall; empty uses the dashboard's current offering.
    ///
    /// Named explicitly so the paywall still resolves if `default` is ever not
    /// the dashboard's "current" offering.
    static let offeringID: String = "default"

    /// The key this build should configure with.
    static var apiKey: String {
        #if DEBUG
        testAPIKey.isEmpty ? appStoreAPIKey : testAPIKey
        #else
        appStoreAPIKey
        #endif
    }

    static var isConfigured: Bool { !apiKey.isEmpty }
}
