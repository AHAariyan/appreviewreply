# L0 desk check — Review-reply + ASO copilot for indie Android devs (2026-09-13)

Working name: **ReviewCopilot** (rename before launch). Scorecard: 41/50 → proceed to L1.

## What Google's API allows (verified from developers.google.com)
- Google Play Developer API `reviews` endpoints: list reviews **with comments only** from the **last 7 days** (older reviews are not retrievable → we must poll daily and keep our own history); reply via POST, **max 350 characters**, plain text.
- Rate limits: 200 GET/hour/app, 2,000 POST/day/app (raisable).
- Auth: **OAuth user consent works** — the developer signs in with their own Google account; what they can do is limited by their Play Console user permissions (needs "Reply to reviews"). Service-account setup (AppFollow style, 10-minute chore) is NOT required. Linking a Cloud project to the Play account is no longer required.
- Google's official Play Console app lets you reply manually; no drafting help.

## OAuth verification (the real friction)
- Every app using Google APIs needs **brand verification**: a homepage on a **domain you own** (verified in Search Console), a privacy policy hosted on that domain, consent-screen branding. `github.io` cannot be an authorized domain → **this idea needs a domain (~$10/yr)**. Owner approval required per strategy §8.
- Until verified (typically days to a few weeks), the OAuth client runs in **Testing mode: up to 100 named test users**, with an "unverified app" warning. That is enough for a closed beta and first payments.
- Whether `androidpublisher` counts as a *sensitive* scope (extra review) could not be confirmed from the scopes page (truncated); assume yes and plan for a short review.

## Competitors and prices (verified)
| Tool | Price | AI reply / auto-reply | Built for |
|---|---|---|---|
| Appbot | $59/mo (5 apps), $119/mo, **$219+/mo** | AI reply + auto-reply **only on $219+ Large plan** | teams |
| AppTweak | $79 / $249 / $499 per month | review analysis; replies via separate "App Reviews Manager" | ASO agencies |
| AppFollow | page not fetchable (known from market: free tier limited, paid ~$139+/mo) | yes on paid | teams |
| Google Play Console app | free | manual replies only | everyone |
| Play Store search "reply to reviews AI" | — | **no app does this for store reviews** (results are chat/WhatsApp auto-repliers) | — |
Gap confirmed: nothing between "free but manual" and "$219/mo with AI".

## Pain evidence
- Direct developer quotes could not be pulled this session (HN Algolia had none for 2025–26; Reddit blocked). The owner *is* the user and confirms the pain; the L1 offer page is the real test.
- Known: Play rewards developer replies (users update ratings; Google states replies improve ratings), and reviews expire from the API after 7 days, so "reply daily" is the natural habit — our product's daily loop.

## Unit economics of the AI drafting
- Per draft ≈ 900 input tokens (review + app context + tone examples) + 120 output tokens.
- `claude-opus-5` ($5 / $25 per M): ≈ $0.0075 per draft → 300 drafts/month ≈ $2.25. `claude-sonnet-5` ($2/$10): ≈ $0.003 → $0.90. `claude-haiku-4-5`: ≈ $0.0015 → $0.45.
- At $9/mo, Opus 5 leaves ~$6 margin at 300 drafts; add a fair-use cap (e.g. 500 drafts/mo) and a BYO-key option for power users. Model choice is the owner's call; default per our API guidance is Opus 5, Sonnet 5 is the cost-safe alternative.
- The API key must not live in the APK → a tiny proxy (Cloudflare Worker, free tier, `*.workers.dev`, no domain needed) holds the key and enforces the per-user cap.

## Risks
1. OAuth verification delay/denial → mitigated by Testing mode (100 users) for beta; start verification on day 1.
2. Google policy on replies: replies must be genuine; we default to **draft → human taps Post**; optional auto-post only for 5★ thank-yous.
3. 7-day API window → daily background fetch (WorkManager) + local history; missed days are lost — tell users clearly.
4. Small audience (indie devs) → fine for $1k MRR (112 subscribers), not a $1M business. Expand to iOS (App Store Connect API also supports review responses) in v2.

## L0 verdict: PASS → L1
