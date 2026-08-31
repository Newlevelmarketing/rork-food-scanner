import {
  NutritionAIError,
  analyzeImage,
  analyzeText,
  isAIConfigured,
  messageForError,
  parseAnalysis,
  resultToItems,
} from "@/lib/ai";
import type { AnalysisResult } from "@/lib/ai";

/**
 * The client holds no credential any more: it posts to the `/api/analyze` proxy,
 * which keeps the model key server-side. So these tests stub `fetch` and assert
 * the request shape and the status-to-error mapping, rather than reaching a
 * network at all.
 */

const validPayload = JSON.stringify({
  title: "Chicken salad",
  isFood: true,
  healthScore: 8,
  items: [{ name: "Chicken", quantity: "150 g", calories: 250, protein: 30, carbs: 0, fat: 12 }],
});

function stubFetch(status: number, body = ""): ReturnType<typeof vi.fn> {
  const spy = vi.fn(async () => new Response(body, { status }));
  vi.stubGlobal("fetch", spy);
  return spy;
}

function resultFor(overrides: Partial<AnalysisResult> = {}): AnalysisResult {
  return {
    title: "Test meal",
    isFood: true,
    healthScore: 7,
    items: [{ name: "Rice", quantity: "1 bowl", calories: 200, protein: 4, carbs: 44, fat: 0.5 }],
    ...overrides,
  };
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("isAIConfigured", () => {
  it("is true, because the client always has a same-origin endpoint", () => {
    // It no longer claims AI *works* - the client holds no credential, so only
    // the server can know that. An unconfigured server answers 503.
    expect(isAIConfigured).toBe(true);
  });
});

describe("request shape", () => {
  it("posts an image as a data URL with the language and jester flag", async () => {
    const spy = stubFetch(200, validPayload);
    await analyzeImage("data:image/jpeg;base64,AAAA", true, "Spanish");

    expect(spy).toHaveBeenCalledTimes(1);
    const [url, init] = spy.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("/api/analyze");
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body as string)).toEqual({
      kind: "image",
      content: "data:image/jpeg;base64,AAAA",
      jesterMode: true,
      language: "Spanish",
    });
  });

  it("posts a description as text", async () => {
    const spy = stubFetch(200, validPayload);
    await analyzeText("two eggs", false, "French");

    const [, init] = spy.mock.calls[0] as [string, RequestInit];
    expect(JSON.parse(init.body as string)).toEqual({
      kind: "text",
      content: "two eggs",
      jesterMode: false,
      language: "French",
    });
  });

  it("sends no authorization header, because it holds no credential", async () => {
    const spy = stubFetch(200, validPayload);
    await analyzeText("two eggs", false);

    const [, init] = spy.mock.calls[0] as [string, RequestInit];
    const headers = (init.headers ?? {}) as Record<string, string>;
    expect(Object.keys(headers).map((key) => key.toLowerCase())).not.toContain("authorization");
  });

  it("defaults the language to English", async () => {
    const spy = stubFetch(200, validPayload);
    await analyzeText("two eggs", false);

    const [, init] = spy.mock.calls[0] as [string, RequestInit];
    expect(JSON.parse(init.body as string).language).toBe("English");
  });

  it("returns the parsed result on success", async () => {
    stubFetch(200, validPayload);
    const result = await analyzeText("chicken salad", false);
    expect(result.title).toBe("Chicken salad");
    expect(result.items[0].calories).toBe(250);
  });
});

describe("status mapping", () => {
  const kindFor = async (status: number): Promise<string> => {
    stubFetch(status, "{}");
    try {
      await analyzeText("two eggs", false);
      throw new Error("expected a rejection");
    } catch (error) {
      if (error instanceof NutritionAIError) return error.kind;
      throw error;
    }
  };

  it("reports an unconfigured server honestly", async () => {
    // 503 is what api/analyze.ts returns when GEMINI_API_KEY is unset. The user
    // sees "AI scanning isn't available in this build yet" - not a suggestion to
    // reload the page, which cannot supply a server-side key.
    expect(await kindFor(503)).toBe("notConfigured");
    expect(messageForError(new NutritionAIError("notConfigured"))).not.toContain("reload");
  });

  it("maps auth failures", async () => {
    expect(await kindFor(401)).toBe("authError");
    expect(await kindFor(403)).toBe("authError");
  });

  it("maps quota exhaustion", async () => {
    expect(await kindFor(402)).toBe("insufficientBalance");
  });

  it("maps an oversized image", async () => {
    expect(await kindFor(413)).toBe("imageTooLarge");
  });

  it("maps rate limiting", async () => {
    expect(await kindFor(429)).toBe("rateLimited");
  });

  it("falls back to a generic server error", async () => {
    expect(await kindFor(500)).toBe("serverError");
    expect(await kindFor(502)).toBe("serverError");
  });

  it("treats a network failure as a server error", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => {
      throw new TypeError("network down");
    }));
    await expect(analyzeText("two eggs", false)).rejects.toBeInstanceOf(NutritionAIError);
  });

  it("still reports notFood for a well-formed reply containing no food", async () => {
    stubFetch(200, JSON.stringify({ title: "Bicycle", isFood: false, healthScore: 1, items: [] }));
    try {
      await analyzeText("my bike", false);
      throw new Error("expected a rejection");
    } catch (error) {
      expect((error as NutritionAIError).kind).toBe("notFood");
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
