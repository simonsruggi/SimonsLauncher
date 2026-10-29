# SimonsLauncher

Android TV launcher for Sony BRAVIA 4K GB (Android 9, API 28, 2GB RAM, 1080p).

## Design

Based on the "SimonsLauncher in Salotto" mockup: near-black background, gold accent, clock + live weather, tab pills, a spotlight with the real titles the apps publish (artwork, description, deep link), row of brand tiles.

## Stack

- Kotlin, Jetpack Compose + TV Compose
- minSdk 28, compileSdk 34
- D-pad navigation, focus states with amber border
- Lightweight: no heavy dependencies

## Target device

- Sony BRAVIA 4K GB
- Android 9 (API 28)
- 1920x1080
- 2GB RAM, 8.2GB storage
- ADB: `adb connect Android.local.:5555`

## Build

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Set as default launcher

```bash
adb shell cmd package set-home-activity com.simonslauncher.app.debug/.MainActivity
```
