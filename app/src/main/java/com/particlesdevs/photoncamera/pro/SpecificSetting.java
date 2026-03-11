package com.particlesdevs.photoncamera.pro;


//Device API specifics
public class SpecificSetting {
    public boolean isDualSessionSupported = false;
    public boolean isRawColorCorrection = false;
    public String[] cameraIDS;
    public float[] apertureList;
    // QualityDoesMatter
    public boolean statisticsHotPixelMapMode = true;
    public boolean useCodeAuroraMultiFrameNoiseReduction = false;
    public boolean useCodeAuroraCinematicMode = false;
    public boolean modeShowUnlimited = true;
    public boolean modeShowMotion = true;
    public boolean modeShowNight = true;
    public boolean modeShowRawVideo = true;
    public boolean showZoomSlider = false;
    public boolean enableQLL = false;
    public boolean enableVideoLut = false;
    public int statisticsLensShadingMapMode = 99;
    public int statisticsOisDataMode = 99;
    public int blackLevelValue = 0;
    public float toneMapGamma = 99;
    public int colorTemperature = 99;
    public int sessionType = 0;
    public int sessionTypeVideo = 0;
    public int priorityShutterSpeed = 0;
    public int priorityIsoValue = 0;
    public int priorityMode = 0;
    public int mfnrFrames = 0;
    public int xiaomiSupernightMode = 0;
    public int codeAuroraTemporalNoiseProcessType = 0;
    public int codeAuroraDCGMode = 0;
    public int codeAuroraEnableHDRDCGMode = 0;
    public int qtiDCGMode = 0;
    public int qtiImageStabilizationMode = 0;
    public int xiaomiHdrMode = 1;
    public float colorTint = 99.0f;
    public String recPrefix = "";
    public String newRecSurfaceType = "COLOR_FormatSurface";
    public String customRawRes = "";
    public String codeAuroraHdrMode = "default"; // SHDR, QHDR, MFHDR, AUTO
    public String hdrMode = "HLG10";
    public String networkSyncBaseUrl = "https://raw.githubusercontent.com/eszdman/PhotonCamera/dev/app/";
    public String toneMappingMode = "quality";
    public SpecificSetting(){
    }
}
