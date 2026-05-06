package com.particlesdevs.photoncamera.settings;

import android.content.SharedPreferences;

/**
 * Extension methods for SettingsManager to support dynamic keys with typed values
 */
public class SettingsManagerExtensions {
    
    /**
     * Get float value with dynamic string key (using native float storage)
     */
    public static Float getFloat(SettingsManager manager, String scope, String key, Float defaultValue) {
        SharedPreferences preferences = manager.getDefaultPreferences();
        if (scope.equals(SettingsManager.SCOPE_GLOBAL)) {
            return preferences.getFloat(key, defaultValue);
        }
        return defaultValue;
    }
    
    /**
     * Set float value with dynamic string key
     */
    public static void setFloat(SettingsManager manager, String scope, String key, float value) {
        SharedPreferences preferences = manager.getDefaultPreferences();
        if (scope.equals(SettingsManager.SCOPE_GLOBAL)) {
            preferences.edit().putFloat(key, value).apply();
        }
    }
    
    /**
     * Get integer value with dynamic string key (using native int storage)
     */
    public static Integer getInteger(SettingsManager manager, String scope, String key, Integer defaultValue) {
        SharedPreferences preferences = manager.getDefaultPreferences();
        if (scope.equals(SettingsManager.SCOPE_GLOBAL)) {
            return preferences.getInt(key, defaultValue);
        }
        return defaultValue;
    }
    
    /**
     * Set integer value with dynamic string key
     */
    public static void setInt(SettingsManager manager, String scope, String key, int value) {
        SharedPreferences preferences = manager.getDefaultPreferences();
        if (scope.equals(SettingsManager.SCOPE_GLOBAL)) {
            preferences.edit().putInt(key, value).apply();
        }
    }
    
    /**
     * Get boolean value with dynamic string key (using native boolean storage)
     */
    public static boolean getBoolean(SettingsManager manager, String scope, String key, boolean defaultValue) {
        SharedPreferences preferences = manager.getDefaultPreferences();
        if (scope.equals(SettingsManager.SCOPE_GLOBAL)) {
            return preferences.getBoolean(key, defaultValue);
        }
        return defaultValue;
    }
    
    /**
     * Set boolean value with dynamic string key
     */
    public static void setBoolean(SettingsManager manager, String scope, String key, boolean value) {
        SharedPreferences preferences = manager.getDefaultPreferences();
        if (scope.equals(SettingsManager.SCOPE_GLOBAL)) {
            preferences.edit().putBoolean(key, value).apply();
        }
    }
}


