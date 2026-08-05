package com.particlesdevs.photoncamera.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.particlesdevs.photoncamera.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GalleryChooserActivity extends Activity {

    public static final String KEY_DEFAULT_GALLERY_PACKAGE = "default_gallery_package";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_chooser);

        ListView listView = findViewById(R.id.gallery_list_view);

        final PackageManager pm = getPackageManager();
        
        Map<String, AppEntry> appMap = new HashMap<>();

        // 1. Add this app's own gallery manually
        try {
            ComponentName componentName = new ComponentName(this, com.particlesdevs.photoncamera.gallery.ui.GalleryActivity.class);
            Drawable icon = pm.getActivityIcon(componentName);
            String appName = getString(R.string.gallery_name);
            appMap.put(getPackageName(), new AppEntry(appName, getPackageName(), icon));
        } catch (PackageManager.NameNotFoundException e) {
            // Should not happen, but handle it gracefully
            e.printStackTrace();
        }

        // 2. Query for all external gallery apps
        Intent viewIntent = new Intent(Intent.ACTION_VIEW, null);
        viewIntent.setType("image/*");
        List<ResolveInfo> viewApps = pm.queryIntentActivities(viewIntent, 0);
        for (ResolveInfo info : viewApps) {
            appMap.put(info.activityInfo.packageName, new AppEntry(info.loadLabel(pm).toString(), info.activityInfo.packageName, info.loadIcon(pm)));
        }

        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        List<ResolveInfo> pickApps = pm.queryIntentActivities(pickIntent, 0);
        for (ResolveInfo info : pickApps) {
            appMap.put(info.activityInfo.packageName, new AppEntry(info.loadLabel(pm).toString(), info.activityInfo.packageName, info.loadIcon(pm)));
        }

        // Convert the map back to a list to remove duplicates
        List<AppEntry> launchables = new ArrayList<>(appMap.values());

        // Sort the final list alphabetically by app name
        Collections.sort(launchables, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry a1, AppEntry a2) {
                return a1.appName.compareToIgnoreCase(a2.appName);
            }
        });

        final AppChooserAdapter adapter = new AppChooserAdapter(this, launchables);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppEntry selectedApp = adapter.getItem(position);
                if (selectedApp != null) {
                    String selectedPackageName = selectedApp.packageName;

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    prefs.edit().putString(KEY_DEFAULT_GALLERY_PACKAGE, selectedPackageName).apply();

                    Toast.makeText(getApplicationContext(), "Default gallery set to: " + selectedApp.appName, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }
}
