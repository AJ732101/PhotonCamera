package com.particlesdevs.photoncamera.ui.settings.custompreferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Keep;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.particlesdevs.photoncamera.util.FileManager;

import java.util.Arrays;

@Keep
public class RestorePreference extends ListPreference {
    public RestorePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public RestorePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public RestorePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RestorePreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setPersistent(false);
        setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String[] filesNames = FileManager.sPHOTON_DIR.list((dir, name) -> {
                    int index = name.lastIndexOf('.');
                    String ext = -1 == index ? "" : name.substring(index + 1);
                    return ext.equalsIgnoreCase("json");
                });

                filesNames = filesNames != null ? filesNames : new String[0]; //null check

                Arrays.sort(filesNames);
                setEntries(filesNames);
                setEntryValues(filesNames);
                return true;
            }
        });
    }
}
