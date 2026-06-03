package com.particlesdevs.photoncamera.processing;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.DataSpace;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.exifinterface.media.ExifInterface;

import com.particlesdevs.photoncamera.api.ParseExif;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.ui.camera.views.viewfinder.MainRenderer;
import com.particlesdevs.photoncamera.util.Log;
import com.radzivon.bartoshyk.avif.coder.AvifChromaSubsampling;
import com.radzivon.bartoshyk.avif.coder.AvifSpeed;
import com.radzivon.bartoshyk.avif.coder.AvifSurfaceMode;
import com.radzivon.bartoshyk.avif.coder.HeifCoder;
import com.radzivon.bartoshyk.avif.coder.PreciseMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;

public class AvifEncoder {

    private static final String TAG = "AvifEncoder";

    private byte[] createExif(Bundle metadata, int orientation) {
        try {
            // 1. Create a real (but tiny) valid JPEG as a base for ExifInterface
            File tempExifFile = File.createTempFile("temp_exif", ".jpg");
            Bitmap tiny = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            try (FileOutputStream out = new FileOutputStream(tempExifFile)) {
                tiny.compress(Bitmap.CompressFormat.JPEG, 95, out);
            }
            tiny.recycle();

            // 2. Use ExifInterface to write metadata
            ExifInterface exif = new ExifInterface(tempExifFile.getAbsolutePath());
            ParseExif.ExifData exifData = ImageSaver.exifDataFromMetadata(metadata, orientation);

            if (exifData.PHOTOGRAPHIC_SENSITIVITY != null) exif.setAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, exifData.PHOTOGRAPHIC_SENSITIVITY);
            if (exifData.F_NUMBER != null) exif.setAttribute(ExifInterface.TAG_F_NUMBER, exifData.F_NUMBER);
            if (exifData.EXPOSURE_TIME != null) exif.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, exifData.EXPOSURE_TIME);
            if (exifData.FOCAL_LENGTH != null) exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, exifData.FOCAL_LENGTH);
            if (exifData.IMAGE_DESCRIPTION != null) exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, exifData.IMAGE_DESCRIPTION);
            if (exifData.MAKE != null) exif.setAttribute(ExifInterface.TAG_MAKE, exifData.MAKE);
            if (exifData.MODEL != null) exif.setAttribute(ExifInterface.TAG_MODEL, exifData.MODEL);
            if (exifData.EQUIVALENT_35MM != null) exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, exifData.EQUIVALENT_35MM);

            // Set the actual orientation in EXIF.
            // When EXIF bytes are provided, most viewers prioritize the EXIF Orientation tag.
            if (exifData.ORIENTATION != null) {
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, exifData.ORIENTATION);
            }

            exif.saveAttributes();

            // 3. Extract the EXIF APP1 segment from the generated JPEG
            byte[] fileBytes = Files.readAllBytes(tempExifFile.toPath());
            tempExifFile.delete();

            // Look for APP1 marker (FF E1)
            for (int i = 0; i < fileBytes.length - 4; i++) {
                if (fileBytes[i] == (byte) 0xFF && fileBytes[i + 1] == (byte) 0xE1) {
                    int length = ((fileBytes[i + 2] & 0xFF) << 8) | (fileBytes[i + 3] & 0xFF);
                    // The payload of APP1 includes the "Exif\0\0" header.
                    // ExifInterface writes the APP1 header (FF E1), then the length (2 bytes),
                    // and then the payload. libavif wants the payload starting with "Exif\0\0".
                    byte[] exifPayload = new byte[length - 2];
                    System.arraycopy(fileBytes, i + 4, exifPayload, 0, length - 2);
                    return exifPayload;
                }
            }

            return null;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create EXIF", e);
            return null;
        }
    }

    /**
     * Encodes an Image object by first converting it to a Bitmap and then to AVIF.
     *
     * @param image      The YUV Image object to encode.
     * @param outputFile The target file for the AVIF image.
     * @throws IOException If encoding or writing the file fails.
     */
    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void encodeYuvToAvif(Image image, File outputFile, int orientation, int quality, Bundle metadata, MainRenderer renderer) throws IOException, ExecutionException, InterruptedException {
        Log.d(TAG, "Starting AVIF encoding for image with resolution: " + image.getWidth() + "x" + image.getHeight());

        // 1. Convert the YUV Image to an ARGB Bitmap.
        Bitmap originalBitmap = null;
        Bitmap rotatedBitmap = null;

        // 1. Convert the YUV Image to a high-precision Bitmap
        if (image.getFormat() == ImageFormat.YUV_420_888) {
            rotatedBitmap = ImageUtils.yuv8BitToBitmap(image);
        }

        // 2. Create an instance of HeifCoder.
        HeifCoder coder = new HeifCoder();
        byte[] exifBytes = createExif(metadata, orientation);

        try {
            // 3. Encode the Bitmap to AVIF using the correct method signature.
            //    Parameters based on the screenshot. The last 3 are enums.
            //    Let's use reasonable defaults.
            byte[] avifByteArray = null;
            // IMPORTANT: Since we are embedding EXIF bytes with the correct orientation tag,
            // we pass '0' as the rotation parameter to the encoder to avoid double rotation (irot vs EXIF).

            int ds = -1;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                switch (PhotonCamera.getSettings().swColorSpace) {
                    case "scRGB LINEAR":
                        ds = DataSpace.DATASPACE_SCRGB_LINEAR;
                        break;
                    case "sRGB":
                        ds = DataSpace.DATASPACE_SRGB;
                        break;
                    case "scRGB":
                        ds = DataSpace.DATASPACE_SCRGB;
                        break;
                    case "DISPLAY P3":
                        ds = DataSpace.DATASPACE_DISPLAY_P3;
                        break;
                    case "BT.2020 HLG":
                        ds = DataSpace.DATASPACE_BT2020_HLG;
                        break;
                    case "BT.2020 PQ":
                        ds = DataSpace.DATASPACE_BT2020_PQ;
                        break;
                    case "ADOBE RGB":
                        ds = DataSpace.DATASPACE_ADOBE_RGB;
                        break;
                    case "JFIF":
                        ds = DataSpace.DATASPACE_JFIF;
                        break;
                    case "BT.601_625":
                        ds = DataSpace.DATASPACE_BT601_625;
                        break;
                    case "BT.601_525":
                        ds = DataSpace.DATASPACE_BT601_525;
                        break;
                    case "BT.2020":
                        ds = DataSpace.DATASPACE_BT2020;
                        break;
                    case "BT.709":
                        ds = DataSpace.DATASPACE_BT709;
                        break;
                    case "DCI P3":
                        ds = DataSpace.DATASPACE_DCI_P3;
                        break;
                    case "sRGB LINEAR":
                        ds = DataSpace.DATASPACE_SRGB_LINEAR;
                        break;
                    case "DISPLAY BT.2020":
                        ds = 142999552;
                        break;
                }
            }

            var preciseMode = PreciseMode.LOSSY;
            if (PhotonCamera.getSettings().useLosslessSwEncoding) {
                preciseMode = PreciseMode.LOSSLESS;
            }

            if (image.getFormat() == ImageFormat.YUV_420_888) {
                Image.Plane yPlane = image.getPlanes()[0];
                Image.Plane uPlane = image.getPlanes()[1];
                Image.Plane vPlane = image.getPlanes()[2];

                ByteBuffer yBuffer = yPlane.getBuffer();
                ByteBuffer uBuffer = uPlane.getBuffer();
                ByteBuffer vBuffer = vPlane.getBuffer();

                int yRowStride = yPlane.getRowStride();
                int uRowStride = uPlane.getRowStride();
                int vRowStride = vPlane.getRowStride();

                int uPixelStride = uPlane.getPixelStride();
                int vPixelStride = vPlane.getPixelStride();

                avifByteArray = coder.encodeAvif420_888(yBuffer, yRowStride, uBuffer, uRowStride, vBuffer, vRowStride, uPixelStride, vPixelStride, image.getWidth(), image.getHeight(), quality, preciseMode, AvifSpeed.EIGHT, ds, 0, exifBytes);
            } else {
                Image.Plane yPlane = image.getPlanes()[0];
                Image.Plane uvPlane = image.getPlanes()[1]; // In P010, U and V are interleaved

                ByteBuffer yBuffer = yPlane.getBuffer().order(ByteOrder.LITTLE_ENDIAN);
                ByteBuffer uvBuffer = uvPlane.getBuffer().order(ByteOrder.LITTLE_ENDIAN);

                int yRowStride = yPlane.getRowStride();
                int uvRowStride = uvPlane.getRowStride();

                avifByteArray = coder.encodeAvifP010(yBuffer, yRowStride, uvBuffer, uvRowStride, image.getWidth(), image.getHeight(), quality, preciseMode, AvifSpeed.EIGHT, ds, 0, exifBytes);
            }

            // 4. Verify that the encoder returned data.
            if (avifByteArray == null || avifByteArray.length == 0) {
                throw new IOException("AVIF encoder returned null or empty data. Encoding failed.");
            }

            // 5. Write the data to the output file.
            java.nio.file.Files.write(outputFile.toPath(), avifByteArray);
            Log.d(TAG, "Successfully saved AVIF file to: " + outputFile.getAbsolutePath() + " (" + avifByteArray.length / 1024 + " KB)");

            XmpMetaDataWriter.writeXmpMetadata(outputFile, metadata);
        }
        catch (Exception e) {
            Log.e(TAG, "AVIF encoding failed.", e);
        }
        finally {
            // The Bitmap should be recycled to free up memory.
            if (rotatedBitmap != null && !rotatedBitmap.isRecycled()) {
                rotatedBitmap.recycle();
            }
        }
    }

    public static void saveBitmapToPng(Bitmap bitmap, File outputFile, int quality) {
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, quality, out);
            Log.d(TAG, "PNG saved successful: " + outputFile.getPath());
        } catch (IOException e) {
            Log.e(TAG, "PNG saved FAILED", e);
        }
    }

    public static void saveBitmapToWebP(Bitmap bitmap, File outputFile, int quality) {
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            if (quality > 0) {
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, out);
            } else {
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, quality, out);
            }
            Log.d(TAG, "WebP saved successful: " + outputFile.getPath());
        } catch (IOException e) {
            Log.e(TAG, "WebP saved FAILED", e);
        }
    }
}
