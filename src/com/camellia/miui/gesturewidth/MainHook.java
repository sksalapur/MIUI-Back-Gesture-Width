package com.camellia.miui.gesturewidth;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MainHook implements IXposedHookLoadPackage {
    public static final String TAG = GestureWidthSettings.TAG;
    public static final String TARGET_PKG = "com.miui.home";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PKG.equals(lpparam.packageName)) {
            return;
        }

        Log.i(TAG, "Initializing MIUI Gesture Width & Custom Launcher Gestures hook in " + lpparam.packageName);
        XposedBridge.log(TAG + ": Initializing MIUI Gesture Width & Custom Launcher Gestures hook in " + lpparam.packageName);

        // --- Side Back Gestures (GestureStubView) ---
        try {
            Class<?> gestureStubViewClass = XposedHelpers.findClass("com.miui.home.recents.GestureStubView", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(gestureStubViewClass, "initScreenSizeAndDensity",
                    new GestureStubHook.InitScreenSizeHook());

            XposedHelpers.findAndHookMethod(gestureStubViewClass, "setSize", int.class,
                    new GestureStubHook.SetSizeHook());

            XposedHelpers.findAndHookMethod(gestureStubViewClass, "getGestureStubWindowParam",
                    new GestureStubHook.GetWindowParamHook());

            XposedHelpers.findAndHookMethod(gestureStubViewClass, "initGestureEdgeSize",
                    new GestureStubHook.InitGestureEdgeSizeHook());

            XposedHelpers.findAndHookMethod(gestureStubViewClass, "onAttachedToWindow",
                    new GestureStubHook.AttachedToWindowHook());

            XposedHelpers.findAndHookMethod(gestureStubViewClass, "onDetachedFromWindow",
                    new GestureStubHook.DetachedFromWindowHook());

            Log.i(TAG, "Successfully installed hooks on GestureStubView");
        } catch (Throwable t) {
            Log.e(TAG, "Error installing hooks on GestureStubView: ", t);
        }

        // --- Side Back Swipe Control (GesturesBackController) ---
        try {
            Class<?> gesturesBackControllerClass = XposedHelpers.findClass("com.miui.home.recents.GesturesBackController", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(gesturesBackControllerClass, "setGestureEdgeWidth", int.class, int.class,
                    new GesturesBackControllerHook.SetGestureEdgeWidthHook());

            Class<?> gestureStubViewClass = XposedHelpers.findClass("com.miui.home.recents.GestureStubView", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(gesturesBackControllerClass, "processPointerEvent",
                    android.view.MotionEvent.class, gestureStubViewClass,
                    new GesturesBackControllerHook.ProcessPointerEventHook());

            XposedHelpers.findAndHookMethod(gesturesBackControllerClass, "isFinished", float.class, int.class,
                    new GesturesBackControllerHook.IsFinishedHook());

            Log.i(TAG, "Successfully installed hooks on GesturesBackController");
        } catch (Throwable t) {
            Log.e(TAG, "Error installing hooks on GesturesBackController: ", t);
        }

        // --- Custom Launcher Bypass Hooks (Utilities & BaseRecentsImpl) ---
        try {
            Class<?> utilitiesClass = XposedHelpers.findClass("com.miui.home.launcher.common.Utilities", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(utilitiesClass, "isUseMiuiHomeAsDefaultHome", Context.class,
                    new NavStubHook.IsUseMiuiHomeHook());

            try {
                XposedHelpers.findAndHookMethod(utilitiesClass, "isUsePocoHomeAsDefaultHome", Context.class,
                        new NavStubHook.IsUseMiuiHomeHook());
            } catch (Throwable ignored) {}

            Class<?> baseRecentsImplClass = XposedHelpers.findClass("com.miui.home.recents.BaseRecentsImpl", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(baseRecentsImplClass, "isUseLauncherRecentsAndFsGesture",
                    new NavStubHook.IsUseLauncherRecentsHook());

            XposedHelpers.findAndHookMethod(baseRecentsImplClass, "setIsUseMiuiHomeAsDefaultHome", boolean.class,
                    new NavStubHook.SetIsUseMiuiHomeHook());

            XposedHelpers.findAndHookMethod(baseRecentsImplClass, "updateFsgWindowState",
                    new NavStubHook.UpdateFsgWindowStateHook());

            Log.i(TAG, "Successfully installed custom launcher gesture bypass hooks on Utilities & BaseRecentsImpl");
        } catch (Throwable t) {
            Log.e(TAG, "Error installing custom launcher bypass hooks: ", t);
        }

        // --- TouchInteractionService & OverviewCommandHelper hooks ---
        try {
            Class<?> tisClass = XposedHelpers.findClass("com.miui.home.recents.TouchInteractionService", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(tisClass, "initWhenUserUnlocked",
                    new NavStubHook.TisInitHook());

            try {
                Class<?> overviewCmdHelperClass = XposedHelpers.findClass("com.miui.home.recents.OverviewCommandHelper", lpparam.classLoader);
                Class<?> observerClass = XposedHelpers.findClass("com.miui.home.recents.OverviewComponentObserver", lpparam.classLoader);
                XposedHelpers.findAndHookConstructor(overviewCmdHelperClass, new Object[] {
                        Context.class, observerClass, new NavStubHook.OverviewCommandHelperInitHook()
                });
            } catch (Throwable t) {
                Log.w(TAG, "Hook OverviewCommandHelper constructor failed: " + t.getMessage());
            }
            Log.i(TAG, "Successfully installed hooks on TouchInteractionService / OverviewCommandHelper");
        } catch (Throwable t) {
            Log.w(TAG, "Hook TouchInteractionService failed: " + t.getMessage());
        }

        // --- Bottom Navigation Gestures (NavStubView & GestureStateMachine) ---
        try {
            Class<?> navStubViewClass = XposedHelpers.findClass("com.miui.home.recents.NavStubView", lpparam.classLoader);

            try {
                XposedHelpers.findAndHookMethod(navStubViewClass, "getHotSpaceHeight",
                        new NavStubHook.GetHotSpaceHeightHook());
            } catch (Throwable t) { Log.w(TAG, "Hook getHotSpaceHeight failed: " + t.getMessage()); }

            try {
                XposedHelpers.findAndHookMethod(navStubViewClass, "getWindowParam", int.class,
                        new NavStubHook.GetNavWindowParamHook());
            } catch (Throwable t) { Log.w(TAG, "Hook getWindowParam failed: " + t.getMessage()); }

            try {
                XposedHelpers.findAndHookMethod(navStubViewClass, "onAttachedToWindow",
                        new NavStubHook.NavAttachedHook());
            } catch (Throwable t) { Log.w(TAG, "Hook onAttachedToWindow failed: " + t.getMessage()); }

            try {
                XposedHelpers.findAndHookMethod(navStubViewClass, "onDetachedFromWindow",
                        new NavStubHook.NavDetachedHook());
            } catch (Throwable t) { Log.w(TAG, "Hook onDetachedFromWindow failed: " + t.getMessage()); }

            try {
                XposedHelpers.findAndHookMethod(navStubViewClass, "setKeepHidden", boolean.class,
                        new NavStubHook.SetKeepHiddenHook());
            } catch (Throwable t) { Log.w(TAG, "Hook setKeepHidden failed: " + t.getMessage()); }

            try {
                XposedHelpers.findAndHookMethod(navStubViewClass, "isFastPullUp",
                        new NavStubHook.IsFastPullUpHook());
            } catch (Throwable t) { Log.w(TAG, "Hook isFastPullUp failed: " + t.getMessage()); }

            try {
                XposedHelpers.findAndHookMethod(navStubViewClass, "finishAppToHome", boolean.class,
                        new NavStubHook.FinishAppToHomeHook());
            } catch (Throwable t) { Log.w(TAG, "Hook finishAppToHome failed: " + t.getMessage()); }

            try {
                XposedHelpers.findAndHookMethod(navStubViewClass, "getCurrentWindowMode",
                        MotionEvent.class, boolean.class, boolean.class, boolean.class,
                        new NavStubHook.GetCurrentWindowModeHook());
            } catch (Throwable t) { Log.w(TAG, "Hook getCurrentWindowMode failed: " + t.getMessage()); }

            try {
                XposedHelpers.findAndHookMethod(navStubViewClass, "performRecentsToHome",
                        new NavStubHook.PerformRecentsToHomeHook());
            } catch (Throwable t) { Log.w(TAG, "Hook performRecentsToHome failed: " + t.getMessage()); }

            try {
                XposedHelpers.findAndHookMethod(navStubViewClass, "performHomeToHome",
                        new NavStubHook.PerformHomeToHomeHook());
            } catch (Throwable t) { Log.w(TAG, "Hook performHomeToHome failed: " + t.getMessage()); }

            try {
                XposedHelpers.findAndHookMethod(navStubViewClass, "isToAppHoldState",
                        new NavStubHook.IsToAppHoldStateHook());
            } catch (Throwable t) { Log.w(TAG, "Hook isToAppHoldState failed: " + t.getMessage()); }

            try {
                XposedHelpers.findAndHookMethod(navStubViewClass, "isToHomeHoldState",
                        new NavStubHook.IsToHomeHoldStateHook());
            } catch (Throwable t) { Log.w(TAG, "Hook isToHomeHoldState failed: " + t.getMessage()); }

            try {
                XposedHelpers.findAndHookMethod(navStubViewClass, "isRecentsLoaded",
                        new NavStubHook.IsRecentsLoadedHook());
            } catch (Throwable t) { Log.w(TAG, "Hook isRecentsLoaded failed: " + t.getMessage()); }

            try {
                XposedHelpers.findAndHookMethod(navStubViewClass, "isNeedStopBecauseRecentsRemoteAnimStartFailed",
                        new NavStubHook.RemoteAnimStartFailedHook());
            } catch (Throwable t) { Log.w(TAG, "Hook isNeedStopBecauseRecentsRemoteAnimStartFailed failed: " + t.getMessage()); }

            try {
                XposedHelpers.findAndHookMethod(navStubViewClass, "isRecentsDisabled",
                        new NavStubHook.IsRecentsDisabledHook());
            } catch (Throwable t) { Log.w(TAG, "Hook isRecentsDisabled failed: " + t.getMessage()); }

            try {
                XposedHelpers.findAndHookMethod(navStubViewClass, "performAppToRecents", boolean.class,
                        new NavStubHook.PerformAppToRecentsHook());
            } catch (Throwable t) { Log.w(TAG, "Hook performAppToRecents(boolean) failed: " + t.getMessage()); }

            try {
                XposedHelpers.findAndHookMethod(navStubViewClass, "performAppToRecents",
                        new NavStubHook.PerformAppToRecentsHook());
            } catch (Throwable t) { Log.w(TAG, "Hook performAppToRecents() failed: " + t.getMessage()); }

            try {
                XposedHelpers.findAndHookMethod(navStubViewClass, "performHomeToRecents",
                        new NavStubHook.PerformHomeToRecentsHook());
            } catch (Throwable t) { Log.w(TAG, "Hook performHomeToRecents failed: " + t.getMessage()); }

            try {
                XposedHelpers.findAndHookMethod(navStubViewClass, "onTouchEvent", MotionEvent.class,
                        new NavStubHook.NavTouchHook());
            } catch (Throwable t) { Log.w(TAG, "Hook onTouchEvent failed: " + t.getMessage()); }

            try {
                XposedHelpers.findAndHookMethod(navStubViewClass, "onPointerEvent", MotionEvent.class,
                        new NavStubHook.NavPointerEventHook());
            } catch (Throwable t) { Log.w(TAG, "Hook onPointerEvent failed: " + t.getMessage()); }

            Log.i(TAG, "Successfully installed bottom gesture & hold duration hooks on NavStubView");
        } catch (Throwable t) {
            Log.e(TAG, "Error installing hooks on NavStubView: ", t);
        }

        // --- Horizontal Recents Overscroll Clamping Hooks ---
        try {
            Class<?> scrollerClass = XposedHelpers.findClass("com.miui.home.recents.views.TaskStackViewScroller", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(scrollerClass, "setStackScrollP", float.class,
                    new NavStubHook.SetStackScrollPHook());

            Class<?> horizScrollerClass = XposedHelpers.findClass("com.miui.home.recents.views.TaskStackViewHorizontalScroller", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(horizScrollerClass, "adjustEndScrollPix", float.class,
                    new NavStubHook.AdjustEndScrollPixHook());

            XposedHelpers.findAndHookMethod(horizScrollerClass, "animateBoundScroll", int.class,
                    new NavStubHook.AnimateBoundScrollHook());

            Class<?> algoHorizClass = XposedHelpers.findClass("com.miui.home.recents.views.TaskStackViewsAlgorithmHorizontal", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(algoHorizClass, "afterFrictionValue", float.class, float.class,
                    new NavStubHook.AfterFrictionValueHook());

            Log.i(TAG, "Successfully installed horizontal Recents overscroll clamping hooks");
        } catch (Throwable t) {
            Log.e(TAG, "Error installing horizontal Recents overscroll clamping hooks: ", t);
        }
    }
}
