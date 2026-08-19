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

export const isAIConfigured: boolean = endpoint() !== null;

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
  if (!url) throw new NutritionAIError("notConfigured");

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
  const json = extractJSON(text);
  if (!json) throw new NutritionAIError("badResponse");

  let parsed: AnalysisResult;
  try {
    parsed = JSON.parse(json) as AnalysisResult;
  } catch {
    throw new NutritionAIError("badResponse");
  }

  if (!parsed.isFood || !Array.isArray(parsed.items) || parsed.items.length === 0) {
    throw new NutritionAIError("notFood");
  }
  return parsed;
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
