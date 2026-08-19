import SwiftUI

/// Type-a-meal flow: free text is sent to the model for a nutrition estimate.
struct DescribeMealView: View {
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss

    @State private var text: String = ""
    @State private var isAnalyzing: Bool = false
    @State private var result: AnalysisResult?
    @State private var errorMessage: String?
    @FocusState private var focused: Bool

    private let suggestions: [String] = [
        "Two scrambled eggs on sourdough with avocado",
        "Chicken caesar salad and an iced latte",
        "Large bowl of spaghetti bolognese",
        "Protein shake with banana and peanut butter"
    ]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Describe what you ate")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(Theme.ink)

                        TextField(
                            "e.g. grilled salmon, rice and broccoli",
                            text: $text,
                            axis: .vertical
                        )
                        .font(.system(size: 17))
                        .lineLimit(4...8)
                        .focused($focused)
                        .padding(14)
                        .background(Theme.well, in: RoundedRectangle(cornerRadius: 18, style: .continuous))

                        Text("Include portions when you can — “two slices”, “a big bowl”, “200 g”.")
                            .font(.system(size: 12))
                            .foregroundStyle(Theme.inkFaint)
                    }
                    .cardStyle(radius: 24, padding: 16)

                    Text("QUICK EXAMPLES")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Theme.inkFaint)
                        .padding(.leading, 4)
                        .padding(.top, 4)

                    VStack(spacing: 9) {
                        ForEach(suggestions, id: \.self) { suggestion in
                            Button {
                                Haptics.selection()
                                text = suggestion
                            } label: {
                                HStack(spacing: 10) {
                                    Image(systemName: "text.quote")
                                        .font(.system(size: 13))
                                        .foregroundStyle(Theme.inkFaint)
                                    Text(suggestion)
                                        .font(.system(size: 14))
                                        .foregroundStyle(Theme.inkSoft)
                                        .multilineTextAlignment(.leading)
                                    Spacer(minLength: 0)
                                }
                                .padding(13)
                                .cardStyle(radius: 18, padding: 0)
                            }
                            .buttonStyle(PressableButtonStyle())
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 120)
            }
            .scrollIndicators(.hidden)
            .background(Theme.backdrop)
            .navigationTitle("Type a meal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }.foregroundStyle(Theme.inkSoft)
                }
            }
            .safeAreaInset(edge: .bottom) {
                Button(action: analyze) {
                    HStack(spacing: 8) {
                        if isAnalyzing {
                            ProgressView().tint(.white)
                        } else {
                            Image(systemName: "sparkles")
                        }
                        Text(isAnalyzing ? "Estimating…" : "Estimate nutrition")
                    }
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 17)
                    .background(text.isEmpty ? Theme.inkFaint : Theme.ink, in: Capsule())
                    .padding(.horizontal, 20)
                    .padding(.bottom, 10)
                }
                .buttonStyle(PressableButtonStyle())
                .disabled(text.trimmingCharacters(in: .whitespaces).isEmpty || isAnalyzing)
                .background(.ultraThinMaterial)
            }
            .onAppear { focused = true }
            .fullScreenCover(item: $result) { value in
                MealResultView(image: nil, result: value, source: .text)
                    .onDisappear { dismiss() }
            }
            .alert("Couldn't estimate", isPresented: Binding(
                get: { errorMessage != nil },
                set: { if !$0 { errorMessage = nil } }
            )) {
                Button("OK", role: .cancel) { errorMessage = nil }
            } message: {
                Text(errorMessage ?? "")
            }
        }
    }

    private func analyze() {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        focused = false
        isAnalyzing = true
        Haptics.tap()

        Task {
            do {
                let analysis = try await NutritionAI.analyze(
                    text: trimmed,
                    jesterMode: store.profile.jesterMode,
                    language: store.language.englishName
                )
                await MainActor.run {
                    Haptics.success()
                    isAnalyzing = false
                    result = analysis
                }
            } catch {
                await MainActor.run {
                    Haptics.warning()
                    isAnalyzing = false
                    errorMessage = NutritionAIError.from(error).errorDescription
                        ?? "Something went wrong. Please try again."
                }
            }
        }
    }
}
