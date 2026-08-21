import {
  NutritionAIError,
  isAIConfigured,
  messageForError,
  parseAnalysis,
  resultToItems,
} from "@/lib/ai";
import type { AnalysisResult } from "@/lib/ai";

/**
 * These tests run with no `.env` present, so `EXPO_PUBLIC_RORK_TOOLKIT_SECRET_KEY`
 * is undefined — which is exactly the unconfigured build this unit is about.
 *
 * Network is never reached: the point of the fix is that an unconfigured build
 * fails before `fetch`. A test that had to mock `fetch` would be testing the
 * wrong thing.
 */

function resultFor(overrides: Partial<AnalysisResult> = {}): AnalysisResult {
  return {
    title: "Test meal",
    isFood: true,
    healthScore: 7,
    items: [{ name: "Rice", quantity: "1 bowl", calories: 200, protein: 4, carbs: 44, fat: 0.5 }],
    ...overrides,
  };
}

describe("isAIConfigured", () => {
  it("is false when no gateway key is present", () => {
    // The gateway URL falls back to a public default, so before this fix the
    // flag was unconditionally true and the app claimed AI was available.
    expect(isAIConfigured).toBe(false);
  });
});

describe("unconfigured builds", () => {
  it("reports notConfigured rather than an auth failure", async () => {
    const { analyzeText } = await import("@/lib/ai");
    await expect(analyzeText("two eggs", false)).rejects.toBeInstanceOf(NutritionAIError);
  });

  it("uses the honest message, not 'please reload the page'", async () => {
    const { analyzeText } = await import("@/lib/ai");
    try {
      await analyzeText("two eggs", false);
      throw new Error("expected analyzeText to reject");
    } catch (error) {
      expect(error).toBeInstanceOf(NutritionAIError);
      expect((error as NutritionAIError).kind).toBe("notConfigured");
      expect((error as NutritionAIError).message).toBe(
        "AI scanning isn't available in this build yet.",
      );
      expect((error as NutritionAIError).message).not.toContain("reload");
    }
  });

  it("rejects image analysis the same way", async () => {
    const { analyzeImage } = await import("@/lib/ai");
    try {
      await analyzeImage("data:image/jpeg;base64,AAAA", false);
      throw new Error("expected analyzeImage to reject");
    } catch (error) {
      expect((error as NutritionAIError).kind).toBe("notConfigured");
    }
  });
});

describe("resultToItems", () => {
  it("rounds calories to whole numbers", () => {
    const items = resultToItems(
      resultFor({
        items: [{ name: "Rice", quantity: "1 bowl", calories: 200.6, protein: 4, carbs: 44, fat: 0.5 }],
      }),
    );
    expect(items[0].calories).toBe(201);
  });

  it("clamps negative values the model may return", () => {
    const items = resultToItems(
      resultFor({
        items: [
          { name: "Odd", quantity: "1", calories: -50, protein: -2, carbs: -1, fat: -0.5 },
        ],
      }),
    );
    expect(items[0].calories).toBe(0);
    expect(items[0].protein).toBe(0);
    expect(items[0].carbs).toBe(0);
    expect(items[0].fat).toBe(0);
  });

  it("gives every item a distinct id", () => {
    const items = resultToItems(
      resultFor({
        items: [
          { name: "A", quantity: "1", calories: 10, protein: 1, carbs: 1, fat: 1 },
          { name: "B", quantity: "1", calories: 10, protein: 1, carbs: 1, fat: 1 },
        ],
      }),
    );
    expect(items).toHaveLength(2);
    expect(items[0].id).not.toBe(items[1].id);
  });

  it("carries the name and quantity through unchanged", () => {
    const items = resultToItems(resultFor());
    expect(items[0].name).toBe("Rice");
    expect(items[0].quantity).toBe("1 bowl");
  });
});

