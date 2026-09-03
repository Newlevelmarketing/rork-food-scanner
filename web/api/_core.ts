/**
 * Shared logic for the meal-analysis proxy.
 *
 * Kept in a `_`-prefixed file because Vercel treats every other file under
 * `api/` as a route. Everything here is pure and unit-tested; the network call
 * lives in `analyze.ts`.
 */

export type AnalyzeKind = "image" | "text";

export interface AnalyzeRequest {
  kind: AnalyzeKind;
  content: string;
  jesterMode: boolean;
  language: string;
}

/**
 * The system prompt.
 *
 * It lives here rather than in the clients on purpose. Previously it existed in
 * three places -- web, iOS and Android -- and had to be edited three times and
 * kept in sync by hand, and it shipped to every user. One copy, changeable
 * without releasing an app.
 */
export function systemPrompt(jester: boolean, languageName: string): string {
  const base = [
    "You are ModernBody, a precise nutrition estimator. Given a meal photo or description,",
    "identify each distinct food component and estimate its nutrition for the portion shown.",
    "",
    "Rules:",
    "- Estimate realistic portion sizes from visual cues (plate size, utensils, hands).",
    "- Break composite dishes into their main components when clearly separable, otherwise return the dish as one item.",
    '- Quantities must be human readable, e.g. "1 medium bowl", "150 g", "2 slices".',
    "- healthScore is 1-10 where 10 is an exceptionally nutritious, whole-food meal.",
    "- title is a short, appetising name for the whole meal (max 4 words).",
    "- If the input clearly contains no edible food, set isFood to false and return an empty items array.",
    "",
    "Respond with ONLY raw JSON matching exactly this shape, no markdown fences:",
    '{"title":string,"isFood":boolean,"healthScore":number,"items":[{"name":string,"quantity":string,"calories":number,"protein":number,"carbs":number,"fat":number}],"quip":string}',
    "",
    `Write title, every item name, quantity and quip in ${languageName}. Keep the JSON keys in English.`,
  ].join("\n");

  return jester
    ? `${base}\n\nSet quip to one savage, funny one-line roast of this meal (max 14 words).`
    : `${base}\n\nSet quip to one short, warm, encouraging note about this meal (max 12 words).`;
}

export interface InlineImage {
  mimeType: string;
  data: string;
}

/**
 * Splits a `data:` URL into the parts Gemini wants.
 *
 * Gemini takes `inline_data` with a bare base64 payload, not a data URL, so the
 * prefix has to come off. Returns null for anything that is not a base64 data
 * URL rather than passing malformed input to the model.
 */
export function parseDataURL(value: string): InlineImage | null {
  const match = /^data:([a-z]+\/[a-z0-9.+-]+);base64,(.+)$/i.exec(value.trim());
  if (match === null) return null;
  const [, mimeType, data] = match;
  if (data.length === 0) return null;
  return { mimeType, data };
}

/** Approximate decoded size of a base64 payload, without decoding it. */
export function base64Bytes(data: string): number {
  const padding = data.endsWith("==") ? 2 : data.endsWith("=") ? 1 : 0;
  return Math.floor((data.length * 3) / 4) - padding;
}

/**
 * Maps a Gemini HTTP status onto the status this proxy returns.
 *
 * The client's error taxonomy is unchanged from the Rork gateway era, so these
 * are the codes `NutritionAIError` already knows how to interpret. Keeping the
 * mapping here means the client needed no new error handling.
 */
export function statusForUpstream(upstream: number): number {
  switch (upstream) {
    case 400:
      return 502;
    case 401:
    case 403:
      return 401;
    case 429:
      return 429;
    case 413:
      return 413;
    default:
      return upstream >= 500 ? 502 : 502;
  }
}

export interface ValidationFailure {
  status: number;
  message: string;
}

/** Rejects a malformed body before any upstream call is spent on it. */
export function validateRequest(raw: unknown): AnalyzeRequest | ValidationFailure {
  if (typeof raw !== "object" || raw === null) {
    return { status: 400, message: "Body must be a JSON object." };
  }
  const body = raw as Record<string, unknown>;

  if (body.kind !== "image" && body.kind !== "text") {
    return { status: 400, message: 'kind must be "image" or "text".' };
  }
  if (typeof body.content !== "string" || body.content.trim() === "") {
    return { status: 400, message: "content is required." };
  }
  // The image path is capped by MAX_IMAGE_BYTES after decoding the data URL, but
  // a text description had no upper bound at all - an unbounded prompt is both a
  // cost and a latency problem.
  if (body.kind === "text" && body.content.length > MAX_TEXT_LENGTH) {
    return { status: 413, message: "Description is too long." };
  }
  if (typeof body.language !== "string" || body.language.trim() === "") {
    return { status: 400, message: "language is required." };
  }

  return {
    kind: body.kind,
    content: body.content,
    jesterMode: body.jesterMode === true,
    language: body.language,
  };
}

export function isValidationFailure(
  value: AnalyzeRequest | ValidationFailure,
): value is ValidationFailure {
  return "status" in value;
}

/** Largest inline image Gemini will accept comfortably, and our own ceiling. */
export const MAX_IMAGE_BYTES = 4_000_000;

/** Longest meal description accepted. A sentence or two is all the prompt asks for. */
export const MAX_TEXT_LENGTH = 2_000;

export interface RateLimitBucket {
  count: number;
  resetAt: number;
}

/**
 * Fixed-window rate limit.
 *
 * Pure so it can be tested: the caller owns the store and supplies the clock.
 *
 * IMPORTANT, and stated here so nobody mistakes this for real protection: the
 * store is per-instance memory. Serverless hosts run many instances and recycle
 * them, so a determined caller spreading requests across instances gets a fresh
 * bucket each time. This stops a naive loop from one client - the common case -
 * and nothing more. Durable protection needs a shared store (Vercel KV, Upstash)
 * or a WAF rule.
 */
export function allowRequest(
  store: Map<string, RateLimitBucket>,
  key: string,
  now: number,
  limit: number,
  windowMs: number,
): boolean {
  const bucket = store.get(key);
  if (bucket === undefined || now >= bucket.resetAt) {
    store.set(key, { count: 1, resetAt: now + windowMs });
    return true;
  }
  if (bucket.count >= limit) return false;
  bucket.count += 1;
  return true;
}

/**
 * Whether a request's Origin is acceptable.
 *
 * `null` origin is allowed on purpose: native apps send no Origin header, and
 * they are intended callers of this endpoint. When no allowlist is configured
 * this returns true and the caller logs a warning - failing closed by default
 * would break the first deployment before anyone had a chance to configure it.
 */
export function isOriginAllowed(origin: string | null, allowList: string[]): boolean {
  if (allowList.length === 0) return true;
  if (origin === null) return true;
  return allowList.includes(origin);
}

export function parseAllowList(raw: string | undefined): string[] {
  return (raw ?? "")
    .split(",")
    .map((entry) => entry.trim())
    .filter((entry) => entry.length > 0);
}
