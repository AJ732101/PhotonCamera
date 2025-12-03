package com.particlesdevs.photoncamera.processing;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;
import android.os.Build;
import androidx.annotation.RequiresApi;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Utility class for Image conversions.
 * Contains methods for 8-bit SDR and 10-bit SDR/HDR data.
 */
public class ImageUtils {

    // Constants for HLG EOTF (Electro-Optical Transfer Function)
    private static final float HLG_A = 0.17883277f;
    private static final float HLG_B = 0.28466892f;
    private static final float HLG_C = 0.55991073f;

    /**
     * Converts a 10-bit YCBCR_P010 (BT.2020 HLG) Image to a high bit-depth RGBA_1010102 Bitmap.
     * @param image The 10-bit YUV Image object with HLG data.
     * @return A Bitmap in RGBA_1010102 format, ready for the encoder.
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static Bitmap p010HdrToBitmap1010102(Image image) {
        if (image.getFormat() != ImageFormat.YCBCR_P010) {
            throw new IllegalArgumentException("Image must be in YCBCR_P010 format");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        Image.Plane yPlane = image.getPlanes()[0];
        Image.Plane uvPlane = image.getPlanes()[1];

        ByteBuffer yBuffer = yPlane.getBuffer().order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer uvBuffer = uvPlane.getBuffer().order(ByteOrder.LITTLE_ENDIAN);

        int yRowStride = yPlane.getRowStride();
        int uvRowStride = uvPlane.getRowStride();
        int uvPixelStride = uvPlane.getPixelStride();

        int[] pixels = new int[width * height];

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                // Step 1: Read 10-bit Y, U, V values
                int y_10bit = (yBuffer.getShort(j * yRowStride + i * 2) & 0xFFFF) >> 6;
                int uvIndex = (j / 2) * uvRowStride + (i / 2) * uvPixelStride;
                int u_10bit = (uvBuffer.getShort(uvIndex) & 0xFFFF) >> 6;
                int v_10bit = (uvBuffer.getShort(uvIndex + 2) & 0xFFFF) >> 6;

                // Step 2: Convert from limited-range Y'Cb'Cr' to full-range float
                float y_prime = (y_10bit - 64.f) / (940.f - 64.f);
                float cb_prime = (u_10bit - 512.f) / (960.f - 64.f);
                float cr_prime = (v_10bit - 512.f) / (960.f - 64.f);

                // Step 3: Y'Cb'Cr' to R'G'B' conversion with BT.2020 matrix
                float r_prime = y_prime + 1.4746f * cr_prime;
                float g_prime = y_prime - 0.16455f * cb_prime - 0.57135f * cr_prime;
                float b_prime = y_prime + 1.8814f * cb_prime;

                // Step 4: Apply HLG Inverse EOTF to linearize the signal
                float r_linear = hlgToLinear(r_prime);
                float g_linear = hlgToLinear(g_prime);
                float b_linear = hlgToLinear(b_prime);

                // Step 5: Clamp and quantize to 10-bit integer range
                int r_10bit = Math.max(0, Math.min(1023, (int)(r_linear * 1023.0f + 0.5f)));
                int g_10bit = Math.max(0, Math.min(1023, (int)(g_linear * 1023.0f + 0.5f)));
                int b_10bit = Math.max(0, Math.min(1023, (int)(b_linear * 1023.0f + 0.5f)));

                // Step 6: Pack into RGBA_1010102 format (A, B, G, R order)
                int alpha = 3; // Full opacity
                int packed = (alpha << 30) | (b_10bit << 20) | (g_10bit << 10) | r_10bit;
                pixels[j * width + i] = packed;
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.RGBA_1010102);
    }

    /**
     * Converts a 10-bit YCBCR_P010 (Rec. 709, SDR) Image to a RGBA_1010102 Bitmap.
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static Bitmap p010SdrToBitmap1010102(Image image) {
        // ... (This method remains the same as in the previous correct answer)
        if (image.getFormat() != ImageFormat.YCBCR_P010) {
            throw new IllegalArgumentException("Image must be in YCBCR_P010 format");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        Image.Plane yPlane = image.getPlanes()[0];
        Image.Plane uvPlane = image.getPlanes()[1];
        ByteBuffer yBuffer = yPlane.getBuffer().order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer uvBuffer = uvPlane.getBuffer().order(ByteOrder.LITTLE_ENDIAN);
        int yRowStride = yPlane.getRowStride();
        int uvRowStride = uvPlane.getRowStride();
        int uvPixelStride = uvPlane.getPixelStride();
        int[] pixels = new int[width * height];
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int y = (yBuffer.getShort(j * yRowStride + i * 2) & 0xFFFF) >> 6;
                int uvIndex = (j / 2) * uvRowStride + (i / 2) * uvPixelStride;
                int u = (uvBuffer.getShort(uvIndex) & 0xFFFF) >> 6;
                int v = (uvBuffer.getShort(uvIndex + 2) & 0xFFFF) >> 6;
                int c = y - 64;
                int d = u - 512;
                int e = v - 512;
                int r = (298 * c + 409 * e + 128) >> 8;
                int g = (298 * c - 100 * d - 208 * e + 128) >> 8;
                int b = (298 * c + 516 * d + 128) >> 8;
                r = Math.max(0, Math.min(1023, r));
                g = Math.max(0, Math.min(1023, g));
                b = Math.max(0, Math.min(1023, b));
                int alpha = 3;
                int packed = (alpha << 30) | (b << 20) | (g << 10) | r;
                pixels[j * width + i] = packed;
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.RGBA_1010102);
    }

    /**
     * Converts a YUV_420_888 (8-bit) Image to a standard ARGB_8888 Bitmap.
     */
    public static Bitmap yuv8BitToBitmap(Image image) {
        // ... (This method remains the same as in the previous correct answer)
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("Image must be in YUV_420_888 format");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        Image.Plane[] planes = image.getPlanes();
        Image.Plane yPlane = planes[0];
        Image.Plane uPlane = planes[1];
        Image.Plane vPlane = planes[2];
        ByteBuffer yBuffer = yPlane.getBuffer();
        ByteBuffer uBuffer = uPlane.getBuffer();
        ByteBuffer vBuffer = vPlane.getBuffer();
        byte[] yData = new byte[yBuffer.remaining()];
        byte[] uData = new byte[uBuffer.remaining()];
        byte[] vData = new byte[vBuffer.remaining()];
        yBuffer.get(yData);
        uBuffer.get(uData);
        vBuffer.get(vData);
        int[] argb = new int[width * height];
        int yRowStride = yPlane.getRowStride();
        int uvRowStride = uPlane.getRowStride();
        int uvPixelStride = uPlane.getPixelStride();
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int y = (yData[j * yRowStride + i] & 0xFF);
                int uvIndex = (j / 2) * uvRowStride + (i / 2) * uvPixelStride;
                int u = (uData[uvIndex] & 0xFF) - 128;
                int v = (vData[uvIndex] & 0xFF) - 128;
                int r = (int) (y + 1.402f * v);
                int g = (int) (y - 0.344f * u - 0.714f * v);
                int b = (int) (y + 1.772f * u);
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));
                argb[j * width + i] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.setPixels(argb, 0, width, 0, 0, width, height);
        return bmp;
    }

    private static float hlgToLinear(float hlgValue) {
        hlgValue = Math.max(0.0f, hlgValue);
        if (hlgValue <= 0.5f) {
            return (hlgValue * hlgValue) / 3.0f;
        } else {
            return ((float)Math.exp((hlgValue - HLG_C) / HLG_A) + HLG_B) / 12.0f;
        }
    }
}
