package com.camellia.miui.gesturewidth;

import android.content.Context;
import android.util.Log;

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

        // --- Bottom Navigation Gestures (NavStubView & GestureStateMachine) ---
        try {
            Class<?> navStubViewClass = XposedHelpers.findClass("com.miui.home.recents.NavStubView", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(navStubViewClass, "getHotSpaceHeight",
                    new NavStubHook.GetHotSpaceHeightHook());

            XposedHelpers.findAndHookMethod(navStubViewClass, "getWindowParam", int.class,
                    new NavStubHook.GetNavWindowParamHook());

            XposedHelpers.findAndHookMethod(navStubViewClass, "onAttachedToWindow",
                    new NavStubHook.NavAttachedHook());

            XposedHelpers.findAndHookMethod(navStubViewClass, "onDetachedFromWindow",
                    new NavStubHook.NavDetachedHook());

            XposedHelpers.findAndHookMethod(navStubViewClass, "isFastPullUp",
                    new NavStubHook.IsFastPullUpHook());

            XposedHelpers.findAndHookMethod(navStubViewClass, "finishAppToHome", boolean.class,
                    new NavStubHook.FinishAppToHomeHook());

            XposedHelpers.findAndHookMethod(navStubViewClass, "isToAppHoldState",
                    new NavStubHook.IsToAppHoldStateHook());

            XposedHelpers.findAndHookMethod(navStubViewClass, "onTouchEvent", android.view.MotionEvent.class,
                    new NavStubHook.NavTouchHook());

            Log.i(TAG, "Successfully installed bottom gesture & hold duration hooks on NavStubView");
        } catch (Throwable t) {
            Log.e(TAG, "Error installing hooks on NavStubView: ", t);
        }
    }
}
