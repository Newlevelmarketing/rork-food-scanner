import {
  MAX_IMAGE_BYTES,
  MAX_TEXT_LENGTH,
  allowRequest,
  base64Bytes,
  isOriginAllowed,
  isValidationFailure,
  parseAllowList,
  parseDataURL,
  statusForUpstream,
  systemPrompt,
  validateRequest,
  type RateLimitBucket,
} from "../../api/_core";

/**
 * The proxy's pure logic. The network call in `api/analyze.ts` is not exercised
 * here - it needs a real Gemini key and a serverless runtime - but everything
 * that decides *what* gets sent, and how failures are classified, is.
 */

describe("parseDataURL", () => {
  it("splits a JPEG data URL into mime type and payload", () => {
    const parsed = parseDataURL("data:image/jpeg;base64,AAECAwQ=");
    expect(parsed).toEqual({ mimeType: "image/jpeg", data: "AAECAwQ=" });
  });

  it("accepts PNG and WebP", () => {
    expect(parseDataURL("data:image/png;base64,AAAA")?.mimeType).toBe("image/png");
    expect(parseDataURL("data:image/webp;base64,AAAA")?.mimeType).toBe("image/webp");
  });

  it("tolerates surrounding whitespace", () => {
    expect(parseDataURL("  data:image/jpeg;base64,AAAA  ")?.data).toBe("AAAA");
  });

  it("rejects a bare URL", () => {
    expect(parseDataURL("https://example.com/cat.jpg")).toBeNull();
  });

  it("rejects a non-base64 data URL", () => {
    expect(parseDataURL("data:image/jpeg,notbase64")).toBeNull();
  });

  it("rejects an empty payload", () => {
    expect(parseDataURL("data:image/jpeg;base64,")).toBeNull();
  });

  it("rejects an empty string", () => {
    expect(parseDataURL("")).toBeNull();
  });
});

describe("base64Bytes", () => {
  it("computes the decoded size", () => {
    // "AAAA" decodes to 3 bytes.
    expect(base64Bytes("AAAA")).toBe(3);
  });

  it("accounts for single padding", () => {
    expect(base64Bytes("AAAAAA=")).toBe(4);
  });

  it("accounts for double padding", () => {
    expect(base64Bytes("AAAA==")).toBe(2);
  });

  it("agrees with a real encoding round-trip", () => {
    const source = "the quick brown fox jumps over the lazy dog";
    const encoded = btoa(source);
    expect(base64Bytes(encoded)).toBe(source.length);
  });

  it("has a ceiling large enough for the client's own image budget", () => {
    // lib/image.ts caps uploads at 2.6 MB, so the proxy must not reject what
    // the client is willing to send.
    expect(MAX_IMAGE_BYTES).toBeGreaterThan(2_600_000);
  });
});

describe("statusForUpstream", () => {
  it("maps auth failures so the client reports authError", () => {
    expect(statusForUpstream(401)).toBe(401);
    expect(statusForUpstream(403)).toBe(401);
  });

  it("passes rate limiting straight through", () => {
    expect(statusForUpstream(429)).toBe(429);
  });

  it("passes an oversized payload through", () => {
    expect(statusForUpstream(413)).toBe(413);
  });

  it("turns an upstream bad request into a server error, not a client one", () => {
    // A Gemini 400 means we built a bad request. That is our fault, so the
    // client should not be told it did something wrong.
    expect(statusForUpstream(400)).toBe(502);
  });

  it("collapses upstream server errors to 502", () => {
    expect(statusForUpstream(500)).toBe(502);
    expect(statusForUpstream(503)).toBe(502);
  });

  it("never returns 503, which is reserved for a missing key", () => {
    // 503 is how the proxy says "no GEMINI_API_KEY". An upstream failure must
    // not be mistaken for an unconfigured deployment.
    for (const status of [400, 401, 403, 404, 413, 429, 500, 502, 503, 504]) {
      expect(statusForUpstream(status)).not.toBe(503);
    }
  });
});

describe("validateRequest", () => {
  const good = { kind: "image", content: "data:image/jpeg;base64,AAAA", jesterMode: true, language: "English" };

  it("accepts a well-formed body", () => {
    const parsed = validateRequest(good);
    expect(isValidationFailure(parsed)).toBe(false);
    if (isValidationFailure(parsed)) return;
    expect(parsed.kind).toBe("image");
    expect(parsed.jesterMode).toBe(true);
  });

  it("defaults jesterMode to false when absent", () => {
    const parsed = validateRequest({ ...good, jesterMode: undefined });
    if (isValidationFailure(parsed)) throw new Error("expected success");
    expect(parsed.jesterMode).toBe(false);
  });

  it("treats a non-boolean jesterMode as false rather than truthy", () => {
    const parsed = validateRequest({ ...good, jesterMode: "yes" });
    if (isValidationFailure(parsed)) throw new Error("expected success");
    expect(parsed.jesterMode).toBe(false);
  });

  it("rejects a missing or unknown kind", () => {
    expect(isValidationFailure(validateRequest({ ...good, kind: undefined }))).toBe(true);
    expect(isValidationFailure(validateRequest({ ...good, kind: "audio" }))).toBe(true);
  });

  it("rejects empty content", () => {
    expect(isValidationFailure(validateRequest({ ...good, content: "" }))).toBe(true);
    expect(isValidationFailure(validateRequest({ ...good, content: "   " }))).toBe(true);
  });

  it("rejects a missing language", () => {
    expect(isValidationFailure(validateRequest({ ...good, language: "" }))).toBe(true);
  });

  it("rejects non-objects", () => {
    expect(isValidationFailure(validateRequest(null))).toBe(true);
    expect(isValidationFailure(validateRequest("nope"))).toBe(true);
    expect(isValidationFailure(validateRequest(42))).toBe(true);
  });

  it("returns a 400 for every rejection", () => {
    const failure = validateRequest({});
    if (!isValidationFailure(failure)) throw new Error("expected failure");
    expect(failure.status).toBe(400);
  });
});

