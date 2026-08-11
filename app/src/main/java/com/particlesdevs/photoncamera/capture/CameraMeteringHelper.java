package com.particlesdevs.photoncamera.capture;

import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.MeteringRectangle;

public class CameraMeteringHelper {

    public static void applyCenterWeightedAE(CaptureRequest.Builder builder,
                                             CameraCharacteristics characteristics,
                                             float centerWeightPercent) {

        Integer maxAeRegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
        if (maxAeRegions == null || maxAeRegions < 1) {
            return;
        }

        Rect activeArray = builder.get(CaptureRequest.SCALER_CROP_REGION);
        if (activeArray == null) {
            activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        }

        if (activeArray == null) return;

        int fullWidth = activeArray.width();
        int fullHeight = activeArray.height();

        int centerWidth = (int) (fullWidth * centerWeightPercent);
        int centerHeight = (int) (fullHeight * centerWeightPercent);

        int left = activeArray.left + (fullWidth - centerWidth) / 2;
        int top = activeArray.top + (fullHeight - centerHeight) / 2;

        MeteringRectangle centerRegion = new MeteringRectangle(
                left,
                top,
                centerWidth,
                centerHeight,
                MeteringRectangle.METERING_WEIGHT_MAX
        );

        if (maxAeRegions >= 2) {
            MeteringRectangle backgroundRegion = new MeteringRectangle(
                    activeArray.left,
                    activeArray.top,
                    fullWidth,
                    fullHeight,
                    MeteringRectangle.METERING_WEIGHT_DONT_CARE
            );

            builder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{ centerRegion, backgroundRegion });
        } else {
            builder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{ centerRegion });
        }
    }
}