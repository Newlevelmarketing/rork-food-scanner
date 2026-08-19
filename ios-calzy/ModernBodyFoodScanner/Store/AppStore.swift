import Foundation
import SwiftUI
import UIKit

nonisolated struct AppData: Codable {
    var profile: UserProfile = UserProfile()
    var meals: [MealEntry] = []
    var exercises: [ExerciseEntry] = []
    var water: [WaterEntry] = []
    var weights: [WeightEntry] = []
    var photos: [ProgressPhoto] = []
    var saved: [SavedFood] = []
}

/// Single source of truth for everything the user logs, persisted as JSON in Documents.
@Observable
final class AppStore {
    private(set) var data: AppData
    var selectedDate: Date = Calendar.current.startOfDay(for: Date())

    private let fileURL: URL
    private let imagesDirectory: URL

    init() {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        fileURL = docs.appendingPathComponent("calzy-data.json")
        imagesDirectory = docs.appendingPathComponent("images", isDirectory: true)
        try? FileManager.default.createDirectory(at: imagesDirectory, withIntermediateDirectories: true)

        if let raw = try? Data(contentsOf: fileURL),
           let decoded = try? JSONDecoder().decode(AppData.self, from: raw) {
            data = decoded
        } else {
            data = AppData()
        }

        Localization.shared.language = AppLanguage.named(data.profile.languageCode)
            ?? AppLanguage.deviceDefault
    }

    // MARK: - Persistence

    private func persist() {
        let snapshot = data
        Task.detached(priority: .utility) { [fileURL] in
            let encoder = JSONEncoder()
            encoder.outputFormatting = .prettyPrinted
            guard let encoded = try? encoder.encode(snapshot) else { return }
            try? encoded.write(to: fileURL, options: .atomic)
        }
    }

    // MARK: - Profile

    var profile: UserProfile {
        get { data.profile }
        set {
            data.profile = newValue
            persist()
        }
    }

    var targets: NutritionTargets { data.profile.targets }

    /// The language the interface and the AI both speak.
    var language: AppLanguage {
        get { AppLanguage.named(data.profile.languageCode) ?? Localization.shared.language }
        set {
            data.profile.languageCode = newValue.code
            Localization.shared.language = newValue
            persist()
        }
    }

    func completeOnboarding() {
        data.profile.hasOnboarded = true
        if data.weights.isEmpty {
            data.weights.append(WeightEntry(date: Date(), kilograms: data.profile.currentWeightKg))
        }
        persist()
    }

    // MARK: - Day queries

    func isSameDay(_ a: Date, _ b: Date) -> Bool {
        Calendar.current.isDate(a, inSameDayAs: b)
    }

    /// Newest first. `sorted(by:)` is not stable, so entries sharing a timestamp
    /// fall back to insertion order (latest logged wins) to keep the list fixed
    /// across redraws.
    func meals(on date: Date) -> [MealEntry] {
        data.meals
            .enumerated()
            .filter { isSameDay($0.element.date, date) }
            .sorted { newestFirst($0, $1, by: \.date) }
            .map(\.element)
    }

    func exercises(on date: Date) -> [ExerciseEntry] {
        data.exercises
            .enumerated()
            .filter { isSameDay($0.element.date, date) }
            .sorted { newestFirst($0, $1, by: \.date) }
            .map(\.element)
    }

    private func newestFirst<T>(
        _ lhs: (offset: Int, element: T),
        _ rhs: (offset: Int, element: T),
        by date: KeyPath<T, Date>
    ) -> Bool {
        let a = lhs.element[keyPath: date]
        let b = rhs.element[keyPath: date]
        return a == b ? lhs.offset > rhs.offset : a > b
    }

    func caloriesEaten(on date: Date) -> Int {
        meals(on: date).reduce(0) { $0 + $1.calories }
    }

    func caloriesBurned(on date: Date) -> Int {
        exercises(on: date).reduce(0) { $0 + $1.calories }
    }

    func protein(on date: Date) -> Double { meals(on: date).reduce(0) { $0 + $1.protein } }
    func carbs(on date: Date) -> Double { meals(on: date).reduce(0) { $0 + $1.carbs } }
    func fat(on date: Date) -> Double { meals(on: date).reduce(0) { $0 + $1.fat } }

    func water(on date: Date) -> Int {
        data.water.filter { isSameDay($0.date, date) }.reduce(0) { $0 + $1.milliliters }
    }

    func hasLogs(on date: Date) -> Bool {
        !meals(on: date).isEmpty || !exercises(on: date).isEmpty
    }

