# MIUI Gesture Width & Custom Launcher Enhancer (v2.0)

A lightweight LSPosed / Xposed module for MIUI and HyperOS Launcher (`com.miui.home`) that enables full-screen navigation gestures on 3rd-party launchers, hides the 3-button navigation bar, and unlocks comprehensive, real-time customization of both **Bottom Gestures (Home & Recents)** and **Side Back Gestures**.

Stock MIUI / HyperOS imposes severe limitations on gesture navigation:
1. **Custom Launcher Gesture Block:** When any 3rd-party launcher (such as DAP launcher, Nova, Lawnchair) is set as default, MIUI forcefully disables gesture navigation and permanently displays the legacy 3-button navigation bar.
2. **Narrow Edge Width:** Stock side trigger zones are fixed at 54 px (~20 dp), forcing awkward reach to the extreme bezel edges of the display.
3. **Excessive Distance & Sluggish Pacing:** Both side back gestures and bottom home/recents gestures hardcode large swipe distance requirements and high velocity thresholds, causing missed gestures and unresponsive navigation.

This module resolves all of these issues while maintaining **100% native MIUI gesture rendering and fluid spring animations** via `NavStubView` and `GestureStubView`.

---

## What's New in Version 2.0

- 🚀 **Full-Screen Gestures for Custom Launchers:**
  - Bypasses MIUI's default launcher checks (`Utilities.isUseMiuiHomeAsDefaultHome`, `BaseRecentsImpl`).
  - Keeps full-screen gestures active when custom launchers (like DAP launcher) are default.
  - Automatically hides the 3-button navigation bar (`force_fsg_nav_bar`).
  - Simulates native home button input (`KEYCODE_HOME`) to transition smoothly back to your active custom launcher.

- ⚙️ **4 Dedicated Bottom Gesture Sliders (Home & Recents):**
  - **Bottom Reach Height (30 px – 180 px):** Adjusts how high above the screen bottom the gesture touch zone begins. Presets: `Stock (54 px)`, `Default (65 px)`, `Reach+ (90 px)`, `Tall (130 px)`.
  - **Home Slide Distance (40 px – 250 px):** Configures how far upward you need to swipe to trigger Home. Presets: `Short (60 px)`, `Default (100 px)`, `Medium (150 px)`, `Stock (180 px)`.
  - **Bottom Speed Sensitivity (0.2 – 3.0 px/ms):** Sets the upward velocity required for quick flicks to return home instantly. Presets: `Gentle (0.4 px/ms)`, `Default (0.8 px/ms)`, `Fast (1.5 px/ms)`, `Stock (2.0 px/ms)`.
  - **Recents Hold Duration (100 ms – 600 ms):** Adjusts how long to slide-and-hold upward before the MIUI Recents app switcher triggers. Presets: `Quick (150 ms)`, `Default (250 ms)`, `Relaxed (350 ms)`, `Stock (300 ms)`.

- 🔙 **Side Back Gesture Sliders:**
  - **Side Gesture Width (50 px – 200 px):** Expands the side touch overlay with optional separate Left & Right edge controls. Presets: `Stock (54 px)`, `Default (108 px)`, `Wide (140 px)`, `Max (200 px)`.
  - **Back Swipe Distance (30 px – 200 px):** Inward horizontal distance threshold. Presets: `Short (50 px)`, `Default (80 px)`, `Medium (120 px)`, `Stock (180 px)`.
  - **Back Speed Sensitivity (0.2 – 3.0 px/ms):** Flick velocity threshold for gentle, rapid back navigation. Presets: `Gentle (0.4 px/ms)`, `Default (0.8 px/ms)`, `Fast (1.5 px/ms)`, `Stock (2.5 px/ms)`.

- ⚡ **Instant Real-Time Sync:**
  - Every setting is synchronized live to `com.miui.home` via `ContentProvider` and `ContentObserver` IPC without requiring a launcher restart or reboot.

- 📱 **Clean Integration:**
  - Accessible strictly through the gear/settings icon in LSPosed Manager.
  - Zero standalone launcher icons added to your home screen or app drawer.

---

## Requirements & Dependencies

This module requires an active LSPosed environment to hook `com.miui.home`.

