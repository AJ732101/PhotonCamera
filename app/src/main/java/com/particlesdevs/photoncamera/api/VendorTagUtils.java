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
        PhotonCamera.hasSharpnessKey = false;
        PhotonCamera.hasEisModeKey = false;
        PhotonCamera.hasAiModeKey = false;
        PhotonCamera.hasMfnrKey = false;
        PhotonCamera.hasXiaomiNight = false;
        PhotonCamera.hasXiaomiSuperNight = false;
        PhotonCamera.hasXiaomiHdr = false;
        PhotonCamera.hasXiaomiAiAutoSceneDetection = false;
        PhotonCamera.hasXiaomiProVideoLog = false;
        PhotonCamera.hasXiaomiProVideoMovie = false;
        PhotonCamera.hasXiaomiReMosaic = false;
        PhotonCamera.hasXiaomiQuadCfa = false;
        PhotonCamera.hasXiaomiSuperResolution = false;
        PhotonCamera.hasVivoZeissColor = false;
        PhotonCamera.hasVivoProMode = false;
        PhotonCamera.hasVivoDistortionCorrection = false;
        PhotonCamera.hasQucommAdrcOff = false;
        PhotonCamera.hasEisRealtime = false;
        PhotonCamera.hasEisLookAhead = false;
        PhotonCamera.hasEisV3 = false;
        PhotonCamera.hasIdealRaw = false;
        PhotonCamera.hasAutoHdr = false;
        PhotonCamera.hasSocHdrMode = false;
    }

    @SuppressLint({"NewApi", "LocalSuppress"})
    public static void builderSessionApply(CameraCharacteristics cameraCharacteristics, CaptureRequest.Builder builder, boolean burst, boolean useMaximumResolutionKey) {
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

        resetFlags();

        try {
            if (!PhotonCamera.getSettings().disableVendorKeys) {
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

                    if (PhotonCamera.getSpecific().specificSetting.mfnrFrames > 0) {
                        var algoMfnrEnable = new CaptureRequest.Key<>("com.xiaomi.algo.mfnrEnable", byte.class);
                        if (isSupported(builder, algoMfnrEnable)) {
                            builder.set(algoMfnrEnable, (byte) 1);
                        }

                        var mfnrFrameNum = new CaptureRequest.Key<>("xiaomi.mfnr.frameNum", Integer.class);
                        if (isSupported(builder, mfnrFrameNum)) {
                            builder.set(mfnrFrameNum, PhotonCamera.getSpecific().specificSetting.mfnrFrames);
                        }

                        var mfnrFrameNum2 = new CaptureRequest.Key<>("com.xiaomi.customization.mfnr.frameNumber", Integer.class);
                        if (isSupported(builder, mfnrFrameNum2)) {
                            builder.set(mfnrFrameNum2, PhotonCamera.getSpecific().specificSetting.mfnrFrames);
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
                    builder.set(useSaturation, (int) PhotonCamera.getSettings().socQualcommSaturation);
                }

                var enableCinematicMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableCinematicMode", Integer.class);
                if (isSupported(builder, enableCinematicMode)) {
                    builder.set(enableCinematicMode, PhotonCamera.getSpecific().specificSetting.useCodeAuroraCinematicMode ? 1 : 0);
                }

                var useMfnr = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.enableMFNR", Integer.class);
                if (isSupported(builder, useMfnr)) {
                    PhotonCamera.hasMfnrKey = true;
                    builder.set(useMfnr, PhotonCamera.getSettings().socQualcommUseMfnr ? 1 : 0);
                }

                var enableIdealRAW = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableIdealRAW", byte.class);
                if (isSupported(builder, enableIdealRAW)) {
                    if (PhotonCamera.isIdealRawOn) {
                        PhotonCamera.hasIdealRaw = true;
                        builder.set(enableIdealRAW, (byte) 1);
                    }
                }

                if (PhotonCamera.getSpecific().specificSetting.mfnrFrames > 0) {
                    var useMfnrQuic = new CaptureRequest.Key<>("org.quic.camera.mfnr.enable", Integer.class);
                    if (isSupported(builder, useMfnrQuic)) {
                        builder.set(useMfnrQuic, 1);
                    }
                    var mFNRTotalNumFrames = new CaptureRequest.Key<>("org.quic.camera2.mfnrconfigs.MFNRTotalNumFrames", Integer.class);
                    if (isSupported(builder, mFNRTotalNumFrames)) {
                        builder.set(mFNRTotalNumFrames, PhotonCamera.getSpecific().specificSetting.mfnrFrames);
                    }
                    var mFNRBlendFrameNum = new CaptureRequest.Key<>("org.quic.camera2.mfnrconfigs.MFNRBlendFrameNum", Integer.class);
                    if (isSupported(builder, mFNRBlendFrameNum)) {
                        builder.set(mFNRBlendFrameNum, PhotonCamera.getSpecific().specificSetting.mfnrFrames);
                    }
                }

                var quicIspCntrLtm = new CaptureRequest.Key<>("org.quic.camera.ispcontrol.DisableBLTMDC", byte.class);
                if (isSupported(builder, quicIspCntrLtm)) {
                    //builder.set(quicIspCntrLtm, (byte) 1);
                }

                var quicDcgMode = new CaptureRequest.Key<>("com.qti.stats_control.DCGMode", Integer.class);
                if (isSupported(builder, quicDcgMode)) {
                    builder.set(quicDcgMode, PhotonCamera.getSpecific().specificSetting.qtiDCGMode);
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
                        var margin = new CaptureRequest.Key<>("org.quic.camera.eislookahead.RequestedMargin", byte.class);

                        if (PhotonCamera.isEisLookAheadOn) {
                            builder.set(eislookahead, (byte) 1);

                            if (isSupported(builder, MOFAlignment)) {
                                builder.set(MOFAlignment, (byte) 1);
                            }
                            if (isSupported(builder, frameDelay)) {
                                builder.set(frameDelay, (byte) 10);
                            }
                            if (isSupported(builder, margin)) {
                                builder.set(margin, (byte) 20);
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

                var qtiIdealRaw = new CaptureRequest.Key<>("com.qti.chi.rawcbinfo.IdealRaw", byte.class);
                if (isSupported(builder, qtiIdealRaw)) {
                    PhotonCamera.hasIdealRaw = true;
                    if (PhotonCamera.isIdealRawOn) {
                        builder.set(qtiIdealRaw, (byte) 1);
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
                    builder.set(useStatsViszualize, (byte) 1);
                }

                var sharpnessStrength = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sharpness.strength", Integer.class);
                if (isSupported(builder, sharpnessStrength)) {
                    PhotonCamera.hasSharpnessKey = true;
                    builder.set(sharpnessStrength, (int) PhotonCamera.getSettings().socQualcommSharpness);
                }

                var aiMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.AICameraMode", Integer.class);
                if (isSupported(builder, aiMode)) {
                    PhotonCamera.hasAiModeKey = true;
                    builder.set(aiMode, (int) PhotonCamera.getSettings().socQualcommAiMode);
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

                var enableHdrDcgMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableHDRDCGMode", Integer.class);
                if (isSupported(builder, enableHdrDcgMode)) {
                    builder.set(enableHdrDcgMode, PhotonCamera.getSpecific().specificSetting.codeAuroraEnableHDRDCGMode);
                }

                var dcgMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.DCGMode", Integer.class);
                if (isSupported(builder, dcgMode)) {
                    builder.set(dcgMode, PhotonCamera.getSpecific().specificSetting.codeAuroraDCGMode);
                }

                // Qualcomm LTM - local tone mapping deactivation
                if (false) {
                    var ltmDarkBoostStrength = new CaptureRequest.Key<>("org.quic.camera.ltmDynamicContrast.ltmDarkBoostStrength", Float.class);
                    if (isSupported(builder, ltmDarkBoostStrength)) {
                        builder.set(ltmDarkBoostStrength, (float) -100.0f);
                    }

                    var ltmBrightBoostStrength = new CaptureRequest.Key<>("org.quic.camera.ltmDynamicContrast.ltmBrightBoostStrength", Float.class);
                    if (isSupported(builder, ltmBrightBoostStrength)) {
                        builder.set(ltmBrightBoostStrength, (float) -100.0f);
                    }

                    var ltmContrastStrength = new CaptureRequest.Key<>("org.quic.camera.ltmDynamicContrast.ltmContrastStrength", Float.class);
                    if (isSupported(builder, ltmContrastStrength)) {
                        builder.set(ltmContrastStrength, (float) -100.0f);
                    }

                    var ltmGamma = new CaptureRequest.Key<>("org.quic.camera.ltmDynamicContrast.ltmGamma", Float.class);
                    if (isSupported(builder, ltmGamma)) {
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

                CaptureRequest.Key temporalDenoise = new CaptureRequest.Key<>("org.codeaurora.qcamera3.temporal_denoise.enable", byte.class);
                if (isSupported(builder, temporalDenoise)) {
                    builder.set(temporalDenoise, PhotonCamera.getSpecific().specificSetting.useCodeAuroraMultiFrameNoiseReduction ? (byte) 1 : (byte) 0);
                }

                CaptureRequest.Key multiFrameData = new CaptureRequest.Key<>("com.qti.chi.multiFrameData.MultiFrameData", byte.class);
                if (isSupported(builder, multiFrameData)) {
                    builder.set(multiFrameData, PhotonCamera.getSpecific().specificSetting.useCodeAuroraMultiFrameNoiseReduction ? (byte) 1 : (byte) 0);
                }

                multiFrameData = new CaptureRequest.Key<>("org.quic.camera.multiFrameData.multiFrameData", Integer.class);
                if (isSupported(builder, multiFrameData)) {
                    builder.set(multiFrameData, PhotonCamera.getSpecific().specificSetting.useCodeAuroraMultiFrameNoiseReduction ? 1 : 0);
                }

                CaptureRequest.Key stackedFrame = new CaptureRequest.Key<>("com.qti.chi.stackedFrame.StackedFrame", byte.class);
                if (isSupported(builder, stackedFrame)) {
                    builder.set(stackedFrame, PhotonCamera.getSpecific().specificSetting.useCodeAuroraMultiFrameNoiseReduction ? (byte) 1 : (byte) 0);
                }

                CaptureRequest.Key processType = new CaptureRequest.Key<>("org.codeaurora.qcamera3.temporal_denoise.process_type", Integer.class);
                if (isSupported(builder, processType)) {
                    builder.set(processType, PhotonCamera.getSpecific().specificSetting.codeAuroraTemporalNoiseProcessType);
                }

                CaptureRequest.Key hdrMode = null;
                hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableAutoHDR", Integer.class);
                if (isSupported(builder, hdrMode)) {
                    PhotonCamera.hasAutoHdr = true;
                    builder.set(hdrMode, PhotonCamera.getSettings().socQualcommAutoHdr ? 1 : 0);
                }

                // set all HDR parameter to 0
                if (true) {
                    hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableSHDR", Integer.class);
                    if (isSupported(builder, hdrMode)) {
                        PhotonCamera.hasSocHdrMode = true;
                        builder.set(hdrMode, (int) 0);
                    }

                    hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableQHDR", Integer.class);
                    if (isSupported(builder, hdrMode)) {
                        PhotonCamera.hasSocHdrMode = true;
                        builder.set(hdrMode, (int) 0);
                    }

                    hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableMFHDR", Integer.class);
                    if (isSupported(builder, hdrMode)) {
                        PhotonCamera.hasSocHdrMode = true;
                        builder.set(hdrMode, (int) 0);
                    }

                    hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.HDRMode", Integer.class);
                    if (isSupported(builder, hdrMode)) {
                        PhotonCamera.hasSocHdrMode = true;
                        builder.set(hdrMode, (int) 0);
                    }
                }

                var hdrModeMasterKey = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.HDRMode", Integer.class);
                boolean hasMasterKey = isSupported(builder, hdrModeMasterKey);
                var snapshotHDRMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.SnapshotHDRMode", Integer.class);
                boolean hasSnapshotKey = isSupported(builder, snapshotHDRMode);

                switch (PhotonCamera.getSettings().socQualcommHdrMode) {
                    case 1:
                        hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableMFHDR", Integer.class);
                        if (isSupported(builder, hdrMode)) {
                            builder.set(hdrMode, (int) 1);
                        }
                        hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.numHDRexposure", Integer.class);
                        if (isSupported(builder, hdrMode)) {
                            builder.set(hdrMode, (int) 3);
                        }
                        if (hasMasterKey) {
                            //builder.set(hdrModeMasterKey, 1);
                        }
                        if (hasSnapshotKey) {
                            //builder.set(snapshotHDRMode, 1);
                        }
                        break;
                    case 2:
                        hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableSHDR", Integer.class);
                        if (isSupported(builder, hdrMode)) {
                            builder.set(hdrMode, (int) 1);
                        }
                        hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.numHDRexposure", Integer.class);
                        if (isSupported(builder, hdrMode)) {
                            builder.set(hdrMode, (int) 3);
                        }
                        if (hasMasterKey) {
                            //builder.set(hdrModeMasterKey, 2);
                        }
                        if (hasSnapshotKey) {
                            //builder.set(snapshotHDRMode, 2);
                        }
                        break;
                    case 3:
                        hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableQHDR", Integer.class);
                        if (isSupported(builder, hdrMode)) {
                            builder.set(hdrMode, (int) 1);
                        }
                        if (hasMasterKey) {
                            //builder.set(hdrModeMasterKey, 3);
                        }
                        if (hasSnapshotKey) {
                            //builder.set(snapshotHDRMode, 3);
                        }
                        break;
                   default:
                        if (hasMasterKey) {
                            builder.set(hdrModeMasterKey, 0);
                        }
                       if (hasSnapshotKey) {
                           builder.set(snapshotHDRMode, 0);
                       }
                        break;
                }

                var hdrPref = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.HDRModePreference", Integer.class);
                if (isSupported(builder, hdrPref)) {
                    builder.set(hdrPref, (int) 1);
                }

                /*CaptureRequest.Key perfKey = new CaptureRequest.Key<>("com.qti.chi.enableadrcpath.enableADRCPath", Integer.class);
                if (isSupported(builder, perfKey)) {
                    PhotonCamera.hasQucommAdrcOff = true;
                    builder.set(perfKey, PhotonCamera.isQucommAdrcOff ? (byte) 0: (byte) 1);
                }*/

                if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                    CaptureRequest.Key qtiKey = new CaptureRequest.Key<>("com.qti.chi.stabilizationmode.imageStabilizationMode", byte.class);
                    if (isSupported(builder, qtiKey)) {
                        builder.set(qtiKey, (byte) PhotonCamera.getSpecific().specificSetting.qtiImageStabilizationMode);
                    }
                }

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
                        builder.set(enableWatermark, (int) 1);
                    }

                    var watermarkMode = new CaptureRequest.Key<>("com.zte.chi.watermark.mode", Integer.class);
                    if (isSupported(builder, watermarkMode)) {
                        builder.set(watermarkMode, (int) 1);
                    }*/

                    if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                        var vidHenceEis = new CaptureRequest.Key<>("com.zte.camera.sessionParameters.vidhance_eis", Integer.class);
                        if (isSupported(builder, vidHenceEis)) {
                            builder.set(vidHenceEis, (int) 1);
                        }
                    }
                }

                // Samsung specific
                if (PhotonCamera.isSamsung) {
                    var enableAIDenoiser = new CaptureRequest.Key<>("samsung.android.control.enableAIDenoiser", Integer.class);
                    if (isSupported(builder, enableAIDenoiser)) {
                        builder.set(enableAIDenoiser, (int) 1);
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
                    var vivoIsProMode = new CaptureRequest.Key<>("vivo.control.is_pro_mode", Integer.class);
                    PhotonCamera.hasVivoProMode = true;
                    if (isSupported(builder, vivoIsProMode)) {
                        if (PhotonCamera.isVivoProModeOn) {
                            builder.set(vivoIsProMode, 1);
                            var vivoProIsoMin = new CaptureRequest.Key<>("vivo.control.pro_isoMin", Integer.class);
                            if (isSupported(builder, vivoProIsoMin)) {
                                builder.set(vivoProIsoMin, 100);
                            }
                            var vivoProIsoMax = new CaptureRequest.Key<>("vivo.control.pro_isoMin", Integer.class);
                            if (isSupported(builder, vivoProIsoMax)) {
                                builder.set(vivoProIsoMax, 1600);
                            }
                        }
                    }

                    var distortionCorrection = new CaptureRequest.Key<>("vivo.control.distortion_correction", Integer.class);
                    if (isSupported(builder, distortionCorrection)) {
                        PhotonCamera.hasVivoZeissColor = true;
                        if (PhotonCamera.isVivoZeissColorOn) {
                            builder.set(distortionCorrection, 1);
                        }
                    }

                    var zeissColor = new CaptureRequest.Key<>("vivo.control.enableZeissColor", Integer.class);
                    if (isSupported(builder, zeissColor)) {
                        PhotonCamera.hasVivoDistortionCorrection = true;
                        if (PhotonCamera.isVivoDistortionCorrectionOn) {
                            builder.set(zeissColor, 1);
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
                        var vivoSuperEis = new CaptureRequest.Key<>("vivo.control.superEis", Integer.class);
                        if (isSupported(builder, vivoSuperEis)) {
                            //builder.set(vivoSuperEis, 1);
                        }

                        var vivoEisConfig = new CaptureRequest.Key<>("vivo.control.eis.config.enable", Integer.class);
                        if (isSupported(builder, vivoEisConfig)) {
                            builder.set(vivoEisConfig, 1);
                        }

                        var vivoEisEnhance = new CaptureRequest.Key<>("vivo.control.eis.enhance", Integer.class);
                        if (isSupported(builder, vivoEisEnhance)) {
                            builder.set(vivoEisEnhance, (int) 1);
                        }
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
                        //builder.set(vivoUltraHighRes, 1);
                    }

                    var vivoForceSensorMode = new CaptureRequest.Key<>("vivo.control.forceSensorMode", Integer.class);
                    if (isSupported(builder, vivoForceSensorMode)) {
                        //builder.set(vivoForceSensorMode, (int) 0);
                    }

                    var vivoSensorMode = new CaptureRequest.Key<>("vivo.control.sensorMode", Integer.class);
                    if (isSupported(builder, vivoSensorMode)) {
                        //builder.set(vivoSensorMode, (int) 0);
                    }

                    var vivoAiGcOn = new CaptureRequest.Key<>("vivo.control.aigcOn", Integer.class);
                    if (isSupported(builder, vivoAiGcOn)) {
                        //builder.set(vivoAiGcOn, (int) 1);
                    }

                    var vivoEngineer = new CaptureRequest.Key<>("vivo.control.engineer", Integer.class);
                    if (isSupported(builder, vivoEngineer)) {
                        //builder.set(vivoEngineer, (int) 1);
                    }

                    var vivoProRaw = new CaptureRequest.Key<>("vivo.control.is_ProRaw_on", Integer.class);
                    if (isSupported(builder, vivoProRaw)) {
                        //builder.set(vivoProRaw, (int) 1);
                    }

                    var vivo3dHdr = new CaptureRequest.Key<>("vivo.control.3dhdr_enable", Integer.class);
                    if (isSupported(builder, vivo3dHdr)) {
                        //builder.set(vivo3dHdr, 1);
                    }

                    var vivoDcgHdr = new CaptureRequest.Key<>("vivo.control.EnableDCGHDR", Integer.class);
                    if (isSupported(builder, vivoDcgHdr)) {
                        //builder.set(vivoDcgHdr, (int) 2564);
                    }

                    var vivoEnableQcomSolution = new CaptureRequest.Key<>("vivo.control.enableQcomSolution", Integer.class);
                    if (isSupported(builder, vivoEnableQcomSolution)) {
                        //builder.set(vivoEnableQcomSolution, (int) 1);
                    }

                    var vivoEngineerRemosaicMode = new CaptureRequest.Key<>("vivo.control.EngineerRemosaicMode", Integer.class);
                    if (isSupported(builder, vivoEngineerRemosaicMode)) {
                        //builder.set(vivoEngineerRemosaicMode, (int) 1);
                    }

                    var vlogEffect = new CaptureRequest.Key<>("vivo.control.session.vlogEffect", Integer.class);
                    if (isSupported(builder, vlogEffect)) {
                        //builder.set(vlogEffect, 1);
                    }
                }

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
            }
        } catch (Exception e){
            Log.w(TAG, "Error applying vendor tags to CaptureRequest.Builder", e);
        }
        if (useMaximumResolutionKey) {
            builder.set(CaptureRequest.SENSOR_PIXEL_MODE, CaptureRequest.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
        }
    }
}
