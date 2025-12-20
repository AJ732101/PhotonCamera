package com.particlesdevs.photoncamera.processing;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;

import com.particlesdevs.photoncamera.api.CameraEventsListener;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.util.Log;

import androidx.exifinterface.media.ExifInterface;

import com.particlesdevs.photoncamera.api.ParseExif;
import com.particlesdevs.photoncamera.control.GyroBurst;
import com.particlesdevs.photoncamera.processing.render.Parameters;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

import static com.particlesdevs.photoncamera.processing.ImageSaverSelector.getImageSaver;
import static com.particlesdevs.photoncamera.processing.ImageSaverSelector.init;

public class ImageSaver {
    /**
     * Image frame buffer
     */
    public static final int JPG_QUALITY = 98;
    private static final String TAG = "ImageSaver";

    public SaverImplementation implementation;
    private int imageFormat;
    private int frameCounter = 0;
    private int desiredFrameCount = 0;
    public boolean newBurst = false;

    public void setFrameCount(int desiredFrameCount){
        this.desiredFrameCount = desiredFrameCount;
    }

    public void updateFrameCount(int desiredFrameCount){
        this.desiredFrameCount = desiredFrameCount;
        this.implementation.frameCount = desiredFrameCount;
    }

    public int bufferSize(){
        return SaverImplementation.IMAGE_BUFFER.size();
    }

    public ImageSaver(ProcessingEventsListener processingEventsListener) {
        implementation = new DefaultSaver(processingEventsListener);
        init(implementation);
    }

    public void initProcess(ImageReader mReader) {
        Log.v(TAG, "initProcess()");
        if((frameCounter < desiredFrameCount) || desiredFrameCount == -1) {
            Log.v(TAG, "initProcess() : called from \"" + Thread.currentThread().getName() + "\" Thread");
            Image mImage;
            try {
                mImage = mReader.acquireNextImage();
            } catch (Exception ignored) {
                return;
            }
            if (mImage == null)
                return;
            int format = mImage.getFormat();
            imageFormat = mReader.getImageFormat();
            implementation = getImageSaver(format, implementation);
            Log.d(TAG,"Implementation:" + implementation);
            implementation.frameCount = desiredFrameCount;
            implementation.newBurst = newBurst;
            implementation.addImage(mImage, 0, 0, 75, null);
        } else {
            Image mImage;
            try {
                mImage = mReader.acquireNextImage();
            } catch (Exception ignored) {
                return;
            }
            if (mImage == null)
                return;
            mImage.close();
        }
        frameCounter++;
    }

    public void directSaveImage(ImageReader mReader, int orientation, int targetFormat, int quality, Bundle metadata) {
        Log.v(TAG, "directSaveImage()");
        Image mImage;
        try {
            mImage = mReader.acquireNextImage();
        } catch (Exception ignored) {
            return;
        }
        if (mImage == null)
            return;
        int format = mImage.getFormat();
        imageFormat = mReader.getImageFormat();
        implementation = getImageSaver(format, implementation);
        Log.d(TAG,"Implementation:" + implementation);
        implementation.frameCount = desiredFrameCount;
        implementation.newBurst = newBurst;
        implementation.addImage(mImage, orientation, targetFormat, quality, metadata);
    }

    public void directSaveImageLut(ByteBuffer imageData, int width, int height, int orientation, int targetFormat, int quality,
                                   Bundle metadata, CameraEventsListener processingEventsListener) {
        Log.v(TAG, "directSaveImageLut() - Starting quick JPEG test");

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(imageData);
        Path jpegFilePath;
        jpegFilePath = ImagePath.newJPGFilePath();

        ParseExif.ExifData exifData = new ParseExif.ExifData();
        if(metadata != null) {
            exifData.PHOTOGRAPHIC_SENSITIVITY = String.valueOf(metadata.getInt("iso"));
            exifData.APERTURE_VALUE = String.valueOf(metadata.getFloat("aperture"));
            exifData.EXPOSURE_TIME = metadata.getString("exposureTimeStr");
            exifData.IMAGE_DESCRIPTION = "PhotonVidCam LUT processed JPEG";
            exifData.EXIF_VERSION = "0231";
            float focalLength = metadata.getFloat("focalLength");
            if (focalLength > 0) {
                exifData.FOCAL_LENGTH = (int) (focalLength * 100) + "/100"; // z.B. "870/100"
            }
            float focalLength35mm = metadata.getFloat("focalLength35mm");
            if (focalLength35mm > 0) {
                exifData.EQUIVALENT_35MM = String.valueOf(Math.round(focalLength35mm)); // z.B. "24"
            }

            switch (orientation) {
                case 0:
                    exifData.ORIENTATION = "3";
                    break;
                case 180:
                    exifData.ORIENTATION = "3";
                    break;
                case 90:
                    exifData.ORIENTATION = "6";
                    break;
                case -90:
                case 270:
                    exifData.ORIENTATION = "8";
                    break;
            }
        }

        Log.d(TAG, "Saving LUT-processed bitmap to: " + jpegFilePath);
        boolean success = Util.saveBitmapAsJPG(jpegFilePath, bitmap, JPG_QUALITY, exifData);

        if (success) {
            Log.d(TAG, "Quick JPEG test successful!");
        } else {
            Log.e(TAG, "Quick JPEG test failed!");
        }

        processingEventsListener.onProcessingFinished("LUT processed JPEG: " + jpegFilePath);
    }

