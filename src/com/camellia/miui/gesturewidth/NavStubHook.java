package com.camellia.miui.gesturewidth;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PointF;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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
    private static boolean sHoldTriggered = false;
    private static boolean sIsHoldingRecents = false;
    private static long sJustTriggeredRecentsTime = 0;

    // Hold timer handler and state
    private static final Handler sHoldHandler = new Handler(Looper.getMainLooper());
    private static volatile Object sCurrentNavStub = null;
    private static volatile boolean sTouchActive = false;
    private static volatile boolean sHoldScheduled = false;
    private static long sLastProcessedEventTime = 0;

    private static final Runnable sHoldRunnable = new Runnable() {
        @Override
        public void run() {
            if (!sTouchActive) return;
            sHoldTriggered = true;
            sIsHoldingRecents = true;
            Log.i(TAG, ">>> Hold duration timer fired! Recents hold armed with haptics! <<<");
            performHoldHaptic(sCurrentNavStub);
        }
    };

    // Retained reference to MIUI's OverviewCommandHelper
    public static volatile Object sOverviewCommandHelper = null;

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
     * Triggers the Recents overview action smoothly without raw key injection race conditions.
     */
    public static void showRecentsSmoothly(Object navStubObj, Context context) {
        long now = SystemClock.uptimeMillis();
        if (now - sJustTriggeredRecentsTime < 400) {
            Log.d(TAG, "showRecentsSmoothly debounced (already triggered " + (now - sJustTriggeredRecentsTime) + "ms ago)");
            return;
        }
        sJustTriggeredRecentsTime = now;
        Log.i(TAG, ">>> showRecentsSmoothly: launching Recents overview! <<<");

        // 1. Tell NavStubView / RecentsAnimation to finish cleanly to Recents
        // This transfers the window surface without freezing or leaving background artifacts!
        if (navStubObj != null) {
            try {
                XposedHelpers.callMethod(navStubObj, "finish", true, (Runnable) null, false);
                Log.d(TAG, "Called navStub.finish(true, null, false)");
            } catch (Throwable t) {
                try {
                    XposedHelpers.callMethod(navStubObj, "finish", true, (Runnable) null);
                } catch (Throwable ignored) {}
            }

            try {
                XposedHelpers.callMethod(navStubObj, "finalization", "toRecents");
            } catch (Throwable ignored) {}
        }

        // 2. Open RecentsActivity via OverviewCommandHelper (native MIUI animation provider)
        boolean launched = false;
        if (sOverviewCommandHelper != null) {
            try {
                XposedHelpers.callMethod(sOverviewCommandHelper, "onOverviewShown", true);
                launched = true;
                Log.i(TAG, "Recents launched smoothly via OverviewCommandHelper.onOverviewShown(true)");
            } catch (Throwable t) {
                Log.w(TAG, "OverviewCommandHelper.onOverviewShown failed: " + t.getMessage());
            }
        }

        // 3. Fallback: IStatusBarService.toggleRecentApps IPC (clean system call, no hardware key clash)
        if (!launched && context != null) {
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
                        Log.i(TAG, "Recents toggled via IStatusBarService.toggleRecentApps fallback");
                        launched = true;
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error calling IStatusBarService.toggleRecentApps: ", t);
            }
        }

        // 4. Reset hold state immediately
        sHoldHandler.removeCallbacks(sHoldRunnable);
        sTouchActive = false;
        sHoldScheduled = false;
        sHoldTriggered = false;
        sIsHoldingRecents = false;
    }

    /**
     * Dismisses the Recents overview activity cleanly and returns to the home screen (DAP launcher).
     */
    public static void dismissRecentsIfActive(Object navStubObj) {
        Log.i(TAG, ">>> dismissRecentsIfActive called <<<");
        try {
            ClassLoader cl = navStubObj.getClass().getClassLoader();
            Class<?> trackerClass = XposedHelpers.findClass("com.miui.home.recents.RecentsActivityTracker", cl);
            Object activity = XposedHelpers.callStaticMethod(trackerClass, "getCurrentActivity");
            if (activity != null) {
                Log.i(TAG, "dismissRecentsIfActive: RecentsActivity is present: " + activity);
                boolean isFinishing = false;
                try {
                    isFinishing = (boolean) XposedHelpers.callMethod(activity, "isFinishing");
                } catch (Throwable ignored) {}

                if (!isFinishing) {
                    boolean dismissed = false;
                    try {
                        Object container = XposedHelpers.getObjectField(activity, "recentsContainer");
                        if (container != null) {
                            XposedHelpers.callMethod(container, "dismissRecentsToHome", true);
                            dismissed = true;
                            Log.i(TAG, "Recents dismissed via recentsContainer.dismissRecentsToHome(true)");
                        }
                    } catch (Throwable t) {
                        Log.w(TAG, "dismissRecentsToHome failed: " + t.getMessage());
                    }

                    if (!dismissed) {
                        try {
                            Object recentsView = XposedHelpers.getObjectField(activity, "recentsView");
                            if (recentsView != null) {
                                XposedHelpers.callMethod(recentsView, "startHome");
                                dismissed = true;
                                Log.i(TAG, "Recents dismissed via recentsView.startHome()");
                            }
                        } catch (Throwable t) {
                            Log.w(TAG, "startHome failed: " + t.getMessage());
                        }
                    }

                    if (!dismissed) {
                        try {
                            XposedHelpers.callMethod(activity, "finish");
                            Log.i(TAG, "Recents dismissed via activity.finish()");
                        } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error in dismissRecentsIfActive: ", t);
        }

        if (navStubObj != null) {
            try {
                XposedHelpers.callMethod(navStubObj, "finishRecentsActivityDirectly");
            } catch (Throwable ignored) {}

            try {
                XposedHelpers.callMethod(navStubObj, "finalization", "performRecentsToHome");
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

    // --- OverviewCommandHelper capture hooks ---
    public static class OverviewCommandHelperInitHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            sOverviewCommandHelper = param.thisObject;
            Log.i(TAG, "Captured OverviewCommandHelper instance via constructor: " + sOverviewCommandHelper);
        }
    }

    public static class TisInitHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                Object helper = XposedHelpers.getObjectField(param.thisObject, "mOverviewCommandHelper");
                if (helper != null) {
                    sOverviewCommandHelper = helper;
                    Log.i(TAG, "Captured OverviewCommandHelper from TouchInteractionService: " + helper);
                }
            } catch (Throwable ignored) {}
        }
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

    // --- Window Mode Override for Recents Overview ---
    public static class GetCurrentWindowModeHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (!GestureWidthSettings.isCustomLauncherGesturesEnabled()) return;
            try {
                ClassLoader cl = param.thisObject.getClass().getClassLoader();
                Class<?> trackerClass = XposedHelpers.findClass("com.miui.home.recents.RecentsActivityTracker", cl);
                Object activity = XposedHelpers.callStaticMethod(trackerClass, "getCurrentActivity");
                if (activity != null) {
                    boolean isFinishing = (boolean) XposedHelpers.callMethod(activity, "isFinishing");
                    boolean isDestroyed = (boolean) XposedHelpers.callMethod(activity, "isDestroyed");
                    if (!isFinishing && !isDestroyed) {
                        // Recents overview is currently in foreground -> Mode 3 (recent-task)
                        param.setResult(3);
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    // --- 4. Touch Event & Hold Detection Hooks ---
    public static void processNavTouchEvent(Object navStub, MotionEvent ev) {
        if (!GestureWidthSettings.isCustomLauncherGesturesEnabled()) return;
        if (ev == null) return;

        int action = ev.getActionMasked();
        long eventTime = ev.getEventTime();
        if (action == MotionEvent.ACTION_MOVE && eventTime == sLastProcessedEventTime) {
            return;
        }
        sLastProcessedEventTime = eventTime;

        // Check if Recents overview is currently active in foreground
        boolean isRecentsActive = false;
        try {
            ClassLoader cl = navStub.getClass().getClassLoader();
            Class<?> trackerClass = XposedHelpers.findClass("com.miui.home.recents.RecentsActivityTracker", cl);
            Object activity = XposedHelpers.callStaticMethod(trackerClass, "getCurrentActivity");
            if (activity != null) {
                boolean isFinishing = (boolean) XposedHelpers.callMethod(activity, "isFinishing");
                boolean isDestroyed = (boolean) XposedHelpers.callMethod(activity, "isDestroyed");
                isRecentsActive = !isFinishing && !isDestroyed;
            }
        } catch (Throwable ignored) {}

        if (action == MotionEvent.ACTION_DOWN) {
            sTouchStartX = ev.getRawX();
            sTouchStartY = ev.getRawY();
            sTouchStartTime = SystemClock.uptimeMillis();
            sTouchActive = true;
            sHoldScheduled = false;
            sHoldTriggered = false;
            sIsHoldingRecents = false;
            sCurrentNavStub = navStub;
            sHoldHandler.removeCallbacks(sHoldRunnable);
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (isRecentsActive) {
                return;
            }

            float dy = sTouchStartY - ev.getRawY();
            int targetDist = GestureWidthSettings.getDesiredBottomDistance();
            int targetHold = GestureWidthSettings.getDesiredBottomHold();
            float targetSpeed = GestureWidthSettings.getDesiredBottomSpeed();

            PointF vel = null;
            try {
                vel = (PointF) XposedHelpers.getObjectField(navStub, "mVelocityPxPerMs");
            } catch (Throwable ignored) {}
            float speed = (vel != null) ? (float) Math.hypot(vel.x, vel.y) : 0f;

            // Schedule hold timer as soon as user pulls upward slightly (min(targetDist, 35px))
            if (dy >= Math.min(targetDist, 35.0f)) {
                if (!sHoldScheduled && !sHoldTriggered) {
                    sHoldScheduled = true;
                    sHoldHandler.postDelayed(sHoldRunnable, Math.max(targetHold, 40));
                    Log.d(TAG, "Scheduled Recents hold timer for " + targetHold + "ms (dy=" + dy + ")");
                }
            }

            // If user drags back down to bottom, cancel pending or armed hold
            if (dy < 15.0f && (sHoldScheduled || sHoldTriggered)) {
                sHoldHandler.removeCallbacks(sHoldRunnable);
                sHoldScheduled = false;
                sHoldTriggered = false;
                sIsHoldingRecents = false;
                Log.d(TAG, "User dragged back down, hold cancelled");
            }

            // If user performs an obvious fast upward flick (Home flick), cancel pending hold timer
            if (speed >= (targetSpeed * 1.5f + 1.0f) && dy > 80.0f && !sHoldTriggered) {
                sHoldHandler.removeCallbacks(sHoldRunnable);
                sHoldScheduled = false;
            }
        } else if (action == MotionEvent.ACTION_UP) {
            sHoldHandler.removeCallbacks(sHoldRunnable);
            sTouchActive = false;
            sHoldScheduled = false;

            if (isRecentsActive) {
                float dy = sTouchStartY - ev.getRawY();
                if (dy >= 30.0f) {
                    Log.i(TAG, "Swipe up inside Recents detected (dy=" + dy + ")! Dismissing to Home.");
                    dismissRecentsIfActive(navStub);
                }
                return;
            }

            if (sHoldTriggered || sIsHoldingRecents) {
                Log.i(TAG, "ACTION_UP with Recents hold armed! Triggering Recents smoothly.");
                Context ctx = (navStub instanceof View) ? ((View) navStub).getContext() : null;
                showRecentsSmoothly(navStub, ctx);
            }

            sHoldTriggered = false;
            sIsHoldingRecents = false;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            sHoldHandler.removeCallbacks(sHoldRunnable);
            sTouchActive = false;
            sHoldScheduled = false;
            sHoldTriggered = false;
            sIsHoldingRecents = false;
        }
    }

    public static class NavTouchHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (param.args == null || param.args.length == 0 || !(param.args[0] instanceof MotionEvent)) return;
                processNavTouchEvent(param.thisObject, (MotionEvent) param.args[0]);
            } catch (Throwable t) {
                Log.e(TAG, "Error in NavTouchHook: ", t);
            }
        }
    }

    public static class NavPointerEventHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (param.args == null || param.args.length == 0 || !(param.args[0] instanceof MotionEvent)) return;
                processNavTouchEvent(param.thisObject, (MotionEvent) param.args[0]);
            } catch (Throwable t) {
                Log.e(TAG, "Error in NavPointerEventHook: ", t);
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
                    Log.i(TAG, "performAppToRecents intercepted with custom launcher! Showing Recents smoothly.");
                    showRecentsSmoothly(param.thisObject, ctx);
                    param.setResult(null); // Prevent NPE on mLauncher.getRecentsView()
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
                    Log.i(TAG, "performHomeToRecents intercepted with custom launcher! Showing Recents smoothly.");
                    showRecentsSmoothly(param.thisObject, ctx);
                    param.setResult(null);
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in PerformHomeToRecentsHook: ", t);
            }
        }
    }

    // --- 8. Swipe Up in Recents Overview to Dismiss (MIUI 11 Return to Home) ---
    public static class PerformRecentsToHomeHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (!GestureWidthSettings.isCustomLauncherGesturesEnabled()) return;
                Log.i(TAG, "performRecentsToHome intercepted! Dismissing Recents overview.");
                dismissRecentsIfActive(param.thisObject);
                param.setResult(null);
            } catch (Throwable t) {
                Log.e(TAG, "Error in PerformRecentsToHomeHook: ", t);
            }
        }
    }

    public static class PerformHomeToHomeHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (!GestureWidthSettings.isCustomLauncherGesturesEnabled()) return;
                dismissRecentsIfActive(param.thisObject);
            } catch (Throwable t) {
                Log.e(TAG, "Error in PerformHomeToHomeHook: ", t);
            }
        }
    }

    // --- 9. Prevent MIUI from aborting Recents when 3rd party launcher is active ---
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

    // --- 10. Recents Horizontal Overscroll Clamping Hooks ---
    public static class SetStackScrollPHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                Object scroller = param.thisObject;
                Object layoutAlgo = XposedHelpers.getObjectField(scroller, "mLayoutAlgorithm");
                if (layoutAlgo == null) return;

                String algoName = layoutAlgo.getClass().getSimpleName();
                if (algoName.contains("Horizontal")) {
                    float minP = XposedHelpers.getFloatField(layoutAlgo, "mMinScrollP");
                    float maxP = XposedHelpers.getFloatField(layoutAlgo, "mMaxScrollP");
                    if (minP > maxP) {
                        float tmp = minP; minP = maxP; maxP = tmp;
                    }
                    float scrollP = (Float) param.args[0];
                    // Maximum overscroll beyond task boundaries is strictly clamped to 0.15f
                    // This prevents task cards from flying off to the right leaving only wallpaper!
                    float maxAllowed = maxP + 0.15f;
                    float minAllowed = minP - 0.15f;
                    if (scrollP > maxAllowed) {
                        param.args[0] = maxAllowed;
                        Log.d(TAG, "Clamped horizontal scrollP from " + scrollP + " to maxAllowed=" + maxAllowed);
                    } else if (scrollP < minAllowed) {
                        param.args[0] = minAllowed;
                        Log.d(TAG, "Clamped horizontal scrollP from " + scrollP + " to minAllowed=" + minAllowed);
                    }
                }
            } catch (Throwable t) {
                // ignore
            }
        }
    }

    public static class AdjustEndScrollPixHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                Object scroller = param.thisObject;
                Object layoutAlgo = XposedHelpers.getObjectField(scroller, "mLayoutAlgorithm");
                if (layoutAlgo == null) return;

                float minP = XposedHelpers.getFloatField(layoutAlgo, "mMinScrollP");
                float maxP = XposedHelpers.getFloatField(layoutAlgo, "mMaxScrollP");
                if (minP > maxP) {
                    float tmp = minP; minP = maxP; maxP = tmp;
                }
                float flingDownScrollP = XposedHelpers.getFloatField(scroller, "mFlingDownScrollP");
                float flingDownX = XposedHelpers.getFloatField(scroller, "mFlingDownX");

                float resultPix = (Float) param.getResult();
                float deltaP = (Float) XposedHelpers.callMethod(layoutAlgo, "getDeltaPForX", flingDownX, resultPix);
                float targetScrollP = flingDownScrollP + deltaP;

                if (targetScrollP > maxP || targetScrollP < minP) {
                    float clampedTarget = Math.max(minP, Math.min(targetScrollP, maxP));
                    float nearest = (Float) XposedHelpers.callMethod(scroller, "getNearestMotionlessScrollP", clampedTarget);
                    nearest = Math.max(minP, Math.min(nearest, maxP));
                    int deltaX = (Integer) XposedHelpers.callMethod(layoutAlgo, "getXForDeltaP", flingDownScrollP, nearest);
                    param.setResult(flingDownX + (float) deltaX);
                    Log.d(TAG, "AdjustEndScrollPixHook: clamped targetScrollP " + targetScrollP + " -> " + nearest);
                }
            } catch (Throwable t) {
                // ignore
            }
        }
    }

    public static class AnimateBoundScrollHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                Object scroller = param.thisObject;
                Object layoutAlgo = XposedHelpers.getObjectField(scroller, "mLayoutAlgorithm");
                if (layoutAlgo == null) return;

                float minP = XposedHelpers.getFloatField(layoutAlgo, "mMinScrollP");
                float maxP = XposedHelpers.getFloatField(layoutAlgo, "mMaxScrollP");
                if (minP > maxP) {
                    float tmp = minP; minP = maxP; maxP = tmp;
                }
                float currentScroll = (Float) XposedHelpers.callMethod(scroller, "getStackScroll");
                float bounded = Math.max(minP, Math.min(currentScroll, maxP));
                if (Float.compare(currentScroll, bounded) != 0) {
                    // Smoothly animate back to within bounds
                    XposedHelpers.callMethod(scroller, "animateScroll", bounded, 250, (Runnable) null);
                    param.setResult(null);
                }
            } catch (Throwable t) {
                // ignore
            }
        }
    }

    public static class AfterFrictionValueHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                float result = (Float) param.getResult();
                if (result > 0.15f) {
                    param.setResult(0.15f);
                } else if (result < -0.15f) {
                    param.setResult(-0.15f);
                }
            } catch (Throwable ignored) {}
        }
    }
}
