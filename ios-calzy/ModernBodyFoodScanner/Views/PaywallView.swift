import RevenueCat
import SwiftUI

/// ModernBody Pro paywall backed by the live RevenueCat offering.
///
/// Every price on screen comes from the store product, never from a constant, so
/// the figure shown is the figure charged in the user's own currency. When no
/// offering can be loaded the screen says so instead of inventing prices.
struct PaywallView: View {
    @Environment(\.dismiss) private var dismiss

    private let store = PurchaseManager.shared

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    hero
                    features

                    if store.isPreviewPricing {
                        notice(
                            "Design preview — these are placeholder prices. Purchasing unlocks once the store products are live."
                        )
                    }

                    if store.isLoading {
                        ProgressView()
                            .tint(Theme.ink)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 30)
                    } else if store.plans.isEmpty {
                        notice(
                            store.message
                                ?? "Subscription plans can't be loaded right now. ModernBody stays fully usable in the meantime."
                        )
                    } else {
                        VStack(spacing: 10) {
                            ForEach(store.plans) { plan in
                                planCard(plan)
                            }
                        }
                    }

                    if let message = store.message, !store.isPreviewPricing {
                        Text(message)
                            .font(.system(size: 13))
                            .foregroundStyle(Theme.inkSoft)
                            .multilineTextAlignment(.center)
                            .fixedSize(horizontal: false, vertical: true)
                    }

                    restoreButton
                    disclosure
                    legalLinks
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
                .padding(.bottom, 40)
            }
            .scrollIndicators(.hidden)
            .background(Theme.backdrop)
            .navigationTitle("ModernBody Pro")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                        .foregroundStyle(Theme.ink)
                }
            }
        }
        .task { await store.refreshOfferings() }
        .onChange(of: store.isSubscribed) { _, subscribed in
            if subscribed { dismiss() }
        }
    }

    // MARK: - Sections

    private var hero: some View {
        VStack(spacing: 10) {
            ZStack {
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [Theme.plum, Theme.protein],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                Image(systemName: "sparkles")
                    .font(.system(size: 30, weight: .semibold))
                    .foregroundStyle(.white)
            }
            .frame(width: 74, height: 74)

            Text("Go Pro")
                .font(.metric(30, .heavy))
                .foregroundStyle(Theme.ink)

            Text("Unlimited AI scans, deeper insights and\nevery future feature — day one.")
                .font(.system(size: 14))
                .foregroundStyle(Theme.inkSoft)
                .multilineTextAlignment(.center)
        }
        .padding(.vertical, 6)
    }

    private var features: some View {
        VStack(spacing: 14) {
            feature("camera.fill", "Unlimited scans", "Photograph every plate, no daily cap")
            feature("chart.line.uptrend.xyaxis", "Deeper insights", "Weekly trends, macro balance coaching")
            feature("globe", "All 32 languages", "AI answers in the language you choose")
        }
        .cardStyle(radius: 22, padding: 16)
    }

    private func feature(_ icon: String, _ title: String, _ caption: String) -> some View {
        HStack(spacing: 12) {
            ZStack {
                Circle().fill(Theme.ink.opacity(0.06))
                Image(systemName: icon)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Theme.ink)
            }
            .frame(width: 38, height: 38)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Theme.ink)
                Text(caption)
                    .font(.system(size: 12))
                    .foregroundStyle(Theme.inkFaint)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 6)
            Image(systemName: "checkmark")
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(Theme.mint)
        }
    }

    private func planCard(_ plan: SubscriptionPlan) -> some View {
        let highlighted = plan.term == .yearly
        let purchasing = store.purchasingID == plan.id
        let disabled = store.purchasingID != nil || plan.package == nil

        return VStack(spacing: 12) {
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 3) {
                    HStack(spacing: 7) {
                        Text(plan.term.label)
                            .font(.system(size: 15, weight: .bold))
                            .foregroundStyle(Theme.ink)
                        if let badge = plan.badge {
                            Text(badge)
                                .font(.system(size: 10, weight: .bold))
                                .foregroundStyle(.white)
                                .padding(.horizontal, 7)
                                .padding(.vertical, 3)
                                .background(Theme.mint, in: Capsule())
                        }
                    }
                    Text("Billed every \(plan.term.unitNoun) · auto-renews")
                        .font(.system(size: 12))
                        .foregroundStyle(Theme.inkFaint)
                }
                Spacer(minLength: 8)
                VStack(alignment: .trailing, spacing: 2) {
                    Text(plan.price)
                        .font(.metric(20, .heavy))
                        .foregroundStyle(Theme.ink)
                    if let perUnit = plan.perUnit {
                        Text(perUnit)
                            .font(.system(size: 11))
                            .foregroundStyle(Theme.inkFaint)
                    }
                }
            }

            Button {
                Haptics.tap()
                Task { await store.purchase(plan) }
            } label: {
                Text(purchasing ? "Processing…" : "Subscribe · \(plan.price)")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 13)
                    .background(
                        disabled ? Theme.ink.opacity(0.3) : Theme.ink,
                        in: Capsule()
                    )
            }
            .pressable()
            .disabled(disabled || purchasing)
        }
        .padding(16)
        .background {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(Color.white.opacity(0.85))
                .overlay {
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .strokeBorder(
                            highlighted ? Theme.ink : Color.white.opacity(0.65),
                            lineWidth: highlighted ? 2 : 1
                        )
                }
        }
    }

    private var restoreButton: some View {
        Button {
            Haptics.tap()
            Task { await store.restore() }
        } label: {
            Text(store.isRestoring ? "Restoring…" : "Restore Purchases")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Theme.ink)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 6)
        }
        .pressable()
        .disabled(store.isRestoring)
    }

    /// Guideline 3.1.2 auto-renewable subscription disclosure.
    private var disclosure: some View {
        Text(
            """
            Subscriptions renew automatically for the same period at the price shown \
            above, charged to your Apple Account, unless auto-renew is turned off at \
            least 24 hours before the current period ends. Manage or cancel in \
            Settings › your name › Subscriptions. Deleting the app does not cancel a \
            subscription.
            """
        )
        .font(.system(size: 11))
        .foregroundStyle(Theme.inkFaint)
        .multilineTextAlignment(.center)
        .fixedSize(horizontal: false, vertical: true)
    }

    private var legalLinks: some View {
        HStack(spacing: 18) {
            NavigationLink {
                TermsOfUseView()
            } label: {
                Text("Terms of Use")
                    .font(.system(size: 12))
                    .foregroundStyle(Theme.inkFaint)
            }
            NavigationLink {
                PrivacyPolicyView()
            } label: {
                Text("Privacy Policy")
                    .font(.system(size: 12))
                    .foregroundStyle(Theme.inkFaint)
            }
        }
    }

    private func notice(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 12))
            .foregroundStyle(Theme.inkSoft)
            .multilineTextAlignment(.center)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity)
            .padding(14)
            .background(Theme.fat.opacity(0.12), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}