describe("systemPrompt", () => {
  it("names the requested language", () => {
    expect(systemPrompt(false, "Japanese")).toContain("Japanese");
  });

  it("asks for a warm note by default", () => {
    const prompt = systemPrompt(false, "English");
    expect(prompt).toContain("warm, encouraging");
    expect(prompt).not.toContain("savage");
  });

  it("asks for a roast in jester mode", () => {
    const prompt = systemPrompt(true, "English");
    expect(prompt).toContain("savage");
    expect(prompt).not.toContain("warm, encouraging");
  });

  it("still demands the JSON shape the client validates", () => {
    const prompt = systemPrompt(false, "English");
    expect(prompt).toContain('"isFood"');
    expect(prompt).toContain('"healthScore"');
    expect(prompt).toContain("1-10");
  });
});


describe("allowRequest", () => {
  const LIMIT = 3;
  const WINDOW = 60_000;

  it("allows requests up to the limit", () => {
    const store = new Map<string, RateLimitBucket>();
    for (let i = 0; i < LIMIT; i += 1) {
      expect(allowRequest(store, "a", 1000, LIMIT, WINDOW)).toBe(true);
    }
  });

  it("blocks the request past the limit", () => {
    const store = new Map<string, RateLimitBucket>();
    for (let i = 0; i < LIMIT; i += 1) allowRequest(store, "a", 1000, LIMIT, WINDOW);
    expect(allowRequest(store, "a", 1000, LIMIT, WINDOW)).toBe(false);
  });

  it("keeps callers in separate buckets", () => {
    const store = new Map<string, RateLimitBucket>();
    for (let i = 0; i < LIMIT; i += 1) allowRequest(store, "a", 1000, LIMIT, WINDOW);
    expect(allowRequest(store, "a", 1000, LIMIT, WINDOW)).toBe(false);
    expect(allowRequest(store, "b", 1000, LIMIT, WINDOW)).toBe(true);
  });

  it("opens a fresh window once the old one expires", () => {
    const store = new Map<string, RateLimitBucket>();
    for (let i = 0; i < LIMIT; i += 1) allowRequest(store, "a", 1000, LIMIT, WINDOW);
    expect(allowRequest(store, "a", 1000, LIMIT, WINDOW)).toBe(false);
    expect(allowRequest(store, "a", 1000 + WINDOW, LIMIT, WINDOW)).toBe(true);
  });

  it("does not reset early, one millisecond before the window ends", () => {
    const store = new Map<string, RateLimitBucket>();
    for (let i = 0; i < LIMIT; i += 1) allowRequest(store, "a", 1000, LIMIT, WINDOW);
    expect(allowRequest(store, "a", 1000 + WINDOW - 1, LIMIT, WINDOW)).toBe(false);
  });
});

describe("isOriginAllowed", () => {
  it("allows anything when no allowlist is configured", () => {
    // Failing closed by default would break the first deploy before anyone
    // had a chance to set ALLOWED_ORIGINS; the handler logs a warning instead.
    expect(isOriginAllowed("https://evil.example", [])).toBe(true);
  });

  it("allows a listed origin", () => {
    expect(isOriginAllowed("https://app.example", ["https://app.example"])).toBe(true);
  });

  it("rejects an unlisted origin", () => {
    expect(isOriginAllowed("https://evil.example", ["https://app.example"])).toBe(false);
  });

  it("allows a null origin, because native apps send none", () => {
    expect(isOriginAllowed(null, ["https://app.example"])).toBe(true);
  });

  it("does not match on prefix", () => {
    expect(isOriginAllowed("https://app.example.evil.com", ["https://app.example"])).toBe(false);
  });
});

describe("parseAllowList", () => {
  it("splits, trims and drops blanks", () => {
    expect(parseAllowList(" https://a.example , https://b.example ,, ")).toEqual([
      "https://a.example",
      "https://b.example",
    ]);
  });

  it("treats undefined and empty as no allowlist", () => {
    expect(parseAllowList(undefined)).toEqual([]);
    expect(parseAllowList("   ")).toEqual([]);
  });
});

describe("text length cap", () => {
  const good = { kind: "text", content: "two eggs", jesterMode: false, language: "English" };

  it("accepts a description at the limit", () => {
    const parsed = validateRequest({ ...good, content: "x".repeat(MAX_TEXT_LENGTH) });
    expect(isValidationFailure(parsed)).toBe(false);
  });

  it("rejects one past the limit with 413", () => {
    const failure = validateRequest({ ...good, content: "x".repeat(MAX_TEXT_LENGTH + 1) });
    if (!isValidationFailure(failure)) throw new Error("expected failure");
    expect(failure.status).toBe(413);
  });

  it("does not apply the text cap to image data URLs", () => {
    // Images are capped by MAX_IMAGE_BYTES after decoding instead, which is a
    // much larger budget - a data URL longer than MAX_TEXT_LENGTH is normal.
    const dataUrl = "data:image/jpeg;base64," + "A".repeat(MAX_TEXT_LENGTH * 2);
    const parsed = validateRequest({ ...good, kind: "image", content: dataUrl });
    expect(isValidationFailure(parsed)).toBe(false);
  });
});
