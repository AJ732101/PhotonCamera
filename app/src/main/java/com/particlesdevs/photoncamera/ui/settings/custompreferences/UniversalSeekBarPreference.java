package com.particlesdevs.photoncamera.ui.settings.custompreferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import com.particlesdevs.photoncamera.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.control.Vibration;

import java.util.Locale;

/**
 * Created by vibhorSrv on 12/09/2020
 */
public class UniversalSeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "UnivSeekBarPref";
    private static final boolean isLoggingOn = false;
    private Vibration vibration;
    private float mMin = 0.0f;
    private float mMax = 100.0f;
    private boolean isFloat = false;
    private boolean showSeekBarValue = true;
    private float mStepPerUnit = 1.0f;
    private int seekBarProgress;
    private TextView seekBarValue;
    private SeekBar seekBar;
    private String fallback_value;

    public UniversalSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    public UniversalSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    public UniversalSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public UniversalSeekBarPreference(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(
                    attrs, R.styleable.UniversalSeekBarPreference, defStyleAttr, defStyleRes);

            mMax = a.getFloat(R.styleable.UniversalSeekBarPreference_maxValue, 100.0f);
            mMin = a.getFloat(R.styleable.UniversalSeekBarPreference_minValue, 0.0f);
            mStepPerUnit = a.getFloat(R.styleable.UniversalSeekBarPreference_stepPerUnit, 1.0f);
            showSeekBarValue = a.getBoolean(R.styleable.UniversalSeekBarPreference_showSeekBarValue, true);
            isFloat = a.getBoolean(R.styleable.UniversalSeekBarPreference_isFloat, false);
            if (!isFloat && mStepPerUnit > 1)
                mStepPerUnit = 1.0f;
            a.recycle();
        }

        try {
            PhotonCamera pc = PhotonCamera.getInstance(context);
            if (pc != null) {
                vibration = PhotonCamera.getVibration();
            }
        } catch (Throwable ignored) {}
    }

    private void log(String msg) {
        if (isLoggingOn)
            Log.d(TAG + getKey(), msg);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(false);
        seekBar = (SeekBar) holder.findViewById(R.id.seekbar);
        seekBarValue = (TextView) holder.findViewById(R.id.seekbar_value);
        if (seekBar != null) {
            seekBar.setMax((int) ((mMax - mMin) * mStepPerUnit));
            seekBar.setOnSeekBarChangeListener(this);
        }
        set(convertToProgress(fallback_value));
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && vibration != null) {
            try {
                vibration.Tick();
            } catch (Exception ignored) {}
        }
        if (fromUser) {
            set(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        String valStr = (defaultValue != null) ? defaultValue.toString() : fallback_value;
        if (valStr != null) {
            set(convertToProgress(valStr));
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        fallback_value = a.getString(index);
        log("onGetDefaultValue : " + fallback_value);
        return a.getString(index);
    }

    private void set(int progress) {
        seekBarProgress = progress;
        String valueToPersist = convertToValue(progress);
        updateLabel(valueToPersist);
        updateSeekbar(progress);
        try {
            persistString(valueToPersist);
        } catch (Exception ignored) {}
        log("set : " + valueToPersist);
    }

    private void updateLabel(String valueToPersist) {
        if (seekBarValue != null) {
            if (showSeekBarValue) {
                seekBarValue.setVisibility(View.VISIBLE);
                seekBarValue.setText(valueToPersist);
            } else
                seekBarValue.setVisibility(View.GONE);
        }
    }

    private void updateSeekbar(int progress) {
        if (seekBar != null)
            seekBar.setProgress(progress);
    }

    private int convertToProgress(String defValue) {
        try {
            String val = getPersistedString(defValue);
            if (val == null) val = defValue;
            if (val == null) return 0;
            return (int) ((Float.parseFloat(val) - mMin) * mStepPerUnit);
        } catch (Exception e) {
            return 0;
        }
    }

    private String convertToValue(int progress) {
        if (isFloat)
            return String.format(Locale.ROOT, "%.2f", (float) progress / mStepPerUnit + mMin);
        else
            return String.valueOf((int) ((float) progress / mStepPerUnit + mMin));
    }

    public String getValue() {
        return getPersistedString(fallback_value);
    }

    public int getSeekBarProgress() {
        return seekBarProgress;
    }

    public SeekBar getSeekBar() {
        return seekBar;
    }

}
