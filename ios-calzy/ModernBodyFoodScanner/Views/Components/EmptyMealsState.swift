import SwiftUI

/// Empty state shown when a day has no meals yet.
///
/// Renders a ghosted meal row on top of a small stack of cards so a blank day
/// previews the shape of what a logged meal will look like.
struct EmptyMealsState: View {
    @State private var isFloating = false

    private let cardHeight: CGFloat = 76

    var body: some View {
        VStack(spacing: 18) {
            ZStack {
                stackedCard(inset: 34, offsetY: 13, opacity: 0.4, radius: 16)
                stackedCard(inset: 20, offsetY: 7, opacity: 0.68, radius: 17)
                ghostRow
            }
            .padding(.bottom, 13)
            .offset(y: isFloating ? -3 : 0)

            Text(L("h.empty"))
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(Theme.inkSoft)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 18)
        .padding(.vertical, 22)
        .background {
            RoundedRectangle(cornerRadius: 26, style: .continuous)
                .fill(.white.opacity(0.4))
                .overlay {
                    RoundedRectangle(cornerRadius: 26, style: .continuous)
                        .strokeBorder(Theme.ink.opacity(0.06), lineWidth: 1)
                }
        }
        .padding(.horizontal, 20)
        .onAppear {
            withAnimation(.easeInOut(duration: 2.6).repeatForever(autoreverses: true)) {
                isFloating = true
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(L("h.empty"))
    }

    private var ghostRow: some View {
        HStack(spacing: 14) {
            Text(verbatim: "\u{1F957}")
                .font(.system(size: 32))

            VStack(alignment: .leading, spacing: 10) {
                Capsule()
                    .fill(Theme.ink.opacity(0.07))
                    .frame(height: 10)
                Capsule()
                    .fill(Theme.ink.opacity(0.07))
                    .frame(height: 10)
                    .padding(.trailing, 58)
            }
        }
        .padding(.horizontal, 16)
        .frame(height: cardHeight)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(.white)
                .shadow(color: .black.opacity(0.05), radius: 10, y: 4)
        )
        .padding(.horizontal, 8)
    }

    private func stackedCard(
        inset: CGFloat,
        offsetY: CGFloat,
        opacity: Double,
        radius: CGFloat
    ) -> some View {
        RoundedRectangle(cornerRadius: radius, style: .continuous)
            .fill(.white.opacity(opacity))
            .frame(height: cardHeight)
            .padding(.horizontal, inset)
            .offset(y: offsetY)
    }
}
