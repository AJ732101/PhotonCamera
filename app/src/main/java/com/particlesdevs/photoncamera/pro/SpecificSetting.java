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
    public boolean xiaomi14Ultra2xHack = false;
    public boolean useExternalViewer = false;
    public boolean useSceneAndEffectMode = false;
    public boolean useAlternatePreviewTemplate = false;
    public boolean useCodeAuroraMultiFrameNoiseReduction = false;
    public boolean useCodeAuroraCinematicMode = false;
    public boolean createSingleShotThumbnail = false;
    public int statisticsLensShadingMapMode = 99;
    public int statisticsOisDataMode = 99;
    public float toneMapGamma = 99;
    public float singleShotZoomFactor = 99;
    public int colorTemperature = 99;
    public float colorTint = 99.0f;
    public int newRecKeyFrameIntervall = 10;
    public int effectMode = 99;
    public int codeAuroraSharpnessStrength = 0;
    public int codeAuroraEisMode = 0;
    public int codeAuroraSaturation = 99;    //X13U [0 10 5 1], 99 = off
    public int codeAuroraAiMode = 99;
    public String contrastCurve = "off";
    public String newRecColorRange = "full";
    public String recPrefix = "";
    public String YCBCR_P010_TargetFormat = "HEVC";
    public String rawFormat = "RAW_SENSOR";
    public String newRecSurfaceType = "COLOR_FormatSurface";
    public String customRawRes = "";
    public String codeAuroraHdrMode = "default"; // SHDR, QHDR, MFHDR, AUTO
    public String previewFormat = "YUV_420_888"; // YUV_420_888, YCBCR_P010
    public SpecificSetting(){
    }
}
