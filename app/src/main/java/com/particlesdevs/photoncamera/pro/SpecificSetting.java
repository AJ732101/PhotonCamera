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
    public boolean modeShowUnlimited = true;
    public boolean modeShowMotion = true;
    public boolean modeShowNight = true;
    public boolean modeShowRawVideo = true;
    public boolean showZoomSlider = false;
    public int statisticsLensShadingMapMode = 99;
    public int statisticsOisDataMode = 99;
    public float toneMapGamma = 99;
    public int colorTemperature = 99;
    public int sessionType = 0;
    public int priorityShutterSpeed = 0;
    public int priorityIsoValue = 0;
    public int priorityMode = 0;
    public float colorTint = 99.0f;
    public String newRecColorRange = "full";
    public String recPrefix = "";
    public String newRecSurfaceType = "COLOR_FormatSurface";
    public String customRawRes = "";
    public String codeAuroraHdrMode = "default"; // SHDR, QHDR, MFHDR, AUTO
    public String hdrMode = "HLG10";
    public SpecificSetting(){
    }
}
