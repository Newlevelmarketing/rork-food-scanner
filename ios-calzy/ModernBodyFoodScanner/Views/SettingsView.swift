import SwiftUI

/// Settings tab: grouped cards with section captions.
struct SettingsView: View {
    @Environment(AppStore.self) private var store

    private let purchases = PurchaseManager.shared

    @State private var showEraseConfirm: Bool = false
    @State private var showPaywall: Bool = false

    /// Hidden entirely in a release build with no RevenueCat key, so an
    /// unconfigured binary can never present an unpurchasable paywall.
    private var showsSubscriptionSection: Bool {
        #if DEBUG
        true
        #else
        purchases.isConfigured
        #endif
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 22) {
                    section(L("s.personal")) {
                        VStack(spacing: 0) {
                            NavigationLink {
                                AccountView()
                            } label: {
                                HStack(spacing: 13) {
                                    ZStack {
                                        Circle().fill(Theme.ink)
                                        Text(initials)
                                            .font(.system(size: 17, weight: .bold, design: .rounded))
                                            .foregroundStyle(.white)
                                    }
                                    .frame(width: 48, height: 48)

                                    VStack(alignment: .leading, spacing: 3) {
                                        Text(displayName)
                                            .font(.system(size: 17, weight: .semibold))
                                            .foregroundStyle(Theme.ink)
                                        Text("\(store.targets.calories) kcal daily target")
                                            .font(.system(size: 13))
                                            .foregroundStyle(Theme.inkFaint)
                                    }
                                    Spacer()
                                    chevron
                                }
                                .padding(14)
                            }
                            divider
                            navRow("target", L("s.goals")) { NutritionGoalsView() }
                            divider
                            navRow("scalemass", L("s.weight")) { GoalsWeightView() }
                            divider
                            navRow("figure.walk", L("s.activity")) { ActivitySettingsView() }
                        }
                    }

                    section(L("s.app")) {
                        VStack(spacing: 0) {
                            NavigationLink {
                                LanguagePickerView()
                            } label: {
                                HStack(spacing: 13) {
                                    Image(systemName: "globe")
                                        .font(.system(size: 17))
                                        .foregroundStyle(Theme.ink)
                                        .frame(width: 28)
                                    Text(L("s.language"))
                                        .font(.system(size: 16, weight: .medium))
                                        .foregroundStyle(Theme.ink)
                                    Spacer(minLength: 8)
                                    HStack(spacing: 6) {
                                        Text(store.language.flag)
                                            .font(.system(size: 16))
                                        Text(store.language.nativeName)
                                            .font(.system(size: 15))
                                            .foregroundStyle(Theme.inkFaint)
                                            .lineLimit(1)
                                    }
                                    .environment(\.layoutDirection, .leftToRight)
                                    chevron
                                }
                                .padding(.horizontal, 14)
                                .padding(.vertical, 15)
                            }
                            divider
                            navRow("bell", L("s.reminders")) { RemindersView() }
                        }
                    }

                    section(L("s.preferences")) {
                        HStack(spacing: 13) {
                            Text("🎭").font(.system(size: 22)).frame(width: 28)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(L("s.jester"))
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundStyle(Theme.ink)
                                Text(L("s.jesterSub"))
                                    .font(.system(size: 12))
                                    .foregroundStyle(Theme.inkFaint)
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                            Spacer(minLength: 8)
                            Toggle("", isOn: Binding(
                                get: { store.profile.jesterMode },
                                set: { newValue in
                                    Haptics.selection()
                                    var profile = store.profile
                                    profile.jesterMode = newValue
                                    store.profile = profile
                                }
                            ))
                            .labelsHidden()
                            .tint(Theme.ink)
                        }
                        .padding(14)
                    }

                    if showsSubscriptionSection {
                        section("SUBSCRIPTION") {
                            Button {
                                Haptics.tap()
                                showPaywall = true
                            } label: {
                                HStack(spacing: 13) {
                                    ZStack {
                                        Circle()
                                            .fill(
                                                LinearGradient(
                                                    colors: [Theme.plum, Theme.protein],
                                                    startPoint: .topLeading,
                                                    endPoint: .bottomTrailing
                                                )
                                            )
                                        Image(systemName: "sparkles")
                                            .font(.system(size: 16, weight: .semibold))
                                            .foregroundStyle(.white)
                                    }
                                    .frame(width: 44, height: 44)

                                    VStack(alignment: .leading, spacing: 3) {
                                        HStack(spacing: 7) {
                                            Text("ModernBody Pro")
                                                .font(.system(size: 16, weight: .bold))
                                                .foregroundStyle(Theme.ink)
                                            Text(purchases.isSubscribed ? "ACTIVE" : "UPGRADE")
                                                .font(.system(size: 10, weight: .bold))
                                                .foregroundStyle(.white)
                                                .padding(.horizontal, 7)
                                                .padding(.vertical, 3)
                                                .background(
                                                    purchases.isSubscribed ? Theme.mint : Theme.ink,
                                                    in: Capsule()
                                                )
                                        }
                                        Text("Unlimited scans, deeper insights")
                                            .font(.system(size: 12))
                                            .foregroundStyle(Theme.inkFaint)
                                    }
                                    Spacer(minLength: 8)
                                    chevron
                                }
                                .padding(14)
                            }
                            .pressable()
                        }
                    }

                    section(L("s.support")) {
                        VStack(spacing: 0) {
                            navRow("questionmark.circle", "Help & Support") { SupportView() }
                            divider
                            navRow("lock.shield", L("s.privacy")) { PrivacyPolicyView() }
                            divider
                            navRow("doc.text", L("s.terms")) { TermsOfUseView() }
                        }
                    }

                    VStack(alignment: .leading, spacing: 10) {
                        HStack(alignment: .top, spacing: 9) {
                            Image(systemName: "iphone.and.arrow.forward")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundStyle(Theme.inkFaint)
                            Text("ModernBody works without an account. Everything you log is stored on this iPhone only.")
                                .font(.system(size: 12))
                                .foregroundStyle(Theme.inkSoft)
                                .fixedSize(horizontal: false, vertical: true)
                        }

                        Button(role: .destructive) {
                            Haptics.tap()
                            showEraseConfirm = true
                        } label: {
                            Text(L("s.erase"))
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(Theme.protein)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 15)
                                .background(Theme.protein.opacity(0.1), in: Capsule())
                        }
                        .buttonStyle(PressableButtonStyle())
                    }
                    .padding(.horizontal, 20)

