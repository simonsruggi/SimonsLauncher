# CLAUDE.md - SimonsLauncher

## Project
Android TV launcher (Kotlin + Jetpack Compose). Target: Sony BRAVIA 4K GB, Android 9, 2GB RAM.

## Build
- **On the TV always install the release build**: `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk` (R8, 1.9 MB, signed with the debug key, package `com.simonslauncher.app`). Debug Compose is ~2.5x slower to start (465 vs 180 ms cold start on the emulator).
- `./gradlew assembleDebug` for debug APK (requires Java 26+, Gradle 9.1.0)
- Debug uses `.debug` suffix so it installs alongside any release version
- ADB: `adb connect Android.local.:5555`
- Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- ANDROID_HOME: `/opt/homebrew/share/android-commandlinetools`

## Architecture
- `MainActivity` > `HomeScreen` (Compose)
- `LauncherViewModel` manages app list, spotlight titles, clock, weather
- `AppLoader` queries PackageManager for installed apps; one row with all of them, no tabs/categories (removed 29/10/2026 on request). Settings packages are kept out of the row: settings open from the gear in the top bar (`Settings.ACTION_SETTINGS`)
- `model/Brand.kt`: streaming apps (DAZN, Netflix, Prime Video, SmartTube, YouTube, Disney+, RaiPlay, Mediaset Infinity, NOW; SmartTube = third-party YouTube client, `org.smarttube.stable`) with their TV package names; enum order = first tiles of the row, other apps follow alphabetically
- **Tiles = the apps' official TV banners** (`pm.getApplicationBanner`, 16:9, what Google TV/Projectivy show), never logos redrawn by hand. Apps without a banner: their icon on a dark tile. Same banner as the app badge in the spotlight panel
- **Personalization**: long press OK on a tile (first key repeat, ~0.5 s) → menu Sposta / Nascondi / Annulla. Sposta: ◀ ▶ moves the tile, OK or Back drops it; order saved in prefs `order_v1`; apps installed later: a streaming brand slots in before the first app of a later brand (SmartTube before YouTube), others go last. Nascondi: prefs `hidden_v1`; a "N nascoste" tile at the end of the row opens the list to put them back. Tiles with a long-press action fire OK on key release (to tell it from the hold); everything else fires on press
- **Sources row** (below the apps, the page scrolls down to it): HDMI/AV inputs from `TvInputManager` (`util/TvSources.kt`), HDMI-CEC device name when present, dimmed if disconnected. Opening = `ACTION_VIEW` on `TvContract.buildChannelUriForPassthroughInput(id)` → Sony TV app on that input (verified HDMI 1 = `ExternalTvInputService/HW2`)
- **Safe mode** (`util/CrashGuard.kt`, `ui/screens/SafeHome.kt`): the launcher is the TV's only home, so two crashes within 2 min make the next start a bare app list (no titles, art, network) with "Riprova". Test: `adb shell am crash com.simonslauncher.app` twice. Don't re-enable `com.google.android.tvlauncher` as a fallback: it has system priority and takes the Home key over our default (verified 29/10/2026)
- Overlays (`OverlayPanel`) trap focus (`focusProperties { onExit = cancelFocusChange() }`) and close with Back
- **Home key**: SimonsLauncher is the default home (`cmd package set-home-activity com.simonslauncher.app/.MainActivity`); verified 29/10/2026 that the Home key from Netflix comes back here. Projectivy was uninstalled from the TV on 29/10/2026, so nothing else competes for the Home key. Home pressed while already home (`onNewIntent`, singleTask) closes menus, scrolls back to the first tile and resets the spotlight to the mix
- Sounds: the TV's own `FX_FOCUS_NAVIGATION` on focus and `FX_KEY_CLICK` on OK (`util/UiSound.kt`), played with the volume overload so they work even with system sounds off; silent until the first key press
- **Spotlight = real titles from the apps** (`util/TvContent.kt`): preview programs that Netflix, DAZN, Prime Video publish in TvProvider (title, description, artwork, deep link, live flag). Needs `READ_TV_LISTINGS` (runtime, asked at first launch; on this TV also `adb shell pm grant com.simonslauncher.app android.permission.READ_TV_LISTINGS`) — without it TvProvider returns only our own rows. `READ_EPG_DATA` alone is not enough (verified 29/10/2026: 0 rows). Projectivy uses the same two permissions.
- On the TV (29/10/2026) DAZN publishes ~55 titles, Netflix 16, Prime Video 10, Sony TV 3 channels; YouTube, Disney+, RaiPlay, NOW publish nothing → focusing them keeps the spotlight as is and shows a brand tint backdrop.
- No invented artwork: never put made-up or mockup images in the spotlight. Logo-only art (DAZN 24/7 channels: >80% white pixels, real art <6%) is filtered by `RemoteArt.isPlaceholder`.
- Artwork already carries title and logo (Netflix "N", key art titles): show it whole, never write text on top of it. Details go in the panel beside it.
- Deep links come from other apps' data: `TvContent.intentFor` pins them to the publishing package, drops selectors and URI-grant flags.
- `util/RemoteArt.kt`: download (HTTPS only, http upgraded), disk cache `cacheDir/art` pruned after 3 days, decode at drawn size, 10 MB memory LRU; DAZN and Amazon CDNs are asked for the exact size via URL params. The backdrop is a 96 px copy blurred/darkened in software and stretched.
- Startup: the spotlight shows a mix (first title of each app); focusing an app tile shows that app's titles (the automatic focus at launch doesn't count).
- Weather: Open-Meteo (Napoli), no key, refreshed every 30 min (`util/WeatherClient.kt`)
- Fewer than 4 launchable apps (emulator) → `MockApps` with the full brand set, to check the layout

