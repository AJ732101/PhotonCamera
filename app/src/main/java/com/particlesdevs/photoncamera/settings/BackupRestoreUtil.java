package com.particlesdevs.photoncamera.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.util.FileManager;
import com.particlesdevs.photoncamera.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class BackupRestoreUtil {
    private static final String TAG = "BackupRestoreUtil";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static String backupSettings(Context context, String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "Error: Empty file name";
        }

        String pkgName = context.getPackageName();
        Map<String, Object> unifiedBackup = new LinkedHashMap<>();

        try {
            // 1. Global Preferences
            SharedPreferences globalPrefs = context.getSharedPreferences(pkgName + "_preferences", Context.MODE_PRIVATE);
            unifiedBackup.put("global", globalPrefs.getAll());

            // 2. Per Lens Settings (Parse internal JSON strings)
            SharedPreferences perLensPrefs = context.getSharedPreferences(pkgName + "_per_lens", Context.MODE_PRIVATE);
            Map<String, ?> perLensRaw = perLensPrefs.getAll();
            Map<String, Object> perLensParsed = new HashMap<>();
            for (Map.Entry<String, ?> entry : perLensRaw.entrySet()) {
                if (entry.getValue() instanceof String) {
                    try {
                        Map<String, Object> cameraSettings = GSON.fromJson((String) entry.getValue(), new TypeToken<Map<String, Object>>() {}.getType());
                        perLensParsed.put(entry.getKey(), cameraSettings);
                    } catch (Exception e) {
                        perLensParsed.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            unifiedBackup.put("per_lens", perLensParsed);

            // 3. Cameras Metadata
            SharedPreferences camerasPrefs = context.getSharedPreferences(pkgName + "_cameras", Context.MODE_PRIVATE);
            Map<String, ?> camerasRaw = camerasPrefs.getAll();
            Map<String, Object> camerasParsed = new HashMap<>();
            for (Map.Entry<String, ?> entry : camerasRaw.entrySet()) {
                if (entry.getValue() instanceof String && (((String) entry.getValue()).startsWith("{") || ((String) entry.getValue()).startsWith("["))) {
                    try {
                        Object parsed = GSON.fromJson((String) entry.getValue(), Object.class);
                        camerasParsed.put(entry.getKey(), parsed);
                    } catch (Exception e) {
                        camerasParsed.put(entry.getKey(), entry.getValue());
                    }
                } else {
                    camerasParsed.put(entry.getKey(), entry.getValue());
                }
            }
            unifiedBackup.put("cameras", camerasParsed);

            // 4. Devices Data
            SharedPreferences devicesPrefs = context.getSharedPreferences(pkgName + "_devices", Context.MODE_PRIVATE);
            unifiedBackup.put("devices", devicesPrefs.getAll());

            // 5. Tunable Settings
            TunableSettingsManager.ensureTunableClassesRegistered();
            unifiedBackup.put("tunable_settings", TunableSettingsManager.exportTunableSettings(context, false));

            // Write to JSON file
            File destFile = new File(FileManager.sPHOTON_DIR, fileName + ".json");
            String jsonOutput = GSON.toJson(unifiedBackup);
            
            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                fos.write(jsonOutput.getBytes(StandardCharsets.UTF_8));
            }

            return "Backup saved to: " + destFile.getName();
        } catch (Exception e) {
            Log.e(TAG, "Backup failed", e);
            return "Error: " + e.getLocalizedMessage();
        }
    }

    public static String restorePreferences(Context context, String fileName) {
        File srcFile = new File(FileManager.sPHOTON_DIR, fileName);
        if (!srcFile.exists()) {
            return "Error: File not found";
        }

        String pkgName = context.getPackageName();

        try {
            String jsonContent;
            try (FileInputStream fis = new FileInputStream(srcFile);
                 Scanner scanner = new Scanner(fis, StandardCharsets.UTF_8.name())) {
                jsonContent = scanner.useDelimiter("\\A").next();
            }
            
            Map<String, Object> unifiedBackup = GSON.fromJson(jsonContent, new TypeToken<Map<String, Object>>() {}.getType());

            // 1. Restore Global
            if (unifiedBackup.containsKey("global")) {
                restoreMapToPrefs(context, pkgName + "_preferences", (Map<String, ?>) unifiedBackup.get("global"));
            }

            // 2. Restore Per Lens (Convert back to JSON strings)
            if (unifiedBackup.containsKey("per_lens")) {
                Map<String, Object> perLensParsed = (Map<String, Object>) unifiedBackup.get("per_lens");
                Map<String, String> perLensStrings = new HashMap<>();
                if (perLensParsed != null) {
                    for (Map.Entry<String, Object> entry : perLensParsed.entrySet()) {
                        perLensStrings.put(entry.getKey(), GSON.toJson(entry.getValue()));
                    }
                }
                restoreMapToPrefs(context, pkgName + "_per_lens", perLensStrings);
            }

            // 3. Restore Cameras
            if (unifiedBackup.containsKey("cameras")) {
                Map<String, Object> camerasParsed = (Map<String, Object>) unifiedBackup.get("cameras");
                Map<String, Object> camerasToStore = new HashMap<>();
                if (camerasParsed != null) {
                    for (Map.Entry<String, Object> entry : camerasParsed.entrySet()) {
                        if (entry.getValue() instanceof Map || entry.getValue() instanceof java.util.List) {
                            camerasToStore.put(entry.getKey(), GSON.toJson(entry.getValue()));
                        } else {
                            camerasToStore.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
                restoreMapToPrefs(context, pkgName + "_cameras", camerasToStore);
            }

            // 4. Restore Devices
            if (unifiedBackup.containsKey("devices")) {
                restoreMapToPrefs(context, pkgName + "_devices", (Map<String, ?>) unifiedBackup.get("devices"));
            }

            // 5. Restore Tunable Settings
            if (unifiedBackup.containsKey("tunable_settings")) {
                Map<String, Object> tunableMap = (Map<String, Object>) unifiedBackup.get("tunable_settings");
                TunableSettingsManager.importTunableSettings(context, tunableMap);
            }

            PhotonCamera.restartWithDelay(context, 1000);
            return "Restored successfully. Restarting...";
        } catch (Exception e) {
            Log.e(TAG, "Restore failed", e);
            return "Restore failed: " + e.getLocalizedMessage();
        }
    }

    private static void restoreMapToPrefs(Context context, String prefName, Map<String, ?> data) {
        if (data == null) return;
        SharedPreferences.Editor editor = context.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit();
        editor.clear();
        for (Map.Entry<String, ?> entry : data.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Boolean) editor.putBoolean(entry.getKey(), (Boolean) value);
            else if (value instanceof Integer) editor.putInt(entry.getKey(), (Integer) value);
            else if (value instanceof Long) editor.putLong(entry.getKey(), (Long) value);
            else if (value instanceof Float) editor.putFloat(entry.getKey(), (Float) value);
            else if (value instanceof Double) {
                double d = (Double) value;
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    editor.putInt(entry.getKey(), (int) d);
                } else {
                    editor.putFloat(entry.getKey(), (float) d);
                }
            } else if (value instanceof String) editor.putString(entry.getKey(), (String) value);
        }
        editor.apply();
    }

    public static boolean resetPreferences(Context context) {
        File data_dir = context.getDataDir();
        File shared_prefs_dir = new File(data_dir, "shared_prefs");
        try {
            FileUtils.deleteDirectory(shared_prefs_dir);
            Log.d(TAG, "Preferences reset (shared_prefs directory deleted)");
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Reset failed", e);
            return false;
        }
    }
}