    public void runRaw(CameraCharacteristics characteristics, CaptureResult captureResult, CaptureRequest captureRequest, ArrayList<GyroBurst> burstShakiness, int cameraRotation, HashMap<Long, Double> exposures) {
        implementation.runRaw(imageFormat, characteristics, captureResult, captureRequest, burstShakiness, cameraRotation, exposures);
    }

    public void processStart(CameraCharacteristics characteristics, CaptureResult captureResult, CaptureRequest captureRequest, int cameraRotation) {
        implementation = ImageSaverSelector.getImageSaver(ImageFormat.RAW_SENSOR, implementation);
        implementation.processStart(imageFormat, characteristics, captureResult, captureRequest, cameraRotation);
    }

    public void processEnd() {
        implementation.processEnd();
    }

    public static class Util {
        public static boolean saveBitmapAsJPG(Path fileToSave, Bitmap img, int jpgQuality, ParseExif.ExifData exifData) {
            exifData.COMPRESSION = String.valueOf(jpgQuality);
            exifData.SOFTWARE = "PhotonVidCam";
            try {
                OutputStream outputStream = Files.newOutputStream(fileToSave);
                img.compress(Bitmap.CompressFormat.JPEG, jpgQuality, outputStream);
                outputStream.flush();
                outputStream.close();
                img.recycle();
                ExifInterface inter = ParseExif.setAllAttributes(fileToSave.toFile(), exifData);
                inter.saveAttributes();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        public static boolean saveBitmapAsPNG(Path fileToSave, Bitmap img, int pngQuality, ParseExif.ExifData exifData) {
            try {
                OutputStream outputStream = Files.newOutputStream(fileToSave);
                img.compress(Bitmap.CompressFormat.PNG, pngQuality, outputStream);
                outputStream.flush();
                outputStream.close();
                img.recycle();
                ExifInterface inter = ParseExif.setAllAttributes(fileToSave.toFile(), exifData);
                inter.saveAttributes();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        public static boolean saveStackedRaw(Path dngFilePath, ByteBuffer buffer, Parameters parameters) {
            return saveSingleRaw(dngFilePath, buffer, parameters);
        }
        public static boolean saveSingleRaw(Path dngFilePath,
                                            ImageFrame image,
                                            CameraCharacteristics characteristics,
                                            CaptureResult captureResult,
                                            int cameraRotation) {
            Parameters parameters = new Parameters();

            parameters.FillConstParameters(characteristics, new Point(image.width, image.height));
            int iso = captureResult.get(CaptureResult.SENSOR_SENSITIVITY);
            parameters.FillDynamicParameters(captureResult, null, iso);
            parameters.cameraRotation = cameraRotation;
            Log.d(TAG, "Camera rotation: " + parameters.cameraRotation);
            Log.d(TAG, "activearr:" + characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE));
            Log.d(TAG, "precorr:" + characteristics.get(CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE));
            return saveSingleRaw(dngFilePath, image.buffer, parameters);
        }

        public static boolean saveSingleRaw(Path dngFilePath,
                                            ByteBuffer buffer, Parameters parameters) {
            DngCreator dngCreator = new DngCreator();
            dngCreator.setParameters(parameters);
            dngCreator.setCompression(PhotonCamera.getSettings().useDngCompression);
            try {
                OutputStream outputStream = Files.newOutputStream(dngFilePath);
                dngCreator.writeBuffer(outputStream, buffer, parameters.rawSize.x, parameters.rawSize.y);
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }
}