package com.camellia.miui.gesturewidth;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PointF;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import java.lang.reflect.Method;

public class NavStubHook {
    public static final String TAG = GestureWidthSettings.TAG;

    private static String sCurrentDefaultHomePkg = null;
    private static long sLastHomeCheckTime = 0;

    // Gesture tracking state
    private static float sTouchStartX = 0f;
    private static float sTouchStartY = 0f;
    private static long sTouchStartTime = 0;
    private static long sHoldStartTime = 0;
    private static boolean sHoldTriggered = false;
    private static boolean sIsHoldingRecents = false;
    private static long sJustTriggeredRecentsTime = 0;

    /**
     * Checks if a third-party launcher (like DAP launcher) is active as default.
     */
    public static boolean isCustomLauncherActive(Context context) {
        if (context == null) return false;
        long now = System.currentTimeMillis();
        if (sCurrentDefaultHomePkg != null && (now - sLastHomeCheckTime < 2000)) {
            return !"com.miui.home".equals(sCurrentDefaultHomePkg);
        }
        try {
            PackageManager pm = context.getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                sCurrentDefaultHomePkg = resolveInfo.activityInfo.packageName;
                sLastHomeCheckTime = now;
                return !"com.miui.home".equals(sCurrentDefaultHomePkg);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error checking default home: ", t);
        }
        return false;
    }