## Design
- Reference mockup: "Interfaccia TV SimonsLauncher in Salotto" (gold #F5B841 accent on near-black #07070B)
- Top bar: SimonsLauncher (gold, bold+regular), clock, divider, weather icon + temperature + city, settings gear
- Spotlight (326 dp high): big 16:9 artwork + panel (app banner, title, description) + next two titles
- App row: 118x66 dp 16:9 banners, 7 visible + the 8th peeking; name only under the focused tile
- All navigation via D-pad, focus = gold border + gold glow; first app tile focused at start
- Screen is 960x540dp (1920x1080 @ 320dpi); check layout on the `SimonsTV` emulator

## Performance rules (Bravia GPU is weak: every full-screen layer counts)
- Focus animations go through `focusGlow` (`ui/components/TvFocus.kt`): animated values are read only in `graphicsLayer`/draw lambdas, so a frame of animation never recomposes. Label colors use `BasicText(color = ColorProducer)` for the same reason.
- Brand tint backdrops (`res/drawable-nodpi/bg_*`) have the shade baked in; the art backdrop is shaded in software when decoded. Never add full-screen gradient layers in Compose.
- Window background is removed after the first frame (`MainActivity`), the opaque backdrop covers it.
- No Material: text is `ui/theme/Text.kt` (BasicText), the gear is an `ImageVector`. Don't add `material3` back.
- Images go through `RemoteImage` (app artwork) / `AsyncImage` (bundled tints): decoded on IO, never blocking the first frame, wrapped in `key(url)` so a new image restarts load and fade (produceState alone keeps the old bitmap: transparent cards bug, 29/10/2026). Released on `onTrimMemory(BACKGROUND+)`.
- Measured on the Bravia (29/10/2026, after `cmd package compile -m speed-profile`): cold start 730 ms vs Projectivy 1.2-1.7 s; RAM 30 MB vs Projectivy 45 MB. Freshly installed and not yet compiled: 1.8 s.
- The app list is cached in prefs (`apps_v1`): the row is complete on the first frame, the PackageManager scan refines it. Icons of unknown apps are decoded per tile, only when shown.
- Clock and weather are `WhileSubscribed` flows collected with `collectAsStateWithLifecycle`: zero work while another app is on screen.
- Tabs with no apps are hidden (`LauncherViewModel.tabs`).
- Backdrop follows focus with a 140 ms settle delay, so scrolling fast doesn't stack crossfades.
- Idle tiles have no shadow; only the focused element gets the gold glow.
- Benchmarks on the emulator: start it headless (`-no-window -gpu host`), otherwise macOS App Nap throttles it after a few minutes and numbers drift 3-5x; wait for `/proc/loadavg` < 0.3, alternate old/new APKs over several rounds, use the median. Key presses via `adb shell input keyevent` (`emu event send` doesn't reach the app). The emulator can't show gains on image decoding (Mac CPU is too fast): those need the Bravia.
- Check with `debug.hwui.overdraw show` and `dumpsys gfxinfo` after visual changes (28/10/2026: overdraw 1-2x, 1.5% janky frames, 99th pct 23 ms, 48 MB PSS on emulator).

## Key constraints
- Keep RAM usage minimal (2GB device)
- No heavy image loading libraries
- App icons rendered from PackageManager drawables directly
- Compose recomposition kept tight (StateFlow + WhileSubscribed)
