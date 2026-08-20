import {
  ageOf,
  bmiCategory,
  bmiOf,
  bmrOf,
  currentSlot,
  maintenanceOf,
  mealCalories,
  mealCarbs,
  mealFat,
  mealProtein,
  mealWithCalories,
  scaleItem,
  targetsOf,
} from "@/lib/nutrition";
import { defaultProfile, type FoodItem, type MealEntry, type UserProfile } from "@/lib/types";

/** A 30-year-old, 80 kg, 180 cm male on the "light" multiplier, losing 0.5 kg/week. */
function profileFor(overrides: Partial<UserProfile> = {}): UserProfile {
  return {
    ...defaultProfile,
    sex: "male",
    birthYear: new Date().getFullYear() - 30,
    heightCm: 180,
    currentWeightKg: 80,
    activity: "light",
    goal: "lose",
    weeklyRateKg: 0.5,
    usesCustomTargets: false,
    ...overrides,
  };
}

function itemFor(overrides: Partial<FoodItem> = {}): FoodItem {
  return {
    id: "item-1",
    name: "Test food",
    quantity: "1 serving",
    calories: 100,
    protein: 10,
    carbs: 10,
    fat: 5,
    ...overrides,
  };
}

function mealFor(items: FoodItem[], portions = 1): MealEntry {
  return {
    id: "meal-1",
    title: "Test meal",
    date: new Date(2026, 0, 1, 12).toISOString(),
    slot: "lunch",
    source: "manual",
    items,
    portions,
    healthScore: 5,
  };
}

const at = (hour: number, minute = 0): Date => new Date(2026, 0, 1, hour, minute);

describe("currentSlot", () => {
  it("maps each boundary hour to the right meal slot", () => {
    expect(currentSlot(at(4))).toBe("breakfast");
    expect(currentSlot(at(10, 59))).toBe("breakfast");
    expect(currentSlot(at(11))).toBe("lunch");
    expect(currentSlot(at(15, 59))).toBe("lunch");
    expect(currentSlot(at(16))).toBe("dinner");
    expect(currentSlot(at(21, 59))).toBe("dinner");
    expect(currentSlot(at(22))).toBe("snack");
  });

  it("treats the small hours as snack", () => {
    expect(currentSlot(at(3))).toBe("snack");
    expect(currentSlot(at(0))).toBe("snack");
  });
});

describe("ageOf", () => {
  it("derives age from the birth year", () => {
    expect(ageOf(profileFor())).toBe(30);
  });

  it("floors age at 13 for an implausible birth year", () => {
    const future = profileFor({ birthYear: new Date().getFullYear() + 5 });
    expect(ageOf(future)).toBe(13);
  });
});

describe("bmrOf", () => {
  // Mifflin–St Jeor: 10w + 6.25h − 5a, then +5 male / −161 female.
  // 10(80) + 6.25(180) − 5(30) = 800 + 1125 − 150 = 1775
  it("adds 5 for male profiles", () => {
    expect(bmrOf(profileFor())).toBe(1780);
  });

  it("subtracts 161 for female profiles", () => {
    expect(bmrOf(profileFor({ sex: "female" }))).toBe(1614);
  });
});

describe("maintenanceOf", () => {
  it("applies the activity multiplier to BMR", () => {
    // 1780 × 1.375 (light)
    expect(maintenanceOf(profileFor())).toBeCloseTo(2447.5, 5);
  });

  it("scales with a higher activity level", () => {
    // 1780 × 1.9 (athlete)
    expect(maintenanceOf(profileFor({ activity: "athlete" }))).toBeCloseTo(3382, 5);
  });
});

describe("targetsOf", () => {
  it("returns the custom targets verbatim when they are enabled", () => {
    const targets = targetsOf(
      profileFor({
        usesCustomTargets: true,
        customCalories: 1800,
        customProtein: 120,
        customCarbs: 200,
        customFat: 60,
      }),
    );
    expect(targets).toEqual({ calories: 1800, protein: 120, carbs: 200, fat: 60 });
  });

  it("subtracts the daily deficit when losing", () => {
    // maintenance 2447.5 − (0.5 × 7700 / 7 = 550) = 1897.5 → 1898
    const targets = targetsOf(profileFor());
    expect(targets.calories).toBe(1898);
    expect(targets.protein).toBe(144); // 80 kg × 1.8
    expect(targets.fat).toBe(57); // 27% of 1898, ÷ 9
    expect(targets.carbs).toBe(202); // remainder ÷ 4
  });

  it("adds a smaller surplus when gaining, and raises protein to 2 g/kg", () => {
    // 2447.5 + (550 × 0.6 = 330) = 2777.5 → 2778
    const targets = targetsOf(profileFor({ goal: "gain" }));
    expect(targets.calories).toBe(2778);
    expect(targets.protein).toBe(160); // 80 kg × 2.0
  });

  it("uses maintenance directly when maintaining", () => {
    const targets = targetsOf(profileFor({ goal: "maintain" }));
    expect(targets.calories).toBe(2448);
    expect(targets.protein).toBe(144);
  });

  it("never drops the calorie target below 1200", () => {
    const extreme = profileFor({
      sex: "female",
      heightCm: 150,
      currentWeightKg: 40,
      activity: "sedentary",
      goal: "lose",
      weeklyRateKg: 1,
    });
    expect(targetsOf(extreme).calories).toBe(1200);
  });

  it("keeps macros consistent with the calorie total", () => {
    const targets = targetsOf(profileFor());
    const fromMacros = targets.protein * 4 + targets.carbs * 4 + targets.fat * 9;
    // Rounding each macro independently allows a few kcal of slack.
    expect(Math.abs(fromMacros - targets.calories)).toBeLessThanOrEqual(5);
  });
});

