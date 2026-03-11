package com.particlesdevs.photoncamera.processing;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.exifinterface.media.ExifInterface;

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
import java.util.concurrent.ExecutionException;

public class AvifEncoder {

    private static final String TAG = "AvifEncoder";

    /**
     * Encodes an Image object by first converting it to a Bitmap and then to AVIF.
     *
     * @param image      The YUV Image object to encode.
     * @param outputFile The target file for the AVIF image.
     * @throws IOException If encoding or writing the file fails.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void encodeYuvToAvif(Image image, File outputFile, int orientation, int quality, Bundle metadata, MainRenderer renderer) throws IOException {
        Log.d(TAG, "Starting AVIF encoding for image with resolution: " + image.getWidth() + "x" + image.getHeight());

        // 1. Convert the YUV Image to an ARGB Bitmap.
        Bitmap originalBitmap = null;
        Bitmap rotatedBitmap = null;

        // 1. Convert the YUV Image to a high-precision Bitmap
        switch (image.getFormat()) {
            case ImageFormat.YUV_420_888:
                originalBitmap = ImageUtils.yuv8BitToBitmap(image);
                break;
            case ImageFormat.YCBCR_P010:
                originalBitmap = ImageUtils.p010SdrToBitmap1010102(image);
                //originalBitmap = ImageUtils.p010SdrToF16BitmapGL(image, renderer);
            break;
        }

        // 2. CORRECT: Physically rotate the Bitmap if needed
        if (orientation != 0) {
            Log.d(TAG, "Rotating bitmap by " + orientation + " degrees.");
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.postRotate(orientation);
            // Create a new, rotated bitmap from the original
            rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
            // Important: Free up memory from the original bitmap immediately
            originalBitmap.recycle();
        }
        else {
            // If no rotation is needed, we just use the original bitmap
            rotatedBitmap = originalBitmap;
        }


        // 2. Create an instance of HeifCoder.
        HeifCoder coder = new HeifCoder();

        try {
            // 3. Encode the Bitmap to AVIF using the correct method signature.
            //    Parameters based on the screenshot. The last 3 are enums.
            //    Let's use reasonable defaults.
            byte[] avifByteArray = null;
            if (PhotonCamera.getSettings().useLosslessSwEncoding) {
                avifByteArray = coder.encodeAvif(rotatedBitmap, quality, AvifSpeed.EIGHT, PreciseMode.LOSSLESS, AvifSurfaceMode.AUTO, AvifChromaSubsampling.YUV420);
            }
            else {
                avifByteArray = coder.encodeAvif(rotatedBitmap, quality, AvifSpeed.EIGHT, PreciseMode.LOSSY, AvifSurfaceMode.AUTO, AvifChromaSubsampling.YUV420);
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