    /// Consecutive days (ending today or yesterday) with at least one logged meal.
    var streak: Int {
        let calendar = Calendar.current
        var count = 0
        var cursor = calendar.startOfDay(for: Date())
        if meals(on: cursor).isEmpty {
            guard let yesterday = calendar.date(byAdding: .day, value: -1, to: cursor),
                  !meals(on: yesterday).isEmpty else { return 0 }
            cursor = yesterday
        }
        while !meals(on: cursor).isEmpty {
            count += 1
            guard let previous = calendar.date(byAdding: .day, value: -1, to: cursor) else { break }
            cursor = previous
        }
        return count
    }

    /// Average calories over the last `days` days, counting only days with logs.
    func averageCalories(days: Int = 7) -> (average: Int, logged: Int, total: Int) {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        var total = 0
        var logged = 0
        for offset in 0..<days {
            guard let day = calendar.date(byAdding: .day, value: -offset, to: today) else { continue }
            let value = caloriesEaten(on: day)
            if value > 0 {
                total += value
                logged += 1
            }
        }
        return (logged == 0 ? 0 : total / logged, logged, days)
    }

    // MARK: - Mutations

    func addMeal(_ meal: MealEntry) {
        data.meals.append(meal)
        persist()
    }

    func updateMeal(_ meal: MealEntry) {
        guard let index = data.meals.firstIndex(where: { $0.id == meal.id }) else { return }
        data.meals[index] = meal
        persist()
    }

    func deleteMeal(_ meal: MealEntry) {
        if let name = meal.photoFileName {
            try? FileManager.default.removeItem(at: imagesDirectory.appendingPathComponent(name))
        }
        data.meals.removeAll { $0.id == meal.id }
        persist()
    }

    func addExercise(_ entry: ExerciseEntry) {
        data.exercises.append(entry)
        persist()
    }

    func deleteExercise(_ entry: ExerciseEntry) {
        data.exercises.removeAll { $0.id == entry.id }
        persist()
    }

    func addWater(_ ml: Int, on date: Date) {
        data.water.append(WaterEntry(date: date, milliliters: ml))
        persist()
    }

    func undoWater(on date: Date) {
        if let index = data.water.lastIndex(where: { isSameDay($0.date, date) }) {
            data.water.remove(at: index)
            persist()
        }
    }

    // MARK: - Weight

    var weightEntries: [WeightEntry] {
        data.weights.sorted { $0.date < $1.date }
    }

    func weight(on date: Date) -> WeightEntry? {
        data.weights.first { isSameDay($0.date, date) }
    }

    func logWeight(_ kg: Double, on date: Date = Date()) {
        if let index = data.weights.firstIndex(where: { isSameDay($0.date, date) }) {
            data.weights[index].kilograms = kg
        } else {
            data.weights.append(WeightEntry(date: date, kilograms: kg))
        }
        if isSameDay(date, Date()) || date > (weightEntries.last?.date ?? .distantPast) {
            data.profile.currentWeightKg = kg
        }
        persist()
    }

    func deleteWeight(_ entry: WeightEntry) {
        data.weights.removeAll { $0.id == entry.id }
        persist()
    }

    // MARK: - Saved foods

    func toggleSaved(title: String, items: [FoodItem], slot: MealSlot) {
        if let index = data.saved.firstIndex(where: { $0.title.caseInsensitiveCompare(title) == .orderedSame }) {
            data.saved.remove(at: index)
        } else {
            data.saved.append(SavedFood(title: title, items: items, slot: slot))
        }
        persist()
    }

    func isSaved(title: String) -> Bool {
        data.saved.contains { $0.title.caseInsensitiveCompare(title) == .orderedSame }
    }

    func deleteSaved(_ food: SavedFood) {
        data.saved.removeAll { $0.id == food.id }
        persist()
    }

    // MARK: - Images

    func saveImage(_ image: UIImage) -> String? {
        guard let jpeg = image.jpegData(compressionQuality: 0.8) else { return nil }
        let name = "\(UUID().uuidString).jpg"
        do {
            try jpeg.write(to: imagesDirectory.appendingPathComponent(name), options: .atomic)
            return name
        } catch {
            return nil
        }
    }

    func image(named name: String?) -> UIImage? {
        guard let name else { return nil }
        return UIImage(contentsOfFile: imagesDirectory.appendingPathComponent(name).path)
    }

    func addProgressPhoto(_ image: UIImage) {
        guard let name = saveImage(image) else { return }
        data.photos.append(
            ProgressPhoto(date: Date(), fileName: name, weightKg: data.profile.currentWeightKg)
        )
        persist()
    }

    func deletePhoto(_ photo: ProgressPhoto) {
        try? FileManager.default.removeItem(at: imagesDirectory.appendingPathComponent(photo.fileName))
        data.photos.removeAll { $0.id == photo.id }
        persist()
    }

    var photos: [ProgressPhoto] { data.photos.sorted { $0.date > $1.date } }

    // MARK: - Reset

    func eraseAll() {
        data = AppData()
        selectedDate = Calendar.current.startOfDay(for: Date())
        persist()
    }
}
