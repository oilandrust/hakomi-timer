# Hakomi Practice Timer

A native Android app for timing Hakomi practice sessions: small groups who take turns as
therapist, client and observer, and need to share a fixed block of time fairly between
rounds, feedback and breaks.

This is a port of the [React web prototype](https://github.com/oilandrust/hakomi-practice-timer)
to Kotlin and Jetpack Compose. The arithmetic is the same; the native shell adds what a browser
could not reliably give: the screen stays on while a round is running, the countdown survives
the app being backgrounded or killed, and a chime sounds even with the phone face down.

The look follows the [Hakomi Institute](https://hakomiinstitute.com/) site: deep forest green,
sage and slate on pale mossy neutrals, a light serif for headings and the clock, and plenty
of room around everything.

## What it does

**Plan** — choose how long the session lasts, either as a duration ("during": 60 / 90 / 120 min
or any custom length) or as an end time ("until": presets snapped to the half hour, or a picked
time). Pick 1–4 rounds, an optional break, and how many minutes to reserve for landing and
wrapping up. The summary shows the minutes each round gets.

**Session** — round and break chips show what is done and what is next. The time for the next
round is recomputed live from the minutes actually remaining, so if the group runs late the
remaining rounds shrink to still finish on time (or, in *Flexible* mode, keep their length and
the projected end time moves instead). A draggable divider splits each round into practice and
feedback.

**Timer** — a large, quiet countdown. Practice runs, a gong sounds, feedback waits for a tap
and runs, a gong sounds, the round is marked complete. Breaks start on their own. Pause, resume
or finish early at any point. The screen stays awake while the timer runs.

## The math

All of it lives in `app/src/main/java/com/hakomi/practicetimer/domain/` and is covered by
JUnit tests in `app/src/test/`.

- **Per-round time** (`SessionPlan`): `floor((total − break − landing) / rounds)`.
- **Landing per round** (`PracticeSession.downtimePerRound`): `ceil(landing / rounds)`, taken
  out of each round rather than timed separately.
- **Finish on time** (`PracticeSession.distribution`): with `remaining = total − elapsed whole
  minutes`, the next round gets `max(1, floor((remaining − pendingBreak) / openRounds) −
  landingPerRound)`.
- **Flexible**: every round gets `max(1, plannedPerRound)`; the remaining session time is
  `openRounds × (plannedPerRound + landingPerRound) + pendingBreak`.
- **Practice / feedback split** (`RoundSplit`): feedback is clamped to `[0, round − 1]` so
  practice always keeps at least one minute and the two always add up to the round.
- **Countdown** (`TimerEngine`): a running phase stores only its absolute deadline, so pausing,
  screen-off, and process death cannot drift it. Remaining seconds round up, so the display
  reads `00:01` until the deadline actually passes.

## Project layout

```
app/src/main/java/com/hakomi/practicetimer/
├── domain/        pure Kotlin: SessionPlan, PracticeSession, TimerEngine, EndTimePresets, TimeFormat
├── data/          SessionRepository — SharedPreferences persistence of session and timer state
├── timer/         TimerController (single source of truth), TimerService (wake lock +
│                  notification), GongPlayer (synthesised chime)
├── ui/            Compose screens: plan/, session/, timer/, shared components/, theme/
├── HakomiApp.kt   application-scoped wiring
└── MainActivity.kt
```

## Building

Requirements: JDK 17+, Android SDK with platform 35 and build-tools 35.0.0 (Android Studio
installs these). The Gradle wrapper fetches everything else.

```bash
# Debug APK
./gradlew :app:assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk

# Unit tests (domain logic)
./gradlew :app:testDebugUnitTest

# Install on a connected device or running emulator
./gradlew :app:installDebug
```

Point Gradle at your SDK with `local.properties` (`sdk.dir=/path/to/Android/Sdk`) or the
`ANDROID_HOME` environment variable.

Minimum Android version: 8.0 (API 26). Target: Android 15 (API 35).

## Running it on your phone

**On the phone itself:** open the download page and tap **Download the APK**.
The page lives in `web/` and can be published to Vercel (use Publish in this chat).
A copy of the same file is `web/hakomi-practice-timer.apk`.

**Option A — sideload the APK.** Every push to `main` also runs the *Android* GitHub Actions
workflow, which uploads `hakomi-practice-timer-debug` as a build artifact.

**Option B — install over USB with adb** (USB debugging enabled in Developer options):

```bash
origin repo clone olivier-rouiller/hakomi-focus   # or git clone the repository URL
cd hakomi-focus
./gradlew :app:installDebug       # builds and installs on the connected phone
```

or, with an APK already in hand:

```bash
adb install -r app-debug.apk
```

**Option C — Android Studio.** Open the project folder, let it sync, plug in the phone and
press Run.

The debug build is signed with the local debug keystore; a build from a different machine
(or from CI) has a different signature, so uninstall the previous copy before installing one
from another source.

## Permissions

- `WAKE_LOCK`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` — keep the countdown
  ticking and the chime audible while a phase is running, via an ongoing notification with
  Pause and Finish actions.
- `POST_NOTIFICATIONS` (Android 13+) — requested the first time a timer is started; if declined,
  the timer still works, only the notification is hidden.
- `VIBRATE` — a short pulse alongside the gong.

## Fonts

Headings and the clock use [Cormorant Garamond](https://fonts.google.com/specimen/Cormorant+Garamond)
(SIL Open Font License, see `licenses/`). Body text uses the system sans-serif.
