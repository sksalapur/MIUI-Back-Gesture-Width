package com.camellia.miui.gesturewidth;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

public class GestureSettingsProvider extends ContentProvider {
    public static final String AUTHORITY = "com.camellia.miui.gesturewidth.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/settings");

    public static final String PREF_NAME = "gesture_config";

    // Side Back Keys
    public static final String KEY_WIDTH = "width";
    public static final String KEY_LEFT = "left";
    public static final String KEY_RIGHT = "right";
    public static final String KEY_DISTANCE = "distance";
    public static final String KEY_SPEED = "speed";

    // Custom Launcher & Bottom Navigation Keys
    public static final String KEY_CUSTOM_LAUNCHER = "custom_launcher";
    public static final String KEY_BOTTOM_REACH = "bottom_reach";
    public static final String KEY_BOTTOM_DISTANCE = "bottom_distance";
    public static final String KEY_BOTTOM_SPEED = "bottom_speed";
    public static final String KEY_BOTTOM_HOLD = "bottom_hold";

    private SharedPreferences mPrefs;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context != null) {
            mPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            // Default enforce force_fsg_nav_bar = 1 if custom launcher enabled
            boolean customLauncher = mPrefs.getBoolean(KEY_CUSTOM_LAUNCHER, GestureWidthSettings.DEFAULT_CUSTOM_LAUNCHER);
            if (customLauncher) {
                try {
                    Settings.Global.putInt(context.getContentResolver(), "force_fsg_nav_bar", 1);
                } catch (Throwable ignored) {}
            }
        }
        return true;
    }

    private Bundle buildSettingsBundle() {
        Bundle res = new Bundle();
        if (mPrefs != null) {
            int w = mPrefs.getInt(KEY_WIDTH, GestureWidthSettings.DEFAULT_WIDTH_PX);
            int l = mPrefs.getInt(KEY_LEFT, -1);
            int r = mPrefs.getInt(KEY_RIGHT, -1);
            int d = mPrefs.getInt(KEY_DISTANCE, GestureWidthSettings.DEFAULT_DISTANCE_PX);
            int s = mPrefs.getInt(KEY_SPEED, GestureWidthSettings.DEFAULT_SPEED_TENTHS);

            boolean cl = mPrefs.getBoolean(KEY_CUSTOM_LAUNCHER, GestureWidthSettings.DEFAULT_CUSTOM_LAUNCHER);
            int br = mPrefs.getInt(KEY_BOTTOM_REACH, GestureWidthSettings.DEFAULT_BOTTOM_REACH_PX);
            int bd = mPrefs.getInt(KEY_BOTTOM_DISTANCE, GestureWidthSettings.DEFAULT_BOTTOM_DISTANCE_PX);
            int bs = mPrefs.getInt(KEY_BOTTOM_SPEED, GestureWidthSettings.DEFAULT_BOTTOM_SPEED_TENTHS);
            int bh = mPrefs.getInt(KEY_BOTTOM_HOLD, GestureWidthSettings.DEFAULT_BOTTOM_HOLD_MS);

            // Short keys
            res.putInt(KEY_WIDTH, w);
            res.putInt(KEY_LEFT, l);
            res.putInt(KEY_RIGHT, r);
            res.putInt(KEY_DISTANCE, d);
            res.putInt(KEY_SPEED, s);

            res.putBoolean(KEY_CUSTOM_LAUNCHER, cl);
            res.putInt(KEY_BOTTOM_REACH, br);
            res.putInt(KEY_BOTTOM_DISTANCE, bd);
            res.putInt(KEY_BOTTOM_SPEED, bs);
            res.putInt(KEY_BOTTOM_HOLD, bh);

            // Full Setting keys for compatibility
            res.putInt(GestureWidthSettings.SETTING_KEY, w);
            res.putInt(GestureWidthSettings.SETTING_KEY_LEFT, l);
            res.putInt(GestureWidthSettings.SETTING_KEY_RIGHT, r);
            res.putInt(GestureWidthSettings.SETTING_KEY_DISTANCE, d);
            res.putInt(GestureWidthSettings.SETTING_KEY_SPEED, s);

            res.putBoolean(GestureWidthSettings.SETTING_KEY_CUSTOM_LAUNCHER, cl);
            res.putInt(GestureWidthSettings.SETTING_KEY_BOTTOM_REACH, br);
            res.putInt(GestureWidthSettings.SETTING_KEY_BOTTOM_DISTANCE, bd);
            res.putInt(GestureWidthSettings.SETTING_KEY_BOTTOM_SPEED, bs);
            res.putInt(GestureWidthSettings.SETTING_KEY_BOTTOM_HOLD, bh);
        } else {
            res.putInt(KEY_WIDTH, GestureWidthSettings.DEFAULT_WIDTH_PX);
            res.putInt(KEY_LEFT, -1);
            res.putInt(KEY_RIGHT, -1);
            res.putInt(KEY_DISTANCE, GestureWidthSettings.DEFAULT_DISTANCE_PX);
            res.putInt(KEY_SPEED, GestureWidthSettings.DEFAULT_SPEED_TENTHS);

            res.putBoolean(KEY_CUSTOM_LAUNCHER, GestureWidthSettings.DEFAULT_CUSTOM_LAUNCHER);
            res.putInt(KEY_BOTTOM_REACH, GestureWidthSettings.DEFAULT_BOTTOM_REACH_PX);
            res.putInt(KEY_BOTTOM_DISTANCE, GestureWidthSettings.DEFAULT_BOTTOM_DISTANCE_PX);
            res.putInt(KEY_BOTTOM_SPEED, GestureWidthSettings.DEFAULT_BOTTOM_SPEED_TENTHS);
            res.putInt(KEY_BOTTOM_HOLD, GestureWidthSettings.DEFAULT_BOTTOM_HOLD_MS);

            res.putInt(GestureWidthSettings.SETTING_KEY, GestureWidthSettings.DEFAULT_WIDTH_PX);
            res.putInt(GestureWidthSettings.SETTING_KEY_LEFT, -1);
            res.putInt(GestureWidthSettings.SETTING_KEY_RIGHT, -1);
            res.putInt(GestureWidthSettings.SETTING_KEY_DISTANCE, GestureWidthSettings.DEFAULT_DISTANCE_PX);
            res.putInt(GestureWidthSettings.SETTING_KEY_SPEED, GestureWidthSettings.DEFAULT_SPEED_TENTHS);

            res.putBoolean(GestureWidthSettings.SETTING_KEY_CUSTOM_LAUNCHER, GestureWidthSettings.DEFAULT_CUSTOM_LAUNCHER);
            res.putInt(GestureWidthSettings.SETTING_KEY_BOTTOM_REACH, GestureWidthSettings.DEFAULT_BOTTOM_REACH_PX);
            res.putInt(GestureWidthSettings.SETTING_KEY_BOTTOM_DISTANCE, GestureWidthSettings.DEFAULT_BOTTOM_DISTANCE_PX);
            res.putInt(GestureWidthSettings.SETTING_KEY_BOTTOM_SPEED, GestureWidthSettings.DEFAULT_BOTTOM_SPEED_TENTHS);
            res.putInt(GestureWidthSettings.SETTING_KEY_BOTTOM_HOLD, GestureWidthSettings.DEFAULT_BOTTOM_HOLD_MS);
        }
        return res;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (mPrefs == null && getContext() != null) {
            mPrefs = getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }

        if ("get".equals(method) || "getAllSettings".equals(method)) {
            return buildSettingsBundle();
        } else if ("set".equals(method) && extras != null && mPrefs != null) {
            SharedPreferences.Editor editor = mPrefs.edit();

            if (extras.containsKey(KEY_WIDTH)) {
                editor.putInt(KEY_WIDTH, extras.getInt(KEY_WIDTH));
            }
            if (extras.containsKey(KEY_LEFT)) {
                editor.putInt(KEY_LEFT, extras.getInt(KEY_LEFT));
            }
            if (extras.containsKey(KEY_RIGHT)) {
                editor.putInt(KEY_RIGHT, extras.getInt(KEY_RIGHT));
            }
            if (extras.containsKey(KEY_DISTANCE)) {
                editor.putInt(KEY_DISTANCE, extras.getInt(KEY_DISTANCE));
            }
            if (extras.containsKey(KEY_SPEED)) {
                editor.putInt(KEY_SPEED, extras.getInt(KEY_SPEED));
            }

            if (extras.containsKey(KEY_CUSTOM_LAUNCHER)) {
                boolean cl = extras.getBoolean(KEY_CUSTOM_LAUNCHER);
                editor.putBoolean(KEY_CUSTOM_LAUNCHER, cl);
                Context ctx = getContext();
                if (ctx != null) {
                    try {
                        Settings.Global.putInt(ctx.getContentResolver(), "force_fsg_nav_bar", cl ? 1 : 0);
                        Log.i(GestureWidthSettings.TAG, "Toggled force_fsg_nav_bar to " + (cl ? 1 : 0));
                    } catch (Throwable t) {
                        Log.e(GestureWidthSettings.TAG, "Error updating force_fsg_nav_bar: ", t);
                    }
                }
            }
            if (extras.containsKey(KEY_BOTTOM_REACH)) {
                editor.putInt(KEY_BOTTOM_REACH, extras.getInt(KEY_BOTTOM_REACH));
            }
            if (extras.containsKey(KEY_BOTTOM_DISTANCE)) {
                editor.putInt(KEY_BOTTOM_DISTANCE, extras.getInt(KEY_BOTTOM_DISTANCE));
            }
            if (extras.containsKey(KEY_BOTTOM_SPEED)) {
                editor.putInt(KEY_BOTTOM_SPEED, extras.getInt(KEY_BOTTOM_SPEED));
            }
            if (extras.containsKey(KEY_BOTTOM_HOLD)) {
                editor.putInt(KEY_BOTTOM_HOLD, extras.getInt(KEY_BOTTOM_HOLD));
            }

            editor.apply();

            Context context = getContext();
            if (context != null) {
                context.getContentResolver().notifyChange(CONTENT_URI, null);
            }
            Bundle res = new Bundle();
            res.putBoolean("success", true);
            return res;
        }
        return new Bundle();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.item/gesture_setting";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
