import Foundation

nonisolated struct FoodRecord: Codable, Identifiable, Hashable {
    var id: String { name }
    let name: String
    let serving: String
    let kcal: Int
    let p: Double
    let c: Double
    let f: Double
    let tags: String

    var asFoodItem: FoodItem {
        FoodItem(name: name, quantity: serving, calories: kcal, protein: p, carbs: c, fat: f)
    }
}

/// Bundled offline food table used by the Search flow.
nonisolated enum FoodDatabase {
    static let all: [FoodRecord] = {
        guard
            let url = Bundle.main.url(forResource: "foods", withExtension: "json"),
            let data = try? Data(contentsOf: url),
            let decoded = try? JSONDecoder().decode([FoodRecord].self, from: data)
        else { return [] }
        return decoded
    }()

    static func search(_ query: String) -> [FoodRecord] {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return Array(all.prefix(24)) }
        return all
            .filter {
                $0.name.localizedCaseInsensitiveContains(trimmed)
                    || $0.tags.localizedCaseInsensitiveContains(trimmed)
            }
            .sorted { lhs, rhs in
                let l = lhs.name.localizedCaseInsensitiveContains(trimmed) ? 0 : 1
                let r = rhs.name.localizedCaseInsensitiveContains(trimmed) ? 0 : 1
                return l == r ? lhs.name < rhs.name : l < r
            }
    }
}

nonisolated struct ExercisePreset: Identifiable, Hashable {
    var id: String { name }
    let name: String
    let symbol: String
    /// Calories burned per minute for an 80 kg person.
    let perMinute: Double

    func calories(minutes: Int, weightKg: Double) -> Int {
        Int((perMinute * Double(minutes) * (weightKg / 80)).rounded())
    }

    static let all: [ExercisePreset] = [
        .init(name: "Walking", symbol: "figure.walk", perMinute: 4.5),
        .init(name: "Running", symbol: "figure.run", perMinute: 11.5),
        .init(name: "Cycling", symbol: "figure.outdoor.cycle", perMinute: 9.0),
        .init(name: "Weights", symbol: "figure.strengthtraining.traditional", perMinute: 6.5),
        .init(name: "HIIT", symbol: "bolt.heart.fill", perMinute: 12.5),
        .init(name: "Swimming", symbol: "figure.pool.swim", perMinute: 10.0),
        .init(name: "Yoga", symbol: "figure.mind.and.body", perMinute: 3.5),
        .init(name: "Football", symbol: "figure.soccer", perMinute: 9.5),
        .init(name: "Tennis", symbol: "figure.tennis", perMinute: 8.0),
        .init(name: "Rowing", symbol: "figure.rower", perMinute: 10.5),
        .init(name: "Boxing", symbol: "figure.boxing", perMinute: 11.0),
        .init(name: "Hiking", symbol: "figure.hiking", perMinute: 7.0)
    ]
}
