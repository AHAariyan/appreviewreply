# Google Cloud + OAuth setup (owner, ~20 min, $0)

No domain needed for the beta. When adding the `androidpublisher` scope in step B, note whether the console labels it sensitive/restricted — that decides whether a domain is needed later for verification.

## A. Cloud project and API
1. https://console.cloud.google.com → New project: **AppReviewReply**.
2. APIs & Services → Library → enable **Google Play Android Developer API**.

## B. OAuth consent screen (Google Auth Platform → Branding / Audience / Data access)
- App name: `AppReviewReply` (no "Google", "Play", "Play Store" in the name).
- User support email: hello@appreviewreply.app (or your Gmail for now).
- App logo: 120×120 PNG of the app icon (assets/icon.png — AI will generate).
- App home page / privacy policy / authorized domain: leave empty for Testing mode. Fill in (site/ pages on appreviewreply.app) only when submitting for verification.
- Audience: **External**, publishing status **Testing**. Add test users: your Gmail + up to 100 beta testers' Gmails.
- Data access / scopes: add `https://www.googleapis.com/auth/androidpublisher`.
- Submit for verification only after 5–10 beta users are happy (verification unlocks >100 users and removes the "unverified app" warning).

## C. OAuth client (Credentials → Create credentials → OAuth client ID)
- Type: **Android**
- Package name: `app.appreviewreply`
- SHA-1 (debug key on this Mac, valid for internal testing builds from here): `10:63:E7:8B:52:89:DD:2F:94:69:80:E4:83:6A:44:E1:E1:D8:A2:2E`
  Later add a second Android client with the Play App Signing SHA-1 (Play Console → Setup → App signing) for store builds.
- No client ID needs to be embedded in the app for the Authorization API; Google matches package + SHA-1.

## D. Play Console
- Your Google account must have **"Reply to reviews"** permission on the apps you add (Users and permissions).
- Create the AppReviewReply app listing (Internal testing track first). Package `app.appreviewreply`.
- Play Billing: create subscription product `pro_monthly` ($9) and `pro_yearly` ($79) with a 7-day free trial — after the free beta.

## E. Cloudflare Worker (proxy/README.md)
- `wrangler secret put ANTHROPIC_API_KEY`, `wrangler secret put APP_SECRET`, deploy.
- Put the worker URL and APP_SECRET into `android/local.properties`: `PROXY_URL=...`, `APP_SECRET=...`.

## F. Website (site/)
- GitHub repo `appreviewreply` → Pages from branch `gh-pages` → custom domain `appreviewreply.app` (add CNAME file = domain; registrar: A records to GitHub Pages IPs or CNAME `www` → `<user>.github.io`). Enforce HTTPS (required for `.app`).
- Email: `hello@appreviewreply.app` — free forwarding via Cloudflare Email Routing to your Gmail.