                    Text("\(L("s.version")) \(Legal.version)")
                        .font(.system(size: 12))
                        .foregroundStyle(Theme.inkFaint)
                        .padding(.top, 4)
                }
                .padding(.bottom, 148)
                .padding(.top, 4)
            }
            .scrollIndicators(.hidden)
            .scrollContentBackground(.hidden)
            .navigationTitle(L("s.title"))
            .navigationBarTitleDisplayMode(.inline)
            .sheet(isPresented: $showPaywall) {
                PaywallView()
            }
            .confirmationDialog(
                "Delete all data?",
                isPresented: $showEraseConfirm,
                titleVisibility: .visible
            ) {
                Button("Delete everything", role: .destructive) {
                    store.eraseAll()
                    ReminderService.shared.cancelAll()
                    Haptics.warning()
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("This permanently removes every meal, exercise, weight, photo and profile detail from this device, and returns ModernBody to its first-run state. This cannot be undone.")
            }
        }
    }

    private var initials: String {
        let name = displayName.split(separator: " ").compactMap(\.first).prefix(2)
        return name.isEmpty ? "M" : String(name).uppercased()
    }

    private var displayName: String {
        store.profile.name.isEmpty ? "Your profile" : store.profile.name
    }

    private var chevron: some View {
        Image(systemName: "chevron.right")
            .font(.system(size: 13, weight: .semibold))
            .foregroundStyle(Theme.inkFaint)
    }

    private var divider: some View {
        Divider().overlay(Theme.hairline).padding(.leading, 55)
    }

    private func section<Content: View>(
        _ title: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 9) {
            Text(title)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(Theme.inkFaint)
                .padding(.leading, 24)
            content()
                .cardStyle(radius: 22, padding: 0)
                .padding(.horizontal, 20)
        }
    }

    private func navRow<Destination: View>(
        _ icon: String,
        _ title: String,
        @ViewBuilder destination: @escaping () -> Destination
    ) -> some View {
        NavigationLink {
            destination()
        } label: {
            HStack(spacing: 13) {
                Image(systemName: icon)
                    .font(.system(size: 17))
                    .foregroundStyle(Theme.ink)
                    .frame(width: 28)
                Text(title)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(Theme.ink)
                Spacer()
                chevron
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 15)
        }
    }
}
