package com.particlesdevs.photoncamera.api;

import android.annotation.SuppressLint;
import android.hardware.camera2.CaptureRequest;

import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.util.Log;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import java.util.Arrays;

public class VendorTagUtils {
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
