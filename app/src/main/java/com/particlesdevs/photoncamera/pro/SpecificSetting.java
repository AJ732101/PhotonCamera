package com.particlesdevs.photoncamera.pro;


//Device API specifics
public class SpecificSetting {
    public boolean isDualSessionSupported = false;
    public boolean isRawColorCorrection = false;
    public String[] cameraIDS;
    public float[] apertureList;
    // QualityDoesMatter
    public int exposureCompensation = 99;
    public boolean statisticsHotPixelMapMode = true;
    public boolean useExternalViewer = false;
    public boolean useCodeAuroraMultiFrameNoiseReduction = false;
    public boolean useCodeAuroraCinematicMode = false;
    public int statisticsLensShadingMapMode = 99;
    public int statisticsOisDataMode = 99;
    public float toneMapGamma = 99;
    public int colorTemperature = 99;
    public float colorTint = 99.0f;
    public int newRecKeyFrameIntervall = 10;
    public int effectMode = 99;
    public String contrastCurve = "off";
    public String newRecColorRange = "full";
    public String recPrefix = "";
    public String newRecSurfaceType = "COLOR_FormatSurface";
    public String customRawRes = "";
    public String codeAuroraHdrMode = "default"; // SHDR, QHDR, MFHDR, AUTO
    public SpecificSetting(){
    }
}
