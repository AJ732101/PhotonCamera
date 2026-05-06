package com.particlesdevs.photoncamera.processing.opengl.scripts;

import android.annotation.SuppressLint;

import com.particlesdevs.photoncamera.util.Log;

import com.particlesdevs.photoncamera.processing.opengl.GLBuffer;
import com.particlesdevs.photoncamera.processing.opengl.GLContext;
import com.particlesdevs.photoncamera.processing.opengl.GLFormat;
import com.particlesdevs.photoncamera.processing.opengl.GLImage;
import com.particlesdevs.photoncamera.processing.opengl.GLProg;
import com.particlesdevs.photoncamera.processing.opengl.GLTexture;


public class ABL implements AutoCloseable{
    private static final String TAG = "ABL";
    GLContext context;
    int histSize;
    public int resize = 3;
    public float[] exposure = new float[4];
    public ABL(GLContext context) {
        this(context,256);
    }
    public ABL(GLContext context, int size) {
        histSize = size;
        this.context = context;
        Log.d(TAG, "Creating ABL with size: " + size);
        for (int i = 0; i < 4; i++) {
            exposure[i] = 1.0f;
        }
    }
    @SuppressLint("DefaultLocale")
    public float[] Compute(double minExposureMpy, double maxEV, double noise, GLTexture input) {
        GLHistogram histogram = new GLHistogram(context, histSize);
        histogram.Rc = true;
        histogram.Gc = true;
        histogram.Bc = true;
        histogram.Ac = false;
        double minNoiseVal = Math.pow(2.0, -maxEV);
        double expoSearch = minExposureMpy/(noise + minNoiseVal); // Search for exposure based on noise level
        Log.d(TAG, "Exposure Search Value: " + expoSearch);
        histogram.exposure[0] = (float) (expoSearch);
        histogram.exposure[1] = (float) (expoSearch);
        histogram.exposure[2] = (float) (expoSearch);

        int[][] hist = histogram.Compute(input);

        histogram.close();

        // Use bruteforce method to find optimal black levels that minimize color shifting
        //float[] blackLevels = bruteforceOptimalBlackLevels(hist);
        float[] blackLevels = new float[3];
        blackLevels[0] = calculateBlackLevel(hist[0]);
        blackLevels[1] = calculateBlackLevel(hist[1]);
        blackLevels[2] = calculateBlackLevel(hist[2]);

        for (int i = 0; i < blackLevels.length; i++) {
            //blackLevels[i] = calculateBlackLevel(hist[i]);
            blackLevels[i] /= (float) (expoSearch);
        }

        Log.d(TAG, String.format("Bruteforce Black Levels - R: %.4f, G: %.4f, B: %.4f",
                blackLevels[0], blackLevels[1], blackLevels[2]));

        return blackLevels;
    }

    @Override
    public void close() {
    }

    /**
     * Calculate black level from histogram data
     * Uses percentile-based approach to find appropriate black point
     */
    private float calculateBlackLevel(int[] histogram) {
        if (histogram == null || histogram.length == 0) {
            return 0.0f;
        }

        // Calculate total pixel count
        long totalPixels = 0;
        for (int count : histogram) {
            totalPixels += count;
        }

        if (totalPixels == 0) {
            return 0.0f;
        }


        long targetPixels = totalPixels / 100; // 1% of pixels
        long currentPixels = 0;

        for (int i = 0; i < histogram.length; i++) {
            currentPixels += histogram[i];
            if (currentPixels >= targetPixels) {
                // Convert bin index to normalized value (0.0 - 1.0)
                float blackLevel = (float) i / (histogram.length - 1);

                // Apply some constraints to avoid extreme values
                blackLevel = Math.min(blackLevel, 0.5f); // Max 50% black level
                blackLevel = Math.max(blackLevel, 0.0f); // Min 0% black level
                Log.d(TAG, "Calculated black level for channel: " + blackLevel);
                return blackLevel;
            }
        }

        return 0.0f;
    }
}
