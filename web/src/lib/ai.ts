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

const MODEL = "google/gemini-3-flash";
const FALLBACK_MODELS = ["anthropic/claude-haiku-4.5", "openai/gpt-5-mini"];

const toolkitURL = (import.meta.env.EXPO_PUBLIC_TOOLKIT_URL ?? "https://toolkit.rork.com").trim();
const toolkitKey = (import.meta.env.EXPO_PUBLIC_RORK_TOOLKIT_SECRET_KEY ?? "").trim();

function endpoint(): string | null {
  if (!toolkitURL) return null;
  const normalized = toolkitURL.endsWith("/") ? toolkitURL.slice(0, -1) : toolkitURL;
  return `${normalized}/v2/vercel/v1/chat/completions`;
}

/**
 * Analysis needs a gateway URL *and* a key.
 *
 * `toolkitURL` falls back to the public gateway, so a build with no key would
 * otherwise report itself configured and then fail every request with a 401 —
 * surfacing "AI features are currently unavailable. Please reload the page." for
 * something no reload can fix. Requiring the key here keeps the honest
 * `notConfigured` message ("AI scanning isn't available in this build yet") for
 * the case it was written to describe.
 */
export const isAIConfigured: boolean = endpoint() !== null && toolkitKey !== "";

function systemPrompt(jester: boolean, languageName: string): string {
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

  // `tsconfig.app.json` sets `strict: false`, and zod's type inference silently
  // degrades to all-properties-optional without `strictNullChecks`. The compiler
  // therefore cannot see that `safeParse` has already guaranteed this shape.
  //
  // This assertion is not the one this unit removed. That one claimed a shape
  // nothing had checked; by this line the shape has been verified at runtime,
  // field by field. See the note in `context/decision-log.md`.
  const parsed = validated.data as AnalysisResult;
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

type UserContent = Array<
  { type: "text"; text: string } | { type: "image_url"; image_url: { url: string } }
>;

async function send(
  userContent: UserContent,
  jesterMode: boolean,
  languageName: string,
): Promise<AnalysisResult> {
  const url = endpoint();
  // Bail before the network call rather than spending a request to learn the
  // build has no credentials.
  if (!url || !isAIConfigured) throw new NutritionAIError("notConfigured");

  let response: Response;
  try {
    response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${toolkitKey}`,
      },
      body: JSON.stringify({
        model: MODEL,
        temperature: 0.2,
        messages: [
          { role: "system", content: systemPrompt(jesterMode, languageName) },
          { role: "user", content: userContent },
        ],
        providerOptions: { gateway: { models: FALLBACK_MODELS } },
      }),
    });
  } catch {
    throw new NutritionAIError("serverError");
  }

  if (!response.ok) {
    switch (response.status) {
      case 401:
      case 403:
        throw new NutritionAIError("authError");
      case 402:
        throw new NutritionAIError("insufficientBalance");
      case 413:
        throw new NutritionAIError("imageTooLarge");
      case 429:
        throw new NutritionAIError("rateLimited");
      default:
        console.error("[NutritionAI] request failed with status", response.status);
        throw new NutritionAIError("serverError");
    }
  }

  let text: string | undefined;
  try {
    const payload = (await response.json()) as {
      choices?: Array<{ message?: { content?: string } }>;
    };
    text = payload.choices?.[0]?.message?.content ?? undefined;
  } catch {
    throw new NutritionAIError("badResponse");
  }

  if (!text) throw new NutritionAIError("badResponse");
  return parseAnalysis(text);
}

export async function analyzeImage(
  dataURL: string,
  jesterMode: boolean,
  languageName: string = "English",
): Promise<AnalysisResult> {
  return send(
    [
      { type: "text", text: "Analyse this meal photo and return the JSON." },
      { type: "image_url", image_url: { url: dataURL } },
    ],
    jesterMode,
    languageName,
  );
}

export async function analyzeText(
  description: string,
  jesterMode: boolean,
  languageName: string = "English",
): Promise<AnalysisResult> {
  return send(
    [{ type: "text", text: `Meal description: "${description}". Return the JSON.` }],
    jesterMode,
    languageName,
  );
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
