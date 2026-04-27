package com.particlesdevs.photoncamera.ui.settings;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.util.Size;
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
import com.particlesdevs.photoncamera.capture.CaptureController;
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

            // RAW format list building
            ListPreference rawPreference = findPreference(getString(R.string.pref_raw_format_key));
            if (rawPreference == null) {
                return;
            }
            String currentRawValue = rawPreference.getValue();

            List<CharSequence> entriesRaw = new ArrayList<>();
            List<CharSequence> entryRawValues = new ArrayList<>();

            entriesRaw.add("RAW_SENSOR");
            entryRawValues.add("32");
            entriesRaw.add("RAW10");
            entryRawValues.add("37");
            if (PhotonCamera.mRaw12IsSupported) {
                entriesRaw.add("RAW12");
                entryRawValues.add("38");
            }

            rawPreference.setEntries(entriesRaw.toArray(new CharSequence[0]));
            rawPreference.setEntryValues(entryRawValues.toArray(new CharSequence[0]));

            if (!entryRawValues.contains(currentRawValue)) {
                if (entryRawValues.equals("RAW_SENSOR")) {
                    rawPreference.setValue("RAW_SENSOR");
                } else if (!entryRawValues.isEmpty()) {
                    rawPreference.setValue(entryRawValues.get(0).toString());
                }
            }

            // preview format list building
            ListPreference prevPreference = findPreference(getString(R.string.pref_real_preview_format_key));
            if (prevPreference == null) {
                return;
            }
            String currentPrevValue = prevPreference.getValue();

            List<CharSequence> entriesPrev = new ArrayList<>();
            List<CharSequence> entryPrevValues = new ArrayList<>();

            entriesPrev.add("YUV_420_888");
            entryPrevValues.add("35");
            if (PhotonCamera.mYuv10IsSupported) {
                entriesPrev.add("YCBCR_P010");
                entryPrevValues.add("54");
            }

            prevPreference.setEntries(entriesPrev.toArray(new CharSequence[0]));
            prevPreference.setEntryValues(entryPrevValues.toArray(new CharSequence[0]));

            if (!entryPrevValues.contains(currentPrevValue)) {
                if (entryPrevValues.equals("YUV_420_888")) {
                    prevPreference.setValue("YUV_420_888");
                } else if (!entryPrevValues.isEmpty()) {
                    prevPreference.setValue(entryPrevValues.get(0).toString());
                }
            }

            // still image format list building
            ListPreference codecPreference = findPreference(getString(R.string.pref_preview_format_key));
            if (codecPreference == null) {
                return;
            }
            String currentValue = codecPreference.getValue();

            List<CharSequence> entries = new ArrayList<>();
            List<CharSequence> entryValues = new ArrayList<>();

            entries.add("JPEG");
            entryValues.add("256");
            if (PhotonCamera.mHeicIsSupported) {
                entries.add("HEIC");
                entryValues.add("1212500294");
            }
            if (PhotonCamera.mJpegRIsSupported) {
                entries.add("JPEG_R");
                entryValues.add("4101");
            }
            if (PhotonCamera.mHeicUltraHdrIsSupported) {
                entries.add("HEIC_ULTRA");
                entryValues.add("4102");
            }
            entries.add("AVIF (SW)");
            entryValues.add("999999999");
            entries.add("HEIC/HEIF (SW)");
            entryValues.add("999999991");
            entries.add("JPEG LUT (SW)");
            entryValues.add("999999992");
            if (PhotonCamera.mYuv10IsSupported) {
                entries.add("YUV RAW");
                entryValues.add("888888888");
            }
            entries.add("PNG (SW)");
            entryValues.add("999999993");
            entries.add("WebP Lossy (SW)");
            entryValues.add("777777777");
            entries.add("WebP Lossless (SW)");
            entryValues.add("666666666");
            entries.add("JPEG/RAW Stacking");
            entryValues.add("0");
            entries.add("Video Codec 8 Bit");
            entryValues.add("35");
            entries.add("Video Codec 10 Bit");
            entryValues.add("54");

            codecPreference.setEntries(entries.toArray(new CharSequence[0]));
            codecPreference.setEntryValues(entryValues.toArray(new CharSequence[0]));

            if (!entryValues.contains(currentValue)) {
                if (entryValues.equals("JPEG")) {
                    codecPreference.setValue("JPEG");
                } else if (!entryValues.isEmpty()) {
                    codecPreference.setValue(entryValues.get(0).toString());
                }
            }

            // function button population
            ListPreference functionOnePreference = findPreference(getString(R.string.pref_function_one_key));
            ListPreference functionTwoPreference = findPreference(getString(R.string.pref_function_two_key));
            if ((functionOnePreference == null) || (functionTwoPreference == null)) {
                return;
            }
            String currentFunctionOneValue = functionOnePreference.getValue();
            String currentFunctionTwoValue = functionTwoPreference.getValue();

            List<CharSequence> entriesFunctionOne = new ArrayList<>();
            List<CharSequence> entryValuesFunctionOne = new ArrayList<>();

            List<CharSequence> entriesFunctionTwo = new ArrayList<>();
            List<CharSequence> entryValuesFunctionTwo = new ArrayList<>();

            if (functionOnePreference.getEntries() != null && functionOnePreference.getEntryValues() != null) {
                Collections.addAll(entriesFunctionOne, functionOnePreference.getEntries());
                Collections.addAll(entryValuesFunctionOne, functionOnePreference.getEntryValues());
            }

            if (functionTwoPreference.getEntries() != null && functionTwoPreference.getEntryValues() != null) {
                Collections.addAll(entriesFunctionTwo, functionTwoPreference.getEntries());
                Collections.addAll(entryValuesFunctionTwo, functionTwoPreference.getEntryValues());
            }

            if (PhotonCamera.hasXiaomiNight) {
                entriesFunctionOne.add("Xiaomi Night Mode");
                entryValuesFunctionOne.add("Xiaomi Night Mode");
                entriesFunctionTwo.add("Xiaomi Night Mode");
                entryValuesFunctionTwo.add("Xiaomi Night Mode");
            }
            if (PhotonCamera.hasXiaomiSuperNight) {
                entriesFunctionOne.add("Xiaomi Super Night Mode");
                entryValuesFunctionOne.add("Xiaomi Super Night Mode");
                entriesFunctionTwo.add("Xiaomi Super Night Mode");
                entryValuesFunctionTwo.add("Xiaomi Super Night Mode");
            }
            if (PhotonCamera.hasXiaomiAiAutoSceneDetection) {
                entriesFunctionOne.add("Xiaomi AI Auto Scene Detection");
                entryValuesFunctionOne.add("Xiaomi AI Auto Scene Detection");
                entriesFunctionTwo.add("Xiaomi AI Auto Scene Detection");
                entryValuesFunctionTwo.add("Xiaomi AI Auto Scene Detection");
            }
            if (PhotonCamera.hasXiaomiProVideoLog) {
                entriesFunctionOne.add("Xiaomi Pro Video LOG");
                entryValuesFunctionOne.add("Xiaomi Pro Video LOG");
                entriesFunctionTwo.add("Xiaomi Pro Video LOG");
                entryValuesFunctionTwo.add("Xiaomi Pro Video LOG");
            }
            if (PhotonCamera.hasXiaomiProVideoMovie) {
                entriesFunctionOne.add("Xiaomi Pro Video Movie");
                entryValuesFunctionOne.add("Xiaomi Pro Video Movie");
                entriesFunctionTwo.add("Xiaomi Pro Video Movie");
                entryValuesFunctionTwo.add("Xiaomi Pro Video Movie");
            }
            if (PhotonCamera.hasXiaomiReMosaic) {
                entriesFunctionOne.add("Xiaomi Re-Mosaic");
                entryValuesFunctionOne.add("Xiaomi Re-Mosaic");
                entriesFunctionTwo.add("Xiaomi Re-Mosaic");
                entryValuesFunctionTwo.add("Xiaomi Re-Mosaic");
            }
            if (PhotonCamera.hasXiaomiQuadCfa) {
                entriesFunctionOne.add("Xiaomi Quad CFA");
                entryValuesFunctionOne.add("Xiaomi Quad CFA");
                entriesFunctionTwo.add("Xiaomi Quad CFA");
                entryValuesFunctionTwo.add("Xiaomi Quad CFA");
            }
            if (PhotonCamera.hasXiaomiHdr) {
                entriesFunctionOne.add("Xiaomi HDR");
                entryValuesFunctionOne.add("Xiaomi HDR");
                entriesFunctionTwo.add("Xiaomi HDR");
                entryValuesFunctionTwo.add("Xiaomi HDR");
            }
            if (PhotonCamera.hasXiaomiUltraHdr) {
                entriesFunctionOne.add("Xiaomi Ultra HDR");
                entryValuesFunctionOne.add("Xiaomi Ultra HDR");
                entriesFunctionTwo.add("Xiaomi Ultra HDR");
                entryValuesFunctionTwo.add("Xiaomi Ultra HDR");
            }
            if (PhotonCamera.hasXiaomiSuperResolution) {
                entriesFunctionOne.add("Xiaomi Super Resolution");
                entryValuesFunctionOne.add("Xiaomi Super Resolution");
                entriesFunctionTwo.add("Xiaomi Super Resolution");
                entryValuesFunctionTwo.add("Xiaomi Super Resolution");
            }
            if (PhotonCamera.hasIdealRaw) {
                entriesFunctionOne.add("Ideal RAW");
                entryValuesFunctionOne.add("Ideal RAW");
                entriesFunctionTwo.add("Ideal RAW");
                entryValuesFunctionTwo.add("Ideal RAW");
            }
            if (PhotonCamera.hasEisLookAhead) {
                entriesFunctionOne.add("EIS Look Ahead");
                entryValuesFunctionOne.add("EIS Look Ahead");
                entriesFunctionTwo.add("EIS Look Ahead");
                entryValuesFunctionTwo.add("EIS Look Ahead");
            }
            if (PhotonCamera.hasEisRealtime) {
                entriesFunctionOne.add("EIS Realtime");
                entryValuesFunctionOne.add("EIS Realtime");
                entriesFunctionTwo.add("EIS Realtime");
                entryValuesFunctionTwo.add("EIS Realtime");
            }
            if (PhotonCamera.hasEisV3) {
                entriesFunctionOne.add("EIS V3");
                entryValuesFunctionOne.add("EIS V3");
                entriesFunctionTwo.add("EIS V3");
                entryValuesFunctionTwo.add("EIS V3");
            }
            if (PhotonCamera.hasQucommAdrcOff) {
                entriesFunctionOne.add("Qualcomm ADRC Off");
                entryValuesFunctionOne.add("Qualcomm ADRC Off");
                entriesFunctionTwo.add("Qualcomm ADRC Off");
                entryValuesFunctionTwo.add("Qualcomm ADRC Off");
            }
            if (PhotonCamera.hasQucommSensorMode) {
                entriesFunctionOne.add("Qualcomm Sensor Mode");
                entryValuesFunctionOne.add("Qualcomm Sensor Mode");
                entriesFunctionTwo.add("Qualcomm Sensor Mode");
                entryValuesFunctionTwo.add("Qualcomm Sensor Mode");
            }
            if (PhotonCamera.hasVivoZeissColor) {
                entriesFunctionOne.add("Vivo Zeiss Color");
                entryValuesFunctionOne.add("Vivo Zeiss Color");
                entriesFunctionTwo.add("Vivo Zeiss Color");
                entryValuesFunctionTwo.add("Vivo Zeiss Color");
            }
            if (PhotonCamera.hasVivoProMode) {
                entriesFunctionOne.add("Vivo Pro Mode");
                entryValuesFunctionOne.add("Vivo Pro Mode");
                entriesFunctionTwo.add("Vivo Pro Mode");
                entryValuesFunctionTwo.add("Vivo Pro Mode");
            }
            if (PhotonCamera.hasVivoSensorMode) {
                entriesFunctionOne.add("Vivo Sensor Mode");
                entryValuesFunctionOne.add("Vivo Sensor Mode");
                entriesFunctionTwo.add("Vivo Sensor Mode");
                entryValuesFunctionTwo.add("Vivo Sensor Mode");
            }
            if (PhotonCamera.hasVivoDistortionCorrection) {
                entriesFunctionOne.add("Vivo Distortion Correction");
                entryValuesFunctionOne.add("Vivo Distortion Correction");
                entriesFunctionTwo.add("Vivo Distortion Correction");
                entryValuesFunctionTwo.add("Vivo Distortion Correction");
            }

            functionOnePreference.setEntries(entriesFunctionOne.toArray(new CharSequence[0]));
            functionOnePreference.setEntryValues(entryValuesFunctionOne.toArray(new CharSequence[0]));
            functionTwoPreference.setEntries(entriesFunctionTwo.toArray(new CharSequence[0]));
            functionTwoPreference.setEntryValues(entryValuesFunctionTwo.toArray(new CharSequence[0]));

            if (!entryValuesFunctionOne.contains(currentFunctionOneValue)) {
                if (entryValuesFunctionOne.equals("ISO Priority")) {
                    functionOnePreference.setValue("ISO Priority");
                } else if (!entryPrevValues.isEmpty()) {
                    functionOnePreference.setValue(entryPrevValues.get(0).toString());
                }
            }

            if (!entryValuesFunctionTwo.contains(currentFunctionTwoValue)) {
                if (entryValuesFunctionTwo.equals("Shutter Priority")) {
                    functionTwoPreference.setValue("Shutter Priority");
                } else if (!entryPrevValues.isEmpty()) {
                    functionTwoPreference.setValue(entryPrevValues.get(0).toString());
                }
            }
        }
    }

    public static class SoCSettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.soc_preferences, rootKey);

            String sharpnessKey = getString(R.string.pref_soc_qualcomm_sharpness_key);
            Preference sharpnessPreference = findPreference(sharpnessKey);
            if (sharpnessPreference != null) {
                sharpnessPreference.setEnabled(PhotonCamera.hasSharpnessKey);
            }

            String saturationKey = getString(R.string.pref_soc_qualcomm_saturation_key);
            Preference saturationPreference = findPreference(saturationKey);
            if (saturationPreference != null) {
                saturationPreference.setEnabled(PhotonCamera.hasSaturationKey);
            }

            String eisKey = getString(R.string.pref_soc_qualcomm_eis_mode_key);
            Preference eisPreference = findPreference(eisKey);
            if (eisPreference != null) {
                eisPreference.setEnabled(PhotonCamera.hasEisModeKey);
            }

            String aiKey = getString(R.string.pref_soc_qualcomm_ai_mode_key);
            Preference aiPreference = findPreference(aiKey);
            if (aiPreference != null) {
                aiPreference.setEnabled(PhotonCamera.hasAiModeKey);
            }

            String iszKey = getString(R.string.pref_soc_qualcomm_isz_key);
            Preference iszPreference = findPreference(iszKey);
            if (iszPreference != null) {
                iszPreference.setEnabled(PhotonCamera.hasIszKey);
            }

            String mfnrKey = getString(R.string.pref_soc_qualcomm_mfnr_key);
            Preference mfnrPreference = findPreference(mfnrKey);
            if (mfnrPreference != null) {
                mfnrPreference.setEnabled(PhotonCamera.hasMfnrKey);
            }

            String mfnrFramesKey = getString(R.string.pref_soc_qualcomm_mfnr_frames_key);
            Preference mfnrFramesPreference = findPreference(mfnrFramesKey);
            if (mfnrFramesPreference != null) {
                mfnrFramesPreference.setEnabled(PhotonCamera.hasMfnrKey);
            }

            String autoHdrKey = getString(R.string.pref_soc_auto_hdr_key);
            Preference autoHdrPreference = findPreference(autoHdrKey);
            if (autoHdrPreference != null) {
                autoHdrPreference.setEnabled(PhotonCamera.hasAutoHdr);
            }

            String socHdrModeKey = getString(R.string.pref_soc_hdr_mode_key);
            Preference socHdrModePreference = findPreference(socHdrModeKey);
            if (socHdrModePreference != null) {
                socHdrModePreference.setEnabled(PhotonCamera.hasSocHdrMode);
            }

            String socManualWbKey = getString(R.string.pref_soc_qualcomm_manual_wb_key);
            Preference socManualWbPreference = findPreference(socManualWbKey);
            if (socManualWbPreference != null) {
                socManualWbPreference.setEnabled(PhotonCamera.hasManualWb);
            }
        }
    }

    public static class VideoSettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.video_preferences, rootKey);

            String hdrKey = getString(R.string.pref_hdr_video_key);
            Preference hdrPreference = findPreference(hdrKey);
            if (hdrPreference != null) {
                hdrPreference.setEnabled(PhotonCamera.hasHdr);
            }

            String tenBitKey = getString(R.string.pref_10bit_video_key);
            Preference tenBitPreference = findPreference(tenBitKey);
            if (tenBitPreference != null) {
                tenBitPreference.setEnabled(PhotonCamera.hasTenBit);
            }

            String hdrModeKey = getString(R.string.pref_hdr_mode_key);
            Preference hdrModePreference = findPreference(hdrModeKey);
            if (hdrModePreference != null) {
                hdrModePreference.setEnabled(PhotonCamera.hasHdr);
            }

            String transfereModeKey = getString(R.string.pref_transfer_function_key);
            Preference transferePreference = findPreference(transfereModeKey);
            if (transferePreference != null) {
                transferePreference.setEnabled(PhotonCamera.hasHdr);
            }

            ListPreference codecPreference = findPreference(getString(R.string.pref_codec_key));
            if (codecPreference == null) {
                return;
            }

            String currentValue = codecPreference.getValue();

            CaptureController.EncoderInfoUtil encoderInfo = new CaptureController.EncoderInfoUtil();
            encoderInfo.getEncoderInfos();

            List<CharSequence> entries = new ArrayList<>();
            List<CharSequence> entryValues = new ArrayList<>();

            Size maxRes = encoderInfo.getMaxResForMimeType(MediaFormat.MIMETYPE_VIDEO_AVC);
            boolean hasHwSupport = encoderInfo.getHwSupportForMimeType(MediaFormat.MIMETYPE_VIDEO_AVC);
            if (maxRes != null) {
                if (hasHwSupport) {
                    entries.add("AVC/H.264 (HW)");
                }
                else {
                    entries.add("AVC/H.264 (SW)");
                }
                entryValues.add("AVC");
            }
            maxRes = encoderInfo.getMaxResForMimeType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            hasHwSupport = encoderInfo.getHwSupportForMimeType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            if (maxRes != null) {
                if (hasHwSupport) {
                    entries.add("HEVC/H.265 (HW)");
                }
                else {
                    entries.add("HEVC/H.265 (SW)");
                }
                entryValues.add("HEVC");
            }
            maxRes = encoderInfo.getMaxResForMimeType(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION);
            hasHwSupport = encoderInfo.getHwSupportForMimeType(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION);
            if (maxRes != null) {
                if (hasHwSupport) {
                    entries.add("Dolby Vision (HW)");
                }
                else {
                    entries.add("Dolby Vision (SW)");
                }
                entryValues.add("DOLBY_VISION");
            }
            maxRes = encoderInfo.getMaxResForMimeType(MediaFormat.MIMETYPE_VIDEO_AV1);
            hasHwSupport = encoderInfo.getHwSupportForMimeType(MediaFormat.MIMETYPE_VIDEO_AV1);
            if (maxRes != null) {
                if (hasHwSupport) {
                    entries.add("AV1 (HW)");
                }
                else {
                    entries.add("AV1 (SW)");
                }
                entryValues.add("AV1");
            }
            maxRes = encoderInfo.getMaxResForMimeType(MediaFormat.MIMETYPE_VIDEO_APV);
            hasHwSupport = encoderInfo.getHwSupportForMimeType(MediaFormat.MIMETYPE_VIDEO_APV);
            if (maxRes != null) {
                if (hasHwSupport) {
                    entries.add("APV (HW)");
                }
                else {
                    entries.add("APV (SW)");
                }
                entryValues.add("APV");
            }
            maxRes = encoderInfo.getMaxResForMimeType(MediaFormat.MIMETYPE_VIDEO_VP8);
            hasHwSupport = encoderInfo.getHwSupportForMimeType(MediaFormat.MIMETYPE_VIDEO_VP8);
            if (maxRes != null) {
                if (hasHwSupport) {
                    entries.add("VP8 (HW)");
                }
                else {
                    entries.add("VP8 (SW)");
                }
                entryValues.add("VP8");
            }
            maxRes = encoderInfo.getMaxResForMimeType(MediaFormat.MIMETYPE_VIDEO_VP9);
            hasHwSupport = encoderInfo.getHwSupportForMimeType(MediaFormat.MIMETYPE_VIDEO_VP9);
            if (maxRes != null) {
                if (hasHwSupport) {
                    entries.add("VP9 (HW)");
                }
                else {
                    entries.add("VP9 (SW)");
                }
                entryValues.add("VP9");
            }

            codecPreference.setEntries(entries.toArray(new CharSequence[0]));
            codecPreference.setEntryValues(entryValues.toArray(new CharSequence[0]));

            if (!entryValues.contains(currentValue)) {
                if (entryValues.contains("HEV")) {
                    codecPreference.setValue("HEVC");
                } else if (!entryValues.isEmpty()) {
                    codecPreference.setValue(entryValues.get(0).toString());
                }
            }
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

                File tuningDir = FileManager.sPHOTON_TUNING_DIR;
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

                File lutDir = FileManager.sPHOTON_LUT_DIR;
                if (lutDir.exists() && lutDir.isDirectory()) {
                    File[] files = lutDir.listFiles((dir, name) -> name.toLowerCase().endsWith("_lut.png"));

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
