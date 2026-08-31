//
//  NutritionMathTests.swift
//  ModernBodyFoodScannerTests
//
//  Mirrors web/src/test/nutrition.test.ts.
//
//  iOS and web are hand-written mirrors of the same equations, and
//  context/architecture.md records drift between the platforms as a known risk.
//  The web side has 46 tests over this maths; this file exists so the two can be
//  shown to agree rather than assumed to.
//
//  Expected values below are the same hand-computed numbers the web suite uses,
//  so a failure here means the platforms have diverged - not that a number was
//  copied from whatever the code happened to return.
//

import Foundation
import Testing

@testable import ModernBodyFoodScanner

private let tolerance = 0.0001

/// A 30-year-old, 80 kg, 180 cm male on the "light" multiplier, losing 0.5 kg/week.
private func makeProfile() -> UserProfile {
    var profile = UserProfile()
    profile.sex = .male
    profile.birthYear = Calendar.current.component(.year, from: Date()) - 30
    profile.heightCm = 180
    profile.currentWeightKg = 80
    profile.activity = .light
    profile.goal = .lose
    profile.weeklyRateKg = 0.5
    profile.usesCustomTargets = false
    return profile
}

private func makeItem(
    name: String = "Test food",
    calories: Int = 100,
    protein: Double = 10,
    carbs: Double = 10,
    fat: Double = 5
) -> FoodItem {
    FoodItem(name: name, quantity: "1 serving", calories: calories, protein: protein, carbs: carbs, fat: fat)
}

private func makeMeal(items: [FoodItem], portions: Double = 1, title: String = "Test meal") -> MealEntry {
    var meal = MealEntry(title: title, date: Date(), slot: .lunch, source: .manual, items: items)
    meal.portions = portions
    return meal
}

private func date(hour: Int, minute: Int = 0) -> Date {
    Calendar.current.date(
        from: DateComponents(year: 2026, month: 1, day: 1, hour: hour, minute: minute)
    )!
}

struct AgeAndBMRTests {
    @Test func ageDerivesFromBirthYear() {
        #expect(makeProfile().age == 30)
    }

    @Test func ageFloorsAt13ForAnImplausibleBirthYear() {
        var profile = makeProfile()
        profile.birthYear = Calendar.current.component(.year, from: Date()) + 5
        #expect(profile.age == 13)
    }

    // Mifflin-St Jeor: 10w + 6.25h - 5a, then +5 male / -161 female.
    // 10(80) + 6.25(180) - 5(30) = 800 + 1125 - 150 = 1775
    @Test func bmrAddsFiveForMale() {
        #expect(abs(makeProfile().bmr - 1780) < tolerance)
    }

    @Test func bmrSubtracts161ForFemale() {
        var profile = makeProfile()
        profile.sex = .female
        #expect(abs(profile.bmr - 1614) < tolerance)
    }

    @Test func maintenanceAppliesTheActivityMultiplier() {
        // 1780 x 1.375 (light)
        #expect(abs(makeProfile().maintenance - 2447.5) < tolerance)
    }

    @Test func maintenanceScalesWithActivity() {
        var profile = makeProfile()
        profile.activity = .athlete
        // 1780 x 1.9
        #expect(abs(profile.maintenance - 3382) < tolerance)
    }
}

struct TargetsTests {
    @Test func customTargetsAreReturnedVerbatim() {
        var profile = makeProfile()
        profile.usesCustomTargets = true
        profile.customCalories = 1800
        profile.customProtein = 120
        profile.customCarbs = 200
        profile.customFat = 60

        let targets = profile.targets
        #expect(targets.calories == 1800)
        #expect(targets.protein == 120)
        #expect(targets.carbs == 200)
        #expect(targets.fat == 60)
    }

    @Test func losingSubtractsTheDailyDeficit() {
        // maintenance 2447.5 - (0.5 x 7700 / 7 = 550) = 1897.5 -> 1898
        let targets = makeProfile().targets
        #expect(targets.calories == 1898)
        #expect(targets.protein == 144)  // 80 kg x 1.8
        #expect(targets.fat == 57)       // 27% of 1898, / 9
        #expect(targets.carbs == 202)    // remainder / 4
    }

