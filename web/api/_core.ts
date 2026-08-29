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
