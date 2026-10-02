# My Notes

A fast, good-looking notes app for Android. Checklists, photo notes, voice notes,
reminders that actually fire, labels, and cloud sync — free forever, no paywalls,
no account wall. Start anonymous, link Google whenever you want a backup.

## Features

- Home grid that matches the mockup: pastel cards (coral, yellow, cream, green, blue),
  staggered layout, filter chips (All / Important / To-do / labels), dotted black background
- Block-based editor: headings, bold/italic/underline/strike, bullets, per-line checklists,
  undo/redo, 800ms autosave (no save button), word count
- Photo notes with on-device OCR (text in pictures becomes searchable)
- Voice notes: background recording, waveform playback, speed control
- Reminders with exact alarms (survive reboot), snooze, repeat daily/weekly
- Pin / archive / 30-day trash with restore, labels with colors, full-text search
- Share notes with viewer/editor roles, per-paragraph conflict pick-one
- Biometric lock per note (or whole app), dark/light/system theme
- Offline-first: Room is the source of truth, Firestore syncs when online
- Export all notes as JSON or a TXT zip

## Build

Prereqs: JDK 17, Android SDK with API 35 + build-tools 35.0.1.

1. `app/google-services.json` is already in the repo (public client config per
   Firebase docs — the API key is locked to the app's package + release cert
   SHA-1 in the Firebase console).
2. Firestore rules are already published live (see `firestore.rules` in the
   parent `mynotes-app/` folder for reference).
3. `./gradlew assembleDebug` for a local debug build.

CI (`.github/workflows/android-build.yml`) builds a **signed release APK** on
every push to `main` and uploads it as an artifact. It needs four repo secrets:
`KEYSTORE_BASE64` (base64 of the release keystore), `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`.

## Tech

Kotlin + Jetpack Compose (Material3) · Room (FTS4 search, sync queue) ·
Firebase Auth (anonymous → Google link via Credential Manager) + Firestore ·
Cloudinary (unsigned uploads for images/voice — Firebase Storage needs Blaze) ·
ML Kit on-device text recognition · Media3 · WorkManager · AlarmManager.

`minSdk 26`, `targetSdk 35`, R8 minify on release, `allowBackup=false`.
