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

    private TextView tvMainValue;
    private TextView tvMainDp;
    private SeekBar sbMain;

    private Switch switchSeparate;
    private LinearLayout layoutSeparate;
    private TextView tvLeftValue;
    private SeekBar sbLeft;
    private TextView tvRightValue;
    private SeekBar sbRight;

    private Button btnPresetStock;
    private Button btnPresetDefault;
    private Button btnPresetWide;
    private Button btnPresetMax;
    private Button btnReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        tvMainValue = (TextView) findViewById(R.id.tv_main_value);
        tvMainDp = (TextView) findViewById(R.id.tv_main_dp);
        sbMain = (SeekBar) findViewById(R.id.sb_main);

        switchSeparate = (Switch) findViewById(R.id.switch_separate);
        layoutSeparate = (LinearLayout) findViewById(R.id.layout_separate);
        tvLeftValue = (TextView) findViewById(R.id.tv_left_value);
        sbLeft = (SeekBar) findViewById(R.id.sb_left);
        tvRightValue = (TextView) findViewById(R.id.tv_right_value);
        sbRight = (SeekBar) findViewById(R.id.sb_right);

        btnPresetStock = (Button) findViewById(R.id.btn_preset_stock);
        btnPresetDefault = (Button) findViewById(R.id.btn_preset_default);
        btnPresetWide = (Button) findViewById(R.id.btn_preset_wide);
        btnPresetMax = (Button) findViewById(R.id.btn_preset_max);
        btnReset = (Button) findViewById(R.id.btn_reset);

        // Hide permission warning banner since ContentProvider needs no special permissions
        View permWarning = findViewById(R.id.layout_perm_warning);
        if (permWarning != null) {
            permWarning.setVisibility(View.GONE);
        }

        // Configure min and max (50 px to 200 px)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sbMain.setMin(50);
            sbMain.setMax(200);
            sbLeft.setMin(50);
            sbLeft.setMax(200);
            sbRight.setMin(50);
            sbRight.setMax(200);
        } else {
            sbMain.setMax(200);
            sbLeft.setMax(200);
            sbRight.setMax(200);
        }

        btnPresetStock.setOnClickListener(this);
        btnPresetDefault.setOnClickListener(this);
        btnPresetWide.setOnClickListener(this);
        btnPresetMax.setOnClickListener(this);
        btnReset.setOnClickListener(this);

        sbMain.setOnSeekBarChangeListener(this);
        sbLeft.setOnSeekBarChangeListener(this);
        sbRight.setOnSeekBarChangeListener(this);

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
        if (id == R.id.btn_preset_stock) {
            setPresetWidth(54);
        } else if (id == R.id.btn_preset_default) {
            setPresetWidth(GestureWidthSettings.DEFAULT_WIDTH_PX);
        } else if (id == R.id.btn_preset_wide) {
            setPresetWidth(140);
        } else if (id == R.id.btn_preset_max) {
            setPresetWidth(200);
        } else if (id == R.id.btn_reset) {
            switchSeparate.setChecked(false);
            setPresetWidth(GestureWidthSettings.DEFAULT_WIDTH_PX);
            Toast.makeText(this, "Reset to default (108 px)", Toast.LENGTH_SHORT).show();
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

        if (bundle != null) {
            width = bundle.getInt(GestureSettingsProvider.KEY_WIDTH, GestureWidthSettings.DEFAULT_WIDTH_PX);
            left = bundle.getInt(GestureSettingsProvider.KEY_LEFT, -1);
            right = bundle.getInt(GestureSettingsProvider.KEY_RIGHT, -1);
        }

        if (width < 50) width = 50;
        if (width > 200) width = 200;

        sbMain.setProgress(width);
        updateDisplayLabels(width);

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
    }

    private void applyMainWidth(int width) {
        updateDisplayLabels(width);

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

    private void updateDisplayLabels(int width) {
        tvMainValue.setText(width + " px");
        float density = getResources().getDisplayMetrics().density;
        float dp = (float) width / density;
        tvMainDp.setText(String.format(Locale.US, "~%.1f dp (Stock MIUI: 54 px / ~%.1f dp)", dp, 54f / density));
    }

    private Bundle callProvider(String method, Bundle extras) {
        try {
            return getContentResolver().call(GestureSettingsProvider.CONTENT_URI, method, null, extras);
        } catch (Throwable t) {
            return null;
        }
    }
}
