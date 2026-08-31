import { z } from "zod";

import type { FoodItem } from "./types";
import { uid } from "./uid";

/** Structured nutrition estimate returned by the model. */
export interface AnalysisItem {
  name: string;
  quantity: string;
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
}

export interface AnalysisResult {
  title: string;
  isFood: boolean;
  healthScore: number;
  items: AnalysisItem[];
  quip?: string;
}

export type NutritionAIErrorKind =
  | "notConfigured"
  | "imageTooLarge"
  | "authError"
  | "insufficientBalance"
  | "rateLimited"
  | "notFood"
  | "badResponse"
  | "serverError";

const messages: Record<NutritionAIErrorKind, string> = {
  notConfigured: "AI scanning isn't available in this build yet.",
  imageTooLarge: "That photo is too large. Try taking a new one.",
  authError: "AI features are currently unavailable. Please reload the page.",
  insufficientBalance: "AI features are temporarily unavailable. Please try again later.",
  rateLimited: "Too many scans at once. Wait a moment and try again.",
  notFood: "We couldn't find any food in that photo. Try again with better lighting.",
  badResponse: "We couldn't read that result. Please try again.",
  serverError: "Something went wrong. Please try again.",
};

export class NutritionAIError extends Error {
  readonly kind: NutritionAIErrorKind;

  constructor(kind: NutritionAIErrorKind) {
    super(messages[kind]);
    this.name = "NutritionAIError";
    this.kind = kind;
  }
}

/**
 * Runtime shape of the model reply.
 *
 * This is the least trusted input in the app: generated text, from a third-party
 * service, over the network. Numbers arrive as numeric strings often enough that
 * refusing them would fail responses that are semantically fine, so they are
 * coerced — but anything that cannot become a finite number is rejected outright
 * rather than allowed to become NaN in someone's meal history.
 */
const numeric = z.coerce.number().finite();

/**
 * A real boolean, or the strings the model sometimes emits instead.
 *
 * Deliberately not `z.coerce.boolean()`, which maps the string "false" to `true`
 * and would silently log a photo of a bicycle as a meal.
 */
const flexibleBoolean = z.union([
  z.boolean(),
  z.enum(["true", "false"]).transform((value) => value === "true"),
]);

const analysisItemSchema = z.object({
  name: z.string().trim().min(1),
  quantity: z.string().trim().min(1),
  calories: numeric,
  protein: numeric,
  carbs: numeric,
  fat: numeric,
});

const analysisSchema = z.object({
  title: z.string().trim().min(1),
  isFood: flexibleBoolean,
  // The system prompt specifies 1-10 and the UI renders a score out of ten;
  // enforcing that here upholds the documented contract at the boundary.
  healthScore: numeric.transform((value) => Math.min(10, Math.max(1, Math.round(value)))),
  items: z.array(analysisItemSchema),
  quip: z.string().optional(),
});

/**
 * Turns a raw model reply into a validated result, or throws.
 *
 * Exported so the untrusted-input path can be tested without a network call.
 * Shape problems raise `badResponse`; a well-formed reply that simply contains no
 * food raises `notFood`. That distinction already exists in the error taxonomy and
 * is worth preserving — one is our problem, the other is the user's photo.
 */
export function parseAnalysis(text: string): AnalysisResult {
  const json = extractJSON(text);
  if (json === null) throw new NutritionAIError("badResponse");

  let raw: unknown;
  try {
    raw = JSON.parse(json);
  } catch {
    throw new NutritionAIError("badResponse");
  }

  const validated = analysisSchema.safeParse(raw);
  if (!validated.success) throw new NutritionAIError("badResponse");

  const parsed = validated.data;
  if (!parsed.isFood || parsed.items.length === 0) {
    throw new NutritionAIError("notFood");
  }
  return parsed;
}

/** Pulls the first balanced JSON object out of a possibly fenced model reply. */
function extractJSON(text: string): string | null {
  const start = text.indexOf("{");
  if (start === -1) return null;
  let depth = 0;
  for (let index = start; index < text.length; index += 1) {
    const character = text[index];
    if (character === "{") depth += 1;
    if (character === "}") {
      depth -= 1;
      if (depth === 0) return text.slice(start, index + 1);
    }
  }
  return null;
}

/**
 * Where meal analysis is sent.
 *
 * Same-origin by default: the app posts to its own `/api/analyze`, which holds
 * the model credential server-side. Nothing here carries a key, which is the
 * whole point -- the previous design sent a shared gateway secret as a Bearer
 * token from the client, so it shipped inside the bundle for anyone to read.
 *
 * Overridable for a split deployment where the proxy is not served from the
 * same origin as the app.
 */
const analyzeEndpoint = (import.meta.env.VITE_ANALYZE_ENDPOINT ?? "/api/analyze").trim();

/**
 * Whether the client has somewhere to send an analysis request.
 *
 * This can no longer speak to whether AI actually *works*: the client holds no
 * credential, so only the server knows that. An unconfigured deployment answers
 * 503, which maps to `notConfigured` below -- the same user-facing message as
 * before, now decided by the only party able to tell the truth about it.
 */
export const isAIConfigured: boolean = analyzeEndpoint !== "";

/** Maps the proxy's status codes onto the existing error taxonomy. */
function kindForStatus(status: number): NutritionAIErrorKind {
  switch (status) {
    case 401:
    case 403:
      return "authError";
    case 402:
      return "insufficientBalance";
    case 413:
      return "imageTooLarge";
    case 429:
      return "rateLimited";
    case 503:
      return "notConfigured";
    default:
      return "serverError";
  }
}

interface AnalyzeBody {
  kind: "image" | "text";
  content: string;
  jesterMode: boolean;
  language: string;
}

async function send(body: AnalyzeBody): Promise<AnalysisResult> {
  if (!isAIConfigured) throw new NutritionAIError("notConfigured");

  let response: Response;
  try {
    response = await fetch(analyzeEndpoint, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
  } catch {
    throw new NutritionAIError("serverError");
  }

  if (!response.ok) {
    throw new NutritionAIError(kindForStatus(response.status));
  }

  let text: string;
  try {
    text = await response.text();
  } catch {
    throw new NutritionAIError("badResponse");
  }

  return parseAnalysis(text);
}

export async function analyzeImage(
  dataURL: string,
  jesterMode: boolean,
  languageName: string = "English",
): Promise<AnalysisResult> {
  return send({ kind: "image", content: dataURL, jesterMode, language: languageName });
}

export async function analyzeText(
  description: string,
  jesterMode: boolean,
  languageName: string = "English",
): Promise<AnalysisResult> {
  return send({ kind: "text", content: description, jesterMode, language: languageName });
}

export function resultToItems(result: AnalysisResult): FoodItem[] {
  return result.items.map((item) => ({
    id: uid(),
    name: item.name,
    quantity: item.quantity,
    calories: Math.max(0, Math.round(item.calories)),
    protein: Math.max(0, item.protein),
    carbs: Math.max(0, item.carbs),
    fat: Math.max(0, item.fat),
  }));
}

export function messageForError(error: unknown): string {
  if (error instanceof NutritionAIError) return error.message;
  return messages.serverError;
}
