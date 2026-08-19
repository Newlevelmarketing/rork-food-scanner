import SwiftUI

/// Offline database search with instant logging.
struct SearchFoodView: View {
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss

    @State private var query: String = ""
    @State private var picked: FoodRecord?

    private var results: [FoodRecord] { FoodDatabase.search(query) }

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 9) {
                    ForEach(results) { record in
                        Button {
                            Haptics.tap()
                            picked = record
                        } label: {
                            HStack(spacing: 12) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(record.name)
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundStyle(Theme.ink)
                                        .multilineTextAlignment(.leading)
                                    Text("\(record.serving) · \(Int(record.p))P \(Int(record.c))C \(Int(record.f))F")
                                        .font(.system(size: 12))
                                        .foregroundStyle(Theme.inkFaint)
                                }
                                Spacer(minLength: 0)
                                Text("\(record.kcal)")
                                    .font(.metric(18, .bold))
                                    .foregroundStyle(Theme.ink)
                                Text("kcal")
                                    .font(.system(size: 11))
                                    .foregroundStyle(Theme.inkFaint)
                            }
                            .padding(14)
                            .cardStyle(radius: 20, padding: 0)
                        }
                        .buttonStyle(PressableButtonStyle())
                    }

                    if results.isEmpty {
                        VStack(spacing: 10) {
                            Image(systemName: "magnifyingglass")
                                .font(.system(size: 30))
                                .foregroundStyle(Theme.inkFaint)
                            Text("No matches for “\(query)”")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundStyle(Theme.ink)
                            Text("Try the Type flow instead — the AI can estimate anything.")
                                .font(.system(size: 13))
                                .foregroundStyle(Theme.inkSoft)
                                .multilineTextAlignment(.center)
                        }
                        .padding(.top, 60)
                        .padding(.horizontal, 30)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 40)
            }
            .scrollIndicators(.hidden)
            .background(Theme.backdrop)
            .navigationTitle("Search food")
            .navigationBarTitleDisplayMode(.inline)
            .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: "Search 100+ foods")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }.foregroundStyle(Theme.inkSoft)
                }
            }
            .sheet(item: $picked) { record in
                MealResultView(
                    image: nil,
                    result: AnalysisResult(
                        title: record.name,
                        isFood: true,
                        healthScore: healthScore(for: record),
                        items: [
                            AnalysisResult.Item(
                                name: record.name,
                                quantity: record.serving,
                                calories: record.kcal,
                                protein: record.p,
                                carbs: record.c,
                                fat: record.f
                            )
                        ],
                        quip: nil
                    ),
                    source: .search
                )
                .onDisappear { dismiss() }
            }
        }
    }

    /// Rough nutrient-density heuristic for database entries.
    private func healthScore(for record: FoodRecord) -> Int {
        let kcal = max(Double(record.kcal), 1)
        let proteinRatio = (record.p * 4) / kcal
        let fatRatio = (record.f * 9) / kcal
        var score = 5.0 + proteinRatio * 8 - max(0, fatRatio - 0.4) * 6
        if record.tags.contains("veg") || record.tags.contains("fruit") { score += 2 }
        if record.tags.contains("treat") || record.tags.contains("fastfood") { score -= 2.5 }
        return Int(min(10, max(1, score.rounded())))
    }
}

/// Bookmarked foods for one-tap re-logging.
struct SavedFoodsView: View {
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss

    @State private var picked: SavedFood?

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 9) {
                    ForEach(store.data.saved) { food in
                        Button {
                            Haptics.tap()
                            picked = food
                        } label: {
                            HStack(spacing: 12) {
                                Image(systemName: "bookmark.fill")
                                    .font(.system(size: 15))
                                    .foregroundStyle(Theme.ink)
                                    .frame(width: 42, height: 42)
                                    .background(Theme.well, in: RoundedRectangle(cornerRadius: 14, style: .continuous))

                                VStack(alignment: .leading, spacing: 4) {
                                    Text(food.title)
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundStyle(Theme.ink)
                                        .multilineTextAlignment(.leading)
                                    Text("\(food.items.count) item\(food.items.count == 1 ? "" : "s")")
                                        .font(.system(size: 12))
                                        .foregroundStyle(Theme.inkFaint)
                                }
                                Spacer(minLength: 0)
                                Text("\(food.calories) kcal")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(Theme.inkSoft)
                            }
                            .padding(12)
                            .cardStyle(radius: 20, padding: 0)
                        }
                        .buttonStyle(PressableButtonStyle())
                        .contextMenu {
                            Button(role: .destructive) {
                                store.deleteSaved(food)
                            } label: {
                                Label("Remove", systemImage: "trash")
                            }
                        }
                    }

                    if store.data.saved.isEmpty {
                        VStack(spacing: 10) {
                            Image(systemName: "bookmark")
                                .font(.system(size: 32))
                                .foregroundStyle(Theme.inkFaint)
                            Text("Nothing saved yet")
                                .font(.system(size: 17, weight: .bold))
                                .foregroundStyle(Theme.ink)
                            Text("Tap the bookmark icon when reviewing a meal to keep it here for one-tap logging.")
                                .font(.system(size: 13))
                                .foregroundStyle(Theme.inkSoft)
                                .multilineTextAlignment(.center)
                        }
                        .padding(.top, 70)
                        .padding(.horizontal, 34)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 40)
            }
            .scrollIndicators(.hidden)
            .background(Theme.backdrop)
            .navigationTitle("Saved meals")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Close") { dismiss() }.foregroundStyle(Theme.inkSoft)
                }
            }
            .sheet(item: $picked) { food in
                MealResultView(
                    image: nil,
                    result: AnalysisResult(
                        title: food.title,
                        isFood: true,
                        healthScore: 7,
                        items: food.items.map {
                            AnalysisResult.Item(
                                name: $0.name,
                                quantity: $0.quantity,
                                calories: $0.calories,
                                protein: $0.protein,
                                carbs: $0.carbs,
                                fat: $0.fat
                            )
                        },
                        quip: nil
                    ),
                    source: .saved
                )
                .onDisappear { dismiss() }
            }
        }
    }
}