describe("parseAnalysis", () => {
  const valid = {
    title: "Chicken salad",
    isFood: true,
    healthScore: 8,
    items: [{ name: "Chicken", quantity: "150 g", calories: 250, protein: 30, carbs: 0, fat: 12 }],
    quip: "Solid choice.",
  };

  const kindOf = (text: string): string => {
    try {
      parseAnalysis(text);
      throw new Error("expected parseAnalysis to throw");
    } catch (error) {
      if (error instanceof NutritionAIError) return error.kind;
      throw error;
    }
  };

  it("accepts a well-formed payload", () => {
    const result = parseAnalysis(JSON.stringify(valid));
    expect(result.title).toBe("Chicken salad");
    expect(result.items[0].calories).toBe(250);
    expect(result.quip).toBe("Solid choice.");
  });

  it("accepts a payload wrapped in markdown fences and prose", () => {
    const fenced = "Sure! Here you go:\n```json\n" + JSON.stringify(valid) + "\n```\nHope that helps.";
    expect(parseAnalysis(fenced).title).toBe("Chicken salad");
  });

  describe("rejects payloads that would corrupt the store", () => {
    it("refuses an item missing a numeric field", () => {
      // Before validation this produced Math.round(undefined) -> NaN, which was
      // persisted to localStorage and turned the day's calorie ring into NaN.
      const missing = {
        ...valid,
        items: [{ name: "Chicken", quantity: "150 g", protein: 30, carbs: 0, fat: 12 }],
      };
      expect(kindOf(JSON.stringify(missing))).toBe("badResponse");
    });

    it("refuses a non-numeric string in a numeric field", () => {
      const bad = {
        ...valid,
        items: [{ ...valid.items[0], calories: "lots" }],
      };
      expect(kindOf(JSON.stringify(bad))).toBe("badResponse");
    });

    it("refuses a null title", () => {
      expect(kindOf(JSON.stringify({ ...valid, title: null }))).toBe("badResponse");
    });

    it("refuses an empty item name", () => {
      const bad = { ...valid, items: [{ ...valid.items[0], name: "   " }] };
      expect(kindOf(JSON.stringify(bad))).toBe("badResponse");
    });

    it("refuses a reply containing no JSON at all", () => {
      expect(kindOf("I'm sorry, I can't help with that.")).toBe("badResponse");
    });

    it("refuses malformed JSON", () => {
      expect(kindOf('{"title": "Broken", ')).toBe("badResponse");
    });
  });

  describe("tolerates what models legitimately do", () => {
    it("coerces numeric strings", () => {
      const stringy = {
        ...valid,
        healthScore: "7",
        items: [
          { name: "Rice", quantity: "1 bowl", calories: "200", protein: "4", carbs: "44", fat: "0.5" },
        ],
      };
      const result = parseAnalysis(JSON.stringify(stringy));
      expect(result.items[0].calories).toBe(200);
      expect(result.items[0].fat).toBe(0.5);
      expect(result.healthScore).toBe(7);
    });

    it("treats the string 'false' as false rather than truthy", () => {
      // z.coerce.boolean() would make this true and log a photo of a bicycle
      // as a meal. The explicit mapping is the whole point.
      expect(kindOf(JSON.stringify({ ...valid, isFood: "false" }))).toBe("notFood");
    });

    it("treats the string 'true' as true", () => {
      expect(parseAnalysis(JSON.stringify({ ...valid, isFood: "true" })).isFood).toBe(true);
    });

    it("omits an absent quip without failing", () => {
      const { quip: _quip, ...withoutQuip } = valid;
      expect(parseAnalysis(JSON.stringify(withoutQuip)).quip).toBeUndefined();
    });
  });

  describe("healthScore is held to the documented 1-10 range", () => {
    it("clamps a score above 10", () => {
      expect(parseAnalysis(JSON.stringify({ ...valid, healthScore: 47 })).healthScore).toBe(10);
    });

    it("clamps a score below 1", () => {
      expect(parseAnalysis(JSON.stringify({ ...valid, healthScore: -3 })).healthScore).toBe(1);
    });

    it("rounds a fractional score", () => {
      expect(parseAnalysis(JSON.stringify({ ...valid, healthScore: 6.4 })).healthScore).toBe(6);
    });
  });

  describe("semantic failures stay distinct from shape failures", () => {
    it("reports notFood when the model says there is no food", () => {
      expect(kindOf(JSON.stringify({ ...valid, isFood: false }))).toBe("notFood");
    });

    it("reports notFood for an empty item list", () => {
      expect(kindOf(JSON.stringify({ ...valid, items: [] }))).toBe("notFood");
    });
  });

  it("never yields a value that resultToItems would turn into NaN", () => {
    const result = parseAnalysis(JSON.stringify(valid));
    for (const item of resultToItems(result)) {
      expect(Number.isNaN(item.calories)).toBe(false);
      expect(Number.isNaN(item.protein)).toBe(false);
      expect(Number.isNaN(item.carbs)).toBe(false);
      expect(Number.isNaN(item.fat)).toBe(false);
    }
  });
});

describe("messageForError", () => {
  it("passes through a known nutrition error's message", () => {
    expect(messageForError(new NutritionAIError("rateLimited"))).toBe(
      "Too many scans at once. Wait a moment and try again.",
    );
  });

  it("falls back to the generic server message for anything else", () => {
    expect(messageForError(new Error("socket hang up"))).toBe(
      "Something went wrong. Please try again.",
    );
    expect(messageForError(undefined)).toBe("Something went wrong. Please try again.");
  });
});
