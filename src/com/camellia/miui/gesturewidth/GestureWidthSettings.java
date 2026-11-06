package com.camellia.miui.gesturewidth;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class GestureWidthSettings {
    public static final String TAG = "MiuiGestureWidth";
    public static final String SETTING_KEY = "miui_back_gesture_width";
    public static final String SETTING_KEY_LEFT = "miui_back_gesture_width_left";
    public static final String SETTING_KEY_RIGHT = "miui_back_gesture_width_right";

    public static final int DEFAULT_WIDTH_PX = 108; // 2x default (54px -> 108px / ~39.3dp)
    public static final int MIN_WIDTH_PX = 40;
    public static final int MAX_WIDTH_PX = 250;

    private static volatile int sCachedWidth = DEFAULT_WIDTH_PX;
    private static volatile int sCachedLeftWidth = -1;
    private static volatile int sCachedRightWidth = -1;
    private static volatile int sCachedScreenWidth = 1080;

    private static boolean sObserverRegistered = false;
    private static final List<WeakReference<Object>> sActiveStubs = new ArrayList<WeakReference<Object>>();
    private static final Object sLock = new Object();

    public static int getDesiredWidth(Context context, int pos) {
        if (context != null) {
            updateFromSettings(context);
        }

        if (pos == 0 && sCachedLeftWidth > 0) {
            return sCachedLeftWidth;
        } else if (pos == 1 && sCachedRightWidth > 0) {
            return sCachedRightWidth;
        }
        return sCachedWidth;
    }

    public static int getCachedScreenWidth() {
        return sCachedScreenWidth;
    }

    public static void setCachedScreenWidth(int width) {
        if (width > 0) {
            sCachedScreenWidth = width;
        }
    }

    public static void registerStub(final Object stubView) {
        if (stubView == null) return;

        Context context = null;
        try {
            context = (Context) XposedHelpers.getObjectField(stubView, "mContext");
        } catch (Throwable ignored) {}

        synchronized (sLock) {
            boolean alreadyTracked = false;
            for (WeakReference<Object> ref : sActiveStubs) {
                if (ref.get() == stubView) {
                    alreadyTracked = true;
                    break;
                }
            }
            if (!alreadyTracked) {
                sActiveStubs.add(new WeakReference<Object>(stubView));
            }

            if (!sObserverRegistered && context != null) {
                registerObserver(context.getApplicationContext());
            }
        }
    }

    public static void unregisterStub(Object stubView) {
        if (stubView == null) return;
        synchronized (sLock) {
            Iterator<WeakReference<Object>> it = sActiveStubs.iterator();
            while (it.hasNext()) {
                Object obj = it.next().get();
                if (obj == null || obj == stubView) {
                    it.remove();
                }
            }
        }
    }

    public static void updateFromSettings(Context context) {
        if (context == null) return;
        try {
            ContentResolver cr = context.getContentResolver();

            // First check dedicated ContentProvider
            Bundle bundle = null;
            try {
                bundle = cr.call(GestureSettingsProvider.CONTENT_URI, "get", null, null);
            } catch (Throwable t) {
                Log.d(TAG, "Provider call fallback: " + t.getMessage());
            }

            if (bundle != null && bundle.containsKey(GestureSettingsProvider.KEY_WIDTH)) {
                int width = bundle.getInt(GestureSettingsProvider.KEY_WIDTH, DEFAULT_WIDTH_PX);
                if (width < MIN_WIDTH_PX) width = MIN_WIDTH_PX;
                if (width > MAX_WIDTH_PX) width = MAX_WIDTH_PX;
                sCachedWidth = width;

                int left = bundle.getInt(GestureSettingsProvider.KEY_LEFT, -1);
                if (left > 0) {
                    if (left < MIN_WIDTH_PX) left = MIN_WIDTH_PX;
                    if (left > MAX_WIDTH_PX) left = MAX_WIDTH_PX;
                }
                sCachedLeftWidth = left;

                int right = bundle.getInt(GestureSettingsProvider.KEY_RIGHT, -1);
                if (right > 0) {
                    if (right < MIN_WIDTH_PX) right = MIN_WIDTH_PX;
                    if (right > MAX_WIDTH_PX) right = MAX_WIDTH_PX;
                }
                sCachedRightWidth = right;
                return;
            }

            int width = Settings.System.getInt(cr, SETTING_KEY, DEFAULT_WIDTH_PX);
            if (width < MIN_WIDTH_PX) width = MIN_WIDTH_PX;
            if (width > MAX_WIDTH_PX) width = MAX_WIDTH_PX;
            sCachedWidth = width;

            int left = Settings.System.getInt(cr, SETTING_KEY_LEFT, -1);
            if (left > 0) {
                if (left < MIN_WIDTH_PX) left = MIN_WIDTH_PX;
                if (left > MAX_WIDTH_PX) left = MAX_WIDTH_PX;
            }
            sCachedLeftWidth = left;

            int right = Settings.System.getInt(cr, SETTING_KEY_RIGHT, -1);
            if (right > 0) {
                if (right < MIN_WIDTH_PX) right = MIN_WIDTH_PX;
                if (right > MAX_WIDTH_PX) right = MAX_WIDTH_PX;
            }
            sCachedRightWidth = right;
        } catch (Throwable t) {
            Log.e(TAG, "Error reading settings: " + t.getMessage());
        }
    }

    private static void registerObserver(final Context context) {
        if (context == null || sObserverRegistered) return;
        try {
            Handler handler = new Handler(Looper.getMainLooper());
            ContentObserver observer = new ContentObserver(handler) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    super.onChange(selfChange, uri);
                    Log.i(TAG, "Settings change detected for back gesture width: " + uri);
                    updateFromSettings(context);
                    applyWidthToActiveStubs();
                }
            };

            ContentResolver cr = context.getContentResolver();
            try {
                cr.registerContentObserver(GestureSettingsProvider.CONTENT_URI, true, observer);
            } catch (Throwable t) {
                Log.e(TAG, "Error registering provider observer: ", t);
            }
            try {
                cr.registerContentObserver(Settings.System.getUriFor(SETTING_KEY), false, observer);
                cr.registerContentObserver(Settings.System.getUriFor(SETTING_KEY_LEFT), false, observer);
                cr.registerContentObserver(Settings.System.getUriFor(SETTING_KEY_RIGHT), false, observer);
            } catch (Throwable ignored) {}
            sObserverRegistered = true;
            Log.i(TAG, "Successfully registered ContentObserver for gesture width settings");
        } catch (Throwable t) {
            Log.e(TAG, "Error registering ContentObserver: ", t);
        }
    }

    public static void applyWidthToActiveStubs() {
        synchronized (sLock) {
            Iterator<WeakReference<Object>> it = sActiveStubs.iterator();
            while (it.hasNext()) {
                final Object stubView = it.next().get();
                if (stubView == null) {
                    it.remove();
                    continue;
                }

                Handler stubHandler = null;
                try {
                    stubHandler = (Handler) XposedHelpers.getObjectField(stubView, "mHandler");
                } catch (Throwable ignored) {}

                if (stubHandler == null && stubView instanceof View) {
                    stubHandler = ((View) stubView).getHandler();
                }

                Runnable updateTask = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            int pos = XposedHelpers.getIntField(stubView, "mGestureStubPos");
                            Context ctx = (Context) XposedHelpers.getObjectField(stubView, "mContext");
                            int targetWidth = getDesiredWidth(ctx, pos);

                            Log.i(TAG, "Applying updated width " + targetWidth + " px to stub pos=" + pos);
                            XposedHelpers.setIntField(stubView, "mGestureStubDefaultSize", targetWidth);
                            XposedHelpers.setIntField(stubView, "mGestureStubSize", targetWidth);
                            XposedHelpers.callMethod(stubView, "setSize", targetWidth);

                            WindowManager wm = (WindowManager) XposedHelpers.getObjectField(stubView, "mWindowManager");
                            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) XposedHelpers.getObjectField(stubView, "mGestureStubParams");
                            if (wm != null && lp != null) {
                                lp.width = targetWidth;
                                wm.updateViewLayout((View) stubView, lp);
                                Log.i(TAG, "Successfully updated WindowManager layout width to " + targetWidth + " px for pos=" + pos);
                            }
                        } catch (Throwable t) {
                            Log.e(TAG, "Error in stub updateTask: ", t);
                        }
                    }
                };

                if (stubHandler != null) {
                    stubHandler.post(updateTask);
                } else {
                    updateTask.run();
                }
            }
        }
    }
}
