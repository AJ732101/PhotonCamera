package com.particlesdevs.photoncamera.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;

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
        
        // --- Comprehensive App Search ---
        // We query for multiple standard gallery intents and merge the results.
        Map<String, ResolveInfo> appMap = new HashMap<>();

        // 1. Query for apps that can VIEW images
        Intent viewIntent = new Intent(Intent.ACTION_VIEW, null);
        viewIntent.setType("image/*");
        List<ResolveInfo> viewApps = pm.queryIntentActivities(viewIntent, 0);
        for (ResolveInfo info : viewApps) {
            appMap.put(info.activityInfo.packageName, info);
        }

        // 2. Query for apps that can PICK images
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        List<ResolveInfo> pickApps = pm.queryIntentActivities(pickIntent, 0);
        for (ResolveInfo info : pickApps) {
            appMap.put(info.activityInfo.packageName, info);
        }

        // Convert the map back to a list to remove duplicates
        List<ResolveInfo> launchables = new ArrayList<>(appMap.values());

        // Sort the final list alphabetically by app name
        Collections.sort(launchables, new ResolveInfo.DisplayNameComparator(pm));

        final AppChooserAdapter adapter = new AppChooserAdapter(this, launchables);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ResolveInfo selectedApp = adapter.getItem(position);
                if (selectedApp != null) {
                    String selectedPackageName = selectedApp.activityInfo.packageName;

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    prefs.edit().putString(KEY_DEFAULT_GALLERY_PACKAGE, selectedPackageName).apply();

                    Toast.makeText(getApplicationContext(), "Default gallery set to: " + selectedApp.loadLabel(pm), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }
}
