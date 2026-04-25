# Hawkeye Android

Android port of the Hawkeye 3D renderer, running the desktop C/Raylib scene natively on Android via NativeActivity and OpenGL ES 3.0.

## Architecture

The app is a `NativeActivity` subclass (`HawkeyeActivity`, ~50 lines of Kotlin) that loads `libhawkeye.so` and runs a standard C `main()` via Raylib's Android platform backend. The Kotlin layer exists only to receive `VIEW`/`SEND` intents for `.ulg` files — all rendering and replay logic is C.

```
android/
├── app/src/main/
│   ├── AndroidManifest.xml       — declares HawkeyeActivity, requires OpenGL ES 3.0
│   ├── assets/                   — all symlinks into the parent Hawkeye project
│   │   ├── fonts  -> ../../../../fonts
│   │   ├── models -> ../../../../models
│   │   ├── shaders -> ../../../../shaders
│   │   └── themes -> ../../../../themes
│   ├── java/com/px4/hawkeye/android/
│   │   └── HawkeyeActivity.kt    — copies inbound .ulg into filesDir/inbox/, writes .ready sentinel
│   └── cpp/
│       ├── CMakeLists.txt        — fetches Raylib 5.5, compiles rendering subset
│       └── android_main.c        — entry point: extracts assets, polls inbox sentinel, render loop
```

The Hawkeye source files compiled in are: `scene.c`, `vehicle.c`, `theme.c`, `ortho_panel.c`, plus the ULog replay subset (`data_source_ulog.c`, `ulog_parser.c`, `ulog_replay.c`). MAVLink, HUD, and the desktop multi-drone replay machinery are excluded.

## Loading flight logs

The app accepts `.ulg` files via Android `VIEW` and `SEND` intents — open a `.ulg` from Drive, Files, or Gmail and pick Hawkeye. There is no in-app file picker yet.

Mechanism: `HawkeyeActivity.handleIntent` copies the file bytes via `ContentResolver` to `filesDir/inbox/current.ulg` and touches `filesDir/inbox/.ready`. The native render loop `stat()`s the sentinel each frame and reloads the replay when its mtime changes, so re-sharing a different `.ulg` into the running app swaps the playback live (the activity uses `launchMode="singleTask"` so the same instance receives subsequent intents via `onNewIntent`).

To test from the command line:

```bash
adb push some-flight.ulg /sdcard/Download/
adb shell am start -a android.intent.action.VIEW \
    -d "file:///sdcard/Download/some-flight.ulg" \
    -t application/octet-stream \
    com.px4.hawkeye.android/.HawkeyeActivity
```

## Shader Compatibility

The original shaders use `#version 330` (desktop OpenGL). On Android they are patched at load time via Raylib's `SetLoadFileTextCallback`:

- `#version 330` → `#version 300 es`
- `precision mediump float;` is injected into fragment shaders

No shader copies are kept in the Android project — the `shaders/` asset dir is a symlink to the originals.

## Environment Setup

If you haven't done Android development before, here's what you need:

### 1. Install Android Studio

Download and install [Android Studio](https://developer.android.com/studio) for your platform (macOS, Linux, or Windows). The installer includes the Android SDK and the SDK Manager.

### 2. Install the NDK and CMake

Open Android Studio, go to **Settings → Languages & Frameworks → Android SDK → SDK Tools**, check:

- **NDK (Side by side)** — install version `30.0.14904198`
- **CMake** — install version `3.22.1`

Click **Apply**.

Alternatively, from the command line (replace `$ANDROID_SDK_ROOT` with your SDK path — typically `~/Library/Android/sdk` on macOS or `~/Android/Sdk` on Linux):

```bash
$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager "ndk;30.0.14904198" "cmake;3.22.1"
```

### 3. Open the project

Open the `android/` directory in Android Studio. Gradle will sync automatically and download any remaining dependencies.

### Note on symlinks

The `assets/` directory uses symlinks into the parent repo (fonts, models, shaders, themes). These work on macOS and Linux out of the box. On Windows, either enable [Developer Mode](https://learn.microsoft.com/en-us/windows/apps/get-started/enable-your-device-for-development) before cloning or use WSL.

## Requirements

- Android SDK (API 29+)
- NDK 30.0.14904198
- CMake 3.22+
- A device or emulator with OpenGL ES 3.0 support

## Building

```bash
./gradlew assembleDebug
```

Raylib 5.5 is fetched automatically by CMake on the first build. Built ABIs: `arm64-v8a`, `x86_64`.

## Deploying

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.px4.hawkeye.android/.HawkeyeActivity
```

