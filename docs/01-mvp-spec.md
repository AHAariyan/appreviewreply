# MVP spec — AppReviewReply (Android) v0.1

## Promise
"Reply to every Play Store review in 2 minutes a day — in your own voice — and never lose a bug report buried in a review."

## Price
$9/month or $79/year (Play Billing subscription), 7-day free trial, fair use 500 AI drafts/month, BYO API key unlocks unlimited.

## Users
Indie Android developers with 1–10 published apps who currently reply to reviews by hand in Play Console or not at all.

## Core loop (daily)
1. Developer signs in with Google (Authorization API, scope androidpublisher) and adds package names by hand — Google's API has no "list my apps" endpoint (AppFollow does the same). The app validates each package by fetching its reviews (last 7 days).
2. Inbox shows unanswered reviews first: stars, text, device, app version, date.
3. Tap a review → AI draft appears (≤350 chars) in the developer's tone; edit if needed → **Post**. Swipe to skip.
4. Reviews mentioning crashes/bugs/feature requests are auto-tagged and collected in a **Issues** tab (bug / feature / praise / question / spam) with counts per app version.
5. Daily notification: "12 new reviews, 3 possible bugs."

## Screens (v0.1)
- Sign in with Google → pick apps → set tone (3 examples of past replies, or choose: friendly / formal / concise) → done.
- Inbox (per app or all), filters: unanswered / 1–2★ / bugs.
- Review detail with draft + edit + Post.
- Issues tab.
- Settings: tone, notification time, subscription, BYO key.

## Out of scope for v0.1
ASO keyword suggestions (v0.2), iOS (v2), auto-reply (v0.2, 5★ only), team seats, web dashboard.

## Architecture
- Android app (Kotlin, Jetpack Compose, single module, no DI), JSON-file store for review history (Room later if needed), WorkManager sync every 12 h + notification.
- Google Play Developer API v3 via REST (OkHttp) with play-services-auth AuthorizationClient (scoped access token, silent refresh) — no service accounts.
- AI drafting through a Cloudflare Worker proxy (TypeScript, Anthropic SDK) that holds the Anthropic key, rate-limits per user (Play purchase token), and returns {reply, tags}. Model: `claude-opus-5` by default, adaptive thinking, structured output for {reply, category}. Prompt: app name/description, developer tone examples, the review, rules (≤350 chars, no promises of features, apologise once, ask for email/support link on bugs).
- Play Billing Library for subscription; trial handled by Play.
- Privacy: reviews are sent to the proxy only when the developer taps "Draft" (or enables auto-draft); nothing stored server-side beyond rate-limit counters.

## Build plan (5 days)
D1 domain + landing + privacy policy + OAuth consent screen (Testing) + Cloud project + enable API.
D2 Android: sign-in, list apps, fetch reviews, Room, inbox UI.
D3 Worker proxy + drafting + tagging; review detail + post reply.
D4 Issues tab, daily sync + notification, Play Billing subscription.
D5 Internal testing track, 10 test users, fix; publish landing with "Join beta / $9/mo".

## Pass criteria (L2)
≥3 paying subscribers (after trial) within 14 days of beta launch.
