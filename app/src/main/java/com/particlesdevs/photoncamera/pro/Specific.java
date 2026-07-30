package com.particlesdevs.photoncamera.pro;

import android.content.Context;
import android.os.Build;

import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.util.Log;

import com.particlesdevs.photoncamera.settings.PreferenceKeys;
import com.particlesdevs.photoncamera.settings.SettingsManager;
import com.particlesdevs.photoncamera.util.HttpLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
//import java.util.Set;

import static com.particlesdevs.photoncamera.settings.PreferenceKeys.Key.ALL_DEVICES_NAMES_KEY;
import static com.particlesdevs.photoncamera.util.FileManager.sPHOTON_TUNING_DIR;

public class Specific {
    private static final String TAG = "Specific";

    public boolean isLoaded = false;
    public SpecificSetting specificSetting = new SpecificSetting();
    public float[] blackLevel;
    private final SettingsManager mSettingsManager;

    public Specific(SettingsManager mSettingsManager) {
        this.mSettingsManager = mSettingsManager;
    }
    ArrayList<String> loadNetwork(String device) throws IOException {
        ArrayList<String> inputStr = new ArrayList<String>();
        BufferedReader indevice = HttpLoader.readURL(PhotonCamera.getSpecific().specificSetting.networkSyncBaseUrl + "specific/" + device + "_specificsettings.txt", 200);
        String str;
        while ((str = indevice.readLine()) != null) {
            Log.d("Specific", "read:" + str);
            inputStr.add(str + "\n");
        }
        return inputStr;
    }
    ArrayList<String> loadLocal(File specifics) throws IOException {
        ArrayList<String> inputStr = new ArrayList<String>();
        String str;
        BufferedReader indevice = new BufferedReader(new FileReader(specifics));
        while ((str = indevice.readLine()) != null) {
            Log.d("Specific", "read:" + str);
            inputStr.add(str + "\n");
        }
        return inputStr;
    }
    public void loadSpecific(){
        // load all that has no GUI element yet and do this without any conditions
        ArrayList<String> noGuiYetStr = new ArrayList<String>();
        File noGuiYet = new File(sPHOTON_TUNING_DIR, "NoGuiYet.txt");
        try {
            var ret = noGuiYet.canRead();
            if (noGuiYet.exists() && !PreferenceKeys.disableNoGuiYet()) {
                noGuiYetStr = loadLocal(noGuiYet);
            }
        } catch (Exception e) {
            Log.e(TAG,e.toString());
        }

        for (String str : noGuiYetStr) {
            String[] caseS = str.replace(" ", "").replace("\n", "").split("=");
            switch (caseS[0]) {
                case "statisticsHotPixelMapMode": {
                    specificSetting.statisticsHotPixelMapMode = Boolean.parseBoolean(caseS[1]);
                    break;
                }
                case "useCodeAuroraCinematicMode": {
                    specificSetting.useCodeAuroraCinematicMode = Boolean.parseBoolean(caseS[1]);
                    break;
                }
                case "modeShowUnlimited": {
                    specificSetting.modeShowUnlimited = Boolean.parseBoolean(caseS[1]);
                    break;
                }
                case "modeShowMotion": {
                    specificSetting.modeShowMotion = Boolean.parseBoolean(caseS[1]);
                    break;
                }
                case "modeShowNight": {
                    specificSetting.modeShowNight = Boolean.parseBoolean(caseS[1]);
                    break;
                }
                case "modeShowRawVideo": {
                    specificSetting.modeShowRawVideo = Boolean.parseBoolean(caseS[1]);
                    break;
                }
                case "enableQLL": {
                    specificSetting.enableQLL = Boolean.parseBoolean(caseS[1]);
                    break;
                }
                case "enableVideoLut": {
                    specificSetting.enableVideoLut = Boolean.parseBoolean(caseS[1]);
                    break;
                }
                case "vivoUseSuperEis": {
                    specificSetting.vivoUseSuperEis = Boolean.parseBoolean(caseS[1]);
                    break;
                }
                case "vivoUseProRaw": {
                    specificSetting.vivoUseProRaw = Boolean.parseBoolean(caseS[1]);
                    break;
                }
                case "vivoUseQcomSolution": {
                    specificSetting.vivoUseQcomSolution = Boolean.parseBoolean(caseS[1]);
                    break;
                }
                case "vivoUseUltraHighResolution": {
                    specificSetting.vivoUseUltraHighResolution = Boolean.parseBoolean(caseS[1]);
                    break;
                }
                case "vivoEngineerRemosaicMode": {
                    specificSetting.vivoEngineerRemosaicMode = Integer.parseInt(caseS[1]);
                    break;
                }
                case "statisticsLensShadingMapMode": {
                    specificSetting.statisticsLensShadingMapMode = Integer.parseInt(caseS[1]);
                    break;
                }
                case "statisticsOisDataMode": {
                    specificSetting.statisticsOisDataMode = Integer.parseInt(caseS[1]);
                    break;
                }
                case "toneMapGamma": {
                    specificSetting.toneMapGamma = Float.parseFloat(caseS[1]);
                    break;
                }
                case "priorityShutterSpeed": {
                    specificSetting.priorityShutterSpeed = Integer.parseInt(caseS[1]);
                    break;
                }
                case "priorityIsoValue": {
                    specificSetting.priorityIsoValue = Integer.parseInt(caseS[1]);
                    break;
                }
                case "priorityMode": {
                    specificSetting.priorityMode = Integer.parseInt(caseS[1]);
                    break;
                }
                case "xiaomiMfnrFrames": {
                    specificSetting.xiaomiMfnrFrames = Integer.parseInt(caseS[1]);
                    break;
                }
                case "xiaomiSupernightMode": {
                    specificSetting.xiaomiSupernightMode = Integer.parseInt(caseS[1]);
                    break;
                }
                case "codeAuroraDCGMode": {
                    specificSetting.codeAuroraDCGMode = Integer.parseInt(caseS[1]);
                    break;
                }
                case "codeAuroraEnableHDRDCGMode": {
                    specificSetting.codeAuroraEnableHDRDCGMode = Integer.parseInt(caseS[1]);
                    break;
                }
                case "qtiDCGMode": {
                    specificSetting.qtiDCGMode = Integer.parseInt(caseS[1]);
                    break;
                }
                case "qtiImageStabilizationMode": {
                    specificSetting.qtiImageStabilizationMode = Integer.parseInt(caseS[1]);
                    break;
                }
                case "xiaomiHdrMode": {
                    specificSetting.xiaomiHdrMode = Integer.parseInt(caseS[1]);
                    break;
                }
                case "vivoVideoMode": {
                    specificSetting.vivoVideoMode = Integer.parseInt(caseS[1]);
                    break;
                }
                case "vivoEisConfig": {
                    specificSetting.vivoEisConfig = Integer.parseInt(caseS[1]);
                    break;
                }
                case "vivoEisEnhance": {
                    specificSetting.vivoEisEnhance = Integer.parseInt(caseS[1]);
                    break;
                }
                case "androidDemosaicMode": {
                    specificSetting.androidDemosaicMode = Integer.parseInt(caseS[1]);
                    break;
                }
                case "targetFps": {
                    specificSetting.targetFps = Integer.parseInt(caseS[1]);
                    break;
                }
                case "recPrefix": {
                    specificSetting.recPrefix = caseS[1];
                    break;
                }
                case "newRecSurfaceType": {
                    specificSetting.newRecSurfaceType = caseS[1];
                    break;
                }
                case "customRawRes": {
                    specificSetting.customRawRes = caseS[1];
                    break;
                }
                case "hdrMode": {
                    specificSetting.hdrMode = caseS[1];
                    break;
                }
                case "networkSyncBaseUrl": {
                    specificSetting.networkSyncBaseUrl = caseS[1];
                    break;
                }
                case "customVendorKeyTypeByteName": {
                    String[] ids = caseS[1].replace("{", "").replace("}", "").split(",");
                    specificSetting.customVendorKeyTypeByteName = new String[ids.length];
                    for (int i = 0; i < specificSetting.customVendorKeyTypeByteName.length; i++) {
                        specificSetting.customVendorKeyTypeByteName[i] = ids[i];
                    }
                    break;
                }
                case "customVendorKeyTypeInt32Name": {
                    String[] ids = caseS[1].replace("{", "").replace("}", "").split(",");
                    specificSetting.customVendorKeyTypeInt32Name = new String[ids.length];
                    for (int i = 0; i < specificSetting.customVendorKeyTypeInt32Name.length; i++) {
                        specificSetting.customVendorKeyTypeInt32Name[i] = ids[i];
                    }
                    break;
                }
                case "customVendorKeyTypeInt64Name": {
                    String[] ids = caseS[1].replace("{", "").replace("}", "").split(",");
                    specificSetting.customVendorKeyTypeInt64Name = new String[ids.length];
                    for (int i = 0; i < specificSetting.customVendorKeyTypeInt64Name.length; i++) {
                        specificSetting.customVendorKeyTypeInt64Name[i] = ids[i];
                    }
                    break;
                }
                case "customVendorKeyTypeFloatName": {
                    String[] ids = caseS[1].replace("{", "").replace("}", "").split(",");
                    specificSetting.customVendorKeyTypeFloatName = new String[ids.length];
                    for (int i = 0; i < specificSetting.customVendorKeyTypeFloatName.length; i++) {
                        specificSetting.customVendorKeyTypeFloatName[i] = ids[i];
                    }
                    break;
                }
                case "customVendorKeyTypeByteValue": {
                    String[] ids = caseS[1].replace("{", "").replace("}", "").split(",");
                    specificSetting.customVendorKeyTypeByteValue = new int[ids.length];
                    for (int i = 0; i < specificSetting.customVendorKeyTypeByteValue.length; i++) {
                        specificSetting.customVendorKeyTypeByteValue[i] = Integer.parseInt(ids[i]);
                    }
                    break;
                }
                case "customVendorKeyTypeInt32Value": {
                    String[] ids = caseS[1].replace("{", "").replace("}", "").split(",");
                    specificSetting.customVendorKeyTypeInt32Value = new int[ids.length];
                    for (int i = 0; i < specificSetting.customVendorKeyTypeInt32Value.length; i++) {
                        specificSetting.customVendorKeyTypeInt32Value[i] = Integer.parseInt(ids[i]);
                    }
                    break;
                }
                case "customVendorKeyTypeInt64Value": {
                    String[] ids = caseS[1].replace("{", "").replace("}", "").split(",");
                    specificSetting.customVendorKeyTypeInt64Value = new long[ids.length];
                    for (int i = 0; i < specificSetting.customVendorKeyTypeInt64Value.length; i++) {
                        specificSetting.customVendorKeyTypeInt64Value[i] = Long.parseLong(ids[i]);
                    }
                    break;
                }
                case "customVendorKeyTypeFloatValue": {
                    String[] ids = caseS[1].replace("{", "").replace("}", "").split(",");
                    specificSetting.customVendorKeyTypeFloatValue = new float[ids.length];
                    for (int i = 0; i < specificSetting.customVendorKeyTypeFloatValue.length; i++) {
                        specificSetting.customVendorKeyTypeFloatValue[i] = Float.parseFloat(ids[i]);
                    }
                    break;
                }
                case "sensorModes": {
                    Log.d(TAG, "Sensor Modes Loaded: " + caseS[1]);
                    String[] ids = caseS[1].replace("{", "").replace("}", "").split(",");
                    specificSetting.sensorModes = new String[ids.length];
                    for (int i = 0; i < specificSetting.sensorModes.length; i++) {
                        specificSetting.sensorModes[i] = ids[i];
                    }
                    break;
                }
                case "sensorModeKey": {
                    specificSetting.sensorModeKey = caseS[1];
                    break;
                }
                case "ultraHdrThreshold": {
                    specificSetting.ultraHdrThreshold = Float.parseFloat(caseS[1]);
                    break;
                }
                case "ultraHdrMaxBoost": {
                    specificSetting.ultraHdrMaxBoost = Float.parseFloat(caseS[1]);
                    break;
                }
                case "ultraHdrGamma": {
                    specificSetting.ultraHdrGamma = Float.parseFloat(caseS[1]);
                    break;
                }
                case "ltmDarkBoostStrength": {
                    specificSetting.ltmDarkBoostStrength = Float.parseFloat(caseS[1]);
                    break;
                }
                case "ltmBrightSupressStrength": {
                    specificSetting.ltmBrightSupressStrength = Float.parseFloat(caseS[1]);
                    break;
                }
                case "ltmDynamicContrastStrength": {
                    specificSetting.ltmDynamicContrastStrength = Float.parseFloat(caseS[1]);
                    break;
                }
            }
        }

        // load the rest bound to conditions
        isLoaded = false; //mSettingsManager.getBoolean(PreferenceKeys.Key.DEVICES_PREFERENCE_FILE_NAME.mValue, "specific_loaded",false);
        boolean exists = mSettingsManager.getBoolean(PreferenceKeys.Key.DEVICES_PREFERENCE_FILE_NAME.mValue, "specific_exists",true);
        Log.d("Specific", "loaded: "+isLoaded+ " exists: " + exists);
        if(exists) {
            if (!isLoaded) {
                try {
                    ArrayList<String> inputStr = null;

                    String device = Build.BRAND.toLowerCase() + "/" + Build.DEVICE.toLowerCase();
                    File deviceSpecific = new File(sPHOTON_TUNING_DIR, "DeviceSpecific.txt");
                    if(deviceSpecific.exists()) {
                        inputStr = loadLocal(deviceSpecific);
                    }
                    else {
                        if (PhotonCamera.getSettings().allowNetworkSync) {
                            inputStr = loadNetwork(device);
                        }
                    }

                    if (inputStr != null) {
                        for (String str : inputStr) {
                            String[] caseS = str.replace(" ", "").replace("\n", "").split("=");
                            switch (caseS[0]) {
                                case "isDualSessionSupported": {
                                    specificSetting.isDualSessionSupported = Boolean.parseBoolean(caseS[1]);
                                    break;
                                }
                                case "blackLevel": {
                                    String[] bl = caseS[1].split(",");
                                    blackLevel = new float[]{Float.parseFloat(bl[0]), Float.parseFloat(bl[1]), Float.parseFloat(bl[2]), Float.parseFloat(bl[3])};
                                    break;
                                }
                                case "rawColorCorrection": {
                                    specificSetting.isRawColorCorrection = Boolean.parseBoolean(caseS[1]);
                                    break;
                                }
                                case "cameraIDS": {
                                    Log.d("Specific", "Camera IDs Loaded: " + caseS[1]);
                                    String[] ids = caseS[1].replace("{", "").replace("}", "").split(",");
                                    specificSetting.cameraIDS = new String[ids.length];
                                    for (int i = 0; i < specificSetting.cameraIDS.length; i++) {
                                        specificSetting.cameraIDS[i] = ids[i];
                                    }
                                    break;
                                }
                                case "apertureList": {
                                    Log.d("Specific", "apertures loaded: " + caseS[1]);
                                    String[] ids = caseS[1].replace("{", "").replace("}", "").split(",");
                                    specificSetting.apertureList = new float[ids.length];
                                    for (int i = 0; i < specificSetting.apertureList.length; i++) {
                                        specificSetting.apertureList[i] = Float.valueOf(ids[i]);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    mSettingsManager.set(PreferenceKeys.Key.DEVICES_PREFERENCE_FILE_NAME.mValue, "specific_loaded", true);
                } catch (Exception e) {
                    Log.e(TAG,e.toString());
                }
            } else {
                specificSetting.isDualSessionSupported = mSettingsManager.getBoolean(PreferenceKeys.Key.DEVICES_PREFERENCE_FILE_NAME.mValue, "specific_is_dual_session", specificSetting.isDualSessionSupported);
            }
            saveSpecific();
        }
        isLoaded = true;
    }
    private void saveSpecific(){
        mSettingsManager.set(PreferenceKeys.Key.DEVICES_PREFERENCE_FILE_NAME.mValue, "specific_is_dual_session", specificSetting.isDualSessionSupported);
    }

    private void parseAndApply(ArrayList<String> inputStr) {
        for (String str : inputStr) {
            String[] caseS = str.replace(" ", "").replace("\n", "").split("=");
            if (caseS.length < 2) continue;
            switch (caseS[0]) {
                case "isDualSessionSupported":
                    specificSetting.isDualSessionSupported = Boolean.parseBoolean(caseS[1]);
                    break;
                case "blackLevel": {
                    String[] bl = caseS[1].split(",");
                    blackLevel = new float[]{Float.parseFloat(bl[0]), Float.parseFloat(bl[1]),
                            Float.parseFloat(bl[2]), Float.parseFloat(bl[3])};
                    break;
                }
                case "rawColorCorrection":
                    specificSetting.isRawColorCorrection = Boolean.parseBoolean(caseS[1]);
                    break;
                case "sensorModeKey": {
                    specificSetting.sensorModeKey = caseS[1];
                    break;
                }
                case "cameraIDS": {
                    Log.d(TAG, "Camera IDs Loaded: " + caseS[1]);
                    String[] ids = caseS[1].replace("{", "").replace("}", "").split(",");
                    specificSetting.cameraIDS = new String[ids.length];
                    for (int i = 0; i < specificSetting.cameraIDS.length; i++) {
                        specificSetting.cameraIDS[i] = ids[i];
                    }
                    break;
                }
                case "sensorModes": {
                    Log.d(TAG, "Sensor Modes Loaded: " + caseS[1]);
                    String[] ids = caseS[1].replace("{", "").replace("}", "").split(",");
                    specificSetting.sensorModes = new String[ids.length];
                    for (int i = 0; i < specificSetting.sensorModes.length; i++) {
                        specificSetting.sensorModes[i] = ids[i];
                    }
                    break;
                }
                case "customVendorKeyTypeByteName": {
                    String[] ids = caseS[1].replace("{", "").replace("}", "").split(",");
                    specificSetting.customVendorKeyTypeByteName = new String[ids.length];
                    for (int i = 0; i < specificSetting.customVendorKeyTypeByteName.length; i++) {
                        specificSetting.customVendorKeyTypeByteName[i] = ids[i];
                    }
                    break;
                }
                case "customVendorKeyTypeInt32Name": {
                    String[] ids = caseS[1].replace("{", "").replace("}", "").split(",");
                    specificSetting.customVendorKeyTypeInt32Name = new String[ids.length];
                    for (int i = 0; i < specificSetting.customVendorKeyTypeInt32Name.length; i++) {
                        specificSetting.customVendorKeyTypeInt32Name[i] = ids[i];
                    }
                    break;
                }
                case "customVendorKeyTypeFloatName": {
                    String[] ids = caseS[1].replace("{", "").replace("}", "").split(",");
                    specificSetting.customVendorKeyTypeFloatName = new String[ids.length];
                    for (int i = 0; i < specificSetting.customVendorKeyTypeFloatName.length; i++) {
                        specificSetting.customVendorKeyTypeFloatName[i] = ids[i];
                    }
                    break;
                }
                case "customVendorKeyTypeByteValue": {
                    String[] ids = caseS[1].replace("{", "").replace("}", "").split(",");
                    specificSetting.customVendorKeyTypeByteValue = new int[ids.length];
                    for (int i = 0; i < specificSetting.customVendorKeyTypeByteValue.length; i++) {
                        specificSetting.customVendorKeyTypeByteValue[i] = Integer.parseInt(ids[i]);
                    }
                    break;
                }
                case "customVendorKeyTypeInt32Value": {
                    String[] ids = caseS[1].replace("{", "").replace("}", "").split(",");
                    specificSetting.customVendorKeyTypeInt32Value = new int[ids.length];
                    for (int i = 0; i < specificSetting.customVendorKeyTypeInt32Value.length; i++) {
                        specificSetting.customVendorKeyTypeInt32Value[i] = Integer.parseInt(ids[i]);
                    }
                    break;
                }
                case "customVendorKeyTypeFloatValue": {
                    String[] ids = caseS[1].replace("{", "").replace("}", "").split(",");
                    specificSetting.customVendorKeyTypeFloatValue = new float[ids.length];
                    for (int i = 0; i < specificSetting.customVendorKeyTypeFloatValue.length; i++) {
                        specificSetting.customVendorKeyTypeFloatValue[i] = Float.parseFloat(ids[i]);
                    }
                    break;
                }
            }
        }
    }

    public void fetchFromNetwork(Context context) {
        try {
            String device = Build.BRAND.toLowerCase() + "/" + Build.DEVICE.toLowerCase();
            Log.d(TAG, "Fetching from network for device: " + device);
            ArrayList<String> inputStr = loadNetwork(device);
            if (!inputStr.isEmpty()) {
                parseAndApply(inputStr);
                mSettingsManager.set(PreferenceKeys.Key.DEVICES_PREFERENCE_FILE_NAME.mValue, "specific_loaded", true);
                saveSpecific();
                Log.d(TAG, "Network fetch successful");
            }
        } catch (Exception e) {
            Log.e(TAG, "Network fetch failed: " + e.toString());
        }
    }
}
