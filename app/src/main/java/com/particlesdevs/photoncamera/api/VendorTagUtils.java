package com.particlesdevs.photoncamera.api;

import android.annotation.SuppressLint;
import android.hardware.camera2.CaptureRequest;

import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.util.Log;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import java.util.Arrays;

public class VendorTagUtils {
    public static CaptureRequest.Key<Integer> SELECT_PRIORITY = new CaptureRequest.Key<>("org.codeaurora.qcamera3.iso_exp_priority.select_priority", Integer.class);
    public static CaptureRequest.Key<Integer> USE_ISO_VALUE = new CaptureRequest.Key<>("org.codeaurora.qcamera3.iso_exp_priority.use_iso_value", Integer.class);
    public static CaptureRequest.Key<Long> ISO_EXP = new CaptureRequest.Key<>("org.codeaurora.qcamera3.iso_exp_priority.use_iso_exp_priority", Long.class);
    public static CameraCharacteristics.Key<int[]> ISO_AVAILABLE_MODES = new CameraCharacteristics.Key<>("org.codeaurora.qcamera3.iso_exp_priority.iso_available_modes", int[].class);
    public static CameraCharacteristics.Key<long[]> EXPOSURE_RANGE = new CameraCharacteristics.Key<>("org.codeaurora.qcamera3.iso_exp_priority.exposure_time_range", long[].class);
    public static CameraCharacteristics.Key<Integer> support_insensor_zoom = new CameraCharacteristics.Key<>("org.quic.camera.swcapabilities.inSensorZoomCapability", Integer.class);
    private static final String TAG = "VendorTagUtils";
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

    public static void setIsoExpPrioritySelectPriority(CaptureRequest.Builder builder, Integer value) {
        if ( isIsoExpPrioritySelectPrioritySupported(builder) ) {
            builder.set(SELECT_PRIORITY, value);
        }
    }
    private static boolean isIsoExpPrioritySelectPrioritySupported(CaptureRequest.Builder builder) {
        return VendorTagUtils.isSupported(builder, SELECT_PRIORITY);
    }

    public static void setIsoExpPriority(CaptureRequest.Builder builder, Long value) {
        if ( isIsoExpPrioritySupported(builder) ) {
            builder.set(ISO_EXP, value);
        }
    }
    public static void setUseIsoValues(CaptureRequest.Builder builder, int value) {
        if ( isUseIsoValueSupported(builder) ) {
            builder.set(USE_ISO_VALUE, value);
        }
    }
    private static boolean isIsoExpPrioritySupported(CaptureRequest.Builder builder) {
        return VendorTagUtils.isSupported(builder, ISO_EXP);
    }

    private static boolean isUseIsoValueSupported(CaptureRequest.Builder builder) {
        return VendorTagUtils.isSupported(builder, USE_ISO_VALUE);
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
                    var apertureMode = new CaptureRequest.Key<>("com.xiaomi.lens.apertureMode", Integer.class);
                    if (isSupported(builder, apertureMode)) {
                        builder.set(apertureMode, 1); // 1 = enable, 0 = disable
                    }

                    var lensAperture = new CaptureRequest.Key<>("com.xiaomi.sessionparams.initAperture", Float.class);
                    //var lensApertureAndroid = new CaptureRequest.Key<>("android.lens.aperture", Float.class);
                    if (isSupported(builder, lensAperture)) {
                        CameraCharacteristics.Key<Float[]> vendorKey = new CameraCharacteristics.Key<>("com.xiaomi.lens.info.availableApertures", Float[].class);
                        Float[] apert = cameraCharacteristics.get(vendorKey);
                        if (apert != null && apert.length > 0) {
                            Log.d(TAG, "Available apertures: " + Arrays.toString(apert));
                            if (Arrays.asList(apert).contains(apertureToUse)) {
                                Log.d(TAG, "Change aperture to: " + apertureToUse);
                                //builder.set(CaptureRequest.LENS_APERTURE, apertureToUse);
                                builder.set(lensAperture, apertureToUse);
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

            var enableHdrDcgMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EnableHDRDCGMode", Integer.class);
            if (isSupported(builder, enableHdrDcgMode)) {
                //builder.set(enableHdrDcgMode, (int) 2564);
            }

            if (!PhotonCamera.getSpecific().specificSetting.codeAuroraHdrMode.equals("default")) {
                CaptureRequest.Key hdrMode = null;
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

            /*if (PhotonCamera.isVivo) {
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

                var vivoUltraHighRes = new CaptureRequest.Key<>("vivo.control.ultra_highresolution", Integer.class);
                if (isSupported(builder, vivoUltraHighRes)) {
                    builder.set(vivoUltraHighRes, (int) 1);
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
                }
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
        } catch (Exception e){
            Log.w(TAG, "Error applying vendor tags to CaptureRequest.Builder", e);
        }
        if (useMaximumResolutionKey) {
            builder.set(CaptureRequest.SENSOR_PIXEL_MODE, CaptureRequest.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
        }
    }
}
