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
    public boolean useCodeAuroraMultiFrameNoiseReduction = false;
    public boolean useCodeAuroraCinematicMode = false;
    public boolean modeShowUnlimited = false;
    public boolean modeShowMotion = false;
    public boolean modeShowNight = false;
    public boolean modeShowRawVideo = false;
    public int statisticsLensShadingMapMode = 99;
    public int statisticsOisDataMode = 99;
    public float toneMapGamma = 99;
    public int colorTemperature = 99;
    public float colorTint = 99.0f;
    public String newRecColorRange = "full";
    public String recPrefix = "";
    public String newRecSurfaceType = "COLOR_FormatSurface";
    public String customRawRes = "";
    public String codeAuroraHdrMode = "default"; // SHDR, QHDR, MFHDR, AUTO
    public SpecificSetting(){
    }
}
