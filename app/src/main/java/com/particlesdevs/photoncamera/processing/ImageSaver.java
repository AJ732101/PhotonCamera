package com.particlesdevs.photoncamera.processing;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Gainmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;

import com.particlesdevs.photoncamera.api.CameraEventsListener;
import com.particlesdevs.photoncamera.app.ContextProvider;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.capture.CaptureController;
import com.particlesdevs.photoncamera.ui.camera.views.viewfinder.MainRenderer;
import com.particlesdevs.photoncamera.util.FileManager;
import com.particlesdevs.photoncamera.util.Log;

import androidx.annotation.RequiresApi;
import androidx.exifinterface.media.ExifInterface;

import com.particlesdevs.photoncamera.api.ParseExif;
import com.particlesdevs.photoncamera.control.GyroBurst;
import com.particlesdevs.photoncamera.processing.render.Parameters;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.particlesdevs.photoncamera.pro.SpecificSetting;
import com.particlesdevs.photoncamera.settings.TunableInjector;

import static com.particlesdevs.photoncamera.processing.ImageSaverSelector.getImageSaver;
import static com.particlesdevs.photoncamera.processing.ImageSaverSelector.init;

public class ImageSaver {
    /**
     * Image frame buffer
     */
    public static final int JPG_QUALITY = 98;
    private static final String TAG = "ImageSaver";

    public static final ImageSaverSettings SETTINGS = new ImageSaverSettings();

    public SaverImplementation implementation;
    private int imageFormat;
    private int frameCounter = 0;
    private int desiredFrameCount = 0;
    public boolean newBurst = false;

    public void setFrameCount(int desiredFrameCount){
        this.desiredFrameCount = desiredFrameCount;
    }

    public void setImageFormat(int imageFormat) {
        this.imageFormat = imageFormat;
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
        TunableInjector.inject(SETTINGS);
    }

