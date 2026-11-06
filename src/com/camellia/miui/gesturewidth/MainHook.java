package com.camellia.miui.gesturewidth;

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

        Log.i(TAG, "Initializing MIUI Gesture Width hook in " + lpparam.packageName);
        XposedBridge.log(TAG + ": Initializing MIUI Gesture Width hook in " + lpparam.packageName);

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
            XposedBridge.log(TAG + " Error installing hooks on GestureStubView: " + t);
        }

        try {
            Class<?> gesturesBackControllerClass = XposedHelpers.findClass("com.miui.home.recents.GesturesBackController", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(gesturesBackControllerClass, "setGestureEdgeWidth", int.class, int.class,
                    new GesturesBackControllerHook());

            Log.i(TAG, "Successfully installed hooks on GesturesBackController");
        } catch (Throwable t) {
            Log.e(TAG, "Error installing hooks on GesturesBackController: ", t);
            XposedBridge.log(TAG + " Error installing hooks on GesturesBackController: " + t);
        }
    }
}