    /**
     * Triggers the Recents overview action via system key injection and status bar IPC.
     */
    public static void triggerRecents(Object navStubObj, Context context) {
        long now = SystemClock.uptimeMillis();
        if (now - sJustTriggeredRecentsTime < 500) {
            Log.d(TAG, "triggerRecents debounced (already triggered " + (now - sJustTriggeredRecentsTime) + "ms ago)");
            return;
        }
        sJustTriggeredRecentsTime = now;
        Log.i(TAG, ">>> Triggering Recents gesture action! <<<");

        boolean injected = false;
        if (navStubObj != null) {
            try {
                XposedHelpers.callMethod(navStubObj, "sendEvent", 0, 0, KeyEvent.KEYCODE_APP_SWITCH);
                XposedHelpers.callMethod(navStubObj, "sendEvent", 1, 0, KeyEvent.KEYCODE_APP_SWITCH);
                injected = true;
                Log.i(TAG, "Recents injected via NavStubView.sendEvent(KEYCODE_APP_SWITCH)");
            } catch (Throwable t) {
                Log.w(TAG, "NavStubView.sendEvent failed: " + t.getMessage());
            }
        }

        // Backup method: call IStatusBarService.toggleRecentApps
        if (!injected && context != null) {
            try {
                Class<?> smClass = Class.forName("android.os.ServiceManager");
                Method getService = smClass.getMethod("getService", String.class);
                IBinder binder = (IBinder) getService.invoke(null, "statusbar");
                if (binder != null) {
                    Class<?> stubClass = Class.forName("com.android.internal.statusbar.IStatusBarService$Stub");
                    Method asInterface = stubClass.getMethod("asInterface", IBinder.class);
                    Object statusBarService = asInterface.invoke(null, binder);
                    if (statusBarService != null) {
                        XposedHelpers.callMethod(statusBarService, "toggleRecentApps");
                        Log.i(TAG, "Recents toggled via IStatusBarService.toggleRecentApps");
                        injected = true;
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error calling IStatusBarService.toggleRecentApps: ", t);
            }
        }

        // Reset NavStubView state so it's fresh for next touch
        if (navStubObj != null) {
            try {
                XposedHelpers.callMethod(navStubObj, "finalization", "toRecents");
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Performs native MIUI gesture haptic feedback when hold state is engaged.
     */
    public static void performHoldHaptic(Object navStubObj) {
        try {
            if (navStubObj instanceof View) {
                Context ctx = ((View) navStubObj).getContext();
                ClassLoader cl = ctx.getClassLoader();
                Class<?> hapticCompatClass = XposedHelpers.findClass("com.miui.home.launcher.common.HapticFeedbackCompat", cl);
                Object haptic = XposedHelpers.callStaticMethod(hapticCompatClass, "getInstance");
                XposedHelpers.callMethod(haptic, "performEnterRecent", (View) navStubObj);
                Log.i(TAG, "Played MIUI native performEnterRecent haptic vibration");
                return;
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not call HapticFeedbackCompat: " + t.getMessage());
        }

        // Fallback to standard view haptic feedback
        try {
            if (navStubObj instanceof View) {
                ((View) navStubObj).performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }
        } catch (Throwable ignored) {}
    }

    public static class SetKeepHiddenHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (GestureWidthSettings.isCustomLauncherGesturesEnabled()) {
                    param.args[0] = false;
                }
            } catch (Throwable t) {
                // ignore
            }
        }
    }

    // --- 1. Utilities Hooks (Bypass MIUI default launcher restriction) ---
    public static class IsUseMiuiHomeHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (GestureWidthSettings.isCustomLauncherGesturesEnabled()) {
                    param.setResult(true);
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in IsUseMiuiHomeHook: ", t);
            }
        }
    }

    // --- 2. BaseRecentsImpl Hooks ---
    public static class IsUseLauncherRecentsHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (GestureWidthSettings.isCustomLauncherGesturesEnabled()) {
                    param.setResult(true);
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in IsUseLauncherRecentsHook: ", t);
            }
        }
    }

    public static class SetIsUseMiuiHomeHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (GestureWidthSettings.isCustomLauncherGesturesEnabled()) {
                    param.args[0] = true;
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in SetIsUseMiuiHomeHook: ", t);
            }
        }
    }

    public static class UpdateFsgWindowStateHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (GestureWidthSettings.isCustomLauncherGesturesEnabled()) {
                    Context ctx = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    if (ctx != null) {
                        GestureWidthSettings.ensureObserver(ctx);
                        GestureWidthSettings.ensureForceFsgNavBar(ctx);
                    }
                    XposedHelpers.setBooleanField(param.thisObject, "mIsUseMiuiHomeAsDefaultHome", true);
                    XposedHelpers.setBooleanField(param.thisObject, "mIsFsgNavBar", true);
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in UpdateFsgWindowStateHook: ", t);
            }
        }
    }

    // --- 3. NavStubView Hooks (Bottom Reach & Window Sizing) ---
    public static class GetHotSpaceHeightHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (GestureWidthSettings.isCustomLauncherGesturesEnabled()) {
                    int customReach = GestureWidthSettings.getDesiredBottomReach();
                    param.setResult(customReach);
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in GetHotSpaceHeightHook: ", t);
            }
        }
    }

    public static class GetNavWindowParamHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (GestureWidthSettings.isCustomLauncherGesturesEnabled()) {
                    WindowManager.LayoutParams lp = (WindowManager.LayoutParams) param.getResult();
                    if (lp != null) {
                        int customReach = GestureWidthSettings.getDesiredBottomReach();
                        lp.height = customReach;
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in GetNavWindowParamHook: ", t);
            }
        }
    }

    public static class NavAttachedHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                GestureWidthSettings.registerNavStub(param.thisObject);
                if (param.thisObject instanceof View) {
                    Context ctx = ((View) param.thisObject).getContext();
                    GestureWidthSettings.ensureObserver(ctx);
                    GestureWidthSettings.ensureForceFsgNavBar(ctx);
                }
                Log.i(TAG, "NavStubView attached to window and registered");
            } catch (Throwable t) {
                Log.e(TAG, "Error in NavAttachedHook: ", t);
            }
        }
    }

    public static class NavDetachedHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                GestureWidthSettings.unregisterNavStub(param.thisObject);
            } catch (Throwable t) {
                Log.e(TAG, "Error in NavDetachedHook: ", t);
            }
        }
    }

    // --- 4. Touch Event & Hold Detection Hooks ---
    public static class NavTouchHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (!GestureWidthSettings.isCustomLauncherGesturesEnabled()) return;
                if (param.args == null || param.args.length == 0 || !(param.args[0] instanceof MotionEvent)) return;

                MotionEvent ev = (MotionEvent) param.args[0];
                int action = ev.getActionMasked();

                if (action == MotionEvent.ACTION_DOWN) {
                    sTouchStartX = ev.getRawX();
                    sTouchStartY = ev.getRawY();
                    sTouchStartTime = SystemClock.uptimeMillis();
                    sHoldStartTime = 0;
                    sHoldTriggered = false;
                    sIsHoldingRecents = false;
                } else if (action == MotionEvent.ACTION_MOVE) {
                    float dy = sTouchStartY - ev.getRawY(); // Upward travel distance
                    int targetDist = GestureWidthSettings.getDesiredBottomDistance();
                    int targetHold = GestureWidthSettings.getDesiredBottomHold();
                    float targetSpeed = GestureWidthSettings.getDesiredBottomSpeed();

                    PointF vel = (PointF) XposedHelpers.getObjectField(param.thisObject, "mVelocityPxPerMs");
                    float speed = (vel != null) ? (float) Math.hypot(vel.x, vel.y) : 0f;

                    // Drag upward threshold reached (at least min(targetDist, 50px))
                    if (dy >= Math.min(targetDist, 50.0f)) {
                        // User has slowed down or paused finger movement
                        if (speed < (targetSpeed * 0.4f + 0.8f)) {
                            long now = SystemClock.uptimeMillis();
                            if (sHoldStartTime == 0) {
                                sHoldStartTime = now;
                            } else if (now - sHoldStartTime >= targetHold) {
                                if (!sHoldTriggered) {
                                    sHoldTriggered = true;
                                    sIsHoldingRecents = true;
                                    performHoldHaptic(param.thisObject);
                                    Log.i(TAG, "Hold duration reached (" + (now - sHoldStartTime) + "ms >= " + targetHold + "ms). Recents hold armed!");
                                }
                            }
                        } else if (speed >= (targetSpeed * 0.4f + 0.8f)) {
                            // User is moving fast upward or in a flick: reset hold start
                            sHoldStartTime = 0;
                        }
                    }

                    // If user slides finger all the way back down to bottom, cancel hold
                    if (dy < 20.0f && sHoldTriggered) {
                        sHoldTriggered = false;
                        sIsHoldingRecents = false;
                        sHoldStartTime = 0;
                        Log.d(TAG, "User dragged back down, hold cancelled");
                    }
                } else if (action == MotionEvent.ACTION_UP) {
                    if (sHoldTriggered || sIsHoldingRecents) {
                        Log.i(TAG, "ACTION_UP with Recents hold armed! Triggering Recents overview.");
                        Context ctx = (param.thisObject instanceof View) ? ((View) param.thisObject).getContext() : null;
                        triggerRecents(param.thisObject, ctx);
                        sHoldTriggered = false;
                        sIsHoldingRecents = false;
                        sHoldStartTime = 0;
                    }
                } else if (action == MotionEvent.ACTION_CANCEL) {
                    sHoldTriggered = false;
                    sIsHoldingRecents = false;
                    sHoldStartTime = 0;
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in NavTouchHook: ", t);
            }
        }
    }

    // --- 5. Fast Pull Up Hook (Home flick vs Recents hold) ---
    public static class IsFastPullUpHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (!GestureWidthSettings.isCustomLauncherGesturesEnabled()) return;

                // CRITICAL: If user is holding or just triggered Recents, NEVER treat as fast pull up to Home!
                if (sHoldTriggered || sIsHoldingRecents || (SystemClock.uptimeMillis() - sJustTriggeredRecentsTime < 800)) {
                    param.setResult(false);
                    return;
                }

                int pivotY = XposedHelpers.getIntField(param.thisObject, "mPivotLocY");
                float followTailY = XposedHelpers.getFloatField(param.thisObject, "mFollowTailY");
                PointF vel = (PointF) XposedHelpers.getObjectField(param.thisObject, "mVelocityPxPerMs");

                float upwardDistance = followTailY - (float) pivotY;
                int targetDistance = GestureWidthSettings.getDesiredBottomDistance();
                float targetSpeed = GestureWidthSettings.getDesiredBottomSpeed();

                boolean isUpward = (vel != null && vel.y < 0);
                float speed = (vel != null) ? (float) Math.hypot(vel.x, vel.y) : 0f;

                // Check 1: Fast upward flick (satisfies velocity sensitivity slider)
                if (isUpward && upwardDistance >= 30.0f && speed >= targetSpeed) {
                    param.setResult(true);
                    return;
                }

                // Check 2: Upward drag distance satisfied WITH continuing movement (not stationary/holding)
                if (upwardDistance >= targetDistance && speed >= 0.5f) {
                    param.setResult(true);
                    return;
                }

                // If decelerating or stationary, do not trigger fast pull up
                if (speed < 0.35f) {
                    param.setResult(false);
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in IsFastPullUpHook: ", t);
            }
        }
    }

    // --- 6. FinishAppToHome Hook ---
    public static class FinishAppToHomeHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (!GestureWidthSettings.isCustomLauncherGesturesEnabled()) return;

                // If Recents was triggered, DO NOT switch to Home!
                if (SystemClock.uptimeMillis() - sJustTriggeredRecentsTime < 800) {
                    Log.i(TAG, "Suppressing finishAppToHome because Recents was recently triggered");
                    return;
                }

                Context ctx = null;
                if (param.thisObject instanceof View) {
                    ctx = ((View) param.thisObject).getContext();
                }

                if (isCustomLauncherActive(ctx)) {
                    Log.i(TAG, "Custom launcher active (" + sCurrentDefaultHomePkg + "). Triggering HOME backend event.");

                    // Inject KEYCODE_HOME KeyEvent via NavStubView.sendEvent
                    try {
                        XposedHelpers.callMethod(param.thisObject, "sendEvent", 0, 0, KeyEvent.KEYCODE_HOME);
                        XposedHelpers.callMethod(param.thisObject, "sendEvent", 1, 0, KeyEvent.KEYCODE_HOME);
                    } catch (Throwable t) {
                        Log.w(TAG, "Could not call sendEvent: " + t.getMessage());
                    }

                    // Send Home Intent directly
                    if (ctx != null) {
                        try {
                            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                            homeIntent.addCategory(Intent.CATEGORY_HOME);
                            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            ctx.startActivity(homeIntent);
                        } catch (Throwable t) {
                            Log.e(TAG, "Error starting home intent: ", t);
                        }
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in FinishAppToHomeHook: ", t);
            }
        }
    }

    // --- 7. Recents Action Execution Hooks ---
    public static class PerformAppToRecentsHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (!GestureWidthSettings.isCustomLauncherGesturesEnabled()) return;
                Context ctx = (param.thisObject instanceof View) ? ((View) param.thisObject).getContext() : null;
                if (isCustomLauncherActive(ctx)) {
                    Log.i(TAG, "performAppToRecents intercepted with custom launcher! Triggering Recents.");
                    triggerRecents(param.thisObject, ctx);
                    param.setResult(null); // Return immediately to prevent fallback to performAppToHome!
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in PerformAppToRecentsHook: ", t);
            }
        }
    }

    public static class PerformHomeToRecentsHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (!GestureWidthSettings.isCustomLauncherGesturesEnabled()) return;
                Context ctx = (param.thisObject instanceof View) ? ((View) param.thisObject).getContext() : null;
                if (isCustomLauncherActive(ctx)) {
                    Log.i(TAG, "performHomeToRecents intercepted with custom launcher! Triggering Recents.");
                    triggerRecents(param.thisObject, ctx);
                    param.setResult(null);
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in PerformHomeToRecentsHook: ", t);
            }
        }
    }

    // --- 8. Prevent MIUI from aborting Recents when 3rd party launcher is active ---
    public static class IsRecentsLoadedHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (GestureWidthSettings.isCustomLauncherGesturesEnabled()) {
                param.setResult(true);
            }
        }
    }

    public static class RemoteAnimStartFailedHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (GestureWidthSettings.isCustomLauncherGesturesEnabled()) {
                param.setResult(false);
            }
        }
    }

    public static class IsRecentsDisabledHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (GestureWidthSettings.isCustomLauncherGesturesEnabled()) {
                param.setResult(false);
            }
        }
    }

    public static class IsToAppHoldStateHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (GestureWidthSettings.isCustomLauncherGesturesEnabled() && (sHoldTriggered || sIsHoldingRecents)) {
                param.setResult(true);
            }
        }
    }

    public static class IsToHomeHoldStateHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (GestureWidthSettings.isCustomLauncherGesturesEnabled() && (sHoldTriggered || sIsHoldingRecents)) {
                param.setResult(true);
            }
        }
    }
}
