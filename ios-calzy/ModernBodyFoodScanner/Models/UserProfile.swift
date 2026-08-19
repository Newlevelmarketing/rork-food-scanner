import Foundation

nonisolated enum Sex: String, Codable, CaseIterable, Identifiable {
    case male, female
    var id: String { rawValue }
    var label: String { rawValue.capitalized }
}

nonisolated enum ActivityLevel: String, Codable, CaseIterable, Identifiable {
    case sedentary, light, moderate, high, athlete
    var id: String { rawValue }

    var label: String {
        switch self {
        case .sedentary: "Sedentary"
        case .light: "Lightly active"
        case .moderate: "Moderately active"
        case .high: "Very active"
        case .athlete: "Athlete"
        }
    }

    var detail: String {
        switch self {
        case .sedentary: "Desk job, little exercise"
        case .light: "1–2 workouts a week"
        case .moderate: "3–4 workouts a week"
        case .high: "5–6 workouts a week"
        case .athlete: "Twice-daily training"
        }
    }

    var multiplier: Double {
        switch self {
        case .sedentary: 1.2
        case .light: 1.375
        case .moderate: 1.55
        case .high: 1.725
        case .athlete: 1.9
        }
    }

    var symbol: String {
        switch self {
        case .sedentary: "chair.lounge"
        case .light: "figure.walk"
        case .moderate: "figure.run"
        case .high: "figure.strengthtraining.traditional"
        case .athlete: "flame.fill"
        }
    }
}

nonisolated enum GoalDirection: String, Codable, CaseIterable, Identifiable {
    case lose, maintain, gain
    var id: String { rawValue }

    var label: String {
        switch self {
        case .lose: "Lose weight"
        case .maintain: "Maintain"
        case .gain: "Build muscle"
        }
    }

    var symbol: String {
        switch self {
        case .lose: "arrow.down.right"
        case .maintain: "equal"
        case .gain: "arrow.up.right"
        }
    }
}

nonisolated enum UnitSystem: String, Codable, CaseIterable, Identifiable {
    case metric, imperial
    var id: String { rawValue }
    var label: String { rawValue.capitalized }
}

/// Everything ModernBodyFoodScanner knows about the person using it, plus their daily targets.
nonisolated struct UserProfile: Codable, Equatable {
    var name: String = ""
    var sex: Sex = .male
    var birthYear: Int = 1996
    var heightCm: Double = 178
    var startWeightKg: Double = 82
    var currentWeightKg: Double = 82
    var goalWeightKg: Double = 76
    var activity: ActivityLevel = .light
    var goal: GoalDirection = .lose
    var weeklyRateKg: Double = 0.5
    var units: UnitSystem = .metric

    /// When true the user has manually overridden the computed targets.
    var usesCustomTargets: Bool = false
    var customCalories: Int = 2200
    var customProtein: Int = 140
    var customCarbs: Int = 240
    var customFat: Int = 70

    var waterGoalMl: Int = 2500
    var jesterMode: Bool = false
    var remindersEnabled: Bool = true
    var reminderTimes: [Int] = [9, 13, 19]
    var hasOnboarded: Bool = false

    /// Language code from `AppLanguage`. Optional so profiles saved before the
    /// language picker shipped still decode; `nil` means "follow the device".
    var languageCode: String?

    var age: Int {
        let year = Calendar.current.component(.year, from: Date())
        return max(13, year - birthYear)
    }

    var bmi: Double {
        let m = heightCm / 100
        guard m > 0 else { return 0 }
        return currentWeightKg / (m * m)
    }

    /// Mifflin–St Jeor basal metabolic rate.
    var bmr: Double {
        let base = 10 * currentWeightKg + 6.25 * heightCm - 5 * Double(age)
        return sex == .male ? base + 5 : base - 161
    }

    var maintenance: Double { bmr * activity.multiplier }

    var targets: NutritionTargets {
        if usesCustomTargets {
            return NutritionTargets(
                calories: customCalories,
                protein: customProtein,
                carbs: customCarbs,
                fat: customFat
            )
        }
        let dailyDelta = (weeklyRateKg * 7700) / 7
        var calories: Double
        switch goal {
        case .lose: calories = maintenance - dailyDelta
        case .maintain: calories = maintenance
        case .gain: calories = maintenance + dailyDelta * 0.6
        }
        calories = max(1200, calories.rounded())

        let proteinPerKg: Double = goal == .gain ? 2.0 : 1.8
        let protein = (currentWeightKg * proteinPerKg).rounded()
        let fat = (calories * 0.27 / 9).rounded()
        let carbs = max(0, ((calories - protein * 4 - fat * 9) / 4).rounded())

        return NutritionTargets(
            calories: Int(calories),
            protein: Int(protein),
            carbs: Int(carbs),
            fat: Int(fat)
        )
    }
}

nonisolated struct NutritionTargets: Codable, Equatable {
    var calories: Int
    var protein: Int
    var carbs: Int
    var fat: Int
}

nonisolated enum BMICategory: String {
    case under = "Underweight"
    case normal = "Healthy"
    case over = "Overweight"
    case obese = "Obese"

    static func from(_ bmi: Double) -> BMICategory {
        switch bmi {
        case ..<18.5: .under
        case ..<25: .normal
        case ..<30: .over
        default: .obese
        }
    }
}
