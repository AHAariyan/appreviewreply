/**
 * Review draft proxy — Cloudflare Worker.
 *
 * POST /draft
 *   headers: X-App-Secret (shared secret), X-User-Id (opaque, hashed on device),
 *            optional X-Api-Key (bring-your-own key for the configured provider: no cap, user's bill)
 *   body: DraftRequest (see schema below)
 *   -> { reply, category, summary, needs_followup, language, usage }
 *
 * PROVIDER = "openai" (default) | "anthropic". API keys never leave this worker.
 * Per-user monthly counters live in KV.
 */
import OpenAI from "openai";
import Anthropic from "@anthropic-ai/sdk";
import { z } from "zod";
import { zodOutputFormat } from "@anthropic-ai/sdk/helpers/zod";

export interface Env {
  PROVIDER?: string;
  OPENAI_API_KEY?: string;
  ANTHROPIC_API_KEY?: string;
  APP_SECRET: string;
  MODEL: string;
  FALLBACK_MODEL?: string;
  MONTHLY_CAP: string;
  MAX_REPLY_CHARS: string;
  LIMITS: KVNamespace;
}

const DraftRequest = z.object({
  app: z.object({
    name: z.string().min(1).max(120),
    description: z.string().max(1500).default(""),
    tone: z.enum(["friendly", "formal", "concise"]).default("friendly"),
    support_email: z.string().max(200).optional(),
    example_replies: z.array(z.string().max(400)).max(5).default([]),
    developer_name: z.string().max(80).optional(),
  }),
  review: z.object({
    stars: z.number().int().min(1).max(5),
    text: z.string().min(1).max(4000),
    author: z.string().max(120).optional(),
    device: z.string().max(120).optional(),
    android_version: z.string().max(40).optional(),
    app_version: z.string().max(60).optional(),
    language: z.string().max(10).optional(),
  }),
  max_chars: z.number().int().min(80).max(350).optional(),
});
type DraftRequest = z.infer<typeof DraftRequest>;

const CATEGORIES = ["bug", "crash", "feature_request", "praise", "question", "complaint", "spam", "other"] as const;

const DraftOutput = z.object({
  reply: z.string(),
  category: z.enum(CATEGORIES),
  summary: z.string(),
  needs_followup: z.boolean(),
  language: z.string(),
});
type DraftOutput = z.infer<typeof DraftOutput>;

// Plain JSON Schema for OpenAI structured outputs (strict mode needs every key required, no extras).
const OPENAI_SCHEMA = {
  type: "object",
  properties: {
    reply: { type: "string", description: "The public reply to post, within the character budget" },
    category: { type: "string", enum: [...CATEGORIES] },
    summary: { type: "string", description: "One line for the developer's internal issue list" },
    needs_followup: { type: "boolean", description: "True if the developer should follow up (bug, crash, unanswered question)" },
    language: { type: "string", description: "ISO 639-1 code of the review's language" },
  },
  required: ["reply", "category", "summary", "needs_followup", "language"],
  additionalProperties: false,
} as const;

const SYSTEM = `You draft public developer replies to Google Play Store reviews on behalf of an independent app developer.

Rules:
- Write in the same language as the review.
- Reply directly to what the reviewer said; never generic. One idea per sentence.
- Never promise a specific feature or date. You may say the team will look into it.
- For bugs or crashes: thank them, apologise once, ask for the device/steps or invite them to email support if an address is given. Do not ask for personal data beyond an email.
- For praise: thank them warmly in one or two sentences; optionally invite them to share what they'd like next.
- For complaints about price or ads: acknowledge, explain briefly if context allows, no arguing.
- For spam or abuse: reply neutrally in one sentence or leave the reply very short.
- Never mention that the reply was drafted by AI. Never use emojis unless the example replies use them.
- Hard limit: the reply must be at most the character budget given in the request, counted in characters.
- Also classify the review and write a one-line internal summary for the developer's issue list.`;

function json(data: unknown, status = 200, extra: Record<string, string> = {}): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", ...extra },
  });
}

