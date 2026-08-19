import SwiftUI

/// Preview-then-share sheet for the day's summary card.
///
/// The card is rasterised once with `ImageRenderer` so what the user sees in
/// the preview is byte-identical to what leaves the app.
struct ShareSummaryView: View {
    let summary: DailySummary

    @Environment(\.dismiss) private var dismiss
    @State private var rendered: Image?

    var body: some View {
        NavigationStack {
            ScrollView {
                DailySummaryCard(summary: summary)
                    .clipShape(.rect(cornerRadius: 26, style: .continuous))
                    .shadow(color: .black.opacity(0.10), radius: 26, x: 0, y: 12)
                    .padding(.vertical, 24)
                    .frame(maxWidth: .infinity)
            }
            .scrollIndicators(.hidden)
            .background(Theme.backdrop)
            .navigationTitle("Share your day")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(Theme.inkSoft)
                }
            }
            .safeAreaInset(edge: .bottom) { shareBar }
        }
        .task { rendered = renderCard() }
    }

    private var shareBar: some View {
        Group {
            if let rendered {
                ShareLink(
                    item: rendered,
                    preview: SharePreview("Daily Summary", image: rendered)
                ) {
                    HStack(spacing: 8) {
                        Image(systemName: "square.and.arrow.up")
                            .font(.system(size: 16, weight: .semibold))
                        Text("Share summary")
                            .font(.system(size: 17, weight: .semibold))
                    }
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(Theme.ink, in: Capsule())
                }
                .simultaneousGesture(TapGesture().onEnded { Haptics.tap() })
            } else {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 18)
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 10)
        .padding(.bottom, 8)
        .background(.ultraThinMaterial)
    }

    @MainActor
    private func renderCard() -> Image? {
        let renderer = ImageRenderer(content: DailySummaryCard(summary: summary))
        renderer.scale = 3
        guard let image = renderer.uiImage else { return nil }
        return Image(uiImage: image)
    }
}
