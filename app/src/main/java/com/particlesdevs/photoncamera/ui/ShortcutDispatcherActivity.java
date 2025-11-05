package com.particlesdevs.photoncamera.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.particlesdevs.photoncamera.gallery.ui.GalleryActivity;
import com.particlesdevs.photoncamera.ui.settings.SettingsActivity;

public class ShortcutDispatcherActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent shortcutIntent = getIntent();
        String action = shortcutIntent.getAction();

        if (action != null) {
            Intent targetIntent = null;

            if ("com.particlesdevs.photoncamera.action.OPEN_SETTINGS".equals(action)) {
                targetIntent = new Intent(this, SettingsActivity.class);
            } else if ("com.particlesdevs.photoncamera.action.OPEN_GALLERY".equals(action)) {
                targetIntent = new Intent(this, GalleryActivity.class);
            } else if ("com.particlesdevs.photoncamera.action.SET_DEFAULT_GALLERY".equals(action)) {
                targetIntent = new Intent(this, GalleryChooserActivity.class);
            }

            if (targetIntent != null) {
                // FLAG_ACTIVITY_NEW_TASK is crucial to bypass the singleTask launchMode of the main app.
                targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(targetIntent);
            }
        }

        finish(); // Dispatcher is done, close it immediately
    }
}