function monthKey(userId: string): string {
  const d = new Date();
  return `u:${userId}:${d.getUTCFullYear()}-${String(d.getUTCMonth() + 1).padStart(2, "0")}`;
}

function buildUserMessage(req: DraftRequest, maxChars: number): string {
  const a = req.app;
  const r = req.review;
  const examples = a.example_replies.length
    ? `Examples of how this developer usually replies (match this voice):\n${a.example_replies.map((e, i) => `${i + 1}. ${e}`).join("\n")}`
    : `No example replies provided. Tone: ${a.tone}.`;
  return [
    `App: ${a.name}`,
    a.description ? `App description: ${a.description}` : "",
    a.developer_name ? `Developer signs as: ${a.developer_name}` : "",
    a.support_email ? `Support email (may be offered for bugs): ${a.support_email}` : "",
    examples,
    "",
    `Review (${r.stars} star${r.stars === 1 ? "" : "s"}${r.app_version ? `, app version ${r.app_version}` : ""}${r.device ? `, ${r.device}` : ""}${r.android_version ? `, Android ${r.android_version}` : ""}):`,
    `"""${r.text}"""`,
    "",
    `Character budget for the reply: ${maxChars}.`,
  ]
    .filter((l) => l !== "")
    .join("\n");
}

class UpstreamError extends Error {
  constructor(public status: number, public code: string, message?: string) {
    super(message ?? code);
  }
}

interface DraftResult {
  out: DraftOutput;
  model: string;
  input_tokens: number;
  output_tokens: number;
  cached_tokens: number;
}

async function draftWithOpenAI(apiKey: string, model: string, user: string): Promise<DraftResult> {
  const client = new OpenAI({ apiKey });
  let completion: OpenAI.Chat.Completions.ChatCompletion;
  try {
    completion = await client.chat.completions.create({
      model,
      max_tokens: 600,
      messages: [
        { role: "system", content: SYSTEM },
        { role: "user", content: user },
      ],
      response_format: { type: "json_schema", json_schema: { name: "review_reply_draft", strict: true, schema: OPENAI_SCHEMA } },
    });
  } catch (e) {
    if (e instanceof OpenAI.RateLimitError) throw new UpstreamError(503, "upstream_rate_limited");
    if (e instanceof OpenAI.AuthenticationError) throw new UpstreamError(401, "invalid_api_key");
    if (e instanceof OpenAI.APIConnectionError) throw new UpstreamError(503, "upstream_unreachable");
    if (e instanceof OpenAI.APIError) throw new UpstreamError(502, "upstream_error", `${e.status} ${e.message}`);
    throw e;
  }
  const choice = completion.choices[0];
  if (choice?.message?.refusal) throw new UpstreamError(422, "refused", choice.message.refusal);
  const text = choice?.message?.content ?? "";
  let out: DraftOutput;
  try {
    out = DraftOutput.parse(JSON.parse(text));
  } catch (e) {
    throw new UpstreamError(502, "bad_model_output", String(e));
  }
  return {
    out,
    model: completion.model,
    input_tokens: completion.usage?.prompt_tokens ?? 0,
    output_tokens: completion.usage?.completion_tokens ?? 0,
    cached_tokens: completion.usage?.prompt_tokens_details?.cached_tokens ?? 0,
  };
}

