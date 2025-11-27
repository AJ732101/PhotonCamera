package com.particlesdevs.photoncamera.processing;

import android.graphics.ImageFormat;
import com.particlesdevs.photoncamera.util.Log;

public class ImageSaverSelector {
    private static JPEGSaver JPEGSaver;
    private static HEICSaver HEICSaver;
    private static YUVSaver YUVSaver;
    private static RAW16Saver RAW16Saver;

    private static final String TAG = "ImageSaverSelector";

    public static void init(SaverImplementation saverImplementation) {
        JPEGSaver = new JPEGSaver(saverImplementation.processingEventsListener);
        HEICSaver = new HEICSaver(saverImplementation.processingEventsListener);
        YUVSaver = new YUVSaver(saverImplementation.processingEventsListener);
        RAW16Saver = new RAW16Saver(saverImplementation.processingEventsListener);
    }

    public static SaverImplementation getImageSaver(int format, SaverImplementation saverImplementation) {
        switch (format) {
            case ImageFormat.JPEG:
                saverImplementation = JPEGSaver;
                break;
            case ImageFormat.HEIC:
                saverImplementation = HEICSaver;
                break;
            case ImageFormat.YUV_420_888:
            case ImageFormat.YCBCR_P010:
                saverImplementation = YUVSaver;
                break;
            case ImageFormat.RAW10:
            case ImageFormat.RAW12:
            case ImageFormat.RAW_SENSOR:
            case ImageFormat.RAW_PRIVATE:
                Log.d(TAG, "Selected RAW16Saver for format: " + format);
                saverImplementation = RAW16Saver;
                break;
            default:
                Log.e(TAG, "Cannot save image, unexpected image format:" + format);
                break;
        }
        return saverImplementation;
    }
}
