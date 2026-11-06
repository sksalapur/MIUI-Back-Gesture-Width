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

import de.robv.android.xposed.XposedHelpers;

public class GestureWidthSettings {
    public static final String TAG = "MiuiGestureWidth";

    // Side Back Keys
    public static final String SETTING_KEY = "miui_back_gesture_width";
    public static final String SETTING_KEY_LEFT = "miui_back_gesture_width_left";
    public static final String SETTING_KEY_RIGHT = "miui_back_gesture_width_right";
    public static final String SETTING_KEY_DISTANCE = "miui_back_gesture_distance";
    public static final String SETTING_KEY_SPEED = "miui_back_gesture_speed";

    // Custom Launcher & Bottom Navigation Gesture Keys
    public static final String SETTING_KEY_CUSTOM_LAUNCHER = "miui_custom_launcher_gestures";
    public static final String SETTING_KEY_BOTTOM_REACH = "miui_bottom_reach_height";
    public static final String SETTING_KEY_BOTTOM_DISTANCE = "miui_bottom_slide_distance";
    public static final String SETTING_KEY_BOTTOM_SPEED = "miui_bottom_speed_tenths";
    public static final String SETTING_KEY_BOTTOM_HOLD = "miui_bottom_hold_duration_ms";

    // Side Back Defaults & Ranges
    public static final int DEFAULT_WIDTH_PX = 108;
    public static final int MIN_WIDTH_PX = 40;
    public static final int MAX_WIDTH_PX = 250;

    public static final int DEFAULT_DISTANCE_PX = 80;
    public static final int MIN_DISTANCE_PX = 30;
    public static final int MAX_DISTANCE_PX = 200;
    public static final int STOCK_DISTANCE_PX = 180;

    public static final int DEFAULT_SPEED_TENTHS = 8; // 0.8 px/ms
    public static final int MIN_SPEED_TENTHS = 2; // 0.2 px/ms
    public static final int MAX_SPEED_TENTHS = 30; // 3.0 px/ms
    public static final int STOCK_SPEED_TENTHS = 25; // 2.5 px/ms

    // Bottom Gestures Defaults & Ranges
    public static final boolean DEFAULT_CUSTOM_LAUNCHER = true;

    // Bottom Reach / Trigger Area Height: 30 to 200 px (stock is ~45-54 px)
    public static final int DEFAULT_BOTTOM_REACH_PX = 65;
    public static final int MIN_BOTTOM_REACH_PX = 30;
    public static final int MAX_BOTTOM_REACH_PX = 200;
    public static final int STOCK_BOTTOM_REACH_PX = 54;

    // Bottom Slide Distance Threshold: 40 to 300 px
    public static final int DEFAULT_BOTTOM_DISTANCE_PX = 100;
    public static final int MIN_BOTTOM_DISTANCE_PX = 40;
    public static final int MAX_BOTTOM_DISTANCE_PX = 300;
    public static final int STOCK_BOTTOM_DISTANCE_PX = 120;

    // Bottom Speed / Responsiveness: 2 to 30 (0.2 to 3.0 px/ms)
    public static final int DEFAULT_BOTTOM_SPEED_TENTHS = 8; // 0.8 px/ms
    public static final int MIN_BOTTOM_SPEED_TENTHS = 2; // 0.2 px/ms
    public static final int MAX_BOTTOM_SPEED_TENTHS = 30; // 3.0 px/ms
    public static final int STOCK_BOTTOM_SPEED_TENTHS = 20; // 2.0 px/ms

    // Bottom Slide & Hold Duration: 100 to 600 ms (stock is ~250-300 ms)
    public static final int DEFAULT_BOTTOM_HOLD_MS = 250;
    public static final int MIN_BOTTOM_HOLD_MS = 100;
    public static final int MAX_BOTTOM_HOLD_MS = 600;
    public static final int STOCK_BOTTOM_HOLD_MS = 300;

    // Cached values
    private static volatile int sCachedWidth = DEFAULT_WIDTH_PX;
    private static volatile int sCachedLeftWidth = -1;
    private static volatile int sCachedRightWidth = -1;
    private static volatile int sCachedDistance = DEFAULT_DISTANCE_PX;
    private static volatile int sCachedSpeedTenths = DEFAULT_SPEED_TENTHS;
    private static volatile int sCachedScreenWidth = 1080;

    private static volatile boolean sCachedCustomLauncher = DEFAULT_CUSTOM_LAUNCHER;
    private static volatile int sCachedBottomReach = DEFAULT_BOTTOM_REACH_PX;
    private static volatile int sCachedBottomDistance = DEFAULT_BOTTOM_DISTANCE_PX;
    private static volatile int sCachedBottomSpeedTenths = DEFAULT_BOTTOM_SPEED_TENTHS;
    private static volatile int sCachedBottomHoldMs = DEFAULT_BOTTOM_HOLD_MS;

