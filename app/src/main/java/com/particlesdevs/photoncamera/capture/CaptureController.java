package com.particlesdevs.photoncamera.capture;
/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.ColorSpaceTransform;
import android.hardware.camera2.params.DynamicRangeProfiles;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioFormat;
import android.media.CamcorderProfile;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.MediaCodecInfo;
import android.media.AudioRecord;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import com.particlesdevs.photoncamera.util.Log;
import android.util.Range;
import android.util.Rational;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;
import android.graphics.ColorSpace;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.api.Camera2ApiAutoFix;
import com.particlesdevs.photoncamera.api.CameraEventsListener;
import com.particlesdevs.photoncamera.api.CameraManager2;
import com.particlesdevs.photoncamera.api.CameraMode;
import com.particlesdevs.photoncamera.api.CameraReflectionApi;
import com.particlesdevs.photoncamera.api.Settings;
import com.particlesdevs.photoncamera.api.VendorTagUtils;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.control.GyroBurst;
import com.particlesdevs.photoncamera.control.TouchFocus;
import com.particlesdevs.photoncamera.debugclient.DebugSender;
import com.particlesdevs.photoncamera.manual.ParamController;
import com.particlesdevs.photoncamera.processing.ImageSaver;
import com.particlesdevs.photoncamera.processing.parameters.ExposureIndex;
import com.particlesdevs.photoncamera.processing.parameters.FrameNumberSelector;
import com.particlesdevs.photoncamera.processing.parameters.IsoExpoSelector;
import com.particlesdevs.photoncamera.processing.parameters.ResolutionSolution;
import com.particlesdevs.photoncamera.settings.PreferenceKeys;
import com.particlesdevs.photoncamera.ui.camera.CameraFragment;
import com.particlesdevs.photoncamera.ui.camera.viewmodel.TimerFrameCountViewModel;
import com.particlesdevs.photoncamera.ui.camera.views.viewfinder.AutoFitPreviewView;
import com.particlesdevs.photoncamera.ui.camera.views.viewfinder.GLPreview;
import com.particlesdevs.photoncamera.util.log.Logger;
import android.media.MediaFormat;
import android.media.MediaCodec;
import android.media.MediaMuxer;
//import android.media.MediaFormat.ColorSpace;
import android.hardware.camera2.params.TonemapCurve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO;
import static android.hardware.camera2.CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON;
import static android.hardware.camera2.CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF;
import static android.hardware.camera2.CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION;
import static android.hardware.camera2.CameraMetadata.FLASH_MODE_TORCH;
import static android.hardware.camera2.CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF;
import static android.hardware.camera2.CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON;
import static android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE;
import static android.hardware.camera2.CaptureRequest.CONTROL_AE_REGIONS;
import static android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE;
import static android.hardware.camera2.CaptureRequest.CONTROL_AF_REGIONS;
import static android.hardware.camera2.CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE;
import static android.hardware.camera2.CaptureRequest.FLASH_MODE;
import static android.hardware.camera2.CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE;

/**
 * Class responsible for image capture and sending images for subsequent processing
 * <p>
 * All relevant events are notified to cameraEventsListener
 * <p>
 * Constructor {@link CaptureController#CaptureController(Activity, ExecutorService, CameraEventsListener)}
 */
public class CaptureController implements MediaRecorder.OnInfoListener {
    public static final int RAW_FORMAT = ImageFormat.RAW_SENSOR;
    public static final int HEIC_FORMAT = ImageFormat.HEIC;
    public static final int YUV_FORMAT = ImageFormat.YUV_420_888;
    private static final String TAG = CaptureController.class.getSimpleName();
    public List<Future<?>> taskResults = new ArrayList<>();
    private final ExecutorService processExecutor;
    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;
    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;
    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;
    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;
    private static final int STATE_CLOSED = 5;
    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;
    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;
    /**
     * Timeout for the pre-capture sequence.
     */
    private static final long PRECAPTURE_TIMEOUT_MS = 100;
    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    private boolean useMaximumResolutionKey = false;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    private Map<String, CameraCharacteristics> mCameraCharacteristicsMap = new HashMap<>();
    public static CameraCharacteristics mCameraCharacteristics;
    public static CaptureResult mCaptureResult;
    public static CaptureRequest mCaptureRequest;

    public static CaptureResult mPreviewCaptureResult;
    public static CaptureRequest mPreviewCaptureRequest;
    public static int mPreviewTargetFormat = ImageFormat.JPEG;
    public boolean isDualSession = false;
    private static int mTargetFormat = ImageFormat.RAW_SENSOR;
    private final ParamController paramController;
    public TouchFocus mTouchFocus;

    public final boolean mFlashEnabled = false;
    private CameraEventsListener cameraEventsListener;
    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private final Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private CameraManager mCameraManager;
    private CameraManager2 mCameraManager2;
    private Activity activity;
    public long mPreviewExposureTime;
    /**
     * ID of the current {@link CameraDevice}.
     */
    public int mPreviewIso;
    public Rational[] mPreviewTemp;
    public ColorSpaceTransform mColorSpaceTransform;
    /**
     * A reference to the opened {@link CameraDevice}.
     */
    public CameraDevice mCameraDevice;
    /*A {@link Handler} for running tasks in the background.*/
    public Handler mBackgroundHandler;
    /*An {@link ImageReader} that handles still image capture.*/
    public ImageReader mImageReaderPreview;
    public ImageReader mImageReaderRaw;
    /*{@link CaptureRequest.Builder} for the camera preview*/
    public CaptureRequest.Builder mPreviewRequestBuilder;
    public CaptureRequest mPreviewInputRequest;
    /**
     * The current state of camera state for taking pictures.
     */
    public int mState = STATE_PREVIEW;
    /**
     * Orientation of the camera sensor
     */
    public int mSensorOrientation;
    public int cameraRotation;
    public int videoRotation = 0;
    public boolean is30Fps = true;
    public boolean onUnlimited = false;
    public boolean unlimitedStarted = false;
    public boolean mFlashed = false;
    public ArrayList<GyroBurst> BurstShakiness;

