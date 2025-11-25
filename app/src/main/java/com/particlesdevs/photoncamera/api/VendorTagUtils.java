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
        boolean isSamsung = Build.BRAND.equalsIgnoreCase("samsung");
        boolean isGoogle = Build.BRAND.equalsIgnoreCase("google");
        boolean isZte = Build.BRAND.equalsIgnoreCase("zte");
        boolean isMotorola = Build.BRAND.equalsIgnoreCase("motorola");
        boolean isXiaomi = Build.BRAND.equalsIgnoreCase("xiaomi");

        try {
            byte enable = 1;
            if (isXiaomi) {
                var clientName = new CaptureRequest.Key<>("com.xiaomi.sessionparams.clientName", String.class);
                if (isSupported(builder, clientName)) {
                    //Log.d(TAG, "com.xiaomi.sessionparams.clientName is supported");
                    builder.set(clientName, "com.android.camera");
                }

                float apertureToUse = PhotonCamera.getSettings().apertureToUse;
                if (apertureToUse < 16) {
                    var apertureMode = new CaptureRequest.Key<>("com.xiaomi.lens.apertureMode", Integer.class);
                    if (isSupported(builder, apertureMode)) {
                        //Log.d(TAG, "com.xiaomi.lens.apertureMode is supported");
                        builder.set(apertureMode, 1); // 1 = enable, 0 = disable
                    }

                    var lensAperture = new CaptureRequest.Key<>("com.xiaomi.lens.info.availableApertures", Float.class);
                    if (isSupported(builder, lensAperture)) {
                        CameraCharacteristics.Key<Float[]> vendorKey = new CameraCharacteristics.Key<>("com.xiaomi.lens.info.availableApertures", Float[].class);
                        Float[] apert = cameraCharacteristics.get(vendorKey);
                        if (apert != null && apert.length > 0) {
                            Log.d(TAG, "Available apertures: " + Arrays.toString(apert));
                            if (Arrays.asList(apert).contains(apertureToUse)) {
                                Log.d(TAG, "Change aperture to: " + apertureToUse);
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
                builder.set(enableInSensorZoomKey, (int) 1);
            }

            if (PhotonCamera.getSpecific().specificSetting.codeAuroraSaturation != 99) {
                var useSaturation = new CaptureRequest.Key<>("org.codeaurora.qcamera3.saturation.use_saturation", Integer.class);
                if (isSupported(builder, useSaturation)) {
                    builder.set(useSaturation, (int) PhotonCamera.getSpecific().specificSetting.codeAuroraSaturation);
                }
            }

            var eisMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.EISMode", Integer.class);
            if (isSupported(builder, eisMode)) {
                builder.set(eisMode, (int) PhotonCamera.getSpecific().specificSetting.codeAuroraEisMode);
            }

            var useMfnr = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.enableMFNR", Integer.class);
            if (isSupported(builder, useMfnr)) {
                builder.set(useMfnr, PhotonCamera.getSpecific().specificSetting.useCodeAuroraMultiFrameNoiseReduction ? 1 : 0);
            }

            var useStatsViszaulize = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.enableStatsVisualizer", byte.class);
            if (isSupported(builder, useStatsViszaulize)) {
                builder.set(useStatsViszaulize, (byte) 1);
            }

            var sharpnessStrength = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sharpness.strength", Integer.class);
            if (isSupported(builder, sharpnessStrength)) {
                builder.set(sharpnessStrength, (int) PhotonCamera.getSpecific().specificSetting.codeAuroraSharpnessStrength);
            }

            if (PhotonCamera.getSpecific().specificSetting.codeAuroraAiMode != 99) {
                var aiMode = new CaptureRequest.Key<>("org.codeaurora.qcamera3.sessionParameters.AICameraMode", Integer.class);
                if (isSupported(builder, aiMode)) {
                    builder.set(aiMode, (int) PhotonCamera.getSpecific().specificSetting.codeAuroraAiMode);
                }
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
            if (isZte) {
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
            if (isSamsung) {
                var enableAIDenoiser = new CaptureRequest.Key<>("samsung.android.control.enableAIDenoiser", Integer.class);
                if (isSupported(builder, enableAIDenoiser)) {
                    builder.set(enableAIDenoiser, (int) 1);
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
        } catch (Exception e){
            Log.w(TAG, "Error applying vendor tags to CaptureRequest.Builder", e);
        }
        if (useMaximumResolutionKey) {
            builder.set(CaptureRequest.SENSOR_PIXEL_MODE, CaptureRequest.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
        }
    }
}
