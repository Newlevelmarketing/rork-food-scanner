import SwiftUI

// MARK: - Reusable disclaimer

/// Inline note reminding the user that nutrition figures are estimates.
struct EstimateDisclaimer: View {
    var compact: Bool = false

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: "info.circle")
                .font(.system(size: compact ? 11 : 12, weight: .semibold))
                .foregroundStyle(Theme.inkFaint)
            Text(Legal.estimateDisclaimer)
                .font(.system(size: compact ? 11 : 12))
                .foregroundStyle(Theme.inkFaint)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, compact ? 0 : 4)
        .accessibilityElement(children: .combine)
    }
}

// MARK: - Document shell

/// Scrolling container for the bundled legal documents.
private struct DocumentView: View {
    let title: String
    let body_: String
    let webMirror: String

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(body_)
                    .font(.system(size: 14))
                    .foregroundStyle(Theme.inkSoft)
                    .lineSpacing(4)
                    .textSelection(.enabled)
                    .fixedSize(horizontal: false, vertical: true)

                if let url = URL(string: webMirror), !webMirror.isEmpty {
                    Button {
                        UIApplication.shared.open(url)
                    } label: {
                        HStack(spacing: 7) {
                            Text("View online")
                                .font(.system(size: 14, weight: .semibold))
                            Image(systemName: "arrow.up.right")
                                .font(.system(size: 11, weight: .bold))
                        }
                        .foregroundStyle(Theme.ink)
                    }
                }

                Text("\(Legal.appName) \(Legal.version)")
                    .font(.system(size: 12))
                    .foregroundStyle(Theme.inkFaint)
            }
            .padding(20)
            .cardStyle(radius: 24, padding: 0)
            .padding(.horizontal, 20)
            .padding(.top, 10)
            .padding(.bottom, 40)
        }
        .scrollIndicators(.hidden)
        .background(Theme.backdrop)
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Documents

struct PrivacyPolicyView: View {
    var body: some View {
        DocumentView(
            title: L("s.privacy"),
            body_: Legal.privacyPolicy,
            webMirror: Legal.privacyPolicyURL
        )
    }
}

struct TermsOfUseView: View {
    var body: some View {
        DocumentView(
            title: L("s.terms"),
            body_: Legal.termsOfUse,
            webMirror: Legal.termsOfUseURL
        )
    }
}

// MARK: - Support

/// Self-contained help centre so support never depends on a reachable web page.
struct SupportView: View {
    @State private var expanded: String?

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Frequently asked")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundStyle(Theme.ink)
                    Text("Answers to the questions we hear most often.")
                        .font(.system(size: 13))
                        .foregroundStyle(Theme.inkFaint)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 24)

                VStack(spacing: 0) {
                    ForEach(Array(Legal.faqs.enumerated()), id: \.offset) { index, faq in
                        Button {
                            Haptics.tap()
                            withAnimation(.spring(response: 0.34, dampingFraction: 0.86)) {
                                expanded = expanded == faq.question ? nil : faq.question
                            }
                        } label: {
                            VStack(alignment: .leading, spacing: 9) {
                                HStack(alignment: .top, spacing: 10) {
                                    Text(faq.question)
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundStyle(Theme.ink)
                                        .multilineTextAlignment(.leading)
                                        .fixedSize(horizontal: false, vertical: true)
                                    Spacer(minLength: 4)
                                    Image(systemName: "chevron.down")
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundStyle(Theme.inkFaint)
                                        .rotationEffect(.degrees(expanded == faq.question ? 180 : 0))
                                }
                                if expanded == faq.question {
                                    Text(faq.answer)
                                        .font(.system(size: 14))
                                        .foregroundStyle(Theme.inkSoft)
                                        .lineSpacing(3)
                                        .multilineTextAlignment(.leading)
                                        .fixedSize(horizontal: false, vertical: true)
                                }
                            }
                            .padding(16)
                        }

                        if index < Legal.faqs.count - 1 {
                            Divider().overlay(Theme.hairline).padding(.horizontal, 16)
                        }
                    }
                }
                .cardStyle(radius: 22, padding: 0)
                .padding(.horizontal, 20)

                if !Legal.supportEmail.isEmpty {
                    Button {
                        if let url = URL(string: "mailto:\(Legal.supportEmail)") {
                            UIApplication.shared.open(url)
                        }
                    } label: {
                        HStack(spacing: 10) {
                            Image(systemName: "envelope.fill")
                                .font(.system(size: 15, weight: .semibold))
                            Text("Email support")
                                .font(.system(size: 16, weight: .semibold))
                            Spacer()
                            Image(systemName: "arrow.up.right")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundStyle(Theme.inkFaint)
                        }
                        .foregroundStyle(Theme.ink)
                        .padding(16)
                    }
                    .cardStyle(radius: 22, padding: 0)
                    .padding(.horizontal, 20)
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text("Important")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(Theme.inkFaint)
                    Text(Legal.wellnessDisclaimer)
                        .font(.system(size: 12))
                        .foregroundStyle(Theme.inkSoft)
                        .lineSpacing(3)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(16)
                .cardStyle(radius: 20, padding: 0)
                .padding(.horizontal, 20)
                .padding(.bottom, 40)
            }
            .padding(.top, 12)
        }
        .scrollIndicators(.hidden)
        .background(Theme.backdrop)
        .navigationTitle("Help & Support")
        .navigationBarTitleDisplayMode(.inline)
    }
}