describe("bmiOf", () => {
  it("computes weight over height squared in metres", () => {
    expect(bmiOf(180, 80)).toBeCloseTo(24.691, 3);
  });

  it("guards against a zero height instead of dividing by zero", () => {
    expect(bmiOf(0, 80)).toBe(0);
  });
});

describe("bmiCategory", () => {
  it("splits on the standard boundaries", () => {
    expect(bmiCategory(18.4)).toBe("Underweight");
    expect(bmiCategory(18.5)).toBe("Healthy");
    expect(bmiCategory(24.9)).toBe("Healthy");
    expect(bmiCategory(25)).toBe("Overweight");
    expect(bmiCategory(29.9)).toBe("Overweight");
    expect(bmiCategory(30)).toBe("Obese");
  });
});

describe("meal totals", () => {
  const meal = mealFor([
    itemFor({ id: "a", calories: 100, protein: 10, carbs: 10, fat: 5 }),
    itemFor({ id: "b", calories: 50, protein: 5, carbs: 5, fat: 2.5 }),
  ]);

  it("sums the items at a single portion", () => {
    expect(mealCalories(meal)).toBe(150);
    expect(mealProtein(meal)).toBe(15);
    expect(mealCarbs(meal)).toBe(15);
    expect(mealFat(meal)).toBe(7.5);
  });

  it("multiplies by the portion count", () => {
    const double = { ...meal, portions: 2 };
    expect(mealCalories(double)).toBe(300);
    expect(mealProtein(double)).toBe(30);
    expect(mealFat(double)).toBe(15);
  });

  it("rounds macros to one decimal for fractional portions", () => {
    const third = { ...meal, portions: 1 / 3 };
    expect(mealProtein(third)).toBe(5);
    expect(mealFat(third)).toBe(2.5);
  });
});

describe("scaleItem", () => {
  it("rounds calories to an integer and macros to one decimal", () => {
    const scaled = scaleItem(itemFor({ calories: 101, protein: 10.44, carbs: 3.33, fat: 1.11 }), 2);
    expect(scaled.calories).toBe(202);
    expect(scaled.protein).toBe(20.9);
    expect(scaled.carbs).toBe(6.7);
    expect(scaled.fat).toBe(2.2);
  });

  it("keeps the item identity intact", () => {
    const scaled = scaleItem(itemFor({ id: "keep-me", name: "Rice" }), 0.5);
    expect(scaled.id).toBe("keep-me");
    expect(scaled.name).toBe("Rice");
  });
});

describe("mealWithCalories", () => {
  it("collapses a meal with no items into one manual entry carrying the target", () => {
    const result = mealWithCalories(mealFor([]), 250);
    expect(result.items).toHaveLength(1);
    expect(result.items[0].calories).toBe(250);
    expect(result.items[0].name).toBe("Test meal");
    expect(result.items[0].quantity).toBe("1 serving");
    expect(result.portions).toBe(1);
  });

  it("falls back to a generic name when the meal has no title", () => {
    const untitled = { ...mealFor([]), title: "   " };
    expect(mealWithCalories(untitled, 100).items[0].name).toBe("Meal");
  });

  it("hits the requested total exactly", () => {
    const meal = mealFor([
      itemFor({ id: "a", calories: 100, protein: 10, carbs: 10, fat: 5 }),
      itemFor({ id: "b", calories: 50, protein: 5, carbs: 5, fat: 2.5 }),
    ]);
    expect(mealCalories(mealWithCalories(meal, 200))).toBe(200);
  });

  it("pushes rounding drift into the largest item so the total is exact", () => {
    // Three 33 kcal items total 99. Scaling to 100 rounds each back to 33,
    // leaving 1 kcal of drift that must land somewhere.
    const meal = mealFor([
      itemFor({ id: "a", calories: 33 }),
      itemFor({ id: "b", calories: 33 }),
      itemFor({ id: "c", calories: 33 }),
    ]);
    const result = mealWithCalories(meal, 100);
    expect(mealCalories(result)).toBe(100);
    expect(result.items.map((item) => item.calories).sort((a, b) => b - a)[0]).toBe(34);
  });

  it("preserves the macro split when scaling", () => {
    const meal = mealFor([itemFor({ calories: 100, protein: 10, carbs: 20, fat: 5 })]);
    const doubled = mealWithCalories(meal, 200);
    expect(doubled.items[0].protein).toBe(20);
    expect(doubled.items[0].carbs).toBe(40);
    expect(doubled.items[0].fat).toBe(10);
  });

  it("clamps a negative target to zero", () => {
    const meal = mealFor([itemFor({ calories: 100 })]);
    expect(mealCalories(mealWithCalories(meal, -50))).toBe(0);
  });

  it("clamps an absurd target to 20000", () => {
    const meal = mealFor([itemFor({ calories: 100 })]);
    expect(mealCalories(mealWithCalories(meal, 999_999))).toBe(20_000);
  });
});
