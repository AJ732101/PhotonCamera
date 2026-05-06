package com.particlesdevs.photoncamera.settings;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import com.particlesdevs.photoncamera.util.Log;
import com.particlesdevs.photoncamera.settings.annotations.Tunable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager for tunable settings - provides reset functionality
 */
public class TunableSettingsManager {
    private static final String TAG = "TunableSettingsMgr";
    private static final List<Class<?>> REGISTERED_CLASSES = new ArrayList<>();
    private static boolean autoRegistered = false;
    
    /**
     * Register a class for tunable management
     */
    public static void registerClass(Class<?> clazz) {
        if (!REGISTERED_CLASSES.contains(clazz)) {
            REGISTERED_CLASSES.add(clazz);
        }
    }
    
    /**
     * Automatically register all tunable classes.
     * This ensures classes are registered even if the tunable settings screen is never opened.
     */
    public static void ensureTunableClassesRegistered() {
        if (autoRegistered) {
            return; // Already registered
        }
        
        Log.d(TAG, "Auto-registering tunable classes...");
        
        for (Class<?> clazz : TunableRegistry.TUNABLE_CLASSES) {
            registerClass(clazz);
        }
        
        autoRegistered = true;
        Log.d(TAG, "Auto-registered " + REGISTERED_CLASSES.size() + " tunable classes");
    }
    
    /**
     * Reset all tunable preferences to their default values by removing persisted values.
     * This allows annotation defaults to always be used for non-customized values.
     */
    public static void resetAllToDefaults(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        
        int resetCount = 0;
        
        for (Class<?> clazz : REGISTERED_CLASSES) {
            String className = clazz.getSimpleName();
            
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Tunable.class)) {
                    Tunable annotation = field.getAnnotation(Tunable.class);
                    if (annotation == null) continue;
                    
                    String prefKey = "pref_tunable_" + className.toLowerCase() + "_" + field.getName().toLowerCase();
                    
                    // Remove the persisted value instead of setting to default
                    // This ensures the annotation's current default is always used
                    if (prefs.contains(prefKey)) {
                        editor.remove(prefKey);
                        resetCount++;
                        
                        // Get default value for logging
                        float defaultValue = annotation.defaultValue();
                        if (defaultValue == -999999f) {
                            defaultValue = annotation.min();
                        }
                        
                        Log.d(TAG, "Reset " + prefKey + " (removed persisted value, will use annotation default: " + defaultValue + ")");
                    }
                }
            }
        }
        
        editor.apply();
        Log.d(TAG, "Reset " + resetCount + " tunable preferences (removed persisted values)");
    }
    
    /**
     * Get count of registered tunable classes
     */
    public static int getRegisteredClassCount() {
        return REGISTERED_CLASSES.size();
    }
    
    /**
     * Export tunable settings to a map (only values that differ from defaults)
     * @param context Context for SharedPreferences
     * @return Map of tunable settings with format: "ClassName.fieldName" -> value
     */
    public static Map<String, Object> exportTunableSettings(Context context) {
        Map<String, Object> tunableSettings = new HashMap<>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        for (Class<?> clazz : REGISTERED_CLASSES) {
            String className = clazz.getSimpleName();
            
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Tunable.class)) {
                    Tunable annotation = field.getAnnotation(Tunable.class);
                    if (annotation == null) continue;
                    
                    String prefKey = "pref_tunable_" + className.toLowerCase() + "_" + field.getName().toLowerCase();
                    String settingKey = className + "." + field.getName();
                    
                    // Get default value from annotation
                    float defaultValue = annotation.defaultValue();
                    if (defaultValue == -999999f) {
                        defaultValue = annotation.min();
                    }
                    
                    // Auto-detect if float based on step value
                    float step = annotation.step();
                    boolean isFloat = (step != Math.floor(step));
                    
                    // Get current value as native type
                    float currentValue;
                    boolean hasValue;
                    if (isFloat) {
                        hasValue = prefs.contains(prefKey);
                        currentValue = prefs.getFloat(prefKey, defaultValue);
                    } else {
                        hasValue = prefs.contains(prefKey);
                        currentValue = (float) prefs.getInt(prefKey, (int) defaultValue);
                    }
                    
                    // Only export if value differs from default
                    if (hasValue && Math.abs(currentValue - defaultValue) > 0.0001f) {
                        tunableSettings.put(settingKey, currentValue);
                        Log.d(TAG, "Exporting tunable: " + settingKey + " = " + currentValue + 
                            " (default: " + defaultValue + ")");
                    } else {
                        Log.d(TAG, "Skipping default: " + settingKey + " (current: " + currentValue + 
                            ", default: " + defaultValue + ")");
                    }
                }
            }
        }
        
        Log.d(TAG, "Exported " + tunableSettings.size() + " non-default tunable settings");
        return tunableSettings;
    }
    
    /**
     * Import tunable settings from a map (using native types)
     * @param context Context for SharedPreferences
     * @param tunableSettings Map of tunable settings
     */
    public static void importTunableSettings(Context context, Map<String, Object> tunableSettings) {
        if (tunableSettings == null || tunableSettings.isEmpty()) {
            Log.d(TAG, "No tunable settings to import");
            return;
        }
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        
        int importedCount = 0;
        
        for (Map.Entry<String, Object> entry : tunableSettings.entrySet()) {
            String settingKey = entry.getKey(); // Format: "ClassName.fieldName"
            Object value = entry.getValue();
            
            // Parse the key
            String[] parts = settingKey.split("\\.");
            if (parts.length != 2) {
                Log.w(TAG, "Invalid tunable setting key: " + settingKey);
                continue;
            }
            
            String className = parts[0];
            String fieldName = parts[1];
            String prefKey = "pref_tunable_" + className.toLowerCase() + "_" + fieldName.toLowerCase();
            
            // Find the field to determine if it's float or int
            Class<?> targetClass = findRegisteredClass(className);
            if (targetClass != null) {
                try {
                    Field field = targetClass.getDeclaredField(fieldName);
                    if (field.isAnnotationPresent(Tunable.class)) {
                        Tunable annotation = field.getAnnotation(Tunable.class);
                        float step = annotation.step();
                        boolean isFloat = (step != Math.floor(step));
                        
                        // Store as native type
                        if (isFloat) {
                            editor.putFloat(prefKey, ((Number) value).floatValue());
                        } else {
                            editor.putInt(prefKey, ((Number) value).intValue());
                        }
                        importedCount++;
                        Log.d(TAG, "Imported tunable: " + settingKey + " = " + value);
                    }
                } catch (NoSuchFieldException e) {
                    Log.w(TAG, "Field not found: " + settingKey, e);
                }
            }
        }
        
        editor.apply();
        Log.d(TAG, "Imported " + importedCount + " tunable settings");
    }
    
    private static Class<?> findRegisteredClass(String className) {
        for (Class<?> clazz : REGISTERED_CLASSES) {
            if (clazz.getSimpleName().equals(className)) {
                return clazz;
            }
        }
        return null;
    }
}

