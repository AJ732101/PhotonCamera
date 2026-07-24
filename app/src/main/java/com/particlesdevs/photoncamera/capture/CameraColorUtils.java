package com.particlesdevs.photoncamera.capture;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.ColorSpaceTransform;
import android.hardware.camera2.params.RggbChannelVector;
import android.util.Rational;

public class CameraColorUtils {

    /**
     * Calculates precise RggbChannelVector gains using the hardware calibration
     * matrices from CameraCharacteristics (DNG Specification Workflow).
     *
     * @param chars  CameraCharacteristics of the active camera sensor
     * @param kelvin Target color temperature in Kelvin (2000K to 12000K)
     * @param tint   Green/Magenta shift (-1.0f = Green, 0.0f = Neutral, +1.0f = Magenta)
     * @return RggbChannelVector for CaptureRequest.COLOR_CORRECTION_GAINS
     */
    public static RggbChannelVector calculateGainsFromKelvinAndTint(
            CameraCharacteristics chars,
            int kelvin,
            float tint) {

        kelvin = Math.max(2000, Math.min(12000, kelvin));
        tint = Math.max(-1.0f, Math.min(1.0f, tint));

        // 1. Calculate target CIE xy chromaticity on the Planckian Locus
        float[] xy = kelvinToCieXy(kelvin);

        // 2. Apply Tint Shift (Increased Duv displacement vector perpendicular to the Planckian Locus)
        if (tint != 0.0f) {
            // Increased scale factor (0.08f) to produce clearly visible Green/Magenta shifts
            float tintOffset = tint * 0.08f;

            // Perpendicular direction vector to the Planckian Locus in CIE xy space
            xy[0] += tintOffset * -0.25f;
            xy[1] += tintOffset * 0.95f;
        }

        // 3. Convert CIE xy to CIE XYZ illuminant vector (Y = 1.0)
        float x = xy[0];
        float y = xy[1];
        float X = x / y;
        float Y = 1.0f;
        float Z = (1.0f - x - y) / y;

        // 4. Extract and interpolate the ColorTransform matrix from CameraCharacteristics
        float[] colorMatrix = getInterpolatedMatrix(
                chars,
                CameraCharacteristics.SENSOR_COLOR_TRANSFORM1,
                CameraCharacteristics.SENSOR_COLOR_TRANSFORM2,
                kelvin
        );

        // 5. Extract and interpolate optional CalibrationTransform matrix
        float[] calibMatrix = getInterpolatedMatrix(
                chars,
                CameraCharacteristics.SENSOR_CALIBRATION_TRANSFORM1,
                CameraCharacteristics.SENSOR_CALIBRATION_TRANSFORM2,
                kelvin
        );

        // Combine matrices: Total Transform = CalibrationTransform * ColorTransform
        float[] totalSensorMatrix = multiply3x3(calibMatrix, colorMatrix);

        // 6. Transform XYZ illuminant to native Sensor RGB space
        // [R_sensor, G_sensor, B_sensor]^T = TotalSensorMatrix * [X, Y, Z]^T
        float rawR = totalSensorMatrix[0] * X + totalSensorMatrix[1] * Y + totalSensorMatrix[2] * Z;
        float rawG = totalSensorMatrix[3] * X + totalSensorMatrix[4] * Y + totalSensorMatrix[5] * Z;
        float rawB = totalSensorMatrix[6] * X + totalSensorMatrix[7] * Y + totalSensorMatrix[8] * Z;

        rawR = Math.max(0.0001f, rawR);
        rawG = Math.max(0.0001f, rawG);
        rawB = Math.max(0.0001f, rawB);

        // 7. Calculate white balance gains (Inverses of sensor responses)
        float gainR = 1.0f / rawR;
        float gainG = 1.0f / rawG;
        float gainB = 1.0f / rawB;

        // 8. Normalize relative to Green channel (Green = 1.0f)
        float finalR = gainR / gainG;
        float finalG = 1.0f;
        float finalB = gainB / gainG;

        return new RggbChannelVector(finalR, finalG, finalG, finalB);
    }

    private static float[] getInterpolatedMatrix(
            CameraCharacteristics chars,
            CameraCharacteristics.Key<ColorSpaceTransform> key1,
            CameraCharacteristics.Key<ColorSpaceTransform> key2,
            int targetKelvin) {

        ColorSpaceTransform t1 = chars.get(key1);
        ColorSpaceTransform t2 = chars.get(key2);

        float[] m1 = transformToFloatArray(t1);
        float[] m2 = transformToFloatArray(t2);

        if (m1 == null && m2 == null) {
            // Identity matrix fallback
            return new float[]{1, 0, 0, 0, 1, 0, 0, 0, 1};
        }
        if (m1 != null && m2 == null) return m1;
        if (m1 == null) return m2;

        var ref1 = chars.get(CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT1);
        var ref2 = chars.get(CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT2);

        int temp1 = illuminantToKelvin((ref1 != null) ? ref1.intValue() : CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT1_DAYLIGHT);
        int temp2 = illuminantToKelvin((ref2 != null) ? ref2.intValue() : CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT1_STANDARD_A);

        float alpha = (float) (targetKelvin - temp1) / (float) (temp2 - temp1);
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));

        float[] result = new float[9];
        for (int i = 0; i < 9; i++) {
            result[i] = m1[i] + alpha * (m2[i] - m1[i]);
        }
        return result;
    }

    private static float[] transformToFloatArray(ColorSpaceTransform transform) {
        if (transform == null) return null;
        float[] array = new float[9];
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Rational r = transform.getElement(col, row);
                array[row * 3 + col] = r.floatValue();
            }
        }
        return array;
    }

    private static int illuminantToKelvin(int illuminant) {
        switch (illuminant) {
            case CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT1_STANDARD_A:
                return 2856;
            case CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT1_D50:
                return 5000;
            case CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT1_D65:
            case CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT1_DAYLIGHT:
                return 6504;
            case CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT1_D75:
                return 7500;
            case CameraCharacteristics.SENSOR_REFERENCE_ILLUMINANT1_FLUORESCENT:
                return 4000;
            default:
                return 5500;
        }
    }

    private static float[] kelvinToCieXy(int kelvin) {
        float T = kelvin;
        float x;

        if (T <= 7000) {
            x = -4.6070e9f / (T * T * T) + 2.9678e6f / (T * T) + 0.09911e3f / T + 0.244063f;
        } else {
            x = -2.0064e9f / (T * T * T) + 1.9018e6f / (T * T) + 0.24748e3f / T + 0.237040f;
        }

        float y = -3.000f * (x * x) + 2.870f * x - 0.275f;
        return new float[]{x, y};
    }

    private static float[] multiply3x3(float[] a, float[] b) {
        float[] result = new float[9];
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                result[row * 3 + col] =
                        a[row * 3 + 0] * b[0 * 3 + col] +
                                a[row * 3 + 1] * b[1 * 3 + col] +
                                a[row * 3 + 2] * b[2 * 3 + col];
            }
        }
        return result;
    }
}