package com.particlesdevs.photoncamera.settings;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import com.particlesdevs.photoncamera.util.Log;

import androidx.annotation.StringRes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.app.PhotonCamera;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Vibhor 06/09/2020
 */
public class PreferenceKeys {
    public static final String SCOPE_GLOBAL = SettingsManager.SCOPE_GLOBAL;
    private static final String TAG = "PreferenceKeys";
    private static final Set<String> COMMON_KEYS = new HashSet<>();
    private static final String PER_LENS_KEY_PREFIX = "settings_for_camera_";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static PreferenceKeys preferenceKeys;

    static {
        COMMON_KEYS.add(Key.CAMERA_ID.mValue);
        COMMON_KEYS.add(Key.KEY_SAVE_PER_LENS_SETTINGS.mValue);
        COMMON_KEYS.add(Key.KEY_SHOW_AF_DATA.mValue);
        COMMON_KEYS.add(Key.KEY_THEME_ACCENT.mValue);
        COMMON_KEYS.add(Key.KEY_THEME.mValue);
        COMMON_KEYS.add(Key.KEY_SHOW_GRID.mValue);
        COMMON_KEYS.add(Key.KEY_SHOW_WATERMARK.mValue);
        COMMON_KEYS.add(Key.KEY_SHOW_ROUND_EDGE.mValue);
        COMMON_KEYS.add(Key.KEY_CAMERA_SOUNDS.mValue);
        COMMON_KEYS.add(Key.KEY_SHOW_GRADIENT.mValue);
        COMMON_KEYS.add(Key.KEY_AF_MODE.mValue);
        COMMON_KEYS.add(Key.KEY_AE_MODE.mValue);
        COMMON_KEYS.add(Key.CAMERA_MODE.mValue);
        COMMON_KEYS.add(Key.KEY_SAVE_RAW.mValue);
        COMMON_KEYS.add(Key.KEY_PREVIEW_FORMAT.mValue);
        COMMON_KEYS.add(Key.KEY_REAL_PREVIEW_FORMAT.mValue);
        COMMON_KEYS.add(Key.KEY_RAW_FORMAT.mValue);
        // QualityDoesMatter - General
        COMMON_KEYS.add(Key.KEY_APERTURE.mValue);
        COMMON_KEYS.add(Key.KEY_EXTEND_ISO.mValue);
        COMMON_KEYS.add(Key.KEY_EXTEND_EXPOSURE.mValue);
        COMMON_KEYS.add(Key.KEY_COUNTDOWN_TIMER.mValue);
        COMMON_KEYS.add(Key.KEY_OIS_ON.mValue);
        COMMON_KEYS.add(Key.KEY_DNG_COMPRESSION_ON.mValue);
        COMMON_KEYS.add(Key.KEY_SHOW_BASIC_OSD.mValue);
        COMMON_KEYS.add(Key.KEY_SINGLE_FRAME_QUALITY.mValue);
        COMMON_KEYS.add(Key.KEY_USE_EXTERNAL_GALLERY.mValue);
        // QualityDoesMatter - Video
        COMMON_KEYS.add(Key.KEY_HDR_VIDEO.mValue);
        COMMON_KEYS.add(Key.KEY_EIS_VIDEO.mValue);
        COMMON_KEYS.add(Key.KEY_10BIT_VIDEO.mValue);
        COMMON_KEYS.add(Key.KEY_NEW_REC_VIDEO.mValue);
        COMMON_KEYS.add(Key.KEY_VIDEO_BITRATE_SEEKBAR.mValue);
        COMMON_KEYS.add(Key.KEY_VIDEO_CODEC.mValue);
        COMMON_KEYS.add(Key.KEY_VIDEO_FRAMERATE.mValue);
        COMMON_KEYS.add(Key.KEY_VIDEO_HEIGHT.mValue);
        COMMON_KEYS.add(Key.KEY_KEYFRAME_INTERVAL.mValue);
        // QualityDoesMatter - Audio
        COMMON_KEYS.add(Key.KEY_AUDIO_PROCESSING.mValue);
        COMMON_KEYS.add(Key.KEY_AUDIO_CODEC.mValue);
        COMMON_KEYS.add(Key.KEY_AUDIO_CHANNELS.mValue);
        COMMON_KEYS.add(Key.KEY_AUDIO_BITRATE.mValue);
        COMMON_KEYS.add(Key.KEY_AUDIO_SPS.mValue);
        // QualityDoesMatter - SoC - Qualcomm/Snapdragon
        COMMON_KEYS.add(Key.KEY_SOC_QUALCOMM_SHARPNESS.mValue);
        COMMON_KEYS.add(Key.KEY_SOC_QUALCOMM_SATURATION.mValue);
        COMMON_KEYS.add(Key.KEY_SOC_QUALCOMM_EIS_MODE.mValue);
        COMMON_KEYS.add(Key.KEY_SOC_QUALCOMM_AI_MODE.mValue);
        COMMON_KEYS.add(Key.KEY_SOC_QUALCOMM_ISZ.mValue);
        COMMON_KEYS.add(Key.KEY_SOC_QUALCOMM_MFNR.mValue);
        // QualityDoesMatter - Single Shot & Video Related
        COMMON_KEYS.add(Key.KEY_USE_ZSL.mValue);
        COMMON_KEYS.add(Key.KEY_USE_SCENE_AND_EFFECT_MODE.mValue);
        COMMON_KEYS.add(Key.KEY_USE_NEW_SETTINGS_GLOBAL.mValue);
        COMMON_KEYS.add(Key.KEY_NOISE_PROCESSING.mValue);
        COMMON_KEYS.add(Key.KEY_EDGE_PROCESSING.mValue);
        COMMON_KEYS.add(Key.KEY_2X_ZOOM.mValue);
        COMMON_KEYS.add(Key.KEY_DIGITAL_ZOOM_FACTOR.mValue);
        COMMON_KEYS.add(Key.KEY_CONTRAST_CURVE.mValue);
        COMMON_KEYS.add(Key.KEY_EFFECT_MODE.mValue);
        // QualityDoesMatter - Sensor Related (and more)
        COMMON_KEYS.add(Key.KEY_HOT_PIXEL_MODE.mValue);
        COMMON_KEYS.add(Key.KEY_COLOR_CORRECTION_ABERRATION_MODE.mValue);
        COMMON_KEYS.add(Key.KEY_DISTORTION_CORRECTION_MODE.mValue);
        COMMON_KEYS.add(Key.KEY_SHADING_MODE.mValue);
        COMMON_KEYS.add(Key.KEY_ALTERNATE_PREVIEW_TEMPLATE.mValue);
    }