    private static boolean sObserverRegistered = false;
    private static final List<WeakReference<Object>> sActiveStubs = new ArrayList<WeakReference<Object>>();
    private static final List<WeakReference<Object>> sActiveNavStubs = new ArrayList<WeakReference<Object>>();
    private static final Object sLock = new Object();

    // Side Back Getters
    public static int getDesiredWidth(Context context, int pos) {
        if (context != null) updateFromSettings(context);
        if (pos == 0 && sCachedLeftWidth > 0) {
            return sCachedLeftWidth;
        } else if (pos == 1 && sCachedRightWidth > 0) {
            return sCachedRightWidth;
        }
        return sCachedWidth;
    }

    public static int getDesiredDistance() {
        return sCachedDistance;
    }

    public static float getDesiredSpeed() {
        return (float) sCachedSpeedTenths / 10.0f;
    }

    public static int getCachedScreenWidth() {
        return sCachedScreenWidth;
    }

    public static void setCachedScreenWidth(int width) {
        if (width > 0) sCachedScreenWidth = width;
    }

    // Bottom / Custom Launcher Getters
    public static boolean isCustomLauncherGesturesEnabled() {
        return sCachedCustomLauncher;
    }

    public static int getDesiredBottomReach() {
        return sCachedBottomReach;
    }

    public static int getDesiredBottomDistance() {
        return sCachedBottomDistance;
    }

    public static float getDesiredBottomSpeed() {
        return (float) sCachedBottomSpeedTenths / 10.0f;
    }

    public static int getDesiredBottomHold() {
        return sCachedBottomHoldMs;
    }

    public static void ensureObserver(Context context) {
        if (context == null || sObserverRegistered) return;
        synchronized (sLock) {
            if (!sObserverRegistered) {
                registerObserver(context.getApplicationContext());
            }
        }
    }

    // Side Stub Registration
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

    // Bottom NavStub Registration
    public static void registerNavStub(final Object navStub) {
        if (navStub == null) return;
        Context context = null;
        try {
            if (navStub instanceof View) {
                context = ((View) navStub).getContext();
            }
        } catch (Throwable ignored) {}

        synchronized (sLock) {
            boolean alreadyTracked = false;
            for (WeakReference<Object> ref : sActiveNavStubs) {
                if (ref.get() == navStub) {
                    alreadyTracked = true;
                    break;
                }
            }
            if (!alreadyTracked) {
                sActiveNavStubs.add(new WeakReference<Object>(navStub));
            }
            if (!sObserverRegistered && context != null) {
                registerObserver(context.getApplicationContext());
            }
        }
    }

    public static void unregisterNavStub(Object navStub) {
        if (navStub == null) return;
        synchronized (sLock) {
            Iterator<WeakReference<Object>> it = sActiveNavStubs.iterator();
            while (it.hasNext()) {
                Object obj = it.next().get();
                if (obj == null || obj == navStub) {
                    it.remove();
                }
            }
        }
    }

