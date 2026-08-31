import Foundation
import Observation
import RevenueCat

/// Billing term shown on the paywall.
nonisolated enum PlanTerm: Int, Sendable {
    case weekly = 0
    case monthly = 1
    case yearly = 2

    var label: String {
        switch self {
        case .weekly: "Weekly"
        case .monthly: "Monthly"
        case .yearly: "Yearly"
        }
    }

    var unitNoun: String {
        switch self {
        case .weekly: "week"
        case .monthly: "month"
        case .yearly: "year"
        }
    }
}

/// One purchasable row on the paywall.
///
/// `package` is nil only for the debug-only design preview, which is why every
/// purchase path checks it before charging anything.
struct SubscriptionPlan: Identifiable {
    let id: String
    let term: PlanTerm
    /// Localized total charged each period, e.g. "€17.99".
    let price: String
    /// Localized equivalent unit price for longer terms, e.g. "€7.42 / month".
    let perUnit: String?
    let badge: String?
    let package: Package?
}

/// RevenueCat wrapper for ModernBody Pro.
///
/// The SDK is only configured when ``PurchaseConfig`` carries a real key. Without
/// one the manager still publishes state so the UI can explain itself, but it
/// never touches StoreKit and never claims a subscription is active.
@Observable
@MainActor
final class PurchaseManager {
    static let shared = PurchaseManager()

    private(set) var plans: [SubscriptionPlan] = []
    private(set) var isSubscribed: Bool = false
    private(set) var isLoading: Bool = false
    private(set) var purchasingID: String?
    private(set) var isRestoring: Bool = false
    var message: String?

    var isConfigured: Bool { PurchaseConfig.isConfigured }

    /// True when the paywall is showing hardcoded design pricing, never purchasable.
    var isPreviewPricing: Bool { !isConfigured && !plans.isEmpty }

    private init() {
        #if DEBUG
        if !PurchaseConfig.isConfigured { plans = Self.previewPlans }
        #endif
    }

    /// Design-only pricing for the in-development paywall.
    ///
    /// Debug builds only, and every row is unpurchasable because `package` is nil.
    /// This exists so the layout can be reviewed before store products are live —
    /// it must never be reachable from a release build.
    private static let previewPlans: [SubscriptionPlan] = [
        SubscriptionPlan(id: "preview_weekly", term: .weekly, price: "€9.99", perUnit: nil, badge: nil, package: nil),
        SubscriptionPlan(id: "preview_monthly", term: .monthly, price: "€17.99", perUnit: nil, badge: nil, package: nil),
        SubscriptionPlan(id: "preview_yearly", term: .yearly, price: "€89.00", perUnit: "€7.42 / month", badge: "Best value", package: nil)
    ]

    /// Configures the SDK once, from the App's `init()`.
    static func configureSDK() {
        guard PurchaseConfig.isConfigured else { return }
        #if DEBUG
        Purchases.logLevel = .debug
        #else
        Purchases.logLevel = .error
        #endif
        Purchases.configure(withAPIKey: PurchaseConfig.apiKey)
    }

    /// Streams entitlement changes for the lifetime of the app.
    func startListening() async {
        guard PurchaseConfig.isConfigured else { return }
        for await info in Purchases.shared.customerInfoStream {
            isSubscribed = Self.isPro(info)
        }
    }

    private static func isPro(_ info: CustomerInfo) -> Bool {
        info.entitlements[PurchaseConfig.entitlementID]?.isActive == true
    }

    func refreshOfferings() async {
        guard PurchaseConfig.isConfigured else { return }
        isLoading = true
        message = nil
        do {
            let offerings = try await Purchases.shared.offerings()
            let offering = PurchaseConfig.offeringID.isEmpty
                ? offerings.current
                : offerings.offering(identifier: PurchaseConfig.offeringID)
            plans = (offering?.availablePackages ?? [])
                .compactMap(Self.plan(from:))
                .sorted { $0.term.rawValue < $1.term.rawValue }
        } catch {
            message = Self.friendlyMessage(for: error)
        }
        isLoading = false
    }

    func purchase(_ plan: SubscriptionPlan) async {
        guard PurchaseConfig.isConfigured, let package = plan.package else {
            message = "Subscriptions aren't available in this build yet."
            return
        }
        purchasingID = plan.id
        message = nil
        do {
            let result = try await Purchases.shared.purchase(package: package)
            if !result.userCancelled {
                isSubscribed = Self.isPro(result.customerInfo)
            }
        } catch ErrorCode.purchaseCancelledError {
            // A deliberate cancel is not a failure and gets no alert.
        } catch ErrorCode.paymentPendingError {
            message = "Your purchase is pending approval. Pro unlocks as soon as it clears."
        } catch {
            message = Self.friendlyMessage(for: error)
        }
        purchasingID = nil
    }

    func restore() async {
        guard PurchaseConfig.isConfigured else {
            message = "Subscriptions aren't available in this build yet."
            return
        }
        isRestoring = true
        message = nil
        do {
            let info = try await Purchases.shared.restorePurchases()
            isSubscribed = Self.isPro(info)
            message = isSubscribed
                ? "Your subscription has been restored."
                : "No previous purchase was found for this Apple Account."
        } catch {
            message = Self.friendlyMessage(for: error)
        }
        isRestoring = false
    }

    // MARK: - Mapping

    private static func plan(from package: Package) -> SubscriptionPlan? {
        let product = package.storeProduct
        let term: PlanTerm?
        switch package.packageType {
        case .weekly: term = .weekly
        case .monthly: term = .monthly
        case .annual: term = .yearly
        default: term = period(of: product)
        }
        guard let term else { return nil }

        return SubscriptionPlan(
            id: package.identifier,
            term: term,
            price: product.localizedPriceString,
            perUnit: term == .yearly ? monthlyEquivalent(of: product) : nil,
            badge: term == .yearly ? "Best value" : nil,
            package: package
        )
    }

    private static func period(of product: StoreProduct) -> PlanTerm? {
        guard let period = product.subscriptionPeriod else { return nil }
        switch period.unit {
        case .week: return .weekly
        case .month: return period.value == 12 ? .yearly : .monthly
        case .year: return .yearly
        default: return nil
        }
    }

    private static func monthlyEquivalent(of product: StoreProduct) -> String? {
        let monthly = product.price / 12
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = product.currencyCode
        formatter.locale = .current
        guard let text = formatter.string(from: monthly as NSDecimalNumber) else { return nil }
        return "\(text) / month"
    }

    private static func friendlyMessage(for error: Error) -> String {
        guard let code = error as? ErrorCode else {
            return "Something went wrong. Please try again."
        }
        switch code {
        case .networkError:
            return "You appear to be offline. Check your connection and try again."
        case .storeProblemError:
            return "The App Store is having trouble right now. Please try again in a moment."
        case .purchaseNotAllowedError:
            return "This device isn't allowed to make purchases."
        case .productAlreadyPurchasedError:
            return "You already own this subscription. Try Restore Purchases."
        default:
            return "Something went wrong with the purchase. Please try again."
        }
    }
}
