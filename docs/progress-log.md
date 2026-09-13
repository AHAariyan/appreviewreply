# Progress log (newest first)

## 2026-09-13
- Name decided: AppReviewReply, domain appreviewreply.app (owner buying), package app.appreviewreply.
- Proxy (Cloudflare Worker) written and type-checked: proxy/.
- Android skeleton written: android/ — Gradle from proven versions (AGP 8.12, Kotlin 2.3.10, Compose BOM 2026.02.01), sign-in via AuthorizationClient, Play API client, JSON store, inbox/detail/issues/settings screens, 12-hourly sync worker. First compile in progress.
- Site: site/index.html + privacy.html + CNAME (for GitHub Pages custom domain).
- Google setup guide: docs/02-google-setup.md.
- Owner to do: buy domain, GitHub repo `appreviewreply`, Cloudflare account, Anthropic key, Google Cloud project + OAuth client (needs debug SHA-1 from `./gradlew signingReport`).
- First debug build succeeded (app-debug.apk, 22.6 MB) after lowering Gradle heap to 2 GB (16 GB Mac ran out of memory with 4 GB heap + parallel).
- Emulator smoke test (Pixel 3a, API 33): installs, launches, sign-in screen renders, sign-in tap calls Google Authorization API without crashing (this image lacks the service; real test needs a device + OAuth client). Screenshots in assets/screenshots/.
- Next: owner buys domain + creates GitHub repo + Cloudflare/Anthropic/Google Cloud setup (docs/02-google-setup.md); then real-device test with owner's own Play apps.
- Added: demo mode (sample reviews, simulated posting), Play Billing scaffold (SUBSCRIPTION_REQUIRED=false during beta), notification permission prompt, unit tests (API parsing, merge), release signing via ~/.appreviewreply upload key, docs 04-release + 05-play-listing, feature graphic.
- GitHub: github.com/AHAariyan/appreviewreply (main + gh-pages site) and github.com/AHAariyan/indiestudio (private) created and pushed.
- Not possible without owner: proxy deploy/live test (no Anthropic key on this Mac), Google Cloud OAuth client, Play Console upload, real-device end-to-end test.
- Screenshots of all screens captured in demo mode (assets/screenshots/); chip-wrapping bug in inbox cards fixed; screenshots added to the website.
- Deliverables ready for owner: signed app-release.aab, debug APK, website live at https://ahaariyan.github.io/appreviewreply/, listing copy, feature graphic.
