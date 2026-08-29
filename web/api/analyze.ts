/**
 * Meal-analysis proxy.
 *
 * Exists so the model API key stays on a server. Before this, every client sent
 * a shared gateway credential as a Bearer token, which meant it shipped inside
 * the web bundle and both app binaries where anyone could lift it.
 *
 * Hosting: Vercel treats `api/` at the project root as functions and this is the
 * signature it expects. Netlify Edge Functions use the same Web-standard
 * `Request`/`Response` shape. Cloudflare Pages needs a thin adapter:
 *
 *     export const onRequestPost = ({ request, env }) => handler(request, env);
 *
 * The only secret read here is GEMINI_API_KEY. It must never be given a `VITE_`
 * or `EXPO_PUBLIC_` prefix -- `vite.config.ts` inlines both into the client
 * bundle, which would recreate the exact bug this file exists to fix.
 */

import {
  MAX_IMAGE_BYTES,
  base64Bytes,
  isValidationFailure,
  parseDataURL,
  statusForUpstream,
  systemPrompt,
  validateRequest,
} from "./_core";

/**
 * Confirm the current model id against Google's documentation before deploying.
 * It is configurable precisely so correcting it is a config change, not a code
 * change and a redeploy of three apps.
 */
const DEFAULT_MODEL = "gemini-2.5-flash";

interface GeminiPart {
  text?: string;
  inline_data?: { mime_type: string; data: string };
}

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

export default async function handler(request: Request, env?: Record<string, string>): Promise<Response> {
  if (request.method !== "POST") {
    return json({ error: "Use POST." }, 405);
  }

  const source = env ?? (globalThis as { process?: { env?: Record<string, string> } }).process?.env ?? {};
  const apiKey = (source.GEMINI_API_KEY ?? "").trim();
  const model = (source.GEMINI_MODEL ?? "").trim() || DEFAULT_MODEL;

  // 503 is what the client maps to "AI scanning isn't available in this build
  // yet". The server is the only party that can honestly know this.
  if (apiKey === "") {
    return json({ error: "notConfigured" }, 503);
  }

  let raw: unknown;
  try {
    raw = await request.json();
  } catch {
    return json({ error: "Body must be valid JSON." }, 400);
  }

  const parsed = validateRequest(raw);
  if (isValidationFailure(parsed)) {
    return json({ error: parsed.message }, parsed.status);
  }

  const parts: GeminiPart[] = [];

  if (parsed.kind === "image") {
    const image = parseDataURL(parsed.content);
    if (image === null) {
      return json({ error: "content must be a base64 data URL." }, 400);
    }
    if (base64Bytes(image.data) > MAX_IMAGE_BYTES) {
      return json({ error: "imageTooLarge" }, 413);
    }
    parts.push({ text: "Analyse this meal photo and return the JSON." });
    parts.push({ inline_data: { mime_type: image.mimeType, data: image.data } });
  } else {
    parts.push({ text: `Meal description: "${parsed.content}". Return the JSON.` });
  }

  const endpoint =
    `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`;

  let upstream: Response;
  try {
    upstream = await fetch(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-goog-api-key": apiKey,
      },
      body: JSON.stringify({
        systemInstruction: { parts: [{ text: systemPrompt(parsed.jesterMode, parsed.language) }] },
        contents: [{ role: "user", parts }],
        generationConfig: {
          temperature: 0.2,
          // Ask for JSON natively rather than prose that needs unfencing. The
          // client still validates, and still tolerates fences, as a fallback.
          responseMimeType: "application/json",
        },
      }),
    });
  } catch {
    return json({ error: "serverError" }, 502);
  }

  if (!upstream.ok) {
    // Never forward the upstream body: it can echo request content and name the
    // provider, neither of which belongs in a client response.
    // 400 and 403 from Gemini almost always mean a bad or unauthorised key, or
    // a model id that does not exist. The user-facing message stays generic on
    // purpose -- neither is something they can act on -- so the actionable
    // detail belongs here, in the operator's log.
    const hint =
      upstream.status === 400 || upstream.status === 403
        ? " - check GEMINI_API_KEY and GEMINI_MODEL"
        : "";
    console.error(`[analyze] gemini responded ${upstream.status}${hint}`);
    return json({ error: "upstream" }, statusForUpstream(upstream.status));
  }

  let text: string | undefined;
  try {
    const payload = (await upstream.json()) as {
      candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }>;
    };
    text = payload.candidates?.[0]?.content?.parts?.[0]?.text ?? undefined;
  } catch {
    return json({ error: "badResponse" }, 502);
  }

  if (text === undefined || text.trim() === "") {
    return json({ error: "badResponse" }, 502);
  }

  // Pass the model's JSON through untouched. The client owns validation, so
  // there is exactly one schema in the system rather than two that can drift.
  return new Response(text, {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
}