    private final SettingsManager settingsManager;

    private PreferenceKeys(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    public static void initialise(SettingsManager settingsManager) {
        preferenceKeys = new PreferenceKeys(settingsManager);
    }
    public static void setDefaults(Context context) {
        SettingsManager settingsManager = preferenceKeys.settingsManager;
        Resources resources = context.getResources();

        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_HDRX, resources.getBoolean(R.bool.pref_hdrx_mode_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_EIS_PHOTO, resources.getBoolean(R.bool.pref_eis_photo_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_QUAD_BAYER, resources.getBoolean(R.bool.pref_quad_bayer_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_REMOSAIC, resources.getBoolean(R.bool.pref_remosaic_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_FPS_PREVIEW, resources.getBoolean(R.bool.pref_fps_preview_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_THUMBNAIL, resources.getBoolean(R.bool.pref_thumbnail_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_AE_MODE, resources.getString(R.string.pref_ae_mode_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.CAMERA_MODE, resources.getString(R.string.pref_camera_mode_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_COUNTDOWN_TIMER, 0);
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_BRACKETING_MODE, 0); // Default to disable bracketing
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_PREVIEW_FORMAT, resources.getString(R.string.pref_preview_format_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_REAL_PREVIEW_FORMAT, resources.getString(R.string.pref_real_preview_format_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_RAW_FORMAT, resources.getString(R.string.pref_raw_format_default));
        settingsManager.setDefaults(Key.CAMERA_ID, resources.getString(R.string.camera_id_default), new String[]{"0", "1"});
        settingsManager.setDefaults(Key.TONEMAP, resources.getString(R.string.tonemap_default), new String[]{resources.getString(R.string.tonemap_default)});
        settingsManager.setDefaults(Key.GAMMA, resources.getString(R.string.gamma_default), new String[]{resources.getString(R.string.gamma_default)});

        // QualityDoesMatter - General
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_OIS_ON, resources.getBoolean(R.bool.pref_ois_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_DNG_COMPRESSION_ON, resources.getBoolean(R.bool.pref_dng_compression_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_SHOW_BASIC_OSD, resources.getBoolean(R.bool.pref_show_basic_osd_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_EXTEND_ISO, resources.getBoolean(R.bool.pref_extend_iso_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_EXTEND_EXPOSURE, resources.getBoolean(R.bool.pref_extend_exposure_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_APERTURE, resources.getString(R.string.pref_aperture_default_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_SINGLE_FRAME_QUALITY, resources.getString(R.string.pref_single_frame_quality_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_USE_EXTERNAL_GALLERY, resources.getBoolean(R.bool.pref_use_external_gallery_default));
        // QualityDoesMatter - Video
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_EIS_VIDEO, resources.getBoolean(R.bool.pref_eis_video_def_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_HDR_VIDEO, resources.getBoolean(R.bool.pref_hdr_video_def_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_10BIT_VIDEO, resources.getBoolean(R.bool.pref_10bit_video_def_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_NEW_REC_VIDEO, resources.getBoolean(R.bool.pref_new_rec_video_def_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_VIDEO_CODEC, resources.getString(R.string.pref_codec_default_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_VIDEO_FRAMERATE, resources.getString(R.string.pref_video_framerate_default_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_VIDEO_HEIGHT, resources.getString(R.string.pref_video_resolution_default_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_VIDEO_BITRATE_SEEKBAR, resources.getString(R.string.pref_bitrate_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_KEYFRAME_INTERVAL, resources.getString(R.string.pref_keyframe_interval_default_value));
        // QualityDoesMatter - Audio
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_AUDIO_PROCESSING, resources.getString(R.string.pref_audio_processing_default_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_AUDIO_CODEC, resources.getString(R.string.pref_audio_codec_default_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_AUDIO_CHANNELS, resources.getString(R.string.pref_audio_channels_default_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_AUDIO_BITRATE, resources.getString(R.string.pref_audio_bitrate_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_AUDIO_SPS, resources.getString(R.string.pref_sps_default_value));
        // QualityDoesMatter - SoC - Qualcomm/Snapdragon
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_SOC_QUALCOMM_SHARPNESS, resources.getString(R.string.pref_soc_qualcomm_sharpness_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_SOC_QUALCOMM_SATURATION, resources.getString(R.string.pref_soc_qualcomm_saturation_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_SOC_QUALCOMM_EIS_MODE, resources.getString(R.string.pref_soc_qualcomm_eis_mode_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_SOC_QUALCOMM_AI_MODE, resources.getString(R.string.pref_soc_qualcomm_ai_mode_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_SOC_QUALCOMM_ISZ, resources.getBoolean(R.bool.pref_soc_qualcomm_isz_default));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_SOC_QUALCOMM_MFNR, resources.getBoolean(R.bool.pref_soc_qualcomm_mfnr_default));
        // QualityDoesMatter - Single Shot and Video Related
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_USE_ZSL, resources.getBoolean(R.bool.pref_zsl_def_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_USE_SCENE_AND_EFFECT_MODE, resources.getBoolean(R.bool.pref_scene_and_effect_mode_def_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_USE_NEW_SETTINGS_GLOBAL, resources.getBoolean(R.bool.pref_new_settings_global_def_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_2X_ZOOM, resources.getBoolean(R.bool.pref_2x_zoom_def_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_NOISE_PROCESSING, resources.getString(R.string.pref_noise_processing_default_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_EDGE_PROCESSING, resources.getString(R.string.pref_edge_processing_default_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_DIGITAL_ZOOM_FACTOR, resources.getString(R.string.pref_digital_zoom_factor_default_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_CONTRAST_CURVE, resources.getString(R.string.pref_contrast_curve_default_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_EFFECT_MODE, resources.getString(R.string.pref_effect_mode_default_value));
        // QualityDoesMatter - Sensor Related and More
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_HOT_PIXEL_MODE, resources.getString(R.string.pref_hot_pixel_mode_default_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_COLOR_CORRECTION_ABERRATION_MODE, resources.getString(R.string.pref_color_correction_aberration_mode_default_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_DISTORTION_CORRECTION_MODE, resources.getString(R.string.pref_distortion_correction_mode_default_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_SHADING_MODE, resources.getString(R.string.pref_shading_mode_default_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_ALTERNATE_PREVIEW_TEMPLATE, resources.getBoolean(R.bool.pref_alternate_preview_template_default_value));
        settingsManager.setInitial(SCOPE_GLOBAL, Key.KEY_TEN_BIT_SURFACE_TARGET, resources.getString(R.string.pref_ten_bit_surface_target_default_value));

        settingsManager.addListener((settingsManager1, key) -> {
            if (isPerLensSettingsOn()) {
                if (key.equals(Key.CAMERA_ID.mValue)) {
                    loadSettingsForCamera(getCameraID());
                }
                if (!COMMON_KEYS.contains(key)) {
                    saveJsonForCamera(getCameraID());
                }
            }
            PhotonCamera.getSettings().loadCache();
            //Log.d(TAG, key + " : changed!");
        });
    }
    public static void addIds(String[] ids){
        if(ids != null) {
            SettingsManager settingsManager = preferenceKeys.settingsManager;
            Log.d(TAG, "Added IDS:" + Arrays.toString(ids));
            settingsManager.setDefaults(Key.CAMERA_ID, ids[0], ids);
            Map<String, ?> map = settingsManager.getDefaultPreferences().getAll();
            map.keySet().removeAll(COMMON_KEYS);
            String json = GSON.toJson(map);
            for (String cameraId : ids) { //Makes a copy of default settings for each camera
                settingsManager.setInitial(Key.PER_LENS_FILE_NAME.mValue, PER_LENS_KEY_PREFIX + cameraId, json);
            }
        }
    }

    private static void saveJsonForCamera(String cameraID) {
        SettingsManager settingsManager = preferenceKeys.settingsManager;
        Map<String, ?> map = settingsManager.getDefaultPreferences().getAll();
        map.keySet().removeAll(COMMON_KEYS);
        String hashmapAsJson = GSON.toJson(map);
        String alreadySavedJSON = settingsManager.getString(Key.PER_LENS_FILE_NAME.mValue, PER_LENS_KEY_PREFIX + cameraID, "");
        if (!alreadySavedJSON.equals(hashmapAsJson)) {
            settingsManager.set(Key.PER_LENS_FILE_NAME.mValue, PER_LENS_KEY_PREFIX + getCameraID(), hashmapAsJson);
//            Log.d(TAG, PER_LENS_KEY_PREFIX + getCameraID() + " : JSON : " + hashmapAsJson);
        }
    }

    public static void loadSettingsForCamera(String cameraID) {
        SettingsManager settingsManager = preferenceKeys.settingsManager;
        String alreadySavedJSON = settingsManager.getString(Key.PER_LENS_FILE_NAME.mValue, PER_LENS_KEY_PREFIX + cameraID, null);
        HashMap<String, ?> map = GSON.fromJson(alreadySavedJSON, HashMap.class);
        for (Map.Entry<String, ?> e : map.entrySet()) {
            settingsManager.set(SCOPE_GLOBAL, e.getKey(), e.getValue().toString());
        }
    }

    public static void setActivityTheme(Activity activity) {
        Map<String, Integer> map = new HashMap<>();
        map.put("default", 0);
        map.put("red", R.style.RedTheme);
        map.put("blue", R.style.BlueTheme);
        map.put("orange", R.style.OrangeTheme);
        map.put("green", R.style.GreenTheme);
        map.put("eszdman", R.style.EszdmanTheme);
        map.put("pink", R.style.PinkTheme);
        map.put("cyan", R.style.CyanTheme);
        map.put("teal", R.style.TealTheme);

        SettingsManager sm = preferenceKeys.settingsManager;

        String theme = sm.getString(SCOPE_GLOBAL, Key.KEY_THEME_ACCENT, activity.getResources().getString(R.string.pref_theme_accent_default_value));
        boolean showGradient = sm.getBoolean(SCOPE_GLOBAL, Key.KEY_SHOW_GRADIENT, activity.getResources().getBoolean(R.bool.pref_show_gradient_def_value));

        if (showGradient) {
            activity.getTheme().applyStyle(R.style.GradientBackgroundTheme, true);
        }
        if (theme != null) {
            Integer themeRes = map.get(theme.toLowerCase());
            activity.getTheme().applyStyle(themeRes == null ? 0 : themeRes, true);
        }

    }

    /**
     * Helper functions for some keys defined in PreferenceFragment.
     */
    public static boolean isAfDataOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_SHOW_AF_DATA);
    }

     public static boolean isRemosaicOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_REMOSAIC);
    }

    public static boolean isDisableAligningOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_DISABLE_ALIGNINIG);
    }

    public static boolean isShowWatermarkOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_SHOW_WATERMARK);
    }

    public static boolean isPerLensSettingsOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_SAVE_PER_LENS_SETTINGS);
    }

    public static boolean isEnhancedProcessionOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_ENHANCED_PROCESSING);
    }

    public static boolean isHdrxNrOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_HDRX_NR);
    }

    public static int isSaveRaw() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_SAVE_RAW);
    }

    public static boolean isBatterySaverOn(){
        return getBool(PreferenceKeys.Key.KEY_ENERGY_SAVING);
    }
    public static boolean isAspect169On(){
        return getBool(Key.KEY_WIDE169);
    }

    public static boolean isThumbnailOn(){
        return getBool(Key.KEY_THUMBNAIL);
    }

    public static void setBatterySaver(boolean value) {
        preferenceKeys.settingsManager.set(SCOPE_GLOBAL, Key.KEY_ENERGY_SAVING,value);
    }
    public static void setSaveRaw(int value) {
        preferenceKeys.settingsManager.set(SCOPE_GLOBAL, Key.KEY_SAVE_RAW,value);
    }

    public static boolean isRoundEdgeOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_SHOW_ROUND_EDGE);
    }

    public static boolean isHdrVideoOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_HDR_VIDEO);
    }

    public static boolean isEisInPreviewVideoOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_EIS_VIDEO);
    }

    public static boolean is10bitVideoOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_10BIT_VIDEO);
    }
    public static boolean isNeRecVideoOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_NEW_REC_VIDEO);
    }

    public static String getMode() {
        return preferenceKeys.settingsManager.getString(SCOPE_GLOBAL, Key.KEY_SHOW_ROUND_EDGE);
    }

    public static int getGridValue() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_SHOW_GRID);
    }

    public static void setGridValue(int value) {
        preferenceKeys.settingsManager.set(SCOPE_GLOBAL, Key.KEY_SHOW_GRID, value);
    }

    public static boolean isCameraSoundsOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_CAMERA_SOUNDS);
    }

    public static int getChromaNrValue() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_CHROMA_NR_SEEKBAR);
    }

    public static int getLumaNrValue() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_LUMA_NR_SEEKBAR);
    }

    public static int getFrameCountValue() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_FRAME_COUNT);
    }

