# MIUI Back Gesture Width

A lightweight LSPosed / Xposed module for MIUI and HyperOS Launcher (`com.miui.home`) that allows you to customize the touch trigger width of the left and right side back swipe gestures.

Stock MIUI gesture back areas are relatively narrow (54 px / ~20 dp), often requiring you to reach the very outer bezel of the screen to trigger a back swipe. This module lets you expand the trigger zone from 50 px up to 200 px on the fly.

---

## Features

- **Live Real-Time Slider:** Adjust back gesture width from **50 px to 200 px** (~18 dp to ~73 dp).
- **Instant Updates:** Uses an in-app `ContentProvider` and `ContentObserver` IPC. Moving the slider resizes the gesture overlay windows instantly in real time—**no launcher restart or reboot required**.
- **Quick Presets:** Instant one-tap presets for `Stock (54 px)`, `Default (108 px)`, `Wide (140 px)`, and `Max (200 px)`.
- **Asymmetric Configuration:** Optional toggle to set independent widths for the left and right edges.
- **Native LSPosed Settings:** Accessible directly via the gear / settings icon in LSPosed Manager. Does not add an unnecessary launcher app icon to your home screen or app drawer.
- **Minimal & Efficient:** Strictly scoped to `com.miui.home` with minimal bytecode hooks.

---

## Requirements

- **Device:** Xiaomi / Redmi / POCO running MIUI 13/14 or HyperOS (Android 12/13/14)
- **Root Solution:** APatch, KernelSU, or Magisk
- **Xposed Framework:** Zygisk LSPosed (v1.9.2+)

---

## Installation

1. Download [`MiuiGestureWidth.apk`](MiuiGestureWidth.apk) from the repository.
2. Install the APK on your device.
3. Open **LSPosed Manager** and enable the **MIUI Gesture Width** module.
4. Ensure the scope is set to **System launcher (`com.miui.home`)**.
5. Restart the launcher once (`killall com.miui.home` or reboot).
6. Tap the module in LSPosed Manager to open settings and adjust the slider to your preferred width.

---

## Technical Architecture

The module targets MIUI Home's edge gesture view hierarchy:
- Hooks `com.miui.home.recents.GestureStubView`:
  - `initScreenSizeAndDensity` & `initGestureEdgeSize`
  - `setSize(int size)`
  - `getGestureStubWindowParam()`
  - `onAttachedToWindow` / `onDetachedFromWindow`
- Hooks `com.miui.home.recents.GesturesBackController`:
  - `setGestureEdgeWidth(int left, int right)`
- Dynamic window resizing is safely dispatched onto `GestureStubView`'s private handler thread (`mHandler`) to prevent `CalledFromWrongThreadException`.
- IPC is handled through `GestureSettingsProvider`, providing secure, permissionless inter-process communication between the LSPosed settings UI and `com.miui.home`.

---

## Building from Source

Built using standard Android SDK command-line tools without Gradle dependencies:

```bash
python build.py
```

Required toolchain:
- Java JDK 17 / 21
- Android SDK Platform `android-34`
- Android Build Tools `34.0.0` (`aapt`, `d8`, `zipalign`, `apksigner`)

---

## License

MIT License. Free for personal and community use.
