import Foundation

/// RevenueCat client configuration.
///
/// These are RevenueCat **public** SDK keys (they begin with `appl_`). Public SDK
/// keys are designed to ship inside the client binary, so keeping them here is
/// not a secret leak — it is the documented RevenueCat integration. The secret
/// key, which must never appear in an app, is a different credential.
///
/// Until a key is filled in, `isConfigured` stays false: the SDK is never
/// configured, the Pro entry point is hidden, and no purchase UI is reachable in
/// a release build.
///
/// `entitlementID` and `offeringID` must match the identifiers configured in the
/// RevenueCat dashboard.
nonisolated enum PurchaseConfig {

    /// Public SDK key for the RevenueCat **Test Store** app, used in debug builds.
    static let testAPIKey: String = ""

    /// Public SDK key for the RevenueCat **App Store** app, used in release builds.
    static let appStoreAPIKey: String = ""

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
