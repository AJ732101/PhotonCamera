package com.particlesdevs.photoncamera.processing;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.os.Bundle;

import com.particlesdevs.photoncamera.api.CameraEventsListener;
import com.particlesdevs.photoncamera.app.ContextProvider;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.ui.camera.views.viewfinder.MainRenderer;
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

    public int bufferSize() {
        return SaverImplementation.IMAGE_BUFFER.size();
    }

    public ImageSaver(ProcessingEventsListener processingEventsListener) {
        implementation = new DefaultSaver(processingEventsListener);
        init(implementation);
    }

    public void initProcess(ImageReader mReader) {
        Log.v(TAG, "initProcess()");
        if ((frameCounter < desiredFrameCount) || desiredFrameCount == -1) {
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
            implementation.addImage(mImage, 0, 0, 75, null, null);
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

    public void directSaveImage(ImageReader mReader, int orientation, int targetFormat, int quality, Bundle metadata, MainRenderer renderer) {
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
        implementation.addImage(mImage, orientation, targetFormat, quality, metadata, renderer);
    }

    public static String createProcessingString() {
        StringBuilder imageDescriptionBuilder = new StringBuilder();
        imageDescriptionBuilder.append("\n   Camera ID: ").append(PhotonCamera.getSettings().mCameraID);
        if ((PhotonCamera.getSettings().previewFormat == 999999992)) {
            imageDescriptionBuilder.append("\n   Processing: LUT processed single shot JPEG");
            imageDescriptionBuilder.append("\n   LUT Name: ").append(PhotonCamera.getSettings().lutName);
        }
        if ((!PhotonCamera.getSettings().contrastCurve.equalsIgnoreCase("off"))) {
            imageDescriptionBuilder.append("\n   Contrast Curve: ").append(PhotonCamera.getSettings().contrastCurve);
        }
        imageDescriptionBuilder.append("\n   Noise Reduction: ").append((PhotonCamera.getSettings().noiseProcessing > 0) ? "Enabled" : "Disabled");
        imageDescriptionBuilder.append("\n   Edge Processing: ").append((PhotonCamera.getSettings().edgeProcessing > 0) ? "Enabled" : "Disabled");
        imageDescriptionBuilder.append("\n   Digital Zoom: ").append(PhotonCamera.getSettings().zoom2X ? "Enabled" : "Disabled");
        imageDescriptionBuilder.append("\n   SoC Saturation: ").append(PhotonCamera.getSettings().socQualcommSaturation);
        imageDescriptionBuilder.append("\n   SoC Contrast: ").append(PhotonCamera.getSettings().socQualcommContrast);
        imageDescriptionBuilder.append("\n   SoC Sharpness: ").append(PhotonCamera.getSettings().socQualcommSharpness);
        if (PhotonCamera.getSettings().hotPixelMode == 99) {
            imageDescriptionBuilder.append("\n   Hot Pixel Mode: Device Default");
        }
        else {
            imageDescriptionBuilder.append("\n   Hot Pixel Mode: ").append(PhotonCamera.getSettings().hotPixelMode);
        }
        if (PhotonCamera.getSettings().colorCorrectionAberrationMode == 99) {
            imageDescriptionBuilder.append("\n   Aberration Correction: Device Default");
        }
        else {
            imageDescriptionBuilder.append("\n   Aberration Correction: ").append(PhotonCamera.getSettings().colorCorrectionAberrationMode);
        }
        if (PhotonCamera.getSettings().distortionCorrectionMode == 99) {
            imageDescriptionBuilder.append("\n   Distortion Correction: Device Default");
        }
        else {
            imageDescriptionBuilder.append("\n   Distortion Correction: ").append(PhotonCamera.getSettings().distortionCorrectionMode);
        }
        if (PhotonCamera.getSettings().shadingMode == 99) {
            imageDescriptionBuilder.append("\n   Vignette Correction: Device Default");
        }
        else {
            imageDescriptionBuilder.append("\n   Vignette Correction: ").append(PhotonCamera.getSettings().shadingMode);
        }
        if (PhotonCamera.hasQucommAdrcOff) {
            imageDescriptionBuilder.append("\n   Qualcomm ADRC: ").append(PhotonCamera.isQucommAdrcOff ? "Off": "On");
        }
        if (PhotonCamera.isVivoSensorModeOn && (PhotonCamera.getSpecific().specificSetting.vivoControlForceSensorMode >= 0)) {
            imageDescriptionBuilder.append("\n   Vivo Sensor Mode: ").append(PhotonCamera.getSpecific().specificSetting.vivoControlForceSensorMode);
        }
        if (PhotonCamera.isQucommSensorModeOn && (PhotonCamera.getSpecific().specificSetting.sensorMetaDataCurrentMode >= 0)) {
            imageDescriptionBuilder.append("\n   Qualcomm Sensor Mode: ").append(PhotonCamera.getSpecific().specificSetting.sensorMetaDataCurrentMode);
        }
        imageDescriptionBuilder.append("\n   Version: ").append(PhotonCamera.getVersion());

        return imageDescriptionBuilder.toString();
    }

    public static ParseExif.ExifData exifDataFromMetadata(Bundle metadata, int orientation) {
        ParseExif.ExifData exifData = new ParseExif.ExifData();
        if (metadata != null) {
            exifData.PHOTOGRAPHIC_SENSITIVITY = String.valueOf(metadata.getInt("iso"));
            exifData.F_NUMBER = String.valueOf(metadata.getFloat("aperture"));
            exifData.EXPOSURE_TIME = metadata.getString("exposureTimeStr");
            exifData.IMAGE_DESCRIPTION = createProcessingString();
            exifData.EXIF_VERSION = "0232";
            exifData.COMPRESSION = String.valueOf(PhotonCamera.getSettings().singleFrameQuality);
            exifData.COLOR_SPACE = "sRGB";
            float focalLength = metadata.getFloat("focalLength");
            if (focalLength > 0) {
                exifData.FOCAL_LENGTH = (int) (focalLength * 100) + "/100";
            }
            int focalLength35mm = metadata.getInt("focalLength35mm");
            if (focalLength35mm > 0) {
                exifData.EQUIVALENT_35MM = String.valueOf(Math.round(focalLength35mm));
            } else {
                int focal35mm = metadata.getInt("focal35mm");
                exifData.EQUIVALENT_35MM = String.valueOf(Math.round(focal35mm));
            }

            switch (orientation) {
                case 0:
                    if (PhotonCamera.getSettings().previewFormat == 999999992) {
                        exifData.ORIENTATION = "3";
                    } else {
                        exifData.ORIENTATION = String.valueOf(ExifInterface.ORIENTATION_NORMAL); // "1"
                    }
                    break;
                case 90:
                    exifData.ORIENTATION = String.valueOf(ExifInterface.ORIENTATION_ROTATE_90); // "6"
                    break;
                case 180:
                    exifData.ORIENTATION = String.valueOf(ExifInterface.ORIENTATION_ROTATE_180); // "3"
                    break;
                case 270:
                case -90:
                    exifData.ORIENTATION = String.valueOf(ExifInterface.ORIENTATION_ROTATE_270); // "8"
                    break;
                default:
                    exifData.ORIENTATION = String.valueOf(ExifInterface.ORIENTATION_UNDEFINED); // "0" oder "1"
                    break;
            }
        }

        return exifData;
    }

    public void directSaveImageLut(ByteBuffer imageData, int width, int height, int orientation, int targetFormat, int quality,
                                   Bundle metadata, CameraEventsListener processingEventsListener) {
        Log.v(TAG, "directSaveImageLut() - Starting quick JPEG test");

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(imageData);
        Path jpegFilePath;
        jpegFilePath = ImagePath.newJPGFilePath();

        ParseExif.ExifData exifData = exifDataFromMetadata(metadata, orientation);

        Log.d(TAG, "Saving LUT-processed bitmap to: " + jpegFilePath);
        boolean success = Util.saveBitmapAsJPG(jpegFilePath, bitmap, PhotonCamera.getSettings().singleFrameQuality, exifData);

        if (success) {
            Log.d(TAG, "Quick JPEG test successful!");
        } else {
            Log.e(TAG, "Quick JPEG test failed!");
        }

        processingEventsListener.onProcessingFinished("LUT processed JPEG: " + jpegFilePath.toAbsolutePath().toString());
    }

    public void runRaw(CameraCharacteristics characteristics, CaptureResult captureResult, CaptureRequest captureRequest, ArrayList<GyroBurst> burstShakiness, int cameraRotation, HashMap<Long, Double> exposures) {
        implementation.runRaw(imageFormat, characteristics, captureResult, captureRequest, burstShakiness, cameraRotation, exposures);
    }

    public void processStart(CameraCharacteristics characteristics, CaptureResult captureResult, CaptureRequest captureRequest, int cameraRotation) {
        implementation = ImageSaverSelector.getImageSaver(PhotonCamera.getSettings().rawFormat, implementation);
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
                if (PhotonCamera.getSettings().gpsLocation && (PhotonCamera.gpsLocation != null)) {
                    inter.setLatLong(PhotonCamera.gpsLocation.getLatitude(), PhotonCamera.gpsLocation.getLongitude());

                    if (PhotonCamera.gpsLocation.hasAltitude()) {
                        inter.setAltitude(PhotonCamera.gpsLocation.getAltitude());
                    }
                }
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
                MediaScannerConnection.scanFile(ContextProvider.getContext(),
                        new String[]{fileToSave.toFile().getAbsolutePath()},
                        new String[]{"image/png"}, null);
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

        public static boolean saveSingleRaw(Path dngFilePath, ByteBuffer buffer, Parameters parameters) {
            DngCreator dngCreator = new DngCreator();
            dngCreator.setParameters(parameters);
            dngCreator.setCompression(PhotonCamera.getSettings().useDngCompression);
            //dngCreator.setBitsPerSample(10);
            try {
                OutputStream outputStream = Files.newOutputStream(dngFilePath);
                dngCreator.writeBuffer(outputStream, buffer, parameters.rawSize.x, parameters.rawSize.y);
                outputStream.close();

                MediaScannerConnection.scanFile(ContextProvider.getContext(),
                        new String[]{dngFilePath.toFile().getAbsolutePath()},
                        new String[]{"image/dng"}, null);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }
}