    public static float getSharpnessValue() {
        return preferenceKeys.settingsManager.getFloat(SCOPE_GLOBAL, Key.KEY_SHARPNESS_SEEKBAR);
    }

    public static float getCompressorValue() {
        return preferenceKeys.settingsManager.getFloat(SCOPE_GLOBAL, Key.KEY_COMPRESSOR_SEEKBAR);
    }

    public static float getGainValue() {
        return preferenceKeys.settingsManager.getFloat(SCOPE_GLOBAL, Key.KEY_GAIN_SEEKBAR);
    }

    public static float getSaturationValue() {
        return preferenceKeys.settingsManager.getFloat(SCOPE_GLOBAL, Key.KEY_SATURATION_SEEKBAR);
    }

    public static float getContrastValue() {
        return preferenceKeys.settingsManager.getFloat(SCOPE_GLOBAL, Key.KEY_CONTRAST_SEEKBAR);
    }

    public static int getAlignMethodValue() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_ALIGN_METHOD);
    }

    public static int getColorMethodValue() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_COLOR_METHOD);
    }

    public static int getFocusPeakValue() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_FOCUS_PEAK);
    }

    public static int getPreviewFormatValue() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_PREVIEW_FORMAT);
    }

    public static int getRealPreviewFormatValue() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_REAL_PREVIEW_FORMAT);
    }

    public static int getRawFormatValue() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_RAW_FORMAT);
    }


    public static int getCFAValue() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_CFA);
    }

    public static int getThemeValue() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_THEME);
    }

    /**
     * Helper functions for other keys such as viewfinder buttons, etc.
     */
    public static boolean isHdrXOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_HDRX);
    }

    public static void setHdrX(boolean value) {
        preferenceKeys.settingsManager.set(SCOPE_GLOBAL, Key.KEY_HDRX, value);
    }

    public static boolean isEisPhotoOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_EIS_PHOTO);
    }

    public static void setEisPhoto(boolean value) {
        preferenceKeys.settingsManager.set(SCOPE_GLOBAL, Key.KEY_EIS_PHOTO, value);
    }

    public static boolean isFpsPreviewOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_FPS_PREVIEW);
    }

    public static void setFpsPreview(boolean value) {
        preferenceKeys.settingsManager.set(SCOPE_GLOBAL, Key.KEY_FPS_PREVIEW, value);
    }

    public static boolean isQuadBayerOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_QUAD_BAYER);
    }

    public static void setQuadBayer(boolean value) {
        preferenceKeys.settingsManager.set(SCOPE_GLOBAL, Key.KEY_QUAD_BAYER, value);
    }

    public static String getCameraID() {
        return preferenceKeys.settingsManager.getString(Key.CAMERAS_PREFERENCE_FILE_NAME.mValue, Key.CAMERA_ID);
    }

    public static int getCountdownTimerIndex() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_COUNTDOWN_TIMER);
    }

    public static void setCountdownTimerIndex(int valueMS) {
        preferenceKeys.settingsManager.set(SCOPE_GLOBAL, Key.KEY_COUNTDOWN_TIMER, valueMS);
    }

    public static int getBracketingMode() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_BRACKETING_MODE);
    }

    public static void setBracketingMode(int value) {
        preferenceKeys.settingsManager.set(SCOPE_GLOBAL, Key.KEY_BRACKETING_MODE, value);
    }

    public static void setCameraID(String value) {
        preferenceKeys.settingsManager.set(Key.CAMERAS_PREFERENCE_FILE_NAME.mValue, Key.CAMERA_ID, value);
    }

    public static int getAfMode() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_AF_MODE);
    }

    public static int getAeMode() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_AE_MODE);
    }

    public static void setAeMode(int value) {
        preferenceKeys.settingsManager.set(SCOPE_GLOBAL, Key.KEY_AE_MODE, value);
    }

    public static int getCameraModeOrdinal() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.CAMERA_MODE);
    }

    public static void setCameraModeOrdinal(int value) {
        preferenceKeys.settingsManager.set(SCOPE_GLOBAL, Key.CAMERA_MODE, value);
    }

    public static String getToneMap() {
        return preferenceKeys.settingsManager.getString(SCOPE_GLOBAL, Key.TONEMAP);
    }

    public static String getPref(Key key) {
        return preferenceKeys.settingsManager.getString(SCOPE_GLOBAL, key);
    }

    public static Set<String> getStringSet(Key key) {
        return preferenceKeys.settingsManager.getStringSet(SCOPE_GLOBAL, key,new HashSet<>(0));
    }

    public static boolean getBool(Key key) {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, key);
    }

    public static float getFloat(Key key) {
        return preferenceKeys.settingsManager.getFloat(SCOPE_GLOBAL, key);
    }

    public static int getInteger(Key key) {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, key);
    }

    // QualityDoesMatter
    public static boolean useExtendIsoOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_EXTEND_ISO);
    }

    public static boolean useExtendExposureOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_EXTEND_EXPOSURE);
    }
    public static boolean useBasicOsdOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_SHOW_BASIC_OSD);
    }

    public static boolean useOisOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_OIS_ON);
    }

    public static boolean useDngCompression() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_DNG_COMPRESSION_ON);
    }

    public static int getVideoBitrate() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_VIDEO_BITRATE_SEEKBAR);
    }

    public static float getAperture() {
        return preferenceKeys.settingsManager.getFloat(SCOPE_GLOBAL, Key.KEY_APERTURE);
    }

    public static String getVideoCodec() {
        return preferenceKeys.settingsManager.getString(SCOPE_GLOBAL, Key.KEY_VIDEO_CODEC);
    }

    public static int getNoiseProcessing() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_NOISE_PROCESSING);
    }

    public static boolean isNoiseProcessingOn() {
        if (preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_NOISE_PROCESSING) == 0) {
            return false;
        }
        else {
            return true;
        }
    }

    public static boolean isEdgeProcessingOn() {
        if (preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_EDGE_PROCESSING) == 0) {
            return false;
        }
        else {
            return true;
        }
    }

    public static boolean isZoomOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_2X_ZOOM);
    }

    public static void setSetZoomOn(boolean value) {
        preferenceKeys.settingsManager.set(SCOPE_GLOBAL, Key.KEY_2X_ZOOM, value);
    }

    public static void setSetNoiseProcessing(int value) {
        preferenceKeys.settingsManager.set(SCOPE_GLOBAL, Key.KEY_NOISE_PROCESSING, value);
    }

    public static int getEdgeProcessing() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_EDGE_PROCESSING);
    }

    public static void setSetEdgeProcessing(int value) {
        preferenceKeys.settingsManager.set(SCOPE_GLOBAL, Key.KEY_EDGE_PROCESSING, value);
    }

    public static int getAudioProcessing() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_AUDIO_PROCESSING);
    }

    public static String getAudioProcessingStr() {
        return preferenceKeys.settingsManager.getString(SCOPE_GLOBAL, Key.KEY_AUDIO_PROCESSING);
    }

    public static int getAudioCodec() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_AUDIO_CODEC);
    }

    public static String getAudioCodecStr() {
        return preferenceKeys.settingsManager.getString(SCOPE_GLOBAL, Key.KEY_AUDIO_CODEC);
    }

    public static int getAudioChannels() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_AUDIO_CHANNELS);
    }

    public static int getAudioBitrate() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_AUDIO_BITRATE);
    }

    public static int getAudioSps() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_AUDIO_SPS);
    }

    public static int getVideoFramerate() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_VIDEO_FRAMERATE);
    }

    public static int getVideoHeight() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_VIDEO_HEIGHT);
    }

    public static int isSystemNrOn() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_ENABLE_SYSTEM_NR);
    }

    public static int getSingleFrameQualityValue() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_SINGLE_FRAME_QUALITY);
    }

    public static int getSocQualcommSharpness() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_SOC_QUALCOMM_SHARPNESS);
    }

    public static int getSocQualcommSaturation() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_SOC_QUALCOMM_SATURATION);
    }

    public static int getSocQualcommEisMode() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_SOC_QUALCOMM_EIS_MODE);
    }

    public static int getSocQualcommAiMode() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_SOC_QUALCOMM_AI_MODE);
    }

    public static boolean isSocQualcommIszOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_SOC_QUALCOMM_ISZ);
    }

    public static boolean isSocQualcommMfnrOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_SOC_QUALCOMM_MFNR);
    }

    public static boolean isZslOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_USE_ZSL);
    }

    public static boolean isSceneAndEffectModeOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_USE_SCENE_AND_EFFECT_MODE);
    }

    public static boolean isNewSettingsGlobalOn() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_USE_NEW_SETTINGS_GLOBAL);
    }

    public static int getHotPixelMode() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_HOT_PIXEL_MODE);
    }

    public static int getColorCorrectionAberrationMode() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_COLOR_CORRECTION_ABERRATION_MODE);
    }

    public static int getDistortionCorrectionMode() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_DISTORTION_CORRECTION_MODE);
    }

    public static int getShadingMode() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_SHADING_MODE);
    }

    public static boolean useAlternatePreviewTemplate() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_ALTERNATE_PREVIEW_TEMPLATE);
    }

    public static String getTenBitSurfaceTarget() {
        return preferenceKeys.settingsManager.getString(SCOPE_GLOBAL, Key.KEY_TEN_BIT_SURFACE_TARGET);
    }

    public static float getDigitalZoomFactorValue() {
        return preferenceKeys.settingsManager.getFloat(SCOPE_GLOBAL, Key.KEY_DIGITAL_ZOOM_FACTOR);
    }

    public static boolean useExternalGallery() {
        return preferenceKeys.settingsManager.getBoolean(SCOPE_GLOBAL, Key.KEY_USE_EXTERNAL_GALLERY);
    }

    public static String getContrastCurve() {
        return preferenceKeys.settingsManager.getString(SCOPE_GLOBAL, Key.KEY_CONTRAST_CURVE);
    }

    public static int getEffectMode() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_EFFECT_MODE);
    }

    public static int getKeyframeInterval() {
        return preferenceKeys.settingsManager.getInteger(SCOPE_GLOBAL, Key.KEY_KEYFRAME_INTERVAL);
    }

    public enum Key {
        KEY_PREF_VERSION(R.string._pref_version),
        KEY_ENABLE_SYSTEM_NR(R.string.pref_enable_system_nr_key),
        KEY_SAVE_PER_LENS_SETTINGS(R.string.pref_save_per_lens_settings),
        KEY_DISABLE_ALIGNINIG(R.string.pref_disable_aligning_key),
        KEY_SHOW_WATERMARK(R.string.pref_show_watermark_key),
        KEY_ENERGY_SAVING(R.string.pref_energy_safe_key),
        KEY_WIDE169(R.string.pref_wide169_key),
        KEY_THUMBNAIL(R.string.pref_thumbnail_key),
        KEY_ENHANCED_PROCESSING(R.string.pref_enhanced_processing_key),
        KEY_HDRX_NR(R.string.pref_hdrx_nr_key),
        KEY_SHOW_ROUND_EDGE(R.string.pref_show_roundedge_key),
        KEY_SHOW_GRID(R.string.pref_show_grid_key),
        KEY_CAMERA_SOUNDS(R.string.pref_camera_sounds_key),
        KEY_CHROMA_NR_SEEKBAR(R.string.pref_chroma_nr_seekbar_key),
        KEY_LUMA_NR_SEEKBAR(R.string.pref_luma_nr_seekbar_key),
        KEY_COMPRESSOR_SEEKBAR(R.string.pref_compressor_seekbar_key),
        KEY_NOISESTR_SEEKBAR(R.string.pref_noise_seekbar_key),
        KEY_MERGE_SEEKBAR(R.string.pref_merge_seekbar_key),
        KEY_GAIN_SEEKBAR(R.string.pref_gain_seekbar_key),
        KEY_SHADOWS_SEEKBAR(R.string.pref_shadows_seekbar_key),
        KEY_FRAME_COUNT(R.string.pref_frame_count_key),
        KEY_CONTRAST_SEEKBAR(R.string.pref_contrast_seekbar_key),
        KEY_SHARPNESS_SEEKBAR(R.string.pref_sharpness_seekbar_key),
        KEY_EXPOCOMPENSATE_SEEKBAR(R.string.pref_expocompensation_seekbar_key),
        KEY_SATURATION_SEEKBAR(R.string.pref_saturation_seekbar_key),
        KEY_ALIGN_METHOD(R.string.pref_align_method_key),
        KEY_COLOR_METHOD(R.string.pref_color_method_key),
        KEY_FOCUS_PEAK(R.string.pref_peak_method_key),
        KEY_PREVIEW_FORMAT(R.string.pref_preview_format_key),
        KEY_REAL_PREVIEW_FORMAT(R.string.pref_real_preview_format_key),
        KEY_RAW_FORMAT(R.string.pref_raw_format_key),
        KEY_TELEGRAM(R.string.pref_telegram_channel_key),
        KEY_CONTRIBUTORS(R.string.pref_contributors_key),
        KEY_THEME(R.string.pref_theme_key),
        KEY_THEME_ACCENT(R.string.pref_theme_accent_key),
        KEY_SHOW_GRADIENT(R.string.pref_show_gradient_key),
        KEY_AF_MODE(R.string.pref_af_mode_key),
        KEY_AE_MODE(R.string.pref_ae_mode_key),
        KEY_BRACKETING_MODE(R.string.pref_bracketing_key),
        KEY_COUNTDOWN_TIMER(R.string.pref_countdown_timer_key),
        KEY_USE_EXTERNAL_GALLERY(R.string.pref_use_external_gallery_key),

        /**
         * QualityDoesMatter - Video settings keys
         */
        KEY_HDR_VIDEO(R.string.pref_hdr_video_key),
        KEY_EIS_VIDEO(R.string.pref_eis_video_key),
        KEY_10BIT_VIDEO(R.string.pref_10bit_video_key),
        KEY_NEW_REC_VIDEO(R.string.pref_new_rec_video_key),
        KEY_VIDEO_HEIGHT(R.string.pref_video_resolution_key),
        KEY_VIDEO_BITRATE_SEEKBAR(R.string.pref_bitrate_key),
        KEY_VIDEO_FRAMERATE(R.string.pref_video_framerate_key),
        KEY_VIDEO_CODEC(R.string.pref_codec_key),
        KEY_NOISE_PROCESSING(R.string.pref_noise_processing_key),
        KEY_EDGE_PROCESSING(R.string.pref_edge_processing_key),
        KEY_2X_ZOOM(R.string.pref_2x_zoom_key),
        KEY_KEYFRAME_INTERVAL(R.string.pref_keyframe_interval_key),

        /**
         * QualityDoesMatter - Audio settings keys
         */
        KEY_AUDIO_PROCESSING(R.string.pref_audio_processing_key),
        KEY_AUDIO_CODEC(R.string.pref_audio_codec_key),
        KEY_AUDIO_CHANNELS(R.string.pref_audio_channels_key),
        KEY_AUDIO_BITRATE(R.string.pref_audio_bitrate_key),
        KEY_AUDIO_SPS(R.string.pref_sps_key),

        /**
         * QualityDoesMatter - SoC - Qualcomm/Snapdragon
         */
        KEY_SOC_QUALCOMM_SHARPNESS(R.string.pref_soc_qualcomm_sharpness_key),
        KEY_SOC_QUALCOMM_SATURATION(R.string.pref_soc_qualcomm_saturation_key),
        KEY_SOC_QUALCOMM_EIS_MODE(R.string.pref_soc_qualcomm_eis_mode_key),
        KEY_SOC_QUALCOMM_AI_MODE(R.string.pref_soc_qualcomm_ai_mode_key),
        KEY_SOC_QUALCOMM_ISZ(R.string.pref_soc_qualcomm_isz_key),
        KEY_SOC_QUALCOMM_MFNR(R.string.pref_soc_qualcomm_mfnr_key),

        /**
         * QualityDoesMatter - other
         */
        KEY_SINGLE_FRAME_QUALITY(R.string.pref_single_frame_quality_key),
        KEY_APERTURE(R.string.pref_aperture_key),
        KEY_SHOW_BASIC_OSD(R.string.pref_show_basic_osd_key),
        KEY_EXTEND_ISO(R.string.pref_extend_iso_key),
        KEY_EXTEND_EXPOSURE(R.string.pref_extend_exposure_key),
        KEY_OIS_ON(R.string.pref_ois_key),
        KEY_DNG_COMPRESSION_ON(R.string.pref_dng_compression_key),

        /**
         * QualityDoesMatter - Single Shot & Video Related
         */
        KEY_USE_ZSL(R.string.pref_zsl_key),
        KEY_USE_SCENE_AND_EFFECT_MODE(R.string.pref_scene_and_effect_mode_key),
        KEY_USE_NEW_SETTINGS_GLOBAL(R.string.pref_new_settings_global_key),
        KEY_DIGITAL_ZOOM_FACTOR(R.string.pref_digital_zoom_factor_key),
        KEY_CONTRAST_CURVE(R.string.pref_contrast_curve_key),
        KEY_EFFECT_MODE(R.string.pref_effect_mode_key),

        /**
         * QualityDoesMatter - Sensor Related (and more)
         */
        KEY_HOT_PIXEL_MODE(R.string.pref_hot_pixel_mode_key),
        KEY_COLOR_CORRECTION_ABERRATION_MODE(R.string.pref_color_correction_aberration_mode_key),
        KEY_DISTORTION_CORRECTION_MODE(R.string.pref_distortion_correction_mode_key),
        KEY_SHADING_MODE(R.string.pref_shading_mode_key),
        KEY_ALTERNATE_PREVIEW_TEMPLATE(R.string.pref_alternate_preview_template_key),
        KEY_TEN_BIT_SURFACE_TARGET(R.string.pref_ten_bit_surface_target_key),

        /**
         * Enhanced settings keys
         */
        KEY_PREVIEW_RESOLUTION(R.string.pref_preview_resolution_key),////TODO add preview resolution selector
        KEY_SHOW_AF_DATA(R.string.pref_show_afdata_key),
        KEY_SAVE_RAW(R.string.pref_save_raw_key),
        KEY_CFA(R.string.pref_cfa_key),
        KEY_REMOSAIC(R.string.pref_remosaic_key),////TODO

        /**
         * Other Keys
         */
        KEY_HDRX(R.string.pref_hdrx_key),
        KEY_EIS_PHOTO(R.string.pref_eis_photo_key),
        KEY_MODE(R.string.pref_eis_photo_key),
        KEY_QUAD_BAYER(R.string.pref_quad_bayer_key),
        KEY_FPS_PREVIEW(R.string.pref_fps_preview_key),
        CAMERA_ID(R.string.camera_id),
        TONEMAP(R.string.tonemap_key),
        GAMMA(R.string.gamma_key),
        CAMERA_MODE(R.string.pref_camera_mode_key),

        /* CameraManager 2 keys */
        CAMERAS_PREFERENCE_FILE_NAME(R.string._cameras),
        ALL_CAMERA_IDS_KEY(R.string.all_camera_ids),
        FRONT_IDS_KEY(R.string.front_camera_ids),
        BACK_IDS_KEY(R.string.back_camera_ids),
        ALL_CAMERA_LENS_KEY(R.string.all_camera_lens),
        CAMERA_COUNT_KEY(R.string.all_camera_count),

        /* SupportedDevice keys */
        DEVICES_PREFERENCE_FILE_NAME(R.string._devices),
        ALL_DEVICES_NAMES_KEY(R.string.all_devices_names),
        /**
         * Per Lens File
         */
        PER_LENS_FILE_NAME(R.string._per_lens),
        FOLDERS_LIST(R.string.pref_folders_list);
        public final String mValue;

        Key(@StringRes int stringId) {
            mValue = PhotonCamera.getStringStatic(stringId);
        }
    }
}
