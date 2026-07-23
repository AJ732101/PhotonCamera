package com.particlesdevs.photoncamera.api;

import android.annotation.SuppressLint;
import android.hardware.camera2.CaptureRequest;

import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.util.Log;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class VendorTagUtils {
    public static CaptureRequest.Key<Integer> SELECT_PRIORITY = new CaptureRequest.Key<>("org.codeaurora.qcamera3.iso_exp_priority.select_priority", Integer.class);
    public static CaptureRequest.Key<Integer> USE_ISO_VALUE = new CaptureRequest.Key<>("org.codeaurora.qcamera3.iso_exp_priority.use_iso_value", Integer.class);
    public static CaptureRequest.Key<Long> ISO_EXP = new CaptureRequest.Key<>("org.codeaurora.qcamera3.iso_exp_priority.use_iso_exp_priority", Long.class);
    public static CameraCharacteristics.Key<int[]> ISO_AVAILABLE_MODES = new CameraCharacteristics.Key<>("org.codeaurora.qcamera3.iso_exp_priority.iso_available_modes", int[].class);
    public static CameraCharacteristics.Key<long[]> EXPOSURE_RANGE = new CameraCharacteristics.Key<>("org.codeaurora.qcamera3.iso_exp_priority.exposure_time_range", long[].class);
    public static CameraCharacteristics.Key<Integer> support_insensor_zoom = new CameraCharacteristics.Key<>("org.quic.camera.swcapabilities.inSensorZoomCapability", Integer.class);
    private static CaptureRequest.Key<Float> TONE_MAPPING_DARK_BOOST = new CaptureRequest.Key<>("org.codeaurora.qcamera3.tmcusercontrol.dark_boost_offset", Float.class);
    private static CaptureRequest.Key<Integer> USE_ISO_VALUE_MT = new CaptureRequest.Key<>("com.mediatek.3afeature.aeIsoSpeed", Integer.class);
    public static final CaptureRequest.Key<Byte> histMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.histogram.enable", byte.class);
    public static final CaptureRequest.Key<Byte> bgStatsMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.bayer_grid.enable", byte.class);
    public static final CaptureRequest.Key<Byte> beStatsMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.bayer_exposure.enable", byte.class);
    public static CaptureResult.Key<Integer> buckets = new CaptureResult.Key<>("org.codeaurora.qcamera3.histogram.buckets", Integer.class);
    public static CaptureResult.Key<Integer> maxCount = new CaptureResult.Key<>("org.codeaurora.qcamera3.histogram.max_count", Integer.class);
    public static CaptureResult.Key<Integer> stats_type = new CaptureResult.Key<>("org.codeaurora.qcamera3.histogram.stats_type",Integer.class);
    public static CaptureResult.Key<int[]> histogramStats = new CaptureResult.Key<>("org.codeaurora.qcamera3.histogram.stats", int[].class);

    private static final String TAG = "VendorTagUtils";
    public static final HashMap<String, Integer> KEY_ISO_INDEX = new HashMap<String, Integer>();

    public static boolean isSupported(CaptureRequest.Builder builder, CaptureRequest.Key<?> key) {
        boolean supported = true;
        try {
            builder.get(key);
        }catch(IllegalArgumentException exception){
            supported = false;
            Log.w(TAG,key.getName() + " is NOT supported");
        }
        if (supported) {
            Log.d(TAG,key.getName() + " is supported");
        }
        return supported;
    }

    public static List<String> getSupportedIso(CameraCharacteristics cameraCharacteristics) {
        KEY_ISO_INDEX.clear();
        KEY_ISO_INDEX.put("auto", 0);
        KEY_ISO_INDEX.put("deblur", 1);
        KEY_ISO_INDEX.put("100", 2);
        KEY_ISO_INDEX.put("200", 3);
        KEY_ISO_INDEX.put("400", 4);
        KEY_ISO_INDEX.put("800", 5);
        KEY_ISO_INDEX.put("1600", 6);
        KEY_ISO_INDEX.put("3200", 7);
        List<String> supportedIso = new ArrayList<>();
        try {
            int[] range = cameraCharacteristics.get(ISO_AVAILABLE_MODES);

            if (range != null) {
                for (int iso : range) {
                    for (String key : KEY_ISO_INDEX.keySet()) {
                        if (KEY_ISO_INDEX.get(key).equals(iso)) {
                            supportedIso.add(key);
                        }
                    }
                }
            } else {
                Log.w(TAG, "Supported ISO priority modes is null.");
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "IllegalArgumentException Supported ISO_AVAILABLE_MODES is wrong.");
        }

        Log.d(TAG, "Supported ISO priority modes: " + supportedIso.toString());
        return supportedIso;
    }

    public static void setIsoExpPrioritySelectPriority(CaptureRequest.Builder builder, Integer value) {
        if (isIsoExpPrioritySelectPrioritySupported(builder)) {
            builder.set(SELECT_PRIORITY, value);
        }
    }

    private static boolean isIsoExpPrioritySelectPrioritySupported(CaptureRequest.Builder builder) {
        return isSupported(builder, SELECT_PRIORITY);
    }

    public static void setIsoExpPriority(CaptureRequest.Builder builder, Long value) {
        if (isIsoExpPrioritySupported(builder)) {
            builder.set(ISO_EXP, value);
        }
    }

    public static void setUseIsoValues(CaptureRequest.Builder builder, int value) {
        if (isUseIsoValueSupported(builder)) {
            builder.set(USE_ISO_VALUE, value);
        }
        else {
            if (isSupported(builder, USE_ISO_VALUE_MT)) {
                builder.set(USE_ISO_VALUE_MT, value);
            }
        }
    }

    private static boolean isIsoExpPrioritySupported(CaptureRequest.Builder builder) {
        return isSupported(builder, ISO_EXP);
    }

    private static boolean isUseIsoValueSupported(CaptureRequest.Builder builder) {
        return isSupported(builder, USE_ISO_VALUE);
    }

    public static void setToneMappingDarkBoostValue(CaptureRequest.Builder builder, float value) {
        if (isSupported(builder, TONE_MAPPING_DARK_BOOST)) {
            builder.set(TONE_MAPPING_DARK_BOOST, value);
        }
    }

    @SuppressLint({"NewApi", "LocalSuppress"})
    public static void builderSessionApply2(CameraCharacteristics cameraCharacteristics, CaptureRequest.Builder builder, boolean burst, boolean useMaximumResolutionKey) {

    }

    private static void resetFlags() {
        PhotonCamera.hasIszKey = false;
        PhotonCamera.hasSaturationKey = false;
        PhotonCamera.hasContrastKey = false;
        PhotonCamera.hasSharpnessKey = false;
        PhotonCamera.hasEisModeKey = false;
        PhotonCamera.hasLtmKey = false;
        PhotonCamera.hasAiModeKey = false;
        PhotonCamera.hasMfnrKey = false;
        PhotonCamera.hasXiaomiNight = false;
        PhotonCamera.hasXiaomiSuperNight = false;
        PhotonCamera.hasXiaomiHdr = false;
        PhotonCamera.hasXiaomiUltraHdr = false;
        PhotonCamera.hasXiaomiAiAutoSceneDetection = false;
        PhotonCamera.hasXiaomiProVideoLog = false;
        PhotonCamera.hasXiaomiProVideoMovie = false;
        PhotonCamera.hasXiaomiReMosaic = false;
        PhotonCamera.hasXiaomiQuadCfa = false;
        PhotonCamera.hasXiaomiSuperResolution = false;
        PhotonCamera.hasVivoZeissColor = false;
        PhotonCamera.hasVivoProMode = false;
        PhotonCamera.hasVivoSensorMode = false;
        PhotonCamera.hasVivoDistortionCorrection = false;
        PhotonCamera.hasQucommAdrcOff = false;
        PhotonCamera.hasEisRealtime = false;
        PhotonCamera.hasEisLookAhead = false;
        PhotonCamera.hasEisV3 = false;
        PhotonCamera.hasIdealRaw = false;
        PhotonCamera.hasAutoHdr = false;
        PhotonCamera.hasSocHdrMode = false;
        PhotonCamera.hasManualWb = false;
    }

    public static void applyIdealRaw(CaptureRequest.Builder builder, int bitDepth) {
        // Diese Keys sind die Qualcomm-Basis für das, was Xiaomi als URAW nutzt
        var qtiIdealRaw = new CaptureRequest.Key<>("com.qti.chi.rawcbinfo.IdealRaw", byte[].class);
        var qtiIdealRawSize = new CaptureRequest.Key<>("com.qti.chi.rawcbinfo.RawSize", byte[].class);

        if (isSupported(builder, qtiIdealRaw) && isSupported(builder, qtiIdealRawSize)) {
            byte mode;
            byte depth;

            // Bit-Tiefe Mapping für Xiaomi/Qualcomm
            switch (bitDepth) {
                case 14: mode = 0x03; depth = 14; break;
                case 12: mode = 0x02; depth = 12; break;
                default: mode = 0x01; depth = 10; break;
            }

            // Der "URAW"-Trigger: Type 1 (Ideal), Mode (Bit-Depth)
            byte[] rawType = new byte[]{
                    0x01, 0x00, 0x00, 0x00,
                    mode, 0x00, 0x00, 0x00
            };

            // Der Buffer-Size-Enforcer (Input/Output gleich setzen für URAW)
            byte[] rawSize = new byte[]{
                    depth, 0x00, 0x00, 0x00,
                    depth, 0x00, 0x00, 0x00
            };

            builder.set(qtiIdealRaw, rawType);
            builder.set(qtiIdealRawSize, rawSize);

            // WICHTIG: Auf Xiaomi Geräten oft zusätzlich nötig für echten URAW-Pfad:
            if (PhotonCamera.isXiaomi) {
                // Falls Xiaomi-spezifische URAW-Keys existieren (Vendor-Abhängig)
                var xiaomiUraw = new CaptureRequest.Key<>("com.xiaomi.stats.enableUraw", byte.class);
                if (isSupported(builder, xiaomiUraw)) {
                    builder.set(xiaomiUraw, (byte) 1);
                }
            }

            PhotonCamera.hasIdealRaw = true;
        }
    }


    @SuppressLint({"NewApi", "LocalSuppress"})
    public static void builderSessionApply(CameraCharacteristics cameraCharacteristics, CaptureRequest.Builder builder, boolean burst, boolean useMaximumResolutionKey, boolean isPreview) {
        try {
            PhotonCamera.isSamsung = Build.BRAND.equalsIgnoreCase("samsung");
            PhotonCamera.isGoogle = Build.BRAND.equalsIgnoreCase("google");
            PhotonCamera.isZte = Build.BRAND.equalsIgnoreCase("zte");
            PhotonCamera.isMotorola = Build.BRAND.equalsIgnoreCase("motorola");
            PhotonCamera.isXiaomi = Build.BRAND.equalsIgnoreCase("xiaomi") || Build.BRAND.equalsIgnoreCase("redmi") || Build.BRAND.equalsIgnoreCase("poco");
            PhotonCamera.isOppo = Build.BRAND.equalsIgnoreCase("oppo");
            PhotonCamera.isVivo = Build.BRAND.equalsIgnoreCase("vivo");
            PhotonCamera.isOnePlus = Build.BRAND.equalsIgnoreCase("oneplus");
            PhotonCamera.isHonor = Build.BRAND.equalsIgnoreCase("honor");
            PhotonCamera.isHuawei = Build.BRAND.equalsIgnoreCase("huawei");
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }

        resetFlags();

        try {
            if (!PhotonCamera.getSettings().disableVendorKeys) {
                var demosaicMode = new CaptureRequest.Key<>("android.demosaic.mode", Byte.class);
                if (isSupported(builder, demosaicMode)) {
                    // 0 = OFF, 1 = FAST, 2 = HIGH_QUALITY
                    if (PhotonCamera.getSpecific().specificSetting.androidDemosaicMode >= 0) {
                        builder.set(demosaicMode, (byte) PhotonCamera.getSpecific().specificSetting.androidDemosaicMode);
                    }
                }

                byte enable = 1;
                if (PhotonCamera.isXiaomi) {
                    var clientName = new CaptureRequest.Key<>("com.xiaomi.sessionparams.clientName", String.class);
                    if (isSupported(builder, clientName)) {
                        builder.set(clientName, "com.android.camera");
                    }

                    var algoNightMode = new CaptureRequest.Key<>("com.xiaomi.algo.nightModeEnable", byte.class);
                    if (isSupported(builder, algoNightMode)) {
                        PhotonCamera.hasXiaomiNight = true;
                        if (PhotonCamera.isNightModeOn) {
                            builder.set(algoNightMode, (byte) 1);
                        }
                    }

                    var algoHdrMode = new CaptureRequest.Key<>("com.xiaomi.algo.hdrMode", byte.class);
                    if (isSupported(builder, algoHdrMode)) {
                        PhotonCamera.hasXiaomiHdr = true;
                        if (PhotonCamera.isHdrOn) {
                            builder.set(algoHdrMode, (byte) PhotonCamera.getSpecific().specificSetting.xiaomiHdrMode);
                        }
                    }

                    var xiaomiSuperResRaw = new CaptureRequest.Key<>("xiaomi.superResolution.rawEnabled", byte.class);
                    if (isSupported(builder, xiaomiSuperResRaw)) {
                        if (PhotonCamera.isSuperResOn) {
                            builder.set(xiaomiSuperResRaw, (byte) 1);
                        }
                    }

                    var xiaomiIszQuadRaw = new CaptureRequest.Key<>("xiaomi.superResolution.IszQuadRawEnabled", byte.class);
                    if (isSupported(builder, xiaomiIszQuadRaw)) {
                        if (PhotonCamera.isSuperResOn) {
                            builder.set(xiaomiIszQuadRaw, (byte) 1);
                        }
                    }

                    var xiaomiHdr = new CaptureRequest.Key<>("xiaomi.hdr.enabled", byte.class);
                    if (isSupported(builder, xiaomiHdr)) {
                        PhotonCamera.hasXiaomiHdr = true;
                        if (PhotonCamera.isHdrOn) {
                            builder.set(xiaomiHdr, (byte) 1);
                        }
                    }

                    var xiaomiHdrCheckerEnabled = new CaptureRequest.Key<>("xiaomi.hdr.hdrChecker.enabled", byte.class);
                    if (isSupported(builder, xiaomiHdrCheckerEnabled)) {
                        if (PhotonCamera.isHdrOn) {
                            builder.set(xiaomiHdr, (byte) 1);
                        }
                    }

                    var xiaomiHdrChecker = new CaptureRequest.Key<>("xiaomi.hdr.hdrChecker", byte.class);
                    if (isSupported(builder, xiaomiHdrChecker)) {
                        if (PhotonCamera.isHdrOn) {
                            builder.set(xiaomiHdr, (byte) 1);
                        }
                    }

                    var xiaomiUiHdrLabel = new CaptureRequest.Key<>("xiaomi.hdr.isUIHDRLabelEnabled", byte.class);
                    if (isSupported(builder, xiaomiUiHdrLabel)) {
                        if (PhotonCamera.isHdrOn) {
                            builder.set(xiaomiUiHdrLabel, (byte) 1);
                        }
                    }

                    var xiaomiSrHdr = new CaptureRequest.Key<>("xiaomi.hdr.sr.enabled", byte.class);
                    if (isSupported(builder, xiaomiSrHdr)) {
                        if (PhotonCamera.isHdrOn) {
                            builder.set(xiaomiSrHdr, (byte) 1);
                        }
                    }

                    var xiaomiRawHdr = new CaptureRequest.Key<>("xiaomi.hdr.raw.enabled", byte.class);
                    if (isSupported(builder, xiaomiRawHdr)) {
                        //builder.set(xiaomiRawHdr, PhotonCamera.isHdrOn ? (byte)1 : (byte)0);
                    }

                    var remosaicEnabled = new CaptureRequest.Key<>("xiaomi.remosaic.enabled", byte.class);
                    if (isSupported(builder, remosaicEnabled)) {
                        PhotonCamera.hasXiaomiReMosaic = true;
                        if (PhotonCamera.isRemosaicOn) {
                            builder.set(remosaicEnabled, (byte) 1);
                        }
                    }

                    var quadcfaEnabled = new CaptureRequest.Key<>("xiaomi.quadcfa.enabled", byte.class);
                    if (isSupported(builder, quadcfaEnabled)) {
                        PhotonCamera.hasXiaomiQuadCfa = true;
                        if (PhotonCamera.isQuadCfaOn) {
                            builder.set(quadcfaEnabled, (byte) 1);
                        }
                    }

                    var xiaomiSuperRes = new CaptureRequest.Key<>("xiaomi.superResolution.enabled", byte.class);
                    if (isSupported(builder, xiaomiSuperRes)) {
                        PhotonCamera.hasXiaomiSuperResolution = true;
                        if (PhotonCamera.isSuperResOn) {
                            builder.set(xiaomiSuperRes, (byte) 1);
                        }
                    }

                    var proVideoLog = new CaptureRequest.Key<>("xiaomi.pro.video.log.enabled", byte.class);
                    if (isSupported(builder, proVideoLog)) {
                        PhotonCamera.hasXiaomiProVideoLog = true;
                        if (PhotonCamera.isProVideoLogOn) {
                            builder.set(proVideoLog, (byte) 1);
                        }
                    }

                    var proVideoMovie = new CaptureRequest.Key<>("xiaomi.pro.video.movie.enabled", byte.class);
                    if (isSupported(builder, proVideoMovie)) {
                        PhotonCamera.hasXiaomiProVideoMovie = true;
                        if (PhotonCamera.isProVideoLogMovie) {
                            builder.set(proVideoMovie, (byte) 1);
                        }
                    }

                    var aiAutoSceneDetection = new CaptureRequest.Key<>("xiaomi.ai.asd.enabled", byte.class);
                    if (isSupported(builder, aiAutoSceneDetection)) {
                        PhotonCamera.hasXiaomiAiAutoSceneDetection = true;
                        if (PhotonCamera.isAiAutoSceneDetectionOn) {
                            builder.set(aiAutoSceneDetection, (byte) 1);
                        }
                    }

                    var aiSceneDetection = new CaptureRequest.Key<>("xiaomi.ai.misd.enabled", byte.class);
                    if (isSupported(builder, aiSceneDetection)) {
                        PhotonCamera.hasXiaomiAiAutoSceneDetection = true;
                        if (PhotonCamera.isAiAutoSceneDetectionOn) {
                            builder.set(aiSceneDetection, (byte) 1);
                        }
                    }

                    var aiUltraRaw = new CaptureRequest.Key<>("xiaomi.ai.asd.UltraRawChecker", byte.class);
                    if (isSupported(builder, aiUltraRaw)) {
                        if (PhotonCamera.isAiAutoSceneDetectionOn) {
                            builder.set(aiUltraRaw, (byte) 1);
                        }
                    }

                    var supernightEnabled = new CaptureRequest.Key<>("xiaomi.supernight.enabled", byte.class);
                    if (isSupported(builder, supernightEnabled)) {
                        PhotonCamera.hasXiaomiSuperNight = true;
                        if (PhotonCamera.isSuperNightModeOn) {
                            builder.set(supernightEnabled, (byte) 1);
                        }
                    }

                    if (PhotonCamera.getSpecific().specificSetting.xiaomiSupernightMode > 0) {
                        var supernightMode = new CaptureRequest.Key<>("xiaomi.supernight.mode", byte.class);
                        if (isSupported(builder, supernightMode)) {
                            builder.set(supernightMode, (byte) PhotonCamera.getSpecific().specificSetting.xiaomiSupernightMode);
                        }
                    }

                    var ultraHdrEnabled = new CaptureRequest.Key<>("com.xiaomi.ultraHDR.enabled", byte.class);
                    if (isSupported(builder, ultraHdrEnabled)) {
                        PhotonCamera.hasXiaomiUltraHdr = true;
                        if (PhotonCamera.isUltraHdrOn) {
                            builder.set(ultraHdrEnabled, (byte) 1);

                            var ultraHdrLinear = new CaptureRequest.Key<>("com.xiaomi.ultraHDR.linearFrame", byte.class);
                            if (isSupported(builder, ultraHdrLinear)) {
                                builder.set(ultraHdrLinear, (byte) 0);
                            }

                            var residualGain = new CaptureRequest.Key<>("com.xiaomi.ultraHDR.residualGain", Float.class);
                            if (isSupported(builder, residualGain)) {
                                builder.set(residualGain, 2.0f);
                            }

                            var ultraHdrMetadata = new CaptureRequest.Key<>("com.xiaomi.ultraHDR.metadata", byte.class);
                            if (isSupported(builder, ultraHdrMetadata)) {
                                builder.set(ultraHdrMetadata, (byte) 1);
                            }

                            var ultraHdrEvInfo = new CaptureRequest.Key<>("com.xiaomi.ultraHDR.evInfo", byte.class);
                            if (isSupported(builder, ultraHdrEvInfo)) {
                                builder.set(ultraHdrEvInfo, (byte) 1);
                            }
                        }
                    }

                    if (false) {
                        // Definition der Qualcomm Tuning Keys
                        var FEATURE_1_MODE = new CaptureRequest.Key<>("org.quic.camera2.tuning.feature.Feature1Mode", Byte.class);
                        var FEATURE_2_MODE = new CaptureRequest.Key<>("org.quic.camera2.tuning.feature.Feature2Mode", Byte.class);
                        var SCENE_MODE = new CaptureRequest.Key<>("org.quic.camera2.tuning.feature.SceneMode", Byte.class);

                        builder.set(FEATURE_1_MODE, (byte) 13);
                        builder.set(FEATURE_2_MODE, (byte) 41);
                        builder.set(SCENE_MODE, (byte) 40);
                    }

                    float apertureToUse = PhotonCamera.getSettings().apertureToUse;
                    if (apertureToUse < 16) {
                        boolean isSupportedGoogle = false;
                        boolean isSupportedXiaomi = false;

                        var apertureMode = new CaptureRequest.Key<>("com.xiaomi.lens.apertureMode", Integer.class);
                        if (isSupported(builder, apertureMode)) {
                            builder.set(apertureMode, 1); // 1 = enable, 0 = disable
                        }

                        var lensApertureXiaomi = new CaptureRequest.Key<>("com.xiaomi.lens.aperture", Float.class);
                        if (isSupported(builder, lensApertureXiaomi)) {
                            isSupportedXiaomi = true;
                        }
                        var lensApertureAndroid = new CaptureRequest.Key<>("android.lens.aperture", Float.class);
                        if (isSupported(builder, lensApertureAndroid)) {
                            isSupportedGoogle = true;
                        }
                        var lensAperture = new CaptureRequest.Key<>("com.xiaomi.sessionparams.initAperture", Float.class);
                        if (isSupported(builder, lensAperture)) {
                            CameraCharacteristics.Key<Float[]> vendorKey = new CameraCharacteristics.Key<>("com.xiaomi.lens.info.availableApertures", Float[].class);
                            Float[] apert = cameraCharacteristics.get(vendorKey);
                            if (apert != null && apert.length > 0) {
                                Log.d(TAG, "Available apertures: " + Arrays.toString(apert));
                                if (Arrays.asList(apert).contains(apertureToUse)) {
                                    Log.d(TAG, "Change aperture to: " + apertureToUse);
                                    builder.set(lensAperture, apertureToUse);
                                    if (isSupportedGoogle) {
                                        builder.set(CaptureRequest.LENS_APERTURE, apertureToUse);
                                    }
                                    if (isSupportedXiaomi) {
                                        builder.set(lensApertureXiaomi, apertureToUse);
                                    }
                                } else {
                                    Log.w(TAG, "Requested aperture " + apertureToUse + " is not supported, available: " + Arrays.toString(apert));
                                }
                            } else {
                                Log.w(TAG, "Requested aperture is not supported");
                            }
                        }
                    }
                }

                var enableInSensorZoomKey = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableInsensorZoom", Integer.class);
                if (isSupported(builder, enableInSensorZoomKey)) {
                    PhotonCamera.hasIszKey = true;
                    builder.set(enableInSensorZoomKey, PhotonCamera.getSettings().socQualcommUseIsz ? 1 : 0);
                }

                var useSaturation = new CaptureRequest.Key<>("org.codeaurora.qcamera3.saturation.use_saturation", Integer.class);
                if (isSupported(builder, useSaturation)) {
                    PhotonCamera.hasSaturationKey = true;
                    if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO) && PhotonCamera.getSettings().videoHDR) {
                        builder.set(useSaturation, PhotonCamera.getSettings().socQualcommSaturationHdrVideo);
                    } else {
                        builder.set(useSaturation, PhotonCamera.getSettings().socQualcommSaturation);
                    }
                }

                var useContrast = new CaptureRequest.Key<>("org.codeaurora.qcamera3.contrast.level", Integer.class);
                if (isSupported(builder, useContrast)) {
                    PhotonCamera.hasContrastKey = true;
                    if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO) && PhotonCamera.getSettings().videoHDR) {
                        builder.set(useContrast, PhotonCamera.getSettings().socQualcommContrastHdrVideo);
                    } else {
                        builder.set(useContrast, PhotonCamera.getSettings().socQualcommContrast);
                    }
                }

                var enableCinematicMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableCinematicMode", Integer.class);
                if (isSupported(builder, enableCinematicMode)) {
                    builder.set(enableCinematicMode, PhotonCamera.getSpecific().specificSetting.useCodeAuroraCinematicMode ? 1 : 0);
                }

                var enableIdealRAW1 = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableIdealRAW", byte.class);
                if (isSupported(builder, enableIdealRAW1)) {
                    PhotonCamera.hasIdealRaw = true;
                    if (PhotonCamera.isIdealRawOn) {
                        builder.set(enableIdealRAW1, (byte) 2);
                        applyIdealRaw(builder, 14);
                    }
                }

                var quicIspCntrLtm = new CaptureRequest.Key<>("org.quic.camera.ispcontrol.DisableBLTMDC", byte.class);
                if (isSupported(builder, quicIspCntrLtm)) {
                    //builder.set(quicIspCntrLtm, (byte) 1);
                }

                dcgControl(builder);
                videoStabilization(builder);

                var manualWb = new CaptureRequest.Key<>("org.codeaurora.qcamera3.manualWB.color_temperature", Integer.class);
                if (isSupported(builder, manualWb)) {
                    PhotonCamera.hasManualWb = true;
                    if (PhotonCamera.getSettings().socQualcommManualWb > 1500) {
                        var partialMwbMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.manualWB.partial_mwb_mode", Integer.class);
                        if (isSupported(builder, partialMwbMode)) {
                            // 0 = Off, 1 = CCT (Kelvin) Mode, 2 = Gains Mode
                            builder.set(partialMwbMode, 1);
                        }
                        builder.set(manualWb, PhotonCamera.getSettings().socQualcommManualWb);
                    }
                }

                var overrideResourceCostValidation = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.overrideResourceCostValidation", byte.class);
                if (isSupported(builder, overrideResourceCostValidation)) {
                    builder.set(overrideResourceCostValidation, (byte) 1);
                }

                var enableQLL = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.enableQLL", Integer.class);
                if (isSupported(builder, enableQLL)) {
                    builder.set(enableQLL, PhotonCamera.getSpecific().specificSetting.enableQLL ? 1 : 0);
                }

                var useStatsViszualize = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.enableStatsVisualizer", byte.class);
                if (isSupported(builder, useStatsViszualize)) {
                    //builder.set(useStatsViszualize, (byte) 1);
                }

                var sharpnessStrength = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sharpness.strength", Integer.class);
                if (isSupported(builder, sharpnessStrength)) {
                    PhotonCamera.hasSharpnessKey = true;
                    builder.set(sharpnessStrength, PhotonCamera.getSettings().socQualcommSharpness);
                }

                var aiMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.AICameraMode", Integer.class);
                if (isSupported(builder, aiMode)) {
                    PhotonCamera.hasAiModeKey = true;
                    builder.set(aiMode, PhotonCamera.getSettings().socQualcommAiMode);
                }

                var histMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.histogram.enable", byte.class);
                if (isSupported(builder, histMode)) {
                    builder.set(histMode, (byte) 1);
                }

                var gridMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.bayer_grid.enable", byte.class);
                if (isSupported(builder, gridMode)) {
                    builder.set(gridMode, (byte) 0);
                }

                var bayerStatsMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.bayer_exposure.enable", byte.class);
                if (isSupported(builder, bayerStatsMode)) {
                    builder.set(bayerStatsMode, (byte) 0);
                }

                // Qualcomm LTM - local tone mapping deactivation
                if (!PhotonCamera.getSettings().socQualcommLtmOff) {
                    var ltmDarkBoostStrength = new CaptureRequest.Key<>("org.quic.camera.ltmDynamicContrast.ltmDarkBoostStrength", Float.class);
                    if (isSupported(builder, ltmDarkBoostStrength)) {
                        PhotonCamera.hasLtmKey = true;
                        builder.set(ltmDarkBoostStrength, (float) -100.0f);
                    }

                    var ltmBrightBoostStrength = new CaptureRequest.Key<>("org.quic.camera.ltmDynamicContrast.ltmBrightBoostStrength", Float.class);
                    if (isSupported(builder, ltmBrightBoostStrength)) {
                        PhotonCamera.hasLtmKey = true;
                        builder.set(ltmBrightBoostStrength, (float) -100.0f);
                    }

                    var ltmContrastStrength = new CaptureRequest.Key<>("org.quic.camera.ltmDynamicContrast.ltmContrastStrength", Float.class);
                    if (isSupported(builder, ltmContrastStrength)) {
                        PhotonCamera.hasLtmKey = true;
                        builder.set(ltmContrastStrength, (float) -100.0f);
                    }

                    var ltmGamma = new CaptureRequest.Key<>("org.quic.camera.ltmDynamicContrast.ltmGamma", Float.class);
                    if (isSupported(builder, ltmGamma)) {
                        PhotonCamera.hasLtmKey = true;
                        builder.set(ltmGamma, (float) 1.0f);
                    }
                }

                // Qualcomm HDR Fusion / AI HDR deactivation
                if (false) {
                    var hdrMode = new CaptureRequest.Key<>("org.quic.camera.hdr.hdrMode", Integer.class);
                    if (isSupported(builder, hdrMode)) {
                        builder.set(hdrMode, (int) 2);
                    }

                    var hdrStrength = new CaptureRequest.Key<>("org.quic.camera.hdr.hdrStrength", Integer.class);
                    if (isSupported(builder, hdrStrength)) {
                        builder.set(hdrStrength, (int) 0);
                    }

                    var hdrEnable = new CaptureRequest.Key<>("org.quic.camera.hdr.hdrEnable", Integer.class);
                    if (isSupported(builder, hdrEnable)) {
                        builder.set(hdrEnable, (int) 0);
                    }
                }

                // Qualcomm AI‑Contrast / AI‑Scene / AI‑ToneMapping deactivation
                if (false) {
                    var aISnapshot = new CaptureRequest.Key<>("org.quic.camera.AICamera.EnableAISnapshot", byte.class);
                    if (isSupported(builder, aISnapshot)) {
                        builder.set(aISnapshot, (byte) 1);
                    }

                    var aIStrength = new CaptureRequest.Key<>("org.quic.camera.AICamera.AIStrength", Integer.class);
                    if (isSupported(builder, aIStrength)) {
                        builder.set(aIStrength, (int) 100);
                    }

                    var asdEnable = new CaptureRequest.Key<>("org.quic.camera.ai.asdEnable", Integer.class);
                    if (isSupported(builder, asdEnable)) {
                        builder.set(asdEnable, (int) 1);
                    }

                    var sceneDetect = new CaptureRequest.Key<>("org.quic.camera.ai.sceneDetect", Integer.class);
                    if (isSupported(builder, sceneDetect)) {
                        builder.set(sceneDetect, (int) 1);
                    }

                    var toneMapEnable = new CaptureRequest.Key<>("org.quic.camera.ai.toneMapEnable", Integer.class);
                    if (isSupported(builder, toneMapEnable)) {
                        builder.set(toneMapEnable, (int) 1);
                    }
                }

                noiseReduction(cameraCharacteristics, builder, isPreview);

                // set all HDR parameter to 0
                CaptureRequest.Key hdrMode = null;
                if (true) {
                    hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableSHDR", Integer.class);
                    if (isSupported(builder, hdrMode)) {
                        PhotonCamera.hasSocHdrMode = true;
                        builder.set(hdrMode, 0);
                    }

                    hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableQHDR", Integer.class);
                    if (isSupported(builder, hdrMode)) {
                        PhotonCamera.hasSocHdrMode = true;
                        builder.set(hdrMode, 0);
                    }

                    hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableMFHDR", Integer.class);
                    if (isSupported(builder, hdrMode)) {
                        PhotonCamera.hasSocHdrMode = true;
                        builder.set(hdrMode, 0);
                    }

                    hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.HDRMode", Integer.class);
                    if (isSupported(builder, hdrMode)) {
                        PhotonCamera.hasSocHdrMode = true;
                        builder.set(hdrMode, 0);
                    }
                }

                hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableAutoHDR", Integer.class);
                if (isSupported(builder, hdrMode)) {
                    PhotonCamera.hasAutoHdr = true;
                    builder.set(hdrMode, PhotonCamera.getSettings().socQualcommAutoHdr ? 1 : 0);
                }

                switch (PhotonCamera.getSettings().socQualcommHdrMode) {
                    case 1:
                        hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableMFHDR", Integer.class);
                        if (isSupported(builder, hdrMode)) {
                            builder.set(hdrMode, 1);
                        }
                        hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.numHDRexposure", Integer.class);
                        if (isSupported(builder, hdrMode)) {
                            builder.set(hdrMode, 3);
                        }
                        break;
                    case 2:
                        hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableSHDR", Integer.class);
                        if (isSupported(builder, hdrMode)) {
                            builder.set(hdrMode, 1);
                        }
                        hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.numHDRexposure", Integer.class);
                        if (isSupported(builder, hdrMode)) {
                            builder.set(hdrMode, 3);
                        }
                        break;
                    case 3:
                        hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableQHDR", Integer.class);
                        if (isSupported(builder, hdrMode)) {
                            builder.set(hdrMode, 1);
                        }
                        break;
                   default:
                        break;
                }

                var hdrPref = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.HDRModePreference", Integer.class);
                if (isSupported(builder, hdrPref)) {
                    //builder.set(hdrPref, 1);
                }

                if (PhotonCamera.getSpecific().specificSetting.sensorModes != null) {
                    for (String id : PhotonCamera.getSpecific().specificSetting.sensorModes) {
                        try {
                            String camID = "";
                            String sensorMode = "";
                            if (id.contains("-")) {
                                camID = id.split("-")[0];
                                sensorMode = id.split("-")[1];
                            }

                            if (PhotonCamera.getSettings().mCameraID.contains(camID)) {
                                var sensorModeKey = new CaptureRequest.Key<>(PhotonCamera.getSpecific().specificSetting.sensorModeKey, Integer.class);
                                if (isSupported(builder, sensorModeKey)) {
                                    PhotonCamera.hasQucommSensorMode = true;
                                    if (PhotonCamera.isQucommSensorModeOn && !sensorMode.equals("x")) {
                                        builder.set(sensorModeKey, Integer.valueOf(sensorMode));
                                    }
                                }
                                break;
                            }
                        } catch (Exception ignored) {

                        }
                    }
                }

                /*CaptureRequest.Key perfKey = new CaptureRequest.Key<>("com.qti.chi.enableadrcpath.enableADRCPath", Integer.class);
                if (isSupported(builder, perfKey)) {
                    PhotonCamera.hasQucommAdrcOff = true;
                    builder.set(perfKey, PhotonCamera.isQucommAdrcOff ? (byte) 0: (byte) 1);
                }*/

                CaptureRequest.Key perfKey = new CaptureRequest.Key<>("org.codeaurora.qcamera3.adrc.disable", byte.class);
                if (isSupported(builder, perfKey)) {
                    PhotonCamera.hasQucommAdrcOff = true;
                    builder.set(perfKey, PhotonCamera.isQucommAdrcOff ? (byte) 1: (byte) 0);
                } else {
                    perfKey = new CaptureRequest.Key<>("com.qti.stats.internal.perFrame.disableADRC", byte.class);
                    if (isSupported(builder, perfKey)) {
                        PhotonCamera.hasQucommAdrcOff = true;
                        builder.set(perfKey, PhotonCamera.isQucommAdrcOff ? (byte) 1: (byte) 0);
                    }
                }
                //builder.set(perfKey, (byte) 1);

                perfKey = new CaptureRequest.Key<>("org.quic.camera.pipelineControl.isDisableSinkNoBuffer", byte.class);
                if (isSupported(builder, perfKey)) {
                    if (PhotonCamera.isQucommAdrcOff) {
                        builder.set(perfKey, (byte) 1);
                    }
                }

                // ZTE specific
                if (PhotonCamera.isZte) {
                    /*var enableWatermark = new CaptureRequest.Key<>("com.zte.chi.watermark.enable", Integer.class);
                    if (isSupported(builder, enableWatermark)) {
                        builder.set(enableWatermark, 1);
                    }

                    var watermarkMode = new CaptureRequest.Key<>("com.zte.chi.watermark.mode", Integer.class);
                    if (isSupported(builder, watermarkMode)) {
                        builder.set(watermarkMode, 1);
                    }*/

                    if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                        var vidHenceEis = new CaptureRequest.Key<>("com.zte.camera.sessionParameters.vidhance_eis", Integer.class);
                        if (isSupported(builder, vidHenceEis)) {
                            builder.set(vidHenceEis, 1);
                        }
                    }
                }

                // Samsung specific
                if (PhotonCamera.isSamsung) {
                    var enableAIDenoiser = new CaptureRequest.Key<>("samsung.android.control.enableAIDenoiser", Integer.class);
                    if (isSupported(builder, enableAIDenoiser)) {
                        builder.set(enableAIDenoiser, 1);
                    }

                    /*var liveHdrMode = new CaptureRequest.Key<>("samsung.android.control.liveHdrMode", Integer.class);
                    if (isSupported(builder, liveHdrMode)) {
                        builder.set(liveHdrMode, (int)4);
                    }

                    var liveHdrLevel = new CaptureRequest.Key<>("samsung.android.control.liveHdrLevel", Integer.class);
                    if (isSupported(builder, liveHdrLevel)) {
                        builder.set(liveHdrLevel, (int)1);
                    }*/

                    /*var captureHint = new CaptureRequest.Key<>("samsung.android.control.captureHint", Integer.class);
                    if (isSupported(builder, captureHint)) {
                        builder.set(captureHint, (int)0);
                    }*/

                    /*var colorTemperature = new CaptureRequest.Key<>("samsung.android.control.colorTemperature", Integer.class);
                    if (isSupported(builder, colorTemperature)) {
                        builder.set(colorTemperature, (int)6000);
                    }*/

                    /*var flipMode = new CaptureRequest.Key<>("samsung.android.control.flipMode", Integer.class);
                    if (isSupported(builder, flipMode)) {
                        builder.set(flipMode, (int)1);
                    }*/
                }

                // Vivo specific
                if (PhotonCamera.isVivo) {
                    var cameraId = new CaptureRequest.Key<>("vivo.control.camera_id", Integer.class);
                    if (isSupported(builder, cameraId)) {
                        builder.set(cameraId, Integer.valueOf(PhotonCamera.getSettings().mCameraID));
                    }

                    var vivoIsProMode = new CaptureRequest.Key<>("vivo.control.is_pro_mode", Integer.class);
                    PhotonCamera.hasVivoProMode = true;
                    if (isSupported(builder, vivoIsProMode)) {
                        if (PhotonCamera.isVivoProModeOn) {
                            builder.set(vivoIsProMode, 1);
                            var vivoProIsoMin = new CaptureRequest.Key<>("vivo.control.pro_isoMin", Integer.class);
                            if (isSupported(builder, vivoProIsoMin)) {
                                builder.set(vivoProIsoMin, 72);
                            }
                            var vivoProIsoMax = new CaptureRequest.Key<>("vivo.control.pro_isoMax", Integer.class);
                            if (isSupported(builder, vivoProIsoMax)) {
                                builder.set(vivoProIsoMax, 1600);
                            }
                            var vivoProIso = new CaptureRequest.Key<>("vivo.control.iso", Integer.class);
                            if (isSupported(builder, vivoProIso)) {
                                builder.set(vivoProIso, 72);
                            }
                            var isoAuto = new CaptureRequest.Key<>("vivo.control.isoAuto", Integer.class);
                            if (isSupported(builder, isoAuto)) {
                                builder.set(isoAuto, 72);
                            }
                            var vivoProShutterMin = new CaptureRequest.Key<>("vivo.control.pro_exptimeMin", Long.class);
                            if (isSupported(builder, vivoProShutterMin)) {
                                builder.set(vivoProShutterMin, 1666666L);
                            }
                            var vivoProShutterMax = new CaptureRequest.Key<>("vivo.control.pro_exptimeMax", Long.class);
                            if (isSupported(builder, vivoProShutterMax)) {
                                builder.set(vivoProShutterMax, 500000000L);
                            }
                            var vivoProShutter = new CaptureRequest.Key<>("vivo.control.exptime", Long.class);
                            if (isSupported(builder, vivoProShutter)) {
                                builder.set(vivoProShutter, 1666666L);
                            }
                            var exptimeAuto = new CaptureRequest.Key<>("vivo.control.exptimeAuto", Long.class);
                            if (isSupported(builder, exptimeAuto)) {
                                builder.set(exptimeAuto, 1666666L);
                            }
                        }
                    }

                    if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO) && (PhotonCamera.getSettings().videoFramrate >= 120)) {
                        var vivoControlHitchcockEnable = new CaptureRequest.Key<>("vivo.control.hitchcock.enable", byte.class);
                        if (isSupported(builder, vivoControlHitchcockEnable)) {
                            builder.set(vivoControlHitchcockEnable, (byte) 1);
                        }

                        var vivoControlHitchcockMode = new CaptureRequest.Key<>("vivo.control.hitchcock.mode", Integer.class);
                        if (isSupported(builder, vivoControlHitchcockMode)) {
                            builder.set(vivoControlHitchcockMode, 4);
                        }

                        var vivoControlHitchcockFramenum = new CaptureRequest.Key<>("vivo.control.hitchcock.framenum", Integer.class);
                        if (isSupported(builder, vivoControlHitchcockFramenum)) {
                            builder.set(vivoControlHitchcockFramenum, PhotonCamera.getSettings().videoFramrate);
                        }

                        var vivoControlCurrentMode = new CaptureRequest.Key<>("vivo.control.currentMode", Integer.class);
                        if (isSupported(builder, vivoControlCurrentMode)) {
                            builder.set(vivoControlCurrentMode, 13);
                        }
                    }

                    var zeissColor = new CaptureRequest.Key<>("vivo.control.enableZeissColor", Integer.class);
                    if (isSupported(builder, zeissColor)) {
                        PhotonCamera.hasVivoZeissColor = true;
                        if (PhotonCamera.isVivoZeissColorOn) {
                            builder.set(zeissColor, 1);
                        }
                    }

                    var distortionCorrection = new CaptureRequest.Key<>("vivo.control.distortion_correction", Integer.class);
                    if (isSupported(builder, distortionCorrection)) {
                        PhotonCamera.hasVivoDistortionCorrection = true;
                        if (PhotonCamera.isVivoDistortionCorrectionOn) {
                            builder.set(distortionCorrection, 1);
                        }
                    }

                    var vivoColorTemp = new CaptureRequest.Key<>("vivo.control.colour.temperature", Integer.class);
                    if (isSupported(builder, vivoColorTemp)) {
                        if (PhotonCamera.getSpecific().specificSetting.colorTemperature > 1000) {
                            builder.set(vivoColorTemp, PhotonCamera.getSpecific().specificSetting.colorTemperature);
                        }
                    }

                    var vivoColorHue = new CaptureRequest.Key<>("vivo.control.colour.hue", Integer.class);
                    if (isSupported(builder, vivoColorHue)) {
                        if (PhotonCamera.getSpecific().specificSetting.colorTint > 0) {
                            builder.set(vivoColorHue, (int) PhotonCamera.getSpecific().specificSetting.colorTint);
                        }
                    }

                    var vivoIsRawNrMode = new CaptureRequest.Key<>("vivo.control.is_rawnr_mode", Integer.class);
                    if (isSupported(builder, vivoIsRawNrMode)) {
                        //builder.set(vivoIsRawNrMode, 0);
                    }

                    var vivoFilterMask = new CaptureRequest.Key<>("vivo.control.filterMask", Integer.class);
                    if (isSupported(builder, vivoFilterMask)) {
                        //builder.set(vivoFilterMask, 1);
                    }

                    var vivoFilter = new CaptureRequest.Key<>("vivo.control.filter", Integer.class);
                    if (isSupported(builder, vivoFilter)) {
                        //builder.set(vivoFilter, 8);
                    }

                    var vivoFilterIntensity = new CaptureRequest.Key<>("vivo.control.filterIntensity", Float.class);
                    if (isSupported(builder, vivoFilterIntensity)) {
                        //builder.set(vivoFilterIntensity, 1.0f);
                    }

                    var vivoBeautyAlgoType = new CaptureRequest.Key<>("vivo.control.beautyAlgoType", Integer.class);
                    if (isSupported(builder, vivoBeautyAlgoType)) {
                        //builder.set(vivoBeautyAlgoType, 0);
                    }

                    var vivoAiSceneMode = new CaptureRequest.Key<>("vivo.control.aiSceneMode", Integer.class);
                    if (isSupported(builder, vivoAiSceneMode)) {
                        //builder.set(vivoAiSceneMode, 0);
                    }

                    var vivoAiSceneType = new CaptureRequest.Key<>("vivo.control.aiSceneType", Integer.class);
                    if (isSupported(builder, vivoAiSceneType)) {
                        //builder.set(vivoAiSceneType, 0);
                    }

                    var vivoVideoFps = new CaptureRequest.Key<>("vivo.control.videoFrameRate", Integer.class);
                    if (isSupported(builder, vivoVideoFps)) {
                        //builder.set(vivoVideoFps, 60);
                    }

                    if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                        var vivoVideoMode = new CaptureRequest.Key<>("vivo.control.videoMode", Integer.class);
                        if (isSupported(builder, vivoVideoMode) && (PhotonCamera.getSpecific().specificSetting.vivoVideoMode >= 0)) {
                            builder.set(vivoVideoMode, PhotonCamera.getSpecific().specificSetting.vivoVideoMode);
                        }
                    }

                    if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO) && PhotonCamera.getSpecific().specificSetting.vivoUseSuperEis) {
                        var vivoSuperEis = new CaptureRequest.Key<>("vivo.control.superEis", Integer.class);
                        if (isSupported(builder, vivoSuperEis)) {
                            builder.set(vivoSuperEis, 1);
                        } else {
                            builder.set(vivoSuperEis, 0);
                        }

                        var vivoEisConfig = new CaptureRequest.Key<>("vivo.control.eis.config.enable", Integer.class);
                        if (isSupported(builder, vivoEisConfig)) {
                            if (isSupported(builder, vivoEisConfig) && (PhotonCamera.getSpecific().specificSetting.vivoEisConfig >= 0)) {
                                builder.set(vivoEisConfig, PhotonCamera.getSpecific().specificSetting.vivoEisConfig);
                            }
                        }

                        var vivoEisEnhance = new CaptureRequest.Key<>("vivo.control.eis.enhance", Integer.class);
                        if (isSupported(builder, vivoEisEnhance)) {
                            if (isSupported(builder, vivoEisEnhance) && (PhotonCamera.getSpecific().specificSetting.vivoEisEnhance >= 0)) {
                                builder.set(vivoEisEnhance, PhotonCamera.getSpecific().specificSetting.vivoEisEnhance);
                            }
                        }
                    }

                    var normalSensorGain = new CaptureRequest.Key<>("com.vivo.node.rawshot.NormalSensorGain", Float.class);
                    if (isSupported(builder, normalSensorGain)) {
                        //builder.set(normalSensorGain, 3.0f);
                    }

                    var normalIspGain = new CaptureRequest.Key<>("com.vivo.node.rawshot.NormalIspGain", Float.class);
                    if (isSupported(builder, normalIspGain)) {
                        //builder.set(normalIspGain, 3.0f);
                    }

                    var normalAdrcGain = new CaptureRequest.Key<>("com.vivo.node.rawshot.NormalAdrcGain", Float.class);
                    if (isSupported(builder, normalAdrcGain)) {
                        //builder.set(normalAdrcGain, 3.0f);
                    }

                    var vivoDisabeHdr = new CaptureRequest.Key<>("vivo.control.disableHDR", byte.class);
                    if (isSupported(builder, vivoDisabeHdr)) {
                        //builder.set(vivoDisabeHdr, (byte) 1);
                    }

                    var vivoWatermark = new CaptureRequest.Key<>("vivo.control.watermark", Integer.class);
                    if (isSupported(builder, vivoWatermark)) {
                        //builder.set(vivoWatermark, 1);
                    }

                    var vivoSuperNight = new CaptureRequest.Key<>("vivo.control.superns_mode", Integer.class);
                    if (isSupported(builder, vivoSuperNight)) {
                        //builder.set(vivoSuperNight, 1);
                    }

                    var vivoNoiseIntensity = new CaptureRequest.Key<>("vivo.control.noiseIntensity", Integer.class);
                    if (isSupported(builder, vivoNoiseIntensity)) {
                        //builder.set(vivoNoiseIntensity, 50);
                    }

                    var vivoAiNr = new CaptureRequest.Key<>("vivo.control.isAINROn", Integer.class);
                    if (isSupported(builder, vivoAiNr)) {
                        //builder.set(vivoAiNr, 1);
                    }

                    var vivoRawNrMode = new CaptureRequest.Key<>("vivo.control.is_rawnr_mode", Integer.class);
                    if (isSupported(builder, vivoRawNrMode)) {
                        //builder.set(vivoRawNrMode, 1);
                    }

                    var vivoUltraHighRes = new CaptureRequest.Key<>("vivo.control.ultra_highresolution", Integer.class);
                    if (isSupported(builder, vivoUltraHighRes)) {
                        builder.set(vivoUltraHighRes, PhotonCamera.getSpecific().specificSetting.vivoUseUltraHighResolution ? 1: 0);
                    }

                    if (PhotonCamera.getSpecific().specificSetting.sensorModes != null) {
                        for (String id : PhotonCamera.getSpecific().specificSetting.sensorModes) {
                            try {
                                String camID = "";
                                String sensorMode = "";
                                if (id.contains("-")) {
                                    camID = id.split("-")[0];
                                    sensorMode = id.split("-")[1];
                                }

                                if (PhotonCamera.getSettings().mCameraID.contains(camID)) {
                                    var vivoControlForceSensorMode = new CaptureRequest.Key<>(PhotonCamera.getSpecific().specificSetting.sensorModeKey, Integer.class);
                                    if (isSupported(builder, vivoControlForceSensorMode)) {
                                        PhotonCamera.hasVivoSensorMode = true;
                                        if (PhotonCamera.isVivoSensorModeOn && !sensorMode.equals("x")) {
                                            builder.set(vivoControlForceSensorMode, Integer.valueOf(sensorMode));
                                        }
                                    }
                                    break;
                                }
                            } catch (Exception ignored) {

                            }
                        }
                    }

                    var vivoAiGcOn = new CaptureRequest.Key<>("vivo.control.aigcOn", byte.class);
                    if (isSupported(builder, vivoAiGcOn)) {
                        //builder.set(vivoAiGcOn, (byte) 1);
                    }

                    var aiaeEnable = new CaptureRequest.Key<>("vivo.control.aiae.enable", Integer.class);
                    if (isSupported(builder, aiaeEnable)) {
                        //builder.set(aiaeEnable, 1);
                    }

                    var lotAlgoEnable = new CaptureRequest.Key<>("vivo.control.lotAlgo.enable", Integer.class);
                    if (isSupported(builder, lotAlgoEnable)) {
                        //builder.set(lotAlgoEnable, 1);
                    }

                    var vivoEngineer = new CaptureRequest.Key<>("vivo.control.engineer", Integer.class);
                    if (isSupported(builder, vivoEngineer)) {
                        builder.set(vivoEngineer, 1);
                    }

                    var vivoProRaw = new CaptureRequest.Key<>("vivo.control.is_ProRaw_on", Integer.class);
                    if (isSupported(builder, vivoProRaw)) {
                        builder.set(vivoProRaw, PhotonCamera.getSpecific().specificSetting.vivoUseProRaw ? 1 : 0);
                    }

                    var vivo3dHdr = new CaptureRequest.Key<>("vivo.control.3dhdr_enable", Integer.class);
                    if (isSupported(builder, vivo3dHdr)) {
                        //builder.set(vivo3dHdr, 1);
                    }

                    var vivoDcgHdr = new CaptureRequest.Key<>("vivo.control.EnableDCGHDR", Integer.class);
                    if (isSupported(builder, vivoDcgHdr)) {
                        //builder.set(vivoDcgHdr, 1);
                    }

                    var ln2 = new CaptureRequest.Key<>("vivo.control.enableln2", Integer.class);
                    if (isSupported(builder, ln2)) {
                        //builder.set(ln2, 1);
                    }

                    var advanceFullsize = new CaptureRequest.Key<>("vivo.control.advance_fullsize", Integer.class);
                    if (isSupported(builder, advanceFullsize)) {
                        //builder.set(advanceFullsize, 1);
                    }

                    var seamlessControl = new CaptureRequest.Key<>("vivo.control.session.seamlesscontrol", Integer.class);
                    if (isSupported(builder, seamlessControl)) {
                        builder.set(seamlessControl, 1);
                    }

                    var currentModeEx = new CaptureRequest.Key<>("vivo.control.session.currentModeEx", Long.class);
                    if (isSupported(builder, currentModeEx)) {
                        //builder.set(currentModeEx, (long) 29);
                    }

                    var vivoStreamsUsage = new CaptureRequest.Key<>("vivo.control.streamsUsage", Integer[].class);
                    if (isSupported(builder, vivoStreamsUsage)) {
                        //Integer[] streamsUsageValues = new Integer[]{3, 1, 0, 0};   // ???
                        //Integer[] streamsUsageValues = new Integer[]{4, 1, 4, 19, 0};   // Photo
                        //Integer[] streamsUsageValues = new Integer[]{3, 1, 4, 0};   // Landscape
                        Integer[] streamsUsageValues = new Integer[]{2, 1, 0};   // Pro
                        //builder.set(vivoStreamsUsage, streamsUsageValues);
                    }

                    var vivoEnableQcomSolution = new CaptureRequest.Key<>("vivo.control.enableQcomSolution", Integer.class);
                    if (isSupported(builder, vivoEnableQcomSolution)) {
                        builder.set(vivoEnableQcomSolution, PhotonCamera.getSpecific().specificSetting.vivoUseQcomSolution ? 1 : 0);
                    }

                    var vivoEngineerRemosaicMode = new CaptureRequest.Key<>("vivo.control.EngineerRemosaicMode", Integer.class);
                    if (isSupported(builder, vivoEngineerRemosaicMode) && (PhotonCamera.getSpecific().specificSetting.vivoEngineerRemosaicMode >= 0)) {
                        builder.set(vivoEngineerRemosaicMode, PhotonCamera.getSpecific().specificSetting.vivoEngineerRemosaicMode);
                    }

                    var vlogEffect = new CaptureRequest.Key<>("vivo.control.session.vlogEffect", Integer.class);
                    if (isSupported(builder, vlogEffect)) {
                        //builder.set(vlogEffect, 1);
                    }
                }

                // custom vendor keys
                if (PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeByteName != null) {
                    for (int i = 0; i < PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeByteName.length; i++) {
                        var customByteKey = new CaptureRequest.Key<>(PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeByteName[i], byte.class);
                        if (isSupported(builder, customByteKey)) {
                            builder.set(customByteKey, (byte) PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeByteValue[i]);
                        }
                    }
                }

                if (PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeInt32Name != null) {
                    for (int i = 0; i < PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeInt32Name.length; i++) {
                        var customInt32Key = new CaptureRequest.Key<>(PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeInt32Name[i], Integer.class);
                        if (isSupported(builder, customInt32Key)) {
                            builder.set(customInt32Key, PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeInt32Value[i]);
                        }
                    }
                }

                if (PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeInt64Name != null) {
                    for (int i = 0; i < PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeInt64Name.length; i++) {
                        var customInt64Key = new CaptureRequest.Key<>(PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeInt64Name[i], Long.class);
                        if (isSupported(builder, customInt64Key)) {
                            builder.set(customInt64Key, PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeInt64Value[i]);
                        }
                    }
                }

                if (PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeFloatName != null) {
                    for (int i = 0; i < PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeFloatName.length; i++) {
                        var customFloatKey = new CaptureRequest.Key<>(PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeFloatName[i], Float.class);
                        if (isSupported(builder, customFloatKey)) {
                            builder.set(customFloatKey, PhotonCamera.getSpecific().specificSetting.customVendorKeyTypeFloatValue[i]);
                        }
                    }
                }

                /*var remosaicEnabled1 = new CaptureRequest.Key<>("xiaomi.remosaic.enabled", byte.class);
                if (isSupported(builder, remosaicEnabled1)) {
                    builder.set(remosaicEnabled1, (byte) 1);
                }

                var quadcfaEnabled1 = new CaptureRequest.Key<>("xiaomi.quadcfa.enabled", byte.class);
                if (isSupported(builder, quadcfaEnabled1)) {
                    builder.set(quadcfaEnabled1, (byte) 1);
                }*/

                if (burst) {
                    var remosaicEnabled = new CaptureRequest.Key<>("xiaomi.remosaic.enabled", Byte.class);
                    if (isSupported(builder, remosaicEnabled)) {
                        builder.set(remosaicEnabled, enable);
                    }
                    var remosaicEnabled2 = new CaptureRequest.Key<>("com.mediatek.control.capture.remosaicenable", int[].class);
                    if (isSupported(builder, remosaicEnabled2)) {
                        builder.set(remosaicEnabled2, new int[]{1});
                    }
                }
            } else {
                var clientName = new CaptureRequest.Key<>("com.xiaomi.sessionparams.clientName", String.class);
                if (isSupported(builder, clientName)) {
                    builder.set(clientName, "com.android.camera");
                }
                if (burst) {
                    var remosaicEnabled = new CaptureRequest.Key<>("xiaomi.remosaic.enabled", Byte.class);
                    if (isSupported(builder, remosaicEnabled)) {
                        builder.set(remosaicEnabled, (byte) 1);
                    }

                    var remosaicEnabled2 = new CaptureRequest.Key<>("com.mediatek.control.capture.remosaicenable", int[].class);
                    if (isSupported(builder, remosaicEnabled2)) {
                        builder.set(remosaicEnabled2, new int[]{1});
                    }
                }
            }
        } catch (Exception e){
            Log.w(TAG, "Error applying vendor tags to CaptureRequest.Builder", e);
        }

        if (useMaximumResolutionKey)
        {
            builder.set(CaptureRequest.SENSOR_PIXEL_MODE, CaptureRequest.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
        }
    }

    private static void dcgControl(CaptureRequest.Builder builder) {
        final int DCG_AUTO = 0;
        final int DCG_LCG = 1;
        final int DCG_HCG = 2;
        final int DCG_Dual = 3;

        var dcgMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.DCGMode", Integer.class);
        if (isSupported(builder, dcgMode)) {
            if (PhotonCamera.getSpecific().specificSetting.codeAuroraDCGMode > 0) {
                builder.set(dcgMode, PhotonCamera.getSpecific().specificSetting.codeAuroraDCGMode);
            }
        }

        var quicDcgMode = new CaptureRequest.Key<>("com.qti.stats_control.DCGMode", Integer.class);
        if (isSupported(builder, quicDcgMode)) {
            if (PhotonCamera.getSpecific().specificSetting.qtiDCGMode > 0) {
                builder.set(quicDcgMode, PhotonCamera.getSpecific().specificSetting.qtiDCGMode);
            }
        }

        var enableHdrDcgMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableHDRDCGMode", Integer.class);
        if (isSupported(builder, enableHdrDcgMode)) {
            if (PhotonCamera.getSpecific().specificSetting.codeAuroraEnableHDRDCGMode > 0) {
                builder.set(enableHdrDcgMode, PhotonCamera.getSpecific().specificSetting.codeAuroraEnableHDRDCGMode);
            }
        }
    }

    private static void videoStabilization(CaptureRequest.Builder builder) {
        if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            CaptureRequest.Key qtiKey = new CaptureRequest.Key<>("com.qti.chi.stabilizationmode.imageStabilizationMode", byte.class);
            if (isSupported(builder, qtiKey)) {
                builder.set(qtiKey, (byte) PhotonCamera.getSpecific().specificSetting.qtiImageStabilizationMode);
            }
        }

        var eisMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EISMode", Integer.class);
        if (isSupported(builder, eisMode)) {
            PhotonCamera.hasEisModeKey = true;
            if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                builder.set(eisMode, (int) PhotonCamera.getSettings().socQualcommEisMode);
            }
        }

        var eislookahead = new CaptureRequest.Key<>("org.quic.camera.eislookahead.Enabled", byte.class);
        if (isSupported(builder, eislookahead)) {
            PhotonCamera.hasEisLookAhead = true;
            if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                var MOFAlignment = new CaptureRequest.Key<>("org.quic.camera.eislookahead.MOFAlignment", byte.class);
                var frameDelay = new CaptureRequest.Key<>("org.quic.camera.eislookahead.FrameDelay", byte.class);
                var requestedMargin = new CaptureRequest.Key<>("org.quic.camera.eislookahead.RequestedMargin", byte.class);
                var stabilizationMargins = new CaptureRequest.Key<>("org.quic.camera.eislookahead.StabilizationMargins", byte.class);
                var additionalCropOffset = new CaptureRequest.Key<>("org.quic.camera.eislookahead.AdditionalCropOffset", byte.class);
                var stabilizedOutputDims = new CaptureRequest.Key<>("org.quic.camera.eislookahead.StabilizedOutputDims", byte.class);
                var minimalTotalMargins = new CaptureRequest.Key<>("org.quic.camera.eislookahead.MinimalTotalMargins", byte.class);
                var DISMVStats = new CaptureRequest.Key<>("org.quic.camera.eislookahead.DISMVStats", byte.class);
                var ExtraHALBuffers = new CaptureRequest.Key<>("org.quic.camera.eislookahead.ExtraHALBuffers", byte.class);

                if (PhotonCamera.isEisLookAheadOn) {
                    builder.set(eislookahead, (byte) 1);

                    if (isSupported(builder, MOFAlignment)) {
                        builder.set(MOFAlignment, (byte) 1);
                    }
                    if (isSupported(builder, frameDelay)) {
                        builder.set(frameDelay, (byte) 10);
                    }
                    if (isSupported(builder, requestedMargin)) {
                        builder.set(requestedMargin, (byte) 20);
                    }
                    if (isSupported(builder, stabilizationMargins)) {
                        builder.set(stabilizationMargins, (byte) 20);
                    }
                    if (isSupported(builder, minimalTotalMargins)) {
                        builder.set(minimalTotalMargins, (byte) 8);
                    }
                    if (isSupported(builder, additionalCropOffset)) {
                        builder.set(additionalCropOffset, (byte) 5);
                    }
                    if (isSupported(builder, DISMVStats)) {
                        builder.set(DISMVStats, (byte) 1);
                    }
                    if (isSupported(builder, ExtraHALBuffers)) {
                        builder.set(ExtraHALBuffers, (byte) 10);
                    }
                } else {
                    builder.set(eislookahead, (byte) 0);
                }
            }
        }

        var eisrealtime = new CaptureRequest.Key<>("org.quic.camera.eisrealtime.Enabled", byte.class);
        if (isSupported(builder, eisrealtime)) {
            PhotonCamera.hasEisRealtime = true;
            if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                var margin = new CaptureRequest.Key<>("org.quic.camera.eisrealtime.RequestedMargin", byte.class);
                var minMargin = new CaptureRequest.Key<>("org.quic.camera.eisrealtime.MinimalTotalMargins", byte.class);
                var eisOisMode = new CaptureRequest.Key<>("org.quic.camera.eisrealtime.EISOISMode", byte.class);
                var distMgmt = new CaptureRequest.Key<>("org.quic.camera.eisrealtime.EIS2ModeWithDM", byte.class);
                var motionInd = new CaptureRequest.Key<>("org.quic.camera.eisrealtime.MotionIndication", byte.class);

                if (PhotonCamera.isEisRealtimeOn) {
                    builder.set(eisrealtime, (byte) 1);

                    if (isSupported(builder, margin)) {
                        builder.set(margin, (byte) 20); // 20% Crop für stabile Videos
                    }
                    if (isSupported(builder, minMargin)) {
                        builder.set(minMargin, (byte) 10);
                    }
                    if (isSupported(builder, eisOisMode)) {
                        builder.set(eisOisMode, (byte) 2); // Hybrid OIS+EIS
                    }
                    if (isSupported(builder, distMgmt)) {
                        builder.set(distMgmt, (byte) 1); // Anti-Warping on
                    }
                    if (isSupported(builder, motionInd)) {
                        builder.set(motionInd, (byte) 1); // Gyro-Support
                    }
                } else {
                    builder.set(eisrealtime, (byte) 0);
                }
            }
        }

        var v3Eis = new CaptureRequest.Key<>("org.quic.camera.eis3enable.EISV3Enable", byte.class);
        if (isSupported(builder, v3Eis)) {
            PhotonCamera.hasEisV3 = true;
            if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                if (PhotonCamera.isEisV3On) {
                    builder.set(v3Eis, (byte) 1);
                }

                var v3OutputCrop = new CaptureRequest.Key<>("org.quic.camera2.ipeicaconfigs.EISv3OutputCropFOV", byte.class);
                if (isSupported(builder, v3OutputCrop)) {
                    builder.set(v3OutputCrop, (byte) 1);
                }
            }
        }
    }

    private static void noiseReduction(CameraCharacteristics cameraCharacteristics, CaptureRequest.Builder builder, boolean isPreview) {
        var useMfnr = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.enableMFNR", Integer.class);
        if (PhotonCamera.getSettings().noiseReduction <= 0) {
            if (isSupported(builder, useMfnr)) {
                PhotonCamera.hasMfnrKey = true;
            }
            return;
        }

        if (PhotonCamera.isXiaomi && (PhotonCamera.getSpecific().specificSetting.xiaomiMfnrFrames > 0)) {
            var algoMfnrEnable = new CaptureRequest.Key<>("com.xiaomi.algo.mfnrEnable", byte.class);
            if (isSupported(builder, algoMfnrEnable)) {
                builder.set(algoMfnrEnable, (byte) 1);
            }

            var xiaomiMfnrEnable = new CaptureRequest.Key<>("xiaomi.mfnr.enabled", byte.class);
            if (isSupported(builder, xiaomiMfnrEnable)) {
                builder.set(xiaomiMfnrEnable, (byte) 1);
            }

            var mfnrFrameNum = new CaptureRequest.Key<>("xiaomi.mfnr.frameNum", Integer.class);
            if (isSupported(builder, mfnrFrameNum)) {
                builder.set(mfnrFrameNum, PhotonCamera.getSpecific().specificSetting.xiaomiMfnrFrames);
            }

            var mfnrFrameNum2 = new CaptureRequest.Key<>("com.xiaomi.customization.mfnr.frameNumber", Integer.class);
            if (isSupported(builder, mfnrFrameNum2)) {
                builder.set(mfnrFrameNum2, PhotonCamera.getSpecific().specificSetting.xiaomiMfnrFrames);
            }

            var mmfmlEnable = new CaptureRequest.Key<>("xiaomi.mfnr.mmfmlEnable", Integer.class);
            if (isSupported(builder, mmfmlEnable)) {
                builder.set(mmfmlEnable, 1);
            }
        }

        if (isSupported(builder, useMfnr)) {
            PhotonCamera.hasMfnrKey = true;
            if (!isPreview) {
                builder.set(useMfnr, PhotonCamera.getSettings().socQualcommUseMfnr ? 1 : 0);
            } else {
                builder.set(useMfnr, 0);
            }
        }

        // MFNR state machine deactivation in preview
        if (isPreview) {
            var useMfnrQuic = new CaptureRequest.Key<>("org.quic.camera.mfnr.enable", Integer.class);
            if (isSupported(builder, useMfnrQuic)) {
                builder.set(useMfnrQuic, 0);
            }

            var temporalDenoise = new CaptureRequest.Key<>("org.codeaurora.qcamera3.temporal_denoise.enable", byte.class);
            if (isSupported(builder, temporalDenoise)) {
                builder.set(temporalDenoise, (byte) 0);
            }

            var processType = new CaptureRequest.Key<>("org.codeaurora.qcamera3.temporal_denoise.process_type", Integer.class);
            if (isSupported(builder, processType)) {
                builder.set(processType, 0);
            }
        }

        if (PhotonCamera.getSettings().socQualcommUseMfnr && !isPreview) {
            if (PhotonCamera.getSettings().socQualcommMfnrFrames > 0) {
                var useMfnrQuic = new CaptureRequest.Key<>("org.quic.camera.mfnr.enable", Integer.class);
                if (isSupported(builder, useMfnrQuic)) {
                    builder.set(useMfnrQuic, 1);
                }
                var mFNRTotalNumFrames = new CaptureRequest.Key<>("org.quic.camera2.mfnrconfigs.MFNRTotalNumFrames", Integer.class);
                if (isSupported(builder, mFNRTotalNumFrames)) {
                    builder.set(mFNRTotalNumFrames, PhotonCamera.getSettings().socQualcommMfnrFrames);
                }
                var mFNRBlendFrameNum = new CaptureRequest.Key<>("org.quic.camera2.mfnrconfigs.MFNRBlendFrameNum", Integer.class);
                if (isSupported(builder, mFNRBlendFrameNum)) {
                    builder.set(mFNRBlendFrameNum, PhotonCamera.getSettings().socQualcommMfnrFrames);
                }
            }

            CaptureRequest.Key temporalDenoise = new CaptureRequest.Key<>("org.codeaurora.qcamera3.temporal_denoise.enable", byte.class);
            if (isSupported(builder, temporalDenoise)) {
                builder.set(temporalDenoise, PhotonCamera.getSettings().socQualcommUseMfnr ? (byte) 1 : (byte) 0);
            }

            CaptureRequest.Key multiFrameData = new CaptureRequest.Key<>("com.qti.chi.multiFrameData.MultiFrameData", byte.class);
            if (isSupported(builder, multiFrameData)) {
                builder.set(multiFrameData, PhotonCamera.getSettings().socQualcommUseMfnr ? (byte) 1 : (byte) 0);
            }

            multiFrameData = new CaptureRequest.Key<>("org.quic.camera.multiFrameData.multiFrameData", Integer.class);
            if (isSupported(builder, multiFrameData)) {
                builder.set(multiFrameData, PhotonCamera.getSettings().socQualcommUseMfnr ? 1 : 0);
            }

            CaptureRequest.Key stackedFrame = new CaptureRequest.Key<>("com.qti.chi.stackedFrame.StackedFrame", byte.class);
            if (isSupported(builder, stackedFrame)) {
                builder.set(stackedFrame, PhotonCamera.getSettings().socQualcommUseMfnr ? (byte) 1 : (byte) 0);
            }

            CaptureRequest.Key processType = new CaptureRequest.Key<>("org.codeaurora.qcamera3.temporal_denoise.process_type", Integer.class);
            if (isSupported(builder, processType)) {
                if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                    builder.set(processType, 1);
                } else {
                    builder.set(processType, 2);
                }
            }
        }
    }
}