> [!IMPORTANT]
> **LSPosed Framework Notice:**
> The legacy [LSPosed repository](https://github.com/LSPosed/LSPosed) has been officially **archived** and is no longer actively maintained. For Android 12, 13, 14, and HyperOS, we strongly recommend using the actively maintained community fork:
> - **[LSPosed Mod by mywalkb](https://github.com/mywalkb/LSPosed_mod)** (Supports Zygisk with Magisk, KernelSU, and APatch)
>
> Download the latest Zygisk release zip from the [LSPosed Mod Releases](https://github.com/mywalkb/LSPosed_mod/releases) page and flash it in your root manager.

- **Device:** Xiaomi / Redmi / POCO running MIUI 13/14 or HyperOS (Android 12/13/14)
- **Root Solution:** [APatch](https://github.com/bmax121/APatch), [KernelSU](https://github.com/tiann/KernelSU), or [Magisk](https://github.com/topjohnwu/Magisk) (v26+) with Zygisk enabled
- **Xposed Framework:** [LSPosed Mod](https://github.com/mywalkb/LSPosed_mod) (v1.9.3+ recommended) or [Legacy LSPosed](https://github.com/LSPosed/LSPosed)
- **Target Application Scope:** System Launcher (`com.miui.home`)

---

## Installation & Setup

1. Download the latest [`MiuiGestureWidth.apk`](https://github.com/sksalapur/MIUI-Back-Gesture-Width/releases/latest) from the [Releases](https://github.com/sksalapur/MIUI-Back-Gesture-Width/releases) page.
2. Install the APK on your device.
3. Open **LSPosed Manager** and enable the **MIUI Gesture Width** module.
4. Ensure the scope includes **System launcher (`com.miui.home`)**.
5. Restart the launcher once (`killall com.miui.home` or restart device).
6. Tap the module's gear/settings icon in LSPosed Manager to configure your preferences live.

---

## Technical Overview

The module hooks the core gesture components inside `com.miui.home`:

- **Launcher Restriction Bypass (`Utilities` & `BaseRecentsImpl`):**
  - Overrides `Utilities.isUseMiuiHomeAsDefaultHome(Context)` to return `true`.
  - Overrides `BaseRecentsImpl.isUseLauncherRecentsAndFsGesture()` to return `true`.
  - Sets `mIsUseMiuiHomeAsDefaultHome = true` and `mIsFsgNavBar = true` in `updateFsgWindowState`.
  - Enforces `force_fsg_nav_bar = 1` in System Settings so the 3-button navigation bar stays permanently hidden.

- **Bottom Navigation Overlay (`NavStubView`):**
  - Overrides `getHotSpaceHeight()` and window `lp.height` to adjust the bottom touch reach zone live.
  - Intercepts `isFastPullUp()` to evaluate custom upward slide distance and flick velocity without false triggers during stationary holds.
  - Tracks swipe-up-and-hold gestures with native MIUI tactile haptic feedback (`HapticFeedbackCompat.performEnterRecent`) and triggers the Recents overview via `KEYCODE_APP_SWITCH` and `IStatusBarService.toggleRecentApps()`.
  - Overrides `setKeepHidden`, `isRecentsLoaded`, and `isNeedStopBecauseRecentsRemoteAnimStartFailed` so that swipe-to-Recents works consistently from both within apps and third-party launchers.
  - Hooks `finishAppToHome()`: when a 3rd-party launcher is default, dispatches `KEYCODE_HOME` key events to seamlessly route home.

- **Side Back Overlays (`GestureStubView` & `GesturesBackController`):**
  - Dynamically resizes `GestureStubLeft` and `GestureStubRight` window layout params live.
  - Intercepts `GesturesBackController.isFinished()` to check custom distance and speed thresholds.

- **Live Inter-Process Communication:**
  - `GestureSettingsProvider` stores settings in module preferences.
  - `GestureWidthSettings` observes changes in `com.miui.home` via `ContentObserver` and immediately reapplies window parameters to active stubs.

---

## Building from Source

Built using standard Android SDK build tools without Gradle dependencies:

```bash
python build.py
```

Toolchain requirements:
- Java JDK 17 / 21
- Android SDK Platform `android-34`
- Android Build Tools `34.0.0` (`aapt`, `d8`, `zipalign`, `apksigner`)
