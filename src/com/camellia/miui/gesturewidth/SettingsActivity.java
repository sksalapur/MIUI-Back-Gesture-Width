package com.camellia.miui.gesturewidth;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class SettingsActivity extends Activity implements
        View.OnClickListener,
        SeekBar.OnSeekBarChangeListener,
        CompoundButton.OnCheckedChangeListener {

    // Custom Launcher Gestures
    private Switch swCustomLauncher;
    private TextView tvCustomLauncherStatus;

    // Bottom Reach
    private TextView tvBottomReachValue;
    private TextView tvBottomReachDp;
    private SeekBar sbBottomReach;
    private Button btnReachStock;
    private Button btnReachDefault;
    private Button btnReachMedium;
    private Button btnReachMax;

    // Bottom Slide Distance
    private TextView tvBottomDistValue;
    private TextView tvBottomDistDp;
    private SeekBar sbBottomDist;
    private Button btnBdistShort;
    private Button btnBdistDefault;
    private Button btnBdistMedium;
    private Button btnBdistStock;

    // Bottom Speed Sensitivity
    private TextView tvBspeedValue;
    private TextView tvBspeedDesc;
    private SeekBar sbBspeed;
    private Button btnBspeedGentle;
    private Button btnBspeedDefault;
    private Button btnBspeedFast;
    private Button btnBspeedStock;

    // Recents Hold Duration
    private TextView tvBholdValue;
    private TextView tvBholdDesc;
    private SeekBar sbBhold;
    private Button btnBholdQuick;
    private Button btnBholdDefault;
    private Button btnBholdRelaxed;
    private Button btnBholdStock;

    // Side Edge Width Controls
    private TextView tvMainValue;
    private TextView tvMainDp;
    private SeekBar sbMain;
    private Button btnPresetStock;
    private Button btnPresetDefault;
    private Button btnPresetWide;
    private Button btnPresetMax;

    private Switch switchSeparate;
    private LinearLayout layoutSeparate;
    private TextView tvLeftValue;
    private SeekBar sbLeft;
    private TextView tvRightValue;
    private SeekBar sbRight;

    // Side Swipe Distance Controls
    private TextView tvDistanceValue;
    private TextView tvDistanceDp;
    private SeekBar sbDistance;
    private Button btnDistShort;
    private Button btnDistDefault;
    private Button btnDistMedium;
    private Button btnDistStock;

    // Side Swipe Speed Controls
    private TextView tvSpeedValue;
    private TextView tvSpeedDesc;
    private SeekBar sbSpeed;
    private Button btnSpeedGentle;
    private Button btnSpeedDefault;
    private Button btnSpeedFast;
    private Button btnSpeedStock;

    // Reset
    private Button btnReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Custom Launcher switch
        swCustomLauncher = (Switch) findViewById(R.id.sw_custom_launcher);
        tvCustomLauncherStatus = (TextView) findViewById(R.id.tv_custom_launcher_status);

        // Bottom Reach
        tvBottomReachValue = (TextView) findViewById(R.id.tv_bottom_reach_value);
        tvBottomReachDp = (TextView) findViewById(R.id.tv_bottom_reach_dp);
        sbBottomReach = (SeekBar) findViewById(R.id.sb_bottom_reach);
        btnReachStock = (Button) findViewById(R.id.btn_reach_stock);
        btnReachDefault = (Button) findViewById(R.id.btn_reach_default);
        btnReachMedium = (Button) findViewById(R.id.btn_reach_medium);
        btnReachMax = (Button) findViewById(R.id.btn_reach_max);

        // Bottom Distance
        tvBottomDistValue = (TextView) findViewById(R.id.tv_bottom_dist_value);
        tvBottomDistDp = (TextView) findViewById(R.id.tv_bottom_dist_dp);
        sbBottomDist = (SeekBar) findViewById(R.id.sb_bottom_dist);
        btnBdistShort = (Button) findViewById(R.id.btn_bdist_short);
        btnBdistDefault = (Button) findViewById(R.id.btn_bdist_default);
        btnBdistMedium = (Button) findViewById(R.id.btn_bdist_medium);
        btnBdistStock = (Button) findViewById(R.id.btn_bdist_stock);

        // Bottom Speed
        tvBspeedValue = (TextView) findViewById(R.id.tv_bspeed_value);
        tvBspeedDesc = (TextView) findViewById(R.id.tv_bspeed_desc);
        sbBspeed = (SeekBar) findViewById(R.id.sb_bspeed);
        btnBspeedGentle = (Button) findViewById(R.id.btn_bspeed_gentle);
        btnBspeedDefault = (Button) findViewById(R.id.btn_bspeed_default);
        btnBspeedFast = (Button) findViewById(R.id.btn_bspeed_fast);
        btnBspeedStock = (Button) findViewById(R.id.btn_bspeed_stock);

        // Bottom Hold
        tvBholdValue = (TextView) findViewById(R.id.tv_bhold_value);
        tvBholdDesc = (TextView) findViewById(R.id.tv_bhold_desc);
        sbBhold = (SeekBar) findViewById(R.id.sb_bhold);
        btnBholdQuick = (Button) findViewById(R.id.btn_bhold_quick);
        btnBholdDefault = (Button) findViewById(R.id.btn_bhold_default);
        btnBholdRelaxed = (Button) findViewById(R.id.btn_bhold_relaxed);
        btnBholdStock = (Button) findViewById(R.id.btn_bhold_stock);

        // Side Edge Width
        tvMainValue = (TextView) findViewById(R.id.tv_main_value);
        tvMainDp = (TextView) findViewById(R.id.tv_main_dp);
        sbMain = (SeekBar) findViewById(R.id.sb_main);
        btnPresetStock = (Button) findViewById(R.id.btn_preset_stock);
        btnPresetDefault = (Button) findViewById(R.id.btn_preset_default);
        btnPresetWide = (Button) findViewById(R.id.btn_preset_wide);
        btnPresetMax = (Button) findViewById(R.id.btn_preset_max);

        switchSeparate = (Switch) findViewById(R.id.sw_separate);
        layoutSeparate = (LinearLayout) findViewById(R.id.layout_separate);
        tvLeftValue = (TextView) findViewById(R.id.tv_left_value);
        sbLeft = (SeekBar) findViewById(R.id.sb_left);
        tvRightValue = (TextView) findViewById(R.id.tv_right_value);
        sbRight = (SeekBar) findViewById(R.id.sb_right);

        // Side Distance
        tvDistanceValue = (TextView) findViewById(R.id.tv_distance_value);
        tvDistanceDp = (TextView) findViewById(R.id.tv_distance_dp);
        sbDistance = (SeekBar) findViewById(R.id.sb_distance);
        btnDistShort = (Button) findViewById(R.id.btn_dist_short);
        btnDistDefault = (Button) findViewById(R.id.btn_dist_default);
        btnDistMedium = (Button) findViewById(R.id.btn_dist_medium);
        btnDistStock = (Button) findViewById(R.id.btn_dist_stock);

        // Side Speed
        tvSpeedValue = (TextView) findViewById(R.id.tv_speed_value);
        tvSpeedDesc = (TextView) findViewById(R.id.tv_speed_desc);
        sbSpeed = (SeekBar) findViewById(R.id.sb_speed);
        btnSpeedGentle = (Button) findViewById(R.id.btn_speed_gentle);
        btnSpeedDefault = (Button) findViewById(R.id.btn_speed_default);
        btnSpeedFast = (Button) findViewById(R.id.btn_speed_fast);
        btnSpeedStock = (Button) findViewById(R.id.btn_speed_stock);

        // Reset
        btnReset = (Button) findViewById(R.id.btn_reset);

        // Configure min and max ranges
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sbBottomReach.setMin(30);
            sbBottomReach.setMax(180);

            sbBottomDist.setMin(40);
            sbBottomDist.setMax(250);

            sbBspeed.setMin(2);
            sbBspeed.setMax(30);

            sbBhold.setMin(100);
            sbBhold.setMax(600);

            sbMain.setMin(50);
            sbMain.setMax(200);
            sbLeft.setMin(50);
            sbLeft.setMax(200);
            sbRight.setMin(50);
            sbRight.setMax(200);

            sbDistance.setMin(30);
            sbDistance.setMax(200);

            sbSpeed.setMin(2);
            sbSpeed.setMax(30);
        } else {
            sbBottomReach.setMax(180);
            sbBottomDist.setMax(250);
            sbBspeed.setMax(30);
            sbBhold.setMax(600);

            sbMain.setMax(200);
            sbLeft.setMax(200);
            sbRight.setMax(200);
            sbDistance.setMax(200);
            sbSpeed.setMax(30);
        }

        // Click listeners - Bottom Reach
        btnReachStock.setOnClickListener(this);
        btnReachDefault.setOnClickListener(this);
        btnReachMedium.setOnClickListener(this);
        btnReachMax.setOnClickListener(this);

        // Click listeners - Bottom Distance
        btnBdistShort.setOnClickListener(this);
        btnBdistDefault.setOnClickListener(this);
        btnBdistMedium.setOnClickListener(this);
        btnBdistStock.setOnClickListener(this);

        // Click listeners - Bottom Speed
        btnBspeedGentle.setOnClickListener(this);
        btnBspeedDefault.setOnClickListener(this);
        btnBspeedFast.setOnClickListener(this);
        btnBspeedStock.setOnClickListener(this);

        // Click listeners - Bottom Hold
        btnBholdQuick.setOnClickListener(this);
        btnBholdDefault.setOnClickListener(this);
        btnBholdRelaxed.setOnClickListener(this);
        btnBholdStock.setOnClickListener(this);

        // Click listeners - Side Width
        btnPresetStock.setOnClickListener(this);
        btnPresetDefault.setOnClickListener(this);
        btnPresetWide.setOnClickListener(this);
        btnPresetMax.setOnClickListener(this);

        // Click listeners - Side Distance
        btnDistShort.setOnClickListener(this);
        btnDistDefault.setOnClickListener(this);
        btnDistMedium.setOnClickListener(this);
        btnDistStock.setOnClickListener(this);

        // Click listeners - Side Speed
        btnSpeedGentle.setOnClickListener(this);
        btnSpeedDefault.setOnClickListener(this);
        btnSpeedFast.setOnClickListener(this);
        btnSpeedStock.setOnClickListener(this);

        // Reset
        btnReset.setOnClickListener(this);

        // SeekBar listeners
        sbBottomReach.setOnSeekBarChangeListener(this);
        sbBottomDist.setOnSeekBarChangeListener(this);
        sbBspeed.setOnSeekBarChangeListener(this);
        sbBhold.setOnSeekBarChangeListener(this);

        sbMain.setOnSeekBarChangeListener(this);
        sbLeft.setOnSeekBarChangeListener(this);
        sbRight.setOnSeekBarChangeListener(this);
        sbDistance.setOnSeekBarChangeListener(this);
        sbSpeed.setOnSeekBarChangeListener(this);

        // Switch listeners
        swCustomLauncher.setOnCheckedChangeListener(this);
        switchSeparate.setOnCheckedChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCurrentSettings();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        // Bottom Reach presets
        if (id == R.id.btn_reach_stock) {
            setPresetBottomReach(GestureWidthSettings.STOCK_BOTTOM_REACH_PX);
        } else if (id == R.id.btn_reach_default) {
            setPresetBottomReach(GestureWidthSettings.DEFAULT_BOTTOM_REACH_PX);
        } else if (id == R.id.btn_reach_medium) {
            setPresetBottomReach(90);
        } else if (id == R.id.btn_reach_max) {
            setPresetBottomReach(130);

        // Bottom Distance presets
        } else if (id == R.id.btn_bdist_short) {
            setPresetBottomDistance(60);
        } else if (id == R.id.btn_bdist_default) {
            setPresetBottomDistance(GestureWidthSettings.DEFAULT_BOTTOM_DISTANCE_PX);
        } else if (id == R.id.btn_bdist_medium) {
            setPresetBottomDistance(150);
        } else if (id == R.id.btn_bdist_stock) {
            setPresetBottomDistance(GestureWidthSettings.STOCK_BOTTOM_DISTANCE_PX);

        // Bottom Speed presets
        } else if (id == R.id.btn_bspeed_gentle) {
            setPresetBottomSpeed(4);
        } else if (id == R.id.btn_bspeed_default) {
            setPresetBottomSpeed(GestureWidthSettings.DEFAULT_BOTTOM_SPEED_TENTHS);
        } else if (id == R.id.btn_bspeed_fast) {
            setPresetBottomSpeed(15);
        } else if (id == R.id.btn_bspeed_stock) {
            setPresetBottomSpeed(GestureWidthSettings.STOCK_BOTTOM_SPEED_TENTHS);

        // Bottom Hold presets
        } else if (id == R.id.btn_bhold_quick) {
            setPresetBottomHold(150);
        } else if (id == R.id.btn_bhold_default) {
            setPresetBottomHold(GestureWidthSettings.DEFAULT_BOTTOM_HOLD_MS);
        } else if (id == R.id.btn_bhold_relaxed) {
            setPresetBottomHold(350);
        } else if (id == R.id.btn_bhold_stock) {
            setPresetBottomHold(GestureWidthSettings.STOCK_BOTTOM_HOLD_MS);

        // Side Width presets
        } else if (id == R.id.btn_preset_stock) {
            setPresetWidth(54);
        } else if (id == R.id.btn_preset_default) {
            setPresetWidth(GestureWidthSettings.DEFAULT_WIDTH_PX);
        } else if (id == R.id.btn_preset_wide) {
            setPresetWidth(140);
        } else if (id == R.id.btn_preset_max) {
            setPresetWidth(200);

        // Side Distance presets
        } else if (id == R.id.btn_dist_short) {
            setPresetDistance(50);
        } else if (id == R.id.btn_dist_default) {
            setPresetDistance(GestureWidthSettings.DEFAULT_DISTANCE_PX);
        } else if (id == R.id.btn_dist_medium) {
            setPresetDistance(120);
        } else if (id == R.id.btn_dist_stock) {
            setPresetDistance(GestureWidthSettings.STOCK_DISTANCE_PX);

        // Side Speed presets
        } else if (id == R.id.btn_speed_gentle) {
            setPresetSpeed(4);
        } else if (id == R.id.btn_speed_default) {
            setPresetSpeed(GestureWidthSettings.DEFAULT_SPEED_TENTHS);
        } else if (id == R.id.btn_speed_fast) {
            setPresetSpeed(15);
        } else if (id == R.id.btn_speed_stock) {
            setPresetSpeed(GestureWidthSettings.STOCK_SPEED_TENTHS);

        // Global Reset
        } else if (id == R.id.btn_reset) {
            resetAllToDefaults();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) return;

        int id = seekBar.getId();
        if (id == R.id.sb_bottom_reach) {
            applyBottomReach(progress);
        } else if (id == R.id.sb_bottom_dist) {
            applyBottomDistance(progress);
        } else if (id == R.id.sb_bspeed) {
            applyBottomSpeed(progress);
        } else if (id == R.id.sb_bhold) {
            applyBottomHold(progress);
        } else if (id == R.id.sb_main) {
            applyMainWidth(progress);
        } else if (id == R.id.sb_left) {
            tvLeftValue.setText(progress + " px");
            Bundle extras = new Bundle();
            extras.putInt(GestureSettingsProvider.KEY_LEFT, progress);
            callProvider("set", extras);
        } else if (id == R.id.sb_right) {
            tvRightValue.setText(progress + " px");
            Bundle extras = new Bundle();
            extras.putInt(GestureSettingsProvider.KEY_RIGHT, progress);
            callProvider("set", extras);
        } else if (id == R.id.sb_distance) {
            applyDistance(progress);
        } else if (id == R.id.sb_speed) {
            applySpeed(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        if (id == R.id.sw_custom_launcher) {
            Bundle extras = new Bundle();
            extras.putBoolean(GestureSettingsProvider.KEY_CUSTOM_LAUNCHER, isChecked);
            callProvider("set", extras);
            updateCustomLauncherDisplay(isChecked);
        } else if (id == R.id.sw_separate) {
            layoutSeparate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            Bundle extras = new Bundle();
            if (isChecked) {
                extras.putInt(GestureSettingsProvider.KEY_LEFT, sbLeft.getProgress());
                extras.putInt(GestureSettingsProvider.KEY_RIGHT, sbRight.getProgress());
            } else {
                extras.putInt(GestureSettingsProvider.KEY_LEFT, -1);
                extras.putInt(GestureSettingsProvider.KEY_RIGHT, -1);
                applyMainWidth(sbMain.getProgress());
            }
            callProvider("set", extras);
        }
    }

    private void loadCurrentSettings() {
        Bundle bundle = callProvider("getAllSettings", null);
        if (bundle == null) bundle = callProvider("get", null);

        boolean customLauncher = GestureWidthSettings.DEFAULT_CUSTOM_LAUNCHER;
        int bottomReach = GestureWidthSettings.DEFAULT_BOTTOM_REACH_PX;
        int bottomDist = GestureWidthSettings.DEFAULT_BOTTOM_DISTANCE_PX;
        int bottomSpeed = GestureWidthSettings.DEFAULT_BOTTOM_SPEED_TENTHS;
        int bottomHold = GestureWidthSettings.DEFAULT_BOTTOM_HOLD_MS;

        int width = GestureWidthSettings.DEFAULT_WIDTH_PX;
        int left = -1;
        int right = -1;
        int distance = GestureWidthSettings.DEFAULT_DISTANCE_PX;
        int speed = GestureWidthSettings.DEFAULT_SPEED_TENTHS;

        if (bundle != null) {
            customLauncher = bundle.getBoolean(GestureSettingsProvider.KEY_CUSTOM_LAUNCHER, GestureWidthSettings.DEFAULT_CUSTOM_LAUNCHER);
            bottomReach = bundle.getInt(GestureSettingsProvider.KEY_BOTTOM_REACH, GestureWidthSettings.DEFAULT_BOTTOM_REACH_PX);
            bottomDist = bundle.getInt(GestureSettingsProvider.KEY_BOTTOM_DISTANCE, GestureWidthSettings.DEFAULT_BOTTOM_DISTANCE_PX);
            bottomSpeed = bundle.getInt(GestureSettingsProvider.KEY_BOTTOM_SPEED, GestureWidthSettings.DEFAULT_BOTTOM_SPEED_TENTHS);
            bottomHold = bundle.getInt(GestureSettingsProvider.KEY_BOTTOM_HOLD, GestureWidthSettings.DEFAULT_BOTTOM_HOLD_MS);

            width = bundle.getInt(GestureSettingsProvider.KEY_WIDTH, GestureWidthSettings.DEFAULT_WIDTH_PX);
            left = bundle.getInt(GestureSettingsProvider.KEY_LEFT, -1);
            right = bundle.getInt(GestureSettingsProvider.KEY_RIGHT, -1);
            distance = bundle.getInt(GestureSettingsProvider.KEY_DISTANCE, GestureWidthSettings.DEFAULT_DISTANCE_PX);
            speed = bundle.getInt(GestureSettingsProvider.KEY_SPEED, GestureWidthSettings.DEFAULT_SPEED_TENTHS);
        }

        // Custom Launcher
        swCustomLauncher.setChecked(customLauncher);
        updateCustomLauncherDisplay(customLauncher);

        // Bottom Reach
        if (bottomReach < 30) bottomReach = 30;
        if (bottomReach > 180) bottomReach = 180;
        sbBottomReach.setProgress(bottomReach);
        updateBottomReachDisplay(bottomReach);

        // Bottom Distance
        if (bottomDist < 40) bottomDist = 40;
        if (bottomDist > 250) bottomDist = 250;
        sbBottomDist.setProgress(bottomDist);
        updateBottomDistDisplay(bottomDist);

        // Bottom Speed
        if (bottomSpeed < 2) bottomSpeed = 2;
        if (bottomSpeed > 30) bottomSpeed = 30;
        sbBspeed.setProgress(bottomSpeed);
        updateBottomSpeedDisplay(bottomSpeed);

        // Bottom Hold
        if (bottomHold < 100) bottomHold = 100;
        if (bottomHold > 600) bottomHold = 600;
        sbBhold.setProgress(bottomHold);
        updateBottomHoldDisplay(bottomHold);

        // Side Width
        if (width < 50) width = 50;
        if (width > 200) width = 200;
        sbMain.setProgress(width);
        updateWidthDisplay(width);

        boolean isSeparate = (left > 0 || right > 0);
        if (left <= 0) left = width;
        if (right <= 0) right = width;
        if (left < 50) left = 50;
        if (left > 200) left = 200;
        if (right < 50) right = 50;
        if (right > 200) right = 200;

        sbLeft.setProgress(left);
        tvLeftValue.setText(left + " px");
        sbRight.setProgress(right);
        tvRightValue.setText(right + " px");

        switchSeparate.setChecked(isSeparate);
        layoutSeparate.setVisibility(isSeparate ? View.VISIBLE : View.GONE);

        // Side Distance
        if (distance < 30) distance = 30;
        if (distance > 200) distance = 200;
        sbDistance.setProgress(distance);
        updateDistanceDisplay(distance);

        // Side Speed
        if (speed < 2) speed = 2;
        if (speed > 30) speed = 30;
        sbSpeed.setProgress(speed);
        updateSpeedDisplay(speed);
    }

    private void updateCustomLauncherDisplay(boolean enabled) {
        if (enabled) {
            tvCustomLauncherStatus.setText("\u2713 Full-screen gestures active on custom launchers; Navigation bar hidden");
            tvCustomLauncherStatus.setTextColor(0xFF34C759);
        } else {
            tvCustomLauncherStatus.setText("\u2717 Gestures on custom launchers disabled; 3-button mode active");
            tvCustomLauncherStatus.setTextColor(0xFF888888);
        }
    }

    // Bottom Reach
    private void applyBottomReach(int reach) {
        updateBottomReachDisplay(reach);
        Bundle extras = new Bundle();
        extras.putInt(GestureSettingsProvider.KEY_BOTTOM_REACH, reach);
        callProvider("set", extras);
    }

    private void setPresetBottomReach(int reach) {
        sbBottomReach.setProgress(reach);
        applyBottomReach(reach);
    }

    private void updateBottomReachDisplay(int reach) {
        tvBottomReachValue.setText(reach + " px");
        float density = getResources().getDisplayMetrics().density;
        float dp = (float) reach / density;
        tvBottomReachDp.setText(String.format(Locale.US, "~%.1f dp trigger zone (Stock: 54 px / ~%.1f dp)", dp, 54f / density));
    }

    // Bottom Distance
    private void applyBottomDistance(int dist) {
        updateBottomDistDisplay(dist);
        Bundle extras = new Bundle();
        extras.putInt(GestureSettingsProvider.KEY_BOTTOM_DISTANCE, dist);
        callProvider("set", extras);
    }

    private void setPresetBottomDistance(int dist) {
        sbBottomDist.setProgress(dist);
        applyBottomDistance(dist);
    }

    private void updateBottomDistDisplay(int dist) {
        tvBottomDistValue.setText(dist + " px");
        float density = getResources().getDisplayMetrics().density;
        float dp = (float) dist / density;
        tvBottomDistDp.setText(String.format(Locale.US, "~%.1f dp upward slide for Home (Stock: 120 px / ~%.1f dp)", dp, 120f / density));
    }

    // Bottom Speed
    private void applyBottomSpeed(int speedTenths) {
        updateBottomSpeedDisplay(speedTenths);
        Bundle extras = new Bundle();
        extras.putInt(GestureSettingsProvider.KEY_BOTTOM_SPEED, speedTenths);
        callProvider("set", extras);
    }

    private void setPresetBottomSpeed(int speedTenths) {
        sbBspeed.setProgress(speedTenths);
        applyBottomSpeed(speedTenths);
    }

    private void updateBottomSpeedDisplay(int speedTenths) {
        float speed = (float) speedTenths / 10.0f;
        tvBspeedValue.setText(String.format(Locale.US, "%.1f px/ms", speed));
        String label = (speed <= 0.4f) ? "Gentle Flick" : (speed <= 1.0f) ? "Responsive (Recommended)" : (speed <= 1.8f) ? "Brisk Flick" : "Stock Fast";
        tvBspeedDesc.setText(String.format(Locale.US, "~%d px/s — %s", (int) (speed * 1000), label));
    }

    // Bottom Hold
    private void applyBottomHold(int holdMs) {
        updateBottomHoldDisplay(holdMs);
        Bundle extras = new Bundle();
        extras.putInt(GestureSettingsProvider.KEY_BOTTOM_HOLD, holdMs);
        callProvider("set", extras);
    }

    private void setPresetBottomHold(int holdMs) {
        sbBhold.setProgress(holdMs);
        applyBottomHold(holdMs);
    }

    private void updateBottomHoldDisplay(int holdMs) {
        tvBholdValue.setText(holdMs + " ms");
        String label = (holdMs <= 180) ? "Quick Trigger" : (holdMs <= 280) ? "Balanced (Recommended)" : (holdMs <= 380) ? "Relaxed" : "Firm Hold";
        tvBholdDesc.setText(String.format(Locale.US, "Hold for %d ms to open Recents — %s", holdMs, label));
    }

    // Side Width
    private void applyMainWidth(int width) {
        updateWidthDisplay(width);
        Bundle extras = new Bundle();
        extras.putInt(GestureSettingsProvider.KEY_WIDTH, width);

        if (!switchSeparate.isChecked()) {
            sbLeft.setProgress(width);
            tvLeftValue.setText(width + " px");
            sbRight.setProgress(width);
            tvRightValue.setText(width + " px");
            extras.putInt(GestureSettingsProvider.KEY_LEFT, -1);
            extras.putInt(GestureSettingsProvider.KEY_RIGHT, -1);
        }
        callProvider("set", extras);
    }

    private void setPresetWidth(int width) {
        sbMain.setProgress(width);
        applyMainWidth(width);
        if (switchSeparate.isChecked()) {
            sbLeft.setProgress(width);
            tvLeftValue.setText(width + " px");
            sbRight.setProgress(width);
            tvRightValue.setText(width + " px");
            Bundle extras = new Bundle();
            extras.putInt(GestureSettingsProvider.KEY_LEFT, width);
            extras.putInt(GestureSettingsProvider.KEY_RIGHT, width);
            callProvider("set", extras);
        }
    }

    private void updateWidthDisplay(int width) {
        tvMainValue.setText(width + " px");
        float density = getResources().getDisplayMetrics().density;
        float dp = (float) width / density;
        tvMainDp.setText(String.format(Locale.US, "~%.1f dp (Stock MIUI: 54 px / ~%.1f dp)", dp, 54f / density));
    }

    // Side Distance
    private void applyDistance(int distance) {
        updateDistanceDisplay(distance);
        Bundle extras = new Bundle();
        extras.putInt(GestureSettingsProvider.KEY_DISTANCE, distance);
        callProvider("set", extras);
    }

    private void setPresetDistance(int distance) {
        sbDistance.setProgress(distance);
        applyDistance(distance);
    }

    private void updateDistanceDisplay(int distance) {
        tvDistanceValue.setText(distance + " px");
        float density = getResources().getDisplayMetrics().density;
        float dp = (float) distance / density;
        tvDistanceDp.setText(String.format(Locale.US, "~%.1f dp (Stock MIUI: 180 px / ~%.1f dp)", dp, 180f / density));
    }

    // Side Speed
    private void applySpeed(int speedTenths) {
        updateSpeedDisplay(speedTenths);
        Bundle extras = new Bundle();
        extras.putInt(GestureSettingsProvider.KEY_SPEED, speedTenths);
        callProvider("set", extras);
    }

    private void setPresetSpeed(int speedTenths) {
        sbSpeed.setProgress(speedTenths);
        applySpeed(speedTenths);
    }

    private void updateSpeedDisplay(int speedTenths) {
        float speed = (float) speedTenths / 10.0f;
        tvSpeedValue.setText(String.format(Locale.US, "%.1f px/ms", speed));
        String label = (speed <= 0.4f) ? "Gentle Flick" : (speed <= 1.0f) ? "Responsive (Recommended)" : (speed <= 1.8f) ? "Brisk Flick" : "Stock MIUI Fast (>2.0)";
        tvSpeedDesc.setText(String.format(Locale.US, "~%d px/s — %s", (int) (speed * 1000), label));
    }

    private void resetAllToDefaults() {
        swCustomLauncher.setChecked(GestureWidthSettings.DEFAULT_CUSTOM_LAUNCHER);
        setPresetBottomReach(GestureWidthSettings.DEFAULT_BOTTOM_REACH_PX);
        setPresetBottomDistance(GestureWidthSettings.DEFAULT_BOTTOM_DISTANCE_PX);
        setPresetBottomSpeed(GestureWidthSettings.DEFAULT_BOTTOM_SPEED_TENTHS);
        setPresetBottomHold(GestureWidthSettings.DEFAULT_BOTTOM_HOLD_MS);

        switchSeparate.setChecked(false);
        setPresetWidth(GestureWidthSettings.DEFAULT_WIDTH_PX);
        setPresetDistance(GestureWidthSettings.DEFAULT_DISTANCE_PX);
        setPresetSpeed(GestureWidthSettings.DEFAULT_SPEED_TENTHS);

        Toast.makeText(this, "Reset all settings to defaults", Toast.LENGTH_SHORT).show();
    }

    private Bundle callProvider(String method, Bundle extras) {
        try {
            return getContentResolver().call(GestureSettingsProvider.CONTENT_URI, method, null, extras);
        } catch (Throwable t) {
            return null;
        }
    }
}
