package com.particlesdevs.photoncamera.api;

import android.annotation.SuppressLint;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.BlackLevelPattern;
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.OutputConfiguration;
import android.media.Image;
import android.os.Handler;
import com.particlesdevs.photoncamera.util.Log;
import android.view.Surface;

import com.particlesdevs.photoncamera.capture.CaptureController;

import org.chickenhook.restrictionbypass.RestrictionBypass;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CameraReflectionApi {
    /**
     * Get camera characteristics keys using RestrictionBypass to access protected getKeys method
     */
    public static ArrayList<Object> getCameraCharacteristicsKeys(
            CameraCharacteristics cameraCharacteristics,
            int[] filterTags,
            boolean includeSynthetic) {
        try {
            Class<?> keyClass = Class.forName("android.hardware.camera2.CameraCharacteristics$Key");
            Method getKeysMethod = RestrictionBypass.getDeclaredMethod(
                    CameraMetadata.class,
                    "getKeys",
                    Class.class,
                    Class.class,
                    CameraMetadata.class,
                    int[].class,
                    boolean.class
            );

            if (getKeysMethod == null) return null;
            getKeysMethod.setAccessible(true);

            @SuppressWarnings("unchecked")
            ArrayList<Object> result = (ArrayList<Object>) getKeysMethod.invoke(
                    cameraCharacteristics,
                    cameraCharacteristics.getClass(),
                    keyClass,
                    cameraCharacteristics,
                    filterTags,
                    includeSynthetic
            );

            Log.i("CameraAPI", "Successfully called getKeys method using RestrictionBypass");
            return result;
        } catch (Exception e) {
            Log.e("CameraAPI", "Error calling getCameraCharacteristicsKeys: " + e.getMessage());
            return null;
        }
    }

    public static ArrayList<Object> getCaptureRequestKeys(
            CaptureRequest captureRequest,
            int[] filterTags,
            boolean includeSynthetic) {
        try {
            Class<?> keyClass = Class.forName("android.hardware.camera2.CaptureRequest$Key");
            Method getKeysMethod = RestrictionBypass.getDeclaredMethod(
                    CameraMetadata.class,
                    "getKeys",
                    Class.class,
                    Class.class,
                    CameraMetadata.class,
                    int[].class,
                    boolean.class
            );

            if (getKeysMethod == null) return null;
            getKeysMethod.setAccessible(true);

            @SuppressWarnings("unchecked")
            ArrayList<Object> result = (ArrayList<Object>) getKeysMethod.invoke(
                    captureRequest,
                    captureRequest.getClass(),
                    keyClass,
                    captureRequest,
                    filterTags,
                    includeSynthetic
            );

            Log.i("CameraAPI", "Successfully called getKeys method using RestrictionBypass");
            return result;
        } catch (Exception e) {
            Log.e("CameraAPI", "Error calling getCaptureRequestKeys: " + e.getMessage());
            return null;
        }
    }

    public static ArrayList<Object> getCaptureResultKeys(
            CaptureResult captureResult,
            int[] filterTags,
            boolean includeSynthetic) {
        try {
            Class<?> keyClass = Class.forName("android.hardware.camera2.CaptureResult$Key");
            Method getKeysMethod = RestrictionBypass.getDeclaredMethod(
                    CameraMetadata.class,
                    "getKeys",
                    Class.class,
                    Class.class,
                    CameraMetadata.class,
                    int[].class,
                    boolean.class
            );

            if (getKeysMethod == null) return null;
            getKeysMethod.setAccessible(true);

            @SuppressWarnings("unchecked")
            ArrayList<Object> result = (ArrayList<Object>) getKeysMethod.invoke(
                    captureResult,
                    captureResult.getClass(),
                    keyClass,
                    captureResult,
                    filterTags,
                    includeSynthetic
            );

            Log.i("CameraAPI", "Successfully called getKeys method using RestrictionBypass");
            return result;
        } catch (Exception e) {
            Log.e("CameraAPI", "Error calling getCaptureResultKeys: " + e.getMessage());
            return null;
        }
    }

    public static <T> void set(CameraCharacteristics characteristics, CameraCharacteristics.Key<T> key, T value) {
        try {
            //Class<?> metadataNativeClass = ReflectBypass.findClass("android/hardware/camera2/impl/CameraMetadataNative");
            Field CameraMetadataNativeField = NativeEngine.getCameraField(CameraCharacteristics.class, "mProperties");
            CameraMetadataNativeField.setAccessible(true);
            Object CameraMetadataNative = CameraMetadataNativeField.get(characteristics);
            assert CameraMetadataNative != null;
            Method set = NativeEngine.getCameraMethod(CameraMetadataNative.getClass(), "setBase", CameraCharacteristics.Key.class, Object.class);
            set.setAccessible(true);
            set.invoke(CameraMetadataNative, key, value);
        } catch (Exception e) {
            Log.d("CameraAPI", "Failed to set CameraCharacteristics key:" + Log.getStackTraceString(e));
        }
    }

    public static <T> void set(CaptureResult.Key<T> key, T value) {
        try {
            Field CameraMetadataNativeField = NativeEngine.getCameraField(CaptureResult.class, "mResults");
            CameraMetadataNativeField.setAccessible(true);
            Object CameraMetadataNative = CameraMetadataNativeField.get(CaptureController.mCaptureResult);
            assert CameraMetadataNative != null;
            Method set = NativeEngine.getCameraMethod(CameraMetadataNative.getClass(), "set", CaptureResult.Key.class, Object.class);
            set.setAccessible(true);
            set.invoke(CameraMetadataNative, key, value);
        } catch (Exception e) {
            Log.d("CameraAPI", "Failed to set CaptureResult key:" + Log.getStackTraceString(e));
        }
    }

    public static <T> void set(CaptureResult.Key<T> key, T value, CaptureResult res) {
        try {
            Field CameraMetadataNativeField = NativeEngine.getCameraField(CaptureResult.class, "mResults");
            CameraMetadataNativeField.setAccessible(true);
            Object CameraMetadataNative = CameraMetadataNativeField.get(res);
            assert CameraMetadataNative != null;
            Method set = NativeEngine.getCameraMethod(CameraMetadataNative.getClass(), "set", CaptureResult.Key.class, Object.class);
            set.setAccessible(true);
            set.invoke(CameraMetadataNative, key, value);
        } catch (Exception e) {
            Log.d("CameraAPI", "Failed to set CaptureResult key:" + Log.getStackTraceString(e));
        }
    }

    public static <T> void set(CaptureRequest request, CaptureRequest.Key<T> key, T value) {
        try {
            Field CameraMetadataNativeField = NativeEngine.getCameraField(CaptureRequest.class, "mLogicalCameraSettings");
            CameraMetadataNativeField.setAccessible(true);
            Object CameraMetadataNative = CameraMetadataNativeField.get(request);
            assert CameraMetadataNative != null;
            Method set = NativeEngine.getCameraMethod(CameraMetadataNative.getClass(), "set", CaptureRequest.Key.class, Object.class);
            set.setAccessible(true);
            set.invoke(CameraMetadataNative, key, value);
        } catch (Exception e) {
            Log.d("CameraAPI", "Failed to set CaptureRequest key:" + Log.getStackTraceString(e));
        }
    }

    public static ByteBuffer replaceImageBuffer(Image.Plane plane, ByteBuffer buffer) {
        ByteBuffer oldBuffer = null;
        try {
            Field mNativeBufferField = NativeEngine.getCameraField(plane.getClass(), "mBuffer");
            mNativeBufferField.setAccessible(true);
            oldBuffer = (ByteBuffer) mNativeBufferField.get(plane);
            mNativeBufferField.set(plane, buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return oldBuffer;
    }

    public static void createCustomCaptureSession(CameraDevice cameraDevice,
                                                  InputConfiguration inputConfig,
                                                  List<OutputConfiguration> outputs,
                                                  int operatingMode,
                                                  CameraCaptureSession.StateCallback callback,
                                                  Handler handler) throws CameraAccessException, InvocationTargetException, IllegalAccessException {
        try {
            Method createCustomCaptureSession = NativeEngine.getCameraMethod(cameraDevice.getClass(), "createCustomCaptureSession",
            //createCustomCaptureSession = cameraDevice.getClass().getDeclaredMethod("createCustomCaptureSession",
                    InputConfiguration.class,List.class,Integer.TYPE,CameraCaptureSession.StateCallback.class,Handler.class);
            createCustomCaptureSession.setAccessible(true);
            createCustomCaptureSession.invoke(cameraDevice,inputConfig,outputs,operatingMode,callback,handler);
        } catch (Exception e) {
            Log.d("CameraAPI", "Failed to find createCustomCaptureSession method:" + Log.getStackTraceString(e));
        }
    }

    public static void PatchBL(BlackLevelPattern pattern, int[] bl) {
        try {
            //noinspection JavaReflectionMemberAccess
            @SuppressLint("SoonBlockedPrivateApi") Field mCfaOffsetsField = pattern.getClass().getDeclaredField("mCfaOffsets");
            mCfaOffsetsField.setAccessible(true);
            Object mCfaOffsets = mCfaOffsetsField.get(pattern);
            for (int i = 0; i < 4; i++) {
                assert mCfaOffsets != null;
                Array.set(mCfaOffsets, i, bl[i]);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.d("CameraAPI", "Failed to patch black level pattern:" + Log.getStackTraceString(e));
        }

    }

    public static Field[] getAllMetadataFields() {
        return CameraMetadata.class.getDeclaredFields();
    }

    private static CaptureResult.Key<byte[]> sHdr10PlusOemKey;

    public static byte[] getHdr10PlusOem(TotalCaptureResult result) {
        // Use a static variable to cache the reflective lookup for performance.
        // This avoids doing the expensive reflection work for every single frame.
        if (sHdr10PlusOemKey == null) {
            try {
                // Get the hidden STATICS_HDR10_PLUS_OEM field from the CaptureResult class.
                Field field = RestrictionBypass.getDeclaredField(CaptureResult.class, "STATISTICS_HDR10_PLUS_OEM");
                // Since it's a static field, the first argument to get() is null.
                sHdr10PlusOemKey = (CaptureResult.Key<byte[]>) field.get(null);
            } catch (Exception e) {
                // If it fails once, it will likely always fail. Log an error.
                Log.e("CameraAPI", "Failed to get CaptureResult.Key for STATISTICS_HDR10_PLUS_OEM", e);
                // Return null to indicate that the key is not available.
                return null;
            }
        }

        // If the key was successfully retrieved, use it to get the value from the result.
        if (sHdr10PlusOemKey != null) {
            return result.get(sHdr10PlusOemKey);
        }

        return null;
    }
}
