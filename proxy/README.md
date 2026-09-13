# Review draft proxy (Cloudflare Worker)

Holds the Anthropic API key, drafts a Play Store review reply + classification, and enforces a per-user monthly cap. Free tier is enough for the beta (100k requests/day; KV 100k reads/day).

## One-time setup (owner, ~10 min, $0)
1. Create a Cloudflare account, install: `npm i -g wrangler` and `wrangler login`.
2. `cd proxy && npm install`
3. `wrangler kv namespace create LIMITS` → paste the id into `wrangler.toml`.
4. `wrangler secret put OPENAI_API_KEY` (or `ANTHROPIC_API_KEY` with `PROVIDER = "anthropic"` in wrangler.toml)
   `wrangler secret put APP_SECRET` (any long random string; the Android app sends it as `X-App-Secret`)
5. `npm run deploy` → URL like `https://appreviewreply-proxy.<account>.workers.dev`

## Test
```sh
curl -s https://appreviewreply-proxy.<account>.workers.dev/draft \
  -H 'content-type: application/json' -H 'x-app-secret: ...' -H 'x-user-id: testuser0001' \
  -d '{"app":{"name":"Developer Tools","description":"JWT, JSON, Regex utilities","tone":"friendly","support_email":"support@example.com"},
       "review":{"stars":2,"text":"Crashes every time I open the JSON formatter on my Pixel 8.","app_version":"3.2.1","device":"Pixel 8"}}'
```
Expected: `{ "reply": "...", "category": "crash", "summary": "...", "needs_followup": true, "language": "en", "usage": {...} }`

## Notes
- Provider switch: `PROVIDER = "openai"` (default, model `gpt-4o-mini`) or `"anthropic"` (`claude-opus-5` with refusal fallback). Same JSON contract either way.
- Structured output enforced by the provider (OpenAI json_schema strict / Anthropic output_config) → the reply is always valid JSON.
- `X-Api-Key` header = bring-your-own key for the configured provider: bypasses the cap, billed to the user.
- Local test: put `OPENAI_API_KEY=...` and `APP_SECRET=...` in `.dev.vars` (git-ignored), run `npx wrangler dev`, curl `http://localhost:8787/draft`.
- Beta-grade auth (shared secret). Before public launch: Play Integrity API token verification.
