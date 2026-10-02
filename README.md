# My Notes

A fast, good-looking notes app for Android. Pastel cards on a black dotted canvas,
checklists, photo notes with on-device OCR, voice notes, reminders that actually
fire, labels, and cloud sync. Free forever — no paywalls, no account wall. Start
anonymous, link Google whenever you want a backup.

## Download

**[MyNotes-v1.0.apk](https://github.com/kurupdevs/MyNotes/releases/download/v1.0/MyNotes-v1.0.apk)**
· [download page](https://kurupdevs.github.io/mynotes/) · [all releases](https://github.com/kurupdevs/MyNotes/releases)

| Home | Editor | Voice notes | Reminders |
|---|---|---|---|
| ![](screenshots/01-home.webp) | ![](screenshots/02-editor.webp) | ![](screenshots/03-voice.webp) | ![](screenshots/04-reminders.webp) |

## Features

- Staggered home grid: pastel cards (coral, yellow, cream, green, blue, purple),
  filter chips (All / Important / To-do / labels), dotted black background
- Block-based editor: headings, bold/italic/underline/strike, bullets, per-line
  checklists, undo/redo, 800ms autosave, word count
- Photo notes with on-device OCR (text in pictures becomes searchable)
- Voice notes: background recording, waveform playback, speed control
- Reminders with exact alarms (survive reboot), snooze, daily/weekly repeat
- Pin / archive / 30-day trash with restore, colored labels, full-text search
- Share notes with viewer/editor roles, per-paragraph conflict pick-one
- Biometric lock per note (or whole app), dark/light/system theme
- Offline-first: Room is the source of truth, Firestore syncs when online
- Export everything as JSON or a TXT zip

## Build

Prereqs: JDK 17, Android SDK with API 35 + build-tools 35.0.1.

1. `app/google-services.json` is already in the repo (public client config per
   Firebase docs — the API key is locked to the app's package + release cert
   SHA-1 in the Firebase console).
2. Firestore rules are already published live (see `firestore.rules` in the
   parent `mynotes-app/` folder for reference).
3. `./gradlew assembleDebug` for a local debug build.

CI (`.github/workflows/android-build.yml`) builds a **signed release APK** on
every push to `main` and uploads it as an artifact. It needs these repo secrets:
`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (the encrypted keystore
`app/release.keystore.enc` is decrypted in CI with OpenSSL).

## Tech

Kotlin + Jetpack Compose (Material3) · Room (FTS4 search, sync queue) ·
Firebase Auth (anonymous → Google link via Credential Manager) + Firestore ·
Cloudinary (unsigned uploads for images/voice — Firebase Storage needs Blaze) ·
ML Kit on-device text recognition · Media3 · WorkManager · AlarmManager.

`minSdk 26`, `targetSdk 35`, R8 minify on release, `allowBackup=false`.