    @Test func gainingAddsASmallerSurplusAndRaisesProtein() {
        var profile = makeProfile()
        profile.goal = .gain
        // 2447.5 + (550 x 0.6 = 330) = 2777.5 -> 2778
        let targets = profile.targets
        #expect(targets.calories == 2778)
        #expect(targets.protein == 160)  // 80 kg x 2.0
    }

    @Test func maintainingUsesMaintenanceDirectly() {
        var profile = makeProfile()
        profile.goal = .maintain
        let targets = profile.targets
        #expect(targets.calories == 2448)
        #expect(targets.protein == 144)
    }

    @Test func caloriesNeverDropBelow1200() {
        var profile = makeProfile()
        profile.sex = .female
        profile.heightCm = 150
        profile.currentWeightKg = 40
        profile.activity = .sedentary
        profile.goal = .lose
        profile.weeklyRateKg = 1
        #expect(profile.targets.calories == 1200)
    }

    @Test func macrosStayConsistentWithTheCalorieTotal() {
        let targets = makeProfile().targets
        let fromMacros = targets.protein * 4 + targets.carbs * 4 + targets.fat * 9
        // Rounding each macro independently allows a few kcal of slack.
        #expect(abs(fromMacros - targets.calories) <= 5)
    }
}

struct BMITests {
    @Test func bmiIsWeightOverHeightSquaredInMetres() {
        #expect(abs(makeProfile().bmi - 24.6913) < 0.001)
    }

    @Test func bmiGuardsAgainstZeroHeight() {
        var profile = makeProfile()
        profile.heightCm = 0
        #expect(profile.bmi == 0)
    }

    @Test func bmiCategorySplitsOnTheStandardBoundaries() {
        #expect(BMICategory.from(18.4) == .under)
        #expect(BMICategory.from(18.5) == .normal)
        #expect(BMICategory.from(24.9) == .normal)
        #expect(BMICategory.from(25) == .over)
        #expect(BMICategory.from(29.9) == .over)
        #expect(BMICategory.from(30) == .obese)
    }
}

struct MealSlotTests {
    @Test func eachBoundaryHourMapsToTheRightSlot() {
        #expect(MealSlot.current(at: date(hour: 4)) == .breakfast)
        #expect(MealSlot.current(at: date(hour: 10, minute: 59)) == .breakfast)
        #expect(MealSlot.current(at: date(hour: 11)) == .lunch)
        #expect(MealSlot.current(at: date(hour: 15, minute: 59)) == .lunch)
        #expect(MealSlot.current(at: date(hour: 16)) == .dinner)
        #expect(MealSlot.current(at: date(hour: 21, minute: 59)) == .dinner)
        #expect(MealSlot.current(at: date(hour: 22)) == .snack)
    }

    @Test func theSmallHoursAreSnack() {
        #expect(MealSlot.current(at: date(hour: 0)) == .snack)
        #expect(MealSlot.current(at: date(hour: 3)) == .snack)
    }
}

struct MealTotalsTests {
    private func twoItemMeal(portions: Double = 1) -> MealEntry {
        makeMeal(
            items: [
                makeItem(name: "A", calories: 100, protein: 10, carbs: 10, fat: 5),
                makeItem(name: "B", calories: 50, protein: 5, carbs: 5, fat: 2.5),
            ],
            portions: portions
        )
    }

    @Test func sumsTheItemsAtASinglePortion() {
        let meal = twoItemMeal()
        #expect(meal.calories == 150)
        #expect(abs(meal.protein - 15) < tolerance)
        #expect(abs(meal.carbs - 15) < tolerance)
        #expect(abs(meal.fat - 7.5) < tolerance)
    }

    @Test func multipliesByThePortionCount() {
        let meal = twoItemMeal(portions: 2)
        #expect(meal.calories == 300)
        #expect(abs(meal.protein - 30) < tolerance)
        #expect(abs(meal.fat - 15) < tolerance)
    }

