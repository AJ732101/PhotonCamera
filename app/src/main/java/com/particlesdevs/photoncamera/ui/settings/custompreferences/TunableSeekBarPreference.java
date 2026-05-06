package com.particlesdevs.photoncamera.ui.settings.custompreferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.control.Vibration;
import com.particlesdevs.photoncamera.util.Log;

import java.util.Locale;

/**
 * Seekbar preference that can be created programmatically.
 * Uses native float/int storage instead of strings for perfect precision.
 */
public class TunableSeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "TunableSeekBarPref";
    private final Vibration vibration;
    private float mMin = 0.0f;
    private float mMax = 100.0f;
    private boolean isFloat = false;
    private float mStepPerUnit = 1.0f;
    private float mDefaultValue = 0.0f;
    private int seekBarProgress;
    private TextView seekBarValue;
    private SeekBar seekBar;
    private boolean isUserInteraction = false;

    public TunableSeekBarPreference(Context context) {
        super(context);
        vibration = PhotonCamera.getVibration();
        setLayoutResource(R.layout.preference_tunable_seekbar); // Use custom wider layout
        setIconSpaceReserved(false); // Don't reserve icon space
    }

    public void setMinValue(float min) {
        this.mMin = min;
    }

    public void setMaxValue(float max) {
        this.mMax = max;
    }

    public void setIsFloat(boolean isFloat) {
        this.isFloat = isFloat;
    }

    public void setStepPerUnit(float stepPerUnit) {
        this.mStepPerUnit = stepPerUnit;
        if (!isFloat && mStepPerUnit > 1)
            mStepPerUnit = 1.0f;
    }
    
    public void setDefaultValue(float defaultValue) {
        this.mDefaultValue = defaultValue;
        Log.d(TAG, "Set default value: " + defaultValue);
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
            
            // Get persisted value as appropriate type
            float currentValue;
            if (isFloat) {
                currentValue = getPersistedFloat(mDefaultValue);
            } else {
                currentValue = (float) getPersistedInt((int) mDefaultValue);
            }
            
            // Update UI only - don't persist again!
            seekBarProgress = valueToProgress(currentValue);
            String displayValue = formatValue(currentValue);
            if (seekBarValue != null) {
                seekBarValue.setText(displayValue);
                updateValueColor(currentValue);
            }
            seekBar.setProgress(seekBarProgress);
            
            // Add click listener for precise value input
            View seekBarContainer = holder.itemView.findViewById(R.id.seekbar);
            if (seekBarContainer != null) {
                seekBarContainer.setOnClickListener(v -> showPreciseValueDialog());
            }
        }
        
        // Also make the value text clickable
        if (seekBarValue != null) {
            seekBarValue.setOnClickListener(v -> showPreciseValueDialog());
        }
        
        // After binding is complete, any changes are user interactions
        isUserInteraction = true;
    }
    
    private void showPreciseValueDialog() {
        Context context = getContext();
        if (context == null) return;
        
        // Get current value as native type for full precision
        float currentValue = getFloatValue();
        String currentValueText = isFloat ? 
            String.format(Locale.ROOT, "%.10f", currentValue).replaceAll("0+$", "").replaceAll("\\.$", "") :
            String.valueOf((int) currentValue);
        
        // Create input dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getTitle());
        builder.setMessage("Enter precise value (" + 
            String.format(Locale.ROOT, isFloat ? "%.10f" : "%.0f", mMin).replaceAll("0+$", "").replaceAll("\\.$", "") + " - " +
            String.format(Locale.ROOT, isFloat ? "%.10f" : "%.0f", mMax).replaceAll("0+$", "").replaceAll("\\.$", "") + 
            ")\nDefault: " + 
            String.format(Locale.ROOT, isFloat ? "%.10f" : "%.0f", mDefaultValue).replaceAll("0+$", "").replaceAll("\\.$", ""));
        
        // Create input field
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | 
            (isFloat ? InputType.TYPE_NUMBER_FLAG_DECIMAL : 0) | 
            InputType.TYPE_NUMBER_FLAG_SIGNED);
        
        input.setText(currentValueText);
        input.setSelectAllOnFocus(true);
        
        // Add padding
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(50, 20, 50, 20);
        input.setLayoutParams(lp);
        
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.addView(input);
        builder.setView(container);
        
        builder.setPositiveButton("Set", (dialog, which) -> {
            try {
                String valueStr = input.getText().toString();
                float value = Float.parseFloat(valueStr);
                
                // Clamp to min/max
                if (value < mMin) {
                    value = mMin;
                    PhotonCamera.showToast("Value clamped to minimum: " + mMin);
                } else if (value > mMax) {
                    value = mMax;
                    PhotonCamera.showToast("Value clamped to maximum: " + mMax);
                }
                
                // Just set the value - it will be persisted as native type
                int progress = valueToProgress(value);
                set(progress);
                
                Log.d(TAG, "Set precise value: " + value + " for " + getKey());
            } catch (NumberFormatException e) {
                PhotonCamera.showToast("Invalid number format");
                Log.w(TAG, "Invalid input: " + input.getText().toString());
            }
        });
        
        builder.setNeutralButton("Reset", (dialog, which) -> {
            // Temporarily disable user interaction flag to prevent re-persistence during UI updates
            boolean wasUserInteraction = isUserInteraction;
            isUserInteraction = false;
            
            // Remove the persisted value to use annotation default
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            if (prefs != null) {
                prefs.edit().remove(getKey()).apply();
            }
            
            // Update UI to match default
            seekBarProgress = valueToProgress(mDefaultValue);
            String displayValue = formatValue(mDefaultValue);
            if (seekBarValue != null) {
                seekBarValue.setText(displayValue);
            }
            if (seekBar != null) {
                seekBar.setProgress(seekBarProgress);
            }
            updateValueColor(mDefaultValue);
            
            // Restore user interaction flag
            isUserInteraction = wasUserInteraction;
            
            PhotonCamera.showToast("Reset to default: " + mDefaultValue);
            Log.d(TAG, "Reset to default (removed persisted value): " + mDefaultValue + " for " + getKey());
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Request keyboard
        input.requestFocus();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(fromUser && vibration != null) vibration.Tick();
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
        // Check if value already persisted
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        boolean hasPersisted = prefs != null && prefs.contains(getKey());
        
        float currentValue;
        if (!hasPersisted) {
            // First time - use default but DON'T persist it yet
            // This allows default changes in @Tunable annotations to take effect
            // Value will only be persisted when user actually changes it
            currentValue = mDefaultValue;
            Log.d(TAG, "First init - using default (NOT persisting yet): " + mDefaultValue);
        } else {
            // Load existing persisted value
            if (isFloat) {
                currentValue = getPersistedFloat(mDefaultValue);
            } else {
                currentValue = (float) getPersistedInt((int) mDefaultValue);
            }
            Log.d(TAG, "Loading persisted: " + currentValue);
        }
        
        // Update UI without re-persisting
        seekBarProgress = valueToProgress(currentValue);
        if (seekBarValue != null) {
            String displayValue = formatValue(currentValue);
            seekBarValue.setText(displayValue);
            updateValueColor(currentValue);
        }
        if (seekBar != null) {
            seekBar.setProgress(seekBarProgress);
        }
    }

    private void set(int progress) {
        seekBarProgress = progress;
        float value = progressToValue(progress);
        String displayValue = formatValue(value);
        
        updateLabel(displayValue);
        updateSeekbar(progress);
        
        // Only modify persistence during user interactions, not during initialization
        if (!isUserInteraction) {
            // During initialization - don't change persistence
            Log.d(TAG, "Init - not modifying persistence for " + getKey());
            updateValueColor(value);
            return;
        }
        
        // User is actively changing the value
        boolean matchesDefault = Math.abs(value - mDefaultValue) < 0.0001f;
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        
        if (matchesDefault) {
            // User explicitly set it to match current default - remove persistence
            // This makes it white (not customized)
            if (prefs != null && prefs.contains(getKey())) {
                prefs.edit().remove(getKey()).apply();
                Log.d(TAG, "User set to default - removed persistence: " + value + " for " + getKey());
            }
        } else {
            // User set it to differ from current default - persist it
            // This makes it green (customized) and it will stay green even if default changes later
            if (isFloat) {
                persistFloat(value);
            } else {
                persistInt((int) value);
            }
            Log.d(TAG, "User set to non-default - persisted: " + value + " (default: " + mDefaultValue + ") for " + getKey());
        }
        
        // Update color based on value
        updateValueColor(value);
    }

    private void updateLabel(String displayValue) {
        if (seekBarValue != null) {
            seekBarValue.setVisibility(View.VISIBLE);
            seekBarValue.setText(displayValue);
        }
    }
    
    private void updateValueColor(float currentValue) {
        if (seekBarValue == null) return;
        
        // Check if there's a persisted value (user set it to non-default at some point)
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        boolean hasPersisted = prefs != null && prefs.contains(getKey());
        
        // Green = persisted (user customized), White = not persisted (default)
        if (hasPersisted) {
            seekBarValue.setTextColor(Color.parseColor("#4CAF50")); // Material Green
        } else {
            seekBarValue.setTextColor(Color.parseColor("#FFFFFF")); // White
        }
        
        Log.d(TAG, "Color: current=" + currentValue + ", default=" + mDefaultValue + 
            ", persisted=" + hasPersisted + ", color=" + (hasPersisted ? "GREEN" : "WHITE"));
    }

    private void updateSeekbar(int progress) {
        if (seekBar != null)
            seekBar.setProgress(progress);
    }

    private int valueToProgress(float value) {
        return (int) ((value - mMin) * mStepPerUnit);
    }
    
    private float progressToValue(int progress) {
        return (float) progress / mStepPerUnit + mMin;
    }

    private String formatValue(float value) {
        if (isFloat) {
            // For very small values < 0.01, use ellipsis format for display
            if (Math.abs(value) < 0.01f && Math.abs(value) > 0.0f) {
                String fullValue = String.format(Locale.ROOT, "%.10f", value);
                
                // Find position of first non-zero digit after decimal
                int firstNonZero = -1;
                boolean afterDecimal = false;
                for (int i = 0; i < fullValue.length(); i++) {
                    char c = fullValue.charAt(i);
                    if (c == '.') {
                        afterDecimal = true;
                        continue;
                    }
                    if (afterDecimal && c != '0') {
                        firstNonZero = i;
                        break;
                    }
                }
                
                if (firstNonZero > 4) {
                    // Show as "0.00...digits" format
                    String lastDigits = fullValue.substring(firstNonZero, Math.min(firstNonZero + 2, fullValue.length()));
                    return "0.00..." + lastDigits;
                }
            }
            
            return String.format(Locale.ROOT, "%.2f", value);
        } else {
            return String.valueOf((int) value);
        }
    }

    public float getFloatValue() {
        return isFloat ? getPersistedFloat(mDefaultValue) : (float) getPersistedInt((int) mDefaultValue);
    }
}

