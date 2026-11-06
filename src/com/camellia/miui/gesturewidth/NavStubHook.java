package com.camellia.miui.gesturewidth;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PointF;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class NavStubHook {
    public static final String TAG = GestureWidthSettings.TAG;

    private static String sCurrentDefaultHomePkg = null;
    private static long sLastHomeCheckTime = 0;

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

    // --- 3. NavStubView Hooks (Bottom Gestures: Reach, Distance, Speed, Action) ---
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

    public static class IsFastPullUpHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (!GestureWidthSettings.isCustomLauncherGesturesEnabled()) return;

                int pivotY = XposedHelpers.getIntField(param.thisObject, "mPivotLocY");
                float followTailY = XposedHelpers.getFloatField(param.thisObject, "mFollowTailY");
                PointF vel = (PointF) XposedHelpers.getObjectField(param.thisObject, "mVelocityPxPerMs");

                float upwardDistance = followTailY - (float) pivotY;
                int targetDistance = GestureWidthSettings.getDesiredBottomDistance();
                float targetSpeed = GestureWidthSettings.getDesiredBottomSpeed();

                boolean isUpward = (vel != null && vel.y < 0);
                float speed = (vel != null) ? (float) Math.hypot(vel.x, vel.y) : 0f;

                if (upwardDistance >= targetDistance) {
                    // Attribute 1: Upward slide distance threshold satisfied
                    param.setResult(true);
                } else if (isUpward && upwardDistance >= 30.0f && speed >= targetSpeed) {
                    // Attribute 2: Upward flick velocity sensitivity satisfied
                    param.setResult(true);
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in IsFastPullUpHook: ", t);
            }
        }
    }

    public static class FinishAppToHomeHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (!GestureWidthSettings.isCustomLauncherGesturesEnabled()) return;

                Context ctx = null;
                if (param.thisObject instanceof View) {
                    ctx = ((View) param.thisObject).getContext();
                }

                if (isCustomLauncherActive(ctx)) {
                    Log.i(TAG, "Custom launcher active (" + sCurrentDefaultHomePkg + "). Triggering HOME backend event.");

                    // Method A: Inject KEYCODE_HOME KeyEvent via NavStubView.sendEvent (simulating home button tap)
                    boolean injected = false;
                    try {
                        XposedHelpers.callMethod(param.thisObject, "sendEvent", 0, 0, KeyEvent.KEYCODE_HOME);
                        XposedHelpers.callMethod(param.thisObject, "sendEvent", 1, 0, KeyEvent.KEYCODE_HOME);
                        injected = true;
                    } catch (Throwable t) {
                        Log.w(TAG, "Could not call sendEvent: " + t.getMessage());
                    }

                    // Method B: Send Home Intent directly
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

    // --- 4. Hold Duration Hooks for Recents ---
    public static class IsToAppHoldStateHook extends XC_MethodHook {
        private static long sHoldStartMs = 0;

        public static void reset() {
            sHoldStartMs = 0;
        }

        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (!GestureWidthSettings.isCustomLauncherGesturesEnabled()) return;

                int targetDist = GestureWidthSettings.getDesiredBottomDistance();
                float targetSpeed = GestureWidthSettings.getDesiredBottomSpeed();
                int targetHold = GestureWidthSettings.getDesiredBottomHold();

                float followTailY = XposedHelpers.getFloatField(param.thisObject, "mFollowTailY");
                int pivotY = XposedHelpers.getIntField(param.thisObject, "mPivotLocY");
                float upwardDist = followTailY - (float) pivotY;

                PointF vel = (PointF) XposedHelpers.getObjectField(param.thisObject, "mVelocityPxPerMs");
                float speed = (vel != null) ? (float) Math.hypot(vel.x, vel.y) : 0f;

                // When user dragged up past threshold and slowed down/holding:
                if (upwardDist >= Math.min(targetDist, 60.0f)) {
                    if (speed < (targetSpeed * 0.4f + 1.2f)) {
                        long now = SystemClock.uptimeMillis();
                        if (sHoldStartMs == 0) {
                            sHoldStartMs = now;
                        } else if (now - sHoldStartMs >= targetHold) {
                            // Hold duration satisfied!
                            param.setResult(true);
                            return;
                        }
                    } else {
                        sHoldStartMs = 0;
                    }
                } else {
                    sHoldStartMs = 0;
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in IsToAppHoldStateHook before: ", t);
            }
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (!GestureWidthSettings.isCustomLauncherGesturesEnabled()) return;

                Boolean res = (Boolean) param.getResult();
                if (res != null && res) {
                    int targetHold = GestureWidthSettings.getDesiredBottomHold();
                    long now = SystemClock.uptimeMillis();
                    if (sHoldStartMs == 0) {
                        sHoldStartMs = now;
                    }
                    if (now - sHoldStartMs < targetHold) {
                        // User has not held long enough yet according to custom duration
                        param.setResult(false);
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in IsToAppHoldStateHook after: ", t);
            }
        }
    }

    public static class NavTouchHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (param.args != null && param.args.length > 0 && param.args[0] instanceof MotionEvent) {
                    MotionEvent ev = (MotionEvent) param.args[0];
                    int action = ev.getActionMasked();
                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        IsToAppHoldStateHook.reset();
                    }
                }
            } catch (Throwable t) {
                // ignore
            }
        }
    }
}
