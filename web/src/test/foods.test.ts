import {
  allFoods,
  exercisePresets,
  foodToItem,
  presetCalories,
  searchFoods,
  type FoodRecord,
} from "@/lib/foods";

describe("allFoods", () => {
  it("bundles the offline food table", () => {
    expect(allFoods).toHaveLength(100);
  });

  it("gives every record a name, a serving and non-negative macros", () => {
    for (const food of allFoods) {
      expect(food.name.trim().length).toBeGreaterThan(0);
      expect(food.serving.trim().length).toBeGreaterThan(0);
      expect(food.kcal).toBeGreaterThanOrEqual(0);
      expect(food.p).toBeGreaterThanOrEqual(0);
      expect(food.c).toBeGreaterThanOrEqual(0);
      expect(food.f).toBeGreaterThanOrEqual(0);
    }
  });

  it("has no duplicate names, which would make search results ambiguous", () => {
    const names = allFoods.map((food) => food.name.toLowerCase());
    expect(new Set(names).size).toBe(names.length);
  });
});

describe("searchFoods", () => {
  it("returns a 24-item starter list for an empty query", () => {
    expect(searchFoods("")).toHaveLength(24);
  });

  it("treats a whitespace-only query as empty", () => {
    expect(searchFoods("   ")).toHaveLength(24);
  });

  it("matches names case-insensitively", () => {
    const lower = searchFoods(allFoods[0].name.toLowerCase());
    const upper = searchFoods(allFoods[0].name.toUpperCase());
    expect(lower.length).toBeGreaterThan(0);
    expect(upper).toEqual(lower);
  });

  it("ignores surrounding whitespace", () => {
    const name = allFoods[0].name;
    expect(searchFoods(`  ${name}  `)).toEqual(searchFoods(name));
  });

  it("returns nothing for a query that matches neither name nor tag", () => {
    expect(searchFoods("zzzzzzzznotafood")).toEqual([]);
  });

  it("ranks name matches above tag-only matches", () => {
    // Find a query where both kinds exist, so the ranking is actually exercised
    // rather than assumed. The bundled table currently yields 6 such queries;
    // the first is "protein", with 2 name matches against 9 tag-only ones.
    //
    // Asserting the query exists rather than returning early on purpose: a
    // silent skip would leave this reading as coverage while testing nothing.
    const query = findRankableQuery();
    expect(query).not.toBeNull();
    if (query === null) return;

    const results = searchFoods(query);
    const isNameMatch = (food: FoodRecord): boolean => food.name.toLowerCase().includes(query);
    const firstTagOnly = results.findIndex((food) => !isNameMatch(food));
    const lastName = results.map(isNameMatch).lastIndexOf(true);

    expect(firstTagOnly).toBeGreaterThan(-1);
    expect(lastName).toBeLessThan(firstTagOnly);
  });

  it("sorts alphabetically within a rank", () => {
    const query = findRankableQuery();
    expect(query).not.toBeNull();
    if (query === null) return;

    const results = searchFoods(query);
    const nameMatches = results
      .filter((food) => food.name.toLowerCase().includes(query))
      .map((food) => food.name);
    const sorted = [...nameMatches].sort((a, b) => a.localeCompare(b));
    expect(nameMatches).toEqual(sorted);
  });
});

/** A query matching at least one name and at least one tag-only record, or null. */
function findRankableQuery(): string | null {
  const candidates = new Set<string>();
  for (const food of allFoods) {
    for (const tag of food.tags.toLowerCase().split(/[\s,]+/)) {
      if (tag.length >= 3) candidates.add(tag);
    }
  }
  for (const query of candidates) {
    const matches = allFoods.filter(
      (food) => food.name.toLowerCase().includes(query) || food.tags.toLowerCase().includes(query),
    );
    const named = matches.filter((food) => food.name.toLowerCase().includes(query));
    if (named.length > 0 && named.length < matches.length) return query;
  }
  return null;
}

describe("foodToItem", () => {
  const record: FoodRecord = {
    name: "Test food",
    serving: "1 cup",
    kcal: 210,
    p: 8,
    c: 30,
    f: 5,
    tags: "test",
  };

  it("maps the record onto a food item", () => {
    const item = foodToItem(record);
    expect(item.name).toBe("Test food");
    expect(item.quantity).toBe("1 cup");
    expect(item.calories).toBe(210);
    expect(item.protein).toBe(8);
    expect(item.carbs).toBe(30);
    expect(item.fat).toBe(5);
  });

  it("gives each call a distinct id, so repeated logging does not collide", () => {
    expect(foodToItem(record).id).not.toBe(foodToItem(record).id);
  });
});

describe("exercisePresets", () => {
  it("ships twelve presets with a positive burn rate", () => {
    expect(exercisePresets).toHaveLength(12);
    for (const preset of exercisePresets) {
      expect(preset.perMinute).toBeGreaterThan(0);
      expect(preset.name.length).toBeGreaterThan(0);
      expect(preset.icon.length).toBeGreaterThan(0);
    }
  });
});

describe("presetCalories", () => {
  const walking = exercisePresets[0];

  it("is exact at the 80 kg reference weight", () => {
    // perMinute is documented as calories per minute for an 80 kg person.
    expect(presetCalories(walking, 30, 80)).toBe(Math.round(walking.perMinute * 30));
  });

  it("scales down for a lighter person", () => {
    expect(presetCalories(walking, 30, 40)).toBe(Math.round(walking.perMinute * 30 * 0.5));
  });

  it("scales up for a heavier person", () => {
    expect(presetCalories(walking, 30, 120)).toBe(Math.round(walking.perMinute * 30 * 1.5));
  });

  it("returns a whole number", () => {
    const value = presetCalories(walking, 7, 73);
    expect(Number.isInteger(value)).toBe(true);
  });

  it("is zero for zero minutes", () => {
    expect(presetCalories(walking, 0, 80)).toBe(0);
  });
});
