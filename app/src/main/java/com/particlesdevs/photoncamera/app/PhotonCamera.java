package com.particlesdevs.photoncamera.app;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.ImageFormat;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.location.Location;
import android.media.Image;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.os.HandlerCompat;

import com.particlesdevs.photoncamera.api.CameraMode;
import com.particlesdevs.photoncamera.api.Settings;
import com.particlesdevs.photoncamera.capture.CaptureController;
import com.particlesdevs.photoncamera.control.Gravity;
import com.particlesdevs.photoncamera.control.Gyro;
import com.particlesdevs.photoncamera.control.HorizonAndGear;
import com.particlesdevs.photoncamera.control.Vibration;
import com.particlesdevs.photoncamera.debugclient.Debugger;
import com.particlesdevs.photoncamera.pro.SensorSpecifics;
import com.particlesdevs.photoncamera.pro.Specific;
import com.particlesdevs.photoncamera.pro.SupportedDevice;
import com.particlesdevs.photoncamera.processing.render.Parameters;
import com.particlesdevs.photoncamera.processing.render.PreviewParameters;
import com.particlesdevs.photoncamera.settings.MigrationManager;
import com.particlesdevs.photoncamera.settings.PreferenceKeys;
import com.particlesdevs.photoncamera.settings.SettingsManager;
import com.particlesdevs.photoncamera.ui.SplashActivity;
import com.particlesdevs.photoncamera.util.AssetLoader;
import com.particlesdevs.photoncamera.util.Log;
import com.particlesdevs.photoncamera.util.SimpleStorageHelper;
import com.particlesdevs.photoncamera.util.ObjectLoader;
import com.particlesdevs.photoncamera.util.log.ActivityLifecycleMonitor;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotonCamera extends Application {
    public static final boolean DEBUG = false;
    private static PhotonCamera sPhotonCamera;
    //    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
//    private final ExecutorService executorService = Executors.newWorkStealingPool();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
    private final Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());
    private Settings mSettings;
    private Gravity mGravity;
    private Gyro mGyro;
    private HorizonAndGear mHorizonAndGear;
    private Vibration mVibration;
    private Parameters mParameters;
    private PreviewParameters mPreviewParameters;
    private CaptureController mCaptureController;
    private SupportedDevice mSupportedDevice;
    private SettingsManager mSettingsManager;
    private AssetLoader mAssetLoader;
    private ObjectLoader objectLoader;
    private Debugger mDebugger;
	private AudioManager audioManager;
    public static boolean isSamsung = false;
    public static boolean isGoogle = false;
    public static boolean isZte = false;
    public static boolean isMotorola = false;
    public static boolean isXiaomi = false;
    public static boolean isOppo = false;
    public static boolean isVivo = false;
    public static boolean isOnePlus = false;
    public static boolean isHonor = false;
    public static boolean isHuawei = false;
    public static boolean hasIszKey = false;
    public static boolean hasSaturationKey = false;
    public static boolean hasContrastKey = false;
    public static boolean hasSharpnessKey = false;
    public static boolean hasEisModeKey = false;
    public static boolean hasLtmKey = false;
    public static boolean hasAiModeKey = false;
    public static boolean hasMfnrKey = false;
    public static boolean hasHdr = false;
    public static boolean hasTenBit = false;
    public static boolean hasXiaomiNight = false;
    public static boolean hasXiaomiSuperNight = false;
    public static boolean hasXiaomiHdr = false;
    public static boolean hasXiaomiUltraHdr = false;
    public static boolean hasXiaomiAiAutoSceneDetection = false;
    public static boolean hasXiaomiProVideoLog = false;
    public static boolean hasXiaomiProVideoMovie = false;
    public static boolean hasXiaomiCineLook = false;
    public static boolean hasXiaomiReMosaic = false;
    public static boolean hasXiaomiQuadCfa = false;
    public static boolean hasEisLookAhead = false;
    public static boolean hasEisRealtime = false;
    public static boolean hasEisV3 = false;
    public static boolean hasXiaomiSuperResolution = false;
    public static boolean hasIdealRaw = false;
    public static boolean hasVivoZeissColor = false;
    public static boolean hasVivoProMode = false;
    public static boolean hasVivoDistortionCorrection = false;
    public static boolean hasQucommAdrcOff = false;
    public static boolean hasSensorMode = false;
    public static boolean hasAutoHdr = false;
    public static boolean hasSocHdrMode = false;
    public static boolean hasManualWb = false;
    public static boolean mHeicIsSupported = false;
    public static boolean mHeicUltraHdrIsSupported = false;
    public static boolean mJpegRIsSupported = false;
    public static boolean mYuv10IsSupported = false;
    public static boolean mRaw10IsSupported = false;
    public static boolean mRaw12IsSupported = false;
    public static boolean mRaw14IsSupported = false;
    public static boolean mRawSensorIsSupported = false;
    public static boolean mRawPrivateIsSupported = false;
    public static boolean mHlgIsSupported = false;
    public static boolean mHdrTenIsSupported = false;
    public static boolean mHdrTenPlusIsSupported = false;
    public static boolean isFunctionOneOn = false;
    public static boolean isFunctionTwoOn = false;
    public static boolean isSessionTypeOn = true;
    public static boolean isSuperNightModeOn = false;
    public static boolean isNightModeOn = false;
    public static boolean isRemosaicOn = false;
    public static boolean isQuadCfaOn = false;
    public static boolean isAiAutoSceneDetectionOn = false;
    public static boolean isProVideoLogOn = false;
    public static boolean isProVideoLogMovie = false;
    public static boolean isCineLook = false;
    public static boolean isHdrOn = false;
    public static boolean isUltraHdrOn = false;
    public static boolean isSuperResOn = false;
    public static boolean isIdealRawOn = false;
    public static boolean isEisLookAheadOn = false;
    public static boolean isEisRealtimeOn = false;
    public static boolean isEisV3On = false;
    public static boolean isVivoZeissColorOn = false;
    public static boolean isVivoDistortionCorrectionOn = false;
    public static boolean isVivoProModeOn = false;
    public static boolean isQucommAdrcOff = false;
    public static boolean isSensorModeOn = false;
    public static SensorManager mSensorManager = null;
    public static Location gpsLocation = null;
    public static final int userFormatAvifSw = 999999999;
    public static final int userFormatHeifSw = 999999991;
    public static final int userFormatJpegLutSw = 999999992;
    public static final int userFormatYuvRaw = 888888888;
    public static final int userFormatPngSw = 999999993;
    public static final int userFormatWebpLossySw = 777777777;
    public static final int userFormatWebpLosslessSw = 666666666;
    public static String rawVideoPath = "";
    public static Map<String, String> vendorKeysMapType = null;
    public static Map<String, String> vendorKeysMapClass = null;
    public static long timeStart = 0L;
    public static final ArrayDeque<String> captureTimesRingBuffer = new ArrayDeque<>();

    @Nullable
    public static PhotonCamera getInstance(Context context) {
        if (context instanceof Activity) {
            Application application = ((Activity) context).getApplication();
            if (application instanceof PhotonCamera) {
                return (PhotonCamera) application;
            }
        }
        return null;
    }

    public static AudioManager getAudioManager() {
        return sPhotonCamera.audioManager;
    }

    public static Handler getMainHandler() {
        return sPhotonCamera.mainThreadHandler;
    }

    public static Settings getSettings() {
        return sPhotonCamera.mSettings;
    }

    public static Gravity getGravity() {
        return sPhotonCamera.mGravity;
    }

    public static Gyro getGyro() {
        return sPhotonCamera.mGyro;
    }

    public static HorizonAndGear getHorizonAndGear() {
        return sPhotonCamera.mHorizonAndGear;
    }

    public static Vibration getVibration() {
        return sPhotonCamera.mVibration;
    }

    public static Parameters getParameters() {
        return sPhotonCamera.mParameters;
    }

    public static PreviewParameters getPreviewParameters() {
        return sPhotonCamera.mPreviewParameters;
    }

    public static Debugger getDebugger(){
        return sPhotonCamera.mDebugger;
    }

    public static Specific getSpecific(){
        return sPhotonCamera.mSupportedDevice.specific;
    }
    public static SensorSpecifics getSpecificSensor(){
        return sPhotonCamera.mSupportedDevice.sensorSpecifics;
    }


    public static CaptureController getCaptureController() {
        return sPhotonCamera.mCaptureController;
    }

    public static void setCaptureController(CaptureController captureController) {
        sPhotonCamera.mCaptureController = captureController;
    }

    public static AssetLoader getAssetLoader() {
        return sPhotonCamera.mAssetLoader;
    }

    public static void restartWithDelay(Context context, long delayMs) {
        getMainHandler().postDelayed(() -> restartApp(context), delayMs);
    }

    public static void restartApp(Context context) {
        Intent intent = new Intent(context, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(intent);
        System.exit(0);
    }

    public static void showToast(String msg) {
        getMainHandler().post(() -> Toast.makeText(sPhotonCamera, msg, Toast.LENGTH_LONG).show());
    }

    public static void showToast(@StringRes int stringRes) {
        getMainHandler().post(() -> Toast.makeText(sPhotonCamera, stringRes, Toast.LENGTH_LONG).show());
    }

    public static void showToastFast(@StringRes int stringRes) {
        getMainHandler().post(() -> Toast.makeText(sPhotonCamera, stringRes, Toast.LENGTH_SHORT).show());
    }

    public static Resources getResourcesStatic() {
        return sPhotonCamera.getResources();
    }

    public static String getStringStatic(@StringRes int stringRes) {
        return sPhotonCamera.getResources().getString(stringRes);
    }

    public static Drawable getDrawableStatic(int resID) {
        return ContextCompat.getDrawable(sPhotonCamera, resID);
    }

    public static PackageInfo getPackageInfo() throws PackageManager.NameNotFoundException {
        return sPhotonCamera.getPackageManager().getPackageInfo(sPhotonCamera.getPackageName(), 0);
    }

    public static String getVersion() {
        String version = "";
        try {
            PackageInfo pInfo = PhotonCamera.getPackageInfo();
            version = pInfo.versionName + '(' + pInfo.versionCode + ')';
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return version;
    }
    public static String getLibsDirectory(){
        return sPhotonCamera.getApplicationInfo().nativeLibraryDir;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public SupportedDevice getSupportedDevice() {
        return mSupportedDevice;
    }

    public SettingsManager getSettingsManager() {
        return mSettingsManager;
    }

    public static SettingsManager getSettingsManagerStatic() {
        return sPhotonCamera != null ? sPhotonCamera.mSettingsManager : null;
    }

    @Override
    public void onCreate() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleMonitor());
        sPhotonCamera = this;
        ContextProvider.setContext(this);
        Log.d("PhotonCamera", "Initializing PhotonCamera Modules");
        initModules();
        super.onCreate();
    }
    private void initModules() {
		SimpleStorageHelper.init(this);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mGravity = new Gravity(mSensorManager);
        mGyro = new Gyro(mSensorManager);
        mVibration = new Vibration(this);
        mHorizonAndGear = new HorizonAndGear(mSensorManager);

        mSettingsManager = new SettingsManager(this);
        mSupportedDevice = new SupportedDevice(mSettingsManager, this);

        MigrationManager.migrate(mSettingsManager);
        PreferenceKeys.initialise(mSettingsManager);

        mSupportedDevice.loadCheck();

        mSettings = new Settings();

        mParameters = new Parameters();
        mPreviewParameters = new PreviewParameters();
        mAssetLoader = new AssetLoader(this);
        mDebugger = new Debugger();
        
        // Initialize gallery icon visibility based on preference
        applyGalleryIconVisibility();
        //test();
    }
    
    /**
     * Applies the gallery icon visibility setting based on the user preference.
     * This should be called on app startup to ensure the launcher icon state matches the saved preference.
     */
    private void applyGalleryIconVisibility() {
        try {
            // Get the hide gallery icon preference
            boolean hideGalleryIcon = mSettingsManager.getBoolean(
                    SettingsManager.SCOPE_GLOBAL,
                    PreferenceKeys.Key.KEY_HIDE_GALLERY_ICON
            );
            
            Log.d("PhotonCamera", "Applying gallery icon visibility: hideGalleryIcon=" + hideGalleryIcon);
            
            // Get the ComponentName for the activity-alias using explicit package name
            String packageName = getPackageName();
            ComponentName galleryLauncher = new ComponentName(
                    packageName,
                    packageName + ".gallery.ui.GalleryActivityLauncher"
            );
            
            // Set the component enabled state based on the preference
            int newState = hideGalleryIcon ? 
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED : 
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
            
            Log.d("PhotonCamera", "Setting component " + galleryLauncher + " to state: " + newState);
            
            getPackageManager().setComponentEnabledSetting(
                    galleryLauncher,
                    newState,
                    PackageManager.DONT_KILL_APP
            );
            
            Log.d("PhotonCamera", "Gallery icon visibility applied successfully");
        } catch (Exception e) {
            Log.e("PhotonCamera", "Error applying gallery icon visibility: " + e.getMessage());
            e.printStackTrace();
        }
    }
    //  a MemoryInfo object for the device's current memory status.
    /*public ActivityManager.MemoryInfo AvailableMemory() {
        ActivityManager activityManager = (ActivityManager) mCameraActivity.SystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo;
    }*/

    public static boolean isSingleShotJpegOrHeic() {
        if (isSensorModeOn && (getSettings().sensorModeCfaPattern == -2)) {
            return false;
        }

        if ((PhotonCamera.getSettings().frameCount == 1) &&
            ((PhotonCamera.getSettings().previewFormat == ImageFormat.HEIC) ||
             (PhotonCamera.getSettings().previewFormat == ImageFormat.JPEG) ||
             (PhotonCamera.getSettings().previewFormat == ImageFormat.JPEG_R) ||
             (PhotonCamera.getSettings().previewFormat == ImageFormat.HEIC_ULTRAHDR) ||
             (PhotonCamera.getSettings().previewFormat == ImageFormat.YCBCR_P010) ||
             (PhotonCamera.getSettings().previewFormat == ImageFormat.YUV_420_888) ||
             (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatAvifSw) ||
             (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatHeifSw) ||
             (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatJpegLutSw) ||
             (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatPngSw) ||
             (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatWebpLossySw) ||
             (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatWebpLosslessSw) ||
             (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatYuvRaw)) &&
            (PhotonCamera.getSettings().rawSaver == 0) &&
            !PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO) &&
            !PhotonCamera.getSettings().selectedMode.equals(CameraMode.UNLIMITED) &&
            !PhotonCamera.getSettings().selectedMode.equals(CameraMode.RAWVIDEO)) {
            return true;
        }
        return false;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        executorService.shutdownNow();
        mCaptureController = null;
        sPhotonCamera = null;
    }
}
