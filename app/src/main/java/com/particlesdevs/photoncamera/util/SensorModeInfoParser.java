package com.particlesdevs.photoncamera.util;

import android.annotation.SuppressLint;
import android.hardware.camera2.CaptureResult;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class SensorModeInfoParser {

    public static class SensorMode {
        public int rawWidth;
        public int rawHeight;
        public int fps;
        public int rawFormat;
        public int byteOffset;
        public int bitDepth;
        public int confidence = 0;

        @SuppressLint("DefaultLocale")
        @Override
        public String toString() {
            return String.format("   Offset %4d | Resolution: %5dx%-4d | BitDepth: %2d-Bit | FPS: %3d | Format/Flags: %-2d | Confidence: %d\n",
                    byteOffset, rawWidth, rawHeight, bitDepth, fps, rawFormat, confidence);
        }
    }

    public static List<SensorMode> parseSensorModeInfo(byte[] byteArray) {
        List<SensorMode> modes = new ArrayList<>();
        if (byteArray == null || byteArray.length < 16) {
            return modes;
        }

        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int length = byteArray.length;
        for (int i = 0; i <= length - 16; i += 4) {

            int val1 = buffer.getInt(i);
            int val2 = buffer.getInt(i + 4);
            int val3 = buffer.getInt(i + 8);
            int val4 = buffer.getInt(i + 12);

            if (isPlausibleResolution(val1, val2)) {
                SensorMode mode = new SensorMode();
                mode.byteOffset = i;
                mode.rawWidth = val1;
                mode.rawHeight = val2;

                if (val3 == 14 || val3 == 12 || val3 == 10) {
                    mode.rawFormat = val3;
                    mode.bitDepth = val3;
                    mode.fps = isPlausibleFpsValue(val4) ? val4 : 30;
                    mode.confidence = 2;
                } else if (val4 == 14 || val4 == 12 || val4 == 10) {
                    mode.rawFormat = val4;
                    mode.bitDepth = val4;
                    mode.fps = isPlausibleFpsValue(val3) ? val3 : 30;
                    mode.confidence = 1;
                } else {
                    mode.rawFormat = val3;
                    mode.fps = isPlausibleFpsValue(val4) ? val4 : 30;
                    mode.bitDepth = 10;
                    mode.confidence = 0;
                }

                if (mode.rawHeight >= 1080) {
                    mode.confidence++;
                }

                modes.add(mode);
                i += 12;
            }
        }

        return modes;
    }

    public static byte[] getSensorModeInfoByteArray(CaptureResult captureResult) {
        if (captureResult == null) {
            return null;
        }

        CaptureResult.Key<byte[]> sensorModeKey = null;

        List<CaptureResult.Key<?>> keys = captureResult.getKeys();
        for (CaptureResult.Key<?> key : keys) {
            if ("org.codeaurora.qcamera3.sensor_meta_data.sensor_mode_info".equals(key.getName())) {
                sensorModeKey = (CaptureResult.Key<byte[]>) key;
                break;
            }
        }

        if (sensorModeKey != null) {
            byte[] modeData = captureResult.get(sensorModeKey);
            if (modeData != null && modeData.length > 0) {
                return modeData;
            }
        }

        return null;
    }

    private static boolean isPlausibleResolution(int width, int height) {
        return (width >= 1280 && width <= 12000) && (height >= 720 && height <= 9000) && (width >= height);
    }

    private static boolean isPlausibleFpsValue(int val) {
        return val >= 12 && val <= 240;
    }
}