    public static void ensureForceFsgNavBar(Context context) {
        if (context == null || !sCachedCustomLauncher) return;
        try {
            ContentResolver cr = context.getContentResolver();
            int current = Settings.Global.getInt(cr, "force_fsg_nav_bar", 0);
            if (current != 1) {
                Settings.Global.putInt(cr, "force_fsg_nav_bar", 1);
                Log.i(TAG, "Enforced force_fsg_nav_bar = 1 for custom launcher gestures");
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error ensuring force_fsg_nav_bar: ", t);
        }
    }

    public static void updateFromSettings(Context context) {
        if (context == null) return;
        try {
            ContentResolver cr = context.getContentResolver();
            Bundle bundle = null;
            try {
                bundle = cr.call(GestureSettingsProvider.CONTENT_URI, "getAllSettings", null, null);
            } catch (Throwable t) {
                Log.w(TAG, "Could not call GestureSettingsProvider: " + t.getMessage());
            }

            if (bundle != null) {
                sCachedWidth = bundle.getInt(SETTING_KEY, DEFAULT_WIDTH_PX);
                sCachedLeftWidth = bundle.getInt(SETTING_KEY_LEFT, -1);
                sCachedRightWidth = bundle.getInt(SETTING_KEY_RIGHT, -1);
                sCachedDistance = bundle.getInt(SETTING_KEY_DISTANCE, DEFAULT_DISTANCE_PX);
                sCachedSpeedTenths = bundle.getInt(SETTING_KEY_SPEED, DEFAULT_SPEED_TENTHS);

                sCachedCustomLauncher = bundle.getBoolean(SETTING_KEY_CUSTOM_LAUNCHER, DEFAULT_CUSTOM_LAUNCHER);
                sCachedBottomReach = bundle.getInt(SETTING_KEY_BOTTOM_REACH, DEFAULT_BOTTOM_REACH_PX);
                sCachedBottomDistance = bundle.getInt(SETTING_KEY_BOTTOM_DISTANCE, DEFAULT_BOTTOM_DISTANCE_PX);
                sCachedBottomSpeedTenths = bundle.getInt(SETTING_KEY_BOTTOM_SPEED, DEFAULT_BOTTOM_SPEED_TENTHS);
                sCachedBottomHoldMs = bundle.getInt(SETTING_KEY_BOTTOM_HOLD, DEFAULT_BOTTOM_HOLD_MS);

                Log.d(TAG, "Loaded settings from Provider: width=" + sCachedWidth + ", left=" + sCachedLeftWidth
                        + ", right=" + sCachedRightWidth + ", dist=" + sCachedDistance + ", speed=" + sCachedSpeedTenths
                        + ", customLauncher=" + sCachedCustomLauncher + ", bReach=" + sCachedBottomReach
                        + ", bDist=" + sCachedBottomDistance + ", bSpeed=" + sCachedBottomSpeedTenths
                        + ", bHold=" + sCachedBottomHoldMs);
                return;
            }

            // Fallback to Settings.System
            sCachedWidth = Settings.System.getInt(cr, SETTING_KEY, DEFAULT_WIDTH_PX);
            sCachedLeftWidth = Settings.System.getInt(cr, SETTING_KEY_LEFT, -1);
            sCachedRightWidth = Settings.System.getInt(cr, SETTING_KEY_RIGHT, -1);
            sCachedDistance = Settings.System.getInt(cr, SETTING_KEY_DISTANCE, DEFAULT_DISTANCE_PX);
            sCachedSpeedTenths = Settings.System.getInt(cr, SETTING_KEY_SPEED, DEFAULT_SPEED_TENTHS);

            sCachedCustomLauncher = Settings.System.getInt(cr, SETTING_KEY_CUSTOM_LAUNCHER, DEFAULT_CUSTOM_LAUNCHER ? 1 : 0) != 0;
            sCachedBottomReach = Settings.System.getInt(cr, SETTING_KEY_BOTTOM_REACH, DEFAULT_BOTTOM_REACH_PX);
            sCachedBottomDistance = Settings.System.getInt(cr, SETTING_KEY_BOTTOM_DISTANCE, DEFAULT_BOTTOM_DISTANCE_PX);
            sCachedBottomSpeedTenths = Settings.System.getInt(cr, SETTING_KEY_BOTTOM_SPEED, DEFAULT_BOTTOM_SPEED_TENTHS);
            sCachedBottomHoldMs = Settings.System.getInt(cr, SETTING_KEY_BOTTOM_HOLD, DEFAULT_BOTTOM_HOLD_MS);
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
                    Log.i(TAG, "Settings change detected: " + uri);
                    updateFromSettings(context);
                    applyWidthToActiveStubs();
                    applyBottomReachToActiveStubs();
                    ensureForceFsgNavBar(context);
                }
            };

            ContentResolver cr = context.getContentResolver();
            try {
                cr.registerContentObserver(GestureSettingsProvider.CONTENT_URI, true, observer);
            } catch (Throwable t) {
                Log.e(TAG, "Error registering provider observer: ", t);
            }

            // Also observe system force_fsg_nav_bar so we maintain it
            try {
                cr.registerContentObserver(Settings.Global.getUriFor("force_fsg_nav_bar"), false, new ContentObserver(handler) {
                    @Override
                    public void onChange(boolean selfChange) {
                        ensureForceFsgNavBar(context);
                    }
                });
            } catch (Throwable ignored) {}

            sObserverRegistered = true;
            Log.i(TAG, "Successfully registered ContentObserver for all gesture settings");
            updateFromSettings(context);
            ensureForceFsgNavBar(context);
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

    public static void applyBottomReachToActiveStubs() {
        synchronized (sLock) {
            Iterator<WeakReference<Object>> it = sActiveNavStubs.iterator();
            while (it.hasNext()) {
                final Object navStub = it.next().get();
                if (navStub == null) {
                    it.remove();
                    continue;
                }

                if (!(navStub instanceof View)) continue;
                final View navView = (View) navStub;
                Handler h = navView.getHandler();
                Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            int targetReach = getDesiredBottomReach();
                            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) navView.getLayoutParams();
                            if (lp != null && lp.height != targetReach) {
                                lp.height = targetReach;
                                WindowManager wm = (WindowManager) navView.getContext().getSystemService(Context.WINDOW_SERVICE);
                                if (wm != null) {
                                    wm.updateViewLayout(navView, lp);
                                    Log.i(TAG, "Successfully updated NavStubView height live to " + targetReach + " px");
                                }
                            }
                        } catch (Throwable t) {
                            Log.e(TAG, "Error in applyBottomReachToActiveStubs: ", t);
                        }
                    }
                };
                if (h != null) {
                    h.post(task);
                } else {
                    task.run();
                }
            }
        }
    }
}
