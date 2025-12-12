package com.particlesdevs.photoncamera.processing;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.exifinterface.media.ExifInterface;

import com.particlesdevs.photoncamera.util.Log;
import com.radzivon.bartoshyk.avif.coder.HeifCoder;
import com.radzivon.bartoshyk.avif.coder.HeifQualityArg;
import com.radzivon.bartoshyk.avif.coder.PreciseMode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class HeifEncoder {

    private static final String TAG = "HeifEncoder";

    /**
     * Encodes an Image object by first converting it to a Bitmap and then to HEIF.
     *
     * @param image      The YUV Image object to encode.
     * @param outputFile The target file for the HEIF image.
     * @throws IOException If encoding or writing the file fails.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void encodeYuvToHeif(Image image, File outputFile, int orientation, int quality, Bundle metadata) throws IOException {
        Log.d(TAG, "Starting HEIF encoding for image with resolution: " + image.getWidth() + "x" + image.getHeight());

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
            // 3. Encode the Bitmap to HEIF using the correct method signature.
            //    Parameters based on the screenshot. The last 3 are enums.
            //    Let's use reasonable defaults.
            byte[] heifByteArray = coder.encodeHeic(rotatedBitmap, PreciseMode.LOSSY, new HeifQualityArg.Quality(quality));

            // 4. Verify that the encoder returned data.
            if (heifByteArray == null || heifByteArray.length == 0) {
                throw new IOException("HEIF encoder returned null or empty data. Encoding failed.");
            }

            // 5. Write the data to the output file.
            java.nio.file.Files.write(outputFile.toPath(), heifByteArray);
            Log.d(TAG, "Successfully saved HEIF file to: " + outputFile.getAbsolutePath() + " (" + heifByteArray.length / 1024 + " KB)");

            XmpMetaDataWriter.writeXmpMetadata(outputFile, metadata);
        } finally {
            // The Bitmap should be recycled to free up memory.
            if (rotatedBitmap != null && !rotatedBitmap.isRecycled()) {
                rotatedBitmap.recycle();
            }
        }
    }
}
