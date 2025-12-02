package com.particlesdevs.photoncamera.api;

import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.settings.PreferenceKeys;

import static android.hardware.camera2.CaptureRequest.NOISE_REDUCTION_MODE_OFF;

public class Settings {
    private final String TAG = "Settings";
    //Preferences

    public int frameCount;
    public int lumenCount;
    public int chromaCount;
    public boolean enhancedProcess;
    public boolean watermark;
    public boolean energySaving;
    public boolean aspect169;
    public boolean useThumbnail;
    public boolean DebugData;
    public boolean roundEdge;
    public boolean align;
    public boolean hdrx;
    public boolean hdrxNR;
    public double exposureCompensation;
    public double saturation;
    public double sharpness;
    public double contrastMpy = 1.0;
    public double noiseRstr;
    public double mergeStrength;
    public double compressor;
    public double gain;
    public double shadows;
    public int rawSaver;
    public boolean QuadBayer;
    public int cfaPattern;
    public int theme;
    public boolean remosaic;//TODO
    public boolean eisPhoto;
    public boolean fpsPreview;
    public int alignAlgorithm;
    public int colorMethod;
    public int focusPeak;
    public int previewFormat;
    public String mCameraID;
    public float[] toneMap;
    public float[] gamma;
    public String mode = "Video"; //

    //Camera direct related
    public int noiseReduction = NOISE_REDUCTION_MODE_OFF;
    public CameraMode selectedMode;

    // QualityDoesMatter
    public boolean useBasicOsd;
    public boolean useOis;
    public boolean useDngCompression;
    public int videoBitrate;
    public float apertureToUse;
    public String videoCodec;
    public int videoFramrate;
    public int videoHeight;
    public boolean videoEisInPreview;
    public boolean videoHDR;
    public boolean video10bit;
    public boolean videoNewRec;
    public boolean useExtendIso;
    public boolean useExtendExposure;
    public boolean zoom2X;
    public int noiseProcessing;
    public int edgeProcessing;
    public int audioProcessing;
    public int audioCodec;
    public String audioCodecStr;
    public String audioProcessingStr;
    public int audioSps;
    public int audioBitrate;
    public int audioChannels;
    public int singleFrameQuality;
    public int socQualcommSharpness;
    public int socQualcommSaturation;
    public int socQualcommEisMode;
    public int socQualcommAiMode;
    public boolean socQualcommUseIsz = false;
    public boolean socQualcommUseMfnr = false;
    public boolean useZsl = false;
    public boolean useSceneAndEffectMode = false;
    public boolean useNewSettingsGloabal = false;

