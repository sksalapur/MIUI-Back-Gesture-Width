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

    // Edge Width Controls
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

    // Swipe Distance Controls
    private TextView tvDistanceValue;
    private TextView tvDistanceDp;
    private SeekBar sbDistance;
    private Button btnDistShort;
    private Button btnDistDefault;
    private Button btnDistMedium;
    private Button btnDistStock;

    // Swipe Speed Controls
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

        // Edge Width views
        tvMainValue = (TextView) findViewById(R.id.tv_main_value);
        tvMainDp = (TextView) findViewById(R.id.tv_main_dp);
        sbMain = (SeekBar) findViewById(R.id.sb_main);
        btnPresetStock = (Button) findViewById(R.id.btn_preset_stock);
        btnPresetDefault = (Button) findViewById(R.id.btn_preset_default);
        btnPresetWide = (Button) findViewById(R.id.btn_preset_wide);
        btnPresetMax = (Button) findViewById(R.id.btn_preset_max);

        switchSeparate = (Switch) findViewById(R.id.switch_separate);
        layoutSeparate = (LinearLayout) findViewById(R.id.layout_separate);
        tvLeftValue = (TextView) findViewById(R.id.tv_left_value);
        sbLeft = (SeekBar) findViewById(R.id.sb_left);
        tvRightValue = (TextView) findViewById(R.id.tv_right_value);
        sbRight = (SeekBar) findViewById(R.id.sb_right);

        // Distance views
        tvDistanceValue = (TextView) findViewById(R.id.tv_distance_value);
        tvDistanceDp = (TextView) findViewById(R.id.tv_distance_dp);
        sbDistance = (SeekBar) findViewById(R.id.sb_distance);
        btnDistShort = (Button) findViewById(R.id.btn_dist_short);
        btnDistDefault = (Button) findViewById(R.id.btn_dist_default);
        btnDistMedium = (Button) findViewById(R.id.btn_dist_medium);
        btnDistStock = (Button) findViewById(R.id.btn_dist_stock);

        // Speed views
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
            sbMain.setMax(200);
            sbLeft.setMax(200);
            sbRight.setMax(200);
            sbDistance.setMax(200);
            sbSpeed.setMax(30);
        }

        // Click listeners
        btnPresetStock.setOnClickListener(this);
        btnPresetDefault.setOnClickListener(this);
        btnPresetWide.setOnClickListener(this);
        btnPresetMax.setOnClickListener(this);

        btnDistShort.setOnClickListener(this);
        btnDistDefault.setOnClickListener(this);
        btnDistMedium.setOnClickListener(this);
        btnDistStock.setOnClickListener(this);

        btnSpeedGentle.setOnClickListener(this);
        btnSpeedDefault.setOnClickListener(this);
        btnSpeedFast.setOnClickListener(this);
        btnSpeedStock.setOnClickListener(this);

        btnReset.setOnClickListener(this);

        // SeekBar listeners
        sbMain.setOnSeekBarChangeListener(this);
        sbLeft.setOnSeekBarChangeListener(this);
        sbRight.setOnSeekBarChangeListener(this);
        sbDistance.setOnSeekBarChangeListener(this);
        sbSpeed.setOnSeekBarChangeListener(this);

        // Switch listener
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

        // Edge width presets
        if (id == R.id.btn_preset_stock) {
            setPresetWidth(54);
        } else if (id == R.id.btn_preset_default) {
            setPresetWidth(GestureWidthSettings.DEFAULT_WIDTH_PX);
        } else if (id == R.id.btn_preset_wide) {
            setPresetWidth(140);
        } else if (id == R.id.btn_preset_max) {
            setPresetWidth(200);

        // Distance presets
        } else if (id == R.id.btn_dist_short) {
            setPresetDistance(50);
        } else if (id == R.id.btn_dist_default) {
            setPresetDistance(GestureWidthSettings.DEFAULT_DISTANCE_PX);
        } else if (id == R.id.btn_dist_medium) {
            setPresetDistance(120);
        } else if (id == R.id.btn_dist_stock) {
            setPresetDistance(GestureWidthSettings.STOCK_DISTANCE_PX);

        // Speed presets
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
        if (id == R.id.sb_main) {
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

    private void loadCurrentSettings() {
        Bundle bundle = callProvider("get", null);

        int width = GestureWidthSettings.DEFAULT_WIDTH_PX;
        int left = -1;
        int right = -1;
        int distance = GestureWidthSettings.DEFAULT_DISTANCE_PX;
        int speed = GestureWidthSettings.DEFAULT_SPEED_TENTHS;

        if (bundle != null) {
            width = bundle.getInt(GestureSettingsProvider.KEY_WIDTH, GestureWidthSettings.DEFAULT_WIDTH_PX);
            left = bundle.getInt(GestureSettingsProvider.KEY_LEFT, -1);
            right = bundle.getInt(GestureSettingsProvider.KEY_RIGHT, -1);
            distance = bundle.getInt(GestureSettingsProvider.KEY_DISTANCE, GestureWidthSettings.DEFAULT_DISTANCE_PX);
            speed = bundle.getInt(GestureSettingsProvider.KEY_SPEED, GestureWidthSettings.DEFAULT_SPEED_TENTHS);
        }

        // Bounds checks
        if (width < 50) width = 50;
        if (width > 200) width = 200;
        if (distance < 30) distance = 30;
        if (distance > 200) distance = 200;
        if (speed < 2) speed = 2;
        if (speed > 30) speed = 30;

        // Edge Width
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

        // Distance
        sbDistance.setProgress(distance);
        updateDistanceDisplay(distance);

        // Speed
        sbSpeed.setProgress(speed);
        updateSpeedDisplay(speed);
    }

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

        int pxPerSec = (int) (speed * 1000);
        String label;
        if (speed <= 0.4f) {
            label = "Gentle Flick";
        } else if (speed <= 1.0f) {
            label = "Responsive (Recommended)";
        } else if (speed <= 1.8f) {
            label = "Brisk Flick";
        } else {
            label = "Stock MIUI Fast (>2.0)";
        }
        tvSpeedDesc.setText(String.format(Locale.US, "~%d px/s — %s", pxPerSec, label));
    }

    private void resetAllToDefaults() {
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
