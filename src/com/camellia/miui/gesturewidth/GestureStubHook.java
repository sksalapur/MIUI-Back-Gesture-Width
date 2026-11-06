package com.camellia.miui.gesturewidth;

import android.content.Context;
import android.util.Log;
import android.view.WindowManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class GestureStubHook {
    public static final String TAG = GestureWidthSettings.TAG;

    public static class InitScreenSizeHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                int pos = XposedHelpers.getIntField(param.thisObject, "mGestureStubPos");
                Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                int targetWidth = GestureWidthSettings.getDesiredWidth(context, pos);

                int sw = XposedHelpers.getIntField(param.thisObject, "mScreenWidth");
                GestureWidthSettings.setCachedScreenWidth(sw);

                XposedHelpers.setIntField(param.thisObject, "mGestureStubDefaultSize", targetWidth);
                XposedHelpers.setIntField(param.thisObject, "mGestureStubSize", targetWidth);

                Log.i(TAG, "initScreenSizeAndDensity: stub pos=" + pos + ", set target width=" + targetWidth + "px");
            } catch (Throwable t) {
                Log.e(TAG, "Error in InitScreenSizeHook: ", t);
            }
        }
    }

    public static class SetSizeHook extends XC_MethodHook {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (param.args != null && param.args.length > 0 && param.args[0] instanceof Integer) {
                    int originalSize = (Integer) param.args[0];
                    if (originalSize > 0) {
                        int pos = XposedHelpers.getIntField(param.thisObject, "mGestureStubPos");
                        Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        int targetWidth = GestureWidthSettings.getDesiredWidth(context, pos);
                        if (originalSize != targetWidth) {
                            param.args[0] = targetWidth;
                            Log.i(TAG, "setSize: override " + originalSize + " -> " + targetWidth + " px (pos=" + pos + ")");
                        }
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in SetSizeHook: ", t);
            }
        }
    }

    public static class GetWindowParamHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                WindowManager.LayoutParams lp = (WindowManager.LayoutParams) param.getResult();
                // Check if magnification overlay (side gesture stub)
                if (lp != null && lp.type == 2027) {
                    int pos = XposedHelpers.getIntField(param.thisObject, "mGestureStubPos");
                    Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    int targetWidth = GestureWidthSettings.getDesiredWidth(context, pos);
                    if (lp.width != targetWidth) {
                        Log.i(TAG, "getGestureStubWindowParam: override lp.width from " + lp.width + " to " + targetWidth);
                        lp.width = targetWidth;
                        param.setResult(lp);
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in GetWindowParamHook: ", t);
            }
        }
    }

    public static class InitGestureEdgeSizeHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                int[] edges = (int[]) param.getResult();
                if (edges != null && edges.length >= 2) {
                    Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    int sw = XposedHelpers.getIntField(param.thisObject, "mScreenWidth");
                    if (sw <= 0) sw = GestureWidthSettings.getCachedScreenWidth();

                    int leftWidth = GestureWidthSettings.getDesiredWidth(context, 0);
                    int rightWidth = GestureWidthSettings.getDesiredWidth(context, 1);

                    edges[0] = leftWidth;
                    edges[1] = sw - rightWidth;
                    param.setResult(edges);

                    Log.i(TAG, "initGestureEdgeSize: edges set to [" + edges[0] + ", " + edges[1] + "] (sw=" + sw + ")");
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in InitGestureEdgeSizeHook: ", t);
            }
        }
    }

    public static class AttachedToWindowHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                GestureWidthSettings.registerStub(param.thisObject);
                int pos = XposedHelpers.getIntField(param.thisObject, "mGestureStubPos");
                Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                int targetWidth = GestureWidthSettings.getDesiredWidth(context, pos);

                XposedHelpers.setIntField(param.thisObject, "mGestureStubDefaultSize", targetWidth);
                XposedHelpers.setIntField(param.thisObject, "mGestureStubSize", targetWidth);
                XposedHelpers.callMethod(param.thisObject, "setSize", targetWidth);

                Log.i(TAG, "onAttachedToWindow: registered stub pos=" + pos + " and enforced width=" + targetWidth);
            } catch (Throwable t) {
                Log.e(TAG, "Error in AttachedToWindowHook: ", t);
            }
        }
    }

    public static class DetachedFromWindowHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                GestureWidthSettings.unregisterStub(param.thisObject);
            } catch (Throwable t) {
                Log.e(TAG, "Error in DetachedFromWindowHook: ", t);
            }
        }
    }
}