    float[] highContrastPoints = {
            0.0f, 0.0f,
            0.25f, 0.15f,
            0.50f, 0.50f,
            0.75f, 0.85f,
            1.0f, 1.0f
    };
    float[] lowContrastPoints = {
            0.00f, 0.15f,
            0.25f, 0.30f,
            0.50f, 0.50f,
            0.75f, 0.70f,
            1.00f, 0.85f
    };
    float[] flatCurve = {
            0.00f, 0.00f,
            0.25f, 0.255f,
            0.50f, 0.50f,
            0.75f, 0.75f,
            1.00f, 1.00f
    };
    float[] slog2ApproxPoints = {
            0.00f, 0.031f,
            0.10f, 0.283f,
            0.20f, 0.384f,
            0.30f, 0.457f,
            0.40f, 0.516f,
            0.50f, 0.567f,
            0.60f, 0.612f,
            0.70f, 0.654f,
            0.80f, 0.693f,
            0.90f, 0.730f,
            1.00f, 0.765f
    };
    public static final float[] S_LOG2_APPROX_POINTS = {
            0.00f, 0.030f,
            0.10f, 0.150f,
            0.20f, 0.250f,
            0.30f, 0.350f,
            0.40f, 0.450f,
            0.50f, 0.530f,
            0.60f, 0.610f,
            0.70f, 0.700f,
            0.80f, 0.800f,
            0.90f, 0.900f,
            1.00f, 0.950f
    };

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    public ImageSaver mImageSaver;
    public HashMap<Long, Double> mExposures = new HashMap<>();
    private final ImageReader.OnImageAvailableListener mOnYuvImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            mImageSaver.initProcess(reader);
        }
    };
    private final ImageReader.OnImageAvailableListener mOnRawImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            if (onUnlimited && !unlimitedStarted) {
                return;
            }
            if(PhotonCamera.getSettings().frameCount != 1) {
                //taskResults.removeIf(Future::isDone); //remove already completed results
                //Future<?> result = processExecutor.submit(() -> mImageSaver.initProcess(reader));
                //taskResults.add(result);
                //processExecutor.execute(() -> mImageSaver.initProcess(reader));
                mImageSaver.initProcess(reader);
                //mBackgroundHandler.post(() -> mImageSaver.initProcess(reader));
                //AsyncTask.execute(() -> mImageSaver.initProcess(reader));
            }
            else {
                mBackgroundHandler.post(() -> mImageSaver.initProcess(reader));
                //mImageSaver.initProcess(reader);
                //processExecutor.execute(() -> mImageSaver.initProcess(reader));
            }
        }

    };
    private Range<Integer> FpsRangeDef;
    private Range<Integer> FpsRangeHigh;
    private int[] mCameraAfModes;
    private int mPreviewWidth;
    private int mPreviewHeight;
    private ArrayList<CaptureRequest> captures;
    private CameraCaptureSession.CaptureCallback CaptureCallback = null;
    private File vid = null;
    public int mMeasuredFrameCnt;
    public static boolean isProcessing;
    /**
     * An {@link AutoFitPreviewView} for camera preview.
     */
    private GLPreview mTextureView = null;
    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession = null;
    /**
     * MediaRecorder
     */
    private MediaRecorder mMediaRecorder = null;
    private MediaFormat mVideoFormat = null;
    private MediaFormat mAudioFormat = null;
    private MediaCodec mVideoCodec = null;
    private MediaCodec mAudioCodec = null;
    private MediaMuxer mMediaMuxer = null;
    private Surface mMediaCodecSurface = null;
    private RecordingUtils.VideoEncoderCallback mVideoEncoderCallback = null;
    private RecordingUtils.AudioEncoderCallback mAudioEncoderCallback = null;
    private RecordingUtils.MuxerThread mMuxerThread = null;
    private RecordingUtils.EncoderData mEncoderData = null;
    AudioRecord mAudioRecord = null;
    /**
     * Whether the app is recording video now
     */
    public boolean mIsRecordingVideo;
    private Size target;
    private float mFocus;
    public int mPreviewAFMode;
    public int mPreviewAEMode;
    public MeteringRectangle[] mPreviewMeteringAF;
    public MeteringRectangle[] mPreviewMeteringAE;
    /**
     * The {@link Size} of camera preview.
     */
    public Size mPreviewSize;
    public Size mBufferSize;
    /*An additional thread for running tasks that shouldn't block the UI.*/
    private HandlerThread mBackgroundThread;
    /**
     * Timer to use with pre-capture sequence to ensure a timely capture if 3A convergence is
     * taking too long.
     */
    private long mCaptureTimer;
    /**
     * Whether the current camera device supports Flash or not.
     */
    private boolean mFlashSupported;
    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    public static boolean burst = false;
    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    public ProcessCallbacks debugCallback = new ProcessCallbacks();
    private final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            debugCallback.process();
            switch (mState) {
                case STATE_PREVIEW:
                    previewProcess();
                    break;
                case STATE_WAITING_LOCK:
                    waitingLockProcess(result);
                    break;
                case STATE_WAITING_PRECAPTURE:
                    waitingPrecaptureProcess(result);
                    break;
                case STATE_WAITING_NON_PRECAPTURE:
                    waitingNonPrecaptureProcess(result);
                    break;
            }
        }

        private void previewProcess() {
            // We have nothing to do when the camera preview is working normally.
            //Log.v(TAG, "PREVIEW");
        }

        private void waitingLockProcess(CaptureResult result) {
            //Log.v(TAG, "WAITING_LOCK");
            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
            // If we haven't finished the pre-capture sequence but have hit our maximum
            // wait timeout, too bad! Begin capture anyway.
            if (hitTimeoutLocked()) {
                Log.w(TAG, "Timed out waiting for pre-capture sequence to complete.");
                mState = STATE_PICTURE_TAKEN;
                captureStillPicture();
            }
            if (afState == null) {
                mState = STATE_PICTURE_TAKEN;
                captureStillPicture();
            } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                // CONTROL_AE_STATE can be null on some devices
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState == null ||
                        aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    mState = STATE_PICTURE_TAKEN;
                    captureStillPicture();
                } else {
                    runPreCaptureSequence();
                }
            }
        }

        private void waitingPrecaptureProcess(CaptureResult result) {
            Log.v(TAG, "WAITING_PRECAPTURE");
            // CONTROL_AE_STATE can be null on some devices
            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
            if (aeState == null ||
                    aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                    aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                mState = STATE_WAITING_NON_PRECAPTURE;
            }
            if (paramController.isManualMode())
                mState = STATE_WAITING_NON_PRECAPTURE;
        }

        private void waitingNonPrecaptureProcess(CaptureResult result) {
            // CONTROL_AE_STATE can be null on some devices
            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
            if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                mState = STATE_PICTURE_TAKEN;
                captureStillPicture();
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {

            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            Object exposure = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
            Object iso = result.get(CaptureResult.SENSOR_SENSITIVITY);
            Object focus = result.get(CaptureResult.LENS_FOCUS_DISTANCE);
            Rational[] mTemp = result.get(CaptureResult.SENSOR_NEUTRAL_COLOR_POINT);
            if (exposure != null) mPreviewExposureTime = (long) exposure;
            if (iso != null) mPreviewIso = (int) iso;
            if (focus != null) mFocus = (float) focus;
            if (mTemp != null) mPreviewTemp = mTemp;
            if (mPreviewTemp == null) {
                mPreviewTemp = new Rational[3];
                for (int i = 0; i < mPreviewTemp.length; i++)
                    mPreviewTemp[i] = new Rational(101, 100);
            }
            mColorSpaceTransform = result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM);
            Integer state = result.get(CaptureResult.FLASH_STATE);
            mFlashed = state != null && state == CaptureResult.FLASH_STATE_PARTIAL || state == CaptureResult.FLASH_STATE_FIRED;
            mPreviewCaptureResult = result;
            mPreviewCaptureRequest = request;
            process(result);
            cameraEventsListener.onPreviewCaptureCompleted(result);
            if(PreferenceKeys.getAfMode() == CaptureRequest.CONTROL_AF_MODE_AUTO && !burst && !mTouchFocus.isTouchFocus) {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
                rebuildPreviewBuilderOneShot();
            }
        }

        //Automatic 60fps preview
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
            /*boolean combinedFpsResult = PhotonCamera.getSettings().fpsPreview;
            if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                combinedFpsResult = PhotonCamera.getSettings().fpsPreview || (PhotonCamera.getSettings().videoFramrate == 60);
            }
            if (frameNumber % 20 == 19) {
                if ((!is30Fps && ExposureIndex.index() - 2.0 > 8.0) || (!is30Fps && !combinedFpsResult)) {
                    if (!is30Fps) {
                        Log.d(TAG, "Changed preview target 30fps");
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, FpsRangeDef);
                        try {
                            mCaptureSession.stopRepeating();
                        } catch (CameraAccessException e) {
                            Log.e(TAG, Log.getStackTraceString(e));
                        }
                        rebuildPreviewBuilder();
                        is30Fps = true;
                    }
                }
                if (ExposureIndex.index() + 2.0 < 8.0) {
                    if (is30Fps && combinedFpsResult && !mCameraDevice.getId().equals("1")) {
                        Log.d(TAG, "Changed preview target 60fps");
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, FpsRangeHigh);
                        try {
                            mCaptureSession.stopRepeating();
                        } catch (CameraAccessException e) {
                            Log.e(TAG, Log.getStackTraceString(e));
                        }
                        rebuildPreviewBuilder();
                        is30Fps = false;
                    }
                }
            }*/
        }
    };
    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            mImageSaver = new ImageSaver(cameraEventsListener);
            createCameraPreviewSession(false);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            showToast("onError() : cameraDevice = [" + cameraDevice + "], error = [" + error + "]");
        }
    };
    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    public final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture texture, int width, int height) {
            try {
                String curID = PhotonCamera.getSettings().mCameraID;
                if(curID.contains("-")){
                    logicalID = curID.split("-")[0];
                    physicalID = curID.split("-")[1];
                } else {
                    logicalID = curID;
                    physicalID = curID;
                }
                Log.d(TAG, "ID:" + mCameraCharacteristicsMap.get(physicalID));
                // list available characteristics ids
                for (String id : mCameraCharacteristicsMap.keySet()) {
                    Log.d(TAG, "Available camera ID: " + id);
                }
                Size optimal = getPreviewOutputSize(mTextureView.getDisplay(),
                        mCameraCharacteristicsMap.get(physicalID),
                        PhotonCamera.getSettings().selectedMode);
                openCamera(optimal.getWidth(), optimal.getHeight());
            } catch (Exception e){
                Log.e(TAG,Log.getStackTraceString(e));
                showToast("Error onSurfaceTextureAvailable:"+e.getLocalizedMessage());
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture texture, int width, int height) {
            Log.d(TAG, " CHANGED SIZE:" + width + ' ' + height);
            configureTransform(width, height);
        }

        @Override

        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture texture) {
        }

    };

    public void setPreviewFormat() {
        mPreviewTargetFormat = ImageFormat.YUV_420_888;
        /*mPreviewTargetFormat = ImageFormat.JPEG;
        if ((PhotonCamera.getSettings().frameCount == 1) && (PhotonCamera.getSettings().previewFormat == ImageFormat.HEIC) && !PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            mPreviewTargetFormat = ImageFormat.HEIC;
        }*/
    }

    public void setTargetFormat() {
        if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            mTargetFormat = ImageFormat.RAW_SENSOR;
        }
        else if (PhotonCamera.getSettings().frameCount == 1) {
            if (PhotonCamera.getSettings().previewFormat == ImageFormat.JPEG || PhotonCamera.getSettings().previewFormat == ImageFormat.HEIC) {
                mTargetFormat = PhotonCamera.getSettings().previewFormat;
            }
            else {
                mTargetFormat = ImageFormat.RAW_SENSOR;
            }
        }
    }

    public CaptureController(Activity activity, ExecutorService processExecutor, CameraEventsListener cameraEventsListener) {
        setPreviewFormat();

        this.activity = activity;
        this.cameraEventsListener = cameraEventsListener;
        this.mTextureView = activity.findViewById(R.id.texture);
        this.mCameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        this.mCameraManager2 = new CameraManager2(mCameraManager, PhotonCamera.getInstance(activity).getSettingsManager());
        PreferenceKeys.addIds(mCameraManager2.getCameraIdList());

        this.processExecutor = processExecutor;
        this.paramController = new ParamController(this);

        this.fillInCameraCharacteristics();
    }

    /**
     * Fills in {@link CaptureController#mCameraCharacteristicsMap} that is used in
     * {@link CaptureController#UpdateCameraCharacteristics}.
     */
    private void fillInCameraCharacteristics() {
        try {
            String[] cameraIds = mCameraManager2.getCameraIdList();
            for (String cameraId : cameraIds) {
                String physicalID = cameraId;
                if(cameraId.contains("-")){
                    physicalID = cameraId.split("-")[1];
                }
                mCameraCharacteristicsMap.put(physicalID, mCameraManager.getCameraCharacteristics(physicalID));
            }
        } catch (CameraAccessException cameraAccessException) {
            // Should not be possible to get here but anyway
            cameraAccessException.printStackTrace();
            showToast("Failed to fetch camera characteristics: " + cameraAccessException.getLocalizedMessage());
        }

    }

    public ParamController getParamController() {
        return paramController;
    }

    public static int getTargetFormat() {
        return mTargetFormat;
    }

    public Range<Integer> getFpsRangeDef() {
        boolean combinedFpsResult60 = PhotonCamera.getSettings().fpsPreview;
        if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            combinedFpsResult60 = PhotonCamera.getSettings().fpsPreview || (PhotonCamera.getSettings().videoFramrate == 60);
        }

        if (combinedFpsResult60) {
            return FpsRangeHigh;
        }
        else {
            return FpsRangeDef;
        }
    }

    public static void setTargetFormat(int targetFormat) {
        mTargetFormat = targetFormat;
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int targetWidth = aspectRatio.getWidth();
        int targetHeight = aspectRatio.getHeight();
        for (Size option : choices) {
            int width = option.getWidth();
            int height = option.getHeight();
            boolean isAspectRatioMatching = (height * targetWidth == width * targetHeight);

            if (width <= maxWidth && height <= maxHeight && isAspectRatioMatching) {
                if (width >= textureViewWidth && height >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough.
        // If there is no one big enough, pick the largest of those not big enough.
        if (!bigEnough.isEmpty()) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (!notBigEnough.isEmpty()) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private Size getCameraOutputSize(Size[] sizes) {
        if (sizes != null) {
            if (sizes.length > 0) {
                Arrays.sort(sizes, new CompareSizesByArea());

                int largestSizeIdx = sizes.length - 1;
                int largestSizeArea = sizes[largestSizeIdx].getWidth() * sizes[largestSizeIdx].getHeight();

                if (largestSizeArea <= ResolutionSolution.highRes) {
                    target = sizes[largestSizeIdx];
                    return target;
                } else if (sizes.length > 1) {
                    target = sizes[largestSizeIdx - 1];
                    return target;
                }
            }
        }
        return null;
    }

    /**
     * For test method {@link CaptureController#getCameraOutputSize(Size[])}
     */
    @TestOnly
    private static Size getCameraOutputSizeTest(Size[] sizes) {
        if (sizes != null) {
            if (sizes.length > 0) {
                Arrays.sort(sizes, new CompareSizesByArea());

                int largestSizeIdx = sizes.length - 1;
                int largestSizeArea = sizes[largestSizeIdx].getWidth() * sizes[largestSizeIdx].getHeight();

                if (largestSizeArea <= ResolutionSolution.highRes) {
                    return sizes[largestSizeIdx];
                } else if (sizes.length > 1) {
                    return sizes[largestSizeIdx - 1];
                }
            }
        }
        return null;
    }

    private Size getCameraOutputSize(Size[] sizes, Size previewSize) {
        if (sizes == null || sizes.length == 0) return previewSize;

        Arrays.sort(sizes, new CompareSizesByArea());
        int largestSizeIdx = sizes.length - 1;
        int largestSizeArea = sizes[largestSizeIdx].getWidth() * sizes[largestSizeIdx].getHeight();

        if (largestSizeArea <= ResolutionSolution.highRes || PhotonCamera.getSettings().QuadBayer) {
            target = sizes[largestSizeIdx];
            if (PhotonCamera.getSettings().QuadBayer) {
                Rect preCorrectionActiveArraySize = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE);
                Rect activeArraySize = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

                if (preCorrectionActiveArraySize != null && activeArraySize != null) {
                    double k = (double) (target.getHeight()) / activeArraySize.bottom;
                    mul(preCorrectionActiveArraySize, k);
                    mul(activeArraySize, k);
                    CameraReflectionApi.set(mCameraCharacteristics, CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE, activeArraySize);
                    CameraReflectionApi.set(mCameraCharacteristics, CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE, preCorrectionActiveArraySize);
                }
            }
            return target;
        } else if (sizes.length > 1) {
            target = sizes[largestSizeIdx - 1];
            return target;
        }
        return previewSize;
    }

    /**
     * For test method {@link CaptureController#getCameraOutputSize(Size[], Size)}
     */
    @TestOnly
    private static Size getCameraOutputSizeTest(Size[] sizes, Size previewSize) {
        if (sizes == null || sizes.length == 0) return previewSize;

        Size temp = null;

        Arrays.sort(sizes, new CompareSizesByArea());
        int largestSizeIdx = sizes.length - 1;
        int largestSizeArea = sizes[largestSizeIdx].getWidth() * sizes[largestSizeIdx].getHeight();

        if (largestSizeArea <= ResolutionSolution.highRes || PhotonCamera.getSettings().QuadBayer) {
            temp = sizes[largestSizeIdx];
            if (PhotonCamera.getSettings().QuadBayer) {
                Rect preCorrectionActiveArraySize = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE);
                Rect activeArraySize = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

                if (preCorrectionActiveArraySize != null && activeArraySize != null) {
                    double k = (double) (temp.getHeight()) / activeArraySize.bottom;
                    mulForTest(preCorrectionActiveArraySize, k);
                    mulForTest(activeArraySize, k);
                    CameraReflectionApi.set(mCameraCharacteristics, CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE, activeArraySize);
                    CameraReflectionApi.set(mCameraCharacteristics, CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE, preCorrectionActiveArraySize);
                }
            }
            return temp;
        } else if (sizes.length > 1) {
            temp = sizes[largestSizeIdx - 1];
            return temp;
        }
        return previewSize;
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private void setUpCameraOutputs(int width, int height) {
        try {
            mPreviewWidth = width;
            mPreviewHeight = height;
            String curID = PhotonCamera.getSettings().mCameraID;
            if(curID.contains("-")) {
                logicalID = curID.split("-")[0];
                physicalID = curID.split("-")[1];
            } else {
                logicalID = curID;
                physicalID = logicalID;
            }
            UpdateCameraCharacteristics(physicalID);
            //Thread thr = new Thread(mImageSaver);
            //thr.start();
        } catch (Exception e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            Log.e(TAG, Log.getStackTraceString(e));
            showToast(activity.getString(R.string.camera_error));
            //cameraEventsListener.onError(R.string.camera_error);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    public void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReaderPreview) {
                if (!isProcessing) {
                    mImageReaderPreview.close();
                    mImageReaderPreview = null;
                }
                if (!isProcessing) {
                    mImageReaderRaw.close();
                    mImageReaderRaw = null;
                }
            }
            if (null != mMediaRecorder) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }

            releaseMediaRecorderNew();

            mState = STATE_CLOSED;
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    public void startBackgroundThread() {
        if (mBackgroundThread == null) {
            mBackgroundThread = new HandlerThread("CameraBackground");
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
            Log.d(TAG, "startBackgroundThread() called from \"" + Thread.currentThread().getName() + "\" Thread");
        }
        //mBackgroundHandler.post(mImageSaver);
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    public void stopBackgroundThread() {
        if (mBackgroundThread == null)
            return;
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
            Log.d(TAG, "stopBackgroundThread() called from \"" + Thread.currentThread().getName() + "\" Thread");
        } catch (InterruptedException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public void rebuildPreviewBuilder() {
        if(burst) return;
        try {
//            mCaptureSession.stopRepeating();
            mCaptureSession.setRepeatingRequest(mPreviewInputRequest = mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (IllegalStateException | IllegalArgumentException | NullPointerException e) {
            Logger.warnShort(TAG, "Cannot rebuildPreviewBuilder()!", e);
        } catch (CameraAccessException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public void rebuildPreviewBuilderOneShot() {
        if(burst) return;
        try {
            Log.d(TAG, "rebuildPreviewBuilderOneShot: " + mCaptureSession + " " + mPreviewRequestBuilder + " " + mCaptureCallback + " " + mBackgroundHandler);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (IllegalStateException | IllegalArgumentException | NullPointerException e) {
            Logger.warnShort(TAG, "Cannot rebuildPreviewBuilderOneShot()!", e);
        } catch (CameraAccessException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreviewSize) {
            return;
        }
        int rotation = PhotonCamera.getGravity().getRotation();//activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        mTextureView.setOrientation(mSensorOrientation+90);
    }

    private ArrayList<Size> getAllTargets(){
        CameraCharacteristics characteristics =  this.mCameraCharacteristicsMap.get(physicalID);
        StreamConfigurationMap map = characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        ArrayList<Size> allTargets = new ArrayList<>();

        setTargetFormat();

        Size[] targetSizes = map.getOutputSizes(mTargetFormat);
        if(targetSizes != null)
            allTargets.addAll(Arrays.asList(targetSizes));
        if(PhotonCamera.getSettings().QuadBayer) {
            useMaximumResolutionKey = false;
            int[] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
            for (int capability : capabilities) {
                if (capability == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR) {
                    Size arraySize = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        arraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION);
                    }
                    if(arraySize != null) {
                        useMaximumResolutionKey = true;
                        allTargets.add(arraySize);
                    }
                }
            }
            if(!useMaximumResolutionKey) {
                Size[] highResSizes = map.getHighResolutionOutputSizes(mTargetFormat);
                // Extend targetSizes with high resolution sizes
                if (highResSizes != null && highResSizes.length > 0) {
                    allTargets.addAll(Arrays.asList(highResSizes));
                }
                var keys = CameraReflectionApi.getCameraCharacteristicsKeys(characteristics, null, true);
                for (Object keyObj : keys) {
                    try {
                        if (keyObj instanceof CameraCharacteristics.Key<?>) {
                            CameraCharacteristics.Key<?> key = (CameraCharacteristics.Key<?>) keyObj;
                            if (key.getName().contains("StreamConfigurations")) {
                                Object res = characteristics.get(key);
                                int[] vals = (int[]) res;
                                for (int i = 0; i < vals.length; i += 4) {
                                    int format = vals[i];
                                    int width = vals[i + 1];
                                    int height = vals[i + 2];
                                    if (format == mTargetFormat) {
                                        allTargets.add(new Size(width, height));
                                        Log.d(TAG, "Added custom resolution(" + key.getName() + "):" + width + " " + height);
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return allTargets;
    }
    @SuppressLint("MissingPermission")
    public void restartCamera() {
        CameraFragment.mSelectedMode = PhotonCamera.getSettings().selectedMode;
        try {
            mCameraOpenCloseLock.acquire();
            if (mIsRecordingVideo) {
                this.VideoEnd();
            }

            if (mCaptureSession != null) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReaderPreview) {
                if (!isProcessing) {
                    mImageReaderPreview.close();
                    mImageReaderPreview = null;
                }
                if (!isProcessing) {
                    mImageReaderRaw.close();
                    mImageReaderRaw = null;
                }
            }
            if (null != mMediaRecorder) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
            if (null != mPreviewRequestBuilder) {
                mPreviewRequestBuilder = null;
            }
            stopBackgroundThread();
            cameraEventsListener.onCameraRestarted();
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw new RuntimeException("Interrupted while trying to lock camera restarting.", e);
        } finally {
            try {
                mCameraOpenCloseLock.release();
            } catch (Exception ignored) {
                showToast("Failed to release camera");
            }
        }
        String curID = PhotonCamera.getSettings().mCameraID;
        if(curID.contains("-")) {
            logicalID = curID.split("-")[0];
            physicalID = curID.split("-")[1];
        } else {
            logicalID = curID;
            physicalID = logicalID;
        }
        CameraCharacteristics characteristics =  this.mCameraCharacteristicsMap.get(physicalID);
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        ArrayList<Size> allTargets = getAllTargets();
        Size preview = getCameraOutputSize(map.getOutputSizes(mPreviewTargetFormat));
        Size target = getCameraOutputSize(allTargets.toArray(new Size[0]), preview);
        int max = 3;
        if (mTargetFormat == mPreviewTargetFormat && isDualSession) {
            max = PhotonCamera.getSettings().frameCount + 3;
        }
        //largest = target;
        mImageReaderPreview = ImageReader.newInstance(target.getWidth(), target.getHeight(), mPreviewTargetFormat, /*maxImages*/max);
        mImageReaderPreview.setOnImageAvailableListener(mOnYuvImageAvailableListener, mBackgroundHandler);

        mImageReaderRaw = ImageReader.newInstance(target.getWidth(), target.getHeight(), mTargetFormat, max);
        mImageReaderRaw.setOnImageAvailableListener(mOnRawImageAvailableListener, mBackgroundHandler);

        try {
            if (!mCameraOpenCloseLock.tryAcquire(1000, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            this.mCameraManager.openCamera(logicalID, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to restart camera.", e);
        }
        //stopBackgroundThread();
        UpdateCameraCharacteristics(physicalID);
        startBackgroundThread();

        Size optimal = getPreviewOutputSize(mTextureView.getDisplay(), mCameraCharacteristics, CameraFragment.mSelectedMode);

        setUpCameraOutputs(optimal.getWidth(), optimal.getHeight());
        configureTransform(optimal.getWidth(), optimal.getHeight());
    }
    private Size getAspect(CameraMode targetMode){
        Size aspectRatio;
        boolean test1 = PhotonCamera.getSettings().aspect169;
        if ((targetMode == CameraMode.VIDEO
                && (PhotonCamera.getSettings().videoHeight != 9999) && (PhotonCamera.getSettings().videoHeight != 8888) && (PhotonCamera.getSettings().videoHeight != 7777))
                || PhotonCamera.getSettings().aspect169) {
            aspectRatio = new Size(9, 16);
        } else {
            aspectRatio = new Size(3, 4);
        }
        return aspectRatio;
    }

    //Size for preview drawing
    private Size getTextureOutputSize(
            Display display,
            CameraMode targetMode
    ) {
        Size aspectRatio = getAspect(targetMode);
        Point displayPoint = new Point();
        display.getRealSize(displayPoint);
        int shortSide = Math.min(displayPoint.x, displayPoint.y);
        int longSide = shortSide * aspectRatio.getHeight() / aspectRatio.getWidth();

        return new Size(longSide, shortSide);
    }

    //Size for preview buffer
    private Size getPreviewOutputSize(
            Display display,
            CameraCharacteristics characteristics,
            CameraMode targetMode
    ) {
        Size aspectRatio = getAspect(targetMode);
        Point displayPoint = new Point();
        display.getRealSize(displayPoint);
        int shortSide = Math.min(displayPoint.x, displayPoint.y);
        int longSide = shortSide / aspectRatio.getWidth() * aspectRatio.getHeight();


        // If image format is provided, use it to determine supported sizes; else use target class
        StreamConfigurationMap config = characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        Size[] allSizes = config.getOutputSizes(SurfaceTexture.class);

        Size retsize = null;
        for (Size size : allSizes) {
            int sizeShort = Math.min(size.getHeight(), size.getWidth());
            int sizeLong = Math.max(size.getHeight(), size.getWidth());
            if (sizeLong % aspectRatio.getHeight() == 0 &&
                    sizeShort == aspectRatio.getWidth() * sizeLong / aspectRatio.getHeight() &&
                    sizeShort * sizeLong <= ResolutionSolution.previewRes) {
                retsize = new Size(sizeShort, sizeLong);
                break;
            }
        }
        if (retsize == null) {
            retsize = new Size(800, 600);
        }
        return retsize;
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        if(burst) return;
        startTimerLocked();
        // This is how to tell the camera to lock focus.
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START);
        // Tell #mCaptureCallback to wait for the lock.
        mState = STATE_WAITING_LOCK;
        try {
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to start camera preview because it couldn't access camera", e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to start camera preview.", e);
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPreCaptureSequence() {
        if(burst) return;
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException | IllegalStateException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private String physicalID = "";
    private String logicalID = "";

    /**
     * Opens the camera specified by {@link Settings#mCameraID}.
     */
    public void openCamera(int width, int height) {
        //Open camera in non ui thread
        processExecutor.execute(()->{
            CameraFragment.mSelectedMode = PhotonCamera.getSettings().selectedMode;
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                //requestCameraPermission();
                return;
            }
            processExecutor.execute(()-> {
                mMediaRecorder = new MediaRecorder();
            });
            cameraEventsListener.onOpenCamera(this.mCameraManager);
            setUpCameraOutputs(width, height);
            configureTransform(width, height);
            try {
                if (!mCameraOpenCloseLock.tryAcquire(1000, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Time out waiting to lock camera opening.");
                }
                physicalID = PhotonCamera.getSettings().mCameraID;
                logicalID = PhotonCamera.getSettings().mCameraID;
                // Split x-y, x - logical, y - physical
                if(PhotonCamera.getSettings().mCameraID.contains("-")){
                    String[] ids = PhotonCamera.getSettings().mCameraID.split("-");
                    logicalID = ids[0];
                    physicalID = ids[1];
                    isDualSession = true;
                }
                this.mCameraManager.openCamera(logicalID, mStateCallback, mBackgroundHandler);
            } catch (CameraAccessException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
            }
        });
    }
    public void setAdvancedParameters(CaptureRequest.Builder captureBuilder) {
        // we do this only in video mode or if framecount is 1 or if forced with forceNewSettingsInRegularPhotoMode
        if (!PhotonCamera.getSpecific().specificSetting.forceNewSettingsInRegularPhotoMode) {
            if (!PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO) && (PhotonCamera.getSettings().frameCount != 1)) {
                return;
            }
        }

        captureBuilder.set(CaptureRequest.JPEG_QUALITY, (byte)PhotonCamera.getSettings().singleFrameQuality);
        //captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION));
        // QualityDoesMatter
        captureBuilder.set(CaptureRequest.STATISTICS_HOT_PIXEL_MAP_MODE, PhotonCamera.getSpecific().specificSetting.statisticsHotPixelMapMode);
        if (PhotonCamera.getSpecific().specificSetting.exposureCompensation != 99)
            captureBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, PhotonCamera.getSpecific().specificSetting.exposureCompensation);
        if (PhotonCamera.getSpecific().specificSetting.hotPixelMode != 99)
            captureBuilder.set(CaptureRequest.HOT_PIXEL_MODE, PhotonCamera.getSpecific().specificSetting.hotPixelMode);
        if (PhotonCamera.getSpecific().specificSetting.colorCorrectionAberrationMode != 99)
            captureBuilder.set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, PhotonCamera.getSpecific().specificSetting.colorCorrectionAberrationMode);
        if (PhotonCamera.getSpecific().specificSetting.distortionCorrectionMode != 99)
            captureBuilder.set(CaptureRequest.DISTORTION_CORRECTION_MODE, PhotonCamera.getSpecific().specificSetting.distortionCorrectionMode);
        if (PhotonCamera.getSpecific().specificSetting.shadingMode != 99)
            captureBuilder.set(CaptureRequest.SHADING_MODE, PhotonCamera.getSpecific().specificSetting.shadingMode);
        if (PhotonCamera.getSpecific().specificSetting.statisticsLensShadingMapMode != 99)
            captureBuilder.set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE, PhotonCamera.getSpecific().specificSetting.statisticsLensShadingMapMode);
        if (PhotonCamera.getSpecific().specificSetting.statisticsOisDataMode != 99)
            captureBuilder.set(CaptureRequest.STATISTICS_OIS_DATA_MODE, PhotonCamera.getSpecific().specificSetting.statisticsOisDataMode);
        if (PhotonCamera.getSpecific().specificSetting.toneMapGamma != 99) {
            captureBuilder.set(CaptureRequest.TONEMAP_MODE, CameraMetadata.TONEMAP_MODE_GAMMA_VALUE);
            captureBuilder.set(CaptureRequest.TONEMAP_GAMMA, 1/PhotonCamera.getSpecific().specificSetting.toneMapGamma);
        }
        if (PhotonCamera.getSpecific().specificSetting.colorTemperature != 99) {
            try {
                captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_OFF);
                captureBuilder.set(CaptureRequest.COLOR_CORRECTION_COLOR_TEMPERATURE, PhotonCamera.getSpecific().specificSetting.colorTemperature);
            }
            catch (Exception e){
                Log.d(TAG, "setCaptureRequestBuilder:"+e);
            }
        }
    }
    public void setContrastCurve(CaptureRequest.Builder captureBuilder) {
        // we do this only in video mode or if framecount is 1 or if forced with forceNewSettingsInRegularPhotoMode
        if (!PhotonCamera.getSpecific().specificSetting.forceNewSettingsInRegularPhotoMode) {
            if (!PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO) && (PhotonCamera.getSettings().frameCount != 1)) {
                return;
            }
        }

        // look for keywords
        if (!PhotonCamera.getSpecific().specificSetting.contrastCurve.equals("slog2") &&
                !PhotonCamera.getSpecific().specificSetting.contrastCurve.equals("high") &&
                !PhotonCamera.getSpecific().specificSetting.contrastCurve.equals("flat") &&
                !PhotonCamera.getSpecific().specificSetting.contrastCurve.equals("low")) {
            return;
        }

        TonemapCurve customCurve = null;
        //captureBuilder.set(CaptureRequest.TONEMAP_MODE, CameraMetadata.TONEMAP_MODE_CONTRAST_CURVE);
        if (PhotonCamera.getSpecific().specificSetting.contrastCurve.equals("slog2")) {
            int points = 64;
            float[] red = new float[points * 2];
            float[] green = new float[points * 2];
            float[] blue = new float[points * 2];

            for (int i = 0; i < points; i++) {
                float x = i / (float)(points - 1);
                float y = 0.432699f * (float)Math.log10(10.0f * x + 1.0f);

                red[i * 2] = x;
                red[i * 2 + 1] = y;

                green[i * 2] = x;
                green[i * 2 + 1] = y;

                blue[i * 2] = x;
                blue[i * 2 + 1] = y;
            }

            customCurve = new TonemapCurve(red, green, blue);
        }
        else if (PhotonCamera.getSpecific().specificSetting.contrastCurve.equals("high")) {
            customCurve = new TonemapCurve(highContrastPoints, highContrastPoints, highContrastPoints);
        }
        else if (PhotonCamera.getSpecific().specificSetting.contrastCurve.equals("low")) {
            customCurve = new TonemapCurve(lowContrastPoints, lowContrastPoints, lowContrastPoints);
        }
        else if (PhotonCamera.getSpecific().specificSetting.contrastCurve.equals("flat")) {
            customCurve = new TonemapCurve(flatCurve, flatCurve, flatCurve);
        }

        //captureBuilder.set(CaptureRequest.TONEMAP_CURVE, customCurve);
    }
    public void UpdateCameraCharacteristics(String cameraId) {
        PhotonCamera.getSpecificSensor().selectSpecifics(Integer.parseInt(cameraId));
        CameraCharacteristics characteristics = this.mCameraCharacteristicsMap.get(cameraId);
        mCameraCharacteristics = characteristics;
        //Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

        StreamConfigurationMap map = null;
        if (mCameraCharacteristics != null) {
            map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        }
        if (map == null) {
            return;
        }
        ArrayList<Size> allTargets = getAllTargets();

        Size preview = getCameraOutputSize(map.getOutputSizes(mPreviewTargetFormat));
        Size target = getCameraOutputSize(allTargets.toArray(new Size[0]), preview);
        int maxjpg = 3;
        if (mTargetFormat == mPreviewTargetFormat && isDualSession) {
            maxjpg = PhotonCamera.getSettings().frameCount + 3;
        }

        Size aspect = getAspect(PhotonCamera.getSettings().selectedMode);
        if(preview.getWidth() > preview.getHeight()) {
            preview = new Size(preview.getWidth(), preview.getWidth() * aspect.getWidth() / aspect.getHeight());
        }
        else {
            preview = new Size(preview.getHeight()*aspect.getWidth()/aspect.getHeight(),preview.getHeight());
        }
        mImageReaderPreview = ImageReader.newInstance(preview.getWidth(), preview.getHeight(), mPreviewTargetFormat, maxjpg);
        mImageReaderPreview.setOnImageAvailableListener(mOnYuvImageAvailableListener, mBackgroundHandler);
        mBufferSize = getPreviewOutputSize(mTextureView.getDisplay(),characteristics,PhotonCamera.getSettings().selectedMode);

        if(mImageReaderRaw != null) {
            mImageReaderRaw.close();
        }
        mImageReaderRaw = ImageReader.newInstance(target.getWidth(), target.getHeight(), mTargetFormat, maxjpg);
        mImageReaderRaw.setOnImageAvailableListener(mOnRawImageAvailableListener, mBackgroundHandler);
        // Find out if we need to swap dimension to get the preview size relative to sensor
        // coordinate.
        int displayRotation = PhotonCamera.getGravity().getRotation();
        mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        Range<Integer>[] ranges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        int def = 30;
        if ((PhotonCamera.getSettings().videoFramrate == 24.0f) && PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            def = 24;
        }
        else if ((PhotonCamera.getSettings().videoFramrate == 60.0f) && PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            def = 60;
        }
        else if ((PhotonCamera.getSettings().videoFramrate == 48.0f) && PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            def = 48;
        }
        else if ((PhotonCamera.getSettings().videoFramrate == 50.0f) && PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            def = 50;
        }
        int min = 20;
        if (ranges == null) {
            ranges = new Range[1];
            if ((PhotonCamera.getSettings().videoFramrate == 24.0f) && PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                ranges[0] = new Range<>(24, 24);
            }
            if ((PhotonCamera.getSettings().videoFramrate == 48.0f) && PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                ranges[0] = new Range<>(48, 48);
            }
            if ((PhotonCamera.getSettings().videoFramrate == 50.0f) && PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                ranges[0] = new Range<>(50, 50);
            }
            else {
                ranges[0] = new Range<>(15, 30);
            }
        }
        for (Range<Integer> value : ranges) {
            if ((int) value.getUpper() >= def) {
                FpsRangeDef = value;
                break;
            }
        }

        if (PhotonCamera.getSettings().videoFramrate != 24.0f) {
            if (FpsRangeDef == null)
                for (Range<Integer> range : ranges) {
                    if ((int) range.getUpper() >= min) {
                        FpsRangeDef = range;
                        break;
                    }
                }
            for (Range<Integer> range : ranges) {
                if (range.getUpper() > def) {
                    FpsRangeDef = range;
                    break;
                }
            }
        }
        if(FpsRangeHigh == null) {
            FpsRangeHigh = new Range<>(60, 60);
        }

        if ((PhotonCamera.getSettings().videoFramrate == 24.0f) && PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            FpsRangeDef = new Range<>(24, 24);
        }
        if ((PhotonCamera.getSettings().videoFramrate == 48) && PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            FpsRangeDef = new Range<>(48, 48);
        }
        if ((PhotonCamera.getSettings().videoFramrate == 50) && PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            FpsRangeDef = new Range<>(50, 50);
        }
        else if(FpsRangeDef == null || FpsRangeDef.getLower() > def) {
            FpsRangeDef = new Range<>(7, 30);
        }

        mCameraAfModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);

        // Check if the flash is supported.
        Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        mFlashSupported = available != null && available;
        Camera2ApiAutoFix.Init();
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
//            setUpMediaRecorder();
        }
        activity.runOnUiThread(() -> {
            //Preview drawing size changing
            mPreviewSize = getTextureOutputSize(mTextureView.getDisplay(), PhotonCamera.getSettings().selectedMode);
            mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            cameraEventsListener.onCharacteristicsUpdated(characteristics);
            if ((PhotonCamera.getSettings().DebugData && !PhotonCamera.getSettings().useBasicOsd))
            {
                showToast("preview:" + new Point(mPreviewWidth, mPreviewHeight));
            }
        });
        //activity.runOnUiThread(() -> cameraEventsListener.onCharacteristicsUpdated(characteristics));
    }
    Surface surface;
    public void createCameraPreviewSession(boolean isBurstSession) {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            // We configure the size of default buffer to be the size of camera preview we want.
            Log.d(TAG, "createCameraPreviewSession() mTextureView:" + mTextureView);
            Log.d(TAG, "createCameraPreviewSession() Texture:" + texture);
            Log.d(TAG, "bufferSize:" + mBufferSize);
            Log.d(TAG, "previewSize:" + mPreviewSize);
            Log.d(TAG, "ID:" + PhotonCamera.getSettings().mCameraID + " deviceID:" + mCameraDevice.getId() + " logicalID:" + logicalID + " physicalID:" + physicalID);

            //Camera output
            texture.setDefaultBufferSize(mBufferSize.getHeight(), mBufferSize.getWidth());

            // This is the output Surface we need to start preview.
            if(surface == null)
                surface = new Surface(texture);
            // We set up a CaptureRequest.Builder with the output Surface.
            setCaptureRequestBuilder();

            // Here, we create a CameraCaptureSession for camera preview.
            List<Surface> surfaces = configureSurfaces(isBurstSession);
            Log.d(TAG, "createCameraPreviewSession() surfaces:" + Arrays.toString(surfaces.toArray()));
            ArrayList<OutputConfiguration> outputConfigurations = new ArrayList<>();
            for (Surface surfacei : surfaces) {
                var config = new OutputConfiguration(surfacei);
                if(!Objects.equals(physicalID, logicalID) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    config.setPhysicalCameraId(physicalID);
                }
                if (mIsRecordingVideo && PhotonCamera.getSettings().videoHDR) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                        config.setDynamicRangeProfile(DynamicRangeProfiles.HLG10);
                    }
                }
                outputConfigurations.add(config);
            }

            CameraCaptureSession.StateCallback stateCallback =
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.d(TAG, "CameraCaptureSession onConfigured():" + cameraCaptureSession);
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }
                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            CameraConstrainedHighSpeedCaptureSession highSpeedSession = null;
                            if ((PhotonCamera.getSettings().videoFramrate >= 120) && mIsRecordingVideo) {
                                highSpeedSession = (CameraConstrainedHighSpeedCaptureSession) cameraCaptureSession;
                            }
                            try {
                                // Auto focus should be continuous for camera preview.
                                //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                resetPreviewAEMode();
                                Camera2ApiAutoFix.applyPrev(mPreviewRequestBuilder);
                                VendorTagUtils.builderSessionApply(mCameraCharacteristics, mPreviewRequestBuilder, false, useMaximumResolutionKey);
                                // Finally, we start displaying the camera preview.
                                boolean combinedFpsResult60 = PhotonCamera.getSettings().fpsPreview;
                                if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                                    combinedFpsResult60 = PhotonCamera.getSettings().fpsPreview || (PhotonCamera.getSettings().videoFramrate == 60);
                                }
                                if (!combinedFpsResult60) {
                                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, FpsRangeDef);
                                } else {
                                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, FpsRangeHigh);
                                }

                                // QualityDoesMatter
                                setAdvancedParameters(mPreviewRequestBuilder);
                                setContrastCurve(mPreviewRequestBuilder);

                                if ((PhotonCamera.getSettings().videoFramrate >= 120) && mIsRecordingVideo) {
                                    List<CaptureRequest> highSpeedRequests = highSpeedSession.createHighSpeedRequestList(mPreviewRequestBuilder.build());
                                }
                                mPreviewInputRequest = mPreviewRequestBuilder.build();
                                if (isBurstSession && isDualSession) {
                                    switch (CameraFragment.mSelectedMode) {
                                        case NIGHT:
                                        case PHOTO:
                                        case MOTION:
                                            mCaptureSession.captureBurst(captures, CaptureCallback, mBackgroundHandler);
                                            break;
                                        case UNLIMITED:
                                            mCaptureSession.setRepeatingBurst(captures, CaptureCallback, mBackgroundHandler);
                                            break;
                                    }
                                } else {
                                    //if(mSelectedMode != CameraMode.VIDEO)
                                    mCaptureSession.setRepeatingRequest(mPreviewInputRequest, mCaptureCallback, mBackgroundHandler);
                                    unlockFocus();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, Log.getStackTraceString(e));
                            }
                            if (mIsRecordingVideo)
                                activity.runOnUiThread(() -> {
                                    // Start recording
                                    if (PhotonCamera.getSpecific().specificSetting.useNewRecordingPipeline) {
                                        //mMediaMuxer.start();
                                    }
                                    else {
                                        mMediaRecorder.start();
                                    }
                                });
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast(activity.getString(R.string.session_on_configure_failed));
                        }
                    };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                int SessionType =  SessionConfiguration.SESSION_REGULAR;
                if ((PhotonCamera.getSettings().videoFramrate >= 120) && mIsRecordingVideo) {
                    SessionType =  SessionConfiguration.SESSION_HIGH_SPEED;
                }
                SessionConfiguration configuration = new SessionConfiguration(
                        SessionType,
                        outputConfigurations,
                        processExecutor,
                        stateCallback
                );

                /*if (checkColorSpaceProfilesSupport(mCameraManager)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        configuration.setColorSpace(ColorSpace.Named.SRGB);
                    }
                }*/

                mCameraDevice.createCaptureSession(configuration);
            } else {
                mCameraDevice.createCaptureSession(surfaces, stateCallback, mBackgroundHandler);
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    @NotNull
    private List<Surface> configureSurfaces(boolean isBurstSession) {
        List<Surface> surfaces = Arrays.asList(surface, mImageReaderPreview.getSurface());
        if (isDualSession) {
            if (isBurstSession) {
                surfaces = Arrays.asList(mImageReaderPreview.getSurface(), mImageReaderRaw.getSurface());
            }
            if (mTargetFormat == mPreviewTargetFormat) {
                surfaces = Arrays.asList(surface, mImageReaderPreview.getSurface());
            }
        } else {
            if(Build.BRAND.equalsIgnoreCase("samsung")){
                surfaces = Arrays.asList(surface, mImageReaderRaw.getSurface());
            } else {
                surfaces = Arrays.asList(surface, mImageReaderPreview.getSurface(), mImageReaderRaw.getSurface());
            }
            surfaces = Arrays.asList(surface, mImageReaderPreview.getSurface(), mImageReaderRaw.getSurface());
            if(PhotonCamera.getSettings().previewFormat == 0) {
                surfaces = Arrays.asList(surface, mImageReaderRaw.getSurface());
            }
        }
        if (mIsRecordingVideo) {
            if (PhotonCamera.getSpecific().specificSetting.useNewRecordingPipeline) {
                setUpMediaRecorderNew();
                surfaces = Arrays.asList(surface, mMediaCodecSurface);
                mPreviewRequestBuilder.addTarget(mMediaCodecSurface);
            }
            else {
                setUpMediaRecorder();
                surfaces = Arrays.asList(surface, mMediaRecorder.getSurface());
                mPreviewRequestBuilder.addTarget(mMediaRecorder.getSurface());
            }
        }
        return surfaces;
    }

    private void setCaptureRequestBuilder() throws CameraAccessException {
        mPreviewRequestBuilder = null;
        if (mIsRecordingVideo) {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        } else {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        }

        mPreviewRequestBuilder.addTarget(surface);
        mPreviewMeteringAF = mPreviewRequestBuilder.get(CONTROL_AF_REGIONS);
        mPreviewAFMode = PreferenceKeys.getAfMode();

        // QualityDoesMatter
        setAdvancedParameters(mPreviewRequestBuilder);
        setContrastCurve(mPreviewRequestBuilder);

        //if (PhotonCamera.getSpecific().specificSetting.singleShotZoomFactor != 99) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_ZOOM_METHOD, CaptureRequest.CONTROL_ZOOM_METHOD_ZOOM_RATIO);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (PhotonCamera.getSettings().zoom2X) {
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, 2.0f);
                }
                else {
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, 1.0f);
                }

            }
        //}

        // AF mode for video
        if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            mPreviewRequestBuilder.set(CONTROL_AF_MODE, CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            mPreviewAFMode = CONTROL_AF_MODE_CONTINUOUS_VIDEO;
        }

        // electronic stabilization for video if turned on
        if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO) && PreferenceKeys.isEisPhotoOn()) {
            // QualityDoesMatter
            if (PhotonCamera.getSettings().videoEisInPreview) {
                mPreviewRequestBuilder.set(CONTROL_VIDEO_STABILIZATION_MODE, CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION);
            }
            else {
                mPreviewRequestBuilder.set(CONTROL_VIDEO_STABILIZATION_MODE, CONTROL_VIDEO_STABILIZATION_MODE_ON);
            }
        }
        else {
            mPreviewRequestBuilder.set(CONTROL_VIDEO_STABILIZATION_MODE, CONTROL_VIDEO_STABILIZATION_MODE_OFF);
        }

        // QualityDoesMatter
        mPreviewRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, PhotonCamera.getSettings().noiseProcessing);
        mPreviewRequestBuilder.set(CaptureRequest.EDGE_MODE, PhotonCamera.getSettings().edgeProcessing);

        mPreviewMeteringAE = mPreviewRequestBuilder.get(CONTROL_AE_REGIONS);
        mPreviewAEMode = mPreviewRequestBuilder.get(CONTROL_AE_MODE);
    }

    private void showToast(String msg) {
        if (activity != null) {
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Initiate a still image capture.
     */
    public void takePicture() {
        if (mCameraAfModes.length > 1) lockFocus();
        else {
            try {
                mState = STATE_WAITING_NON_PRECAPTURE;
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
            } catch (CameraAccessException e) {
                Log.e(TAG, "Failed to start camera preview because it couldn't access camera", e);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Failed to start camera preview.", e);
            }
        }
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    public void unlockFocus() {
        try {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            rebuildPreviewBuilderOneShot();
            reset3Aparams();
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            rebuildPreviewBuilderOneShot();
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            rebuildPreviewBuilderOneShot();
            paramController.setupPreview();
            mState = STATE_PREVIEW;
            rebuildPreviewBuilder();
        }catch(Exception e){
            Log.d(TAG, "unlockFocus:"+e);
        }
    }
    public CaptureRequest.Builder getDebugCaptureRequestBuilder(){
        final CaptureRequest.Builder captureBuilder;
        try {
            captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            if (mTargetFormat != mPreviewTargetFormat)
                captureBuilder.addTarget(mImageReaderRaw.getSurface());
            else
                captureBuilder.addTarget(mImageReaderPreview.getSurface());
            return captureBuilder;
        } catch (CameraAccessException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return null;
    }
    private void debugCapture(CaptureRequest.Builder builder){
        try {
            if (null == mCameraDevice) {
                return;
            }
            Camera2ApiAutoFix.applyEnergySaving();
            captures = new ArrayList<>();

            int frameCount = 1;
            cameraEventsListener.onFrameCountSet(frameCount);

            captures.add(builder.build());


            Log.d(TAG, "FrameCount:" + frameCount);

            Log.d(TAG, "CaptureStarted!");

            final long[] baseFrameNumber = {0};
            final int[] maxFrameCount = {frameCount};

            cameraEventsListener.onCaptureStillPictureStarted("CaptureStarted!");
            mMeasuredFrameCnt = 0;
            mImageSaver.implementation = new DebugSender(cameraEventsListener);

            cameraEventsListener.onBurstPrepared(null);
            this.CaptureCallback = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session,
                                             @NonNull CaptureRequest request,
                                             long timestamp,
                                             long frameNumber) {

                    if (baseFrameNumber[0] == 0) {
                        baseFrameNumber[0] = frameNumber - 1L;
                        Log.v("BurstCounter", "CaptureStarted with FirstFrameNumber:" + frameNumber);
                    } else {
                        Log.v("BurstCounter", "CaptureStarted:" + frameNumber);
                    }
                    cameraEventsListener.onFrameCaptureStarted(null);
                }

                @Override
                public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                                @NonNull CaptureResult partialResult) {
                    //mCaptureResult = partialResult;
                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {

                    int frameCount = (int) (result.getFrameNumber() - baseFrameNumber[0]);
                    Log.v("BurstCounter", "CaptureCompleted! FrameCount:" + frameCount);
                    long frametime = 100;
                    Object time = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
                    if(time != null) frametime = (long)time;
                    cameraEventsListener.onFrameCaptureCompleted(
                            new TimerFrameCountViewModel.FrameCntTime(frameCount, maxFrameCount[0], frametime));
                    mCaptureResult = result;
                }

                @Override
                public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session,
                                                       int sequenceId,
                                                       long lastFrameNumber) {

                    int finalFrameCount = (int) (lastFrameNumber - baseFrameNumber[0]);
                    Log.v("BurstCounter", "CaptureSequenceCompleted! FrameCount:" + finalFrameCount);
                    Log.v("BurstCounter", "CaptureSequenceCompleted! LastFrameNumber:" + lastFrameNumber);
                    Log.d(TAG, "SequenceCompleted");
                    mBackgroundHandler.postDelayed(() -> {
                        while(mImageSaver.implementation.IMAGE_BUFFER.size() > PhotonCamera.getSettings().frameCount/2) {
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException ignored) {}
                        }
                        cameraEventsListener.onCaptureSequenceCompleted(null);
                    }, 100);
                    mMeasuredFrameCnt = finalFrameCount;
                    burst = false;
                    //Surface texture related
                    activity.runOnUiThread(() -> UpdateCameraCharacteristics(physicalID));
                    if (!isDualSession)
                        unlockFocus();
                    else
                        createCameraPreviewSession(false);
                    taskResults.removeIf(Future::isDone); //remove already completed results
                    Future<?> result = processExecutor.submit(() -> mImageSaver.runRaw(mCameraCharacteristics, mCaptureResult, mCaptureRequest, new ArrayList<>(BurstShakiness), cameraRotation, mExposures));
                    taskResults.add(result);
                }
            };
            burst = true;
            Camera2ApiAutoFix.ApplyBurst();
            if (isDualSession)
                createCameraPreviewSession(true);
            else {
                mCaptureSession.captureBurst(captures, CaptureCallback, mBackgroundHandler);
            }

        } catch (CameraAccessException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public void runDebug(CaptureRequest.Builder builder){
        activity.runOnUiThread(() -> debugCapture(builder));
    }

    private void captureStillPicture() {
        try {
            if (null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            float focus = mFocus;
            double frametime = ExposureIndex.time2sec(IsoExpoSelector.GenerateExpoPair(-1, this).exposure);

            // QualityDoesMatter
            setAdvancedParameters(captureBuilder);
            setContrastCurve(captureBuilder);

            //if (PhotonCamera.getSpecific().specificSetting.singleShotZoomFactor != 99) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_ZOOM_METHOD, CaptureRequest.CONTROL_ZOOM_METHOD_ZOOM_RATIO);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (PhotonCamera.getSettings().zoom2X) {
                        captureBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, 2.0f);
                    }
                    else {
                        captureBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, 1.0f);
                    }
                }
            //}

            var CurrHotPixelMode = captureBuilder.get(CaptureRequest.HOT_PIXEL_MODE);
            Log.d(TAG, "HOT_PIXEL_MODE: " + CurrHotPixelMode.toString());
            if (isDualSession) {
                if (mTargetFormat != mPreviewTargetFormat)
                    captureBuilder.addTarget(mImageReaderRaw.getSurface());
                else
                    captureBuilder.addTarget(mImageReaderPreview.getSurface());
            } else {
                captureBuilder.addTarget(mImageReaderRaw.getSurface());
                if(frametime > 0.06 && !isDualSession || PhotonCamera.getSettings().selectedMode == CameraMode.MOTION) {
                    captureBuilder.addTarget(surface);
                }
            }

            //captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION));

            Camera2ApiAutoFix.applyEnergySaving();
            cameraRotation = PhotonCamera.getGravity().getCameraRotation(mSensorOrientation);

            if (mFlashed) captureBuilder.set(FLASH_MODE, FLASH_MODE_TORCH);
            Log.d(TAG, "Focus:" + focus);
            captureBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL);
            int[] stabilizationModes = mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION);
            if (stabilizationModes != null && stabilizationModes.length > 1) {
                Log.d(TAG, "LENS_OPTICAL_STABILIZATION_MODE");
                if (PhotonCamera.getSettings().useOis == true) {
                    captureBuilder.set(LENS_OPTICAL_STABILIZATION_MODE, LENS_OPTICAL_STABILIZATION_MODE_ON);//Fix ois bugs for preview and burst
                } else {
                    captureBuilder.set(LENS_OPTICAL_STABILIZATION_MODE, LENS_OPTICAL_STABILIZATION_MODE_OFF);//Fix ois bugs for preview and burst
                }
            }
            // QualityDoesMatter
            captureBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, PhotonCamera.getSettings().noiseProcessing);
            captureBuilder.set(CaptureRequest.EDGE_MODE, PhotonCamera.getSettings().edgeProcessing);
            for (int i = 0; i < 3; i++) {
                Log.d(TAG, "Temperature:" + mPreviewTemp[i]);
            }
            Log.d(TAG, "CaptureBuilderStarted!");
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, PhotonCamera.getGravity().getCameraRotation(mSensorOrientation));
            VendorTagUtils.builderSessionApply(mCameraManager.getCameraCharacteristics(mCameraDevice.getId()), captureBuilder, true, useMaximumResolutionKey);
			//captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION));
            captures = new ArrayList<>();
            BurstShakiness = new ArrayList<>();

            int frameCount = FrameNumberSelector.getFrames();
            //if (frameCount == 1) frameCount++;
            cameraEventsListener.onFrameCountSet(frameCount);
            Log.d(TAG, "HDRFact1:" + paramController.isManualMode() + " HDRFact2:" + PhotonCamera.getSettings().alignAlgorithm);
            IsoExpoSelector.HDR = true;
            Log.d(TAG, "HDR:" + IsoExpoSelector.HDR);
            Object mode = mPreviewRequestBuilder.get(CONTROL_AF_MODE);
            if(mode != null && (int) mode != CaptureRequest.CONTROL_AF_MODE_AUTO || PreferenceKeys.getAfMode() == CaptureRequest.CONTROL_AF_MODE_AUTO) {
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
                captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
            }
            MeteringRectangle rectaf = new MeteringRectangle(0, 0, 0, 0, 0);
            IsoExpoSelector.useTripod = PhotonCamera.getGyro().getTripod();
            if (frameCount == -1) {
                for (int i = 0; i < 1; i++) {
                    IsoExpoSelector.setExpo(captureBuilder, i, this);
                    //captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION));
                    captures.add(captureBuilder.build());
                }
            } else {
                long[] times = new long[frameCount];
                for (int i = 0; i < frameCount; i++) {
                    IsoExpoSelector.setExpo(captureBuilder, i, this);
                    times[i] = IsoExpoSelector.lastSelectedExposure;
                    captures.add(captureBuilder.build());
                    //captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION));
                    mCaptureRequest = captureBuilder.build();
                }
                PhotonCamera.getGyro().PrepareGyroBurst(times, BurstShakiness);
            }

            //img
            Log.d(TAG, "FrameCount:" + frameCount);
            mImageSaver = new ImageSaver(cameraEventsListener);
            mImageSaver.setFrameCount(frameCount);
            Log.d(TAG, "CaptureStarted!");

            final long[] baseFrameNumber = {0};
            final int[] maxFrameCount = {frameCount};

            cameraEventsListener.onCaptureStillPictureStarted("CaptureStarted!");
            mMeasuredFrameCnt = 0;

            cameraEventsListener.onBurstPrepared(null);
            this.CaptureCallback = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session,
                                             @NonNull CaptureRequest request,
                                             long timestamp,
                                             long frameNumber) {

                    if (baseFrameNumber[0] == 0) {
                        baseFrameNumber[0] = frameNumber;
                        if (maxFrameCount[0] != -1) PhotonCamera.getGyro().CaptureGyroBurst();
                        Log.v("BurstCounter", "CaptureStarted with FirstFrameNumber:" + frameNumber);
                    } else {
                        Log.v("BurstCounter", "CaptureStarted:" + frameNumber);
                    }
                    cameraEventsListener.onFrameCaptureStarted(null);
                }

                @Override
                public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                                @NonNull CaptureResult partialResult) {
                    int frameCount = (int) (partialResult.getFrameNumber() - baseFrameNumber[0]);
                    Log.v("BurstCounter", "CaptureProgressed! FrameCount:" + frameCount);
                    if (mCaptureResult == null) {
                        mCaptureResult = partialResult;
                    }
                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {

                    int frameCount = (int) (result.getFrameNumber() - baseFrameNumber[0]);
                    Log.v("BurstCounter", "CaptureCompleted! FrameCount:" + frameCount);
                    Object time = result.get(CaptureResult.SENSOR_TIMESTAMP);
                    Log.d(TAG, "Timestamp:" + time);
                    if (time != null) {
                        // get exposure multiply ISO and exposure time
                        Object isoKey = result.get(CaptureResult.SENSOR_SENSITIVITY);
                        int iso = 50;
                        if (isoKey != null) {
                            iso = (int) isoKey;
                        }
                        Object timeKey = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
                        double exposureTime = ExposureIndex.time2sec((long) timeKey);
                        mExposures.put((long) time, exposureTime * iso);
                    }
                    cameraEventsListener.onFrameCaptureCompleted(
                            new TimerFrameCountViewModel.FrameCntTime(frameCount, maxFrameCount[0], frametime));

                    if (onUnlimited && !unlimitedStarted) {
                        mImageSaver.unlimitedStart(mCameraCharacteristics, result, request, cameraRotation);
                        unlimitedStarted = true;
                    }
                    if(frameCount == 0)
                        mCaptureResult = result;
                    if (maxFrameCount[0] != -1) PhotonCamera.getGyro().CaptureGyroBurst();
                }

                @Override
                public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session,
                                                       int sequenceId,
                                                       long lastFrameNumber) {

                    int finalFrameCount = (int) (lastFrameNumber - baseFrameNumber[0]) + 1;
                    Log.v("BurstCounter", "CaptureSequenceCompleted! FrameCount:" + finalFrameCount);
                    Log.d("DefaultSaver", "CaptureSequenceCompleted! FrameCount:" + finalFrameCount);
                    Log.v("BurstCounter", "CaptureSequenceCompleted! LastFrameNumber:" + lastFrameNumber);
                    Log.d(TAG, "SequenceCompleted");
                    mMeasuredFrameCnt = finalFrameCount;
                    cameraEventsListener.onCaptureSequenceCompleted(null);
                    burst = false;
                    if (PhotonCamera.getSettings().selectedMode != CameraMode.UNLIMITED) {
                        processExecutor.execute(() -> {
                            int cnt = 0;
                            //int captureNumber = PhotonCamera.getGyro().capturingNumber;
                            while (PhotonCamera.getGyro().capturingNumber < finalFrameCount || mImageSaver.bufferSize() < finalFrameCount){
                                if(cnt > 1000) {
                                    Log.d(TAG, "GyroBurstTimeout");
                                    break;
                                }
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException ignored) {
                                }
                                cnt++;
                            }
                            PhotonCamera.getGyro().CompleteSequence();
                            mBackgroundHandler.post(() -> {
                                if (!isDualSession)
                                    unlockFocus();
                                else
                                    createCameraPreviewSession(false);
                            });
                            try{
                                if(mImageSaver.bufferSize() == 0){
                                    return;
                                }
                                mImageSaver.updateFrameCount(mImageSaver.bufferSize());
                                if (mImageSaver.bufferSize() != 0)
                                    mImageSaver.runRaw(mCameraCharacteristics, mCaptureResult, mCaptureRequest, new ArrayList<>(BurstShakiness), cameraRotation, mExposures);
                            } catch (Exception e){
                                Log.e(TAG, "runRaw:"+Log.getStackTraceString(e));
                                cameraEventsListener.onProcessingError(e.getLocalizedMessage());
                            }
                        });
                    }
                }
            };
            burst = true;
            Camera2ApiAutoFix.ApplyBurst();
            if (isDualSession)
                createCameraPreviewSession(true);
            else {
                switch (PhotonCamera.getSettings().selectedMode) {
                    case UNLIMITED:
                        mCaptureSession.setRepeatingBurst(captures, CaptureCallback, mBackgroundHandler);
                        break;
                    case NIGHT:
                    case PHOTO:
                    case MOTION:
                        mCaptureSession.captureBurst(captures, CaptureCallback, mBackgroundHandler);
                        break;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public void abortCaptures() {
        try {
            mCaptureSession.abortCaptures();
        } catch (CameraAccessException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public void reset3Aparams() {
        setAEMode(mPreviewRequestBuilder, PreferenceKeys.getAeMode());
        setAFMode(mPreviewRequestBuilder, PreferenceKeys.getAfMode());
        rebuildPreviewBuilder();
    }

    public void setPreviewAEModeRebuild(int aeMode) {
        setAEMode(mPreviewRequestBuilder, aeMode);
        rebuildPreviewBuilder();
    }

    public void resetPreviewAEMode() {
        setAEMode(mPreviewRequestBuilder, PreferenceKeys.getAeMode());
    }

    /**
     * @param requestBuilder CaptureRequest.Builder
     * @param aeMode         possible values = 0, 1, 2, 3
     */
    private void setAEMode(CaptureRequest.Builder requestBuilder, int aeMode) {
        if (requestBuilder != null) {
            if (mFlashSupported) {
                requestBuilder.set(CONTROL_AE_MODE, Math.max(aeMode, 1));//here AE_MODE will never be OFF(0)
                requestBuilder.set(CaptureRequest.FLASH_MODE,
                        aeMode == 0 ? CaptureRequest.FLASH_MODE_TORCH : CaptureRequest.FLASH_MODE_OFF);
            } else {
                requestBuilder.set(CONTROL_AE_MODE, CONTROL_AE_MODE_ON);
                requestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            }
        }
    }

    private void setAFMode(CaptureRequest.Builder builder, int afMode) {
        if (builder != null) {
            builder.set(CaptureRequest.CONTROL_AF_REGIONS, builder.get(CONTROL_AF_REGIONS));
            builder.set(CaptureRequest.CONTROL_AE_REGIONS, builder.get(CONTROL_AE_REGIONS));
            builder.set(CaptureRequest.CONTROL_AF_MODE, afMode);
        }
    }

    /**
     * Start the timer for the pre-capture sequence.
     * <p/>
     * Call this only with { #mCameraStateLock} held.
     */
    private void startTimerLocked() {
        mCaptureTimer = SystemClock.elapsedRealtime();
    }

    /**
     * Check if the timer for the pre-capture sequence has been hit.
     * <p/>
     * Call this only with { #mCameraStateLock} held.
     *
     * @return true if the timeout occurred.
     */
    private boolean hitTimeoutLocked() {
        return (SystemClock.elapsedRealtime() - mCaptureTimer) > PRECAPTURE_TIMEOUT_MS;
    }

    public void callUnlimitedEnd() {
        onUnlimited = false;
        //mImageSaver.unlimitedEnd();
        mBackgroundHandler.post(() -> mImageSaver.unlimitedEnd());
        abortCaptures();
        createCameraPreviewSession(false);
        unlimitedStarted = false;
    }

    public void callUnlimitedStart() {
        onUnlimited = true;
        takePicture();
    }

    public void VideoEnd() {
        mIsRecordingVideo = false;
        stopRecordingVideo();
    }

    public void VideoStart() {
        mIsRecordingVideo = true;
        createCameraPreviewSession(false);
    }

    private boolean checkColorSpaceProfilesSupport(CameraManager manager) {
        // Diese Capability ist erst ab API 34 (Android 14) verfügbar
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            System.out.println("❌ Gerät unterstützt API 34+ nicht.");
            return false;
        }

        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(PhotonCamera.getSettings().mCameraID);

            // Überprüfen, ob die Kamera die Capability unterstützt
            int[] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);

            boolean supportsColorSpace = false;
            if (capabilities != null) {
                for (int capability : capabilities) {
                    if (capability == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_COLOR_SPACE_PROFILES) {
                        supportsColorSpace = true;
                        break;
                    }
                }
            }

            if (supportsColorSpace) {
                System.out.println("✅ Kamera " + PhotonCamera.getSettings().mCameraID + " unterstützt COLOR_SPACE_PROFILES.");
            } else {
                System.out.println("❌ Kamera " + PhotonCamera.getSettings().mCameraID + " unterstützt COLOR_SPACE_PROFILES NICHT.");
            }
            return supportsColorSpace;

        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }
    }

    // QualityDoesMatter - for later to have more control of the encoding parameters like color space and transfer characteristics
    public Size getMaxSensorResolution(CameraManager manager, String cameraId) {
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            if (map == null) {
                return null;
            }
            Size[] jpegSizes = map.getOutputSizes(ImageFormat.JPEG);
            if (jpegSizes == null || jpegSizes.length == 0) {
                jpegSizes = map.getOutputSizes(ImageFormat.RAW_SENSOR);
            }
            if (jpegSizes == null || jpegSizes.length == 0) {
                return null;
            }
            return Collections.max(Arrays.asList(jpegSizes),
                    (size1, size2) -> Long.signum((long) size1.getWidth() * size1.getHeight() -
                            (long) size2.getWidth() * size2.getHeight()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private MediaFormat createAudioFormat() {
        String mimeAud = MediaFormat.MIMETYPE_AUDIO_AAC;
        // create MediaFormat to fill out with audio parameters
        MediaFormat format = MediaFormat.createAudioFormat(mimeAud, PhotonCamera.getSettings().audioSps, PhotonCamera.getSettings().audioChannels);
        format.setInteger(MediaFormat.KEY_BIT_RATE, PhotonCamera.getSettings().audioBitrate * 1024);

        return format;
    }

    private MediaFormat createVideoFormat() {
        // resolution related
        int vidHeight = PhotonCamera.getSettings().videoHeight;
        int vidWidth = 2 * 1920;

        if (PhotonCamera.getSettings().videoHeight == 4 * 1080) {
            vidWidth = 4 * 1920;
        } else if (PhotonCamera.getSettings().videoHeight == 2 * 1080) {
            vidWidth = 2 * 1920;
        } else if (PhotonCamera.getSettings().videoHeight == 1080) {
            vidWidth = 1920;
        }
        else if (PhotonCamera.getSettings().videoHeight == 9999) {
            Size maxRes = getMaxSensorResolution(mCameraManager, PhotonCamera.getSettings().mCameraID);
            vidWidth = maxRes.getWidth();
            vidHeight = maxRes.getHeight();
        }
        else if (PhotonCamera.getSettings().videoHeight == 8888) {
            vidWidth = 6016;
            vidHeight = 4512;
        }
        else if (PhotonCamera.getSettings().videoHeight == 7777) {
            vidWidth = 7680;
            vidHeight = 5760;
        }
        else {
            vidWidth = 1280;
        }

        // compression related
        String mimeVid = MediaFormat.MIMETYPE_VIDEO_AVC;
        if (PhotonCamera.getSettings().videoCodec.equals("HEVC") || PhotonCamera.getSettings().videoCodec.equals("H265")) {
            mimeVid = MediaFormat.MIMETYPE_VIDEO_HEVC;
        } else if (PhotonCamera.getSettings().videoCodec.equals("VP9")) {
            mimeVid = MediaFormat.MIMETYPE_VIDEO_VP9;
        } else if ((PhotonCamera.getSettings().videoCodec.equals("DOLBY_VISION")) || (PhotonCamera.getSettings().videoCodec.equals("DOLBY"))) {
            mimeVid = MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION;
        }

        // create MediaFormat to fill out with video parameters
        MediaFormat format = MediaFormat.createVideoFormat(mimeVid, vidWidth, vidHeight);

        format.setString("camera_application_name", "com.particlesdevs.photonvidcam");
        format.setInteger("camera_module_id", Integer.valueOf(PhotonCamera.getSettings().mCameraID));
        format.setString("eis_enabled", PhotonCamera.getSettings().eisPhoto ? "true" : "false");
        format.setInteger("noise_processing", PhotonCamera.getSettings().noiseReduction);
        format.setInteger("edge_processing", PhotonCamera.getSettings().edgeProcessing);
        format.setString("eis_enabled", PhotonCamera.getSettings().eisPhoto ? "true" : "false");

        if (PhotonCamera.getSettings().videoCodec.equals("HEVC") || PhotonCamera.getSettings().videoCodec.equals("H265")) {
            if (PhotonCamera.getSettings().video10bit && PhotonCamera.getSettings().videoHDR) {
                format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10);
            }
            else if (PhotonCamera.getSettings().video10bit && !PhotonCamera.getSettings().videoHDR) {
                format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10);
            }
            format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel52);
        }
        else if (PhotonCamera.getSettings().videoCodec.equals("VP9")) {

        }
        else if ((PhotonCamera.getSettings().videoCodec.equals("DOLBY_VISION")) || (PhotonCamera.getSettings().videoCodec.equals("DOLBY"))) {
            format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheSt);
            format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.DolbyVisionLevelUhd60);
        }
        else if (PhotonCamera.getSettings().videoCodec.equals("AVC") || PhotonCamera.getSettings().videoCodec.equals("H264")) {
            format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
            format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel4);
        }

        final int BUFFER_SIZE_HINT = vidWidth * vidHeight * 3 / 2;
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, BUFFER_SIZE_HINT * 3);

        format.setFloat(MediaFormat.KEY_FRAME_RATE, PhotonCamera.getSettings().videoFramrate);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, PhotonCamera.getSettings().videoBitrate * 1024 * 1024);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, PhotonCamera.getSpecific().specificSetting.newRecKeyFrameIntervall);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, PhotonCamera.getSettings().videoFramrate);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (PhotonCamera.getSettings().video10bit && PhotonCamera.getSettings().videoHDR) {
                format.setFeatureEnabled("hdr-editing", true);
                format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT2020);
                format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_HLG);
            }
            else {
                format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT709);
                format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_SDR_VIDEO);
            }
        }
        else {
            format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT709);
            format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_SDR_VIDEO);
        }

        if (PhotonCamera.getSpecific().specificSetting.newRecColorRange.equals("full")) {
            format.setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_FULL);
        }
        else {
            format.setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_LIMITED);
        }

        CameraCharacteristics camChar = mCameraCharacteristicsMap.get(PhotonCamera.getSettings().mCameraID);
        boolean facingFront = camChar.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) {
            switch (videoRotation) {
                case 0:
                    format.setInteger(MediaFormat.KEY_ROTATION, 270);
                    break;
                case 90:
                    format.setInteger(MediaFormat.KEY_ROTATION, 180);
                    break;
                case 180:
                    format.setInteger(MediaFormat.KEY_ROTATION, 90);
                    break;
                case -90:
                    format.setInteger(MediaFormat.KEY_ROTATION, 0);
                    break;
            }
        }
        else {
            switch (videoRotation) {
                case 0:
                    format.setInteger(MediaFormat.KEY_ROTATION, 90);
                    break;
                case 90:
                    format.setInteger(MediaFormat.KEY_ROTATION, 0);
                    break;
                case 180:
                    format.setInteger(MediaFormat.KEY_ROTATION, 270);
                    break;
                case -90:
                    format.setInteger(MediaFormat.KEY_ROTATION, 180);
                    break;
            }
        }

        return format;
    }

    private MediaCodec createAudioCodec(MediaFormat audioFormat) {
        MediaCodec audioEncoder = null;

        /*String mimeAud = MediaFormat.MIMETYPE_AUDIO_AAC;
        try {
            audioEncoder = MediaCodec.createEncoderByType(mimeAud);
        }
        catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        audioEncoder.configure(audioFormat);

        int bufferSize = AudioRecord.getMinBufferSize(PhotonCamera.getSettings().audioSps, PhotonCamera.getSettings().audioChannels, AudioFormat.ENCODING_PCM_16BIT);

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, PhotonCamera.getSettings().audioSps, PhotonCamera.getSettings().audioChannels, AudioFormat.ENCODING_PCM_16BIT, bufferSize * 4 );*/

        return audioEncoder;
    }

    private MediaCodec createVideoCodec(MediaFormat videoFormat) {
        String mimeVid = MediaFormat.MIMETYPE_VIDEO_AVC;
        if (PhotonCamera.getSettings().videoCodec.equals("HEVC") || PhotonCamera.getSettings().videoCodec.equals("H265"))
            mimeVid = MediaFormat.MIMETYPE_VIDEO_HEVC;
        else if (PhotonCamera.getSettings().videoCodec.equals("VP9"))
            mimeVid = MediaFormat.MIMETYPE_VIDEO_VP9;
        else if ((PhotonCamera.getSettings().videoCodec.equals("DOLBY_VISION")) || (PhotonCamera.getSettings().videoCodec.equals("DOLBY")))
            mimeVid = MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION;

        MediaCodec videoEncoder = null;
        try {
            videoEncoder = MediaCodec.createEncoderByType(mimeVid);
        }
        catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        return videoEncoder;
    }

    private MediaMuxer createMediaMuxer() {
        MediaMuxer mediaMuxer = null;
        createRecordingFile();
        try {
            mediaMuxer = new MediaMuxer(vid.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        }
        catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return mediaMuxer;
    }

    private void setUpMediaRecorderNew() {
        if (mEncoderData == null) {
            mEncoderData = new RecordingUtils.EncoderData();
        }

        mVideoFormat = createVideoFormat();
        //mAudioFormat = createAudioFormat();
        mVideoCodec = createVideoCodec(mVideoFormat);
        //mAudioCodec = createAudioCodec(mAudioFormat);
        mMediaMuxer = createMediaMuxer();

        mVideoEncoderCallback = new RecordingUtils.VideoEncoderCallback(mMediaMuxer, mEncoderData);
        //mAudioEncoderCallback = new AudioEncoderCallback(mMediaMuxer, mEncoderData);
        mMuxerThread = new RecordingUtils.MuxerThread(mMediaMuxer);
        mVideoCodec.setCallback(mVideoEncoderCallback);
        //mAudioCodec.setCallback(mAudioEncoderCallback);
        mVideoEncoderCallback.setMuxerThread(mMuxerThread);
        //mAudioEncoderCallback.setMuxerThread(mMuxerThread);
        mVideoCodec.configure(mVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        //mAudioCodec.configure(mAudioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodecSurface = mVideoCodec.createInputSurface();
        mVideoCodec.start();
    }

    private void releaseMediaRecorderNew() {
        if (mMediaMuxer != null)
        {
            if (mVideoEncoderCallback != null) {
                if (mVideoEncoderCallback.mMuxerStarted) {
                    mMediaMuxer.stop();
                }
            }
            mMediaMuxer.release();
            mMediaMuxer = null;
            mVideoEncoderCallback = null;
        }
        if (mVideoCodec != null)
        {
            mVideoCodec.stop();
            mVideoCodec.release();
            mVideoCodec = null;
        }
        if (mAudioCodec != null)
        {
            mAudioCodec.stop();
            mAudioCodec.release();
            mAudioCodec = null;
        }
        if (mVideoFormat != null)
        {
            mVideoFormat = null;
        }
        if (mAudioFormat != null)
        {
            mAudioFormat = null;
        }
        if (mEncoderData != null) {
            mEncoderData.mVideoTrackIndex = -1;
            mEncoderData.mAudioTrackIndex = -1;
        }
    }

    private void setUpMediaRecorder() {
        CamcorderProfile profile;
        mMediaRecorder.reset();
        if (PhotonCamera.getSettings().audioCodec != 0) {
            mMediaRecorder.setAudioSource(PhotonCamera.getSettings().audioProcessing);
        }
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        // codec
        if (PhotonCamera.getSettings().videoCodec.equals("HEVC") || PhotonCamera.getSettings().videoCodec.equals("H265"))
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.HEVC);
        else if (PhotonCamera.getSettings().videoCodec.equals("VP9"))
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.VP9);
        else if ((PhotonCamera.getSettings().videoCodec.equals("DOLBY_VISION")) ||
                (PhotonCamera.getSettings().videoCodec.equals("DOLBY")))
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DOLBY_VISION);
        else
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (PhotonCamera.getSettings().video10bit) {
                switch (PhotonCamera.getSettings().videoCodec) {
                    case "DOLBY_VISION":
                    case "DOLBY":
                        mMediaRecorder.setVideoEncodingProfileLevel(MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheSt, MediaCodecInfo.CodecProfileLevel.DolbyVisionLevelUhd60);
                        break;
                    case "HEVC":
                    case "H265":
                        if (PhotonCamera.getSettings().videoHDR)
                            mMediaRecorder.setVideoEncodingProfileLevel(MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus, MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel52);
                        else
                            mMediaRecorder.setVideoEncodingProfileLevel(MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10, MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel52);
                        break;
                }
            }
            else {
                switch (PhotonCamera.getSettings().videoCodec) {
                    case "DOLBY_VISION":
                    case "DOLBY":
                        mMediaRecorder.setVideoEncodingProfileLevel(MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheSt, MediaCodecInfo.CodecProfileLevel.DolbyVisionLevelUhd60);
                        break;
                    case "HEVC":
                    case "H265":
                        mMediaRecorder.setVideoEncodingProfileLevel(MediaCodecInfo.CodecProfileLevel.HEVCProfileMain, MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel51);
                        break;
                }
            }
        }

        // resolution
        if (PhotonCamera.getSettings().videoHeight == 4 * 1080)
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_8KUHD);
        else if (PhotonCamera.getSettings().videoHeight == 2 * 1080)
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_2160P);
        else if (PhotonCamera.getSettings().videoHeight == 1080)
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
        else
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);

        if ((PhotonCamera.getSettings().videoFramrate == 24.0f) && PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            mMediaRecorder.setVideoFrameRate(24);
            mMediaRecorder.setCaptureRate(24.0d);
        }
        else if ((PhotonCamera.getSettings().videoFramrate == 48.0f) && PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            mMediaRecorder.setVideoFrameRate(48);
            mMediaRecorder.setCaptureRate(48.0d);
        }
        else if ((PhotonCamera.getSettings().videoFramrate == 50.0f) && PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            mMediaRecorder.setVideoFrameRate(50);
            mMediaRecorder.setCaptureRate(50.0d);
        }
        else {
            mMediaRecorder.setVideoFrameRate((int)PhotonCamera.getSettings().videoFramrate);
            mMediaRecorder.setCaptureRate((double)PhotonCamera.getSettings().videoFramrate);
        }

        if (PhotonCamera.getSettings().videoHeight == 9999) {
            Size maxRes = getMaxSensorResolution(mCameraManager, PhotonCamera.getSettings().mCameraID);
            mMediaRecorder.setVideoSize(maxRes.getWidth(), maxRes.getHeight());
        }
        else if (PhotonCamera.getSettings().videoHeight == 8888) {
            mMediaRecorder.setVideoSize(6016, 4512);
        }
        else if (PhotonCamera.getSettings().videoHeight == 7777) {
            mMediaRecorder.setVideoSize(7680, 5760);
        }
        else {
            mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        }

        mMediaRecorder.setVideoEncodingBitRate(PhotonCamera.getSettings().videoBitrate * 1024 * 1024);

        // audio
        if (PhotonCamera.getSettings().audioCodec != 0) {
            mMediaRecorder.setAudioEncoder(PhotonCamera.getSettings().audioCodec);
            mMediaRecorder.setAudioEncodingBitRate(PhotonCamera.getSettings().audioBitrate * 1024);
            mMediaRecorder.setAudioSamplingRate(PhotonCamera.getSettings().audioSps);
            mMediaRecorder.setAudioChannels(PhotonCamera.getSettings().audioChannels);
        }

        mMediaRecorder.setOnInfoListener(this);
        CameraCharacteristics camChar = mCameraCharacteristicsMap.get(PhotonCamera.getSettings().mCameraID);
        boolean facingFront = camChar.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) {
            switch (videoRotation) {
                case 0:
                    mMediaRecorder.setOrientationHint(270);
                    break;
                case 90:
                    mMediaRecorder.setOrientationHint(180);
                    break;
                case 180:
                    mMediaRecorder.setOrientationHint(90);
                    break;
                case -90:
                    mMediaRecorder.setOrientationHint(0);
                    break;
            }
        }
        else {
            switch (videoRotation) {
                case 0:
                    mMediaRecorder.setOrientationHint(90);
                    break;
                case 90:
                    mMediaRecorder.setOrientationHint(0);
                    break;
                case 180:
                    mMediaRecorder.setOrientationHint(270);
                    break;
                case -90:
                    mMediaRecorder.setOrientationHint(180);
                    break;
            }
        }

        createRecordingFile();
        mMediaRecorder.setOutputFile(vid.getAbsolutePath());
        try {
            mMediaRecorder.prepare();
            Log.d(TAG, "video record start");

        } catch (Exception e) {
            mIsRecordingVideo = false;
            Log.d(TAG, "video record failed");
            Toast.makeText(activity.getApplicationContext(), "Failed to start recording", Toast.LENGTH_SHORT).show();
            if (vid != null) {
                if (vid.exists()) {
                    if (vid.delete()) {
                        Toast.makeText(activity.getApplicationContext(), "Video file has been removed", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            mMediaRecorder.reset();
            if (vid != null) {
                cameraEventsListener.onRequestTriggerMediaScanner(Uri.fromFile(vid));
            }
            createCameraPreviewSession(false);
        }
    }

    private void createRecordingFile() {
        Date currentDate = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        String dateText = dateFormat.format(currentDate);
        File dir = new File(Environment.getExternalStorageDirectory() + "//DCIM//Camera//");

        String addOptions = "";
        if (PhotonCamera.getSettings().zoom2X) {
            addOptions += "_2x";
        }
        if ((PhotonCamera.getSettings().noiseProcessing != 0) || (PhotonCamera.getSettings().edgeProcessing != 0))
        {
            if (PhotonCamera.getSettings().noiseProcessing != 0) {
                addOptions += "_N";
            }
            if (PhotonCamera.getSettings().edgeProcessing != 0) {
                addOptions += "_E";
            }
        }

        if (!PhotonCamera.getSpecific().specificSetting.recPrefix.isEmpty()) {
            vid = new File(dir.getAbsolutePath(), PhotonCamera.getSpecific().specificSetting.recPrefix + dateText + "_ID" + PhotonCamera.getSettings().mCameraID.toString() + addOptions + ".mp4");
        }
        else
        {
            vid = new File(dir.getAbsolutePath(), "PVC_" + dateText + "_ID" + PhotonCamera.getSettings().mCameraID.toString() + addOptions + ".mp4");
        }
        try {
            vid.createNewFile();
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private void stopRecordingVideo() {
        mIsRecordingVideo = false;

        if (PhotonCamera.getSpecific().specificSetting.useNewRecordingPipeline) {
            releaseMediaRecorderNew();
        }
        else {
            try {
                mMediaRecorder.stop();
            } catch (Exception stopFailure) {
                Log.d(TAG, "Failed to stop recording " + Log.getStackTraceString(stopFailure));
                Toast.makeText(activity.getApplicationContext(), "Failed to stop recording", Toast.LENGTH_SHORT).show();
                if (vid != null) {
                    if (vid.exists()) {
                        if (vid.delete()) {
                            Toast.makeText(activity.getApplicationContext(), "Video file has been removed", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
            mMediaRecorder.reset();
        }
        if (vid != null) {
            cameraEventsListener.onRequestTriggerMediaScanner(Uri.fromFile(vid));
        }
        createCameraPreviewSession(false);
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            Log.v(TAG, "Maximum Duration Reached, Call stopRecordingVideo()");
            stopRecordingVideo();
        }
    }

    private void mul(Rect in, double k) {
        in.bottom *= k;
        in.left *= k;
        in.right *= k;
        in.top *= k;
    }

    @TestOnly
    private static void mulForTest(Rect in, double k) {
        in.bottom *= k;
        in.left *= k;
        in.right *= k;
        in.top *= k;
    }

    @Override
    protected void finalize() throws Throwable {
        activity = null;
        cameraEventsListener = null;
        mCameraManager = null;
        mTextureView = null;
        super.finalize();
    }
    public void resumeCamera() {
        setPreviewFormat();

        processExecutor.execute(() -> {
            if (mTextureView == null)
                mTextureView = new GLPreview(activity);
            if (mTextureView.isAvailable()) {
                Log.d(TAG,"ID:"+mCameraCharacteristicsMap.get(physicalID));
                Size optimal = getPreviewOutputSize(mTextureView.getDisplay(), mCameraCharacteristicsMap.get(physicalID), PhotonCamera.getSettings().selectedMode);
                openCamera(optimal.getWidth(), optimal.getHeight());
            } else {
                mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
            }
        });
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    public static class CameraProperties {
        private final Float minFocal = mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        private final Float maxFocal = mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE);
        public Range<Float> focusRange = (!(minFocal == null || maxFocal == null || minFocal == 0.0f)) ? new Range<>(Math.min(minFocal, maxFocal), Math.max(minFocal, maxFocal)) : null;
        public Range<Integer> isoRange = new Range<>(IsoExpoSelector.getISOLOWExt(), IsoExpoSelector.getISOHIGHExt());
        public Range<Long> expRange = new Range<>(IsoExpoSelector.getEXPLOW(), IsoExpoSelector.getEXPHIGH());
        private final float evStep = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP).floatValue();
        public Range<Float> evRange = new Range<>((mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE).getLower() * evStep),
                (mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE).getUpper() * evStep));

        public CameraProperties() {
            logIt();
        }

        private void logIt() {
            String lens = PhotonCamera.getSettings().mCameraID;
            Log.d(TAG, "focusRange(" + lens + ") : " + (focusRange == null ? "Fixed [" + maxFocal + "]" : focusRange.toString()));
            Log.d(TAG, "isoRange(" + lens + ") : " + isoRange.toString());
            Log.d(TAG, "expRange(" + lens + ") : " + expRange.toString());
            Log.d(TAG, "evCompRange(" + lens + ") : " + evRange.toString());
        }
    }
}