# Voice Screen Locker – Android (Kotlin + Jetpack Compose)

## Open & run
1. Android Studio (Koala or newer) → *Open* this folder → let Gradle sync (it generates the Gradle wrapper).
2. Run on a device (speech recognition, biometrics and overlay permission need a real phone).
3. Optional wallpapers: put `theme1.jpg … theme10.jpg` in `app/src/main/res/drawable/` (gradient placeholders otherwise).
4. Replace the TODOs: support email and privacy URL (Home.kt), Play Billing (Premium.kt), translation backend (Notes.kt → AiService).

## Screen-size support
Phones, small phones and tablets: content is centred with a 560 dp max width, screens scroll, numpad/lock text shrink on
short screens, grids use 2/4 and 3/5 columns by width, pattern pad scales to the available width, edge-to-edge insets.
Light / dark / follow-system theme (Settings → Appearance).

## How the real lock works
`LockService` (foreground service, started when any lock method is configured and again after boot) listens for
SCREEN_ON / USER_PRESENT and opens `LockActivity` (shown over the lock screen, back disabled). Android launches an
activity from the background only if "Display over other apps" is granted – that's why the permission step exists.
Unlock by voice (auto-listens), PIN, pattern, fingerprint, or the security question.

## Honest limits
- Android cannot replace the system keyguard. This is an extra lock layer; Settings → "Disable System Lock" sends the user
  to system security settings so they can remove the stock lock.
- Voice unlock compares the speech-to-text result to the saved phrase (stored hashed). Convenient, not strong security –
  speaker verification needs an ML model/service.
- Some manufacturers (Xiaomi, Oppo, Vivo, Huawei…) kill background services; users may need to allow auto-start / disable battery optimisation.
- Google Play restricts overlay/lock-screen apps: review the policy before publishing.
- Not compiled in this environment: expect to fix small things on first build.
