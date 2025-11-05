package com.particlesdevs.photoncamera.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.particlesdevs.photoncamera.R;

import java.util.List;

// Helper class to hold app information
class AppEntry {
    final String appName;
    final String packageName;
    final Drawable icon;

    AppEntry(String appName, String packageName, Drawable icon) {
        this.appName = appName;
        this.packageName = packageName;
        this.icon = icon;
    }
}

public class AppChooserAdapter extends ArrayAdapter<AppEntry> {

    public AppChooserAdapter(@NonNull Context context, List<AppEntry> apps) {
        super(context, 0, apps);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_app_chooser, parent, false);
        }

        AppEntry currentApp = getItem(position);

        ImageView iconView = listItemView.findViewById(R.id.app_icon);
        TextView nameView = listItemView.findViewById(R.id.app_name);

        if (currentApp != null) {
            iconView.setImageDrawable(currentApp.icon);
            nameView.setText(currentApp.appName);
        }

        return listItemView;
    }
}
