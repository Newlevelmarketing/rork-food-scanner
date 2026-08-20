import { NutritionAIError, isAIConfigured, messageForError, resultToItems } from "@/lib/ai";
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
