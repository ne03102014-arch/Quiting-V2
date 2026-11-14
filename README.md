# Quiting

Android VPN configurator built with Kotlin and Jetpack Compose.

## Features

- Import profiles from share links: `vless://`, `vmess://`, `trojan://`, `ss://`
- Import a whole subscription from a URL (plain or base64 list)
- Favorites, selection and delete for configurations
- Four interface styles: Material You, Liquid Glass, Minimalism, Neon Holo
- Light, dark and system theme, switched in Settings
- Animated backgrounds and a rotating holographic connect orb

## The tunnel

- `vpn/TunnelService.kt` is a real `VpnService` with a foreground notification and a TUN interface.
- `data/ConfigBuilder.kt` turns each profile into a sing-box configuration (VLESS, VMess, Trojan, Shadowsocks, TLS, Reality, WS, gRPC).
- `vpn/LibboxCore.kt` starts the sing-box core. **The core is not included.** Add `libbox.aar` to `app/libs/`
  and add `implementation(files("libs/libbox.aar"))` to `app/build.gradle.kts`. Then check the class and method
  names in `LibboxCore.kt` against your libbox version. This code has not been run against a real build.
  Without the AAR, Connect shows the error "Tunnel core is missing".

## Build the APK

### Option 1: GitHub Actions (no local setup)

1. Create a GitHub repository and push this folder to it.
2. Open the **Actions** tab. The **Build APK** workflow runs on every push.
3. Open the finished run and download the `Quiting-debug-apk` artifact. It contains `app-debug.apk`.

You can also start the workflow manually from the Actions tab with **Run workflow**.

### Option 2: Android Studio

1. Open the project folder in Android Studio (Ladybug or newer).
2. Let Gradle sync, then choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
3. The APK appears in `app/build/outputs/apk/debug/`.

Requirements: JDK 17, Android SDK 35, minimum Android 8.0 (API 26).

## Install

Copy `app-debug.apk` to the phone and open it. Allow installation from unknown sources when asked.

## Project layout

```
app/src/main/java/com/quiting/app/
  MainActivity.kt          entry point
  data/Models.kt           profile model, link parser, subscription fetch, storage
  viewmodel/AppViewModel.kt  state and actions
  ui/Theme.kt              four styles, light/dark schemes, animated backgrounds, glass cards
  ui/Screens.kt            Home, Configs, Settings and dialogs
.github/workflows/android.yml  CI build of the debug APK
```
