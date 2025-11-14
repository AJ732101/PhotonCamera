/*
 *
 *  PhotonCamera
 *  CameraFragment.java
 *  Copyright (C) 2020 - 2021  Eszdman
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.particlesdevs.photoncamera.ui.camera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.ImageFormat;
import android.graphics.RectF;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import com.particlesdevs.photoncamera.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.camera2.CaptureRequest;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.api.CameraEventsListener;
import com.particlesdevs.photoncamera.api.CameraManager2;
import com.particlesdevs.photoncamera.api.CameraMode;
import com.particlesdevs.photoncamera.api.CameraReflectionApi;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.app.base.BaseActivity;
import com.particlesdevs.photoncamera.capture.CaptureController;
import com.particlesdevs.photoncamera.capture.CaptureEventsListener;
import com.particlesdevs.photoncamera.circularbarlib.api.ManualInstanceProvider;
import com.particlesdevs.photoncamera.circularbarlib.api.ManualModeConsole;
import com.particlesdevs.photoncamera.control.Swipe;
import com.particlesdevs.photoncamera.control.TouchFocus;
import com.particlesdevs.photoncamera.databinding.CameraFragmentBinding;
import com.particlesdevs.photoncamera.gallery.ui.GalleryActivity;
import com.particlesdevs.photoncamera.pro.SupportedDevice;
import com.particlesdevs.photoncamera.processing.ProcessingEventsListener;
import com.particlesdevs.photoncamera.processing.parameters.IsoExpoSelector;
import com.particlesdevs.photoncamera.settings.PreferenceKeys;
import com.particlesdevs.photoncamera.settings.SettingsManager;
import com.particlesdevs.photoncamera.ui.camera.data.CameraLensData;
import com.particlesdevs.photoncamera.ui.camera.viewmodel.*;
import com.particlesdevs.photoncamera.ui.camera.views.viewfinder.GLPreview;
import com.particlesdevs.photoncamera.ui.camera.views.viewfinder.SurfaceViewOverViewfinder;
import com.particlesdevs.photoncamera.ui.settings.SettingsActivity;
import com.particlesdevs.photoncamera.util.log.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import android.os.Handler;
import android.os.Looper;
import java.util.Locale;

import android.os.Process;
import java.io.FileReader;


public class CameraFragment extends Fragment implements BaseActivity.BackPressedListener {
    public static final int REQUEST_CAMERA_PERMISSION = 1;
    public static final String FRAGMENT_DIALOG = "dialog";
    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = CameraFragment.class.getSimpleName();
    private static final String ACTIVE_BACKCAM_ID = "ACTIVE_BACKCAM_ID"; //key for savedInstanceState
    private static final String ACTIVE_FRONTCAM_ID = "ACTIVE_FRONTCAM_ID"; //key for savedInstanceState
    private static final String NOTIFICATION_CHANNEL_ID = "NOTIFICATION_CHANNEL_ID";
    /**
     * sActiveBackCamId is either
     * = 0 or camera_id stored in SharedPreferences in case of fresh application Start; or
     * = camera id set from {@link CameraFragment#onViewStateRestored(Bundle)} if Activity re-created due to configuration change.
     * it will NEVER be = 1 *assuming* that 1 is the id of Front Camera on most devices
     */
    public static String sActiveBackCamId = "0";
    public static String sActiveFrontCamId = "1";
    public static CameraMode mSelectedMode;
    private final Field[] metadataFields = CameraReflectionApi.getAllMetadataFields();
    private final int NOTIFICATION_ID = 1;
    /*
    private final ExecutorService processExecutorService = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ProcessingThread");
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });*/
    private final ExecutorService processExecutorService = Executors.newFixedThreadPool(2);
    public SurfaceViewOverViewfinder surfaceView;
    public Map<String, CameraLensData> mCameraLensDataMap;
    public Activity activity;
    private TimerFrameCountViewModel timerFrameCountViewModel;
    private CameraUIView mCameraUIView;
    private CameraUIController mCameraUIEventsListener;
    public CaptureController captureController;
    private CameraFragmentViewModel cameraFragmentViewModel;
    public AuxButtonsViewModel auxButtonsViewModel;
    public CameraFragmentBinding cameraFragmentBinding;
    private TouchFocus mTouchFocus;
    public Swipe mSwipe;
    private MediaPlayer burstPlayer;
    private MediaPlayer endPlayer;
    public GLPreview textureView;
    private NotificationManagerCompat notificationManager;
    private SettingsManager settingsManager;
    private SupportedDevice supportedDevice;
    private SettingsBarEntryProvider settingsBarEntryProvider;
    private ManualModeConsole manualModeConsole;
    public float displayAspectRatio;

    public CameraFragment() {
        Log.v(TAG, "fragment created");
    }

    public static CameraFragment newInstance() {
        return new CameraFragment();
    }

    public TouchFocus getTouchFocus() {
        return mTouchFocus;
    }

    public CaptureController getCaptureController() {
        return captureController;
    }

    public ManualModeConsole getManualModeConsole() {
        return manualModeConsole;
    }

    private final Handler timerHandlerVideoRec = new Handler(Looper.getMainLooper());
    private final Handler timerHandlerAlways = new Handler(Looper.getMainLooper());
    private long recordingStartTime;
    File mVidFile = null;
    boolean mIsTenBit = false;
    boolean mIsHdr = false;
    private TextView recordingTimerTextView;
    private TextView recordingSizeTextView;
    private TextView tenBitIndicatorTextView;
    private TextView hdrIndicatorTextView;
    private TextView currentIsoTextView;
    private TextView currentShutterTextView;


    private final Runnable timerRunnableVideoRec = new Runnable() {
        AppCpuReader mCpuReader = new AppCpuReader();
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - recordingStartTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds %= 60;

            //recordingTimerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d - CPU: %02d%%", minutes, seconds, (int)mCpuReader.getCpuUsage()));
            recordingTimerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            recordingTimerTextView.invalidate();
            if ((mVidFile != null) && mVidFile.exists()) {
                recordingSizeTextView.setText(String.format(Locale.getDefault(), "%02dMB", mVidFile.length() / (1024 * 1024)));
            }
            recordingSizeTextView.invalidate();
            timerHandlerVideoRec.postDelayed(this, 1000);
        }
    };

    private final Runnable timerRunnableAlways = new Runnable() {
        @Override
        public void run() {
            if (captureController != null) {
                currentIsoTextView.setText("ISO" + captureController.cameraEventsListener.mCurrentIso);
                currentIsoTextView.invalidate();
                currentShutterTextView.setText(captureController.cameraEventsListener.mCurrentShutterSpeed);
                currentShutterTextView.invalidate();
            }
            timerHandlerAlways.postDelayed(this, 1000);
        }
    };

    public CameraFragmentViewModel getCameraFragmentViewModel() {
        return cameraFragmentViewModel;
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        activity = getActivity();
        assert activity != null;
        notificationManager = NotificationManagerCompat.from(activity);
        settingsManager = Objects.requireNonNull(PhotonCamera.getInstance(activity)).getSettingsManager();
        supportedDevice = Objects.requireNonNull(PhotonCamera.getInstance(activity)).getSupportedDevice();
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //create the ui binding
        this.cameraFragmentBinding = DataBindingUtil.inflate(inflater, R.layout.camera_fragment, container, false);
        Log.d(TAG, "onCreateView: ");
        initMembers();
        setModelsToLayout();
        return cameraFragmentBinding.getRoot();
    }
    private void initMembers() {
        //create the viewmodel which updates the model
        cameraFragmentViewModel = new ViewModelProvider(this).get(CameraFragmentViewModel.class);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        logDisplayProperties(dm);
        displayAspectRatio = (float) Math.max(dm.heightPixels, dm.widthPixels) / Math.min(dm.heightPixels, dm.widthPixels);
        cameraFragmentViewModel.setScreenAspectRatio(displayAspectRatio);

        timerFrameCountViewModel = new ViewModelProvider(this).get(TimerFrameCountViewModel.class);
        manualModeConsole = ManualInstanceProvider.getNewManualModeConsole();
        settingsBarEntryProvider = new ViewModelProvider(this).get(SettingsBarEntryProvider.class);
        auxButtonsViewModel = new ViewModelProvider(this).get(AuxButtonsViewModel.class);
        surfaceView = cameraFragmentBinding.layoutViewfinder.surfaceView;
        textureView = cameraFragmentBinding.layoutViewfinder.texture;
    }

    private void setModelsToLayout() {
        //bind the model to the ui, it applies changes when the model values get changed
        cameraFragmentBinding.setUimodel(cameraFragmentViewModel.getCameraFragmentModel());
        cameraFragmentBinding.layoutTopbar.setUimodel(cameraFragmentViewModel.getCameraFragmentModel());
        cameraFragmentBinding.layoutBottombar.bottomButtons.setUimodel(cameraFragmentViewModel.getCameraFragmentModel());
        // associating timer model with layouts
        cameraFragmentBinding.layoutBottombar.bottomButtons.setTimermodel(timerFrameCountViewModel.getTimerFrameCountModel());
        cameraFragmentBinding.layoutViewfinder.setTimermodel(timerFrameCountViewModel.getTimerFrameCountModel());
        // associating AuxButtonsModel with layout
        cameraFragmentBinding.setAuxmodel(auxButtonsViewModel.getAuxButtonsModel());
    }
    @Override
    public void onViewCreated(@NonNull final View view, Bundle savedInstanceState) {
        this.mCameraUIView = new CameraUIViewImpl(this);
        this.mCameraUIEventsListener = new CameraUIController(this);
        this.mCameraUIView.setCameraUIEventsListener(mCameraUIEventsListener);
        this.captureController = new CaptureController(activity, processExecutorService, new CameraEventsListenerImpl());
        this.manualModeConsole.addParamObserver(captureController.getParamController());
        PhotonCamera.setCaptureController(captureController);
        captureController.isDualSession = supportedDevice.specific.specificSetting.isDualSessionSupported;
        this.mSwipe = new Swipe(this);
        recordingTimerTextView = cameraFragmentBinding.recordingTimerText;
        recordingSizeTextView = cameraFragmentBinding.recordingSizeText;
        tenBitIndicatorTextView = cameraFragmentBinding.tenBitIndicatorText;
        hdrIndicatorTextView = cameraFragmentBinding.hdrIndicatorText;
        currentIsoTextView = cameraFragmentBinding.currentIsoText;
        currentShutterTextView = cameraFragmentBinding.currentShutterText;
        initSettingsBar();
    }

    private void initSettingsBar() {
        settingsBarEntryProvider.createEntries();
        settingsBarEntryProvider.addObserver(mCameraUIEventsListener);
        try {
            settingsBarEntryProvider.addEntries(cameraFragmentBinding.settingsBar);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateSettingsBar(){
        settingsBarEntryProvider.updateAllEntries();
        settingsBarEntryProvider.addEntries(cameraFragmentBinding.settingsBar);
        this.mCameraUIView.refresh(CaptureController.isProcessing);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ACTIVE_BACKCAM_ID, sActiveBackCamId);
        outState.putString(ACTIVE_FRONTCAM_ID, sActiveFrontCamId);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (PhotonCamera.DEBUG)
            Log.d("FragmentMonitor", "[" + getClass().getSimpleName() + "] : onViewStateRestored(), savedInstanceState = [" + savedInstanceState + "]");
        if (savedInstanceState != null) {
            sActiveBackCamId = savedInstanceState.getString(ACTIVE_BACKCAM_ID);
            sActiveFrontCamId = savedInstanceState.getString(ACTIVE_FRONTCAM_ID);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                showErrorDialog(R.string.request_permission);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        updateSettingsBar();
        mSwipe.init();
        this.mCameraUIView.refresh(CaptureController.isProcessing);
        AsyncTask.execute(() -> {
            PhotonCamera.getGyro().register();
            PhotonCamera.getGravity().register();
            burstPlayer = MediaPlayer.create(activity, R.raw.sound_burst2);
            endPlayer = MediaPlayer.create(activity,R.raw.sound_end);
            cameraFragmentViewModel.updateGalleryThumb(null);
        });
        cameraFragmentViewModel.onResume();
        auxButtonsViewModel.setAuxButtonListener(mCameraUIEventsListener);
        captureController.startBackgroundThread();
        captureController.resumeCamera();
        initTouchFocus();
        manualModeConsole.onResume();
    }

    private void initTouchFocus() {
        if (cameraFragmentBinding != null && captureController != null) {
            View focusCircle = cameraFragmentBinding.layoutViewfinder.touchFocus;
            textureView.post(() -> {
                mTouchFocus = new TouchFocus(captureController,focusCircle,textureView);
                captureController.mTouchFocus = mTouchFocus;
            });
        }
    }

    @Override
    public void onPause() {
        timerHandlerAlways.removeCallbacks(timerRunnableAlways);
        PhotonCamera.getGravity().unregister();
        PhotonCamera.getGyro().unregister();
        PhotonCamera.getSettings().saveID();
        captureController.closeCamera();
//        stopBackgroundThread();
        cameraFragmentViewModel.onPause();
        mCameraUIEventsListener.onPause();
        auxButtonsViewModel.setAuxButtonListener(null);
        burstPlayer.release();
        endPlayer.release();
        mSwipe.SwipeDown();
        manualModeConsole.onPause();
        super.onPause();
    }

    @Override
    public boolean onBackPressed() {
        boolean handleBack = false;
        if (cameraFragmentViewModel.isSettingsBarVisible()) {
            cameraFragmentViewModel.setSettingsBarVisible(false);
            handleBack = true;
        }
        if (manualModeConsole.isPanelVisible()) {
            mSwipe.SwipeDown();
            handleBack = true;
        }
        return handleBack;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            // 1. Signal background threads to stop their work.
            if (captureController != null) {
                captureController.stopBackgroundThread();
            }

            // 2. Shut down the executor and wait for tasks to complete.
            if (processExecutorService != null) {
                processExecutorService.shutdown(); // Disable new tasks from being submitted
                // Wait a reasonable time (e.g., 2 seconds) for existing tasks to terminate
                if (!processExecutorService.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    Log.e(TAG, "Background tasks did not terminate in 2 seconds, forcing shutdown.");
                    processExecutorService.shutdownNow(); // Cancel currently executing tasks
                }
            }
        } catch (InterruptedException ie) {
            Log.e(TAG, "onDestroy was interrupted while waiting for tasks to finish.", ie);
            if(processExecutorService != null) {
                processExecutorService.shutdownNow();
            }
            // Preserve the interrupt status
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Log.e(TAG, "Error during resource cleanup in onDestroy.", e);
        }

        getParentFragmentManager().beginTransaction().remove(CameraFragment.this).commitAllowingStateLoss();
        if(settingsBarEntryProvider != null) settingsBarEntryProvider.removeObserver(mCameraUIEventsListener);
        if(mCameraUIView != null) mCameraUIView.destroy();
        if(manualModeConsole != null) manualModeConsole.onDestroy();

        cameraFragmentBinding = null;
        mCameraUIView = null;
        mCameraUIEventsListener = null;
        PhotonCamera.setCaptureController(null);
        captureController = null;

        Log.d(TAG, "onDestroy() finished");
    }

    @SuppressLint("DefaultLocale")
    private void updateScreenLog(CaptureResult result) {
        surfaceView.post(() -> {
            PhotonCamera.getCaptureController().videoRotation = getCameraFragmentViewModel().getCameraFragmentModel().getOrientation();
            mTouchFocus.setState(result.get(CaptureResult.CONTROL_AF_STATE));
            if (result.getFrameNumber() % 5 == 0) {
                captureController.cameraEventsListener.mCurrentIso = String.valueOf(result.get(CaptureResult.SENSOR_SENSITIVITY));
                Long exposureTimeNs = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);

                if (exposureTimeNs != null && exposureTimeNs > 0) {
                    if (exposureTimeNs >= 1_000_000_000L) {
                        // Belichtungszeit >= 1 Sekunde
                        double seconds = exposureTimeNs / 1_000_000_000.0;
                        captureController.cameraEventsListener.mCurrentShutterSpeed = String.format(Locale.getDefault(), "%.1fs", seconds);
                    } else {
                        // Belichtungszeit < 1 Sekunde
                        long divisor = (long) (1_000_000_000.0 / exposureTimeNs);
                        captureController.cameraEventsListener.mCurrentShutterSpeed = "1/" + divisor + "s";
                    }
                }
                //captureController.cameraEventsListener.mCurrentShutterSpeed = String.valueOf(result.get(CaptureResult.SENSOR_EXPOSURE_TIME) / 1000000000);
            }
            if (PreferenceKeys.isAfDataOn()) {
                //stringMap.put("ISO", String.valueOf(expoPair.iso));
                String camID = result.getCameraId();
                String physCamId = result.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID);
                LinkedHashMap<String, String> stringMap = new LinkedHashMap<>();
                if (!PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                    if ((PhotonCamera.getSettings().frameCount == 1) &&
                       ((PhotonCamera.getSettings().previewFormat == ImageFormat.JPEG) ||
                        (PhotonCamera.getSettings().previewFormat == ImageFormat.HEIC) ||
                        (PhotonCamera.getSettings().previewFormat == ImageFormat.YCBCR_P010)))
                    {
                        stringMap.put("Mode", "SINGLE SHOT");
                    }
                    else {
                        stringMap.put("Mode", "MULTIFRAME SHOT");
                    }
                }
                if (physCamId != null) {
                    stringMap.put("Camera ID", camID + "-" + physCamId);
                }
                else {
                    stringMap.put("Camera ID", camID);
                }
                if (!PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                    if ((PhotonCamera.getSettings().frameCount == 1) &&
                       ((PhotonCamera.getSettings().previewFormat == ImageFormat.JPEG) ||
                        (PhotonCamera.getSettings().previewFormat == ImageFormat.HEIC) ||
                        (PhotonCamera.getSettings().previewFormat == ImageFormat.YCBCR_P010))) {
                        switch (PhotonCamera.getSpecific().specificSetting.effectMode) {
                            case CaptureRequest.CONTROL_EFFECT_MODE_MONO:
                                stringMap.put("Effectmode:", "Mono");
                                break;
                            case CaptureRequest.CONTROL_EFFECT_MODE_NEGATIVE:
                                stringMap.put("Effectmode:", "Negative");
                                break;
                            case CaptureRequest.CONTROL_EFFECT_MODE_SOLARIZE:
                                stringMap.put("Effectmode:", "Solarize");
                                break;
                            case CaptureRequest.CONTROL_EFFECT_MODE_SEPIA:
                                stringMap.put("Effectmode:", "Sepia");
                                break;
                            case CaptureRequest.CONTROL_EFFECT_MODE_POSTERIZE:
                                stringMap.put("Effectmode:", "Posterize");
                                break;
                            case CaptureRequest.CONTROL_EFFECT_MODE_WHITEBOARD:
                                stringMap.put("Effectmode:", "Whiteboard");
                                break;
                            case CaptureRequest.CONTROL_EFFECT_MODE_BLACKBOARD:
                                stringMap.put("Effectmode:", "Blackboard");
                                break;
                            case CaptureRequest.CONTROL_EFFECT_MODE_AQUA:
                                stringMap.put("Effectmode:", "Aqua");
                                break;
                            default:
                                stringMap.put("Effectmode:", "None");
                                break;
                        }
                    }
                }
                IsoExpoSelector.ExpoPair expoPair = IsoExpoSelector.GenerateExpoPair(-1, captureController);
                stringMap.put("ISO", String.valueOf(result.get(CaptureResult.SENSOR_SENSITIVITY)));
                //stringMap.put("ISO2", String.valueOf(expoPair.iso));
                stringMap.put("Shutter", captureController.cameraEventsListener.mCurrentShutterSpeed);
                //stringMap.put("Shutter2", String.valueOf(result.get(CaptureResult.SENSOR_EXPOSURE_TIME)));
                //stringMap.put("Shutter3", String.valueOf(expoPair.exposure));
                stringMap.put("Aperture", String.valueOf(result.get(CaptureResult.LENS_APERTURE)));
                stringMap.put("Focal length", String.valueOf(result.get(CaptureResult.LENS_FOCAL_LENGTH)) + "mm");
                                float len35mm = 0;
                if (physCamId == null) {
                    var lensData = mCameraLensDataMap.get(camID);
                    len35mm = (float) Math.ceil(lensData.getCamera35mmFocalLength());
                }
                else {
                    var lensData = mCameraLensDataMap.get(camID + "-" + physCamId);
                    if (lensData != null) {
                        len35mm = (float) Math.ceil(lensData.getCamera35mmFocalLength());
                    }
                    else {
                        lensData = mCameraLensDataMap.get(physCamId);
                        if (lensData != null) {
                            len35mm = (float) Math.ceil(lensData.getCamera35mmFocalLength());
                        }
                        else {
                            len35mm = 0;
                        }
                    }
                }
                stringMap.put("35mm Focal length", String.valueOf(len35mm) + "mm");
                stringMap.put("OIS", getResultFieldName("LENS_OPTICAL_STABILIZATION_MODE_", result.get(CaptureResult.LENS_OPTICAL_STABILIZATION_MODE)));
                stringMap.put("Orientation", String.valueOf(getCameraFragmentViewModel().getCameraFragmentModel().getOrientation()));
                stringMap.put("FPS Prev", String.valueOf(captureController.getFpsRangeDef().getLower()));
                stringMap.put("Ext. ISO", String.valueOf(PhotonCamera.getSettings().useExtendIso));
                stringMap.put("Ext. Expo", String.valueOf(PhotonCamera.getSettings().useExtendIso));
                stringMap.put("DNG Compr", String.valueOf(PhotonCamera.getSettings().useDngCompression));
                // QualityDoesMatter
                if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                    stringMap.put("--VIDEO--", "--OPTS--");
                    stringMap.put("CodecV", PhotonCamera.getSettings().videoCodec);
                    if (PhotonCamera.getSettings().videoHeight == 7777) {
                        stringMap.put("Height", "8K+ 4:3");
                    }
                    else if (PhotonCamera.getSettings().videoHeight == 8888) {
                        stringMap.put("Height", "6K+ 4:3");
                    }
                    else if (PhotonCamera.getSettings().videoHeight == 9999) {
                        stringMap.put("Height", "4K+ 4:3");
                    }
                    else {
                        stringMap.put("Height", String.valueOf(PhotonCamera.getSettings().videoHeight));
                    }
                    stringMap.put("FPS", String.valueOf(PhotonCamera.getSettings().videoFramrate));
                    stringMap.put("BitrateV", String.valueOf(PhotonCamera.getSettings().videoBitrate) + "MBit/s");
                    stringMap.put("HDR", String.valueOf(PhotonCamera.getSettings().videoHDR));
                    stringMap.put("10 bit", String.valueOf(PhotonCamera.getSettings().video10bit));
                    stringMap.put("Noise Processing.", String.valueOf(PhotonCamera.getSettings().noiseProcessing));
                    stringMap.put("Edge Processing", String.valueOf(PhotonCamera.getSettings().edgeProcessing));
                    stringMap.put("Prev EIS", String.valueOf(PhotonCamera.getSettings().videoEisInPreview));
                    //stringMap.put("FPS", String.valueOf(captureController.getFpsRangeDef().getLower()));
                    // as long as audio not working with new pipeline
                    if (!PhotonCamera.getSpecific().specificSetting.useNewRecordingPipeline) {
                        stringMap.put("--AUDIO--", "--OPTS--");
                        switch (PhotonCamera.getSettings().audioProcessing) {
                            case 0:
                                stringMap.put("Source", "No Audio");
                                break;
                            case 1:
                                stringMap.put("Source", "MIC");
                                break;
                            case 9:
                                stringMap.put("Source", "Unprocessed");
                                break;
                            case 10:
                                stringMap.put("Source", "Voice Performance");
                                break;
                            case 6:
                                stringMap.put("Source", "Voice Recognition");
                                break;
                            case 5:
                                stringMap.put("Source", "Camcorder");
                                break;
                        }
                        if ((PhotonCamera.getSettings().videoCodec.equals("VP8")) || PhotonCamera.getSettings().videoCodec.equals("VP9")) {
                            stringMap.put("CodecA", "Opus");
                        }
                        else {
                            switch (PhotonCamera.getSettings().audioCodec) {
                                case 0:
                                    stringMap.put("CodecA", "Default");
                                    break;
                                case 1:
                                    stringMap.put("CodecA", "AMR_NB");
                                    break;
                                case 2:
                                    stringMap.put("CodecA", "AMR_WB");
                                    break;
                                case 3:
                                    stringMap.put("CodecA", "AAC");
                                    break;
                                case 4:
                                    stringMap.put("CodecA", "HE_AAC");
                                    break;
                                case 5:
                                    stringMap.put("CodecA", "AAC_ELD");
                                    break;
                            }
                        }
                        stringMap.put("Channels", String.valueOf(PhotonCamera.getSettings().audioChannels));
                        stringMap.put("BitrateA", String.valueOf(PhotonCamera.getSettings().audioBitrate) + "KBit/s");
                        stringMap.put("SPS", String.valueOf(PhotonCamera.getSettings().audioSps / 1024) + "kHz");
                        stringMap.put("----------", "----------");
                    }
                }
                if (!PhotonCamera.getSettings().useBasicOsd)
                {
                    stringMap.put("AF_MODE", getResultFieldName("CONTROL_AF_MODE_", result.get(CaptureResult.CONTROL_AF_MODE)));
                    stringMap.put("AF_TRIGGER", getResultFieldName("CONTROL_AF_TRIGGER_", result.get(CaptureResult.CONTROL_AF_TRIGGER)));
                    stringMap.put("AF_STATE", getResultFieldName("CONTROL_AF_STATE_", result.get(CaptureResult.CONTROL_AF_STATE)));
                    stringMap.put("AE_MODE", getResultFieldName("CONTROL_AE_MODE_", result.get(CaptureResult.CONTROL_AE_MODE)));
                    stringMap.put("FLASH_MODE", getResultFieldName("FLASH_MODE_", result.get(CaptureResult.FLASH_MODE)));
                    stringMap.put("FOCUS_DISTANCE", String.valueOf(result.get(CaptureResult.LENS_FOCUS_DISTANCE)));
                    //stringMap.put("EXPOSURE_TIME_CR", String.format(Locale.ROOT,"%.5f",result.get(CaptureResult.SENSOR_EXPOSURE_TIME).doubleValue()/1E9)+ "s");
                    //stringMap.put("ISO_CR", String.valueOf(result.get(CaptureResult.SENSOR_SENSITIVITY)));
                    stringMap.put("Shakiness", String.valueOf(PhotonCamera.getGyro().getShakiness()));
                    stringMap.put("TripodShakiness", String.valueOf(PhotonCamera.getGyro().tripodShakiness));
                    stringMap.put("Tripod", String.valueOf(PhotonCamera.getGyro().getTripod()));
                    stringMap.put("FrameNumber", String.valueOf(result.getFrameNumber()));
                    float[] temp = new float[3];
                    temp[0] = captureController.mPreviewTemp[0].floatValue();
                    temp[1] = captureController.mPreviewTemp[1].floatValue();
                    temp[2] = captureController.mPreviewTemp[2].floatValue();
                    stringMap.put("White Point", String.format("%.3f %.3f %.3f", temp[0], temp[1], temp[2]));
                    MeteringRectangle[] afRect = result.get(CaptureResult.CONTROL_AF_REGIONS);
                    stringMap.put("AF_RECT", Arrays.deepToString(afRect));
                    if (afRect != null && afRect.length > 0) {
                        RectF rect = getScreenRectFromMeteringRect(afRect[0]);
                        stringMap.put("AF_RECT(px)", rect.toString());
                        surfaceView.setAFRect(rect);
                    } else {
                        surfaceView.setAFRect(null);
                    }
                    MeteringRectangle[] aeRect = result.get(CaptureResult.CONTROL_AE_REGIONS);
                    stringMap.put("AE_RECT", Arrays.deepToString(aeRect));
                    if (aeRect != null && aeRect.length > 0) {
                        RectF rect = getScreenRectFromMeteringRect(aeRect[0]);
                        stringMap.put("AE_RECT(px)", rect.toString());
                        surfaceView.setAERect(rect);
                    } else {
                        surfaceView.setAERect(null);
                    }
                }
                surfaceView.setDebugText(Logger.createTextFrom(stringMap));
                surfaceView.refresh();
            } else {
                if (surfaceView.isCanvasDrawn) {
                    surfaceView.clear();
                }
            }
        });
    }

    private RectF getScreenRectFromMeteringRect(MeteringRectangle meteringRectangle) {
        if (captureController.mImageReaderPreview == null) return new RectF();
        Size size = CaptureController.mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
        if (size == null) {
            size = new Size(captureController.mImageReaderPreview.getWidth(), captureController.mImageReaderPreview.getHeight());
        }
        float left = (((float) meteringRectangle.getY() / size.getHeight()) * (textureView.getWidth()));
        float top = (((float) meteringRectangle.getX() / size.getWidth()) * (textureView.getHeight()));
        float width = (((float) meteringRectangle.getHeight() / size.getHeight()) * (textureView.getWidth()));
        float height = (((float) meteringRectangle.getWidth() / size.getWidth()) * (textureView.getHeight()));
        //left = textureView.getWidth() - left;
        return new RectF(
                //meteringRectangle.getY()-left, //Left
                textureView.getWidth()-left-width,//Right
                top,  //Top
                //meteringRectangle.getY() - (left + width),//Right
                textureView.getWidth()-left,
                top + height //Bottom
        );
    }

    private String getResultFieldName(String prefix, Integer value) {
        if(value == null) return "";
        for (Field f : this.metadataFields)
            if (f.getName().startsWith(prefix)) {
                try {
                    if (f.getInt(f) == value)
                        return f.getName().replace(prefix, "").concat("(" + value + ")");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        return "";
    }

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    public void showToast(final String text) {
        if (activity != null) {
            activity.runOnUiThread(() -> Toast.makeText(activity, text, Toast.LENGTH_SHORT).show());
        }
    }

    public void showSnackBar(final String text) {
        final View v = getView();
        if (v != null) {
            v.post(() -> Snackbar.make(v, text, Snackbar.LENGTH_SHORT).show());
        }
    }

    /**
     * Returns the ConstraintLayout object after adjusting the LayoutParams of Views contained in it.
     * Adjusts the relative position of layout_top-bar and camera_container (= viewfinder + rest of the buttons excluding layout_topbar)
     * depending on the aspect ratio of device.
     * This is done in order to re-organise the camera layout for long displays (having aspect ratio > 16:9)
     *
     * @param aspectRatio     the aspect ratio of device display given by (height in pixels / width in pixels)
     * @param activity_layout here, the layout of activity_main
     * @return Object of {@param activity_layout} after adjustments.
     */
    private ConstraintLayout getAdjustedLayout(float aspectRatio, ConstraintLayout activity_layout) {
        ConstraintLayout camera_container = activity_layout.findViewById(R.id.camera_container);
        ConstraintLayout.LayoutParams camera_containerLP = (ConstraintLayout.LayoutParams) camera_container.getLayoutParams();
        if (aspectRatio > 16f / 9f) {
            DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
            float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
            float dpWidth = displayMetrics.widthPixels / displayMetrics.density;

            float dpmargin = (dpHeight - (dpWidth / 9f * 16f));
            ConstraintLayout.LayoutParams layout_topbarLP = ((ConstraintLayout.LayoutParams) activity_layout.findViewById(R.id.layout_topbar).getLayoutParams());

            layout_topbarLP.topMargin = (int) dpmargin;
            camera_containerLP.bottomMargin = (int) dpmargin;
            camera_containerLP.topToTop = -1;
            camera_containerLP.topToBottom = R.id.layout_topbar;
        }
        return activity_layout;
    }

    /**
     * Logs the device display properties
     *
     * @param dm Object of {@link DisplayMetrics} obtained from Fragment
     */
    private void logDisplayProperties(DisplayMetrics dm) {
        String TAG = "DisplayProps";
        Log.i(TAG, "ScreenResolution = " + Math.max(dm.heightPixels, dm.widthPixels) + "x" + Math.min(dm.heightPixels, dm.widthPixels));
        Log.i(TAG, "AspectRatio = " + ((float) Math.max(dm.heightPixels, dm.widthPixels) / Math.min(dm.heightPixels, dm.widthPixels)));
        Log.i(TAG, "SmallestWidth = " + (int) (Math.min(dm.heightPixels, dm.widthPixels) / (dm.densityDpi / 160f)) + "dp");
    }

    public void initCameraIDLists(CameraManager cameraManager) {
        CameraManager2 manager2 = new CameraManager2(cameraManager, settingsManager);
        this.mCameraLensDataMap = manager2.getCameraLensDataMap();
    }

    public String cycler(String savedCameraID) {
        if (Objects.requireNonNull(mCameraLensDataMap.get(savedCameraID)).getFacing() == CameraCharacteristics.LENS_FACING_BACK) {
            sActiveBackCamId = savedCameraID;
            return sActiveFrontCamId;
        } else {
            sActiveFrontCamId = savedCameraID;
            return sActiveBackCamId;
        }
    }

    public void triggerMediaScanner(Uri imageUri) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//        Bitmap bitmap = BitmapDecoder.from(Uri.fromFile(imageToSave)).scaleBy(0.1f).decode();
        mediaScanIntent.setData(imageUri);
        if (activity != null)
            activity.sendBroadcast(mediaScanIntent);
    }

    public void launchGallery() {
        Intent galleryIntent = new Intent(activity, GalleryActivity.class);
        // Create gallery bundle
        galleryIntent.putExtra("CameraFragment", true);
        
        startActivity(galleryIntent, null);
    }

    public void launchSettings() {
        Intent settingsIntent = new Intent(activity, SettingsActivity.class);
        startActivity(settingsIntent);
    }

    public <T extends View> T findViewById(@IdRes int id) {
        return activity.findViewById(id);
    }

    public void showErrorDialog(String errorMsg) {
        ErrorDialog.newInstance(errorMsg).show(getChildFragmentManager(), FRAGMENT_DIALOG);
    }

    public void showErrorDialog(@StringRes int stringRes) {
        try {
            ErrorDialog.newInstance(getString(stringRes)).show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } catch (Resources.NotFoundException e) {
            showErrorDialog(String.valueOf(stringRes));
        }
    }

    public void invalidateSurfaceView() {
        if (surfaceView != null) {
            surfaceView.invalidate();
        }
    }

    private void showNotification(String processName) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(activity, NOTIFICATION_CHANNEL_ID);
        NotificationChannel channel = new NotificationChannel
                (NOTIFICATION_CHANNEL_ID, "NotificationChannel", NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(channel);
        notificationBuilder
                .setSmallIcon(R.drawable.ic_round_photo_camera_24)
                .setContentTitle(activity.getString(R.string.app_name))
                .setContentText(activity.getString(R.string.processing_processname, processName))
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(0, 0, true);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void stopNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    //*****************************************************************************************************************
    //**************************************ErrorDialog****************************************************************
    //*****************************************************************************************************************

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            assert getArguments() != null;
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                        if (activity != null) {
                            activity.finish();
                        }
                    })
                    .create();
        }
    }

    //*****************************************************************************************************************
    //**************************************CameraEventsListenerImpl***************************************************
    //*****************************************************************************************************************

    private class CameraEventsListenerImpl extends CameraEventsListener {
        /**
         * Implementation of {@link ProcessingEventsListener}
         */

        @Override
        public void onProcessingStarted(String processName) {
            logD("onProcessingStarted: " + processName + " Processing Started");
            mCameraUIView.setProcessingProgressBarIndeterminate(true);
            mCameraUIView.activateShutterButton(true);
            showNotification(processName);
        }

        @Override
        public void onProcessingChanged(Object obj) {
        }

        @Override
        public void onProcessingFinished(Object obj) {
            logD("onProcessingFinished: " + obj);
            mCameraUIView.setProcessingProgressBarIndeterminate(false);
            mCameraUIView.activateShutterButton(true);
            mCameraUIView.lockUIForBurst(false);
            stopNotification();

        }

        @Override
        public void notifyImageSavedStatus(boolean saved, Path savedFilePath) {
            if (saved) {
                Uri imageUri = null;
                if (savedFilePath != null) {
                    triggerMediaScanner(imageUri = Uri.fromFile(savedFilePath.toFile()));
                    logD("ImageSaved: " + savedFilePath);
//                    showSnackBar("ImageSaved: " + savedFilePath.toString());
                }
                cameraFragmentViewModel.updateGalleryThumb(imageUri);
            } else {
                logE("ImageSavingError");
                showSnackBar("ImageSavingError");
            }
        }

        @Override
        public void onProcessingError(Object obj) {
            if (obj instanceof String)
                showToast((String) obj);
            mCameraUIView.lockUIForBurst(false);
            onProcessingFinished("Processing Finished Unexpectedly!!");
        }

        //*****************************************************************************************************************

        /**
         * Implementation of {@link CaptureEventsListener}
         */
        @Override
        public void onFrameCountSet(int frameCount) {
            mCameraUIView.setCaptureProgressMax(frameCount);
        }

        @Override
        public void onCaptureStillPictureStarted(Object o) {
            mCameraUIView.setCaptureProgressBarOpacity(1.0f);
            mCameraUIView.lockUIForBurst(true);
            //textureView.post(() -> textureView.setAlpha(0.8f));
        }

        private long prevPlayTime = 0;
        @Override
        public void onFrameCaptureStarted(Object o) {
            long seekDelay = 50;
            if(prevPlayTime + seekDelay < System.currentTimeMillis()){
                prevPlayTime = System.currentTimeMillis();
                burstPlayer.seekTo(0);
            }
        }

        @Override
        public void onBurstPrepared(Object o) {
        }
        @Override
        public void onFrameCaptureProgressed(Object o) {
        }

        @Override
        public void onFrameCaptureCompleted(Object o) {
            mCameraUIView.incrementCaptureProgressBar(1);
            if (PreferenceKeys.isCameraSoundsOn()) {
                burstPlayer.start();
            }
            if (o instanceof TimerFrameCountViewModel.FrameCntTime) {
                timerFrameCountViewModel.setFrameTimeCnt((TimerFrameCountViewModel.FrameCntTime) o);
            }
        }

        @Override
        public void onCaptureSequenceCompleted(Object o) {
            if (PreferenceKeys.isCameraSoundsOn()) {
                endPlayer.start();
            }
            timerFrameCountViewModel.clearFrameTimeCnt();
            mCameraUIView.resetCaptureProgressBar();
            mCameraUIView.lockUIForBurst(false);
            textureView.post(() -> textureView.setAlpha(1f));
        }

        @Override
        public void onPreviewCaptureCompleted(CaptureResult captureResult) {
            updateScreenLog(captureResult);
        }

        /**
         * Implementation of abstract methods of {@link CameraEventsListener}
         */

        @Override
        public void onOpenCamera(CameraManager cameraManager) {
            initCameraIDLists(cameraManager);
            auxButtonsViewModel.initCameraLists(mCameraLensDataMap);
        }

        @Override
        public void onCameraRestarted() {
            mCameraUIView.refresh(CaptureController.isProcessing);
            mTouchFocus.resetFocusCircle();
        }

        @Override
        public void onCharacteristicsUpdated(CameraCharacteristics characteristics) {
            auxButtonsViewModel.setActiveId(PreferenceKeys.getCameraID());
            Boolean flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            mCameraUIView.showFlashButton(flashAvailable != null && flashAvailable);
            manualModeConsole.init(activity, characteristics, PhotonCamera.getSettings().useExtendIso, PhotonCamera.getSettings().useExtendExposure);
            manualModeConsole.onResume();
        }

        @Override
        public void onError(Object o) {
            if (o instanceof String) {
                showErrorDialog(o.toString());
            }
            if (o instanceof Integer) {
                showErrorDialog((Integer) o);
            }
        }

        @Override
        public void onFatalError(String errorMsg) {
            logE("onFatalError: " + errorMsg);
            activity.finish();
        }

        @Override
        public void onRequestTriggerMediaScanner(Uri fileUri) {
            triggerMediaScanner(fileUri);
        }

        public void onVideoRecordingStarted(File vid, boolean isTenBit, boolean isHdr) {
            requireActivity().runOnUiThread(() -> {
                mVidFile = vid;
                mIsTenBit = isTenBit;
                mIsHdr = isHdr;
                recordingStartTime = System.currentTimeMillis();
                recordingTimerTextView.setVisibility(View.VISIBLE);
                recordingSizeTextView.setText("0MB");
                recordingSizeTextView.setVisibility(View.VISIBLE);
                if (mIsTenBit) {
                    tenBitIndicatorTextView.setVisibility(View.VISIBLE);
                    tenBitIndicatorTextView.invalidate();
                } else {
                    tenBitIndicatorTextView.setVisibility(View.GONE);
                }
                if (mIsHdr) {
                    hdrIndicatorTextView.setVisibility(View.VISIBLE);
                    hdrIndicatorTextView.invalidate();
                }
                else {
                    hdrIndicatorTextView.setVisibility(View.GONE);
                }

                timerHandlerVideoRec.post(timerRunnableVideoRec);
            });
        }

        public void onPreviewStarted() {
            requireActivity().runOnUiThread(() -> {
                currentIsoTextView.setVisibility(View.VISIBLE);
                currentShutterTextView.setVisibility(View.VISIBLE);

                timerHandlerAlways.post(timerRunnableAlways);
            });
        }

        @Override
        public void onVideoRecordingStopped() {
            requireActivity().runOnUiThread(() -> {
                timerHandlerVideoRec.removeCallbacks(timerRunnableVideoRec);
                recordingTimerTextView.setVisibility(View.GONE);
                recordingSizeTextView.setVisibility(View.GONE);
                recordingTimerTextView.setText("00:00");
                recordingSizeTextView.setText("0MB");
                currentIsoTextView.setText("ISO100");
                currentShutterTextView.setText("1/100s");
                tenBitIndicatorTextView.setVisibility(View.GONE);
                hdrIndicatorTextView.setVisibility(View.GONE);
            });
        }
    }
}

