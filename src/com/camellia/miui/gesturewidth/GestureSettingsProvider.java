package com.camellia.miui.gesturewidth;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

public class GestureSettingsProvider extends ContentProvider {
    public static final String AUTHORITY = "com.camellia.miui.gesturewidth.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/settings");

    public static final String PREF_NAME = "gesture_config";
    public static final String KEY_WIDTH = "width";
    public static final String KEY_LEFT = "left";
    public static final String KEY_RIGHT = "right";

    private SharedPreferences mPrefs;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context != null) {
            mPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (mPrefs == null && getContext() != null) {
            mPrefs = getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }

        Bundle res = new Bundle();
        if ("get".equals(method)) {
            if (mPrefs != null) {
                res.putInt(KEY_WIDTH, mPrefs.getInt(KEY_WIDTH, GestureWidthSettings.DEFAULT_WIDTH_PX));
                res.putInt(KEY_LEFT, mPrefs.getInt(KEY_LEFT, -1));
                res.putInt(KEY_RIGHT, mPrefs.getInt(KEY_RIGHT, -1));
            } else {
                res.putInt(KEY_WIDTH, GestureWidthSettings.DEFAULT_WIDTH_PX);
                res.putInt(KEY_LEFT, -1);
                res.putInt(KEY_RIGHT, -1);
            }
            return res;
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
            editor.apply();

            Context context = getContext();
            if (context != null) {
                context.getContentResolver().notifyChange(CONTENT_URI, null);
            }
            res.putBoolean("success", true);
            return res;
        }
        return res;
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

