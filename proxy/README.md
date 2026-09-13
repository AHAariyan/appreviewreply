# Review draft proxy (Cloudflare Worker)

Holds the Anthropic API key, drafts a Play Store review reply + classification, and enforces a per-user monthly cap. Free tier is enough for the beta (100k requests/day; KV 100k reads/day).

## One-time setup (owner, ~10 min, $0)
1. Create a Cloudflare account, install: `npm i -g wrangler` and `wrangler login`.
2. `cd proxy && npm install`
3. `wrangler kv namespace create LIMITS` → paste the id into `wrangler.toml`.
4. `wrangler secret put ANTHROPIC_API_KEY` (from console.anthropic.com)
   `wrangler secret put APP_SECRET` (any long random string; the Android app sends it as `X-App-Secret`)
5. `npm run deploy` → URL like `https://review-draft-proxy.<account>.workers.dev`

## Test
```sh
curl -s https://review-draft-proxy.<account>.workers.dev/draft \
  -H 'content-type: application/json' -H 'x-app-secret: ...' -H 'x-user-id: testuser0001' \
  -d '{"app":{"name":"Developer Tools","description":"JWT, JSON, Regex utilities","tone":"friendly","support_email":"support@example.com"},
       "review":{"stars":2,"text":"Crashes every time I open the JSON formatter on my Pixel 8.","app_version":"3.2.1","device":"Pixel 8"}}'
```
Expected: `{ "reply": "...", "category": "crash", "summary": "...", "needs_followup": true, "language": "en", "usage": {...} }`

## Notes
- Model: `claude-opus-5` with server-side refusal fallback to `claude-opus-4-8` (configured in `wrangler.toml`).
- Structured output via `output_config.format` (Zod schema) → the reply is always valid JSON.
- The stable system prompt is cached (`cache_control`), so repeated drafts are cheaper.
- `X-Anthropic-Key` header = bring-your-own key: bypasses the cap, billed to the user.
- Beta-grade auth (shared secret). Before public launch: Play Integrity API token verification.