    public void initProcess(ImageReader mReader) throws IOException {
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

    public void directSaveImage(ImageReader mReader, int orientation, int targetFormat, int quality, Bundle metadata, MainRenderer renderer) throws IOException {
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

    public void directSaveImageLut(ByteBuffer imageData, int width, int height, int orientation, int targetFormat, int quality,
                                   Bundle metadata, CameraEventsListener processingEventsListener) throws IOException {
        Log.v(TAG, "directSaveImageLut() - Starting quick still image single shot LUT test");

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(imageData);

        if (PhotonCamera.getSettings().watermark) {
            File waterExternal = new File(FileManager.sPHOTON_TUNING_DIR, "watermark.png");
            Bitmap watermark = null;
            try {
                if (waterExternal.exists()) {
                    watermark = BitmapFactory.decodeFile(waterExternal.getAbsolutePath());
                }

                if (watermark == null) {
                    java.io.InputStream is = PhotonCamera.getAssetLoader().getInputStream("watermark/photoncamera_watermark.png");
                    watermark = BitmapFactory.decodeStream(is);
                    is.close();
                }

                Matrix matrix = new Matrix();
                if (orientation == 90) {
                    matrix.postRotate(270);
                } else if (orientation == 0) {
                    matrix.postRotate(180);
                } else if (orientation == 180) {
                    matrix.postRotate(0);
                } else if ((orientation == 270) || (orientation == -90)) {
                    matrix.postRotate(90);
                }

                Bitmap rotatedWatermark = Bitmap.createBitmap(watermark, 0, 0, watermark.getWidth(), watermark.getHeight(), matrix, true);

                if (rotatedWatermark != null) {
                    Canvas canvas = new Canvas(bitmap);
                    float left = 0;
                    float top = 0;
                    if (orientation == 90) {
                        left = bitmap.getWidth() - watermark.getHeight();
                        top = bitmap.getHeight() - watermark.getWidth();
                    } else if (orientation == 0) {
                        left = bitmap.getWidth() - watermark.getWidth();
                        top = 0;
                    } else if (orientation == 180) {
                        left = 0;
                        top = bitmap.getWidth() + watermark.getWidth();
                    } else if ((orientation == 270) || (orientation == -90)) {
                        left = 0;
                        top = 0; //bitmap.getHeight() - watermark.getHeight();
                    }

                    canvas.drawBitmap(rotatedWatermark, left, top, null);
                    rotatedWatermark.recycle();
                    watermark.recycle();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error loading watermark", e);
            }
        }

        boolean success = false;
        ParseExif.ExifData exifData = exifDataFromMetadata(metadata, orientation);

        Path exportFilePath = ImagePath.newJPGFilePath();
        if (targetFormat == PhotonCamera.userFormatJpegLutSw) {
            exportFilePath = ImagePath.newJPGFilePath();
            if (PhotonCamera.getSettings().useJpegUltraHdr) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    success = createUltraHdrFromSdr(bitmap, exportFilePath, exifData);
                }
            } else {
                success = ImageSaver.Util.saveBitmapAsJpg(exportFilePath, bitmap, PhotonCamera.getSettings().singleFrameQuality, exifData);
            }
        } else if (targetFormat == PhotonCamera.userFormatPngSw) {
            exportFilePath = ImagePath.newPNGFilePath();
            success = Util.saveBitmapAsPng(exportFilePath, bitmap, PhotonCamera.getSettings().singleFrameQuality, exifData);
        } else if ((targetFormat == PhotonCamera.userFormatWebpLossySw) || (targetFormat == PhotonCamera.userFormatWebpLosslessSw)) {
            exportFilePath = ImagePath.newWEBPFilePath();

            if (targetFormat == PhotonCamera.userFormatWebpLossySw) {
                success = Util.saveBitmapAsWebP(exportFilePath, bitmap, PhotonCamera.getSettings().singleFrameQuality, exifData, false);
            } else {
                success = Util.saveBitmapAsWebP(exportFilePath, bitmap, PhotonCamera.getSettings().singleFrameQuality, exifData, true);
            }
        }

        if (success) {
            Log.d(TAG, "Saving LUT-processed bitmap to: " + exportFilePath);
            Log.d(TAG, "Quick still image single shot test successful!");
        } else {
            Log.e(TAG, "Quick still image single shot test failed!");
        }

        processingEventsListener.onProcessingFinished("LUT processed JPEG: " + exportFilePath.toAbsolutePath().toString());
    }

    /*public static String createUserCommentString() {
        StringBuilder userCommentBuilder = new StringBuilder();
        userCommentBuilder.append("Unicode\0");
        userCommentBuilder.append("\n   User Comment Test");
        return userCommentBuilder.toString();
    }*/

    public static String createSpecificsString() {
        StringBuilder specificsBuilder = new StringBuilder();
        SpecificSetting sp = PhotonCamera.getSpecific().specificSetting;
        specificsBuilder.append("\nSpecifics:");
        specificsBuilder.append("\n   isDualSessionSupported: ").append(sp.isDualSessionSupported);
        specificsBuilder.append("\n   isRawColorCorrection: ").append(sp.isRawColorCorrection);
        specificsBuilder.append("\n   cameraIDS: ").append(Arrays.toString(sp.cameraIDS));
        specificsBuilder.append("\n   apertureList: ").append(Arrays.toString(sp.apertureList));
        specificsBuilder.append("\n   statisticsHotPixelMapMode: ").append(sp.statisticsHotPixelMapMode);
        specificsBuilder.append("\n   useCodeAuroraCinematicMode: ").append(sp.useCodeAuroraCinematicMode);
        specificsBuilder.append("\n   modeShowUnlimited: ").append(sp.modeShowUnlimited);
        specificsBuilder.append("\n   modeShowMotion: ").append(sp.modeShowMotion);
        specificsBuilder.append("\n   modeShowNight: ").append(sp.modeShowNight);
        specificsBuilder.append("\n   modeShowRawVideo: ").append(sp.modeShowRawVideo);
        specificsBuilder.append("\n   enableQLL: ").append(sp.enableQLL);
        specificsBuilder.append("\n   enableVideoLut: ").append(sp.enableVideoLut);
        specificsBuilder.append("\n   vivoUseSuperEis: ").append(sp.vivoUseSuperEis);
        specificsBuilder.append("\n   vivoUseProRaw: ").append(sp.vivoUseProRaw);
        specificsBuilder.append("\n   vivoUseQcomSolution: ").append(sp.vivoUseQcomSolution);
        specificsBuilder.append("\n   vivoUseUltraHighResolution: ").append(sp.vivoUseUltraHighResolution);
        specificsBuilder.append("\n   vivoEngineerRemosaicMode: ").append(sp.vivoEngineerRemosaicMode);
        specificsBuilder.append("\n   statisticsLensShadingMapMode: ").append(sp.statisticsLensShadingMapMode);
        specificsBuilder.append("\n   statisticsOisDataMode: ").append(sp.statisticsOisDataMode);
        specificsBuilder.append("\n   toneMapGamma: ").append(sp.toneMapGamma);
        specificsBuilder.append("\n   priorityShutterSpeed: ").append(sp.priorityShutterSpeed);
        specificsBuilder.append("\n   priorityIsoValue: ").append(sp.priorityIsoValue);
        specificsBuilder.append("\n   priorityMode: ").append(sp.priorityMode);
        specificsBuilder.append("\n   xiaomiMfnrFrames: ").append(sp.xiaomiMfnrFrames);
        specificsBuilder.append("\n   xiaomiSupernightMode: ").append(sp.xiaomiSupernightMode);
        specificsBuilder.append("\n   codeAuroraDCGMode: ").append(sp.codeAuroraDCGMode);
        specificsBuilder.append("\n   codeAuroraEnableHDRDCGMode: ").append(sp.codeAuroraEnableHDRDCGMode);
        specificsBuilder.append("\n   qtiDCGMode: ").append(sp.qtiDCGMode);
        specificsBuilder.append("\n   qtiImageStabilizationMode: ").append(sp.qtiImageStabilizationMode);
        specificsBuilder.append("\n   xiaomiHdrMode: ").append(sp.xiaomiHdrMode);
        specificsBuilder.append("\n   vivoVideoMode: ").append(sp.vivoVideoMode);
        specificsBuilder.append("\n   vivoEisConfig: ").append(sp.vivoEisConfig);
        specificsBuilder.append("\n   vivoEisEnhance: ").append(sp.vivoEisEnhance);
        specificsBuilder.append("\n   androidDemosaicMode: ").append(sp.androidDemosaicMode);
        specificsBuilder.append("\n   targetFps: ").append(sp.targetFps);
        specificsBuilder.append("\n   recPrefix: ").append(sp.recPrefix);
        specificsBuilder.append("\n   newRecSurfaceType: ").append(sp.newRecSurfaceType);
        specificsBuilder.append("\n   hdrMode: ").append(sp.hdrMode);
        specificsBuilder.append("\n   customVendorKeyTypeByteName: ").append(Arrays.toString(sp.customVendorKeyTypeByteName));
        specificsBuilder.append("\n   customVendorKeyTypeInt32Name: ").append(Arrays.toString(sp.customVendorKeyTypeInt32Name));
        specificsBuilder.append("\n   customVendorKeyTypeInt64Name: ").append(Arrays.toString(sp.customVendorKeyTypeInt64Name));
        specificsBuilder.append("\n   customVendorKeyTypeFloatName: ").append(Arrays.toString(sp.customVendorKeyTypeFloatName));
        specificsBuilder.append("\n   customVendorKeyTypeByteValue: ").append(Arrays.toString(sp.customVendorKeyTypeByteValue));
        specificsBuilder.append("\n   customVendorKeyTypeInt32Value: ").append(Arrays.toString(sp.customVendorKeyTypeInt32Value));
        specificsBuilder.append("\n   customVendorKeyTypeInt64Value: ").append(Arrays.toString(sp.customVendorKeyTypeInt64Value));
        specificsBuilder.append("\n   customVendorKeyTypeFloatValue: ").append(Arrays.toString(sp.customVendorKeyTypeFloatValue));
        specificsBuilder.append("\n   networkSyncBaseUrl: ").append(sp.networkSyncBaseUrl);
        specificsBuilder.append("\n   ultraHdrThreshold: ").append(sp.ultraHdrThreshold);
        specificsBuilder.append("\n   ultraHdrMaxBoost: ").append(sp.ultraHdrMaxBoost);
        specificsBuilder.append("\n   ultraHdrGamma: ").append(sp.ultraHdrGamma);

        return specificsBuilder.toString();
    }

    public static String createUserCommentString() {
        String commentText = createSpecificsString();
        byte[] header = new byte[] {
                'A', 'S', 'C', 'I', 'I', 0x00, 0x00, 0x00
        };

        try {
            byte[] textBytes = commentText.getBytes("US-ASCII");

            byte[] payload = new byte[header.length + textBytes.length];
            System.arraycopy(header, 0, payload, 0, header.length);
            System.arraycopy(textBytes, 0, payload, header.length, textBytes.length);

            return new String(payload, "ISO-8859-1");

        } catch (UnsupportedEncodingException e) {
            return commentText;
        }
    }

    public static String createProcessingString() {
        StringBuilder imageDescriptionBuilder = new StringBuilder();
        imageDescriptionBuilder.append("\n   Camera ID: ").append(PhotonCamera.getSettings().mCameraID);
         if ((PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatJpegLutSw) ||
             (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatHeifSw) ||
             (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatAvifSw) ||
             (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatPngSw) ||
             (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatWebpLossySw) ||
             (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatWebpLosslessSw)) {
            imageDescriptionBuilder.append("\n   Processing: LUT processed single shot JPEG");
            if (!PhotonCamera.getSettings().lutName.equalsIgnoreCase("lut.png")) {
                imageDescriptionBuilder.append("\n   LUT Name: ").append(PhotonCamera.getSettings().lutName);
            } else {
                imageDescriptionBuilder.append("\n   LUT Name: None");
            }
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
        imageDescriptionBuilder.append("\n   SoC LTM Control: ").append(PhotonCamera.getSettings().socQualcommLtmOff ? "Enabled" : "Disabled");
        if (PhotonCamera.getSettings().socQualcommManualWb > 1500) {
            imageDescriptionBuilder.append("\n   SoC Temperature: ").append(PhotonCamera.getSettings().socQualcommManualWb).append("K");
        }
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
            imageDescriptionBuilder.append("\n   Qualcomm ADRC: ").append(PhotonCamera.isQucommAdrcOff ? "Disabled": "Enabled");
        }
        if ((PhotonCamera.isSessionTypeOn) && (PhotonCamera.getSettings().sessionType > 0)) {
            imageDescriptionBuilder.append("\n   OpCode: ").append(PhotonCamera.getSettings().sessionType);
        }

        long elapsedTime = SystemClock.elapsedRealtime() - PhotonCamera.timeStart;
        imageDescriptionBuilder.append("\n   Processing Time: ").append(elapsedTime).append("ms");

        if (PhotonCamera.isSensorModeOn) {
            imageDescriptionBuilder.append("\n   Sensor Mode: ").append(PhotonCamera.getSettings().sensorMode);
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
            exifData.USER_COMMENT = createUserCommentString();
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
                    if ((PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatJpegLutSw) ||
                        /*(PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatHeifSw) ||
                        (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatAvifSw) ||*/
                        (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatPngSw) ||
                        (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatWebpLossySw) ||
                        (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatWebpLosslessSw)) {
                        if (CaptureController.mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                            exifData.ORIENTATION = String.valueOf(ExifInterface.ORIENTATION_NORMAL);
                        } else {
                            exifData.ORIENTATION = "3";
                        }
                    } else {
                        exifData.ORIENTATION = String.valueOf(ExifInterface.ORIENTATION_NORMAL); // "1"
                    }
                    break;
                case 90:
                    exifData.ORIENTATION = String.valueOf(ExifInterface.ORIENTATION_ROTATE_90); // "6"
                    break;
                case 180:
                    if ((PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatJpegLutSw) ||
                        /*(PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatHeifSw) ||
                        (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatAvifSw) ||*/
                            (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatPngSw) ||
                            (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatWebpLossySw) ||
                            (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatWebpLosslessSw)) {
                        if (CaptureController.mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                            exifData.ORIENTATION = String.valueOf(ExifInterface.ORIENTATION_ROTATE_180);
                        } else {
                            exifData.ORIENTATION = String.valueOf(ExifInterface.ORIENTATION_NORMAL);
                        }
                    } else {
                        exifData.ORIENTATION = String.valueOf(ExifInterface.ORIENTATION_ROTATE_180); // "1"
                    } // "3"
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

    public void runRaw(CameraCharacteristics characteristics, TotalCaptureResult captureResult, CaptureRequest captureRequest, ArrayList<GyroBurst> burstShakiness, int cameraRotation, HashMap<Long, Double> exposures) {
    	TunableInjector.inject(SETTINGS);
        implementation.runRaw(imageFormat, characteristics, captureResult, captureRequest, burstShakiness, cameraRotation, exposures);
    }

    public void processStart(CameraCharacteristics characteristics, TotalCaptureResult captureResult, CaptureRequest captureRequest, int cameraRotation) {
    	TunableInjector.inject(SETTINGS);
        implementation = ImageSaverSelector.getImageSaver(PhotonCamera.getSettings().rawFormat, implementation);
        implementation.processStart(imageFormat, characteristics, captureResult, captureRequest, cameraRotation);
    }

    public void processEnd() {
        implementation.processEnd();
    }

    public static class Util {
        public static boolean saveBitmapAsAvif(Path fileToSave, Bitmap img, int jpgQuality, ParseExif.ExifData exifData) {
            File heicFile = new File(fileToSave.toString());
            Boolean ret = false;
            AvifEncoder avifEncoder = new AvifEncoder();
            try {
                ret = avifEncoder.encodeBmpToAvif(img, heicFile, 0, PhotonCamera.getSettings().singleFrameQuality, null, exifData);
            }
            catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            return ret;
        }

        public static boolean saveBitmapAsJpg(Path fileToSave, Bitmap img, int jpgQuality, ParseExif.ExifData exifData) {
            Log.d(TAG, "saveBitmapAsJpg() - Bit depth: " + img.getConfig());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && img.getColorSpace() != null) {
                Log.d(TAG, "saveBitmapAsJpg() - Color space: " + img.getColorSpace().getName());
            }
            //img = ensure8Bit(img);
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

        private static Bitmap ensure8Bit(Bitmap img) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (img.getConfig() == Bitmap.Config.RGBA_F16) {
                    Log.d(TAG, "Converting RGBA_F16 to ARGB_8888 for compatible saving");
                    Bitmap copy = img.copy(Bitmap.Config.ARGB_8888, false);
                    img.recycle();
                    return copy;
                }
            }
            return img;
        }

        public static boolean saveBitmapAsWebP(Path fileToSave, Bitmap img, int jpgQuality, ParseExif.ExifData exifData, boolean lossless) {
            exifData.COMPRESSION = String.valueOf(jpgQuality);
            exifData.SOFTWARE = "PhotonVidCam";
            try {
                OutputStream outputStream = Files.newOutputStream(fileToSave);
                if (lossless) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        img.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, outputStream);
                    } else {
                        img.compress(Bitmap.CompressFormat.WEBP, jpgQuality, outputStream);
                    }
                } else {
                    img.compress(Bitmap.CompressFormat.WEBP, jpgQuality, outputStream);
                }
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
                MediaScannerConnection.scanFile(ContextProvider.getContext(),
                        new String[]{fileToSave.toFile().getAbsolutePath()},
                        new String[]{"image/webp"}, null);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        public static boolean saveBitmapAsPng(Path fileToSave, Bitmap img, int pngQuality, ParseExif.ExifData exifData) {
            exifData.SOFTWARE = "PhotonVidCam";
            try {
                OutputStream outputStream = Files.newOutputStream(fileToSave);
                img.compress(Bitmap.CompressFormat.PNG, pngQuality, outputStream);
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
                MediaScannerConnection.scanFile(ContextProvider.getContext(),
                        new String[]{fileToSave.toFile().getAbsolutePath()},
                        new String[]{"image/png"}, null);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        public static boolean saveStackedRaw(Path dngFilePath, ByteBuffer buffer, Parameters parameters, CaptureResult captureResult) {
            if (PhotonCamera.getSettings().writeCaptureResult && (captureResult != null)) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("'PVC_'yyyyMMdd_HHmmss_");
                String captureResultName = LocalDateTime.now().format(formatter);
                try {
                    StringBuilder sb = new StringBuilder();
                    for (CaptureResult.Key<?> key : captureResult.getKeys()) {
                        Object val = captureResult.get(key);
                        sb.append(key.getName()).append(" = ");
                        if (val != null && val.getClass().isArray()) {
                            if (val instanceof byte[]) sb.append(Arrays.toString((byte[]) val));
                            else if (val instanceof int[]) sb.append(Arrays.toString((int[]) val));
                            else if (val instanceof float[]) sb.append(Arrays.toString((float[]) val));
                            else if (val instanceof double[]) sb.append(Arrays.toString((double[]) val));
                            else if (val instanceof long[]) sb.append(Arrays.toString((long[]) val));
                            else if (val instanceof short[]) sb.append(Arrays.toString((short[]) val));
                            else if (val instanceof boolean[]) sb.append(Arrays.toString((boolean[]) val));
                            else if (val instanceof Object[]) sb.append(Arrays.deepToString((Object[]) val));
                            else sb.append(val);
                        } else {
                            sb.append(val);
                        }
                        sb.append("\n");
                    }
                    Path resultPath = FileManager.sPHOTON_RAW_DIR.toPath().resolve(captureResultName + "CaptureResult_ID" + PhotonCamera.getSettings().mCameraID + ".txt");
                    Files.write(resultPath, sb.toString().getBytes());
                    Log.d(TAG, "Saved CaptureResult to: " + resultPath);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to save CaptureResult.txt", e);
                }
            }

            return saveSingleRaw(dngFilePath, buffer, parameters);
        }

        public static boolean saveSingleRaw(Path dngFilePath,
                                            ImageFrame image,
                                            CameraCharacteristics characteristics,
                                            CaptureResult captureResult,
                                            int cameraRotation) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("'PVC_'yyyyMMdd_HHmmss_");
            String captureResultName = LocalDateTime.now().format(formatter);

            Parameters parameters = new Parameters();
            parameters.FillConstParameters(characteristics, new Point(image.width, image.height));
            int iso = captureResult.get(CaptureResult.SENSOR_SENSITIVITY);
            parameters.FillDynamicParameters(captureResult, null, iso);
            parameters.cameraRotation = cameraRotation;
            Log.d(TAG, "Camera rotation: " + parameters.cameraRotation);
            Log.d(TAG, "activearr:" + characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE));
            Log.d(TAG, "precorr:" + characteristics.get(CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE));

            if (PhotonCamera.getSettings().writeCaptureResult) {
                try {
                    StringBuilder sb = new StringBuilder();
                    for (CaptureResult.Key<?> key : captureResult.getKeys()) {
                        Object val = captureResult.get(key);
                        sb.append(key.getName()).append(" = ");
                        if (val != null && val.getClass().isArray()) {
                            if (val instanceof byte[]) sb.append(Arrays.toString((byte[]) val));
                            else if (val instanceof int[]) sb.append(Arrays.toString((int[]) val));
                            else if (val instanceof float[]) sb.append(Arrays.toString((float[]) val));
                            else if (val instanceof double[]) sb.append(Arrays.toString((double[]) val));
                            else if (val instanceof long[]) sb.append(Arrays.toString((long[]) val));
                            else if (val instanceof short[]) sb.append(Arrays.toString((short[]) val));
                            else if (val instanceof boolean[]) sb.append(Arrays.toString((boolean[]) val));
                            else if (val instanceof Object[]) sb.append(Arrays.deepToString((Object[]) val));
                            else sb.append(val);
                        } else {
                            sb.append(val);
                        }
                        sb.append("\n");
                    }
                    Path resultPath = FileManager.sPHOTON_RAW_DIR.toPath().resolve(captureResultName + "CaptureResult_ID" + PhotonCamera.getSettings().mCameraID + ".txt");
                    Files.write(resultPath, sb.toString().getBytes());
                    Log.d(TAG, "Saved CaptureResult to: " + resultPath);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to save CaptureResult.txt", e);
                }
            }

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

    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public static Boolean createUltraHdrFromSdr(Bitmap hdrBitmap, Path fileToSave, ParseExif.ExifData exifData) throws IOException {
        //hdrBitmap.setColorSpace(ColorSpace.get(ColorSpace.Named.EXTENDED_SRGB));

        Bitmap sdrBase = hdrBitmap.copy(Bitmap.Config.ARGB_8888, false);

        Bitmap gainmapContents = Bitmap.createBitmap(
                hdrBitmap.getWidth() / 2,
                hdrBitmap.getHeight() / 2,
                Bitmap.Config.ALPHA_8
        );

        Canvas canvas = new Canvas(gainmapContents);

        float threshold = PhotonCamera.getSpecific().specificSetting.ultraHdrThreshold;
        float scale = 1.0f / (1.0f - threshold);

        ColorMatrix simulateHDR = new ColorMatrix(new float[] {
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0.2126f * scale, 0.7152f * scale, 0.0722f * scale, 0, -threshold * scale
        });

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(simulateHDR));
        canvas.drawBitmap(hdrBitmap, null, new Rect(0, 0, gainmapContents.getWidth(), gainmapContents.getHeight()), paint);

        float maxHdrBoost = PhotonCamera.getSpecific().specificSetting.ultraHdrMaxBoost;
        float hdrGamma = PhotonCamera.getSpecific().specificSetting.ultraHdrGamma;
        Gainmap gainmap = new Gainmap(gainmapContents);
        gainmap.setRatioMin(1.0f, 1.0f, 1.0f);
        gainmap.setRatioMax(maxHdrBoost, maxHdrBoost, maxHdrBoost);
        gainmap.setGamma(hdrGamma, hdrGamma, hdrGamma);

        sdrBase.setGainmap(gainmap);

        try (OutputStream outputStream = Files.newOutputStream(fileToSave)) {
            sdrBase.compress(Bitmap.CompressFormat.JPEG, PhotonCamera.getSettings().singleFrameQuality, outputStream);
            outputStream.flush();
            ExifInterface inter = ParseExif.setAllAttributes(fileToSave.toFile(), exifData);
            if (PhotonCamera.getSettings().gpsLocation && (PhotonCamera.gpsLocation != null)) {
                inter.setLatLong(PhotonCamera.gpsLocation.getLatitude(), PhotonCamera.gpsLocation.getLongitude());

                if (PhotonCamera.gpsLocation.hasAltitude()) {
                    inter.setAltitude(PhotonCamera.gpsLocation.getAltitude());
                }
            }
            inter.saveAttributes();
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return false;
        } finally {
            sdrBase.recycle();
            gainmapContents.recycle();
        }
        return true;
    }
}
