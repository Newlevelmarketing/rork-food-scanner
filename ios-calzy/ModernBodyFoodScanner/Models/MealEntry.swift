import Foundation

nonisolated enum MealSlot: String, Codable, CaseIterable, Identifiable {
    case breakfast, lunch, dinner, snack
    var id: String { rawValue }
    var label: String { rawValue.capitalized }

    var symbol: String {
        switch self {
        case .breakfast: "sunrise.fill"
        case .lunch: "sun.max.fill"
        case .dinner: "moon.stars.fill"
        case .snack: "takeoutbag.and.cup.and.straw.fill"
        }
    }

    static func current(at date: Date = Date()) -> MealSlot {
        switch Calendar.current.component(.hour, from: date) {
        case 4..<11: .breakfast
        case 11..<16: .lunch
        case 16..<22: .dinner
        default: .snack
        }
    }
}

nonisolated enum EntrySource: String, Codable {
    case photo, text, search, saved, manual

    var symbol: String {
        switch self {
        case .photo: "camera.fill"
        case .text: "character.cursor.ibeam"
        case .search: "magnifyingglass"
        case .saved: "bookmark.fill"
        case .manual: "square.and.pencil"
        }
    }
}

/// One food item inside a logged meal.
nonisolated struct FoodItem: Codable, Identifiable, Equatable, Hashable {
    var id: UUID = UUID()
    var name: String
    var quantity: String
    var calories: Int
    var protein: Double
    var carbs: Double
    var fat: Double

    static func scaled(_ item: FoodItem, by factor: Double) -> FoodItem {
        FoodItem(
            id: item.id,
            name: item.name,
            quantity: item.quantity,
            calories: Int((Double(item.calories) * factor).rounded()),
            protein: (item.protein * factor * 10).rounded() / 10,
            carbs: (item.carbs * factor * 10).rounded() / 10,
            fat: (item.fat * factor * 10).rounded() / 10
        )
    }
}

/// A logged meal, optionally backed by a photo the user scanned.
nonisolated struct MealEntry: Codable, Identifiable, Equatable {
    var id: UUID = UUID()
    var title: String
    var date: Date
    var slot: MealSlot
    var source: EntrySource
    var items: [FoodItem]
    var portions: Double = 1
    var photoFileName: String?
    var healthScore: Int = 7
    var note: String?
    var quip: String?

    var calories: Int { Int((Double(items.reduce(0) { $0 + $1.calories }) * portions).rounded()) }
    var protein: Double { (items.reduce(0) { $0 + $1.protein } * portions * 10).rounded() / 10 }
    var carbs: Double { (items.reduce(0) { $0 + $1.carbs } * portions * 10).rounded() / 10 }
    var fat: Double { (items.reduce(0) { $0 + $1.fat } * portions * 10).rounded() / 10 }

    /// Returns a copy whose total calories equal `target`.
    ///
    /// Calories are derived from the item list, so a manual correction scales
    /// every item's energy and macros by the same factor — the macro split the
    /// user already sees stays intact. Meals with no nutrition yet collapse to a
    /// single manual item carrying the entered calories.
    func settingCalories(_ target: Int) -> MealEntry {
        var copy = self
        let clamped = max(0, min(target, 20000))
        let current = calories

        guard current > 0, !items.isEmpty else {
            copy.items = [
                FoodItem(
                    name: title.isEmpty ? "Meal" : title,
                    quantity: "1 serving",
                    calories: clamped,
                    protein: 0,
                    carbs: 0,
                    fat: 0
                )
            ]
            copy.portions = 1
            return copy
        }

        let factor = Double(clamped) / Double(current)
        copy.items = items.map { FoodItem.scaled($0, by: factor) }

        // Per-item rounding can drift a few kcal off the requested total; push the
        // remainder into the largest item so the row shows exactly what was typed.
        let drift = clamped - copy.calories
        if drift != 0, copy.portions > 0,
           let index = copy.items.indices.max(by: { copy.items[$0].calories < copy.items[$1].calories }) {
            let correction = Int((Double(drift) / copy.portions).rounded())
            copy.items[index].calories = max(0, copy.items[index].calories + correction)
        }

        return copy
    }
}

/// Burned-calorie entry that offsets the daily budget.
nonisolated struct ExerciseEntry: Codable, Identifiable, Equatable {
    var id: UUID = UUID()
    var name: String
    var date: Date
    var minutes: Int
    var calories: Int
    var symbol: String
}

nonisolated struct WaterEntry: Codable, Identifiable, Equatable {
    var id: UUID = UUID()
    var date: Date
    var milliliters: Int
}

nonisolated struct WeightEntry: Codable, Identifiable, Equatable {
    var id: UUID = UUID()
    var date: Date
    var kilograms: Double
}

nonisolated struct ProgressPhoto: Codable, Identifiable, Equatable {
    var id: UUID = UUID()
    var date: Date
    var fileName: String
    var weightKg: Double?
}

/// A food the user bookmarked for one-tap re-logging.
nonisolated struct SavedFood: Codable, Identifiable, Equatable {
    var id: UUID = UUID()
    var title: String
    var items: [FoodItem]
    var slot: MealSlot = .snack

    var calories: Int { items.reduce(0) { $0 + $1.calories } }
}
