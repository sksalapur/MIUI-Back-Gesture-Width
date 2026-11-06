package com.camellia.miui.gesturewidth;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;

public class GesturesBackControllerHook {
    public static final String TAG = GestureWidthSettings.TAG;

    private static volatile long sCurrentDurationMs = -1;

    public static class SetGestureEdgeWidthHook extends XC_MethodHook {
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
                Log.e(TAG, "Error in SetGestureEdgeWidthHook: ", t);
            }
        }
    }

    public static class ProcessPointerEventHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                MotionEvent event = (MotionEvent) param.args[0];
                if (event != null) {
                    long dt = event.getEventTime() - event.getDownTime();
                    sCurrentDurationMs = (dt > 0) ? dt : 1;
                }

                if (param.args.length > 1 && param.args[1] != null) {
                    Object stubView = param.args[1];
                    if (stubView instanceof View) {
                        Context ctx = ((View) stubView).getContext();
                        GestureWidthSettings.ensureObserver(ctx);
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in ProcessPointerEventHook before: ", t);
            }
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            sCurrentDurationMs = -1;
        }
    }

    public static class IsFinishedHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (param.args == null || param.args.length < 2) return;

                float offsetX = (Float) param.args[0];
                int stockSpeed = (Integer) param.args[1];

                if (offsetX <= 0.0f) {
                    param.setResult(false);
                    return;
                }

                int targetDistance = GestureWidthSettings.getDesiredDistance();
                float targetSpeed = GestureWidthSettings.getDesiredSpeed();

                long dt = sCurrentDurationMs;
                float speedPxPerMs = (dt > 0) ? (offsetX / (float) dt) : (float) stockSpeed;

                // Deadzone to prevent micro-movements/accidental touches from triggering
                float minFlickDistance = 30.0f;

                boolean finished = false;
                if (offsetX >= targetDistance) {
                    // Attribute 1: Inward swipe distance satisfied
                    finished = true;
                } else if (offsetX >= minFlickDistance && speedPxPerMs >= targetSpeed) {
                    // Attribute 2: Flick speed sensitivity satisfied
                    finished = true;
                } else if (stockSpeed > 2 && offsetX >= minFlickDistance) {
                    // Stock MIUI speed fallback
                    finished = true;
                }

                param.setResult(finished);
            } catch (Throwable t) {
                Log.e(TAG, "Error in IsFinishedHook: ", t);
            }
        }
    }
}
