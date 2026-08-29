import {
  MAX_IMAGE_BYTES,
  base64Bytes,
  isValidationFailure,
  parseDataURL,
  statusForUpstream,
  systemPrompt,
  validateRequest,
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
