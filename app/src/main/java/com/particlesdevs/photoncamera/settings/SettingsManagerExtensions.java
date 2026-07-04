package com.particlesdevs.photoncamera.settings;

import android.content.SharedPreferences;

/**
 * Extension methods for SettingsManager to support dynamic keys with typed values
 */
public class SettingsManagerExtensions {
    
    /**
     * Get float value with dynamic string key (using native float storage)
     * Robust version that handles String-encoded values from legacy/restored settings
     */
    public static Float getFloat(SettingsManager manager, String scope, String key, Float defaultValue) {
        SharedPreferences preferences = manager.getDefaultPreferences();
        if (scope.equals(SettingsManager.SCOPE_GLOBAL)) {
            try {
                return preferences.getFloat(key, defaultValue);
            } catch (ClassCastException e) {
                // Handle legacy String-stored values
                try {
                    String value = preferences.getString(key, null);
                    if (value != null) {
                        return Float.parseFloat(value);
                    }
                } catch (Exception ignored) {}
            }
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
     * Robust version that handles String-encoded values
     */
    public static Integer getInteger(SettingsManager manager, String scope, String key, Integer defaultValue) {
        SharedPreferences preferences = manager.getDefaultPreferences();
        if (scope.equals(SettingsManager.SCOPE_GLOBAL)) {
            try {
                return preferences.getInt(key, defaultValue);
            } catch (ClassCastException e) {
                // Handle legacy String-stored values
                try {
                    String value = preferences.getString(key, null);
                    if (value != null) {
                        return Integer.parseInt(value);
                    }
                } catch (Exception ignored) {}
            }
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
     * Robust version that handles String-encoded values
     */
    public static boolean getBoolean(SettingsManager manager, String scope, String key, boolean defaultValue) {
        SharedPreferences preferences = manager.getDefaultPreferences();
        if (scope.equals(SettingsManager.SCOPE_GLOBAL)) {
            try {
                return preferences.getBoolean(key, defaultValue);
            } catch (ClassCastException e) {
                // Handle legacy String-stored values (often stored as "0" or "1")
                try {
                    String value = preferences.getString(key, null);
                    if (value != null) {
                        if (value.equals("1") || value.equalsIgnoreCase("true")) return true;
                        if (value.equals("0") || value.equalsIgnoreCase("false")) return false;
                        return Integer.parseInt(value) != 0;
                    }
                } catch (Exception ignored) {}
            }
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


