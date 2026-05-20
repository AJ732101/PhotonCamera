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
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.DataSpace;
import android.hardware.Sensor;
import android.hardware.SensorManager;
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
import android.hardware.camera2.params.BlackLevelPattern;
import android.hardware.camera2.params.ColorSpaceTransform;
import android.hardware.camera2.params.DynamicRangeProfiles;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.RggbChannelVector;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.media.MediaCodecList;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaCodecInfo;
import android.media.AudioRecord;
import android.media.MicrophoneDirection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.particlesdevs.photoncamera.app.ContextProvider;
import com.particlesdevs.photoncamera.processing.ImagePath;
import com.particlesdevs.photoncamera.ui.camera.data.CameraLensData;
import com.particlesdevs.photoncamera.ui.settings.SettingsActivity;
import com.particlesdevs.photoncamera.util.FileManager;
import com.particlesdevs.photoncamera.util.Log;
import android.util.Range;
import android.util.Rational;
import android.util.Size;
import android.util.SizeF;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;
import android.graphics.ColorSpace;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
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
import android.hardware.HardwareBuffer;
//import android.media.MediaFormat.ColorSpace;
import android.hardware.camera2.params.TonemapCurve;
import com.particlesdevs.photoncamera.processing.CurvePresets;

import org.chickenhook.restrictionbypass.RestrictionBypass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import android.hardware.camera2.params.InputConfiguration;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_OFF;
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
import static androidx.core.content.ContextCompat.getSystemService;

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
    public static int maxImageReaderImages = 3;
    public static CaptureResult mCaptureResult;
    public static CaptureRequest mCaptureRequest;

    public static CaptureResult mPreviewCaptureResult;
    public static CaptureRequest mPreviewCaptureRequest;
    public static int mPreviewTargetFormat = ImageFormat.JPEG;
    public static float mDigitalZoom = 1.0f;
    public boolean isDualSession = false;
    public boolean mIsCaptureInProgress = false;
    private static int mTargetFormat = ImageFormat.RAW_SENSOR;
    public boolean mFormatsDetectionDone = false;
    public boolean mIsViewFinderMagnified = false;
    public boolean mIsFunctionOneOn = false;
    public boolean mIsFunctionTwoOn = false;
    private com.particlesdevs.photoncamera.ui.camera.views.viewfinder.MainRenderer mMainRenderer = null;
    private final AtomicBoolean mIsProcessingImage = new AtomicBoolean(false);
    private final ParamController paramController;
    public static EncoderInfoUtil mEncoderInfo = new EncoderInfoUtil();
    public TouchFocus mTouchFocus;
    public String mSocVendor = "";
    private int mVidWidth = 1280;
    private int mVidHeight = 720;

    public final boolean mFlashEnabled = false;
    public CameraEventsListener cameraEventsListener;
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
    public String mLastCaptureResult = "";
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

    public Bundle mMetaData = null;

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
            Map<String, CameraLensData> lensDataMap = mCameraManager2.getCameraLensDataMap();
            if (lensDataMap != null) {
                CameraLensData camLensData = lensDataMap.get(PhotonCamera.getSettings().mCameraID);
                if (camLensData != null) {
                    PhotonCamera.getParameters().current35mmFocalLength = (int) Math.ceil(camLensData.getCamera35mmFocalLength());
                }
            }
            if (isSingleShotJpegOrAvifOrHeic()) {
                if (mMetaData == null) {
                    mMetaData = new Bundle();
                }
                if (mCaptureResult != null) {
                    Integer iso = mCaptureResult.get(CaptureResult.SENSOR_SENSITIVITY);
                    if (iso != null) {
                        mMetaData.putInt("iso", iso);
                    }

                    Long exposureTime = mCaptureResult.get(CaptureResult.SENSOR_EXPOSURE_TIME);
                    String strExposureTime = "";
                    if (exposureTime != null && exposureTime > 0) {
                        if (exposureTime >= 1_000_000_000L) {
                            double seconds = exposureTime / 1_000_000_000.0;
                            strExposureTime = String.format(Locale.getDefault(), "%.1fs", seconds);
                        } else {
                            long divisor = (long) (1_000_000_000.0 / exposureTime);
                            strExposureTime = "1/" + divisor;
                        }
                    }

                    if (exposureTime != null) {
                        mMetaData.putLong("exposureTime", exposureTime);
                        mMetaData.putString("exposureTimeStr", strExposureTime);
                    }

                    Float focalLength = mCaptureResult.get(CaptureResult.LENS_FOCAL_LENGTH);
                    if (focalLength != null) {
                        mMetaData.putFloat("focalLength", focalLength);
                    }

                    Float aperture = mCaptureResult.get(CaptureResult.LENS_APERTURE);
                    if (aperture != null) {
                        mMetaData.putFloat("aperture", aperture);
                    }

                    if (PhotonCamera.getParameters().current35mmFocalLength != 0) {
                        mMetaData.putInt("focal35mm", PhotonCamera.getParameters().current35mmFocalLength);
                    }

                    mMetaData.putString("physCamID", physicalID);
                    if (physicalID != logicalID) {
                        mMetaData.putString("logiCamID", logicalID);
                    }

                    if (PhotonCamera.getSettings().gpsLocation && (PhotonCamera.gpsLocation != null)) {
                        mMetaData.putString("latitude", String.valueOf(PhotonCamera.gpsLocation.getLatitude()));
                        mMetaData.putString("longitude", String.valueOf(PhotonCamera.gpsLocation.getLongitude()));
                        if (PhotonCamera.gpsLocation.hasAltitude()) {
                            mMetaData.putString("altitude", String.valueOf(PhotonCamera.gpsLocation.getAltitude()));
                        }
                    }

                    // all the capture meta data
                    mMetaData.putString("completeCaptureResult", mLastCaptureResult);
                }

                if ((!isSingleShotJpegOrAvifOrHeic() || isSingleShotSwEncoder()) && !PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO) && !PhotonCamera.getSettings().selectedMode.equals(CameraMode.RAWVIDEO)) {
                    processImageWithLutAndSave(reader);
                } else {
                    mImageSaver.directSaveImage(reader, getOrientation(), PhotonCamera.getSettings().previewFormat, PhotonCamera.getSettings().singleFrameQuality, mMetaData, mMainRenderer);
                }
                return;
            }
            if (onUnlimited && !unlimitedStarted) {
                return;
            }
            if(PhotonCamera.getSettings().frameCount != 1) {
                mImageSaver.initProcess(reader);
            }
            else {
                mBackgroundHandler.post(() -> mImageSaver.initProcess(reader));
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
    public static int wasLogged = 0;
    /**
     * An {@link AutoFitPreviewView} for camera preview.
     */
    private GLPreview mTextureView = null;
    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession = null;
    private CameraConstrainedHighSpeedCaptureSession mHighSpeedCaptureSession = null;
    /**
     * MediaRecorder
     */
    private MediaRecorder mMediaRecorder = null;
    boolean mIsHighSpeedSupported = false;
    private MediaRecorder mAudioRecorder = null;
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
    Surface mVideoRecordingSurface = null;
    Surface mTextureSurface = null;
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
            processHistogram(result);
            if (mIsRecordingVideo) {
                Long timestamp = result.get(CaptureResult.SENSOR_TIMESTAMP);
                if (timestamp != null && mMainRenderer != null) {
                    mMainRenderer.setFrameTimestamp(timestamp);
                }
            }
            if (mIsRecordingVideo && // Use your actual state variable for recording
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                    PhotonCamera.getSettings().hdrMode == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus) {

                mCaptureResult = result;

                // 1. Get the dynamic HDR10+ metadata using your reflection helper.
                byte[] hdr10PlusData = CameraReflectionApi.getHdr10PlusOem(result);

                if (hdr10PlusData != null) {
                    // 2. Create a Bundle to hold the parameter.
                    Bundle params = new Bundle();
                    params.putByteArray(MediaCodec.PARAMETER_KEY_HDR10_PLUS_INFO, hdr10PlusData);

                    // 3. Apply the parameter to the video codec.
                    // Make sure your mVideoCodec instance is accessible here.
                    if (mVideoCodec != null) {
                        mVideoCodec.setParameters(params);
                    }
                }
            }

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

            if (mIsRecordingVideo || isSingleShotJpegOrAvifOrHeic()) {
                return;
            }

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
                var display = mTextureView.getDisplay();
                var characteristic = mCameraCharacteristicsMap.get(physicalID);

                Size optimal = getPreviewOutputSize(display, characteristic, PhotonCamera.getSettings().selectedMode);
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

    public boolean checkHdrSupport(CameraCharacteristics characteristics)
    {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                if ((characteristics == null) || (characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES) == null)) {
                    Log.d(TAG, "DynamicRangeProfiles not available on this camera sensor -> try HLG fallback.");
                    PhotonCamera.mHlgIsSupported = true;
                    return false;
                }
                java.util.Set<Long> supportedProfiles = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES).getSupportedProfiles();
                if (supportedProfiles == null) {
                    Log.d(TAG, "DynamicRangeProfiles not available on this camera sensor.");
                    return false;
                }
                Log.d(TAG, "Supported profiles: " + supportedProfiles.toString());

                String dynRangeProf = "Supported dynamic range profile: ";
                Log.d(TAG, "Supported dynamic range profile: HDR10+");
                if (supportedProfiles.contains(DynamicRangeProfiles.HDR10_PLUS)) {
                    dynRangeProf += "HDR10+";
                    PhotonCamera.mHdrTenPlusIsSupported = true;
                }
                if (supportedProfiles.contains(DynamicRangeProfiles.HDR10)) {
                    dynRangeProf += " - HDR10";
                    PhotonCamera.mHdrTenIsSupported = true;
                }
                if (supportedProfiles.contains(DynamicRangeProfiles.HLG10)) {
                    dynRangeProf += " - HLG10";
                    PhotonCamera.mHlgIsSupported = true;
                }
                Log.d(TAG, dynRangeProf);
                return true;
            } else {
                Log.d(TAG, "DynamicRangeProfiles not available for this Android version.");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking HDR support: " + e.getMessage());
            return false;
        }
    }

    public boolean isSingleShotSwEncoder() {
        if ((PhotonCamera.getSettings().frameCount == 1) &&
                (PhotonCamera.getSettings().previewFormat == 999999992) &&
                (PhotonCamera.getSettings().rawSaver != 2) &&
                !PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO) &&
                !PhotonCamera.getSettings().selectedMode.equals(CameraMode.UNLIMITED) &&
                !PhotonCamera.getSettings().selectedMode.equals(CameraMode.RAWVIDEO)) {
            return true;
        }
        return false;
    }

    public boolean isSingleShotJpegOrAvifOrHeic() {
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
                (PhotonCamera.getSettings().rawSaver != 2) &&
                !PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO) &&
                !PhotonCamera.getSettings().selectedMode.equals(CameraMode.UNLIMITED) &&
                !PhotonCamera.getSettings().selectedMode.equals(CameraMode.RAWVIDEO)) {
            return true;
        }
        return false;
    }

    public void setPreviewFormat() {
        mPreviewTargetFormat = PhotonCamera.getSettings().realPreviewFormat;

        if (PhotonCamera.getSettings().previewFormat == ImageFormat.YCBCR_P010) {
            mPreviewTargetFormat = ImageFormat.YCBCR_P010;
        }
    }

    public void setTargetFormat() {
        if (PhotonCamera.getSettings().rawFormat == 2) {
            mTargetFormat = PhotonCamera.getSettings().rawFormat;
            return;
        }
        if (isSingleShotJpegOrAvifOrHeic() && !PhotonCamera.getSettings().selectedMode.equals(CameraMode.UNLIMITED)) {
            var targetFromUi = PhotonCamera.getSettings().previewFormat;
            if ((targetFromUi == PhotonCamera.userFormatAvifSw) || (targetFromUi == PhotonCamera.userFormatHeifSw) ||
                    (targetFromUi == PhotonCamera.userFormatJpegLutSw) || (targetFromUi == PhotonCamera.userFormatPngSw) ||
                    (targetFromUi == PhotonCamera.userFormatYuvRaw) || (targetFromUi == PhotonCamera.userFormatWebpLossySw) || (targetFromUi == PhotonCamera.userFormatWebpLosslessSw)) {
                mTargetFormat = PhotonCamera.getSettings().realPreviewFormat;
            }
            else {
                mTargetFormat = PhotonCamera.getSettings().previewFormat;
            }
            return;
        }

        mTargetFormat = PhotonCamera.getSettings().rawFormat;
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
            }
            if (null != mImageReaderRaw) {
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
            if (mCaptureSession == null || mPreviewRequestBuilder == null) {
                return;
            }
            mPreviewInputRequest = mPreviewRequestBuilder.build();
            if (mPreviewInputRequest != null) {
                mCaptureSession.setRepeatingRequest(mPreviewInputRequest, mCaptureCallback, mBackgroundHandler);
            } else {
                Log.e(TAG, "mPreviewInputRequest == null");
            }
        } catch (IllegalStateException | IllegalArgumentException | NullPointerException e) {
            Logger.warnShort(TAG, "Cannot rebuildPreviewBuilder()!", e);
        } catch (CameraAccessException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public void rebuildPreviewBuilderOneShot() {
        if (burst) {
            return;
        }
        try {
            Log.d(TAG, "rebuildPreviewBuilderOneShot: " + mCaptureSession + " " + mPreviewRequestBuilder + " " + mCaptureCallback + " " + mBackgroundHandler);
            var captureSession = mCaptureSession;
            var previewRequestBuilder = mPreviewRequestBuilder;
            var captureCallback = mCaptureCallback;
            var backgroundHandler = mBackgroundHandler;
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
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        checkStillImageFormatsSupport(map);
        HashSet<Size> uniqueTargets = new HashSet<>();

        setTargetFormat();

        Size[] targetSizes = map.getOutputSizes(mTargetFormat);
        if(targetSizes != null) {
            uniqueTargets.addAll(Arrays.asList(targetSizes));
        }

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
                        uniqueTargets.add(arraySize);
                    }
                }
            }
            if (!useMaximumResolutionKey) {
                Size[] highResSizes = map.getHighResolutionOutputSizes(mTargetFormat);
                if (highResSizes != null && highResSizes.length > 0) {
                    uniqueTargets.addAll(Arrays.asList(highResSizes));
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
                                    if ((width > 6000) || (height > 6000)) {
                                        //if ((format == mTargetFormat) || (format == mPreviewTargetFormat))
                                        {
                                            /*Log.d(TAG, "Added custom resolution(" + key.getName() + "):" + width + " " + height + " - format=" + format + "/0x" + String.format("%02X", format)
                                                    + " - target format=" + mTargetFormat + "/0x" + String.format("%02X", mTargetFormat)
                                                    + " - preview format=" + mPreviewTargetFormat + "/0x" + String.format("%02X", mPreviewTargetFormat)
                                                    + " - real preview format=" + PhotonCamera.getSettings().realPreviewFormat + "/0x" + String.format("%02X", PhotonCamera.getSettings().realPreviewFormat));*/
                                        }
                                        if (uniqueTargets.add(new Size(width, height))) {
                                            Log.d(TAG, "Added custom resolution(" + key.getName() + "):" + width + " " + height + " - format=" + format + "/0x" + String.format("%02X", format)
                                                    + " - target format=" + mTargetFormat + "/0x" + String.format("%02X", mTargetFormat)
                                                    + " - preview format=" + mPreviewTargetFormat + "/0x" + String.format("%02X", mPreviewTargetFormat)
                                                    + " - real preview format=" + PhotonCamera.getSettings().realPreviewFormat + "/0x" + String.format("%02X", PhotonCamera.getSettings().realPreviewFormat));
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        return new ArrayList<>(uniqueTargets);
    }

    @SuppressLint("MissingPermission")
    public void restartCamera() {
        mSocVendor = getSoCVendor();
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
            }
            if (null != mImageReaderRaw) {
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
        createImageReaders(physicalID);

        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            this.mCameraManager.openCamera(logicalID, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to restart camera.", e);
        }
        //stopBackgroundThread();
        //UpdateCameraCharacteristics(physicalID);
        startBackgroundThread();

        Size optimal = getPreviewOutputSize(mTextureView.getDisplay(), mCameraCharacteristics, CameraFragment.mSelectedMode);

        setUpCameraOutputs(optimal.getWidth(), optimal.getHeight());
        configureTransform(optimal.getWidth(), optimal.getHeight());
    }

    private void createImageReaders(String cameraId) {
        createImageReaderPreview(cameraId);
        if (!PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            createImageReaderRaw();
        }
    }

    private void checkStillImageFormatsSupport(StreamConfigurationMap map) {
        if (mFormatsDetectionDone) {
            return; // one time is enough, device will not gain or loose any formats
        }

        int[] outputFormats = map.getOutputFormats();
        PhotonCamera.mHeicIsSupported = false;
        PhotonCamera.mHeicUltraHdrIsSupported = false;
        PhotonCamera.mJpegRIsSupported = false;
        PhotonCamera.mRaw10IsSupported = false;
        PhotonCamera.mRaw12IsSupported = false;
        PhotonCamera.mRawPrivateIsSupported = false;
        PhotonCamera.mRawSensorIsSupported = false;
        PhotonCamera.mYuv10IsSupported = false;

        for (int format : outputFormats) {
            if (format == ImageFormat.HEIC) {
                PhotonCamera.mHeicIsSupported = true;
            }
            if (format == ImageFormat.HEIC_ULTRAHDR) {
                PhotonCamera.mHeicUltraHdrIsSupported = true;
            }
            if (format == ImageFormat.JPEG_R) {
                PhotonCamera.mJpegRIsSupported = true;
            }
            if (format == ImageFormat.RAW10) {
                PhotonCamera.mRaw10IsSupported = true;
            }
            if (format == ImageFormat.RAW12) {
                PhotonCamera.mRaw12IsSupported = true;
            }
            if (format == ImageFormat.RAW_PRIVATE) {
                PhotonCamera.mRawPrivateIsSupported = true;
            }
            if (format == ImageFormat.RAW_SENSOR) {
                PhotonCamera.mRawSensorIsSupported = true;
            }
            if (format == ImageFormat.YCBCR_P010) {
                PhotonCamera.mYuv10IsSupported = true;
            }
        }

        if (PhotonCamera.mHeicIsSupported) {
            Log.d(TAG, "HEIC is supported");
        }
        else {
            Log.d(TAG, "HEIC is NOT supported");
        }
        if (PhotonCamera.mHeicUltraHdrIsSupported) {
            Log.d(TAG, "HEIC_ULTRAHDR is supported");
        }
        else {
            Log.d(TAG, "HEIC_ULTRAHDR is NOT supported");
        }
        if (PhotonCamera.mJpegRIsSupported) {
            Log.d(TAG, "JPEG_R is supported");
        }
        else {
            Log.d(TAG, "JPEG_R is NOT supported");
        }
        if (PhotonCamera.mRaw10IsSupported) {
            Log.d(TAG, "RAW10 is supported");
        }
        else {
            Log.d(TAG, "RAW10 is NOT supported");
        }
        if (PhotonCamera.mRaw12IsSupported) {
            Log.d(TAG, "RAW12 is supported");
        }
        else {
            Log.d(TAG, "RAW12 is NOT supported");
        }
        if (PhotonCamera.mRawPrivateIsSupported) {
            Log.d(TAG, "RAW_PRIVATE is supported");
        }
        else {
            Log.d(TAG, "RAW_PRIVATE is NOT supported");
        }
        if (PhotonCamera.mRawSensorIsSupported) {
            Log.d(TAG, "RAW_SENSOR is supported");
        }
        else {
            Log.d(TAG, "RAW_SENSOR is NOT supported");
        }
        if (PhotonCamera.mYuv10IsSupported) {
            Log.d(TAG, "YCBCR_P010 is supported");
        }
        else {
            Log.d(TAG, "YCBCR_P010 is NOT supported");
        }

        mFormatsDetectionDone = true;
    }

    private void createImageReaderPreview(String cameraId) {
        mIsCaptureInProgress = false;
        if (((mTargetFormat == mPreviewTargetFormat) && isDualSession) ||
                PhotonCamera.getSettings().selectedMode.equals(CameraMode.UNLIMITED) ||
                PhotonCamera.getSettings().selectedMode.equals(CameraMode.RAWVIDEO)) {
            maxImageReaderImages = Math.min(PhotonCamera.getSettings().frameCount + 3, 30);
        }
        else if (isSingleShotJpegOrAvifOrHeic() && !PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            maxImageReaderImages = 2;
        }

        PhotonCamera.getSpecificSensor().selectSpecifics(Integer.parseInt(cameraId));
        mCameraCharacteristics = this.mCameraCharacteristicsMap.get(cameraId);
        StreamConfigurationMap map = null;
        if (mCameraCharacteristics != null) {
            map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        }
        if (map == null) {
            return;
        }

        checkStillImageFormatsSupport(map);

        ArrayList<Size> allTargets = getAllTargets();
        Size preview = getCameraOutputSize(map.getOutputSizes(mPreviewTargetFormat));
        //Log.d(TAG, "preview ImageReader before check" + preview.getWidth() + "x" + preview.getHeight() + " - orientation: " + mSensorOrientation);
        Size aspect = getAspect(PhotonCamera.getSettings().selectedMode);
        if (preview.getWidth() > preview.getHeight()) {
            preview = new Size(preview.getWidth(), preview.getWidth() * aspect.getWidth() / aspect.getHeight());
        }
        else {
            preview = new Size(preview.getHeight() * aspect.getWidth() / aspect.getHeight(), preview.getHeight());
        }
        //Log.d(TAG, "preview ImageReader after check" + preview.getWidth() + "x" + preview.getHeight() + " - orientation: " + mSensorOrientation);
        target = getCameraOutputSize(allTargets.toArray(new Size[0]), preview);

        if (mImageReaderPreview != null) {
            mImageReaderPreview.close();
            mImageReaderPreview = null;
        }
        mImageReaderPreview = ImageReader.newInstance(preview.getWidth(), preview.getHeight(), mPreviewTargetFormat, maxImageReaderImages);
        mImageReaderPreview.setOnImageAvailableListener(mOnYuvImageAvailableListener, mBackgroundHandler);
        mBufferSize = getPreviewOutputSize(mTextureView.getDisplay(), mCameraCharacteristics, PhotonCamera.getSettings().selectedMode);
    }

    private void createImageReaderRaw() {
        try {
            if (mImageReaderRaw != null) {
                mImageReaderRaw.close();
            }
            applyFormatFallback();
            if (customRawResForCamIdCheck(physicalID)) {
                Size newSize = customRawResForCamId(physicalID);
                mImageReaderRaw = ImageReader.newInstance(newSize.getHeight(), newSize.getWidth(), mTargetFormat, maxImageReaderImages/*, HardwareBuffer.USAGE_SENSOR_DIRECT_DATA*/);
            } else {
                if (target.getHeight() > target.getWidth()) {
                    mImageReaderRaw = ImageReader.newInstance(target.getHeight(), target.getWidth(), mTargetFormat, maxImageReaderImages);
                }
                else {
                    mImageReaderRaw = ImageReader.newInstance(target.getWidth(), target.getHeight(), mTargetFormat, maxImageReaderImages);
                }
                //Log.d(TAG, "create ImageReader " + target.getWidth() + "x" + target.getHeight() + " - orientation: " + mSensorOrientation);
                /*if (PhotonCamera.getSettings().QuadBayer) {
                    mImageReaderRaw = ImageReader.newInstance(target.getHeight(), target.getWidth(), mTargetFormat, maxImagerReaderImages);
                } else {
                    mImageReaderRaw = ImageReader.newInstance(target.getWidth(), target.getHeight(), mTargetFormat, maxImagerReaderImages);
                }*/
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
        if (mImageReaderRaw != null) {
            mImageReaderRaw.setOnImageAvailableListener(mOnRawImageAvailableListener, mBackgroundHandler);
        }
    }

    private void applyFormatFallback() {
        // RAW formats first
        if ((mTargetFormat == ImageFormat.RAW10) && !PhotonCamera.mRaw10IsSupported) {
            mTargetFormat = ImageFormat.RAW_SENSOR;
            Log.w(TAG, "Requested RAW10 but not supported -> fallback to RAW_SENSOR");
        }
        if ((mTargetFormat == ImageFormat.RAW12) && !PhotonCamera.mRaw12IsSupported) {
            mTargetFormat = ImageFormat.RAW_SENSOR;
            Log.w(TAG, "Requested RAW12 but not supported -> fallback to RAW_SENSOR");
        }
        // Bitmap formats
        if ((mTargetFormat == ImageFormat.HEIC) && !PhotonCamera.mHeicIsSupported) {
            mTargetFormat = ImageFormat.JPEG;
            Log.w(TAG, "Requested HEIC but not supported -> fallback to JPEG");
        }
        if ((mTargetFormat == ImageFormat.HEIC_ULTRAHDR) && !PhotonCamera.mHeicUltraHdrIsSupported) {
            mTargetFormat = ImageFormat.JPEG;
            Log.w(TAG, "Requested HEIC_ULTRAHDR but not supported -> fallback to JPEG");
        }
        if ((mTargetFormat == ImageFormat.JPEG_R) && !PhotonCamera.mJpegRIsSupported) {
            mTargetFormat = ImageFormat.JPEG;
            Log.w(TAG, "Requested JPEG_R but not supported -> fallback to JPEG");
        }

        // YUV formats
        if ((mTargetFormat == ImageFormat.YCBCR_P010) && !PhotonCamera.mYuv10IsSupported) {
            mTargetFormat = ImageFormat.YUV_420_888;
            Log.w(TAG, "Requested YCBCR_P010 but not supported -> fallback to YUV_420_888");
        }
    }

    private Size getAspect(CameraMode targetMode) {
        if (targetMode == CameraMode.VIDEO) {
            if (PhotonCamera.isProVideoLogMovie) {
                return new Size(9, 21);
            }
            else if ((PhotonCamera.getSettings().videoHeight != 9999) && (PhotonCamera.getSettings().videoHeight != 8888) && (PhotonCamera.getSettings().videoHeight != 7777)){
                return new Size(9, 16);
            }
            else {
                return new Size(3, 4);
            }
        }

        //if ((targetMode == CameraMode.RAWVIDEO) || PhotonCamera.getSettings().aspect169) {
        if (PhotonCamera.getSettings().aspect169) {
            return new Size(9, 16);
        }
        else {
            return new Size(3, 4);
        }
    }

    //Size for preview drawing
    private Size getTextureOutputSize(Display display, CameraMode targetMode) {
        Size aspectRatio = getAspect(targetMode);
        Point displayPoint = new Point();
        display.getRealSize(displayPoint);
        int shortSide = Math.min(displayPoint.x, displayPoint.y);
        int longSide = shortSide * aspectRatio.getHeight() / aspectRatio.getWidth();

        return new Size(longSide, shortSide);
    }

    //Size for preview buffer
    private Size getPreviewOutputSize(Display display, CameraCharacteristics characteristics, CameraMode targetMode) {
        Size aspectRatio = getAspect(targetMode);
        Point displayPoint = new Point();
        display.getRealSize(displayPoint);
        int shortSide = Math.min(displayPoint.x, displayPoint.y);
        int longSide = shortSide / aspectRatio.getWidth() * aspectRatio.getHeight();


        // If image format is provided, use it to determine supported sizes; else use target class
        StreamConfigurationMap config = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        Size[] allSizes;
        if (targetMode == CameraMode.VIDEO && PhotonCamera.getSettings().videoFramrate >= 120) {
            allSizes = config.getHighSpeedVideoSizes();
        } else {
            allSizes = config.getOutputSizes(SurfaceTexture.class);
        }

        Size retsize = null;
        if (allSizes != null) {
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
        }
        if (retsize == null && allSizes != null && allSizes.length > 0) {
            retsize = allSizes[0];
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
        if (burst) {
            return;
        }
        startTimerLocked();
        // This is how to tell the camera to lock focus.
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        // Tell #mCaptureCallback to wait for the lock.
        mState = STATE_WAITING_LOCK;
        try {
            CaptureRequest req = mPreviewRequestBuilder.build();
            if (req != null) {
                mCaptureSession.setRepeatingRequest(req, mCaptureCallback, mBackgroundHandler);
            } else {
                Log.e(TAG,"mPreviewRequestBuilder.build() failed.");
            }
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
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
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
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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
                    if (!PhotonCamera.getSettings().QuadBayer) {
                        isDualSession = true;
                    }
                }
                this.mCameraManager.openCamera(logicalID, mStateCallback, mBackgroundHandler);
            } catch (CameraAccessException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
            }
        });
    }
    public void setAdvancedParameters(CaptureRequest.Builder captureBuilder, boolean isPreview) throws CameraAccessException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        // we do this only in video mode or if framecount is 1 or if forced with forceNewSettingsInRegularPhotoMode
        if (!PhotonCamera.getSettings().useNewSettingsGloabal) {
            if (!PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO) && (PhotonCamera.getSettings().frameCount != 1)) {
                return;
            }
        }

        captureBuilder.set(CaptureRequest.JPEG_QUALITY, (byte)PhotonCamera.getSettings().singleFrameQuality);
        //captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION));
        // QualityDoesMatter
        captureBuilder.set(CaptureRequest.STATISTICS_HOT_PIXEL_MAP_MODE, PhotonCamera.getSpecific().specificSetting.statisticsHotPixelMapMode);
        if ((PhotonCamera.getSettings().exposureCompensation2 != 0) && !PhotonCamera.getSettings().contrastCurve.equals("off"))
            captureBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, PhotonCamera.getSettings().exposureCompensation2);
        if (PhotonCamera.getSettings().hotPixelMode != 99)
            captureBuilder.set(CaptureRequest.HOT_PIXEL_MODE, PhotonCamera.getSettings().hotPixelMode);
        if (PhotonCamera.getSettings().colorCorrectionAberrationMode != 99)
            captureBuilder.set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, PhotonCamera.getSettings().colorCorrectionAberrationMode);
        if (PhotonCamera.getSettings().distortionCorrectionMode != 99)
            captureBuilder.set(CaptureRequest.DISTORTION_CORRECTION_MODE, PhotonCamera.getSettings().distortionCorrectionMode);
        if (PhotonCamera.getSettings().shadingMode != 99)
            captureBuilder.set(CaptureRequest.SHADING_MODE, PhotonCamera.getSettings().shadingMode);
        if (PhotonCamera.getSpecific().specificSetting.statisticsLensShadingMapMode != 99)
            captureBuilder.set(CaptureRequest.STATISTICS_LENS_SHADING_MAP_MODE, PhotonCamera.getSpecific().specificSetting.statisticsLensShadingMapMode);
        if (PhotonCamera.getSpecific().specificSetting.statisticsOisDataMode != 99)
            captureBuilder.set(CaptureRequest.STATISTICS_OIS_DATA_MODE, PhotonCamera.getSpecific().specificSetting.statisticsOisDataMode);
        if (!PhotonCamera.getSettings().contrastCurve.equals("off")) {
            setContrastCurve(captureBuilder);
        }
        else if (PhotonCamera.getSpecific().specificSetting.toneMapGamma != 99) {
            captureBuilder.set(CaptureRequest.TONEMAP_MODE, CameraMetadata.TONEMAP_MODE_GAMMA_VALUE);
            captureBuilder.set(CaptureRequest.TONEMAP_GAMMA, 1/PhotonCamera.getSpecific().specificSetting.toneMapGamma);
        }
        else {
            if (PhotonCamera.getSettings().isTonnemappingModeQuality) {
                if (checkToneMappingModes(CameraMetadata.TONEMAP_MODE_HIGH_QUALITY)) {
                    captureBuilder.set(CaptureRequest.TONEMAP_MODE, CameraMetadata.TONEMAP_MODE_HIGH_QUALITY);
                }
            } else {
                if (checkToneMappingModes(CameraMetadata.TONEMAP_MODE_FAST)) {
                    captureBuilder.set(CaptureRequest.TONEMAP_MODE, CameraMetadata.TONEMAP_MODE_FAST);
                }
            }
        }

        // test priority modes
        VendorTagUtils.getSupportedIso(mCameraCharacteristics);
        if (mIsFunctionOneOn) {
            if (PhotonCamera.getSettings().functionOne.equals("ISO Priority") && (PhotonCamera.getSpecific().specificSetting.priorityIsoValue != 0)) {
                setIsoPriorityMode(captureBuilder, PhotonCamera.getSpecific().specificSetting.priorityIsoValue);
            }

            if (PhotonCamera.getSettings().functionOne.equals("Shutter Priority") && (PhotonCamera.getSpecific().specificSetting.priorityShutterSpeed != 0)) {
                long ONE_SECOND_IN_NANOS = 1_000_000_000L;
                long desiredShutterSpeed = ONE_SECOND_IN_NANOS / PhotonCamera.getSpecific().specificSetting.priorityShutterSpeed;
                setShutterPriorityMode(captureBuilder, desiredShutterSpeed);
            }
        }

        if (mIsFunctionTwoOn) {
            if (PhotonCamera.getSettings().functionTwo.equals("ISO Priority") && (PhotonCamera.getSpecific().specificSetting.priorityIsoValue != 0)) {
                setIsoPriorityMode(captureBuilder, PhotonCamera.getSpecific().specificSetting.priorityIsoValue);
            }

            if (PhotonCamera.getSettings().functionTwo.equals("Shutter Priority") && (PhotonCamera.getSpecific().specificSetting.priorityShutterSpeed != 0)) {
                long ONE_SECOND_IN_NANOS = 1_000_000_000L;
                long desiredShutterSpeed = ONE_SECOND_IN_NANOS / PhotonCamera.getSpecific().specificSetting.priorityShutterSpeed;
                setShutterPriorityMode(captureBuilder, desiredShutterSpeed);
            }
        }

        //VendorTagUtils.setToneMappingDarkBoostValue(captureBuilder, -1.0f);

        // check if CaptureRequest.COLOR_CORRECTION_MODE_CCT is supported
        boolean supportsColorTemperature = false;
        try {
            CameraCharacteristics.Key<int[]> key = new CameraCharacteristics.Key<>("android.colorCorrection.availableModes", int[].class);
            int[] availableCorrectionModes = mCameraManager.getCameraCharacteristics("0").get(key);
            Log.d(TAG, "Supported color correction methods:");
            for (int mode : availableCorrectionModes) {
                if (mode == 0) {
                    Log.d(TAG, "   COLOR_CORRECTION_MODE_TRANSFORM_MATRIX");
                }
                if (mode == 1) {
                    Log.d(TAG, "   COLOR_CORRECTION_MODE_FAST");
                }
                if (mode == 2) {
                    Log.d(TAG, "   COLOR_CORRECTION_MODE_HIGH_QUALITY");
                }
                if (mode == 3) {
                    supportsColorTemperature = true;
                    Log.d(TAG, "   COLOR_CORRECTION_MODE_CCT");
                }
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }

        captureBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, FpsRangeDef);

        if (PhotonCamera.getSpecific().specificSetting.colorTemperature > 1000) {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) && supportsColorTemperature) {
                captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
                captureBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_CCT);
                captureBuilder.set(CaptureRequest.COLOR_CORRECTION_COLOR_TEMPERATURE, PhotonCamera.getSpecific().specificSetting.colorTemperature);
                captureBuilder.set(CaptureRequest.COLOR_CORRECTION_COLOR_TINT, (int) PhotonCamera.getSpecific().specificSetting.colorTint);
            } else {
                if (!Build.BRAND.equalsIgnoreCase("vivo")) {
                    try {
                        captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
                        captureBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX);
                        android.hardware.camera2.params.RggbChannelVector gains = kelvinAndTintToGains(PhotonCamera.getSpecific().specificSetting.colorTemperature, PhotonCamera.getSpecific().specificSetting.colorTint);
                        captureBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, gains);
                    } catch (Exception e) {
                        Log.e(TAG, "setCaptureRequestBuilder:" + e);
                    }
                }
            }
        }
    }

    private boolean checkToneMappingModes(int modeToMatch) {
        int[] availableToneMapModes = mCameraCharacteristics.get(CameraCharacteristics.TONEMAP_AVAILABLE_TONE_MAP_MODES);
        boolean isSupported = false;
        if (availableToneMapModes != null) {
            Log.d(TAG, "Supported tone map modes:");
            for (int mode : availableToneMapModes) {
                if (mode == modeToMatch) {
                    isSupported = true;
                }
                switch (mode) {
                    case CameraMetadata.TONEMAP_MODE_CONTRAST_CURVE:
                        Log.d(TAG, "    TONEMAP_MODE_CONTRAST_CURVE");
                        break;
                    case CameraMetadata.TONEMAP_MODE_FAST:
                        Log.d(TAG, "    TONEMAP_MODE_FAST");
                        break;
                    case CameraMetadata.TONEMAP_MODE_HIGH_QUALITY:
                        Log.d(TAG, "    TONEMAP_MODE_HIGH_QUALITY");
                        break;
                    case CameraMetadata.TONEMAP_MODE_GAMMA_VALUE:
                        Log.d(TAG, "    TONEMAP_MODE_GAMMA_VALUE");
                        break;
                    case CameraMetadata.TONEMAP_MODE_PRESET_CURVE:
                        Log.d(TAG, "    TONEMAP_MODE_PRESET_CURVE");
                        break;
                }
            }
        }
        return isSupported;
    }

    private boolean isAePriorityModeSupported(CameraCharacteristics characteristics, int priorityMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            int[] availableModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_PRIORITY_MODES);

            if (availableModes != null) {
                for (int mode : availableModes) {
                    if (mode == priorityMode) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void setIsoPriorityMode(CaptureRequest.Builder reqBuilder, int desiredIso) {
        if (!isAePriorityModeSupported(mCameraCharacteristics, CameraMetadata.CONTROL_AE_PRIORITY_MODE_SENSOR_SENSITIVITY_PRIORITY)) {
            VendorTagUtils.setIsoExpPrioritySelectPriority(reqBuilder, PhotonCamera.getSpecific().specificSetting.priorityMode);
            VendorTagUtils.setUseIsoValues(reqBuilder, desiredIso);
            VendorTagUtils.setIsoExpPriority(reqBuilder, 1_000_000_000L/10);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            reqBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
            reqBuilder.set(CaptureRequest.CONTROL_AE_PRIORITY_MODE, CameraMetadata.CONTROL_AE_PRIORITY_MODE_SENSOR_SENSITIVITY_PRIORITY);
            reqBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, desiredIso);
        }
    }

    private void setShutterPriorityMode(CaptureRequest.Builder reqBuilder, long desiredShutterSpeed) {
        if (!isAePriorityModeSupported(mCameraCharacteristics, CameraMetadata.CONTROL_AE_PRIORITY_MODE_SENSOR_EXPOSURE_TIME_PRIORITY)) {
            VendorTagUtils.setIsoExpPrioritySelectPriority(reqBuilder, 1);
            VendorTagUtils.setIsoExpPriority(reqBuilder, desiredShutterSpeed);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            reqBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
            reqBuilder.set(CaptureRequest.CONTROL_AE_PRIORITY_MODE, CameraMetadata.CONTROL_AE_PRIORITY_MODE_SENSOR_EXPOSURE_TIME_PRIORITY);
            reqBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, desiredShutterSpeed);
        }
    }

    public TonemapCurve loadCustomCurve() {
        File customContrastCurve = new File(FileManager.sPHOTON_TUNING_DIR, "CustomContrastCurve.curve");

        if (!customContrastCurve.exists()) {
            Log.e(TAG, "Custom curve file not found at: " + customContrastCurve.getAbsolutePath());
            return null;
        }

        List<Float> redList = new ArrayList<>();
        List<Float> greenList = new ArrayList<>();
        List<Float> blueList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(customContrastCurve)))) {
            String line;
            List<Float> currentList = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.contains("CUSTOM_R")) { currentList = redList; continue; }
                if (line.contains("CUSTOM_G")) { currentList = greenList; continue; }
                if (line.contains("CUSTOM_B")) { currentList = blueList; continue; }

                if (currentList != null && line.matches("^[0-9.-].*")) {
                    String[] parts = line.replace("f", "").split("[,\\s]+");
                    for (String part : parts) {
                        if (!part.isEmpty()) {
                            try {
                                currentList.add(Float.parseFloat(part));
                            } catch (NumberFormatException nfe) {
                                Log.e(TAG, "Invalid number format: " + part);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading custom curve: " + e.getMessage());
            return null;
        }

        if (redList.isEmpty() || greenList.isEmpty() || blueList.isEmpty()) {
            return null;
        }

        return new TonemapCurve(
                listToArray(redList),
                listToArray(greenList),
                listToArray(blueList)
        );
    }

    /**
     * Hilfsmethode zur Konvertierung einer Liste in ein float-Array
     */
    private float[] listToArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    public void setContrastCurve(CaptureRequest.Builder captureBuilder) {
        // we do this only in video mode or if framecount is 1 or if forced with forceNewSettingsInRegularPhotoMode
        if (!PhotonCamera.getSettings().useNewSettingsGloabal) {
            if (!PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO) && (PhotonCamera.getSettings().frameCount != 1)) {
                return;
            }
        }

        // look for keywords
        if (!PhotonCamera.getSettings().contrastCurve.contains("slog") &&
                !PhotonCamera.getSettings().contrastCurve.contains("logc3") &&
                !PhotonCamera.getSettings().contrastCurve.equals("high") &&
                !PhotonCamera.getSettings().contrastCurve.equals("linear") &&
                !PhotonCamera.getSettings().contrastCurve.equals("low") &&
                !PhotonCamera.getSettings().contrastCurve.equals("custom") &&
                !PhotonCamera.getSettings().contrastCurve.contains("style")) {
            return;
        }

        TonemapCurve customCurve = null;
        captureBuilder.set(CaptureRequest.TONEMAP_MODE, CameraMetadata.TONEMAP_MODE_CONTRAST_CURVE);
        if (PhotonCamera.getSettings().contrastCurve.equals("slog2")) {
            int points = 64;
            float[] red = new float[points * 2];
            float[] green = new float[points * 2];
            float[] blue = new float[points * 2];

            for (int i = 0; i < points; i++) {
                float x = i / (float)(points - 1);

                float y;
                if (x >= 0.011) {
                    y = (0.432699f * (float)Math.log10(10.0f * x + 1.0f) + 0.037584f) / 0.616596f;
                } else {
                    y = (x * 171.0f / 219.0f) + 0.037584f; // linear approximation in shadows
                }

                // clamp [0,1]
                y = Math.min(1.0f, Math.max(0.0f, y));

                red[i * 2] = x;
                red[i * 2 + 1] = y;

                green[i * 2] = x;
                green[i * 2 + 1] = y;

                blue[i * 2] = x;
                blue[i * 2 + 1] = y;
            }

            customCurve = new TonemapCurve(red, green, blue);
        }
        else if (PhotonCamera.getSettings().contrastCurve.equals("slogA")) {
            customCurve = new TonemapCurve(CurvePresets.SLOG2_APPROX_POINTS_A, CurvePresets.SLOG2_APPROX_POINTS_A, CurvePresets.SLOG2_APPROX_POINTS_A);
        }
        else if (PhotonCamera.getSettings().contrastCurve.equals("slogB")) {
            customCurve = new TonemapCurve(CurvePresets.SLOG2_APPROX_POINTS_B, CurvePresets.SLOG2_APPROX_POINTS_B, CurvePresets.SLOG2_APPROX_POINTS_B);
        }
        else if (PhotonCamera.getSettings().contrastCurve.equals("high")) {
            customCurve = new TonemapCurve(CurvePresets.HIGH_CONTRAST_POINTS, CurvePresets.HIGH_CONTRAST_POINTS, CurvePresets.HIGH_CONTRAST_POINTS);
        }
        else if (PhotonCamera.getSettings().contrastCurve.equals("low")) {
            customCurve = new TonemapCurve(CurvePresets.LOW_CONTRAST_POINTS, CurvePresets.LOW_CONTRAST_POINTS, CurvePresets.LOW_CONTRAST_POINTS);
        }
        else if (PhotonCamera.getSettings().contrastCurve.equals("linear")) {
            customCurve = new TonemapCurve(CurvePresets.LINEAR_CURVE, CurvePresets.LINEAR_CURVE, CurvePresets.LINEAR_CURVE);
        }
        else if (PhotonCamera.getSettings().contrastCurve.equals("style1")) {
            customCurve = new TonemapCurve(CurvePresets.RED_CURVE_STYLE_1, CurvePresets.GREEN_CURVE_STYLE_1, CurvePresets.BLUE_CURVE_STYLE_1);
        }
        else if (PhotonCamera.getSettings().contrastCurve.equals("style2")) {
            customCurve = new TonemapCurve(CurvePresets.RED_CURVE_STYLE_1, CurvePresets.BLUE_CURVE_STYLE_1, CurvePresets.GREEN_CURVE_STYLE_1);
        }
        else if (PhotonCamera.getSettings().contrastCurve.equals("custom")) {
            customCurve = loadCustomCurve();
        }
        else if (PhotonCamera.getSettings().contrastCurve.equals("logc3_ei800")) {
            int points = 64;
            float[] red = new float[points * 2];
            float[] green = new float[points * 2];
            float[] blue = new float[points * 2];

            final float a  = 3.0f;
            final float b  = 0.015f;
            final float c  = 0.150f;
            final float d  = 6.00f;
            final float e  = 0.310f;
            final float xc = 0.018f;

            for (int i = 0; i < points; i++) {
                float x = i / (float)(points - 1);

                float y;
                if (x <= xc) {
                    y = a * x + b;
                } else {
                    y = c * (float)Math.log10(d * x + 1.0f) + e;
                }

                y = Math.min(1.0f, Math.max(0.0f, y));

                red[i * 2] = x;
                red[i * 2 + 1] = y;
                green[i * 2] = x;
                green[i * 2 + 1] = y;
                blue[i * 2] = x;
                blue[i * 2 + 1] = y;
            }

            customCurve = new TonemapCurve(red, green, blue);
        }
        else if (PhotonCamera.getSettings().contrastCurve.equals("logc3_ei400")) {
            int points = 64;
            float[] red = new float[points * 2];
            float[] green = new float[points * 2];
            float[] blue = new float[points * 2];

            final float a  = 3.0f;
            final float b  = 0.015f;
            final float c  = 0.165f;
            final float d  = 6.50f;
            final float e  = 0.290f;
            final float xc = 0.018f;

            for (int i = 0; i < points; i++) {
                float x = i / (float)(points - 1);

                float y;
                if (x <= xc) {
                    y = a * x + b;
                } else {
                    y = c * (float)Math.log10(d * x + 1.0f) + e;
                }

                y = Math.min(1.0f, Math.max(0.0f, y));

                red[i * 2] = x;
                red[i * 2 + 1] = y;
                green[i * 2] = x;
                green[i * 2 + 1] = y;
                blue[i * 2] = x;
                blue[i * 2 + 1] = y;
            }

            customCurve = new TonemapCurve(red, green, blue);
        }

        if (customCurve != null) {
            captureBuilder.set(CaptureRequest.TONEMAP_CURVE, customCurve);
        }
    }

    public void determineFramerate(CameraCharacteristics characteristics) {
        if ((PhotonCamera.getSpecific().specificSetting.targetFps > 0) && !PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            FpsRangeHigh = new Range<>(PhotonCamera.getSpecific().specificSetting.targetFps, PhotonCamera.getSpecific().specificSetting.targetFps);
            FpsRangeDef = new Range<>(PhotonCamera.getSpecific().specificSetting.targetFps, PhotonCamera.getSpecific().specificSetting.targetFps);

            return;
        }

        Range<Integer>[] ranges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        int def = 30;
        if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            def = PhotonCamera.getSettings().videoFramrate;
        }
        int min = 20;
        if (ranges == null) {
            ranges = new Range[1];
            if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                ranges[0] = new Range<>(PhotonCamera.getSettings().videoFramrate, PhotonCamera.getSettings().videoFramrate);
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

        if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            int targetFps = PhotonCamera.getSettings().videoFramrate;
            if (targetFps >= 120) {
                StreamConfigurationMap config = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Range<Integer>[] hsRanges = config.getHighSpeedVideoFpsRanges();
                boolean hsSupported = false;
                if (hsRanges != null) {
                    for (Range<Integer> r : hsRanges) {
                        if (r.getUpper() >= targetFps) {
                            hsSupported = true;
                            break;
                        }
                    }
                }
                if (hsSupported) {
                    FpsRangeDef = new Range<>(targetFps, targetFps);
                    Log.d(TAG, "High Speed FPS Range set to: " + FpsRangeDef);
                } else {
                    Log.w(TAG, "Requested High Speed FPS " + targetFps + " not supported. Fallback to 60.");
                    FpsRangeDef = new Range<>(60, 60);
                }
            } else {
                FpsRangeDef = new Range<>(targetFps, targetFps);
            }
        }
        else if(FpsRangeDef == null || FpsRangeDef.getLower() > def) {
            FpsRangeDef = new Range<>(7, 30);
        }
    }

    public boolean customRawResForCamIdCheck(String camId) {
        String customResStr = PhotonCamera.getSpecific().specificSetting.customRawRes;
        if (customResStr == null || customResStr.isBlank() || !customResStr.startsWith("{") || !customResStr.endsWith("}")) {
            return false;
        }
        // Remove brackets {} and split by comma
        String[] entries = customResStr.substring(1, customResStr.length() - 1).split(",");
        for (String entry : entries) {
            // Entry format is "id-widthxheight"
            String[] parts = entry.split("-");
            if (parts.length > 0 && parts[0].trim().equals(camId)) {
                return true; // Found the camera ID
            }
        }
        return false; // Did not find the camera ID
    }

    public Size customRawResForCamId(String camId) {
        String customResStr = PhotonCamera.getSpecific().specificSetting.customRawRes;
        if (customResStr == null || customResStr.isBlank() || !customResStr.startsWith("{") || !customResStr.endsWith("}")) {
            return null;
        }
        // Remove brackets {} and split by comma
        String[] entries = customResStr.substring(1, customResStr.length() - 1).split(",");
        for (String entry : entries) {
            try {
                // Entry format is "id-widthxheight"
                String[] parts = entry.split("-");
                if (parts.length == 2 && parts[0].trim().equals(camId)) {
                    String[] resolution = parts[1].split("x");
                    if (resolution.length == 2) {
                        int width = Integer.parseInt(resolution[0].trim());
                        int height = Integer.parseInt(resolution[1].trim());
                        Log.d(TAG, "Found custom resolution for camId " + camId + ": " + width + "x" + height);
                        return new Size(width, height);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse custom resolution entry: '" + entry + "'", e);
                // Continue to the next entry in case of a malformed one
            }
        }
        return null; // Return zero size if not found
    }

    public void UpdateCameraCharacteristics(String cameraId) {
        createImageReaders(cameraId);

        // Find out if we need to swap dimension to get the preview size relative to sensor
        // coordinate.
        int displayRotation = PhotonCamera.getGravity().getRotation();
        mSensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        determineFramerate(mCameraCharacteristics);
        mCameraAfModes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);

        // Check if the flash is supported.
        Boolean available = mCameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        mFlashSupported = available != null && available;
        Camera2ApiAutoFix.Init();
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }
        activity.runOnUiThread(() -> {
            //Preview drawing size changing
            mPreviewSize = getTextureOutputSize(mTextureView.getDisplay(), PhotonCamera.getSettings().selectedMode);
            mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            cameraEventsListener.onCharacteristicsUpdated(mCameraCharacteristics);
            if ((PhotonCamera.getSettings().DebugData && !PhotonCamera.getSettings().useBasicOsd))
            {
                showToast("preview:" + new Point(mPreviewWidth, mPreviewHeight));
            }
        });
        //activity.runOnUiThread(() -> cameraEventsListener.onCharacteristicsUpdated(characteristics));
    }

    public boolean activateLut() {
        boolean test1 = isSingleShotJpegOrAvifOrHeic();
        boolean test2 = isSingleShotSwEncoder();
        boolean test3 = PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO);
        boolean test4 = PhotonCamera.getSettings().selectedMode.equals(CameraMode.RAWVIDEO);
        int test5 = PhotonCamera.getSettings().rawSaver;

        if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO) && !PhotonCamera.getSettings().lutName.equals("None") && PhotonCamera.getSpecific().specificSetting.enableVideoLut) {
            File previewLut = new File(FileManager.sPHOTON_TUNING_DIR, PhotonCamera.getSettings().lutName);
            if (!previewLut.exists()) {
                previewLut = new File(FileManager.sPHOTON_LUT_DIR, PhotonCamera.getSettings().lutName);
            }
            mMainRenderer.setLut(previewLut);
            mMainRenderer.setLutEnabled(!PhotonCamera.getSettings().lutName.equals("None"));
            return true;
        }

        if ((!isSingleShotJpegOrAvifOrHeic() || isSingleShotSwEncoder()) &&
                !PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO) &&
                !PhotonCamera.getSettings().selectedMode.equals(CameraMode.RAWVIDEO) &&
                !(PhotonCamera.getSettings().rawSaver == 2)) {
            File previewLut = new File(FileManager.sPHOTON_TUNING_DIR, PhotonCamera.getSettings().lutName);
            if (!previewLut.exists()) {
                previewLut = new File(FileManager.sPHOTON_LUT_DIR, PhotonCamera.getSettings().lutName);
            }
            mMainRenderer.setLut(previewLut);
            mMainRenderer.setLutEnabled(!PhotonCamera.getSettings().lutName.equals("None"));
            return true;
        }
        else {
            mMainRenderer.setLutEnabled(false);
            return false;
        }
    }

    boolean isUseCaseSupported(CameraCharacteristics chars, int useCase) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false;
        }

        long[] availableUseCases = chars.get(CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES);

        if (availableUseCases == null) {
            return false;
        }

        for (long caseItem : availableUseCases) {
            if (caseItem == useCase) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<OutputConfiguration> setOutputConfigurationLutVideo(List<Surface> surfaces) {
        ArrayList<OutputConfiguration> outputConfigurations = new ArrayList<>();
        for (Surface surfacei : surfaces) {
            var config = new OutputConfiguration(surfacei);
            outputConfigurations.add(config);
        }
        return outputConfigurations;
    }

    public ArrayList<OutputConfiguration> setOutputConfiguration(List<Surface> surfaces) {
        // special handling for LUT video
        if (PhotonCamera.getSpecific().specificSetting.enableVideoLut && PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO) && mIsRecordingVideo) {
            return setOutputConfigurationLutVideo(surfaces);
        }

        // other
        ArrayList<OutputConfiguration> outputConfigurations = new ArrayList<>();
        for (Surface surfacei : surfaces) {
            var config = new OutputConfiguration(surfacei);
            boolean isVideoSurface = (mVideoRecordingSurface == surfacei);
            boolean isPreviewSurface = (mImageReaderPreview.getSurface() == surfacei);

            if (!Objects.equals(physicalID, logicalID) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (PhotonCamera.getSettings().videoLogicalWorkaround && mIsRecordingVideo) {
                    if (!isVideoSurface) {
                        config.setPhysicalCameraId(physicalID);
                    } else {
                        Map<String, CameraLensData> lensDataMap = mCameraManager2.getCameraLensDataMap();
                        if (lensDataMap != null) {
                            CameraLensData camLensData = lensDataMap.get(PhotonCamera.getSettings().mCameraID);
                            if (camLensData != null) {
                                float test = camLensData.getZoomFactor();
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, camLensData.getZoomFactor());
                            }
                        }
                    }
                } else {
                    config.setPhysicalCameraId(physicalID);
                }
            }

            // activating HDR path and setting stream use case
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) && !PhotonCamera.getSettings().selectedMode.equals(CameraMode.RAWVIDEO)) {
                boolean gainMapRequested = checkHdrSupport(mCameraCharacteristics);
                long hdrProfile = DynamicRangeProfiles.HLG10;
                if (PhotonCamera.mHdrTenPlusIsSupported && PhotonCamera.getSpecific().specificSetting.hdrMode.equals("HDR10+")) {
                    hdrProfile = DynamicRangeProfiles.HDR10_PLUS;
                }
                if (PhotonCamera.mHdrTenIsSupported && PhotonCamera.getSpecific().specificSetting.hdrMode.equals("HDR10")) {
                    hdrProfile = DynamicRangeProfiles.HDR10;
                }
                // video
                if (mIsRecordingVideo && PhotonCamera.getSettings().videoHDR && PhotonCamera.getSettings().videoFramrate < 120) {
                    config.setDynamicRangeProfile(hdrProfile);
                }
                // Ultra HDR or 10 Bit surface as target (for encoding after image capture)
                if ((mTargetFormat == ImageFormat.JPEG_R) || (mTargetFormat == ImageFormat.YCBCR_P010)) {
                    if ((mImageReaderRaw.getSurface() == surfacei) || (mImageReaderPreview.getSurface() == surfacei)) {
                        config.setDynamicRangeProfile(hdrProfile);
                    }
                }
                else if ((mPreviewTargetFormat == ImageFormat.YCBCR_P010) && (mImageReaderPreview.getSurface() == surfacei) && PhotonCamera.getSettings().videoFramrate < 120) {
                    config.setDynamicRangeProfile(hdrProfile);
                }

                if (PhotonCamera.getSettings().useStreamUseCases) {
                    if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                        if (mIsRecordingVideo) {
                            if (mImageReaderPreview.getSurface() == surfacei) {
                                if (isUseCaseSupported(mCameraCharacteristics, CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW)) {
                                    config.setStreamUseCase(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW);
                                }
                            }

                            if (mVideoRecordingSurface == surfacei) {
                                if (isUseCaseSupported(mCameraCharacteristics, CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD)) {
                                    config.setStreamUseCase(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD);
                                    Log.i(TAG, "Using stream use case SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD");
                                }
                                if (mIsRecordingVideo && PhotonCamera.getSettings().videoHDR) {
                                    config.setDynamicRangeProfile(hdrProfile);
                                }
                            }
                        }
                    } else if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.PHOTO) && isSingleShotJpegOrAvifOrHeic()) {
                        if (mImageReaderRaw.getSurface() == surfacei) {
                            config.setStreamUseCase(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE);
                        }
                    }
                }
            }

            outputConfigurations.add(config);
        }

        return outputConfigurations;
    }

    public void createCameraPreviewSession(boolean isBurstSession) {
        try {
            createVendorKeysList();
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
            if (mTextureSurface != null) {
                mTextureSurface.release();
            }
            mTextureSurface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            setCaptureRequestBuilder();

            // Here, we create a CameraCaptureSession for camera preview.
            List<Surface> surfaces = configureSurfaces(isBurstSession);
            // check high speed request
            int SessionType = 0;
            int SessionTypeVideo = 0;
            if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                if (PhotonCamera.isEisLookAheadOn && (physicalID == logicalID)) {
                    SessionTypeVideo = PhotonCamera.getSettings().sessionTypeVideo | 0xF008;
                } else if (PhotonCamera.isEisRealtimeOn && (physicalID == logicalID)) {
                    SessionTypeVideo = PhotonCamera.getSettings().sessionTypeVideo | 0xF004;
                } else {
                    SessionTypeVideo = PhotonCamera.getSettings().sessionTypeVideo;
                }
            }
            else {
                SessionType = PhotonCamera.getSettings().sessionType;
                if ((mTargetFormat == ImageFormat.HEIC) || !PhotonCamera.isSessionTypeOn) {
                    SessionType = 0;
                }
            }
            int[] capabilities = mCameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
            if (capabilities != null) {
                for (int capability : capabilities) {
                    if (capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO) {
                        mIsHighSpeedSupported = true;
                        break;
                    }
                }
            }
            boolean isHighSpeedSessionRequested = mIsRecordingVideo && mIsHighSpeedSupported && (PhotonCamera.getSettings().videoFramrate >= 120);

            if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                if (isHighSpeedSessionRequested) {
                    SessionTypeVideo = SessionConfiguration.SESSION_HIGH_SPEED;
                }
            }

            Log.d(TAG, "createCameraPreviewSession() surfaces:" + Arrays.toString(surfaces.toArray()));
            ArrayList<OutputConfiguration> outputConfigurations = setOutputConfiguration(surfaces);

            CameraCaptureSession.StateCallback stateCallback = new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.d(TAG, "CameraCaptureSession onConfigured():" + cameraCaptureSession);
                    // The camera is already closed
                    if (null == mCameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    mCaptureSession = cameraCaptureSession;
                    if  (mIsRecordingVideo && mIsHighSpeedSupported && (PhotonCamera.getSettings().videoFramrate >= 120)) {
                        mHighSpeedCaptureSession = (CameraConstrainedHighSpeedCaptureSession) cameraCaptureSession;
                    }
                    try {
                        // Auto focus should be continuous for camera preview.
                        //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        // Flash is automatically enabled when necessary.
                        if (PhotonCamera.getSettings().functionOne.contains("Lock Video Params") && mIsRecordingVideo) {
                            suspendAutomatics(mPreviewRequestBuilder);
                        }
                        else {
                            resetPreviewAEMode();
                        }
                        Camera2ApiAutoFix.applyPrev(mPreviewRequestBuilder);
                        VendorTagUtils.builderSessionApply(mCameraCharacteristics, mPreviewRequestBuilder, false, useMaximumResolutionKey, true);
                        // Finally, we start displaying the camera preview.
                        Range<Integer> targetRange = FpsRangeDef;
                        if (PhotonCamera.getSettings().videoFramrate >= 120 && !mIsRecordingVideo) {
                            // If we are not recording, use a variable range to allow regular session compatibility
                            targetRange = new Range<>(30, PhotonCamera.getSettings().videoFramrate);
                        }
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, targetRange);

                        setAdvancedParameters(mPreviewRequestBuilder, true);
                        activateLut();

                        List<CaptureRequest> highSpeedRequests = null;
                        if (mIsRecordingVideo && mIsHighSpeedSupported && (PhotonCamera.getSettings().videoFramrate >= 120)) {
                            highSpeedRequests = mHighSpeedCaptureSession.createHighSpeedRequestList(mPreviewRequestBuilder.build());
                        }
                        else {
                            mPreviewInputRequest = mPreviewRequestBuilder.build();
                        }
                        if (isBurstSession && isDualSession) {
                            switch (CameraFragment.mSelectedMode) {
                                case NIGHT:
                                case PHOTO:
                                case MOTION:
                                    mCaptureSession.captureBurst(captures, CaptureCallback, mBackgroundHandler);
                                    break;
                                case UNLIMITED:
                                case RAWVIDEO:
                                    mCaptureSession.setRepeatingBurst(captures, CaptureCallback, mBackgroundHandler);
                                    break;
                            }
                        } else {
                            //if(mSelectedMode != CameraMode.VIDEO)
                            if (mIsRecordingVideo && (PhotonCamera.getSettings().videoFramrate >= 120) && mIsHighSpeedSupported && (highSpeedRequests != null)) {
                                mHighSpeedCaptureSession.setRepeatingBurst(highSpeedRequests, mCaptureCallback, mBackgroundHandler);
                            }
                            else {
                                if (mPreviewInputRequest != null) {
                                    mCaptureSession.setRepeatingRequest(mPreviewInputRequest, mCaptureCallback, mBackgroundHandler);
                                } else {
                                    Log.e(TAG, "mPreviewInputRequest == null");
                                }
                            }
                            unlockFocus();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                    if (mIsRecordingVideo)
                        activity.runOnUiThread(() -> {
                            // Start recording
                            if (PhotonCamera.getSettings().videoNewRec) {
                                //mMediaMuxer.start();
                            }
                            else {
                                if (mMediaRecorder != null) {
                                    mMediaRecorder.start();
                                }
                            }
                        });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    showToast(activity.getString(R.string.session_on_configure_failed));
                    Log.e(TAG, "CameraCaptureSession ON CONFIGURE FAILED!");
                }
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                SessionConfiguration configuration = null;
                if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                    configuration = new SessionConfiguration(
                            SessionTypeVideo,
                            outputConfigurations,
                            processExecutor,
                            stateCallback
                    );
                }
                else {
                    configuration = new SessionConfiguration(
                            SessionType,
                            outputConfigurations,
                            processExecutor,
                            stateCallback
                    );
                }
                /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    if (checkColorSpaceProfilesSupport(mCameraManager)) {
                        configuration.setColorSpace(ColorSpace.Named.SRGB);
                    }
                }*/
                if (configuration != null) {
                    configuration.setSessionParameters(mPreviewRequestBuilder.build());
                    mCameraDevice.createCaptureSession(configuration);
                }
            } else {
                mCameraDevice.createCaptureSession(surfaces, stateCallback, mBackgroundHandler);
            }

            if (cameraEventsListener != null) {
                cameraEventsListener.onPreviewStarted();
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public static void createCustomCaptureSession(CameraDevice device, SessionConfiguration config, int opMode)
            throws CameraAccessException, ReflectiveOperationException {

        if (device == null || config == null) {
            throw new IllegalArgumentException("CameraDevice and SessionConfiguration must not be null");
        }

        Log.d(TAG, "Trying createCustomCaptureSession with OpCode/OpMode " + opMode + " using reflection");

        InputConfiguration inputConfig = config.getInputConfiguration();
        List<android.view.Surface> outputSurfaces = config.getOutputConfigurations().stream()
                .map(android.hardware.camera2.params.OutputConfiguration::getSurface)
                .collect(java.util.stream.Collectors.toList());
        CameraCaptureSession.StateCallback stateCallback = config.getStateCallback();
        Executor executor = config.getExecutor();

        Handler handler = (executor instanceof Handler) ? (Handler) executor : new Handler(android.os.Looper.myLooper());

        try {
            //Method createCustomCaptureSessionMethod = CameraDevice.class.getMethod("createCustomCaptureSession", InputConfiguration.class, List.class, Integer.TYPE, CameraCaptureSession.StateCallback.class, Handler.class);
            Method createCustomCaptureSessionMethod = RestrictionBypass.getDeclaredMethod(device.getClass(), "createCustomCaptureSession", InputConfiguration.class, List.class, Integer.TYPE, CameraCaptureSession.StateCallback.class, Handler.class);
            createCustomCaptureSessionMethod.setAccessible(true);
            createCustomCaptureSessionMethod.invoke(device, inputConfig, outputSurfaces, opMode, stateCallback, handler);
            Log.i(TAG, "Invoking createCustomCaptureSession success");
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Method createCustomCaptureSession was not found by reflection", e);
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Invoking createCustomCaptureSession failed", e);
            if (e.getCause() instanceof CameraAccessException) {
                throw (CameraAccessException) e.getCause();
            }
            throw new ReflectiveOperationException("Error in reflection method createCustomCaptureSession attempt", e);
        }
    }

    @NotNull
    private List<Surface> configureSurfacesRawVideo(boolean isBurstSession) {
        List<Surface> surfaces = new ArrayList<>();

        if (mPreviewRequestBuilder != null) {
            if (mTextureSurface != null && mTextureSurface.isValid()) {
                surfaces.add(mTextureSurface);
            }
            surfaces.add(mImageReaderPreview.getSurface());
            surfaces.add(mImageReaderRaw.getSurface());

        }
        Log.d(TAG, "Final number of surfaces for RAW video mode: " + surfaces.size());
        return surfaces;
    }

    private List<Surface> configureSurfacesLutVideo(boolean isBurstSession) {
        List<Surface> surfaces = new ArrayList<>();

        if (mPreviewRequestBuilder != null) {
            if (mTextureSurface != null && mTextureSurface.isValid()) {
                surfaces.add(mTextureSurface);
            }
            surfaces.add(mImageReaderPreview.getSurface());
        }
        if (PhotonCamera.getSettings().videoNewRec) {
            if (setUpMediaRecorderNew()) {
                mVideoRecordingSurface = mMediaCodecSurface;
            } else {
                mIsRecordingVideo = false;
            }
        } else {
            if (setUpMediaRecorder()) {
                mVideoRecordingSurface = mMediaRecorder.getSurface();
            } else {
                mIsRecordingVideo = false;
            }
        }
        if (mVidHeight < mVidWidth) {
            mMainRenderer.setVideoRecordingSurface(mVideoRecordingSurface, mVidWidth, mVidHeight);
        }
        else {
            mMainRenderer.setVideoRecordingSurface(mVideoRecordingSurface, mVidHeight, mVidWidth);
        }
        Log.d(TAG, "Final number of surfaces for LUT video mode: " + surfaces.size());
        return surfaces;
    }

    @NotNull
    private List<Surface> configureSurfaces(boolean isBurstSession) {
        if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.RAWVIDEO)) {
            return configureSurfacesRawVideo(isBurstSession);
        }

        boolean isHighSpeedSupported = false;
        int[] capabilities = mCameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
        if (capabilities != null) {
            for (int capability : capabilities) {
                if (capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO) {
                    isHighSpeedSupported = true;
                    break;
                }
            }
        }
        boolean isHighSpeedSessionRequested = mIsRecordingVideo && isHighSpeedSupported && (PhotonCamera.getSettings().videoFramrate >= 120);

        if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO) && mIsRecordingVideo && PhotonCamera.getSpecific().specificSetting.enableVideoLut && !isHighSpeedSessionRequested) {
            return configureSurfacesLutVideo(isBurstSession);
        }

        List<Surface> surfaces = new ArrayList<>();

        if (mIsRecordingVideo) {
            if (PhotonCamera.getSettings().videoNewRec) {
                if (setUpMediaRecorderNew()) {
                    mVideoRecordingSurface = mMediaCodecSurface;
                } else {
                    mIsRecordingVideo = false;
                    //cameraEventsListener.onError(TAG + "setUpMediaRecorderNew FAILED.");
                }
            } else {
                if (setUpMediaRecorder()) {
                    mVideoRecordingSurface = mMediaRecorder.getSurface();
                } else {
                    mIsRecordingVideo = false;
                }
            }

            if (isHighSpeedSessionRequested) {
                if (mTextureSurface != null && mTextureSurface.isValid()) {
                    surfaces.add(mTextureSurface);
                }
                if (mVideoRecordingSurface != null && mVideoRecordingSurface.isValid()) {
                    surfaces.add(mVideoRecordingSurface);
                    if (mPreviewRequestBuilder != null) {
                        mPreviewRequestBuilder.addTarget(mVideoRecordingSurface);
                    }
                }
                Log.i(TAG, "Configuring surfaces for HIGH-SPEED session.");
            } else {
                if (mTextureSurface != null && mTextureSurface.isValid()) {
                    surfaces.add(mTextureSurface);
                }
                if (mVideoRecordingSurface != null && mVideoRecordingSurface.isValid()) {
                    surfaces.add(mVideoRecordingSurface);
                    if (mPreviewRequestBuilder != null) {
                        mPreviewRequestBuilder.addTarget(mVideoRecordingSurface);
                    }
                }
                Log.i(TAG, "Configuring surfaces for REGULAR video session.");
            }
            return surfaces;
        }

        if (mTextureSurface != null && mTextureSurface.isValid()) {
            surfaces.add(mTextureSurface);
        }

        if (isDualSession && isBurstSession) {
            if (mImageReaderPreview != null && mImageReaderPreview.getSurface() != null) {
                surfaces.add(mImageReaderPreview.getSurface());
            }
            if (mImageReaderRaw != null && mImageReaderRaw.getSurface() != null) {
                surfaces.add(mImageReaderRaw.getSurface());
            }
        } else {
            if (!PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO) && (mImageReaderRaw != null) && (mImageReaderRaw.getSurface() != null)) {
                surfaces.add(mImageReaderRaw.getSurface());
            }
        }

        Log.d(TAG, "Final number of surfaces: " + surfaces.size());
        return surfaces;
    }


    private void setCaptureRequestBuilder() throws CameraAccessException {
        mPreviewRequestBuilder = null;
        if (mIsRecordingVideo) {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        } else {
            if (PhotonCamera.getSettings().useAlternatePreviewTemplate) {
                mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            }
            else {
                mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            }
        }

        if (mIsRecordingVideo && PhotonCamera.getSettings().videoHDR) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_HDR);
            }
        }

        mPreviewRequestBuilder.addTarget(mTextureSurface);
        mPreviewMeteringAF = mPreviewRequestBuilder.get(CONTROL_AF_REGIONS);
        mPreviewAFMode = PreferenceKeys.getAfMode();

        // QualityDoesMatter
        try {
            setAdvancedParameters(mPreviewRequestBuilder, true);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_ZOOM_METHOD, CaptureRequest.CONTROL_ZOOM_METHOD_ZOOM_RATIO);
        }

        setDigitalZoomFactor(mPreviewRequestBuilder);

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
        setSceneAndEffectMode(mPreviewRequestBuilder);

        mPreviewMeteringAE = mPreviewRequestBuilder.get(CONTROL_AE_REGIONS);
        mPreviewAEMode = mPreviewRequestBuilder.get(CONTROL_AE_MODE);
    }

    private void setSceneAndEffectMode(CaptureRequest.Builder builder) {
        if (isSingleShotJpegOrAvifOrHeic() && PhotonCamera.getSettings().useSceneAndEffectMode) {
            if (!paramController.isManualMode()) {
                builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE);
                switch (PhotonCamera.getSettings().selectedMode) {
                    case NIGHT:
                        builder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_NIGHT);
                        if (PhotonCamera.getSettings().effectMode != 0) {
                            builder.set(CaptureRequest.CONTROL_EFFECT_MODE, PhotonCamera.getSettings().effectMode);
                        }
                        break;
                    case MOTION:
                        builder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_SPORTS);
                        if (PhotonCamera.getSettings().effectMode != 0) {
                            builder.set(CaptureRequest.CONTROL_EFFECT_MODE, PhotonCamera.getSettings().effectMode);
                        }
                        break;
                    case PHOTO:
                        builder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_HDR);
                        if (PhotonCamera.getSettings().effectMode != 99) {
                            builder.set(CaptureRequest.CONTROL_EFFECT_MODE, PhotonCamera.getSettings().effectMode);
                        }
                        break;
                }
            }
        }
    }

    /*** Converts a color temperature (in Kelvin) and a tint value into an
     * RggbChannelVector suitable for manual white balance control in Camera2.
     *
     * This function includes a calibration constant (TINT_NEUTRAL_OFFSET) to
     * compensate for the inherent color bias of a specific camera sensor, ensuring
     * that a tint value of 0.0f results in a visually neutral image.
     *
     * @param kelvin The color temperature in Kelvin. Typical values range from 2000K (very warm)
     *               to 8000K (very cold). A value of 6500K is approximately neutral daylight.
     * @param tint   The tint value for green-magenta color shift. A useful range is
     *               -1.0f (adds green) to +1.0f (adds magenta). 0.0f should be neutral.
     * @return An RggbChannelVector to be used with CaptureRequest.COLOR_CORRECTION_GAINS.
     */
    public static RggbChannelVector kelvinAndTintToGains(int kelvin, float tint) {

        // --- Calibration Constant ---
        // This constant compensates for the strong inherent green bias of the sensor.
        // It's based on the observation that a tint of 1.0f was needed for a neutral image.
        // This value might need slight tuning for different devices, but it's a strong baseline.
        final float TINT_NEUTRAL_OFFSET = 1.0f;

        // Clamp input values to a reasonable range
        kelvin = Math.max(1000, Math.min(15000, kelvin));
        tint = Math.max(-1.0f, Math.min(1.0f, tint));

        // --- Step 1: Approximate the RGB color of a black-body radiator ---
        float temp = kelvin / 100.0f;
        float red, green, blue;

        // Calculate Red component
        if (temp <= 66) {
            red = 255;
        } else {
            red = temp - 60;
            red = (float) (329.698727446 * Math.pow(red, -0.1332047592));
        }

        // Calculate Green component
        if (temp <= 66) {
            green = temp;
            green = (float) (99.4708025861 * Math.log(green) - 161.1195681661);
        } else {
            green = temp - 60;
            green = (float) (288.1221695283 * Math.pow(green, -0.0755148492));
        }

        // Calculate Blue component
        if (temp >= 66) {
            blue = 255;
        } else {
            if (temp <= 19) {
                blue = 0;
            } else {
                blue = temp - 10;
                blue = (float) (138.5177312231 * Math.log(blue) - 305.0447927307);
            }
        }

        red = Math.max(0, Math.min(255, red));
        green = Math.max(0, Math.min(255, green));
        blue = Math.max(0, Math.min(255, blue));

        // --- Step 2: Calculate the inverse gains required to neutralize the light color ---
        float redGain = 255.0f / red;
        float greenGain = 255.0f / green;
        float blueGain = 255.0f / blue;

        // --- Step 3: Normalize the gains ---
        float minGain = Math.min(redGain, Math.min(greenGain, blueGain));
        redGain /= minGain;
        greenGain /= minGain;
        blueGain /= minGain;

        // --- Step 4: Apply the tint adjustment with the calibration offset ---
        // The user's tint is now combined with the neutral offset before being applied.
        float totalTint = tint + TINT_NEUTRAL_OFFSET;
        float tintFactor = totalTint * 0.5f;
        greenGain = greenGain * (1.0f - tintFactor);

        // The RggbChannelVector expects gains for (Red, Green_even, Green_odd, Blue)
        return new RggbChannelVector(redGain, greenGain, greenGain, blueGain);
    }

    private void showToast(String msg) {
        if (activity != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast toast = Toast.makeText(activity, msg, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            });
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
                CaptureRequest req = mPreviewRequestBuilder.build();
                if (req != null) {
                    mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
                } else {
                    Log.e(TAG, "mPreviewRequestBuilder.build() failed.");
                }
            } catch (CameraAccessException e) {
                Log.e(TAG, "Failed to start camera preview because it couldn't access camera", e);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Failed to start camera preview.", e);
            }
        }
    }

    public void zoomSliderChanged(float newZoomValue) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!PhotonCamera.getSettings().zoom2X) {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, newZoomValue);
                try {
                    if (mCaptureSession != null) {
                        CaptureRequest req = mPreviewRequestBuilder.build();
                        if (req != null) {
                            mCaptureSession.setRepeatingRequest(req, mCaptureCallback, mBackgroundHandler);
                        } else {
                            Log.e(TAG, "mPreviewRequestBuilder.build() failed.");
                        }
                        mDigitalZoom = newZoomValue;
                    }
                } catch (CameraAccessException e) {
                    Log.e(TAG, "Failed to update zoom for preview.", e);
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Failed to update zoom, camera session is not available.", e);
                }
            }
        }
    }

    public void fillMetadataFromCaptureResult(CaptureResult captureResult) {
        if (isSingleShotJpegOrAvifOrHeic()) {
            if (mMetaData == null) {
                mMetaData = new Bundle();
            }
            Map<String, CameraLensData> lensDataMap = mCameraManager2.getCameraLensDataMap();
            if (lensDataMap != null) {
                CameraLensData camLensData = lensDataMap.get(PhotonCamera.getSettings().mCameraID);
                if (camLensData != null) {
                    PhotonCamera.getParameters().current35mmFocalLength = (int) Math.ceil(camLensData.getCamera35mmFocalLength());
                }
            }
            if (captureResult != null) {
                Integer iso = captureResult.get(CaptureResult.SENSOR_SENSITIVITY);
                if (iso != null) {
                    mMetaData.putInt("iso", iso);
                }

                Long exposureTime = captureResult.get(CaptureResult.SENSOR_EXPOSURE_TIME);
                String strExposureTime = "";
                if (exposureTime != null && exposureTime > 0) {
                    if (exposureTime >= 1_000_000_000L) {
                        double seconds = exposureTime / 1_000_000_000.0;
                        strExposureTime = String.format(Locale.getDefault(), "%.1fs", seconds);
                    } else {
                        long divisor = (long) (1_000_000_000.0 / exposureTime);
                        strExposureTime = "1/" + divisor;
                    }
                }

                if (exposureTime != null) {
                    mMetaData.putLong("exposureTime", exposureTime);
                    mMetaData.putString("exposureTimeStr", strExposureTime);
                }

                Float focalLength = captureResult.get(CaptureResult.LENS_FOCAL_LENGTH);
                if (focalLength != null) {
                    mMetaData.putFloat("focalLength", focalLength);
                }

                Float aperture = captureResult.get(CaptureResult.LENS_APERTURE);
                if (aperture != null) {
                    mMetaData.putFloat("aperture", aperture);
                }

                if (PhotonCamera.getParameters().current35mmFocalLength != 0) {
                    mMetaData.putInt("focal35mm", PhotonCamera.getParameters().current35mmFocalLength);
                }

                mMetaData.putString("physCamID", physicalID);
                if (physicalID != logicalID) {
                    mMetaData.putString("logiCamID", logicalID);
                }

                if (PhotonCamera.getSettings().gpsLocation && (PhotonCamera.gpsLocation != null)) {
                    mMetaData.putString("latitude", String.valueOf(PhotonCamera.gpsLocation.getLatitude()));
                    mMetaData.putString("longitude", String.valueOf(PhotonCamera.gpsLocation.getLongitude()));
                    if (PhotonCamera.gpsLocation.hasAltitude()) {
                        mMetaData.putString("altitude", String.valueOf(PhotonCamera.gpsLocation.getAltitude()));
                    }
                }
            }
        }
    }

    public void functionOne() {
        mIsFunctionOneOn = !mIsFunctionOneOn;
        PhotonCamera.isFunctionOneOn = mIsFunctionOneOn;

        if (PhotonCamera.getSettings().functionOne.equals("Session Type (OpCode) OFF")) {
            PhotonCamera.isSessionTypeOn = !mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionOne.equals("Xiaomi Super Night Mode")) {
            PhotonCamera.isSuperNightModeOn = mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionOne.equals("Xiaomi Night Mode")) {
            PhotonCamera.isNightModeOn = mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionOne.equals("Xiaomi Re-Mosaic")) {
            PhotonCamera.isRemosaicOn = mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionOne.equals("Xiaomi AI Auto Scene Detection")) {
            PhotonCamera.isAiAutoSceneDetectionOn = mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionOne.equals("Xiaomi Pro Video LOG")) {
            PhotonCamera.isProVideoLogOn = mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionOne.equals("Xiaomi Pro Video Movie")) {
            PhotonCamera.isProVideoLogMovie = mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionOne.equals("Xiaomi HDR")) {
            PhotonCamera.isHdrOn = mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionOne.equals("Xiaomi Ultra HDR")) {
            PhotonCamera.isUltraHdrOn = mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionOne.equals("Xiaomi Super Resolution")) {
            PhotonCamera.isSuperResOn = mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionOne.equals("Xiaomi Quad CFA")) {
            PhotonCamera.isQuadCfaOn = mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionOne.equals("Ideal RAW")) {
            PhotonCamera.isIdealRawOn = mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionOne.equals("Qualcomm ADRC Off")) {
            PhotonCamera.isQucommAdrcOff = mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionOne.equals("Qualcomm Sensor Mode")) {
            PhotonCamera.isQucommSensorModeOn = mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionOne.equals("EIS Look Ahead")) {
            PhotonCamera.isEisLookAheadOn = mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionOne.equals("EIS Realtime")) {
            PhotonCamera.isEisRealtimeOn = mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionOne.equals("EIS V3")) {
            PhotonCamera.isEisV3On = mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionOne.equals("Vivo Zeiss Color")) {
            PhotonCamera.isVivoZeissColorOn = mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionOne.equals("Vivo Pro Mode")) {
            PhotonCamera.isVivoProModeOn = mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionOne.equals("Vivo Sensor Mode")) {
            PhotonCamera.isVivoSensorModeOn = mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionOne.equals("Vivo Distortion Correction")) {
            PhotonCamera.isVivoDistortionCorrectionOn = mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionOne.equals("Closed Aperture")) {
            if (mIsFunctionOneOn) {
                if (PhotonCamera.getSettings().functionOne.contains("Closed Aperture")) {
                    var lensApertureXiaomi = new CaptureRequest.Key<>("com.xiaomi.lens.aperture", Float.class);
                    if (VendorTagUtils.isSupported(mPreviewRequestBuilder, lensApertureXiaomi)) {
                        mPreviewRequestBuilder.set(lensApertureXiaomi, 4.0f);
                    }
                }

                if (PhotonCamera.getSettings().functionOne.equals("Opened Aperture")) {
                    float minAperture = 4.0f;
                    CameraCharacteristics.Key<Float[]> vendorKey = new CameraCharacteristics.Key<>("com.xiaomi.lens.info.availableApertures", Float[].class);
                    Float[] apert = mCameraCharacteristics.get(vendorKey);
                    if (apert != null && apert.length > 0) {
                        minAperture = apert[0];
                    }
                    var lensApertureXiaomi = new CaptureRequest.Key<>("com.xiaomi.lens.aperture", Float.class);
                    if (VendorTagUtils.isSupported(mPreviewRequestBuilder, lensApertureXiaomi)) {
                        mPreviewRequestBuilder.set(lensApertureXiaomi, minAperture);
                    }
                }
            } else {
                var lensApertureXiaomi = new CaptureRequest.Key<>("com.xiaomi.lens.aperture", Float.class);
                if (VendorTagUtils.isSupported(mPreviewRequestBuilder, lensApertureXiaomi)) {
                    mPreviewRequestBuilder.set(lensApertureXiaomi, PhotonCamera.getSettings().apertureToUse);
                }
            }
            try {
                if (mCaptureSession != null) {
                    CaptureRequest req = mPreviewRequestBuilder.build();
                    if (req != null) {
                        mCaptureSession.setRepeatingRequest(req, mCaptureCallback, mBackgroundHandler);
                    } else {
                        Log.e(TAG, "mPreviewRequestBuilder.build() failed.");
                    }
                }
            } catch (CameraAccessException e) {
                Log.e(TAG, "Aperture change failed", e);
            } catch (IllegalStateException e) {
                Log.e(TAG, "No camera session", e);
            }
        }

        if (PhotonCamera.getSettings().functionOne.contains("Priority")) {
            restartCamera();
        }
    }

    public void functionTwo() {
        mIsFunctionTwoOn = !mIsFunctionTwoOn;
        PhotonCamera.isFunctionTwoOn = mIsFunctionTwoOn;

        if (PhotonCamera.getSettings().functionOne.equals("Session Type (OpCode) OFF")) {
            PhotonCamera.isSessionTypeOn = !mIsFunctionOneOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionTwo.equals("Xiaomi Super Night Mode")) {
            PhotonCamera.isSuperNightModeOn = mIsFunctionTwoOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionTwo.equals("Xiaomi Night Mode")) {
            PhotonCamera.isNightModeOn = mIsFunctionTwoOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionTwo.equals("Xiaomi Re-Mosaic")) {
            PhotonCamera.isRemosaicOn = mIsFunctionTwoOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionTwo.equals("Xiaomi AI Auto Scene Detection")) {
            PhotonCamera.isAiAutoSceneDetectionOn = mIsFunctionTwoOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionTwo.equals("Xiaomi Pro Video LOG")) {
            PhotonCamera.isProVideoLogOn = mIsFunctionTwoOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionTwo.equals("Xiaomi Pro Video Movie")) {
            PhotonCamera.isProVideoLogMovie = mIsFunctionTwoOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionTwo.equals("Xiaomi HDR")) {
            PhotonCamera.isHdrOn = mIsFunctionTwoOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionTwo.equals("Xiaomi Ultra HDR")) {
            PhotonCamera.isUltraHdrOn = mIsFunctionTwoOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionTwo.equals("Xiaomi Super Resolution")) {
            PhotonCamera.isSuperResOn = mIsFunctionTwoOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionTwo.equals("Xiaomi Quad CFA")) {
            PhotonCamera.isQuadCfaOn = mIsFunctionTwoOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionTwo.equals("Ideal RAW")) {
            PhotonCamera.isIdealRawOn = mIsFunctionTwoOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionTwo.equals("Qualcomm ADRC Off")) {
            PhotonCamera.isQucommAdrcOff = mIsFunctionTwoOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionTwo.equals("Qualcomm Sensor Mode")) {
            PhotonCamera.isQucommSensorModeOn = mIsFunctionTwoOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionTwo.equals("EIS Look Ahead")) {
            PhotonCamera.isEisLookAheadOn = mIsFunctionTwoOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionTwo.equals("EIS Realtime")) {
            PhotonCamera.isEisRealtimeOn = mIsFunctionTwoOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionTwo.equals("EIS V3")) {
            PhotonCamera.isEisV3On = mIsFunctionTwoOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionTwo.equals("Vivo Zeiss Color")) {
            PhotonCamera.isVivoZeissColorOn = mIsFunctionTwoOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionTwo.equals("Vivo Pro Mode")) {
            PhotonCamera.isVivoProModeOn = mIsFunctionTwoOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionTwo.equals("Vivo Sensor Mode")) {
            PhotonCamera.isVivoSensorModeOn = mIsFunctionTwoOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionTwo.equals("Vivo Distortion Correction")) {
            PhotonCamera.isVivoDistortionCorrectionOn = mIsFunctionTwoOn;
            restartCamera();
            return;
        } else if (PhotonCamera.getSettings().functionTwo.equals("Closed Aperture")) {
            if (mIsFunctionTwoOn) {
                if (PhotonCamera.getSettings().functionTwo.contains("Closed Aperture")) {
                    var lensApertureXiaomi = new CaptureRequest.Key<>("com.xiaomi.lens.aperture", Float.class);
                    if (VendorTagUtils.isSupported(mPreviewRequestBuilder, lensApertureXiaomi)) {
                        mPreviewRequestBuilder.set(lensApertureXiaomi, 4.0f);
                    }
                }

                if (PhotonCamera.getSettings().functionTwo.equals("Opened Aperture")) {
                    float minAperture = 4.0f;
                    CameraCharacteristics.Key<Float[]> vendorKey = new CameraCharacteristics.Key<>("com.xiaomi.lens.info.availableApertures", Float[].class);
                    Float[] apert = mCameraCharacteristics.get(vendorKey);
                    if (apert != null && apert.length > 0) {
                        minAperture = apert[0];
                    }
                    var lensApertureXiaomi = new CaptureRequest.Key<>("com.xiaomi.lens.aperture", Float.class);
                    if (VendorTagUtils.isSupported(mPreviewRequestBuilder, lensApertureXiaomi)) {
                        mPreviewRequestBuilder.set(lensApertureXiaomi, minAperture);
                    }
                }
            } else {
                var lensApertureXiaomi = new CaptureRequest.Key<>("com.xiaomi.lens.aperture", Float.class);
                if (VendorTagUtils.isSupported(mPreviewRequestBuilder, lensApertureXiaomi)) {
                    mPreviewRequestBuilder.set(lensApertureXiaomi, PhotonCamera.getSettings().apertureToUse);
                }
            }
            try {
                if (mCaptureSession != null) {
                    CaptureRequest req = mPreviewRequestBuilder.build();
                    if (req != null) {
                        mCaptureSession.setRepeatingRequest(req, mCaptureCallback, mBackgroundHandler);
                    } else {
                        Log.e(TAG, "mPreviewRequestBuilder.build() failed.");
                    }
                }
            } catch (CameraAccessException e) {
                Log.e(TAG, "Aperture change failed", e);
            } catch (IllegalStateException e) {
                Log.e(TAG, "No camera session", e);
            }
        }

        if (PhotonCamera.getSettings().functionTwo.contains("Priority")) {
            restartCamera();
        }
    }

    public void setAutoExposureCenter() {
        Rect activeArray = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (activeArray == null) return;

        int fullWidth = activeArray.width();
        int fullHeight = activeArray.height();

        int rectWidth = (int) (fullWidth * 0.3f);
        int rectHeight = (int) (fullHeight * 0.3f);

        int left = (fullWidth - rectWidth) / 2;
        int top = (fullHeight - rectHeight) / 2;

        Rect rect = new Rect(left, top, left + rectWidth, top + rectHeight);
        MeteringRectangle meteringRect = new MeteringRectangle(rect, MeteringRectangle.METERING_WEIGHT_MAX);

        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{meteringRect});

        try {
            if (mCaptureSession != null) {
                CaptureRequest req = mPreviewRequestBuilder.build();
                if (req != null) {
                    mCaptureSession.setRepeatingRequest(req, mCaptureCallback, mBackgroundHandler);
                } else {
                    Log.e(TAG, "mPreviewRequestBuilder.build() failed.");
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to set AE region", e);
        }
    }

    public void magnifyViewfinder() {
        if (mIsViewFinderMagnified) {
            if (PhotonCamera.getSettings().zoom2X) {
                if (PhotonCamera.getSettings().useAlternateLoupe) {
                    if (mMainRenderer != null) {
                        mMainRenderer.setMagnifyEnabled(false);
                    }
                }
                else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, PhotonCamera.getSettings().digitalZoomFactor);
                    }
                }
            }
            else {
                if (PhotonCamera.getSettings().useAlternateLoupe) {
                    if (mMainRenderer != null) {
                        mMainRenderer.setMagnifyEnabled(false);
                    }
                }
                else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, 1.0f);
                    }
                }
            }
        }
        else {
            if (PhotonCamera.getSettings().useAlternateLoupe) {
                if (mMainRenderer != null) {
                    mMainRenderer.setMagnifyEnabled(true);
                }
            }
            else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, 4.0f);
                }
            }
        }
        mIsViewFinderMagnified = !mIsViewFinderMagnified;

        if (!PhotonCamera.getSettings().useAlternateLoupe) {
            try {
                CaptureRequest req = mPreviewRequestBuilder.build();
                if (req != null) {
                    mCaptureSession.setRepeatingRequest(req, mCaptureCallback, mBackgroundHandler);
                } else {
                    Log.e(TAG, "mPreviewRequestBuilder.build() failed.");
                }
            } catch (CameraAccessException e) {
                Log.e(TAG, "Failed to update zoom for preview.", e);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Failed to update zoom, camera is not available.", e);
            }
        }
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    public void unlockFocus() {
        try {
            if (mPreviewRequestBuilder != null) {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                reset3Aparams();
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                paramController.setupPreview();
                mState = STATE_PREVIEW;
                rebuildPreviewBuilder();
            }
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

                    mCaptureResult = result;

                    Object time = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
                    if(time != null) frametime = (long)time;
                    cameraEventsListener.onFrameCaptureCompleted(new TimerFrameCountViewModel.FrameCntTime(frameCount, maxFrameCount[0], frametime));
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
                            } catch (InterruptedException ignored) {
                                Log.d(TAG, "Interrupted Exception");
                            }
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

    private void setDigitalZoomFactor(CaptureRequest.Builder captureBuilder) {
        if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.UNLIMITED) || PhotonCamera.getSettings().selectedMode.equals(CameraMode.RAWVIDEO)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (PhotonCamera.getSettings().zoom2X) {
                captureBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, PhotonCamera.getSettings().digitalZoomFactor);
            }
            else {
                if (PhotonCamera.getSettings().showZoomSlider) {
                    captureBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, mDigitalZoom);
                }
                else {
                    if (PhotonCamera.getSettings().videoLogicalWorkaround && PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
                        Map<String, CameraLensData> lensDataMap = mCameraManager2.getCameraLensDataMap();
                        if (lensDataMap != null) {
                            CameraLensData camLensData = lensDataMap.get(PhotonCamera.getSettings().mCameraID);
                            if (camLensData != null) {
                                float test = camLensData.getZoomFactor();
                                captureBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, camLensData.getZoomFactor());
                            }
                        }
                    } else {
                        captureBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, 1.0f);
                    }
                }
            }
        }
    }

    private void applySingleShotSettings(CaptureRequest.Builder captureBuilder) {
        try {
            setAdvancedParameters(captureBuilder, false);
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        VendorTagUtils.builderSessionApply(mCameraCharacteristics, captureBuilder, false, useMaximumResolutionKey, false);

        setDigitalZoomFactor(captureBuilder);

        var CurrHotPixelMode = captureBuilder.get(CaptureRequest.HOT_PIXEL_MODE);
        Log.d(TAG, "HOT_PIXEL_MODE: " + CurrHotPixelMode.toString());

        int[] stabilizationModes = mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION);
        if (stabilizationModes != null && stabilizationModes.length > 1) {
            Log.d(TAG, "LENS_OPTICAL_STABILIZATION_MODE");
            if (PhotonCamera.getSettings().useOis == true) {
                captureBuilder.set(LENS_OPTICAL_STABILIZATION_MODE, LENS_OPTICAL_STABILIZATION_MODE_ON);//Fix ois bugs for preview and burst
            } else {
                captureBuilder.set(LENS_OPTICAL_STABILIZATION_MODE, LENS_OPTICAL_STABILIZATION_MODE_OFF);//Fix ois bugs for preview and burst
            }
        }

        captureBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, PhotonCamera.getSettings().noiseProcessing);
        captureBuilder.set(CaptureRequest.EDGE_MODE, PhotonCamera.getSettings().edgeProcessing);

        if (PhotonCamera.getSettings().useThumbnail) {
            captureBuilder.set(CaptureRequest.JPEG_THUMBNAIL_SIZE, new Size(320, 240));
        }

        if (paramController.isManualMode()) {
            Log.d(TAG, "Applying manual exposure settings.");

            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF);

            int currentISO = (int)paramController.getCurrentISOValue();
            captureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, currentISO);

            long shutterSpeedNs = (long)paramController.getCurrentExposureValue();
            captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, shutterSpeedNs);

            captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_OFF);
            RggbChannelVector gains = new RggbChannelVector(1.0f, 1.0f, 1.0f, 1.0f);
            captureBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, gains);

            Log.d(TAG, "Manual settings applied: ISO=" + currentISO + ", Shutter=" + shutterSpeedNs + "ns");
        }
        //boolean gainMapRes = requestGainMap(captureBuilder, mCameraCharacteristics);
    }

    private void processImageWithLutAndSave(ImageReader reader) {
        if (!mIsProcessingImage.compareAndSet(false, true)) {
            Log.w(TAG, "processImageWithLutAndSave is already running, skipping this frame.");
            try (Image image = reader.acquireLatestImage()) {

            } catch (Exception ignored) {
                Log.e(TAG, "Could not acquire image for LUT processing.");
            }
            return;
        }

        try {
            Image image = reader.acquireLatestImage();
            if (image == null) {
                Log.e(TAG, "Could not acquire image for LUT processing.");
                mIsProcessingImage.set(false);
                return;
            }

            final int sensorWidth = image.getWidth();
            final int sensorHeight = image.getHeight();
            boolean isSideways = (videoRotation == 90 || videoRotation == 270);
            int rotation = getOrientation();
            if (isSideways) {
                rotation = videoRotation;
            }

            if (mMainRenderer != null) {
                Log.d(TAG, "Requesting LUT processing from MainRenderer.");
                mMainRenderer.processYuvImage(image, rotation, (processedData) -> {
                    try {
                        if (processedData != null) {
                            Log.d(TAG, "LUT processing complete, handing data to ImageSaver.");
                            mImageSaver.directSaveImageLut(processedData, sensorHeight, sensorWidth, videoRotation,
                                    PhotonCamera.getSettings().previewFormat, PhotonCamera.getSettings().singleFrameQuality, mMetaData, cameraEventsListener);
                        } else {
                            Log.e(TAG, "LUT processing failed, renderer returned null data.");
                            cameraEventsListener.onProcessingFinished("LUT processing failed, renderer returned null data.");
                        }
                    } finally {
                        mIsProcessingImage.set(false);
                    }
                });
            } else {
                Log.e(TAG, "MainRenderer is null, cannot process image with LUT. Closing image.");
                image.close();
                mIsProcessingImage.set(false);
            }
        }
        catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            mIsProcessingImage.set(false);
        }
    }

    public void suspendAutomatics(CaptureRequest.Builder captureBuilder) {
        if (!mIsFunctionOneOn) {
            return;
        }
        if (PhotonCamera.getSettings().functionOne.equals("Lock Video Params 1") && mIsRecordingVideo) {
            captureBuilder.set(CaptureRequest.CONTROL_AE_LOCK, true);
            captureBuilder.set(CaptureRequest.CONTROL_AWB_LOCK, true);
        }
        if (PhotonCamera.getSettings().functionOne.equals("Lock Video Params 2") && mIsRecordingVideo) {
            if (mPreviewCaptureResult == null) {
                return;
            }

            Integer lastIso = mPreviewCaptureResult.get(CaptureResult.SENSOR_SENSITIVITY);
            Long lastShutterNs = mPreviewCaptureResult.get(CaptureResult.SENSOR_EXPOSURE_TIME);
            Integer postRawBoost = mPreviewCaptureResult.get(CaptureResult.CONTROL_POST_RAW_SENSITIVITY_BOOST);

            if ((lastIso == null) || (lastShutterNs == null)) {
                return;
            }

            String humanReadableShutter = "";
            double seconds = lastShutterNs / 1_000_000_000.0;
            if (seconds >= 1.0) {
                humanReadableShutter.format("%.1fs", seconds);
            } else {
                int denominator = (int) Math.round(1.0 / seconds);
                humanReadableShutter = "1/" + denominator + "s";
            }

            Log.i(TAG, "Lock video params, last ISO=" + lastIso + " - last shutter speed=" + humanReadableShutter + " - last post RAW boost=" + postRawBoost);

            captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            captureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, lastIso);
            captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, lastShutterNs);
            captureBuilder.set(CaptureRequest.CONTROL_POST_RAW_SENSITIVITY_BOOST, postRawBoost);
        }
    }

    public void resumeAutomatics(CaptureRequest.Builder captureBuilder) {
        if (PhotonCamera.getSettings().functionOne.equals("Lock Video Params")) {
            captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
            //captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            setAEMode(mPreviewRequestBuilder, PreferenceKeys.getAeMode());
            //rebuildPreviewBuilder();
        }
    }

    public String getSoCVendor() {
        try {
            String hardware = android.os.Build.HARDWARE.toLowerCase();
            if (hardware.contains("msm") || hardware.contains("sdm") || hardware.contains("qcom") || hardware.contains("qcom")) {
                return "Qualcomm Snapdragon";
            }

            Process process = Runtime.getRuntime().exec("getprop ro.board.platform");
            if (process == null) {
                return "Unknown";
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if (reader == null) {
                return "Unknown";
            }
            String line = reader.readLine();
            if (line == null) {
                reader.close();
                return "Unknown";
            }
            reader.close();

            if (line != null) {
                line = line.toLowerCase();
                if (line.contains("msm") || line.contains("sdm") || line.contains("qcom") || line.contains("kalama") || line.contains("taro") || line.contains("lahaina")) {
                    return "Qualcomm Snapdragon";
                } else if (line.contains("mt") || line.contains("mediatek")) {
                    return "MediaTek";
                } else if (line.contains("exynos") || line.contains("s5e")) {
                    return "Samsung Exynos";
                } else if (line.contains("kirin") || line.contains("hi")) {
                    return "HiSilicon Kirin";
                } else if (line.contains("tensor")) {
                    return "Google Tensor";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    private void processHistogram(@NonNull TotalCaptureResult result) {
        if (!mSocVendor.equals("Qualcomm Snapdragon")) {
            return;
        }

        int bucketSize = 0;
        int maxCountNr = 0;
        int statsSize = 0;
        int statsType = 0;
        int[] histDataArray = null;

        if (wasLogged == 100) {
            List<CaptureResult.Key<?>> keys = result.getKeys();

            int maxKeyLength = 0;
            int maxTypeLength = 0;

            for (CaptureResult.Key<?> key : keys) {
                maxKeyLength = Math.max(maxKeyLength, key.getName().length());
                Object val = result.get(key);
                if (val != null) {
                    maxTypeLength = Math.max(maxTypeLength, val.getClass().getSimpleName().length());
                }
            }

            final int keyPadding = Math.min(maxKeyLength, 150);
            final int typePadding = Math.min(maxTypeLength, 30);
            String formatTemplate = "Key: %-" + keyPadding + "s | Type: %-" + typePadding + "s | Size/Len: %s";

            Log.d(TAG, "--- Start of TotalCaptureResult Keys (Count: " + keys.size() + ") ---");

            for (CaptureResult.Key<?> key : keys) {
                Object value = result.get(key);
                String type = "null";
                String size = "0";

                if (value != null) {
                    type = value.getClass().getSimpleName();
                    if (value.getClass().isArray()) {
                        size = String.valueOf(java.lang.reflect.Array.getLength(value));
                    } else if (value instanceof Collection) {
                        size = String.valueOf(((Collection<?>) value).size());
                    } else {
                        size = "1";
                    }
                }

                Log.d(TAG, String.format(formatTemplate, key.getName(), type, size));
            }
            Log.d(TAG, "--- End of TotalCaptureResult Keys ---");
        }
        wasLogged++;

        try {
            Object histData = result.get(VendorTagUtils.buckets);
            if (histData != null) {
                bucketSize = (int) histData;
            }
            histData = result.get(VendorTagUtils.stats_type);
            if (histData != null) {
                statsType = (int) histData;
            }
            histData = result.get(VendorTagUtils.maxCount);
            if (histData != null) {
                maxCountNr = (int) histData;
            }
            histData = result.get(VendorTagUtils.histogramStats);
            if (histData != null) {
                histDataArray = (int[]) histData;
                statsSize = histDataArray.length;
            }

            if (wasLogged == 100) {
                Log.d(TAG, "Histogram data received: type=" + statsType + " bucket size=" + bucketSize + " max count=" + maxCountNr);
            }

            if ((bucketSize != 0) && (maxCountNr != 0) && (statsSize != 0) && (statsType != 0)) {
                if (cameraEventsListener != null) {
                    cameraEventsListener.onHistogramDataReceived(histDataArray, bucketSize, statsType, maxCountNr);
                }
            }
        }
        catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public String serializeCaptureResult(TotalCaptureResult result) {
        Map<String, Object> metadataMap = new LinkedHashMap<>();
        for (CaptureResult.Key<?> key : result.getKeys()) {
            String keyName = key.getName();

            if (keyName.equals("xiaomi.fd.mifdbeautyparam") ||
                    keyName.equals("com.qti.stats.af.AFSelectionMapTag") ||
                    keyName.equals("org.codeaurora.qcamera3.bayer_grid.r_stats") ||
                    keyName.equals("org.codeaurora.qcamera3.bayer_grid.g_stats") ||
                    keyName.equals("org.codeaurora.qcamera3.bayer_grid.b_stats") ||
                    keyName.equals("org.codeaurora.qcamera3.bayer_exposure.r_stats") ||
                    keyName.equals("org.codeaurora.qcamera3.bayer_exposure.g_stats") ||
                    keyName.equals("org.codeaurora.qcamera3.bayer_exposure.b_stats") ||
                    keyName.equals("com.qti.stats.af.ConfidenceMapTag") ||
                    keyName.equals("com.qti.stats.af.DistanceMapTag") ||
                    keyName.equals("com.qti.stats.af.AFSelectionMapTag") ||
                    keyName.equals("com.qti.stats.af.DefocusMapTag") ||
                    keyName.equals("xiaomi.exifInfo.info") ||
                    keyName.equals("org.codeaurora.qcamera3.histogram.stats")) {
                continue;
            }

            try {
                Object value = result.get(key);
                if (value != null) {
                    if (value.getClass().isArray()) {
                        int length = java.lang.reflect.Array.getLength(value);
                        StringBuilder arrayContent = new StringBuilder("[");
                        for (int i = 0; i < length; i++) {
                            arrayContent.append(java.lang.reflect.Array.get(value, i));
                            if (i < length - 1) arrayContent.append(", ");
                        }
                        arrayContent.append("]");
                        metadataMap.put(keyName, arrayContent.toString());
                    } else {
                        metadataMap.put(keyName, value.toString());
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return new GsonBuilder()
                .setPrettyPrinting()
                .serializeSpecialFloatingPointValues()
                .create()
                .toJson(metadataMap);
    }

    private void captureSingleStillPicture() {
        try {
            if (null == mCameraDevice) {
                Log.e(TAG, "CameraDevice is null, cannot start single shot capture.");
                cameraEventsListener.onProcessingError("CameraDevice is null, cannot start single shot capture.");
                return;
            }

            if (null == mImageReaderRaw) {
                Log.e(TAG, "ImageReader is null, cannot start single shot capture.");
                cameraEventsListener.onProcessingError("ImageReader is null, cannot start single shot capture.");
                return;
            }

            if (mIsCaptureInProgress) {
                Log.e(TAG, "Capture in progress, cannot start another single shot capture.");
                return;
            }

            mIsCaptureInProgress = true;

            CaptureRequest.Builder captureBuilder = null;
            if (PhotonCamera.getSettings().useZsl) {
                captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG);
            }
            else {
                captureBuilder =  mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            }
            captureBuilder.addTarget(mImageReaderRaw.getSurface());
            int rotation = PhotonCamera.getGravity().getCameraRotation(mSensorOrientation);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, rotation);
            applySingleShotSettings(captureBuilder);

            boolean useFlash = (PreferenceKeys.getAeMode() == 2) || (PreferenceKeys.getAeMode() == 3);
            if (useFlash) {
                captureBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE);
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
            }

            CameraCaptureSession.CaptureCallback singleShotCaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    mCaptureResult = result;
                    mLastCaptureResult = serializeCaptureResult(result);
                    if (PreferenceKeys.isCameraSoundsOn()) {
                        final MediaActionSound mCameraSound = new MediaActionSound();
                        if (PreferenceKeys.isCameraSoundsOn()) {
                            mCameraSound.play(MediaActionSound.SHUTTER_CLICK);
                        }
                    }
                    Integer pixelMode = result.get(CaptureResult.SENSOR_PIXEL_MODE);
                    Log.d(TAG, "Single shot capture completed. Sensor Pixel Mode: " + pixelMode);
                }

                @Override
                public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long lastFrameNumber) {
                    super.onCaptureSequenceCompleted(session, sequenceId, lastFrameNumber);
                    Log.d(TAG, "Single shot sequence completed. Unlocking focus.");
                    unlockFocus();
                    mIsCaptureInProgress = false;
                    if (useFlash) {
                        try {
                            mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                            CaptureRequest req = mPreviewRequestBuilder.build();
                            if (req != null) {
                                mCaptureSession.setRepeatingRequest(req, mCaptureCallback, mBackgroundHandler);
                            } else {
                                Log.e(TAG, "mPreviewRequestBuilder.build() failed.");
                            }
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            if (useFlash) {
                captureBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            }

            mCaptureSession.stopRepeating();
            //captureBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<>(7, 7));
            CaptureRequest request = captureBuilder.build();
            mCaptureSession.capture(request, singleShotCaptureCallback, mBackgroundHandler);
            Log.d(TAG, "Single shot capture command sent.");
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to capture single shot picture: " + e.getMessage());
        }
    }

    private void captureStillPicture() {
        try {
            if (null == mCameraDevice) {
                Log.e(TAG, "CameraDevice is null, cannot start still image capture.");
                cameraEventsListener.onProcessingError("CameraDevice is null, cannot start still image capture.");
                return;
            }

            if (null == mImageReaderRaw) {
                Log.e(TAG, "ImageReader is null, cannot start still image capture.");
                cameraEventsListener.onProcessingError("ImageReader is null, cannot start still image capture.");
                return;
            }

            // single shot logic
            if (isSingleShotJpegOrAvifOrHeic()) {
                captureSingleStillPicture();
                return;
            }

            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder;
            if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.RAWVIDEO)) {
                captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                if (PhotonCamera.getSettings().fpsPreview){
                    captureBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, FpsRangeHigh);
                } else {
                    captureBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<>(30, 30));
                }
            } else {
                captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<>(30, 30));
            }
            float focus = mFocus;
            double frametime = ExposureIndex.time2sec(IsoExpoSelector.GenerateExpoPair(-1, this).exposure);

            // QualityDoesMatter
            try {
                setAdvancedParameters(captureBuilder, false);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_ZOOM_METHOD, CaptureRequest.CONTROL_ZOOM_METHOD_ZOOM_RATIO);
            }

            var CurrHotPixelMode = captureBuilder.get(CaptureRequest.HOT_PIXEL_MODE);
            Log.d(TAG, "HOT_PIXEL_MODE: " + CurrHotPixelMode.toString());
            if (isDualSession) {
                if (mTargetFormat != mPreviewTargetFormat)
                    captureBuilder.addTarget(mImageReaderRaw.getSurface());
                else
                    captureBuilder.addTarget(mImageReaderPreview.getSurface());
            } else {
                captureBuilder.addTarget(mImageReaderRaw.getSurface());
                CameraMode selectedMode = PhotonCamera.getSettings().selectedMode;
                if(frametime > 0.06 && !isDualSession || selectedMode == CameraMode.RAWVIDEO || selectedMode == CameraMode.UNLIMITED || (!IsoExpoSelector.HDR)) {
                    captureBuilder.addTarget(mTextureSurface);
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

            for (int i = 0; i < 3; i++) {
                Log.d(TAG, "Temperature:" + mPreviewTemp[i]);
            }
            Log.d(TAG, "CaptureBuilderStarted!");
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, PhotonCamera.getGravity().getCameraRotation(mSensorOrientation));
            VendorTagUtils.builderSessionApply(mCameraCharacteristics, captureBuilder, true, useMaximumResolutionKey, false);
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
            if(mode != null && (int) mode != CaptureRequest.CONTROL_AF_MODE_AUTO || PreferenceKeys.getAfMode() == CaptureRequest.CONTROL_AF_MODE_AUTO && !PhotonCamera.getSettings().selectedMode.equals(CameraMode.RAWVIDEO)) {
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
                captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
            }

            //captureBuilder.set(CaptureRequest.CONTROL_ZOOM_METHOD, CaptureRequest.CONTROL_ZOOM_METHOD_ZOOM_RATIO);
            //captureBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, 2.0f);

            MeteringRectangle rectaf = new MeteringRectangle(0, 0, 0, 0, 0);
            IsoExpoSelector.useTripod = PhotonCamera.getGyro().getTripod();
            if (frameCount == -1) {
                for (int i = 0; i < 1; i++) {
                    if(!PhotonCamera.getSettings().selectedMode.equals(CameraMode.RAWVIDEO))
                        IsoExpoSelector.setExpo(captureBuilder, i, this);
                    else {
                        captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, mPreviewAFMode);
                        captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, mPreviewAEMode);
                    }
                    captures.add(captureBuilder.build());
                }
            } else {
                long[] times = new long[frameCount];
                for (int i = 0; i < frameCount; i++) {
                    IsoExpoSelector.setExpo(captureBuilder, i, this);
                    times[i] = IsoExpoSelector.lastSelectedExposure;
                    if (i >= 1) {
                        captureBuilder.removeTarget(mTextureSurface);
                        captureBuilder.removeTarget(mImageReaderPreview.getSurface());
                        captureBuilder.addTarget(mImageReaderRaw.getSurface());
                    }

                    setSceneAndEffectMode(captureBuilder);
                    CaptureRequest request = captureBuilder.build();
                    captures.add(request);
                    //captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION));
                    mCaptureRequest = request;
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
                    processHistogram(result);
                    mCaptureResult = result;
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

                    Integer whiteLevel = result.get(CaptureResult.SENSOR_DYNAMIC_WHITE_LEVEL);
                    if (whiteLevel != null) {
                        Log.d(TAG, "Dynamic WhiteLevel: " + whiteLevel);
                    } else {
                        // Fallback auf statischen Wert aus den Characteristics
                        Integer staticWhiteLevel = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL);
                        Log.d(TAG, "Static WhiteLevel: " + staticWhiteLevel);
                    }

                    float[] blackLevels = result.get(CaptureResult.SENSOR_DYNAMIC_BLACK_LEVEL);
                    if (blackLevels != null) {
                        Log.d(TAG, "BlackLevel R: " + blackLevels[0] + " Gr: " + blackLevels[1] + " Gb: " + blackLevels[2] + " B: " + blackLevels[3]);
                    } else {
                        BlackLevelPattern staticBlackLevel = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_BLACK_LEVEL_PATTERN);
                        Log.d(TAG, "Static BlackLevel: " + staticBlackLevel.toString());
                    }

                    cameraEventsListener.onFrameCaptureCompleted(new TimerFrameCountViewModel.FrameCntTime(frameCount, maxFrameCount[0], frametime));

                    if (onUnlimited && !unlimitedStarted) {
                        mImageSaver.processStart(mCameraCharacteristics, result, request, cameraRotation);
                        //setupAudioRecorder(PhotonCamera.rawVideoPath + "/audio_track.m4a");
                        unlimitedStarted = true;
                    }
                    if(frameCount == 0)
                        mCaptureResult = result;
                    if (maxFrameCount[0] != -1) PhotonCamera.getGyro().CaptureGyroBurst();

                    mIsCaptureInProgress = false;
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
                    if (PhotonCamera.getSettings().selectedMode != CameraMode.UNLIMITED && PhotonCamera.getSettings().selectedMode != CameraMode.RAWVIDEO) {
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
                                    Log.d(TAG, "Interrupted Exception");
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
                            mIsCaptureInProgress = false;
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
                    case RAWVIDEO:
                        mCaptureSession.setRepeatingRequest(captures.get(0), CaptureCallback, mBackgroundHandler);
                        break;
                    case NIGHT:
                    case PHOTO:
                    case MOTION:
                        //if (isSingleShotJpegOrHeic()) {
                        if (PhotonCamera.getSettings().frameCount == 1) {
                            Log.d(TAG, "number of capture requests: " + Integer.toString(captures.size()));
                            mCaptureSession.capture(captures.get(0), CaptureCallback, mBackgroundHandler);
                        }
                        else {
                            mCaptureSession.captureBurst(captures, CaptureCallback, mBackgroundHandler);
                        }
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
        if (PhotonCamera.getSettings().functionOne.equals("Lock Video Params") && mIsRecordingVideo) {
            return;
        }

        if (aeMode == CONTROL_AE_MODE_ON) {
            Log.d(TAG, "Requested AE Mode: ON");
        }
        else if (aeMode == CONTROL_AE_MODE_OFF) {
            Log.d(TAG, "Requested AE Mode: OFF");
        }

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

    public void setupAudioRecorder(String filePath) {
        AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
        AudioDeviceInfo devicesBack = null;
        AudioDeviceInfo devicesFront = null;

        for (AudioDeviceInfo device : devices) {
            if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
                Log.d("MIC_CHECK", "Microfon ID: " + device.getId() + " | Typ: " + device.getProductName());
                if (device.getAddress().contains("back")) {
                    devicesBack = device;
                }
                if (device.getAddress().contains("bottom")) {
                    devicesFront = device;
                }
            }
        }

        if (mAudioRecorder == null) {
            mAudioRecorder = new MediaRecorder();
        }

        try {
            // 1. Define output file path
            if (filePath.isEmpty()) {
                Path audioFilePath = ImagePath.getNewImageFilePath("m4a");
                filePath = audioFilePath.toString();
            }

            // 2. Configure the MediaRecorder
            mAudioRecorder.setAudioSource(PhotonCamera.getSettings().audioProcessing);
            mAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mAudioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mAudioRecorder.setAudioEncodingBitRate(PhotonCamera.getSettings().audioBitrate * 1024);
            mAudioRecorder.setAudioSamplingRate(PhotonCamera.getSettings().audioSps);
            mAudioRecorder.setAudioChannels(PhotonCamera.getSettings().audioChannels);
            mAudioRecorder.setOutputFile(filePath);

            if ((devicesBack != null) && (devicesFront != null)) {
                if (PhotonCamera.getSettings().audioDirection == MicrophoneDirection.MIC_DIRECTION_AWAY_FROM_USER) {
                    mAudioRecorder.setPreferredDevice(devicesBack);
                } else if (PhotonCamera.getSettings().audioDirection == MicrophoneDirection.MIC_DIRECTION_TOWARDS_USER) {
                    mAudioRecorder.setPreferredDevice(devicesFront);
                }
                boolean retDirection = mAudioRecorder.setPreferredMicrophoneDirection(PhotonCamera.getSettings().audioDirection);
                boolean retZoom = mAudioRecorder.setPreferredMicrophoneFieldDimension(PhotonCamera.getSettings().audioZoom);

                Log.d(TAG, "Audio recording direction shaping request: " + retDirection + " - " + retZoom);
            }

            // 3. Prepare and start recording
            mAudioRecorder.prepare();
            mAudioRecorder.start();
            Log.d(TAG, "Audio recording started, saving to: " + filePath);

            if ((devicesBack != null) && (devicesFront != null)) {
                if (PhotonCamera.getSettings().audioDirection == MicrophoneDirection.MIC_DIRECTION_AWAY_FROM_USER) {
                    mAudioRecorder.setPreferredDevice(devicesBack);
                } else if (PhotonCamera.getSettings().audioDirection == MicrophoneDirection.MIC_DIRECTION_TOWARDS_USER) {
                    mAudioRecorder.setPreferredDevice(devicesFront);
                }
                boolean retDirection = mAudioRecorder.setPreferredMicrophoneDirection(PhotonCamera.getSettings().audioDirection);
                boolean retZoom = mAudioRecorder.setPreferredMicrophoneFieldDimension(PhotonCamera.getSettings().audioZoom);

                Log.d(TAG, "Audio recording direction shaping response: " + retDirection + " - " + retZoom);
            }

        } catch (IOException e) {
            Log.e(TAG, "prepare() failed for audio recording", e);
        }
    }

    public void releaseAudioRecorder() {
        if (mAudioRecorder != null) {
            mAudioRecorder.stop();
            mAudioRecorder.release();
            mAudioRecorder = null;
        }
    }

    public void callUnlimitedEnd() {
        onUnlimited = false;
        //mImageSaver.unlimitedEnd();
        if (!PhotonCamera.getSettings().selectedMode.equals(CameraMode.UNLIMITED)) {
            releaseAudioRecorder();
        }
        mBackgroundHandler.post(() -> mImageSaver.processEnd());
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
        if (cameraEventsListener != null) {
            cameraEventsListener.onVideoRecordingStopped();
        }
        stopRecordingVideo();
    }

    public boolean VideoStart() {
        mIsRecordingVideo = true;
        createCameraPreviewSession(false);
        if ((cameraEventsListener != null) && mIsRecordingVideo) {
            cameraEventsListener.onVideoRecordingStarted(vid, PhotonCamera.getSettings().video10bit, PhotonCamera.getSettings().videoHDR);
        }
        return mIsRecordingVideo;
    }

    private boolean checkColorSpaceProfilesSupport(CameraManager manager) {
        // Diese Capability ist erst ab API 34 (Android 14) verfügbar
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            System.out.println("❌ Device not supporting API 34+");
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
                System.out.println("✅ Camera " + PhotonCamera.getSettings().mCameraID + " supports COLOR_SPACE_PROFILES");
            } else {
                System.out.println("❌ Camera " + PhotonCamera.getSettings().mCameraID + " NOT supporting COLOR_SPACE_PROFILES");
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
            if(cameraId.contains("-")){
                logicalID = cameraId.split("-")[0];
                physicalID = cameraId.split("-")[1];
            } else {
                logicalID = cameraId;
                physicalID = cameraId;
            }

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(physicalID);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            if (map == null) {
                return null;
            }
            Size[] jpegSizes = map.getOutputSizes(ImageFormat.JPEG);
            if (jpegSizes == null || jpegSizes.length == 0) {
                jpegSizes = map.getOutputSizes(PhotonCamera.getSettings().rawFormat);
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

    private int getOrientation() {
        var camId1 = PhotonCamera.getSettings().mCameraID;
        var camId2 = mCameraDevice.getId();
        String physicalID = PhotonCamera.getSettings().mCameraID;
        if(PhotonCamera.getSettings().mCameraID.contains("-")){
            physicalID = PhotonCamera.getSettings().mCameraID.split("-")[1];
        }
        CameraCharacteristics camChar = mCameraCharacteristicsMap.get(physicalID);
        int orientation = 0;
        if (camChar != null) {
            boolean facingFront = camChar.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
            if (facingFront) {
                switch (videoRotation) {
                    case 0:
                        orientation = 270;
                        break;
                    case 90:
                        orientation = 180;
                        break;
                    case 180:
                        orientation = 90;
                        break;
                    case -90:
                        orientation = 0;
                        break;
                }
            } else {
                switch (videoRotation) {
                    case 0:
                        orientation = 90;
                        break;
                    case 90:
                        orientation = 0;
                        break;
                    case 180:
                        orientation = 270;
                        break;
                    case -90:
                        orientation = 180;
                        break;
                }
            }
        }
        return orientation;
    }

    private MediaFormat createAudioFormat() {
        String mimeAud = MediaFormat.MIMETYPE_AUDIO_AAC;
        if (PhotonCamera.getSettings().videoCodec.equals("VP8") || PhotonCamera.getSettings().videoCodec.equals("VP9")) {
            mimeAud = MediaFormat.MIMETYPE_AUDIO_OPUS;
        }
        // create MediaFormat to fill out with audio parameters
        MediaFormat format = MediaFormat.createAudioFormat(mimeAud, PhotonCamera.getSettings().audioSps, PhotonCamera.getSettings().audioChannels);
        format.setInteger(MediaFormat.KEY_BIT_RATE, PhotonCamera.getSettings().audioBitrate * 1024);

        return format;
    }

    private MediaFormat createVideoFormat(MediaCodec codec) {
        Log.d(TAG, "createVideoFormat start");

        // compression related
        String mimeVid = MediaFormat.MIMETYPE_VIDEO_AVC;
        if (PhotonCamera.getSettings().videoCodec.equals("HEVC") || PhotonCamera.getSettings().videoCodec.equals("H265")) {
            mimeVid = MediaFormat.MIMETYPE_VIDEO_HEVC;
        } else if ((PhotonCamera.getSettings().videoCodec.equals("DOLBY_VISION")) || (PhotonCamera.getSettings().videoCodec.equals("DOLBY"))) {
            mimeVid = MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION;
        } else if (PhotonCamera.getSettings().videoCodec.equals("AV1")) {
            mimeVid = MediaFormat.MIMETYPE_VIDEO_AV1;
        } else if (PhotonCamera.getSettings().videoCodec.equals("APV")) {
            mimeVid = MediaFormat.MIMETYPE_VIDEO_APV;
        } else if (PhotonCamera.getSettings().videoCodec.equals("VP8")) {
            mimeVid = MediaFormat.MIMETYPE_VIDEO_VP8;
        } else if (PhotonCamera.getSettings().videoCodec.equals("VP9")) {
            mimeVid = MediaFormat.MIMETYPE_VIDEO_VP9;
        }

        // get max encoder resolution
        Size maxRes = mEncoderInfo.getMaxResForMimeType(mimeVid);
        if (maxRes == null) {
            Log.d(TAG, "encoder getMaxResForMimeType failed");
            return null;
        }

        // resolution related
        int vidHeight = PhotonCamera.getSettings().videoHeight;
        int vidWidth = 2 * 1920;

        if (PhotonCamera.getSettings().videoHeight == 4 * 1080) {
            vidWidth = 4 * 1920;
        } else if (PhotonCamera.getSettings().videoHeight == 2 * 1080) {
            vidWidth = 2 * 1920;
        } else if (PhotonCamera.getSettings().videoHeight == 1080) {
            vidWidth = 1920;
        } else if (PhotonCamera.getSettings().videoHeight == 800) {
            vidWidth = 1920;
        } else if (PhotonCamera.getSettings().videoHeight == 1600) {
            vidWidth = 2 * 1920;
        } else if (PhotonCamera.getSettings().videoHeight == 9999) {
            Size maxSensorRes = getMaxSensorResolution(mCameraManager, PhotonCamera.getSettings().mCameraID);
            if (maxSensorRes != null) {
                vidWidth = maxSensorRes.getWidth();
                vidHeight = maxSensorRes.getHeight();
            } else {
                Log.d(TAG, "getMaxSensorResolution failed");
                return null;
            }
        } else if (PhotonCamera.getSettings().videoHeight == 8888) {
            vidWidth = 6016;
            vidHeight = 4512;
        } else if (PhotonCamera.getSettings().videoHeight == 7777) {
            vidWidth = 7680;
            vidHeight = 5760;
        } else if (PhotonCamera.getSettings().videoHeight == 6666) {
            vidWidth = 8192;
            vidHeight = 6144;
        } else if (PhotonCamera.getSettings().videoHeight == 2304) {
            vidWidth = 4096;
            vidHeight = 2304;
        } else if (PhotonCamera.getSettings().videoHeight == 2296) {
            vidWidth = 4080;
            vidHeight = 2296;
        } else if (PhotonCamera.getSettings().videoHeight == 1836) {
            vidWidth = 3264;
            vidHeight = 1836;
        } else if (PhotonCamera.getSettings().videoHeight == 2608) {
            vidWidth = 4624;
            vidHeight = 2608;
        } else if (PhotonCamera.getSettings().videoHeight == 1584) {
            vidWidth = 2816;
            vidHeight = 1584;
        } else {
            vidWidth = 1280;
        }

        // create MediaFormat to fill out with video parameters
        MediaFormat format = null;
        if (Math.min(vidWidth, maxRes.getWidth()) == 2048) {
            format = MediaFormat.createVideoFormat(mimeVid, 1920, 1920);
            Log.d(TAG, "using recording resolution: 1920x1920 -> fallback from 2024x2024");
        }
        else {
            format = MediaFormat.createVideoFormat(mimeVid, Math.min(vidWidth, maxRes.getWidth()), Math.min(vidHeight, maxRes.getHeight()));
            Log.d(TAG, "using recording resolution: " + Integer.toString(Math.min(vidWidth, maxRes.getWidth())) + "x" + Integer.toString(Math.min(vidHeight, maxRes.getHeight())) + " at " + Integer.toString(PhotonCamera.getSettings().videoFramrate) + "fps");
        }

        //format.setString("camera_application_name", "com.particlesdevs.photonvidcam");
        //format.setInteger("camera_module_id", Integer.valueOf(PhotonCamera.getSettings().mCameraID));
        //format.setString("eis_enabled", PhotonCamera.getSettings().eisPhoto ? "true" : "false");
        //format.setInteger("noise_processing", PhotonCamera.getSettings().noiseReduction);
        //format.setInteger("edge_processing", PhotonCamera.getSettings().edgeProcessing);
        //format.setString("eis_enabled", PhotonCamera.getSettings().eisPhoto ? "true" : "false");

        if (PhotonCamera.getSettings().videoCodec.equals("HEVC") || PhotonCamera.getSettings().videoCodec.equals("H265")) {
            if (PhotonCamera.getSettings().video10bit && PhotonCamera.getSettings().videoHDR) {
                format.setInteger(MediaFormat.KEY_PROFILE, PhotonCamera.getSettings().hdrMode);
            }
            else if (PhotonCamera.getSettings().video10bit && !PhotonCamera.getSettings().videoHDR) {
                format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10);
            }
            format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel52);
            format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
        }
        else if ((PhotonCamera.getSettings().videoCodec.equals("DOLBY_VISION")) || (PhotonCamera.getSettings().videoCodec.equals("DOLBY"))) {
            format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheSt);
            format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.DolbyVisionLevelUhd60);
            format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
        }
        else if (PhotonCamera.getSettings().videoCodec.equals("AVC") || PhotonCamera.getSettings().videoCodec.equals("H264")) {
            format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
            format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel4);
        }
        else if (PhotonCamera.getSettings().videoCodec.equals("VP8")) {
            format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.VP8ProfileMain);
            format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.VP8Level_Version0);
        }
        else if (PhotonCamera.getSettings().videoCodec.equals("VP9")) {
            format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.VP9Profile0);
            format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.VP9Level41);
        }
        else if (PhotonCamera.getSettings().videoCodec.equals("AV1")) {
            if (PhotonCamera.getSettings().video10bit) {
                if (PhotonCamera.getSettings().videoHDR) {
                    format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10);
                }
                else {
                    format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10);
                }
            }
            else {
                format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AV1ProfileMain8);
            }
            format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AV1Level6);
        }
        else if (PhotonCamera.getSettings().videoCodec.equals("APV")) {
            format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
            if (PhotonCamera.getSettings().videoHDR) {
                format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.APVProfile422_10HDR10);
            }
            else {
                format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.APVProfile422_10);
            }
            format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.APVLevel71Band3);
        }

        //final int BUFFER_SIZE_HINT = vidWidth * vidHeight * 3 / 2;
        //format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, BUFFER_SIZE_HINT * 3);

        //format.setFloat(MediaFormat.KEY_FRAME_RATE, PhotonCamera.getSettings().videoFramrate);
        switch (PhotonCamera.getSpecific().specificSetting.newRecSurfaceType) {
            case "COLOR_FormatMonochrome":
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatMonochrome);
                break;
            case "COLOR_FormatYUVP010":
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUVP010);
                break;
            case "COLOR_FormatYUV420Flexible":
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
                break;
            case "COLOR_FormatYUV420Planar":
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
                break;
            case "COLOR_FormatYUV420PackedPlanar":
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar);
                break;
            case "COLOR_FormatYUV420SemiPlanar":
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
                break;
            default:
                if (PhotonCamera.getSettings().video10bit) {
                    format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUVP010);
                } else  {
                    format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                }
                break;
        }
        if ((PhotonCamera.getSettings().videoBitrate * 1024 * 1024) > mEncoderInfo.getMaxBitrateForMimeType(mimeVid)) {
            Log.w(TAG, "selected video bitrate (" + Integer.toString(PhotonCamera.getSettings().videoBitrate) + "MBit/s)exceeds the maximum supported by the encoder (" +
                    mEncoderInfo.getMaxBitrateForMimeType(mimeVid)/(1024*1024) + "MBit/s)");
        }
        format.setInteger(MediaFormat.KEY_BIT_RATE, PhotonCamera.getSettings().videoBitrate * 1024 * 1024);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, PhotonCamera.getSettings().videoFramrate);

        int keyframeIntervalInFrames = PhotonCamera.getSettings().keyframeInterval;
        int videoFramrate = PhotonCamera.getSettings().videoFramrate;
        float intervalInSeconds = (float) keyframeIntervalInFrames / videoFramrate;
        int finalInterval = Math.max(1, Math.round(intervalInSeconds));
        format.setFloat(MediaFormat.KEY_I_FRAME_INTERVAL, intervalInSeconds);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (PhotonCamera.getSettings().video10bit && PhotonCamera.getSettings().videoHDR) {
                format.setFeatureEnabled("hdr-editing", true);
                format.setInteger(MediaFormat.KEY_COLOR_STANDARD, PhotonCamera.getSettings().colorspace);
                var test = PhotonCamera.getSettings().transferFunction;
                format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, PhotonCamera.getSettings().transferFunction);

                if ((PhotonCamera.getSettings().hdrMode == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10) ||
                        (PhotonCamera.getSettings().hdrMode == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus)) {
                    ByteBuffer hdrStaticInfo = ByteBuffer.allocate(25);
                    hdrStaticInfo.order(ByteOrder.LITTLE_ENDIAN);

                    // Mastering Display Color Primaries (e.g., P3 or BT.2020)
                    // For BT.2020 primaries:
                    hdrStaticInfo.putShort(0, (short) (0.708 * 50000));  // Primary R, x
                    hdrStaticInfo.putShort(2, (short) (0.292 * 50000));  // Primary R, y
                    hdrStaticInfo.putShort(4, (short) (0.170 * 50000));  // Primary G, x
                    hdrStaticInfo.putShort(6, (short) (0.797 * 50000));  // Primary G, y
                    hdrStaticInfo.putShort(8, (short) (0.131 * 50000));  // Primary B, x
                    hdrStaticInfo.putShort(10, (short) (0.046 * 50000)); // Primary B, y
                    hdrStaticInfo.putShort(12, (short) (0.3127 * 50000)); // White Point, x
                    hdrStaticInfo.putShort(14, (short) (0.3290 * 50000)); // White Point, y

                    // Mastering Display Max/Min Luminance (in Nits)
                    // These are typical values for high-end mastering displays.
                    hdrStaticInfo.putShort(16, (short) (1000 * 10000));      // Max luminance (e.g., 1000 Nits)
                    hdrStaticInfo.putShort(18, (short) (0.005 * 10000));     // Min luminance (e.g., 0.005 Nits)

                    // Content Light Level Information
                    hdrStaticInfo.putShort(20, (short) 1000); // MaxCLL: Maximum Content Light Level (e.g., 1000 Nits)
                    hdrStaticInfo.putShort(22, (short) 400);  // MaxFALL: Maximum Frame-Average Light Level (e.g., 400 Nits)

                    format.setByteBuffer(MediaFormat.KEY_HDR_STATIC_INFO, hdrStaticInfo);
                }
            }
            else {
                format.setInteger(MediaFormat.KEY_COLOR_STANDARD, PhotonCamera.getSettings().colorspace);
                format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, PhotonCamera.getSettings().transferFunction);
            }
        }
        else {
            format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT709);
            format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_SDR_VIDEO);
        }

        if (PhotonCamera.getSettings().videoRange.equals("Full")) {
            format.setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_FULL);
        }
        else {
            format.setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_LIMITED);
        }

        format.setInteger(MediaFormat.KEY_ROTATION, getOrientation());

        Log.d(TAG, "createVideoFormat done - " + format.toString());
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

    private MediaCodec createVideoCodec() {
        Log.d(TAG, "createVideoCodec start");
        String mimeVid = MediaFormat.MIMETYPE_VIDEO_AVC;
        String codecName = "c2.android.avc.encoder";
        Size maxEncoderRes = null;

        switch (PhotonCamera.getSettings().videoCodec) {
            case "HEVC":
            case "H265":
                mimeVid = MediaFormat.MIMETYPE_VIDEO_HEVC;
                break;
            case "DOLBY_VISION":
            case "DOLBY":
                mimeVid = MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION;
                break;
            case "AV1":
                mimeVid = MediaFormat.MIMETYPE_VIDEO_AV1;
                break;
            case "APV":
                mimeVid = MediaFormat.MIMETYPE_VIDEO_APV;
                break;
            case "VP8":
                mimeVid = MediaFormat.MIMETYPE_VIDEO_VP8;
                break;
            case "VP9":
                mimeVid = MediaFormat.MIMETYPE_VIDEO_VP9;
                break;
            default:
                mimeVid = MediaFormat.MIMETYPE_VIDEO_AVC;
                break;
        }

        MediaCodec videoEncoder = null;
        try {
            if (PhotonCamera.getSettings().videoEncoderName.equals("Device Default") || !mimeVid.equals(MediaFormat.MIMETYPE_VIDEO_HEVC)) {
                videoEncoder = MediaCodec.createEncoderByType(mimeVid);
            }
            else {
                videoEncoder = MediaCodec.createByCodecName(PhotonCamera.getSettings().videoEncoderName);
            }

            //maxEncoderRes = maxEncoderSizes.getMaxResForMimeType(mimeVid);
        }
        catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        Log.d(TAG, "createVideoCodec done - " + videoEncoder.toString());
        return videoEncoder;
    }

    private MediaMuxer createMediaMuxer() {
        Log.d(TAG, "createMediaMuxer start");
        MediaMuxer mediaMuxer = null;
        createRecordingFile();
        try {
            if (PhotonCamera.getSettings().videoCodec.equals("VP8") || PhotonCamera.getSettings().videoCodec.equals("VP9")) {
                mediaMuxer = new MediaMuxer(vid.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM);
            }
            else {
                mediaMuxer = new MediaMuxer(vid.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            }
        }
        catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        if (mediaMuxer != null) {
            mediaMuxer.setOrientationHint(getOrientation());
            if (PhotonCamera.getSettings().gpsLocation && (PhotonCamera.gpsLocation != null)) {
                mediaMuxer.setLocation((float)PhotonCamera.gpsLocation.getLatitude(), (float)PhotonCamera.gpsLocation.getLongitude());
            }
        }
        Log.d(TAG, "createMediaMuxer done - " + mediaMuxer.toString());

        String fullPath = vid.getAbsolutePath();
        int lastDotIndex = fullPath.lastIndexOf('.');
        String audioPath;
        if (lastDotIndex > 0) {
            audioPath = fullPath.substring(0, lastDotIndex);
        } else {
            audioPath = fullPath;
        }
        audioPath += ".m4a";
        setupAudioRecorder(audioPath);

        return mediaMuxer;
    }

    private boolean setUpMediaRecorderNew() {
        Log.d(TAG, "setUpMediaRecorderNew start");
        if (mEncoderData == null) {
            mEncoderData = new RecordingUtils.EncoderData();
        }

        //mAudioFormat = createAudioFormat();
        //mAudioCodec = createAudioCodec(mAudioFormat);
        mVideoCodec = createVideoCodec();
        if (mVideoCodec == null)
        {
            return false;
        }
        mVideoFormat = createVideoFormat(mVideoCodec);
        if (mVideoFormat == null)
        {
            mVideoCodec.release();
            mVideoCodec = null;
            return false;
        }
        mMediaMuxer = createMediaMuxer();
        if (mMediaMuxer == null)
        {
            mVideoFormat = null;
            mVideoCodec.release();
            mVideoCodec = null;
            return false;
        }

        mVideoEncoderCallback = new RecordingUtils.VideoEncoderCallback(mMediaMuxer, mEncoderData);
        if (mVideoEncoderCallback == null) {
            mVideoFormat = null;
            mVideoCodec.release();
            mVideoCodec = null;
            mMediaMuxer.release();
            mMediaMuxer = null;
        }

        //mAudioEncoderCallback = new AudioEncoderCallback(mMediaMuxer, mEncoderData);
        mMuxerThread = new RecordingUtils.MuxerThread(mMediaMuxer);
        mVideoEncoderCallback = new RecordingUtils.VideoEncoderCallback(mMediaMuxer, mEncoderData);
        if (mVideoEncoderCallback == null) {
            mVideoFormat = null;
            mVideoCodec.release();
            mVideoCodec = null;
            mMediaMuxer.release();
            mMediaMuxer = null;
        }

        //mAudioCodec.setCallback(mAudioEncoderCallback);
        //mAudioEncoderCallback.setMuxerThread(mMuxerThread);
        mVideoCodec.setCallback(mVideoEncoderCallback);
        mVideoEncoderCallback.setMuxerThread(mMuxerThread);

        try {
            //mVideoCodec.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            mVideoCodec.configure(mVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            //mAudioCodec.configure(mAudioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodecSurface = mVideoCodec.createInputSurface();
            mVideoCodec.start();
        }
        catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            mVideoFormat = null;
            mVideoCodec.release();
            mVideoCodec = null;
            mMediaMuxer.release();
            mMediaMuxer = null;
            showToast("Invalid Recording Configuration");
            return false;
        }

        Log.d(TAG, "setUpMediaRecorderNew done");
        return true;
    }

    private void releaseMediaRecorderNew() {
        Log.d(TAG, "releaseMediaRecorderNew start");
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
        Log.d(TAG, "releaseMediaRecorderNew done");
    }

    private boolean setUpMediaRecorder() {
        if (PhotonCamera.getSettings().videoCodec.equals("APV")) {
            showToast("APV encoding is not supported by old MediaRecorder based implementation");
            return false;
        }

        // codec selection
        String mimeType = MediaFormat.MIMETYPE_VIDEO_AVC;
        if (PhotonCamera.getSettings().videoCodec.equals("HEVC") || PhotonCamera.getSettings().videoCodec.equals("H265")) {
            mimeType = MediaFormat.MIMETYPE_VIDEO_HEVC;
        }
        else if (PhotonCamera.getSettings().videoCodec.equals("AV1")) {
            mimeType = MediaFormat.MIMETYPE_VIDEO_AV1;
        }
        else if (PhotonCamera.getSettings().videoCodec.equals("VP8")) {
            mimeType = MediaFormat.MIMETYPE_VIDEO_VP8;
        }
        else if (PhotonCamera.getSettings().videoCodec.equals("VP9")) {
            mimeType = MediaFormat.MIMETYPE_VIDEO_VP9;
        }
        else if ((PhotonCamera.getSettings().videoCodec.equals("DOLBY_VISION")) || (PhotonCamera.getSettings().videoCodec.equals("DOLBY"))) {
            mimeType = MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION;
        }

        // check max encoder resolution
        Size maxEncRes = mEncoderInfo.getMaxResForMimeType(mimeType);
        if (maxEncRes == null) {
            return false;
        }

        Size maxSensorRes = getMaxSensorResolution(mCameraManager, PhotonCamera.getSettings().mCameraID);
        if (maxSensorRes == null) {
            return false;
        }

        Log.d(TAG, "setUpMediaRecorder start");
        mVidWidth = 1280;
        mVidHeight = 720;
        mMediaRecorder.reset();
        if (PhotonCamera.getSettings().audioCodec != 0) {
            mMediaRecorder.setAudioSource(PhotonCamera.getSettings().audioProcessing);
        }
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        if (PhotonCamera.getSettings().videoCodec.equals("VP8") || PhotonCamera.getSettings().videoCodec.equals("VP9")) {
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.WEBM);
        }
        else {
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        }

        // codec
        if (PhotonCamera.getSettings().videoCodec.equals("HEVC") || PhotonCamera.getSettings().videoCodec.equals("H265")) {
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.HEVC);
        }
        else if (PhotonCamera.getSettings().videoCodec.equals("AV1")) {
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.AV1);
        }
        else if (PhotonCamera.getSettings().videoCodec.equals("VP8")) {
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.VP8);
        }
        else if (PhotonCamera.getSettings().videoCodec.equals("VP9")) {
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.VP9);
        }
        else if ((PhotonCamera.getSettings().videoCodec.equals("DOLBY_VISION")) || (PhotonCamera.getSettings().videoCodec.equals("DOLBY"))) {
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DOLBY_VISION);
        }
        else {
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        }

        if (PhotonCamera.getSettings().gpsLocation && (PhotonCamera.gpsLocation != null)) {
            mMediaRecorder.setLocation((float)PhotonCamera.gpsLocation.getLatitude(), (float)PhotonCamera.gpsLocation.getLongitude());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (PhotonCamera.getSettings().video10bit) {
                switch (PhotonCamera.getSettings().videoCodec) {
                    case "DOLBY_VISION":
                    case "DOLBY":
                        mMediaRecorder.setVideoEncodingProfileLevel(MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheSt, MediaCodecInfo.CodecProfileLevel.DolbyVisionLevelUhd60);
                        break;
                    case "HEVC":
                    case "H265":
                        if (PhotonCamera.getSettings().videoHDR) {
                            if (PhotonCamera.mHdrTenPlusIsSupported == true) {
                                mMediaRecorder.setVideoEncodingProfileLevel(MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus, MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel62);
                            } else {
                                mMediaRecorder.setVideoEncodingProfileLevel(MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10, MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel62);
                            }
                        } else {
                            mMediaRecorder.setVideoEncodingProfileLevel(MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10, MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel62);
                        }
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
        if (PhotonCamera.getSpecific().specificSetting.enableVideoLut) {
            //mVidWidth = mPreviewSize.getHeight();
            //mVidHeight = mPreviewSize.getWidth();
            mVidWidth = 1080;
            mVidHeight = 1080;
        }
        else if (PhotonCamera.getSettings().videoHeight == 4 * 1080) {
            mVidWidth = 4 * 1920;
            mVidHeight = 4 * 1080;
        }
        else if (PhotonCamera.getSettings().videoHeight == 2 * 1080) {
            mVidWidth = 2 * 1920;
            mVidHeight = 2 * 1080;
        }
        else if (PhotonCamera.getSettings().videoHeight == 1080) {
            mVidWidth = 1920;
            mVidHeight = 1080;
        }
        else if (PhotonCamera.getSettings().videoHeight == 800) {
            mVidWidth = 1920;
            mVidHeight = 800;
        }
        else if (PhotonCamera.getSettings().videoHeight == 2 *800) {
            mVidWidth = 2 * 1920;
            mVidHeight = 2 * 800;
        }
        else if (PhotonCamera.getSettings().videoHeight == 9999) {
            mVidWidth = maxSensorRes.getWidth();
            mVidHeight = maxSensorRes.getHeight();
        }
        else if (PhotonCamera.getSettings().videoHeight == 8888) {
            mVidWidth = 6016;
            mVidHeight = 4512;
        }
        else if (PhotonCamera.getSettings().videoHeight == 7777) {
            mVidWidth = 7680;
            mVidHeight = 5760;
        }
        else if (PhotonCamera.getSettings().videoHeight == 6666) {
            mVidWidth = 8192;
            mVidHeight = 6144;
        }
        else if (PhotonCamera.getSettings().videoHeight == 2304) {
            mVidWidth = 4096;
            mVidHeight = 2304;
        }
        else if (PhotonCamera.getSettings().videoHeight == 2296) {
            mVidWidth = 4080;
            mVidHeight = 2296;
        }
        else if (PhotonCamera.getSettings().videoHeight == 1836) {
            mVidWidth = 3264;
            mVidHeight = 1836;
        }
        else if (PhotonCamera.getSettings().videoHeight == 2608) {
            mVidWidth = 4624;
            mVidHeight = 2608;
        }
        else if (PhotonCamera.getSettings().videoHeight == 1584) {
            mVidWidth = 2816;
            mVidHeight = 1584;
        }

        mMediaRecorder.setVideoFrameRate(PhotonCamera.getSettings().videoFramrate);
        mMediaRecorder.setCaptureRate(PhotonCamera.getSettings().videoFramrate);
        try {
            int keyframeIntervalInFrames = PhotonCamera.getSettings().keyframeInterval;
            int videoFramrate = PhotonCamera.getSettings().videoFramrate;
            float intervalInSeconds = (float) keyframeIntervalInFrames / videoFramrate;
            int finalInterval = Math.max(1, Math.round(intervalInSeconds));

            Method setVideoKeyFrameInterval = RestrictionBypass.getDeclaredMethod(MediaRecorder.class, "setVideoKeyFrameInterval", int.class);
            if (setVideoKeyFrameInterval != null) {
                setVideoKeyFrameInterval.invoke(mMediaRecorder, finalInterval);
                Log.d(TAG, "Successfully set keyframe interval to " + finalInterval + "s via RestrictionBypass.");
            }
            else {
                Log.w(TAG, "setVideoKeyFrameInterval method not found even with RestrictionBypass. Skipping.");
            }
        } catch (NoSuchMethodException e) {
            Log.w(TAG, "setVideoKeyFrameInterval method not found even with RestrictionBypass. Skipping.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set keyframe interval via RestrictionBypass.", e);
        }

        if (Math.min(mVidWidth, maxEncRes.getWidth()) == 2048) {
            mMediaRecorder.setVideoSize(1920, 1920);
            Log.d(TAG, "using recording resolution: 1920x1920 -> fallback from 2024x2024");
        }
        else {
            Log.d(TAG, "using recording resolution: " + Integer.toString(Math.min(mVidWidth, maxEncRes.getWidth())) + "x" + Integer.toString(Math.min(mVidHeight, maxEncRes.getHeight())));
            mMediaRecorder.setVideoSize(Math.min(mVidWidth, maxEncRes.getWidth()), Math.min(mVidHeight, maxEncRes.getHeight()));
        }
        if ((PhotonCamera.getSettings().videoBitrate * 1024 * 1024) > mEncoderInfo.getMaxBitrateForMimeType(mimeType)) {
            Log.w(TAG, "selected video bitrate (" + Integer.toString(PhotonCamera.getSettings().videoBitrate) + "MBit/s)exceeds the maximum supported by the encoder (" +
                    mEncoderInfo.getMaxBitrateForMimeType(mimeType)/(1024*1024) + "MBit/s)");
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
        mMediaRecorder.setOrientationHint(getOrientation());

        createRecordingFile();
        mMediaRecorder.setOutputFile(vid.getAbsolutePath());
        try {
            mMediaRecorder.prepare();
            Log.d(TAG, "video record start");

        } catch (Exception e) {
            mIsRecordingVideo = false;
            Log.e(TAG, "video record failed");
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
        Log.d(TAG, "setUpMediaRecorder done");

        return true;
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

        String extension = ".mp4";
        if (PhotonCamera.getSettings().videoCodec.equals("VP8") || PhotonCamera.getSettings().videoCodec.equals("VP9")) {
            extension = ".webm";
        }
        if (!PhotonCamera.getSpecific().specificSetting.recPrefix.isEmpty()) {
            vid = new File(dir.getAbsolutePath(), PhotonCamera.getSpecific().specificSetting.recPrefix + dateText + "_ID" + PhotonCamera.getSettings().mCameraID.toString() + addOptions + extension);
        }
        else
        {
            vid = new File(dir.getAbsolutePath(), "PVC_" + dateText + "_ID" + PhotonCamera.getSettings().mCameraID.toString() + addOptions + extension);
        }
        try {
            vid.createNewFile();
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private void stopRecordingVideo() {
        Log.d(TAG, "stop video recording");
        mIsRecordingVideo = false;
        mMainRenderer.setVideoRecordingSurface(null, 0, 0);

        releaseAudioRecorder();

        if (PhotonCamera.getSettings().videoNewRec) {
            releaseMediaRecorderNew();
        }
        else {
            try {
                mMediaRecorder.stop();
            } catch (Exception stopFailure) {
                Log.e(TAG, "Failed to stop recording " + Log.getStackTraceString(stopFailure));
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

    public void setMainRenderer(com.particlesdevs.photoncamera.ui.camera.views.viewfinder.MainRenderer renderer) {
        mMainRenderer = renderer;
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

    public void createVendorKeysList() {
        if (PhotonCamera.vendorKeysMap == null) {
            PhotonCamera.vendorKeysMap = new HashMap<>();
        } else {
            PhotonCamera.vendorKeysMap.clear();
        }

        List<CaptureRequest.Key<?>> requestKeys = CaptureController.mCameraCharacteristics.getAvailableCaptureRequestKeys();
        if (requestKeys != null) {
            for (CaptureRequest.Key<?> key : requestKeys) {
                String keyName = key.getName();

                Class<?> type = null;
                try {
                    // Deep search: Check all fields of the Key and its potential internal Key delegate
                    java.lang.reflect.Field[] fields = key.getClass().getDeclaredFields();
                    for (java.lang.reflect.Field f : fields) {
                        f.setAccessible(true);
                        Object val = f.get(key);
                        if (val instanceof Class) {
                            type = (Class<?>) val;
                            break;
                        }
                        // If it's the internal CameraMetadataNative.Key (common in newer Android)
                        if (val != null && val.getClass().getName().contains("Key")) {
                            for (java.lang.reflect.Field f2 : val.getClass().getDeclaredFields()) {
                                f2.setAccessible(true);
                                Object val2 = f2.get(val);
                                if (val2 instanceof Class) {
                                    type = (Class<?>) val2;
                                    break;
                                }
                            }
                        }
                        if (type != null) break;
                    }
                } catch (Exception ignored) {}

                String keyType = "???";
                if (type != null) {
                    keyType = type.getSimpleName()
                            .replace("Integer", "Int32")
                            .replace("Long", "Int64");

                    if (type.isArray()) {
                        keyType = type.getComponentType().getSimpleName()
                                .replace("Integer", "Int32")
                                .replace("Long", "Int64") + "[]";
                    }
                }

                PhotonCamera.vendorKeysMap.put(keyName, keyType);
            }
        }
    }

    public void checkTenBitAndHdr() {
        // Werte zurücksetzen
        PhotonCamera.hasHdr = false;
        PhotonCamera.hasTenBit = false;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Log.d(TAG, "10-bit and HDR checks require API 30+");
            return;
        }

        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        for (MediaCodecInfo codecInfo : codecList.getCodecInfos()) {
            if (!codecInfo.isEncoder() || !codecInfo.isHardwareAccelerated()) {
                continue;
            }

            for (String type : codecInfo.getSupportedTypes()) {
                if (!type.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_HEVC) &&
                        !type.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_AV1)) {
                    continue;
                }

                MediaCodecInfo.CodecCapabilities caps;
                try {
                    caps = codecInfo.getCapabilitiesForType(type);
                } catch (IllegalArgumentException e) {
                    continue;
                }

                if (caps == null || caps.profileLevels == null) {
                    continue;
                }

                for (MediaCodecInfo.CodecProfileLevel profileLevel : caps.profileLevels) {
                    if (profileLevel.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10 ||
                            profileLevel.profile == MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10) {
                        PhotonCamera.hasTenBit = true;
                    }

                    if (profileLevel.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10 ||
                            profileLevel.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus ||
                            profileLevel.profile == MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10 ||
                            profileLevel.profile == MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10Plus) {
                        PhotonCamera.hasHdr = true;
                    }
                }
            }
        }

        Log.i(TAG, "Device Support Check - Has 10-Bit: " + PhotonCamera.hasTenBit + ", Has HDR: " + PhotonCamera.hasHdr);
    }

    public void resumeCamera() {
        mSocVendor = getSoCVendor();
        mEncoderInfo.getEncoderInfos();
        checkTenBitAndHdr();
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

    static public class EncoderInfoUtil {
        List<String> mimeTypes = new ArrayList<String>();;
        final Map<String, Size> maxResolutions = new HashMap<>();
        final Map<String, Integer> maxBitrates = new HashMap<>();
        final Map<String, Boolean> hwSupports = new HashMap<>();
        final Map<String, String> encoderNames = new HashMap<>();

        public EncoderInfoUtil() {
            mimeTypes.add(MediaFormat.MIMETYPE_VIDEO_AVC);
            mimeTypes.add(MediaFormat.MIMETYPE_VIDEO_HEVC);
            mimeTypes.add(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION);
            mimeTypes.add(MediaFormat.MIMETYPE_VIDEO_VP8);
            mimeTypes.add(MediaFormat.MIMETYPE_VIDEO_VP9);
            mimeTypes.add(MediaFormat.MIMETYPE_VIDEO_AV1);
            mimeTypes.add(MediaFormat.MIMETYPE_VIDEO_APV);
        }

        public void getEncoderInfos() {maxResolutions.clear();
            maxBitrates.clear();
            hwSupports.clear();
            encoderNames.clear();

            MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
            MediaCodecInfo[] codecInfos = codecList.getCodecInfos();

            for (String mimeType : mimeTypes) {
                MediaCodecInfo bestCodec = null;
                boolean bestIsHardware = false;

                for (MediaCodecInfo codecInfo : codecInfos) {
                    if (!codecInfo.isEncoder()) {
                        continue;
                    }

                    try {
                        codecInfo.getCapabilitiesForType(mimeType);
                    } catch (IllegalArgumentException e) {
                        continue;
                    }

                    boolean isHardware = codecInfo.isHardwareAccelerated();

                    if (bestCodec == null || (isHardware && !bestIsHardware)) {
                        bestCodec = codecInfo;
                        bestIsHardware = isHardware;
                    }
                }

                if (bestCodec != null) {
                    try {
                        MediaCodecInfo.CodecCapabilities caps = bestCodec.getCapabilitiesForType(mimeType);
                        if (caps != null && caps.getVideoCapabilities() != null) {
                            MediaCodecInfo.VideoCapabilities videoCaps = caps.getVideoCapabilities();

                            Size maxSize = new Size(videoCaps.getSupportedWidths().getUpper(), videoCaps.getSupportedHeights().getUpper());

                            maxResolutions.put(mimeType, maxSize);
                            maxBitrates.put(mimeType, videoCaps.getBitrateRange().getUpper());
                            hwSupports.put(mimeType, bestIsHardware);
                            encoderNames.put(mimeType, bestCodec.getName());
                        }
                    } catch (Exception e) {

                    }
                }
            }

            for (String mimeType : encoderNames.keySet()) {
                String name = encoderNames.get(mimeType);
                Size resolution = maxResolutions.get(mimeType);
                Integer bitrate = maxBitrates.get(mimeType);
                Boolean hwSupport = hwSupports.get(mimeType);

                // Baue den Log-String zusammen
                String logMessage = "VideoEncoderName: " + name +
                        " - mimeType: " + mimeType +
                        " - max encoder resolution: " + (resolution != null ? resolution.toString() : "N/A") +
                        " - max bitrate: " + (bitrate != null ? (bitrate / (1024 * 1024)) + "MBit/s" : "N/A") +
                        " - HW supported: " + (hwSupport != null ? hwSupport.toString() : "N/A");

                Log.d(TAG, logMessage);
            }
        }

        public Size getMaxResForMimeType(String mimeType) {
            return maxResolutions.get(mimeType);
        }
        public Integer getMaxBitrateForMimeType(String mimeType) {
            return maxBitrates.get(mimeType);
        }

        public String getEncoderNameForMimeType(String mimeType) {
            return encoderNames.get(mimeType);
        }

        public Boolean getHwSupportForMimeType(String mimeType) {
            var hwSupport = hwSupports.get(mimeType);
            if (hwSupport == null) {
                return false;
            }
            else {
                return hwSupport;
            }
        }
    }

    /**
     * Compares two {@code Size}s, prioritizing 4:3 aspect ratios, then by area.
     */
    static class CompareSizesByAreaFourByThree implements Comparator<Size> {

        private static final float TARGET_ASPECT_RATIO = 4.0f / 3.0f;
        private static final float ASPECT_RATIO_TOLERANCE = 0.01f;

        private boolean isFourByThree(Size size) {
            float ratio = (float) size.getWidth() / size.getHeight();
            return Math.abs(ratio - TARGET_ASPECT_RATIO) < ASPECT_RATIO_TOLERANCE;
        }

        @Override
        public int compare(Size lhs, Size rhs) {
            boolean lhsIsTarget = isFourByThree(lhs);
            boolean rhsIsTarget = isFourByThree(rhs);

            if (lhsIsTarget && !rhsIsTarget) {
                return -1; // Prefer lhs
            }
            if (!lhsIsTarget && rhsIsTarget) {
                return 1; // Prefer rhs
            }

            // If both are the target ratio or neither are, compare by area (descending to get largest)
            return Long.signum((long) rhs.getWidth() * rhs.getHeight() -
                    (long) lhs.getWidth() * lhs.getHeight());
        }
    }

    /**
     * Compares two {@code Size}s, prioritizing 16:9 aspect ratios, then by area.
     */
    static class CompareSizesByAreaSixteenByNine implements Comparator<Size> {

        private static final float TARGET_ASPECT_RATIO = 16.0f / 9.0f;
        private static final float ASPECT_RATIO_TOLERANCE = 0.01f;

        private boolean isSixteenByNine(Size size) {
            float ratio = (float) size.getWidth() / size.getHeight();
            return Math.abs(ratio - TARGET_ASPECT_RATIO) < ASPECT_RATIO_TOLERANCE;
        }

        @Override
        public int compare(Size lhs, Size rhs) {
            boolean lhsIsTarget = isSixteenByNine(lhs);
            boolean rhsIsTarget = isSixteenByNine(rhs);

            if (lhsIsTarget && !rhsIsTarget) {
                return -1; // Prefer lhs
            }
            if (!lhsIsTarget && rhsIsTarget) {
                return 1; // Prefer rhs
            }

            // If both are the target ratio or neither are, compare by area (descending to get largest)
            return Long.signum((long) rhs.getWidth() * rhs.getHeight() -
                    (long) lhs.getWidth() * lhs.getHeight());
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