    public void loadCache() {
        noiseReduction = PreferenceKeys.isSystemNrOn();
        frameCount = PreferenceKeys.getFrameCountValue();
        align = PreferenceKeys.isDisableAligningOn();
        lumenCount = PreferenceKeys.getLumaNrValue();
        chromaCount = PreferenceKeys.getChromaNrValue();
        enhancedProcess = PreferenceKeys.isEnhancedProcessionOn();
        watermark = PreferenceKeys.isShowWatermarkOn();
        energySaving = PreferenceKeys.getBool(PreferenceKeys.Key.KEY_ENERGY_SAVING);
        aspect169 = PreferenceKeys.getBool(PreferenceKeys.Key.KEY_WIDE169);
        useThumbnail = PreferenceKeys.getBool(PreferenceKeys.Key.KEY_THUMBNAIL);
        DebugData = PreferenceKeys.isAfDataOn();
        roundEdge = PreferenceKeys.isRoundEdgeOn();
        sharpness = PreferenceKeys.getSharpnessValue();
        contrastMpy = PreferenceKeys.getContrastValue();//TODO recheck
        saturation = PreferenceKeys.getSaturationValue();
        exposureCompensation = PreferenceKeys.getFloat(PreferenceKeys.Key.KEY_EXPOCOMPENSATE_SEEKBAR);
        compressor = PreferenceKeys.getCompressorValue();
        noiseRstr = PreferenceKeys.getFloat(PreferenceKeys.Key.KEY_NOISESTR_SEEKBAR);
        mergeStrength = PreferenceKeys.getFloat(PreferenceKeys.Key.KEY_MERGE_SEEKBAR);
        gain = PreferenceKeys.getGainValue();
        shadows = PreferenceKeys.getFloat(PreferenceKeys.Key.KEY_SHADOWS_SEEKBAR);
        hdrx = PreferenceKeys.isHdrxNrOn();
        cfaPattern = PreferenceKeys.getCFAValue();
        rawSaver = PreferenceKeys.isSaveRaw();
        remosaic = PreferenceKeys.isRemosaicOn();
        eisPhoto = PreferenceKeys.isEisPhotoOn();
        QuadBayer = PreferenceKeys.isQuadBayerOn();
        fpsPreview = PreferenceKeys.isFpsPreviewOn();
        hdrxNR = PreferenceKeys.isHdrxNrOn();
        alignAlgorithm = PreferenceKeys.getAlignMethodValue();
        colorMethod = PreferenceKeys.getColorMethodValue();
        focusPeak = PreferenceKeys.getFocusPeakValue();
        previewFormat = PreferenceKeys.getPreviewFormatValue();
        selectedMode = CameraMode.valueOf(PreferenceKeys.getCameraModeOrdinal());
        toneMap = parseToneMapArray();
        gamma = parseGammaArray();
        mCameraID = PreferenceKeys.getCameraID();
        theme = PreferenceKeys.getThemeValue();
        // QualityDoesMatter - Genaral
        useBasicOsd = PreferenceKeys.useBasicOsdOn();
        useOis = PreferenceKeys.useOisOn();
        useDngCompression = PreferenceKeys.useDngCompression();
        singleFrameQuality = PreferenceKeys.getSingleFrameQualityValue();
        apertureToUse = PreferenceKeys.getAperture();
        useExtendIso = PreferenceKeys.useExtendIsoOn();
        useExtendExposure = PreferenceKeys.useExtendExposureOn();
        noiseProcessing = PreferenceKeys.getNoiseProcessing();
        edgeProcessing = PreferenceKeys.getEdgeProcessing();
        zoom2X = PreferenceKeys.isZoomOn();
        // QualityDoesMatter - Video
        videoBitrate = PreferenceKeys.getVideoBitrate();
        videoCodec = PreferenceKeys.getVideoCodec();
        videoFramrate = PreferenceKeys.getVideoFramerate();
        videoHeight = PreferenceKeys.getVideoHeight();
        videoEisInPreview = PreferenceKeys.isEisInPreviewVideoOn();
        videoHDR = PreferenceKeys.isHdrVideoOn();
        video10bit = PreferenceKeys.is10bitVideoOn();
        videoNewRec = PreferenceKeys.isNeRecVideoOn();
        // QualityDoesMatter - Audio
        audioProcessing = PreferenceKeys.getAudioProcessing();
        audioCodec = PreferenceKeys.getAudioCodec();
        audioProcessingStr = PreferenceKeys.getAudioProcessingStr();
        audioCodecStr = PreferenceKeys.getAudioCodecStr();
        audioSps = PreferenceKeys.getAudioSps();
        audioBitrate = PreferenceKeys.getAudioBitrate();
        audioChannels = PreferenceKeys.getAudioChannels();
        // QualityDoesMatter - SoC - Qualcomm/Snapdragon
        socQualcommSharpness = PreferenceKeys.getSocQualcommSharpness();
        socQualcommSaturation = PreferenceKeys.getSocQualcommSaturation();
        socQualcommEisMode = PreferenceKeys.getSocQualcommEisMode();
        socQualcommAiMode = PreferenceKeys.getSocQualcommAiMode();
        socQualcommUseIsz = PreferenceKeys.isSocQualcommIszOn();
        socQualcommUseMfnr = PreferenceKeys.isSocQualcommMfnrOn();
        // QualityDoesMatter - Single Shot & Video Related
        useZsl = PreferenceKeys.isZslOn();
        useSceneAndEffectMode = PreferenceKeys.isSceneAndEffectModeOn();
        useNewSettingsGloabal = PreferenceKeys.isSceneAndEffectModeOn();
    }

    public void saveID() {
        PreferenceKeys.setCameraID(mCameraID);
    }

    float[] parseToneMapArray() {
        String savedArrayAsString = PreferenceKeys.getToneMap();
        if (savedArrayAsString == null)
            return new float[0];
        String[] array = savedArrayAsString.replace("[", "").replace("]", "").split(",");
        float[] finalArray = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            finalArray[i] = Float.parseFloat(array[i].trim());
        }
        return finalArray;
    }

    float[] parseGammaArray() {
        String savedArrayAsString = PreferenceKeys.getPref(PreferenceKeys.Key.GAMMA);
        if (savedArrayAsString == null)
            return new float[0];
        String[] array = savedArrayAsString.replace("[", "").replace("]", "").split(",");
        float[] finalArray = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            finalArray[i] = Float.parseFloat(array[i].trim());
        }
        return finalArray;
    }

}
