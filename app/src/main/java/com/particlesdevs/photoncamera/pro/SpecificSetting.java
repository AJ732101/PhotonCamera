package com.particlesdevs.photoncamera.pro;


//Device API specifics
public class SpecificSetting {
    public boolean isDualSessionSupported = false;
    public boolean isRawColorCorrection = false;
    public String[] cameraIDS;
    public float[] apertureList;
    // QualityDoesMatter
    public boolean forceNewSettingsInRegularPhotoMode = false;
    public boolean useNewRecordingPipeline = false;
    public int hotPixelMode = 99;
    public int colorCorrectionAberrationMode = 99;
    public int distortionCorrectionMode = 99;
    public int shadingMode = 99;
    public int exposureCompensation = 99;
    public boolean statisticsHotPixelMapMode = true;
    public int statisticsLensShadingMapMode = 99;
    public int statisticsOisDataMode = 99;
    public float toneMapGamma = 99;
    public float singleShotZoomFactor = 99;
    public int colorTemperature = 99;
    public int newRecKeyFrameIntervall = 10;
    public String contrastCurve = "off";
    public String newRecColorRange = "full";
    public String recPrefix = "";
    public SpecificSetting(){
    }
}
