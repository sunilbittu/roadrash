# Road Rash — Android

A pseudo-3D motorcycle racing & combat game for Android, inspired by the classic
**Road Rash**. Race a pack of rival bikers down a long, hilly, curving highway —
and throw punches to knock them off their bikes while dodging civilian traffic.

Built from scratch in **Kotlin** with a custom `SurfaceView` game loop and a
hand-rolled pseudo-3D road renderer (segment projection à la *OutRun* / *Road
Rash*). No game engine, no bitmap assets — all vehicles are drawn procedurally
on the `Canvas`.

## Gameplay

- **Race** a procedurally generated track with curves, rolling hills and S-bends.
- **Fight** five rival racers: ride alongside one and tap **HIT** to punch them.
  Land enough hits and they wipe out. They punch back, so watch your **damage** bar.
- **Dodge** civilian traffic — rear-ending a car at speed crashes you and costs health.
- **Win** by finishing in the top half of the pack. Run your damage bar to zero and
  you're *wasted*.

## Controls (on-screen, multi-touch)

| Button | Action |
| ------ | ------ |
| ◀ / ▶ (bottom-left) | Steer left / right |
| **GAS** (bottom-right) | Accelerate |
| **BRK** | Brake |
| **HIT** | Punch the nearest rival when alongside |

Tap the screen to start a race and to continue from the results screen.

## Project structure

```
app/src/main/java/com/roadrash/game/
├── MainActivity.kt        Full-screen, landscape entry point
├── GameView.kt            SurfaceView host + touch wiring + thread lifecycle
├── GameThread.kt          Fixed-step (60 Hz) update/render loop
├── Game.kt                Game state machine, physics, combat, ranking, rendering
├── engine/
│   ├── MathUtil.kt        Interpolation, easing, helpers
│   ├── Point3D.kt         World → camera → screen projection
│   ├── Segment.kt         One road slice
│   ├── ColorSet.kt        Per-band road/grass/rumble colors
│   ├── Road.kt            Procedural track builder
│   └── Renderer.kt        Canvas trapezoid / band / sky / fog drawing
├── entities/
│   ├── Player.kt          Player bike: steering, speed, health, combat timers
│   ├── Opponent.kt        Rival racer AI + melee combat
│   └── Traffic.kt         Civilian traffic hazards
├── input/
│   ├── InputState.kt      Per-frame control snapshot
│   └── Controls.kt        On-screen multi-touch buttons (interpret + draw)
└── ui/
    ├── Art.kt             Procedural bike / car drawing
    └── Hud.kt             Speed, position, health, progress, messages
```

## Building

Open the project in **Android Studio** (Giraffe or newer) and run, or from the
command line:

```bash
./gradlew assembleDebug      # builds app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug       # build + install on a connected device/emulator
```

- **minSdk 26**, **targetSdk 34**, Kotlin 1.9, AGP 8.5.
- Requires the Android SDK (set `sdk.dir` in `local.properties`, or let Android
  Studio configure it automatically).

## Release APK

The project is configured to produce a **signed, installable release APK**:

```bash
./gradlew assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

Signing uses the committed demo keystore (`app/roadrash-release.jks`, store/key
password `roadrash`, alias `roadrash`) so the release build works out of the box.
For a real / Play Store release, supply your own keystore via gradle properties
or environment variables (any subset overrides the defaults):

```
ROADRASH_STORE_FILE, ROADRASH_STORE_PASSWORD, ROADRASH_KEY_ALIAS, ROADRASH_KEY_PASSWORD
```

### CI build (`.github/workflows/release-apk.yml`)

Because the Android Gradle Plugin and the SDK come from Google's servers, the
APK is built in **GitHub Actions**, which has the SDK pre-installed and open
network access. The workflow runs on pushes to the dev branch (and on manual
dispatch), builds `assembleRelease`, and uploads the signed APK as a build
artifact named **`roadrash-release-apk`** — download it from the workflow run's
*Artifacts* section. Pushing a `v*` tag additionally publishes a GitHub Release
with the APK attached.

> **Note on CI / sandboxed builds:** the Android Gradle Plugin and AndroidX are
> served from Google's Maven repository (`dl.google.com`). If your build
> environment blocks that host the build cannot fetch AGP. The
> framework-independent game logic (everything under `engine/`, `entities/`,
> `input/`) compiles against plain Kotlin/JVM, which is how it was verified in a
> network-restricted sandbox.
