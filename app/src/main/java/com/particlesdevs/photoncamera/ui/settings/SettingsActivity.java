package com.particlesdevs.photoncamera.ui.settings;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.google.android.material.snackbar.Snackbar;
import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.app.base.BaseActivity;
import com.particlesdevs.photoncamera.pro.SupportedDevice;
import com.particlesdevs.photoncamera.settings.BackupRestoreUtil;
import com.particlesdevs.photoncamera.settings.PreferenceKeys;
import com.particlesdevs.photoncamera.settings.SettingsManager;
import com.particlesdevs.photoncamera.ui.SplashActivity;
import com.particlesdevs.photoncamera.ui.settings.custompreferences.ResetPreferences;
import com.particlesdevs.photoncamera.util.log.FragmentLifeCycleMonitor;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import androidx.preference.ListPreference;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.particlesdevs.photoncamera.util.FileManager;

import static com.particlesdevs.photoncamera.settings.PreferenceKeys.Key.ALL_DEVICES_NAMES_KEY;
import static com.particlesdevs.photoncamera.settings.PreferenceKeys.SCOPE_GLOBAL;

public class SettingsActivity extends BaseActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
        PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    public static boolean toRestartApp;

    public static class GeneralSettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.general_preferences, rootKey);
        }
    }

    public static class SoCSettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.soc_preferences, rootKey);
        }
    }

    public static class VideoSettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.video_preferences, rootKey);
        }
    }

    public static class AudioSettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.audio_preferences, rootKey);
        }
    }

    public static class StackingSettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.stacking_preferences, rootKey);

            ListPreference lutPreference = findPreference(getString(R.string.pref_lut_key));

            if (lutPreference != null) {
                List<CharSequence> entries = new ArrayList<>();
                List<CharSequence> entryValues = new ArrayList<>();

                if (lutPreference.getEntries() != null) {
                    Collections.addAll(entries, lutPreference.getEntries());
                    Collections.addAll(entryValues, lutPreference.getEntryValues());
                }

                // --- HIER IST DIE KORREKTUR ---
                // FileManager.sPHOTON_TUNING_DIR ist bereits ein File-Objekt.
                File tuningDir = FileManager.sPHOTON_TUNING_DIR;
                // --- ENDE DER KORREKTUR ---

                if (tuningDir.exists() && tuningDir.isDirectory()) {
                    File[] files = tuningDir.listFiles((dir, name) -> name.toLowerCase().endsWith("_lut.png"));

                    if (files != null) {
                        for (File file : files) {
                            String fileName = file.getName();
                            if (!entryValues.contains(fileName)) {
                                entries.add(fileName);

                                entryValues.add(fileName);
                            }
                        }
                    }
                }

                lutPreference.setEntries(entries.toArray(new CharSequence[0]));
                lutPreference.setEntryValues(entryValues.toArray(new CharSequence[0]));

                String currentValue = lutPreference.getValue();
                if (currentValue == null || !entryValues.contains(currentValue)) {
                    if (!entryValues.isEmpty()) {
                        lutPreference.setValueIndex(0);
                    }
                }
            }
        }
    }

    public static class SingleShotSettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.single_shot_preferences, rootKey);
        }
    }

    public static class SensorAndMoreSettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.sensor_and_more_preferences, rootKey);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // --- COLD START FIX ---
        if (PhotonCamera.getInstance(this) == null) {
            Intent intent = new Intent(this, SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return; // Stop further execution
        }
        // --- END OF FIX ---

        getDelegate().setLocalNightMode(PreferenceKeys.getThemeValue());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentLifeCycleMonitor(), true);

    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, @NonNull Preference pref) {
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment()
        );
        fragment.setArguments(pref.getExtras());
        fragment.setTargetFragment(caller, 0);

        // Ersetze das aktuelle Fragment durch das neue.
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_container, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }

    public void back(View view) {
        onBackPressed();
    }
    @Override
    public boolean onPreferenceStartScreen(@NonNull PreferenceFragmentCompat preferenceFragmentCompat,
                                           PreferenceScreen preferenceScreen) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.animate_slide_left_enter, R.anim.animate_slide_left_exit
                        , R.anim.animate_card_enter, R.anim.animate_slide_right_exit);
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.getKey());
        fragment.setArguments(args);
        ft.replace(R.id.settings_container, fragment, preferenceScreen.getKey());
        ft.addToBackStack(preferenceScreen.getKey());
        ft.commit();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (toRestartApp) {
            PhotonCamera.restartApp(this);
        }
        super.onBackPressed();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, PreferenceManager.OnPreferenceTreeClickListener {
        private static final String KEY_MAIN_PARENT_SCREEN = "prefscreen";
        private Activity activity;
        private SettingsManager mSettingsManager;
        private Context mContext;
        private View mRootView;
        private SupportedDevice supportedDevice;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            Preference generalSettingsButton = findPreference("general_settings_screen");
            if (generalSettingsButton != null) {
                generalSettingsButton.setOnPreferenceClickListener(preference -> {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.settings_container, new GeneralSettingsFragment())
                            .addToBackStack(null)
                            .commit();
                    return true;
                });
            }

            Preference socSettingsButton = findPreference("soc_settings_screen");
            if (socSettingsButton != null) {
                socSettingsButton.setOnPreferenceClickListener(preference -> {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.settings_container, new SoCSettingsFragment())
                            .addToBackStack(null)
                            .commit();
                    return true;
                });
            }

            Preference videoSettingsButton = findPreference("video_settings_screen");
            if (videoSettingsButton != null) {
                videoSettingsButton.setOnPreferenceClickListener(preference -> {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.settings_container, new VideoSettingsFragment())
                            .addToBackStack(null)
                            .commit();
                    return true;
                });
            }

            Preference audioSettingsButton = findPreference("audio_settings_screen");
            if (audioSettingsButton != null) {
                audioSettingsButton.setOnPreferenceClickListener(preference -> {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.settings_container, new AudioSettingsFragment())
                            .addToBackStack(null)
                            .commit();
                    return true;
                });
            }

            Preference stackingSettingsButton = findPreference("stacking_settings_screen");
            if (stackingSettingsButton != null) {
                stackingSettingsButton.setOnPreferenceClickListener(preference -> {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.settings_container, new StackingSettingsFragment())
                            .addToBackStack(null)
                            .commit();
                    return true;
                });
            }

            Preference singleShotSettingsButton = findPreference("single_shot_settings_screen");
            if (singleShotSettingsButton != null) {
                singleShotSettingsButton.setOnPreferenceClickListener(preference -> {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.settings_container, new SingleShotSettingsFragment())
                            .addToBackStack(null)
                            .commit();
                    return true;
                });
            }

            Preference sensorAndMoreSettingsButton = findPreference("sensor_and_more_settings_screen");
            if (sensorAndMoreSettingsButton != null) {
                sensorAndMoreSettingsButton.setOnPreferenceClickListener(preference -> {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.settings_container, new SensorAndMoreSettingsFragment())
                            .addToBackStack(null)
                            .commit();
                    return true;
                });
            }
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            activity = getActivity();
            mContext = getContext();
            mSettingsManager = Objects.requireNonNull(PhotonCamera.getInstance(activity)).getSettingsManager();
            supportedDevice = Objects.requireNonNull(PhotonCamera.getInstance(activity)).getSupportedDevice();
            Objects.requireNonNull(getPreferenceScreen().getSharedPreferences())
                    .registerOnSharedPreferenceChangeListener(this);
            showHideHdrxSettings();
            setFramesSummary();
            setVersionDetails();
            setHdrxTitle();
            checkEszdTheme();
            setTelegramPref();
            setGithubPref();
            setBackupPref();
            setRestorePref();
            setSupportedDevices();
            setProTitle();
            setThisDevice();
        }

        private void showHideHdrxSettings() {
            if (PreferenceKeys.isHdrXOn())
                removePreferenceFromScreen(mContext.getString(R.string.pref_category_jpg_key));
            else
                removePreferenceFromScreen(mContext.getString(R.string.pref_category_hdrx_key));
        }

        @NonNull
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            if (container != null) container.removeAllViews();
            return super.onCreateView(inflater, container, savedInstanceState);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mRootView = view;
            setupToolbar();
        }

        private void setupToolbar() {
            if (activity != null) {
                Toolbar toolbar = activity.findViewById(R.id.settings_toolbar);
                if (toolbar != null) {
                    toolbar.setTitle(getPreferenceScreen().getTitle());
                }
            }
        }

        @Override
        public void onResume() {
            super.onResume();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            getParentFragmentManager().beginTransaction().remove(SettingsFragment.this).commitAllowingStateLoss();
        }

        private void setTelegramPref() {
            activity.runOnUiThread(()-> {
                Preference myPref = findPreference(PreferenceKeys.Key.KEY_TELEGRAM.mValue);
                if (myPref != null)
                    myPref.setOnPreferenceClickListener(preference -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/photon_camera_channel"));
                        startActivity(browserIntent);
                        return true;
                    });
            });
        }

        private void setGithubPref() {
            activity.runOnUiThread(()-> {
            Preference github = findPreference(PreferenceKeys.Key.KEY_CONTRIBUTORS.mValue);
            if (github != null)
                github.setOnPreferenceClickListener(preference -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/eszdman/PhotonCamera"));
                    startActivity(browserIntent);
                    return true;
                });
            });
        }

        private void setRestorePref() {
                activity.runOnUiThread(()-> {
            Preference restorePref = findPreference(mContext.getString(R.string.pref_restore_preferences_key));
            if (restorePref != null) {
                restorePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    String restoreResult = BackupRestoreUtil.restorePreferences(mContext, newValue.toString());
                    Snackbar.make(mRootView, restoreResult, Snackbar.LENGTH_LONG).show();
                    return true;
                });
            }
          });
        }

        private void setBackupPref() {
            activity.runOnUiThread(()-> {
                Preference backupPref = findPreference(mContext.getString(R.string.pref_backup_preferences_key));
                if (backupPref != null) {
                    backupPref.setOnPreferenceChangeListener((preference, newValue) -> {
                        String backupResult = BackupRestoreUtil.backupSettings(mContext, newValue.toString());
                        Snackbar.make(mRootView, backupResult, Snackbar.LENGTH_LONG).show();
                        return true;
                    });
                }
           });
        }
        private void setSupportedDevices() {
            activity.runOnUiThread(()-> {
                Preference preference = findPreference(PreferenceKeys.Key.ALL_DEVICES_NAMES_KEY.mValue);
                if (preference != null) {
                    preference.setSummary((mSettingsManager.getStringSet(PreferenceKeys.Key.DEVICES_PREFERENCE_FILE_NAME.mValue,
                            ALL_DEVICES_NAMES_KEY, Collections.singleton(mContext.getString(R.string.list_not_loaded)))
                            .stream().sorted().map(s -> s + "\n").reduce("\n", String::concat)));
                }
           });
        }

        private void setProTitle() {
            activity.runOnUiThread(()-> {
                    Preference preference = findPreference(mContext.getString(R.string.pref_about_key));
                    if (preference != null && supportedDevice.isSupportedDevice()) {
                        preference.setTitle(R.string.device_support);
                    }
            });
        }

        private void setThisDevice() {
            Preference preference = findPreference(mContext.getString(R.string.pref_this_device_key));
            if (preference != null) {
                preference.setSummary(mContext.getString(R.string.this_device, SupportedDevice.THIS_DEVICE));
            }
        }

        private void removePreferenceFromScreen(String preferenceKey) {
            PreferenceScreen parentScreen = findPreference(SettingsFragment.KEY_MAIN_PARENT_SCREEN);
            if (parentScreen != null)
                if (parentScreen.findPreference(preferenceKey) != null) {
                    parentScreen.removePreference(Objects.requireNonNull(parentScreen.findPreference(preferenceKey)));
                }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(PreferenceKeys.Key.KEY_SAVE_PER_LENS_SETTINGS.mValue)) {
                setHdrxTitle();
                if (PreferenceKeys.isPerLensSettingsOn()) {
                    PreferenceKeys.loadSettingsForCamera(PreferenceKeys.getCameraID());
                    restartActivity();
                }
            }
            if (key.equalsIgnoreCase(PreferenceKeys.Key.KEY_THEME.mValue)) {
                restartActivity();
            }
            if (key.equalsIgnoreCase(PreferenceKeys.Key.KEY_THEME_ACCENT.mValue)) {
                checkEszdTheme();
                restartActivity();
                toRestartApp = true;
            }
            if (key.equalsIgnoreCase(PreferenceKeys.Key.KEY_SHOW_GRADIENT.mValue)) {
                toRestartApp = true;
            }
            if (key.equalsIgnoreCase(PreferenceKeys.Key.KEY_FRAME_COUNT.mValue)) {
                setFramesSummary();
            }
        }

        private void checkEszdTheme() {
            Preference p = findPreference(PreferenceKeys.Key.KEY_SHOW_GRADIENT.mValue);
            if (p != null)
                p.setEnabled(!mSettingsManager.getString(SCOPE_GLOBAL, PreferenceKeys.Key.KEY_THEME_ACCENT).equalsIgnoreCase("eszdman"));
        }

        private void setHdrxTitle() {
            Preference p = findPreference(mContext.getString(R.string.pref_category_hdrx_key));
            if (p != null) {
                if (PreferenceKeys.isPerLensSettingsOn()) {
                    p.setTitle(mContext.getString(R.string.hdrx) + "\t(Lens: " + PreferenceKeys.getCameraID() + ')');
                } else {
                    p.setTitle(mContext.getString(R.string.hdrx));
                }
            }
        }

        private void setFramesSummary() {
            Preference frameCountPreference = findPreference(PreferenceKeys.Key.KEY_FRAME_COUNT.mValue);
            if (frameCountPreference != null) {
                if (mSettingsManager.getInteger(PreferenceKeys.SCOPE_GLOBAL, PreferenceKeys.Key.KEY_FRAME_COUNT) == 1) {
                    frameCountPreference.setSummary(mContext.getString(R.string.unprocessed_raw));
                } else {
                    frameCountPreference.setSummary(mContext.getString(R.string.frame_count_summary));
                }
            }
        }

        private void restartActivity() {
            if (getActivity() != null) {
                Intent intent = new Intent(mContext, getActivity().getClass());
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent,
                        ActivityOptions.makeCustomAnimation(mContext, R.anim.fade_in, R.anim.fade_out).toBundle());
            }
        }

        private void setVersionDetails() {
            activity.runOnUiThread(() -> {
                Preference about = findPreference(mContext.getString(R.string.pref_version_key));
                if (about != null) {
                    try {
                        PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
                        String versionName = packageInfo.versionName;
                        long versionCode = packageInfo.versionCode;

                        Date date = new Date(packageInfo.lastUpdateTime);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss z", Locale.US);
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

                        about.setSummary(mContext.getString(R.string.version_summary, versionName + "." + versionCode, sdf.format(date)));

                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }

                }
            });

        }

        @Override
        public boolean onPreferenceTreeClick(@NonNull Preference preference) {
            return true;
        }

        @Override
        public void onDisplayPreferenceDialog(@NonNull Preference preference) {
            if (preference instanceof ResetPreferences) {
                DialogFragment dialogFragment = ResetPreferences.Dialog.newInstance(preference);
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(getParentFragmentManager(), null);
            } else {
                super.onDisplayPreferenceDialog(preference);
            }
        }
    }
}
