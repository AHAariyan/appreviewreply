# Progress log (newest first)

## 2026-09-13
- Name decided: AppReviewReply, domain appreviewreply.app (owner buying), package app.appreviewreply.
- Proxy (Cloudflare Worker) written and type-checked: proxy/.
- Android skeleton written: android/ — Gradle from proven versions (AGP 8.12, Kotlin 2.3.10, Compose BOM 2026.02.01), sign-in via AuthorizationClient, Play API client, JSON store, inbox/detail/issues/settings screens, 12-hourly sync worker. First compile in progress.
- Site: site/index.html + privacy.html + CNAME (for GitHub Pages custom domain).
- Google setup guide: docs/02-google-setup.md.
- Owner to do: buy domain, GitHub repo `appreviewreply`, Cloudflare account, Anthropic key, Google Cloud project + OAuth client (needs debug SHA-1 from `./gradlew signingReport`).
