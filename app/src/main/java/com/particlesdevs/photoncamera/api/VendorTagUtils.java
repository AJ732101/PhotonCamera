package com.particlesdevs.photoncamera.api;

import android.annotation.SuppressLint;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;

import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.capture.CaptureController;
import com.particlesdevs.photoncamera.util.Log;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.Map;

public class VendorTagUtils {
    private static final String TAG = "VendorTagUtils";
    private static boolean isSupported(CaptureRequest.Builder builder,
                                       CaptureRequest.Key<?> key) {
        boolean supported = true;
        try {
            builder.get(key);
        }catch(IllegalArgumentException exception){
            supported = false;
            Log.w(TAG,"vendor tag " + key.getName() + " is not supported");
        }
        if ( supported ) {
            Log.d(TAG,"vendor tag " + key.getName() + " is supported");
        }
        return supported;
    }
    @SuppressLint({"NewApi", "LocalSuppress"})
    public static void builderSessionApply(CameraCharacteristics cameraCharacteristics, CaptureRequest.Builder builder, boolean burst, boolean useMaximumResolutionKey) {
        try {
            byte enable = 1;
            var clientName = new CaptureRequest.Key<>("com.xiaomi.sessionparams.clientName", String.class);
            if(isSupported(builder,clientName)) {
                Log.d(TAG, "com.xiaomi.sessionparams.clientName supported");
                builder.set(clientName, "com.android.camera");
            }
            var apertureMode = new CaptureRequest.Key<>("com.xiaomi.lens.apertureMode", Integer.class);
            if (isSupported(builder, apertureMode)) {
                Log.d(TAG, "com.xiaomi.lens.apertureMode is supported");
                builder.set(apertureMode, 1); // 1 = enable, 0 = disable
            }
            var lensAperture = new CaptureRequest.Key<>("com.xiaomi.lens.aperture", Float.class);
            if (isSupported(builder, lensAperture)) {
                CameraCharacteristics.Key<Float[]> vendorKey = new CameraCharacteristics.Key<>("com.xiaomi.lens.info.availableApertures", Float[].class);
                Float[] apert = cameraCharacteristics.get(vendorKey);
                Log.d(TAG, "com.xiaomi.lens.aperture is supported");
                //builder.set(lensAperture, apert[apert.length - 1]); // stopped down
                //builder.set(lensAperture, apert[0]); // fully open
                builder.set(lensAperture, PhotonCamera.getSpecific().specificSetting.apertureToUse);
            }
            if(burst) {
                var remosaicEnabled = new CaptureRequest.Key<>("xiaomi.remosaic.enabled", Byte.class);
                if (isSupported(builder, remosaicEnabled)) {
                    Log.d(TAG, "remosaic.enabled is supported");
                    builder.set(remosaicEnabled, enable);
                }
                var remosaicEnabled2 = new CaptureRequest.Key<>("com.mediatek.control.capture.remosaicenable", int[].class);
                if (isSupported(builder, remosaicEnabled2)) {
                    Log.d(TAG, "capture.remosaicenable is supported");
                    builder.set(remosaicEnabled2, new int[]{1});
                }
            }
        } catch (Exception e){
            Log.w(TAG, "Error applying vendor tags to CaptureRequest.Builder", e);
        }
        if(useMaximumResolutionKey) {
            builder.set(CaptureRequest.SENSOR_PIXEL_MODE, CaptureRequest.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION);
        }
    }
}
