package com.particlesdevs.photoncamera.ui;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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

public class AppChooserAdapter extends ArrayAdapter<ResolveInfo> {

    private final PackageManager pm;

    public AppChooserAdapter(@NonNull Context context, List<ResolveInfo> apps) {
        super(context, 0, apps);
        this.pm = context.getPackageManager();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_app_chooser, parent, false);
        }

        ResolveInfo currentApp = getItem(position);

        ImageView iconView = listItemView.findViewById(R.id.app_icon);
        TextView nameView = listItemView.findViewById(R.id.app_name);

        if (currentApp != null) {
            iconView.setImageDrawable(currentApp.loadIcon(pm));
            nameView.setText(currentApp.loadLabel(pm));
        }

        return listItemView;
    }
}
