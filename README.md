# SimonsLauncher

A fast, lightweight home screen for Android TV, built with Jetpack Compose.
Made for (and tested on) a Sony BRAVIA running Android TV 9 with 2 GB of RAM.

## Features

- **Spotlight with real titles**: shows what Netflix, Prime Video, DAZN and other apps
  publish on their Android TV preview channels (artwork, description, live badge).
  OK opens the title directly in its app.
- **App row with the official TV banners** each app ships for launchers.
  Long press OK on an app to move or hide it.
- **Sources row** (scroll below the apps): HDMI and AV inputs, with the name of the
  HDMI-CEC device when available.
- Clock, current weather (Open-Meteo, no API key), settings shortcut.
- Background that follows the focused title, D-pad focus animations and TV system sounds.
- **Safe mode**: if the launcher crashes twice at start, it opens a bare app list so the
  TV is never left without a home screen.

## Performance

Measured on a Sony BRAVIA (Android 9, 2 GB RAM): ~700 ms cold start, ~30-50 MB of RAM,
1.8 MB APK. No image-loading or UI libraries beyond Compose foundation.

## Build

```bash
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

Requires JDK 17+ and the Android SDK. The release build is signed with your local debug
key; configure your own signing for distribution.

## Setup on the TV

```bash
# Let the launcher read the titles other apps publish (also asked on first launch)
adb shell pm grant com.simonslauncher.app android.permission.READ_TV_LISTINGS

# Make it the default home screen
adb shell cmd package set-home-activity com.simonslauncher.app/.MainActivity
```

The weather location is currently hard-coded (Naples, Italy) in `WeatherClient.kt`.

## Notes

Not affiliated with any of the services it shows. App names, banners and artwork come
from the apps installed on the TV and belong to their respective owners.

## License

GPL-3.0, see [LICENSE](LICENSE). You can use, modify and share it; modified versions you distribute must stay open source under the same license.