async function draftWithAnthropic(apiKey: string, model: string, fallbackModel: string | undefined, user: string): Promise<DraftResult> {
  const client = new Anthropic({ apiKey });
  let response: Anthropic.Beta.Messages.BetaMessage;
  try {
    response = await client.beta.messages.create({
      model,
      max_tokens: 1024,
      ...(fallbackModel ? { betas: ["server-side-fallback-2026-06-01"], fallbacks: [{ model: fallbackModel }] } : {}),
      output_config: { effort: "medium", format: zodOutputFormat(DraftOutput) },
      system: [{ type: "text", text: SYSTEM, cache_control: { type: "ephemeral" } }],
      messages: [{ role: "user", content: user }],
    });
  } catch (e) {
    if (e instanceof Anthropic.RateLimitError) throw new UpstreamError(503, "upstream_rate_limited");
    if (e instanceof Anthropic.AuthenticationError) throw new UpstreamError(401, "invalid_api_key");
    if (e instanceof Anthropic.APIConnectionError) throw new UpstreamError(503, "upstream_unreachable");
    if (e instanceof Anthropic.APIError) throw new UpstreamError(502, "upstream_error", `${e.status} ${e.message}`);
    throw e;
  }
  if (response.stop_reason === "refusal") throw new UpstreamError(422, "refused", response.stop_details?.explanation ?? undefined);
  const text = response.content.find((b) => b.type === "text")?.text ?? "";
  let out: DraftOutput;
  try {
    out = DraftOutput.parse(JSON.parse(text));
  } catch (e) {
    throw new UpstreamError(502, "bad_model_output", String(e));
  }
  return {
    out,
    model: response.model,
    input_tokens: response.usage.input_tokens,
    output_tokens: response.usage.output_tokens,
    cached_tokens: response.usage.cache_read_input_tokens ?? 0,
  };
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    const provider = (env.PROVIDER || "openai").toLowerCase();
    if (request.method === "GET" && url.pathname === "/health") return json({ ok: true, provider, model: env.MODEL });
    if (request.method !== "POST" || url.pathname !== "/draft") return json({ error: "not found" }, 404);

    if (request.headers.get("x-app-secret") !== env.APP_SECRET) return json({ error: "unauthorised" }, 401);
    const userId = request.headers.get("x-user-id") || "";
    if (!/^[A-Za-z0-9_-]{8,128}$/.test(userId)) return json({ error: "missing or invalid X-User-Id" }, 400);

    let body: DraftRequest;
    try {
      body = DraftRequest.parse(await request.json());
    } catch (e) {
      return json({ error: "invalid request", detail: String(e) }, 400);
    }

    const byoKey = request.headers.get("x-api-key") || request.headers.get("x-anthropic-key") || "";
    const cap = Number(env.MONTHLY_CAP) || 500;
    const key = monthKey(userId);
    let used = 0;
    if (!byoKey) {
      used = Number((await env.LIMITS.get(key)) || "0");
      if (used >= cap) return json({ error: "monthly_cap_reached", used, cap }, 429);
    }

    const maxChars = Math.min(body.max_chars ?? (Number(env.MAX_REPLY_CHARS) || 350), 350);
    const user = buildUserMessage(body, maxChars);

    let result: DraftResult;
    try {
      if (provider === "anthropic") {
        const apiKey = byoKey || env.ANTHROPIC_API_KEY || "";
        if (!apiKey) return json({ error: "server_key_missing" }, 500);
        result = await draftWithAnthropic(apiKey, env.MODEL, env.FALLBACK_MODEL, user);
      } else {
        const apiKey = byoKey || env.OPENAI_API_KEY || "";
        if (!apiKey) return json({ error: "server_key_missing" }, 500);
        result = await draftWithOpenAI(apiKey, env.MODEL, user);
      }
    } catch (e) {
      if (e instanceof UpstreamError) {
        const status = e.code === "invalid_api_key" ? (byoKey ? 400 : 500) : e.status;
        return json({ error: e.code, detail: e.message !== e.code ? e.message : undefined }, status, e.status === 503 ? { "retry-after": "10" } : {});
      }
      return json({ error: "unexpected", detail: String(e) }, 500);
    }

    const out = result.out;
    if (out.reply.length > maxChars) out.reply = out.reply.slice(0, maxChars - 1).trimEnd() + "…";

    if (!byoKey) await env.LIMITS.put(key, String(used + 1), { expirationTtl: 60 * 60 * 24 * 40 });

    return json({
      ...out,
      usage: {
        used: byoKey ? null : used + 1,
        cap: byoKey ? null : cap,
        provider,
        model: result.model,
        input_tokens: result.input_tokens,
        output_tokens: result.output_tokens,
        cached_tokens: result.cached_tokens,
      },
    });
  },
};
