package com.particlesdevs.photoncamera.processing;

public final class CurvePresets {

    private CurvePresets() {}

    public static final float[] HIGH_CONTRAST_POINTS = {
            0.0f, 0.0f,
            0.25f, 0.15f,
            0.50f, 0.50f,
            0.75f, 0.85f,
            1.0f, 1.0f
    };

    public static final float[] LOW_CONTRAST_POINTS = {
            0.00f, 0.15f,
            0.25f, 0.30f,
            0.50f, 0.50f,
            0.75f, 0.70f,
            1.00f, 0.85f
    };

    public static final float[] LINEAR_CURVE = {
            0.00f, 0.00f,
            0.25f, 0.25f,
            0.50f, 0.50f,
            0.75f, 0.75f,
            1.00f, 1.00f
    };

    public static final float[] SLOG2_APPROX_POINTS_A = {
            0.00f, 0.031f,
            0.10f, 0.283f,
            0.20f, 0.384f,
            0.30f, 0.457f,
            0.40f, 0.516f,
            0.50f, 0.567f,
            0.60f, 0.612f,
            0.70f, 0.654f,
            0.80f, 0.693f,
            0.90f, 0.730f,
            1.00f, 0.765f
    };

    public static final float[] SLOG2_APPROX_POINTS_B = {
            0.00f, 0.030f,
            0.10f, 0.150f,
            0.20f, 0.250f,
            0.30f, 0.350f,
            0.40f, 0.450f,
            0.50f, 0.530f,
            0.60f, 0.610f,
            0.70f, 0.700f,
            0.80f, 0.800f,
            0.90f, 0.900f,
            1.00f, 0.950f
    };

    public static final float[] RED_CURVE_STYLE_1 = new float[]{
            0.0f, 0.0f,
            0.25f, 0.180f,
            0.50f, 0.480f,
            0.75f, 0.750f,
            1.0f, 1.0f
    };

    public static final float[] GREEN_CURVE_STYLE_1 = new float[]{
            0.0f, 0.0f,
            0.25f, 0.185f,
            0.50f, 0.480f,
            0.75f, 0.740f,
            1.0f, 1.0f
    };

    public static final float[] BLUE_CURVE_STYLE_1 = new float[]{
            0.0f, 0.0f,
            0.25f, 0.235f,
            0.50f, 0.480f,
            0.75f, 0.690f,
            1.0f, 1.0f
    };
}
