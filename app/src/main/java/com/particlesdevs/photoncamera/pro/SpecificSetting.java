package com.particlesdevs.photoncamera.pro;


//Device API specifics
public class SpecificSetting {
    public boolean isDualSessionSupported = false;
    public boolean isRawColorCorrection = false;
    public String[] cameraIDS;
    public float[] apertureList;
    // QualityDoesMatter
    public boolean statisticsHotPixelMapMode = true;
    public boolean useCodeAuroraCinematicMode = false;
    public boolean modeShowUnlimited = true;
    public boolean modeShowMotion = true;
    public boolean modeShowNight = true;
    public boolean modeShowRawVideo = true;
    public boolean enableQLL = false;
    public boolean enableVideoLut = false;
    public boolean vivoUseSuperEis = false;
    public boolean vivoUseProRaw = false;
    public boolean vivoUseQcomSolution = false;
    public boolean vivoUseUltraHighResolution = false;
    public int vivoEngineerRemosaicMode = -1;
    public int statisticsLensShadingMapMode = 99;
    public int statisticsOisDataMode = 99;
    public float toneMapGamma = 99;
    public int priorityShutterSpeed = 0;
    public int priorityIsoValue = 0;
    public int priorityMode = 0;
    public int xiaomiMfnrFrames = 0;
    public int xiaomiSupernightMode = 0;
    public int codeAuroraDCGMode = 0;
    public int codeAuroraEnableHDRDCGMode = 0;
    public int qtiDCGMode = 0;
    public int qtiImageStabilizationMode = 0;
    public int xiaomiHdrMode = 1;
    public String[] sensorModes;
    public String sensorModeKey = "vivo.control.forceSensorMode";
    public int vivoVideoMode = -1;
    public int vivoEisConfig = -1;
    public int vivoEisEnhance = -1;
    public int androidDemosaicMode = -1;
    public int targetFps = -1;
    public String recPrefix = "";
    public String newRecSurfaceType = "COLOR_FormatSurface";
    public String customRawRes = "";
    public String hdrMode = "HLG10";
    public String[] customVendorKeyTypeByteName;
    public String[] customVendorKeyTypeInt32Name;
    public String[] customVendorKeyTypeInt64Name;
    public String[] customVendorKeyTypeFloatName;
    public int[] customVendorKeyTypeByteValue;
    public int[] customVendorKeyTypeInt32Value;
    public long[] customVendorKeyTypeInt64Value;
    public float[] customVendorKeyTypeFloatValue;
    public String networkSyncBaseUrl = "https://raw.githubusercontent.com/eszdman/PhotonCamera/dev/app/";
    public float ultraHdrThreshold = 0.85f;
    public float ultraHdrMaxBoost = 2.0f;
    public float ultraHdrGamma = 1.5f;
    public SpecificSetting(){
    }
}