class AppCpuReader {
    private static final String TAG = "AppCpuReader";
    private long lastAppCpuTime = 0;
    private long lastSystemTime = 0;
    private int cpuCores = 0;

    public AppCpuReader() {
        cpuCores = Runtime.getRuntime().availableProcessors();
    }

    public float getCpuUsage() {
        int pid = Process.myPid();
        String procFile = "/proc/" + pid + "/stat";

        try (BufferedReader reader = new BufferedReader(new FileReader(procFile))) {
            String line = reader.readLine();
            String[] parts = line.split(" ");

            long utime = Long.parseLong(parts[13]);
            long stime = Long.parseLong(parts[14]);
            long cutime = Long.parseLong(parts[15]);
            long cstime = Long.parseLong(parts[16]);

            long appCpuTime = utime + stime + cutime + cstime;
            long systemTime = System.nanoTime();

            if (lastAppCpuTime == 0 || lastSystemTime == 0) {
                lastAppCpuTime = appCpuTime;
                lastSystemTime = systemTime;
                return 0f;
            }

            long appCpuTimeDelta = appCpuTime - lastAppCpuTime;
            long systemTimeDelta = systemTime - lastSystemTime;

            lastAppCpuTime = appCpuTime;
            lastSystemTime = systemTime;

            if (systemTimeDelta > 0) {
                long appCpuTimeNs = appCpuTimeDelta * 10_000_000L;
                float cpuUsage = (float) appCpuTimeNs / systemTimeDelta;
                return (cpuUsage / cpuCores) * 100f;
            }
        } catch (IOException e) {
            Log.e(TAG, "Fehler beim Lesen der CPU-Statistik.", e);
            return -1f;
        }

        return 0f;
    }
}
