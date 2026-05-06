package com.particlesdevs.photoncamera.api;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Native engine for accessing hidden Android APIs through JNI reflection
 */
public class NativeEngine {
    private static final String TAG = "NativeEngine";
    private static boolean initialized = false;
    
    static {
        System.loadLibrary("camera2native");
    }
    
    /**
     * Initialize the native engine and set up hidden API exemptions
     * Must be called early in the app lifecycle
     */
    public static synchronized void initialize() {
        if (!initialized) {
            Log.d(TAG, "Initializing NativeEngine and setting up hidden API exemptions");
            nativeInitialize();
            initialized = true;
        }
    }
    
    /**
     * Get a camera API method from a class for metadata access
     * @param clazz The class containing the method
     * @param methodName The name of the method
     * @param parameterTypes The parameter types of the method
     * @return The Method object, or null if not found
     */
    public static Method getCameraMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        // Ensure initialization happens before any camera operations
        initialize();
        
        if (clazz == null || methodName == null) {
            Log.e(TAG, "getCameraMethod: class or method name is null");
            return null;
        }
        
        try {
            // First try using native camera method resolver
            Method method = nativeGetCameraMethod(clazz, methodName, parameterTypes);
            if (method != null) {
                return method;
            }
            
            // Fallback to standard camera API access if native method fails
            Log.d(TAG, "Native camera method failed, using standard access");
            method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
            
        } catch (Exception e) {
            Log.e(TAG, "Error accessing camera method: " + methodName, e);
            return null;
        }
    }
    
    /**
     * Get a camera metadata field from a class for key access
     * @param clazz The class containing the field
     * @param fieldName The name of the field
     * @return The Field object, or null if not found
     */
    public static Field getCameraField(Class<?> clazz, String fieldName) {
        // Ensure initialization happens before any camera operations
        initialize();
        
        if (clazz == null || fieldName == null) {
            Log.e(TAG, "getCameraField: class or field name is null");
            return null;
        }
        
        try {
            // First try using native camera field resolver
            Field field = nativeGetCameraField(clazz, fieldName);
            if (field != null) {
                return field;
            }
            
            // Fallback to standard camera API access if native method fails
            Log.d(TAG, "Native camera field failed, using standard access");
            field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
            
        } catch (Exception e) {
            Log.e(TAG, "Error accessing camera field: " + fieldName, e);
            return null;
        }
    }
    
    /**
     * Native method to initialize camera subsystem
     */
    private static native void nativeInitialize();
    
    /**
     * Native method to resolve camera API methods
     */
    private static native Method nativeGetCameraMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes);
    
    /**
     * Native method to resolve camera metadata fields
     */
    private static native Field nativeGetCameraField(Class<?> clazz, String fieldName);
}