    @Test func roundsMacrosToOneDecimalForFractionalPortions() {
        let meal = twoItemMeal(portions: 1.0 / 3.0)
        #expect(abs(meal.protein - 5) < tolerance)
        #expect(abs(meal.fat - 2.5) < tolerance)
    }
}

struct ScaledItemTests {
    @Test func roundsCaloriesToAnIntegerAndMacrosToOneDecimal() {
        let scaled = FoodItem.scaled(
            makeItem(calories: 101, protein: 10.44, carbs: 3.33, fat: 1.11),
            by: 2
        )
        #expect(scaled.calories == 202)
        #expect(abs(scaled.protein - 20.9) < tolerance)
        #expect(abs(scaled.carbs - 6.7) < tolerance)
        #expect(abs(scaled.fat - 2.2) < tolerance)
    }

    @Test func keepsTheItemIdentityIntact() {
        let original = makeItem(name: "Rice")
        let scaled = FoodItem.scaled(original, by: 0.5)
        #expect(scaled.id == original.id)
        #expect(scaled.name == "Rice")
    }
}

struct SettingCaloriesTests {
    @Test func collapsesAMealWithNoItemsIntoOneManualEntry() {
        let result = makeMeal(items: []).settingCalories(250)
        #expect(result.items.count == 1)
        #expect(result.items[0].calories == 250)
        #expect(result.items[0].name == "Test meal")
        #expect(result.items[0].quantity == "1 serving")
        #expect(abs(result.portions - 1) < tolerance)
    }

    @Test func fallsBackToAGenericNameWhenTheMealHasNoTitle() {
        // NOTE: only the genuinely empty title is asserted. The web mirror also
        // trims, so a whitespace-only title becomes "Meal" there and stays
        // whitespace here. That divergence is recorded as drift in
        // context/feature-specs/18-ios-nutrition-tests.md rather than pinned as
        // correct behaviour by a test.
        let untitled = makeMeal(items: [], title: "")
        #expect(untitled.settingCalories(100).items[0].name == "Meal")
    }

    @Test func hitsTheRequestedTotalExactly() {
        let meal = makeMeal(items: [
            makeItem(name: "A", calories: 100, protein: 10, carbs: 10, fat: 5),
            makeItem(name: "B", calories: 50, protein: 5, carbs: 5, fat: 2.5),
        ])
        #expect(meal.settingCalories(200).calories == 200)
    }

    @Test func pushesRoundingDriftIntoTheLargestItem() {
        // Three 33 kcal items total 99. Scaling to 100 rounds each back to 33,
        // leaving 1 kcal of drift that must land somewhere.
        //
        // Deliberately agnostic about *which* item receives it: web keeps the
        // first maximum on a tie and Swift's max(by:) keeps the last, so
        // asserting an index would fail on one platform for no useful reason.
        let meal = makeMeal(items: [
            makeItem(name: "A", calories: 33),
            makeItem(name: "B", calories: 33),
            makeItem(name: "C", calories: 33),
        ])
        let result = meal.settingCalories(100)
        #expect(result.calories == 100)
        #expect(result.items.map(\.calories).max() == 34)
    }

    @Test func preservesTheMacroSplitWhenScaling() {
        let meal = makeMeal(items: [makeItem(calories: 100, protein: 10, carbs: 20, fat: 5)])
        let doubled = meal.settingCalories(200)
        #expect(abs(doubled.items[0].protein - 20) < tolerance)
        #expect(abs(doubled.items[0].carbs - 40) < tolerance)
        #expect(abs(doubled.items[0].fat - 10) < tolerance)
    }

    @Test func clampsANegativeTargetToZero() {
        let meal = makeMeal(items: [makeItem(calories: 100)])
        #expect(meal.settingCalories(-50).calories == 0)
    }

    @Test func clampsAnAbsurdTargetTo20000() {
        let meal = makeMeal(items: [makeItem(calories: 100)])
        #expect(meal.settingCalories(999_999).calories == 20_000)
    }
}
