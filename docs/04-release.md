# Release process

## Signing
- Upload keystore: `~/.appreviewreply/upload.jks` (alias `upload`), passwords in `~/.appreviewreply/keystore.properties`. **Not in git. Back both files up** (password manager + encrypted drive). Losing them means losing the ability to update the app unless Play App Signing key reset is granted.
- Gradle reads the properties file automatically (`app/build.gradle.kts` → `signingConfigs.upload`). If the file is missing, release builds are unsigned.
- SHA-1 fingerprints (for the Google OAuth Android clients):
  - Debug key (this Mac): `10:63:E7:8B:52:89:DD:2F:94:69:80:E4:83:6A:44:E1:E1:D8:A2:2E`
  - Upload key: `9D:CD:3E:33:9C:9C:2C:9F:62:91:7A:1D:A9:DF:48:50:C1:E5:86:29`
  - Play App Signing key: copy from Play Console → Setup → App signing after the first upload, and add a third OAuth Android client for it. **Store builds use this one.**

## Build
```sh
cd android
./gradlew :app:testDebugUnitTest          # unit tests
./gradlew :app:assembleDebug              # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:bundleRelease              # app/build/outputs/bundle/release/app-release.aab (upload this)
```
Memory: gradle.properties is tuned for a 16 GB Mac (2 GB heap, no parallel). Close the emulator and Chrome tabs before building.

## Versioning
Bump `versionCode` (+1 every upload) and `versionName` in `app/build.gradle.kts`.

## Play Console first upload (owner)
1. Create app → package `app.appreviewreply`, Tools, free.
2. Enable Play App Signing (default). Upload `app-release.aab` to **Internal testing**; add tester emails.
3. Fill listing from docs/05-play-listing.md; screenshots from assets/screenshots/; privacy URL.
4. Data safety: no data collected/shared; review text processed ephemerally on user action.
5. Copy the App Signing SHA-1 → Google Cloud OAuth Android client (docs/02-google-setup.md C).
6. Subscriptions (after beta): product `pro`, base plans `monthly` $9 and `yearly` $79, 7-day trial offer. Then set `BillingManager.SUBSCRIPTION_REQUIRED = true`, bump version, release.

## Beta config
`android/local.properties` must contain `PROXY_URL` and `APP_SECRET` matching the deployed worker before building the APK/AAB you hand to testers.
