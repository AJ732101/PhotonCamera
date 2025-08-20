package com.particlesdevs.photoncamera.pro;


//Device API specifics
public class SpecificSetting {
    public boolean isDualSessionSupported = false;
    public boolean isRawColorCorrection = false;
    public boolean isOisOn = true;
    public boolean isEssentialOsd = true;
    public boolean isQuadBayer = true;
    public boolean isHighBitrate = true;
    public boolean isExposureExtended = false;
    public boolean isIsoExtended = false;
    public boolean isH265 = true;
    public boolean is8k = false;
    public boolean is24fps = false;
    public int[] cameraIDS;
    public float[] apertureList;
    public float apertureToUse = 4.0f;
    public String setPhysicalCameraId = "2";
    public String physicalCameraIdTarget = "0";
    public SpecificSetting(){
    }
}
