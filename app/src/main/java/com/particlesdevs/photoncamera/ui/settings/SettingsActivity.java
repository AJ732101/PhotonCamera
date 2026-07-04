package com.particlesdevs.photoncamera.ui.settings;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
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
import com.particlesdevs.photoncamera.settings.TunablePreferenceGenerator;
import com.particlesdevs.photoncamera.ui.SplashActivity;
import com.particlesdevs.photoncamera.ui.settings.custompreferences.ResetPreferences;
import com.particlesdevs.photoncamera.util.Log;
import com.particlesdevs.photoncamera.util.log.FragmentLifeCycleMonitor;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceViewHolder;

import java.io.File;
import java.lang.reflect.Field;
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
            if (PhotonCamera.mRaw12IsSupported) {
                entriesRaw.add("RAW14");
                entryRawValues.add("44");
            }
            if (PhotonCamera.mRawPrivateIsSupported) {
                entriesRaw.add("RAW_PRIVATE");
                entryRawValues.add("36");
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
            entries.add("JPEG (SW, LUT)");
            entryValues.add("999999992");
            if (PhotonCamera.mYuv10IsSupported) {
                entries.add("YUV RAW");
                entryValues.add("888888888");
            }
            entries.add("PNG (SW, LUT)");
            entryValues.add("999999993");
            entries.add("WebP Lossy (SW, LUT)");
            entryValues.add("777777777");
            entries.add("WebP Lossless (SW, LUT)");
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
            String saturationVideoKey = getString(R.string.pref_soc_qualcomm_saturation_video_key);
            Preference saturationPreference = findPreference(saturationKey);
            Preference saturationVideoPreference = findPreference(saturationVideoKey);
            if (saturationPreference != null) {
                saturationPreference.setEnabled(PhotonCamera.hasSaturationKey);
            }
            if (saturationVideoPreference != null) {
                saturationVideoPreference.setEnabled(PhotonCamera.hasSaturationKey);
            }

            String contrastKey = getString(R.string.pref_soc_qualcomm_contrast_key);
            String contrastVideoKey = getString(R.string.pref_soc_qualcomm_contrast_video_key);
            Preference contrastPreference = findPreference(contrastKey);
            Preference contrastVideoPreference = findPreference(contrastVideoKey);
            if (contrastPreference != null) {
                contrastPreference.setEnabled(PhotonCamera.hasContrastKey);
            }
            if (contrastVideoPreference != null) {
                contrastVideoPreference.setEnabled(PhotonCamera.hasContrastKey);
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

            String ultraHdrKey = getString(R.string.pref_ultra_hdr_key);
            Preference ultraHdrPreference = findPreference(ultraHdrKey);
            String sixteenBitKey = getString(R.string.pref_16bit_key);
            Preference sixteenBitPreference = findPreference(sixteenBitKey);
            if (ultraHdrPreference != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ultraHdrPreference.setEnabled(true);
                } else {
                    ultraHdrPreference.setEnabled(false);
                }
            }
            if (sixteenBitPreference != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    sixteenBitPreference.setEnabled(true);
                } else {
                    sixteenBitPreference.setEnabled(false);
                }
            }

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

    public static class DeviceInfoFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.device_info_preferences, rootKey);

            PreferenceCategory generalCategory = new androidx.preference.PreferenceCategory(getContext());
            generalCategory.setTitle(R.string.general);
            getPreferenceScreen().addPreference(generalCategory);

            float[] apertures = CaptureController.mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES);
            if (apertures != null && apertures.length > 0) {
                generalCategory.addPreference(createCompactText(getString(R.string.aperture1) + String.format(Locale.US, ": F%.2f", apertures[0])));
            }
            generalCategory.addPreference(createCompactText(getString(R.string.flength35) + String.format(Locale.US, ": %dmm", PhotonCamera.getParameters().current35mmFocalLength)));

            PreferenceCategory photoCategory = new androidx.preference.PreferenceCategory(getContext());
            photoCategory.setTitle(R.string.still_image_label);
            getPreferenceScreen().addPreference(photoCategory);

            photoCategory.addPreference(createCompactCheckBox("OIS", isOisSupported(), false));
            photoCategory.addPreference(createCompactCheckBox("HEIC", PhotonCamera.mHeicIsSupported, false));
            photoCategory.addPreference(createCompactCheckBox("JPEG-R (Ultra HDR)", PhotonCamera.mJpegRIsSupported, false));
            photoCategory.addPreference(createCompactCheckBox("Ultra HEIC", PhotonCamera.mHeicUltraHdrIsSupported, false));
            photoCategory.addPreference(createCompactCheckBox("YCBCR_P010 (YUV RAW)", PhotonCamera.mYuv10IsSupported, false));
            photoCategory.addPreference(createCompactCheckBox("RAW10", PhotonCamera.mRaw10IsSupported, false));
            Size[] rawSizes = getRawSensorSizes(ImageFormat.RAW10);
            if (rawSizes != null) {
                for (Size size : rawSizes) {
                    Preference p = new Preference(getContext());
                    p.setLayoutResource(R.layout.preference_compact_item);
                    p.setTitle("   " + size.getWidth() + " x " + size.getHeight());
                    p.setEnabled(false);
                    photoCategory.addPreference(p);
                }
            }
            photoCategory.addPreference(createCompactCheckBox("RAW12", PhotonCamera.mRaw12IsSupported, false));
            rawSizes = getRawSensorSizes(ImageFormat.RAW12);
            if (rawSizes != null) {
                for (Size size : rawSizes) {
                    Preference p = new Preference(getContext());
                    p.setLayoutResource(R.layout.preference_compact_item);
                    p.setTitle("   " + size.getWidth() + " x " + size.getHeight());
                    p.setEnabled(false);
                    photoCategory.addPreference(p);
                }
            }
            photoCategory.addPreference(createCompactCheckBox("RAW14", PhotonCamera.mRaw14IsSupported, false));
            rawSizes = getRawSensorSizes(ImageFormat.RAW14);
            if (rawSizes != null) {
                for (Size size : rawSizes) {
                    Preference p = new Preference(getContext());
                    p.setLayoutResource(R.layout.preference_compact_item);
                    p.setTitle("   " + size.getWidth() + " x " + size.getHeight());
                    p.setEnabled(false);
                    photoCategory.addPreference(p);
                }
            }
            photoCategory.addPreference(createCompactCheckBox("RAW_SENSOR", PhotonCamera.mRawSensorIsSupported, false));
            rawSizes = getRawSensorSizes(ImageFormat.RAW_SENSOR);
            if (rawSizes != null) {
                for (Size size : rawSizes) {
                    Preference p = new Preference(getContext());
                    p.setLayoutResource(R.layout.preference_compact_item);
                    p.setTitle("   " + size.getWidth() + " x " + size.getHeight());
                    p.setEnabled(false);
                    photoCategory.addPreference(p);
                }
            }

            PreferenceCategory videoCategory = new androidx.preference.PreferenceCategory(getContext());
            videoCategory.setTitle("Video");
            getPreferenceScreen().addPreference(videoCategory);

            CaptureController.EncoderInfoUtil encoderInfo = new CaptureController.EncoderInfoUtil();
            encoderInfo.getEncoderInfos();
            Size maxRes = encoderInfo.getMaxResForMimeType(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION);
            boolean hasHwHevc = encoderInfo.getHwSupportForMimeType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            boolean hasHwAv1 = encoderInfo.getHwSupportForMimeType(MediaFormat.MIMETYPE_VIDEO_AV1);
            boolean hasHwApv = encoderInfo.getHwSupportForMimeType(MediaFormat.MIMETYPE_VIDEO_APV);

            videoCategory.addPreference(createCompactCheckBox("Dolby Vision", (maxRes == null) ? false : true, false));
            videoCategory.addPreference(createCompactCheckBox("HW HEVC", hasHwHevc, false));
            videoCategory.addPreference(createCompactCheckBox("HW AV1", hasHwAv1, false));
            videoCategory.addPreference(createCompactCheckBox("HW APV", hasHwApv, false));
            videoCategory.addPreference(createCompactCheckBox("EIS", isEisSupported(), false));
            videoCategory.addPreference(createCompactCheckBox("10 Bit", PhotonCamera.hasTenBit, false));
            videoCategory.addPreference(createCompactCheckBox("HDR", PhotonCamera.hasHdr, false));
            videoCategory.addPreference(createCompactCheckBox("   HLG", PhotonCamera.mHlgIsSupported, true));
            videoCategory.addPreference(createCompactCheckBox("   HDR10", PhotonCamera.mHdrTenIsSupported, true));
            videoCategory.addPreference(createCompactCheckBox("   HDR10+", PhotonCamera.mHdrTenPlusIsSupported, true));
        }

        private CheckBoxPreference createCompactCheckBox(String title, boolean checked, boolean indent) {
            CheckBoxPreference pref = new CheckBoxPreference(getContext());
            pref.setLayoutResource(R.layout.preference_compact_item);
            pref.setTitle(title);
            pref.setChecked(checked);
            pref.setEnabled(false);
            return pref;
        }

        private Preference createCompactText(String text) {
            Preference pref = new Preference(getContext());
            pref.setLayoutResource(R.layout.preference_compact_item);
            pref.setTitle(text);
            pref.setEnabled(false);
            return pref;
        }

        private Size[] getRawSensorSizes(int imageFormat) {
            try {
                int[] capabilities = CaptureController.mCameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                boolean supportsRaw = false;

                if (capabilities != null) {
                    for (int capability : capabilities) {
                        if (capability == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) {
                            supportsRaw = true;
                            break;
                        }
                    }
                }

                if (!supportsRaw) {
                    return null;
                }

                StreamConfigurationMap map = CaptureController.mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (map != null) {
                    return map.getOutputSizes(imageFormat);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        private boolean isOisSupported() {
            int[] stabilizationModes = CaptureController.mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION);
            if (stabilizationModes != null) {
                for (int mode : stabilizationModes) {
                    if (mode == CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean isEisSupported() {
            int[] stabilizationModes = CaptureController.mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES);
            if (stabilizationModes != null) {
                for (int mode : stabilizationModes) {
                    if ((mode == CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON) ||
                        (mode == CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public static class VendorKeysSettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            if (PhotonCamera.getCaptureController() != null) {
                PhotonCamera.getCaptureController().createVendorKeysList();
            }
            setPreferencesFromResource(R.xml.vendor_keys_preferences, rootKey);
            PreferenceScreen screen = getPreferenceScreen();
            Context ctx = getContext();

            EditTextPreference filterNamePreference = findPreference(getString(R.string.pref_vendor_keys_name_filter_key));
            ListPreference filterTypePreference = findPreference(getString(R.string.pref_vendor_keys_type_filter_key));
            ListPreference filterClassPreference = findPreference(getString(R.string.pref_vendor_keys_class_filter_key));
            if (filterTypePreference != null && PhotonCamera.vendorKeysMapType != null) {
                java.util.Set<String> uniqueTypes = new java.util.HashSet<>(PhotonCamera.vendorKeysMapType.values());
                List<String> sortedTypes = new ArrayList<>(uniqueTypes);
                java.util.Collections.sort(sortedTypes);

                List<CharSequence> entries = new ArrayList<>();
                List<CharSequence> entryValues = new ArrayList<>();

                entries.add("All");
                entryValues.add("");

                for (String type : sortedTypes) {
                    entries.add(type);
                    entryValues.add(type);
                }
                
                filterTypePreference.setEntries(entries.toArray(new CharSequence[0]));
                filterTypePreference.setEntryValues(entryValues.toArray(new CharSequence[0]));

                filterClassPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    if (getActivity() != null) {
                        getActivity().recreate();
                    }
                    return true;
                });

                filterTypePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    if (getActivity() != null) {
                        getActivity().recreate();
                    }
                    return true;
                });

                filterNamePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    if (getActivity() != null) {
                        getActivity().recreate();
                    }
                    return true;
                });
            }

            Preference exportButton = new Preference(ctx) {
                @Override
                public void onBindViewHolder(androidx.preference.PreferenceViewHolder holder) {
                    super.onBindViewHolder(holder);
                    View widgetFrameView = holder.findViewById(android.R.id.widget_frame);
                    if (widgetFrameView instanceof ViewGroup) {
                        ViewGroup widgetFrame = (ViewGroup) widgetFrameView;
                        widgetFrame.removeAllViews();
                        widgetFrame.setVisibility(View.VISIBLE);

                        android.widget.CheckBox cb = new android.widget.CheckBox(widgetFrame.getContext());
                        cb.setText("CSV");
                        cb.setChecked(PreferenceManager.getDefaultSharedPreferences(widgetFrame.getContext()).getBoolean("pref_vendor_keys_export_csv", false));
                        cb.setOnCheckedChangeListener((v, isChecked) -> 
                            PreferenceManager.getDefaultSharedPreferences(v.getContext()).edit().putBoolean("pref_vendor_keys_export_csv", isChecked).apply());

                        widgetFrame.addView(cb);
                    }
                }
            };
            exportButton.setLayoutResource(R.layout.preference_with_margin);
            exportButton.setTitle("Export Filtered List");
            exportButton.setSummary("/DCIM/PhotonVidCam/Tuning/VendorKeysList(.txt)(.csv)");
            exportButton.setIcon(R.drawable.save_24px);
            exportButton.setOnPreferenceClickListener(preference -> {
                boolean isCsv = PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean("pref_vendor_keys_export_csv", false);
                exportVendorKeys(isCsv);
                return true;
            });
            screen.addPreference(exportButton);

            androidx.preference.PreferenceCategory categoryConfigured = new androidx.preference.PreferenceCategory(ctx);
            categoryConfigured.setTitle("Configured (NoGuiYet.txt)");
            screen.addPreference(categoryConfigured);

            Preference headerConfigured = new Preference(ctx);
            headerConfigured.setLayoutResource(R.layout.vendor_keys_header);
            headerConfigured.setSelectable(false);
            screen.addPreference(headerConfigured);

            // 1. Byte
            if (PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeByteName != null) {
                for (int i = 0; i < PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeByteName.length; i++) {
                    screen.addPreference(new VendorKeyEntry(ctx,
                            PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeByteName[i], "Byte",
                            String.valueOf(PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeByteValue[i])));
                }
            }

            // 2. Int32
            if (PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeInt32Name != null) {
                for (int i = 0; i < PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeInt32Name.length; i++) {
                    screen.addPreference(new VendorKeyEntry(ctx,
                            PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeInt32Name[i], "Int32",
                            String.valueOf(PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeInt32Value[i])));
                }
            }

            // 3. Int64
            if (PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeInt64Name != null) {
                for (int i = 0; i < PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeInt64Name.length; i++) {
                    screen.addPreference(new VendorKeyEntry(ctx,
                            PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeInt64Name[i], "Int64",
                            String.valueOf(PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeInt64Value[i])));
                }
            }

            // 4. Float
            if (PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeFloatName != null) {
                for (int i = 0; i < PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeFloatName.length; i++) {
                    screen.addPreference(new VendorKeyEntry(ctx,
                            PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeFloatName[i], "Float",
                            String.valueOf(PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeFloatValue[i])));
                }
            }

            androidx.preference.PreferenceCategory categoryFoundOnDevice = new androidx.preference.PreferenceCategory(ctx);
            categoryFoundOnDevice.setTitle("Available for this Camera ID");
            screen.addPreference(categoryFoundOnDevice);

            Preference header = new Preference(ctx);
            header.setLayoutResource(R.layout.vendor_keys_device_header);
            header.setSelectable(false);
            screen.addPreference(header);

            int keyNum = 0;
            if (PhotonCamera.vendorKeysMapType != null) {
                String filterClassValue = filterClassPreference != null ? filterClassPreference.getValue() : "";
                String filterTypeValue = filterTypePreference != null ? filterTypePreference.getValue() : "";
                String filterNameValue = filterNamePreference != null ? filterNamePreference.getText() : "";
                
                if (filterClassValue == null) filterClassValue = "";
                if (filterTypeValue == null) filterTypeValue = "";
                if (filterNameValue == null) filterNameValue = "";
                filterNameValue = filterNameValue.trim().toLowerCase();

                // Sort keys alphabetically for better readability
                List<String> sortedIds = new ArrayList<>(PhotonCamera.vendorKeysMapType.keySet());
                java.util.Collections.sort(sortedIds);
                
                for (String uniqueId : sortedIds) {
                    String[] parts = uniqueId.split("@");
                    String originalName = parts[0];
                    String keyType = PhotonCamera.vendorKeysMapType.get(uniqueId);
                    String keyClass = PhotonCamera.vendorKeysMapClass.get(uniqueId);
                    
                    if (keyType == null) keyType = "???";
                    if (keyClass == null) keyClass = "???";

                    boolean classMatch = filterClassValue.isEmpty() || keyClass.equalsIgnoreCase(filterClassValue);
                    boolean typeMatch = filterTypeValue.isEmpty() || keyType.equalsIgnoreCase(filterTypeValue);
                    boolean nameMatch = filterNameValue.isEmpty() || originalName.toLowerCase().contains(filterNameValue);

                    if (classMatch && typeMatch && nameMatch) {
                        keyNum++;
                        screen.addPreference(new VendorKeyDeviceEntry(ctx, originalName, keyType, keyClass));
                    }
                }
            }

            Preference overallKeys = new Preference(ctx);
            overallKeys.setSelectable(false);
            overallKeys.setTitle("Vendor Keys: " + keyNum + " (of " + PhotonCamera.vendorKeysMapType.size() + ")");
            screen.addPreference(overallKeys);

            Preference bottomSpacer = new Preference(ctx);
            bottomSpacer.setSelectable(false);
            bottomSpacer.setTitle("");
            screen.addPreference(bottomSpacer);
        }

        private void exportVendorKeys(boolean isCsv) {
            if (PhotonCamera.vendorKeysMapType == null) return;

            EditTextPreference filterNamePreference = findPreference(getString(R.string.pref_vendor_keys_name_filter_key));
            ListPreference filterTypePreference = findPreference(getString(R.string.pref_vendor_keys_type_filter_key));
            ListPreference filterClassPreference = findPreference(getString(R.string.pref_vendor_keys_class_filter_key));

            String filterClassValue = filterClassPreference != null ? filterClassPreference.getValue() : "";
            String filterTypeValue = filterTypePreference != null ? filterTypePreference.getValue() : "";
            String filterNameValue = filterNamePreference != null ? filterNamePreference.getText() : "";

            if (filterClassValue == null) filterClassValue = "";
            if (filterTypeValue == null) filterTypeValue = "";
            if (filterNameValue == null) filterNameValue = "";
            filterNameValue = filterNameValue.trim().toLowerCase();

            StringBuilder sb = new StringBuilder();
            if (!isCsv) {
                sb.append("Vendor Keys Export\n");
                sb.append("Camera ID: ").append(com.particlesdevs.photoncamera.settings.PreferenceKeys.getCameraID()).append("\n");
                sb.append("Filters: Class=").append(filterClassValue.isEmpty() ? "All" : filterClassValue)
                  .append(", Type=").append(filterTypeValue.isEmpty() ? "All" : filterTypeValue)
                  .append(", Name=").append(filterNameValue.isEmpty() ? "None" : filterNameValue).append("\n\n");
                
                sb.append(String.format("%-80s | %-35s | %-10s\n", "Key Name", "Type", "Class"));
                sb.append("--------------------------------------------------------------------------------------------------------------------------------\n");
            } else {
                sb.append("Key Name,Type,Class\n");
            }

            List<String> sortedIds = new ArrayList<>(PhotonCamera.vendorKeysMapType.keySet());
            java.util.Collections.sort(sortedIds);

            int count = 0;
            for (String uniqueId : sortedIds) {
                String[] parts = uniqueId.split("@");
                String originalName = parts[0];
                String keyType = PhotonCamera.vendorKeysMapType.get(uniqueId);
                String keyClass = PhotonCamera.vendorKeysMapClass.get(uniqueId);

                if (keyType == null) keyType = "???";
                if (keyClass == null) keyClass = "???";

                boolean classMatch = filterClassValue.isEmpty() || keyClass.equalsIgnoreCase(filterClassValue);
                boolean typeMatch = filterTypeValue.isEmpty() || keyType.equalsIgnoreCase(filterTypeValue);
                boolean nameMatch = filterNameValue.isEmpty() || originalName.toLowerCase().contains(filterNameValue);

                if (classMatch && typeMatch && nameMatch) {
                    count++;
                    if (isCsv) {
                        sb.append(originalName).append(",").append(keyType).append(",").append(keyClass).append("\n");
                    } else {
                        sb.append(String.format("%-80s | %-35s | %-10s\n", originalName, keyType, keyClass));
                    }
                }
            }

            if (!isCsv) {
                sb.append("\nTotal Exported Keys: ").append(count);
            }

            try {
                File tuningDir = FileManager.sPHOTON_TUNING_DIR;
                if (!tuningDir.exists()) tuningDir.mkdirs();
                File outFile = new File(tuningDir, isCsv ? "VendorKeysList.csv" : "VendorKeysList.txt");
                java.io.PrintWriter writer = new java.io.PrintWriter(outFile);
                writer.print(sb.toString());
                writer.close();
                PhotonCamera.showToast("Exported " + count + " keys to " + outFile.getName());
            } catch (Exception e) {
                Log.e("VendorKeys", "Export failed", e);
                PhotonCamera.showToast("Export failed: " + e.getMessage());
            }
        }
    }

    private static class VendorKeyEntry extends Preference {
        private final String name, type, value;
        public VendorKeyEntry(Context context, String name, String type, String value) {
            super(context);
            this.name = name;
            this.type = type;
            this.value = value;
            setLayoutResource(R.layout.vendor_key_row);
        }
        @Override
        public void onBindViewHolder(androidx.preference.PreferenceViewHolder holder) {
            super.onBindViewHolder(holder);
            ((TextView) holder.findViewById(R.id.key_name)).setText(name);
            ((TextView) holder.findViewById(R.id.key_type)).setText(type);
            ((TextView) holder.findViewById(R.id.key_value)).setText(value);

            // Long Press to copy to clipboard
            holder.itemView.setOnLongClickListener(v -> {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                        getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    String textToCopy = name + " [" + type + "] = " + value;
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Vendor Key", textToCopy);
                    clipboard.setPrimaryClip(clip);
                    com.particlesdevs.photoncamera.app.PhotonCamera.showToast("Copied to clipboard");
                }
                return true;
            });
        }
    }

    private static class VendorKeyDeviceEntry extends Preference {
        private final String name, type, clazz;
        public VendorKeyDeviceEntry(Context context, String name, String type, String clazz) {
            super(context);
            this.name = name;
            this.type = type;
            this.clazz = clazz;

            setLayoutResource(R.layout.vendor_key_device_row);
        }
        @Override
        public void onBindViewHolder(androidx.preference.PreferenceViewHolder holder) {
            super.onBindViewHolder(holder);
            ((TextView) holder.findViewById(R.id.key_device_name)).setText(name);
            ((TextView) holder.findViewById(R.id.key_device_type)).setText(type);
            ((TextView) holder.findViewById(R.id.key_device_class)).setText(clazz);

            View.OnLongClickListener longClickListener = v -> {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                        getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    String textToCopy = name + " [" + type + "]" + " [" + clazz + "]";
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Vendor Key Name", textToCopy);
                    clipboard.setPrimaryClip(clip);
                    com.particlesdevs.photoncamera.app.PhotonCamera.showToast("Key name copied");
                }
                return true;
            };

            // Long Press to copy to clipboard (on the whole row)
            holder.itemView.setOnLongClickListener(longClickListener);

            // Click on name to show details (if it's a Char key)
            if ("Char".equals(clazz)) {
                View nameView = holder.findViewById(R.id.key_device_name);
                if (nameView != null) {
                    nameView.setOnClickListener(v -> showKeyDetailsDialog());
                    // Also attach the long click listener to the nameView so it doesn't block the parent's detector
                    nameView.setOnLongClickListener(longClickListener);
                }
            }
        }

        private void showKeyDetailsDialog() {
            String value = getCharacteristicsValue(name);
            Context context = getContext();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            SpannableStringBuilder title = new SpannableStringBuilder(context.getString(R.string.key_details_label));
            title.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, title.length(), 0);
            title.setSpan(new android.text.style.RelativeSizeSpan(1.25f), 0, title.length(), 0);
            builder.setTitle(title);

            SpannableStringBuilder sb = new SpannableStringBuilder();
            sb.append("\nClass:\nCameraCharacteristics\n\n");
            sb.append("Name:\n").append(name).append("\n\n");
            sb.append("Type:\n").append(type).append("\n\n");
            sb.append("Value:\n");

            int start = sb.length();
            sb.append(value);
            int end = sb.length();

            sb.setSpan(new android.text.style.ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                            context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        android.content.ClipData clip = android.content.ClipData.newPlainText("Vendor Key Value", value);
                        clipboard.setPrimaryClip(clip);
                        PhotonCamera.showToast("Value copied to clipboard");
                    }
                }

                @Override
                public void updateDrawState(@NonNull android.text.TextPaint ds) {
                    // Keep the original text color and remove the underline
                    ds.setUnderlineText(false);
                }
            }, start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            TextView textView = new TextView(context);
            textView.setText(sb);
            textView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
            int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
            textView.setPadding(padding, padding, padding, padding);
            textView.setTextSize(16);

            builder.setView(textView);
            builder.setPositiveButton("OK", null);
            builder.show();
        }
        
        private String getCharacteristicsValue(String keyName) {
            if (CaptureController.mCameraCharacteristics == null) return "N/A";
            try {
                // Try to find the key in the officially reported keys first
                for (CameraCharacteristics.Key<?> key : CaptureController.mCameraCharacteristics.getKeys()) {
                    if (key.getName().equals(keyName)) {
                        Object val = CaptureController.mCameraCharacteristics.get(key);
                        return val == null ? "null" : formatValue(val);
                    }
                }
                
                // If not found in official keys, it might be a hidden/vendor key
                Class<?> typeClass = getTypeClass(type);
                java.lang.reflect.Constructor<CameraCharacteristics.Key> charConstructor = 
                        CameraCharacteristics.Key.class.getDeclaredConstructor(String.class, Class.class);
                charConstructor.setAccessible(true);
                CameraCharacteristics.Key<?> hiddenKey = charConstructor.newInstance(keyName, typeClass);
                
                Object val = CaptureController.mCameraCharacteristics.get(hiddenKey);
                return val == null ? "null" : formatValue(val);
            } catch (IllegalArgumentException e) {
                // This happens when the key is known to the vendor tag descriptor but not present for this camera ID
                return "N/A (Not on this camera)";
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }

        private Class<?> getTypeClass(String typeStr) {
            if (typeStr == null) return Object.class;
            try {
                switch (typeStr) {
                    case "int":
                    case "Int32": return Integer.class;
                    case "long":
                    case "Int64": return Long.class;
                    case "float":
                    case "Float": return Float.class;
                    case "byte":
                    case "Byte": return Byte.class;
                    case "double":
                    case "Double": return Double.class;
                    case "Boolean":
                    case "boolean": return Boolean.class;
                    case "Rational": return android.util.Rational.class;
                    case "int[]":
                    case "Int32[]": return int[].class;
                    case "long[]":
                    case "Int64[]": return long[].class;
                    case "float[]": return float[].class;
                    case "byte[]":
                    case "Byte[]": return byte[].class;
                    case "double[]": return double[].class;
                    case "Rational[]": return android.util.Rational[].class;
                    case "Rect": return android.graphics.Rect.class;
                    case "Rect[]": return android.graphics.Rect[].class;
                    case "Size": return android.util.Size.class;
                    case "Size[]": return android.util.Size[].class;
                    case "StreamConfigurationDuration":
                        return Class.forName("android.hardware.camera2.params.StreamConfigurationDuration");
                    case "StreamConfigurationDuration[]":
                        return Class.forName("[Landroid.hardware.camera2.params.StreamConfigurationDuration;");
                    case "StreamConfiguration":
                        return Class.forName("android.hardware.camera2.params.StreamConfiguration");
                    case "StreamConfiguration[]":
                        return Class.forName("[Landroid.hardware.camera2.params.StreamConfiguration;");
                    case "HighSpeedVideoConfiguration":
                        return Class.forName("android.hardware.camera2.params.HighSpeedVideoConfiguration");
                    case "HighSpeedVideoConfiguration[]":
                        return Class.forName("[Landroid.hardware.camera2.params.HighSpeedVideoConfiguration;");
                    case "DeviceStateSensorOrientationMap":
                        return Class.forName("android.hardware.camera2.params.DeviceStateSensorOrientationMap");
                    case "LensShadingMap":
                        return android.hardware.camera2.params.LensShadingMap.class;
                    case "MultiResolutionStreamConfigurationMap":
                        return android.hardware.camera2.params.MultiResolutionStreamConfigurationMap.class;
                    case "BlackLevelPattern":
                        return android.hardware.camera2.params.BlackLevelPattern.class;
                    case "ColorSpaceTransform":
                        return android.hardware.camera2.params.ColorSpaceTransform.class;
                    case "TonemapCurve":
                        return android.hardware.camera2.params.TonemapCurve.class;
                    case "ReprocessFormatsMap":
                        return Class.forName("android.hardware.camera2.params.ReprocessFormatsMap");
                    case "MeteringRectangle":
                        return android.hardware.camera2.params.MeteringRectangle.class;
                    case "MeteringRectangle[]":
                        return android.hardware.camera2.params.MeteringRectangle[].class;
                    default:
                        if (typeStr.contains("Range")) return android.util.Range.class;
                        return Object.class;
                }
            } catch (ClassNotFoundException e) {
                return Object.class;
            }
        }

        private String formatValue(Object val) {
            if (val == null) return "null";
            if (val.getClass().isArray()) {
                int length = java.lang.reflect.Array.getLength(val);
                if (length == 0) return "[]";
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < length; i++) {
                    Object item = java.lang.reflect.Array.get(val, i);
                    String formatted = formatSingleValue(item);
                    // Add newline for complex objects in arrays
                    if (length > 1 && formatted.contains("{")) sb.append("\n  ");
                    sb.append(formatted);
                    if (i < length - 1) sb.append(", ");
                }
                if (sb.toString().contains("\n")) sb.append("\n]");
                else sb.append("]");
                return sb.toString();
            }
            if (val instanceof java.util.Collection) {
                java.util.Collection<?> col = (java.util.Collection<?>) val;
                if (col.isEmpty()) return "[]";
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                for (Object item : col) {
                    if (!first) sb.append(", ");
                    String formatted = formatValue(item);
                    if (col.size() > 1 && formatted.contains("{")) sb.append("\n  ");
                    sb.append(formatted);
                    first = false;
                }
                if (sb.toString().contains("\n")) sb.append("\n]");
                else sb.append("]");
                return sb.toString();
            }
            return formatSingleValue(val);
        }

        private String formatSingleValue(Object val) {
            if (val == null) return "null";
            String toString = val.toString();
            // If it's the default Object.toString(), use reflection to show fields
            if (toString.startsWith(val.getClass().getName() + "@")) {
                StringBuilder sb = new StringBuilder(val.getClass().getSimpleName()).append("{");
                boolean first = true;
                for (java.lang.reflect.Field f : val.getClass().getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                    try {
                        f.setAccessible(true);
                        if (!first) sb.append(", ");
                        String name = f.getName();
                        // Clean up internal names like mWidth -> width
                        if (name.startsWith("m") && name.length() > 1 && Character.isUpperCase(name.charAt(1))) {
                            name = Character.toLowerCase(name.charAt(1)) + name.substring(2);
                        }
                        sb.append(name).append("=").append(formatValue(f.get(val)));
                        first = false;
                    } catch (Exception ignored) {}
                }
                return sb.append("}").toString();
            }
            return toString;
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
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
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
        private boolean tunablePreferencesGenerated = false;

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

            Preference vendorKeysSettingsButton = findPreference("vendor_keys_settings_screen");
            if (vendorKeysSettingsButton != null) {
                vendorKeysSettingsButton.setOnPreferenceClickListener(preference -> {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.settings_container, new VendorKeysSettingsFragment())
                            .addToBackStack(null)
                            .commit();
                    return true;
                });
            }

            Preference deviceInfoButton = findPreference("device_info_screen");
            if (deviceInfoButton != null) {
                deviceInfoButton.setOnPreferenceClickListener(preference -> {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.settings_container, new DeviceInfoFragment())
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

            String rootKey = getArguments() != null ? getArguments().getString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT) : null;
            if ("pref_tunable_submenu".equals(rootKey))
            {
                Log.d("SettingsFragment", "Inside Stacking or Tunable container, preparing dynamic prefs");
                generateTunablePreferences();
            }

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

        private void generateTunablePreferences() {
            // Only generate once per fragment instance
            if (tunablePreferencesGenerated) {
                Log.d("SettingsActivity", "Tunable preferences already generated, skipping");
                return;
            }
            tunablePreferencesGenerated = true;
            Log.d("SettingsActivity", "=== generateTunablePreferences called ===");
            Log.d("SettingsActivity", "Context: " + (mContext != null ? "OK" : "NULL"));
            Log.d("SettingsActivity", "PreferenceScreen: " + (getPreferenceScreen() != null ? "OK" : "NULL"));

            try {
                // Ensure tunable classes are registered
                com.particlesdevs.photoncamera.settings.TunableSettingsManager.ensureTunableClassesRegistered();

                // Register with TunablePreferenceGenerator for UI generation
                for (Class<?> clazz : com.particlesdevs.photoncamera.settings.TunableRegistry.TUNABLE_CLASSES) {
                    TunablePreferenceGenerator.registerTunableClass(clazz);
                }

                Log.d("SettingsActivity", "Registered classes, now generating preferences...");

                PreferenceScreen screen = getPreferenceScreen();
                Log.d("SettingsActivity", "Target PreferenceScreen: " + screen.getKey() + " (count before: " + screen.getPreferenceCount() + ")");

                // Generate preferences and add to screen
                TunablePreferenceGenerator.generatePreferences(mContext, screen);

                Log.d("SettingsActivity", "Generated preferences (count after: " + screen.getPreferenceCount() + ")");

                // Add reset button for tunable preferences
                addTunableResetButton();

                Log.d("SettingsActivity", "=== generateTunablePreferences completed (final count: " + screen.getPreferenceCount() + ") ===");
            } catch (Exception e) {
                Log.e("SettingsActivity", "ERROR in generateTunablePreferences", e);
                e.printStackTrace();
            }
        }

        private void addTunableResetButton() {
            try {
                // When we're inside the tunable submenu fragment, getPreferenceScreen() IS the tunable submenu
                androidx.preference.PreferenceScreen tunableSubmenu = getPreferenceScreen();

                if (tunableSubmenu != null) {
                    Log.d("SettingsActivity", "Adding reset button to tunable submenu (preferenceCount before: " + tunableSubmenu.getPreferenceCount() + ")");

                    // Create reset button preference
                    androidx.preference.Preference resetButton = new androidx.preference.Preference(mContext);
                    resetButton.setKey("pref_reset_tunable_settings");
                    resetButton.setTitle("Reset All to Defaults");
                    resetButton.setSummary("Reset all tunable parameters to their default values");
                    resetButton.setIcon(android.R.drawable.ic_menu_revert);
                    resetButton.setOrder(9999); // Force to the end

                    resetButton.setOnPreferenceClickListener(preference -> {
                        // Reset all tunable settings
                        com.particlesdevs.photoncamera.settings.TunableSettingsManager.resetAllToDefaults(mContext);

                        // Restart the settings activity to refresh UI
                        if (getActivity() != null) {
                            getActivity().recreate();
                        }

                        com.particlesdevs.photoncamera.app.PhotonCamera.showToast("Tunable settings reset to defaults");
                        return true;
                    });

                    tunableSubmenu.addPreference(resetButton);
                    Log.d("SettingsActivity", "Added reset button (preferenceCount after: " + tunableSubmenu.getPreferenceCount() + ")");

                    androidx.preference.Preference spacer = new androidx.preference.Preference(mContext);
                    spacer.setSelectable(false);
                    spacer.setKey("pref_spacer_bottom");
                    spacer.setTitle("");
                    spacer.setSummary("");
                    spacer.setOrder(10000);

                    tunableSubmenu.addPreference(spacer);
                } else {
                    Log.w("SettingsActivity", "PreferenceScreen is null, cannot add reset button");
                }
            } catch (Exception e) {
                Log.e("SettingsActivity", "Error adding reset button", e);
            }
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
                restorePref.setSummary(mContext.getString(R.string.restore_summary_json));
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
                    backupPref.setSummary(mContext.getString(R.string.backup_summary_json));
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

        private void setFetchConfigurationsPref() {
            Preference fetchPref = findPreference(mContext.getString(R.string.pref_fetch_configurations_key));
            if (fetchPref != null) {
                fetchPref.setOnPreferenceClickListener(preference -> {
                    preference.setSummary(mContext.getString(R.string.fetch_configurations_summary) + " (fetching�)");
                    new Thread(() -> {
                        supportedDevice.fetchFromNetwork();
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            preference.setSummary(mContext.getString(R.string.fetch_configurations_summary));
                            com.google.android.material.snackbar.Snackbar.make(
                                    activity.findViewById(android.R.id.content),
                                    "Device configurations updated. Restart to apply camera changes.",
                                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                            ).show();
                        });
                    }
                    }).start();
                    return true;
                });
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
            // Guard against null key (can happen during preference restore)
            if (key == null) {
                return;
            }
            
            Log.d("SettingsFragment", "onSharedPreferenceChanged: key=" + key);
            
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
            if (key.equalsIgnoreCase(PreferenceKeys.Key.KEY_HIDE_GALLERY_ICON.mValue)) {
                Log.d("SettingsFragment", "Hide gallery icon changed, expected key: " + PreferenceKeys.Key.KEY_HIDE_GALLERY_ICON.mValue);
                try {
                    boolean hideIcon = mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, PreferenceKeys.Key.KEY_HIDE_GALLERY_ICON);
                    Log.d("SettingsFragment", "Hide gallery icon value: " + hideIcon);
                    toggleGalleryIconVisibility(hideIcon);
                } catch (Exception e) {
                    Log.e("SettingsFragment", "Error toggling gallery icon: " + e.getMessage());
                    e.printStackTrace();
                }
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

        private void toggleGalleryIconVisibility(boolean hideIcon) {
            try {
                // Get the ComponentName for the activity-alias using explicit package name
                String packageName = mContext.getPackageName();
                ComponentName galleryLauncher = new ComponentName(
                        packageName,
                        packageName + ".gallery.ui.GalleryActivityLauncher"
                );
                
                // Get the package manager
                PackageManager pm = mContext.getPackageManager();
                
                // Set the component enabled state based on hideIcon preference
                // If hideIcon is true, disable the launcher icon; otherwise enable it
                int newState = hideIcon ? 
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED : 
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                
                Log.d("SettingsFragment", "Toggling gallery icon visibility:");
                Log.d("SettingsFragment", "  hideIcon=" + hideIcon);
                Log.d("SettingsFragment", "  newState=" + newState);
                Log.d("SettingsFragment", "  component=" + galleryLauncher);
                
                pm.setComponentEnabledSetting(
                        galleryLauncher,
                        newState,
                        PackageManager.DONT_KILL_APP
                );
                
                Log.d("SettingsFragment", "Component state changed successfully");
                
                // Show a message to user
                if (activity != null) {
                    String message = hideIcon ? 
                            "Gallery icon will be hidden from launcher" : 
                            "Gallery icon will be visible in launcher";
                    activity.runOnUiThread(() -> 
                            com.google.android.material.snackbar.Snackbar.make(
                                    activity.findViewById(android.R.id.content),
                                    message,
                                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                            ).show()
                    );
                }
            } catch (Exception e) {
                Log.e("SettingsFragment", "Error in toggleGalleryIconVisibility: " + e.getMessage());
                e.printStackTrace();
                // Show error message to user
                if (activity != null) {
                    activity.runOnUiThread(() -> 
                            com.google.android.material.snackbar.Snackbar.make(
                                    activity.findViewById(android.R.id.content),
                                    "Error toggling gallery icon: " + e.getMessage(),
                                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                            ).show()
                    );
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
            // Log which preference was clicked
            Log.d("SettingsFragment", "onPreferenceTreeClick: " + preference.getKey());
            
            // Handle tunable submenu click manually to ensure proper navigation
            if ("pref_tunable_submenu".equals(preference.getKey())) {
                Log.d("SettingsFragment", "Tunable submenu clicked, navigating...");
                
                // Navigate to the submenu (preferences will be generated in the new fragment's onCreate)
                if (preference instanceof PreferenceScreen) {
                    PreferenceScreen screen = (PreferenceScreen) preference;
                    if (activity instanceof SettingsActivity) {
                        ((SettingsActivity) activity).onPreferenceStartScreen(this, screen);
                        return true;
                    }
                }
            }
            
            // Return false to allow default handling (like opening other subscreens)
            return super.onPreferenceTreeClick(preference);
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
