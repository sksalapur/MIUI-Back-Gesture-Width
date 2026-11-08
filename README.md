# MIUI Back Gesture Width & Sensitivity

A lightweight LSPosed / Xposed module for MIUI and HyperOS Launcher (`com.miui.home`) that allows you to fully customize the touch trigger width, inward swipe distance, and flick speed sensitivity of the side back gestures.

Stock MIUI gestures have two significant issues:
1. **Narrow Edge Width:** Stock trigger areas are only 54 px (~20 dp), forcing your finger to reach the extreme bezel edges of the display.
2. **Excessive Distance & Slow Pacing:** Stock MIUI hardcodes an inward swipe distance requirement of **180 px (~65 dp)** (`progress >= 180.0f`), and rejects flicks unless swiped at an extreme speed of > 2.0 px/ms (> 2000–3000 px/s). This forces users to drag their finger excessively far inward or swipe at an unnatural pace to be recognized.

This module resolves both issues with a real-time customization UI inside LSPosed.

---

## Features

- **Gesture Edge Width Slider (50 px – 200 px):**
  - Expands the side touch overlay width so back gestures can be initiated easily without bezel hunting.
  - Quick presets: `Stock (54 px)`, `Default (108 px)`, `Wide (140 px)`, and `Max (200 px)`.
  - Independent left and right edge width controls.

- **Swipe Trigger Distance Slider (30 px – 200 px):**
  - Configures the minimum inward horizontal distance required to trigger the back action.
  - Stock MIUI requires **180 px**. This module sets a comfortable default of **80 px (~29 dp)**, or can be tuned down to **30 px**.
  - Quick presets: `Short (50 px)`, `Default (80 px)`, `Medium (120 px)`, and `Stock (180 px)`.

- **Swipe Speed / Flick Sensitivity Slider (0.2 – 3.0 px/ms):**
  - Configures the velocity threshold for flick recognition.
  - Calculates real-time finger speed in pixels per millisecond (`px/ms`), allowing short and gentle flicks to trigger back instantly without needing full distance travel.
  - Quick presets: `Gentle (0.4 px/ms)`, `Default (0.8 px/ms)`, `Fast (1.5 px/ms)`, and `Stock (2.5 px/ms)`.

- **Instant Real-Time Updates:**
  - Uses an internal `ContentProvider` and `ContentObserver` IPC mechanism. Moving any slider applies changes live to `com.miui.home` **without requiring a launcher restart or reboot**.

- **Clean LSPosed Integration:**
  - Accessible directly via the gear / settings icon in LSPosed Manager.
  - No launcher app icon added to your home screen or app drawer.

- **Lightweight & Single-Purpose:**
  - Strictly scoped to `com.miui.home`. Zero background battery drain or extra processes.

---

## Requirements & Dependencies

This module is an Xposed/LSPosed module and **requires an active LSPosed framework environment** to hook into `com.miui.home`.

> [!IMPORTANT]
> **LSPosed Framework Notice:**
> The legacy [LSPosed repository](https://github.com/LSPosed/LSPosed) has been officially **archived** and is no longer actively maintained. For Android 12, 13, 14, and HyperOS, we strongly recommend using the actively maintained community fork:
> - **[LSPosed Mod by mywalkb](https://github.com/mywalkb/LSPosed_mod)** (Supports Zygisk with Magisk, KernelSU, and APatch)
>
> Download the latest Zygisk release zip from the [LSPosed Mod Releases](https://github.com/mywalkb/LSPosed_mod/releases) page and flash it in your root manager.

- **Device:** Xiaomi / Redmi / POCO running MIUI 13/14 or HyperOS (Android 12/13/14)
- **Root Solution:** [APatch](https://github.com/bmax121/APatch), [KernelSU](https://github.com/tiann/KernelSU), or [Magisk](https://github.com/topjohnwu/Magisk) (v26+) with Zygisk enabled
- **Xposed Framework:** [LSPosed Mod](https://github.com/mywalkb/LSPosed_mod) (v1.9.3+ recommended) or [Legacy LSPosed](https://github.com/LSPosed/LSPosed)
- **Target Application:** System Launcher (`com.miui.home`)

---

## Installation

1. Download the latest [`MiuiGestureWidth.apk`](https://github.com/sksalapur/MIUI-Back-Gesture-Width/releases/latest) from the [Releases](https://github.com/sksalapur/MIUI-Back-Gesture-Width/releases) section (or directly from the repository).
2. Install the APK on your device.
3. Open **LSPosed Manager** and enable the **MIUI Gesture Width** module.
4. Ensure the scope includes **System launcher (`com.miui.home`)**.
5. Restart the launcher once (`killall com.miui.home` or reboot).
6. Tap the module in LSPosed Manager to open settings and adjust edge width, trigger distance, and flick speed live.

---

## Technical Architecture

The module hooks the gesture pipeline in `com.miui.home`:
- **Edge Window Overlays (`GestureStubView`):**
  - Hooks `initScreenSizeAndDensity`, `initGestureEdgeSize`, `setSize`, `getGestureStubWindowParam`, and `onAttachedToWindow` to dynamically resize `GestureStubLeft` and `GestureStubRight` windows.
  - Resizing is dispatched to `mHandler` to ensure thread safety.
- **Gesture Recognition Controller (`GesturesBackController`):**
  - Hooks `setGestureEdgeWidth(int left, int right)` to override edge detection boundaries.
  - Hooks `processPointerEvent(MotionEvent, GestureStubView)` to track exact millisecond duration timestamps.
  - Hooks `isFinished(float offsetX, int speed)` to dynamically evaluate:
    - `offsetX >= targetDistance` (Distance condition)
    - `speedPxPerMs >= targetSpeed && offsetX >= minFlickDistance` (Velocity/Flick condition)
- **IPC Architecture:**
  - `GestureSettingsProvider` exposes configuration via URI `content://com.camellia.miui.gesturewidth.provider/settings`.
  - `GestureWidthSettings` registers a `ContentObserver` in `com.miui.home` to reload values instantly on changes.

---

## Building from Source

Built using standard Android SDK build tools without Gradle dependencies:

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
