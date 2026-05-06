package com.particlesdevs.photoncamera.pro;

import android.content.Context;
import android.os.Build;
import com.particlesdevs.photoncamera.util.Log;

import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.settings.PreferenceKeys;
import com.particlesdevs.photoncamera.settings.SettingsManager;
import com.particlesdevs.photoncamera.util.HttpLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.particlesdevs.photoncamera.settings.PreferenceKeys.Key.ALL_CAMERA_IDS_KEY;
import static com.particlesdevs.photoncamera.settings.PreferenceKeys.Key.ALL_CAMERA_LENS_KEY;
import static com.particlesdevs.photoncamera.settings.PreferenceKeys.Key.ALL_DEVICES_NAMES_KEY;
import static com.particlesdevs.photoncamera.settings.PreferenceKeys.Key.CAMERAS_PREFERENCE_FILE_NAME;
import static com.particlesdevs.photoncamera.settings.PreferenceKeys.Key.CAMERA_COUNT_KEY;

public class SupportedDevice {
    public static final String THIS_DEVICE = Build.BRAND.toLowerCase() + ":" + Build.DEVICE.toLowerCase();
    private static final String TAG = "SupportedDevice";
    private final SettingsManager mSettingsManager;
    private final Context mContext;
    private Set<String> mSupportedDevicesSet = new LinkedHashSet<>();
    public Specific specific;
    public SensorSpecifics sensorSpecifics;
    private boolean loaded = false;
    private int checkedCount = 0;

    public SupportedDevice(SettingsManager manager, Context context) {
        mSettingsManager = manager;
        mContext = context;
        sensorSpecifics = new SensorSpecifics();
        specific = new Specific(mSettingsManager);
    }

    public void loadCheck() {
        Boolean allowNetworkSync = null;
        try {
            allowNetworkSync = PhotonCamera.getSettings().allowNetworkSync;
            if (PhotonCamera.getSettings().allowNetworkSync) {
                specific.loadSpecific();
            }
        } catch (Exception e) {
            allowNetworkSync = false;
            specific.loadSpecific();
        }

        final Boolean finalAllowNetworkSync = allowNetworkSync;
        new Thread(() -> {
            if (finalAllowNetworkSync) {
                try {
                    if (checkedCount < 1) {
                        loadSupportedDevicesList();
                        isSupported();
                        specific.loadSpecific();
                    }
                } catch (IOException e) {
                    specific.loadSpecific();
                    e.printStackTrace();
                }
            }
            if (!loaded && mSettingsManager.isSet(PreferenceKeys.Key.DEVICES_PREFERENCE_FILE_NAME.mValue, ALL_DEVICES_NAMES_KEY))
                mSupportedDevicesSet = mSettingsManager.getStringSet(PreferenceKeys.Key.DEVICES_PREFERENCE_FILE_NAME.mValue, ALL_DEVICES_NAMES_KEY, null);
        }).start();
        Log.d(TAG, "Checked count:"+checkedCount);
        new Thread(() -> sensorSpecifics.loadSpecifics(mSettingsManager)).start();
    }

    private void clearCameraCache() {
        String camerasScope = CAMERAS_PREFERENCE_FILE_NAME.mValue;
        mSettingsManager.remove(camerasScope, ALL_CAMERA_IDS_KEY);
        mSettingsManager.remove(camerasScope, ALL_CAMERA_LENS_KEY);
        mSettingsManager.remove(camerasScope, CAMERA_COUNT_KEY);
        Log.d(TAG, "Camera ID and characteristics cache cleared");
    }

    public void fetchFromNetwork() {
        new Thread(() -> {
            Log.d(TAG, "Fetching all configurations from network");
            try {
                loadSupportedDevicesList();
                isSupported();
            } catch (IOException e) {
                Log.e(TAG, "Failed to fetch supported devices list: " + e.toString());
            }
            specific.fetchFromNetwork(mContext);
            sensorSpecifics.fetchFromNetwork(mSettingsManager, mContext);
            clearCameraCache();
        }).start();
    }

    private void isSupported() {
        checkedCount++;
        if (mSupportedDevicesSet == null) {
            return;
        }
        if (mSupportedDevicesSet.contains(THIS_DEVICE)) {
            PhotonCamera.showToastFast(R.string.device_support);
        } else {
            PhotonCamera.showToastFast(R.string.device_unsupport);
        }
    }

    public boolean isSupportedDevice() {
        if (mSupportedDevicesSet == null) {
            return false;
        }
        return mSupportedDevicesSet.contains(THIS_DEVICE);
    }

    private void loadSupportedDevicesListFromAssets() throws IOException {
        InputStream is = mContext.getAssets().open("specific/SupportedList.txt");
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String str;
        while ((str = in.readLine()) != null) {
            Log.d(TAG, "read asset:" + str);
            mSupportedDevicesSet.add(str);
        }
        in.close();
        loaded = true;
        Log.d(TAG, "Supported devices loaded from assets, count: " + mSupportedDevicesSet.size());
        mSettingsManager.set(PreferenceKeys.Key.DEVICES_PREFERENCE_FILE_NAME.mValue, ALL_DEVICES_NAMES_KEY, mSupportedDevicesSet);
    }
    private void loadSupportedDevicesList() throws IOException {
        BufferedReader in = HttpLoader.readURL(PhotonCamera.getSpecific().specificSetting.networkSyncBaseUrl + "SupportedList.txt", 200);
        String str;
        while ((str = in.readLine()) != null) {
            mSupportedDevicesSet.add(str);
        }

        loaded = true;
        in.close();
        mSettingsManager.set(PreferenceKeys.Key.DEVICES_PREFERENCE_FILE_NAME.mValue, ALL_DEVICES_NAMES_KEY, mSupportedDevicesSet);
    }
}
