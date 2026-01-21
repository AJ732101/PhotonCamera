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

    @SuppressLint({"NewApi", "LocalSuppress"})
    public static void builderSessionApply(CameraCharacteristics cameraCharacteristics, CaptureRequest.Builder builder, boolean burst, boolean useMaximumResolutionKey) {
        PhotonCamera.isSamsung = Build.BRAND.equalsIgnoreCase("samsung");
        PhotonCamera.isGoogle = Build.BRAND.equalsIgnoreCase("google");
        PhotonCamera.isZte = Build.BRAND.equalsIgnoreCase("zte");
        PhotonCamera.isMotorola = Build.BRAND.equalsIgnoreCase("motorola");
        PhotonCamera.isXiaomi = Build.BRAND.equalsIgnoreCase("xiaomi");
        PhotonCamera.isOppo = Build.BRAND.equalsIgnoreCase("oppo");
        PhotonCamera.isVivo = Build.BRAND.equalsIgnoreCase("vivo");
        PhotonCamera.isOnePlus = Build.BRAND.equalsIgnoreCase("oneplus");
        PhotonCamera.isHonor = Build.BRAND.equalsIgnoreCase("honor");
        PhotonCamera.isHuawei = Build.BRAND.equalsIgnoreCase("huawei");

        try {
            byte enable = 1;
            if (PhotonCamera.isXiaomi) {
                var clientName = new CaptureRequest.Key<>("com.xiaomi.sessionparams.clientName", String.class);
                if (isSupported(builder, clientName)) {
                    builder.set(clientName, "com.android.camera");
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

            var eisMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EISMode", Integer.class);
            if (isSupported(builder, eisMode)) {
                PhotonCamera.hasEisModeKey = true;
                builder.set(eisMode, (int) PhotonCamera.getSettings().socQualcommEisMode);
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
            var useMfnrQuic = new CaptureRequest.Key<>("org.quic.camera.mfnr.enable", Integer.class);
            if (isSupported(builder, useMfnrQuic)) {
                builder.set(useMfnrQuic, PhotonCamera.getSettings().socQualcommUseMfnr ? 1 : 0);
            }

            var enableQLL = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.enableQLL", Integer.class);
            if (isSupported(builder, enableQLL)) {
                builder.set(enableQLL, PhotonCamera.getSpecific().specificSetting.enableQLL ? 1 : 0);
            }

            var useStatsViszaulize = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.enableStatsVisualizer", byte.class);
            if (isSupported(builder, useStatsViszaulize)) {
                builder.set(useStatsViszaulize, (byte) 1);
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
                //builder.set(enableHdrDcgMode, (int) 2564);
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
                    builder.set(hdrMode, (int) 0);
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
                var asdEnable = new CaptureRequest.Key<>("org.quic.camera.ai.asdEnable", Integer.class);
                if (isSupported(builder, asdEnable)) {
                    builder.set(asdEnable, (int) 0);
                }

                var sceneDetect = new CaptureRequest.Key<>("org.quic.camera.ai.sceneDetect", Integer.class);
                if (isSupported(builder, sceneDetect)) {
                    builder.set(sceneDetect, (int) 0);
                }

                var toneMapEnable = new CaptureRequest.Key<>("org.quic.camera.ai.toneMapEnable", Integer.class);
                if (isSupported(builder, toneMapEnable)) {
                    builder.set(toneMapEnable, (int) 0);
                }

                var aIStrength = new CaptureRequest.Key<>("org.quic.camera.AICamera.AIStrength", Integer.class);
                if (isSupported(builder, aIStrength)) {
                    builder.set(aIStrength, (int) 255);
                }
            }

            if (!PhotonCamera.getSpecific().specificSetting.codeAuroraHdrMode.equals("default")) {
                CaptureRequest.Key hdrMode = null;
                hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableAutoHDR", Integer.class);
                if (isSupported(builder, hdrMode)) {
                    builder.set(hdrMode, (int) 0);
                }
                hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.HDRMode", Integer.class);
                if (isSupported(builder, hdrMode)) {
                    //builder.set(hdrMode, (int) 2);
                }
                CaptureRequest.Key hdrPref = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.HDRModePreference", Integer.class);
                if (isSupported(builder, hdrPref)) {
                    builder.set(hdrPref, (int) 1);
                }
                CaptureRequest.Key snapshotHDRMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.SnapshotHDRMode", Integer.class);
                if (isSupported(builder, snapshotHDRMode)) {
                    builder.set(snapshotHDRMode, (int) 2);
                }

                switch (PhotonCamera.getSpecific().specificSetting.codeAuroraHdrMode) {
                    case "SHDR":
                        hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableSHDR", Integer.class);
                        if (isSupported(builder, hdrMode)) {
                            builder.set(hdrMode, (int) 1);
                        }
                        break;
                    case "QHDR":
                        hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableQHDR", Integer.class);
                        if (isSupported(builder, hdrMode)) {
                            builder.set(hdrMode, (int) 1);
                        }
                        break;
                    case "MFHDR":
                        hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableMFHDR", Integer.class);
                        if (isSupported(builder, hdrMode)) {
                            builder.set(hdrMode, (int) 1);
                        }
                        hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.numHDRexposure", Integer.class);
                        if (isSupported(builder, hdrMode)) {
                            builder.set(hdrMode, (int) 3);
                        }
                        break;
                    case "AUTO":
                        hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableAutoHDR", Integer.class);
                        if (isSupported(builder, hdrMode)) {
                            builder.set(hdrMode, (int) 1);
                        }
                        break;
                    case "HDR":
                        hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.HDRMode", Integer.class);
                        if (isSupported(builder, hdrMode)) {
                            builder.set(hdrMode, (int) 2);
                        }
                        break;
                }
            }

            if (false) {
                CaptureRequest.Key hdrMode = null;
                hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableSHDR", Integer.class);
                if (isSupported(builder, hdrMode)) {
                    builder.set(hdrMode, (int) 0);
                }

                hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableQHDR", Integer.class);
                if (isSupported(builder, hdrMode)) {
                    builder.set(hdrMode, (int) 0);
                }

                hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableMFHDR", Integer.class);
                if (isSupported(builder, hdrMode)) {
                    builder.set(hdrMode, (int) 0);
                }

                hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableAutoHDR", Integer.class);
                if (isSupported(builder, hdrMode)) {
                    builder.set(hdrMode, (int) 0);
                }

                hdrMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.HDRMode", Integer.class);
                if (isSupported(builder, hdrMode)) {
                    builder.set(hdrMode, (int) 0);
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

                var vidHenceEis = new CaptureRequest.Key<>("com.zte.camera.sessionParameters.vidhance_eis", Integer.class);
                if (isSupported(builder, vidHenceEis)) {
                    builder.set(vidHenceEis, (int) 1);
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

            if (PhotonCamera.isVivo) {
                /*var vivoUltraHighRes = new CaptureRequest.Key<>("vivo.control.ultra_highresolution", Integer.class);
                if (isSupported(builder, vivoUltraHighRes)) {
                    builder.set(vivoUltraHighRes, (int) 1);
                }

                var vivoForceSensorMode = new CaptureRequest.Key<>("vivo.control.forceSensorMode", Integer.class);
                if (isSupported(builder, vivoForceSensorMode)) {
                    builder.set(vivoForceSensorMode, (int) 0);
                }

                var vivoAiGcOn = new CaptureRequest.Key<>("vivo.control.aigcOn", Integer.class);
                if (isSupported(builder, vivoAiGcOn)) {
                    //builder.set(vivoAiGcOn, (int) 1);
                }

                var vivoEngineer = new CaptureRequest.Key<>("vivo.control.engineer", Integer.class);
                if (isSupported(builder, vivoEngineer)) {
                    builder.set(vivoEngineer, (int) 1);
                }

                var vivoProRaw = new CaptureRequest.Key<>("vivo.control.is_ProRaw_on", Integer.class);
                if (isSupported(builder, vivoProRaw)) {
                    builder.set(vivoProRaw, (int) 1);
                }

                var vivo3dHdr = new CaptureRequest.Key<>("vivo.control.3dhdr_enable", Integer.class);
                if (isSupported(builder, vivo3dHdr)) {
                    builder.set(vivo3dHdr, (int) 1);
                }

                var vivoDcgHdr = new CaptureRequest.Key<>("vivo.control.EnableDCGHDR", Integer.class);
                if (isSupported(builder, vivoDcgHdr)) {
                    builder.set(vivoDcgHdr, (int) 2564);
                }

                var vivoEisEnhance = new CaptureRequest.Key<>("vivo.control.eis.enhance", Integer.class);
                if (isSupported(builder, vivoEisEnhance)) {
                    builder.set(vivoEisEnhance, (int) 1);
                }

                var vivoEnableQcomSolution = new CaptureRequest.Key<>("vivo.control.enableQcomSolution", Integer.class);
                if (isSupported(builder, vivoEnableQcomSolution)) {
                    //builder.set(vivoEnableQcomSolution, (int) 1);
                }

                var vivoEngineerRemosaicMode = new CaptureRequest.Key<>("vivo.control.EngineerRemosaicMode", Integer.class);
                if (isSupported(builder, vivoEngineerRemosaicMode)) {
                    //builder.set(vivoEngineerRemosaicMode, (int) 1);
                }*/
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
        } catch (Exception e){
            Log.w(TAG, "Error applying vendor tags to CaptureRequest.Builder", e);
        }
        if (useMaximumResolutionKey) {
            builder.set(CaptureRequest.SENSOR_PIXEL_MODE, CaptureRequest.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
        }
    }
}
