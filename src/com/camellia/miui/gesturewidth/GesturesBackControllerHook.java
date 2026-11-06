package com.camellia.miui.gesturewidth;

import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class GesturesBackControllerHook extends XC_MethodHook {
    public static final String TAG = GestureWidthSettings.TAG;

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        try {
            if (param.args != null && param.args.length >= 2) {
                int left = (Integer) param.args[0];
                int right = (Integer) param.args[1];

                int targetLeft = GestureWidthSettings.getDesiredWidth(null, 0);
                int targetRight = GestureWidthSettings.getDesiredWidth(null, 1);
                int sw = GestureWidthSettings.getCachedScreenWidth();

                int newLeft = Math.max(left, targetLeft);
                int expectedRight = sw - targetRight;
                int newRight = Math.min(right, expectedRight);

                if (newLeft != left || newRight != right) {
                    param.args[0] = newLeft;
                    param.args[1] = newRight;
                    Log.i(TAG, "GesturesBackController.setGestureEdgeWidth: overridden [" + left + ", " + right + "] -> [" + newLeft + ", " + newRight + "]");
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error in GesturesBackControllerHook: ", t);
        }
    }
}

