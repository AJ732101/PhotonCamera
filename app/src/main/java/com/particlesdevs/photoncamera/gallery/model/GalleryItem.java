package com.particlesdevs.photoncamera.gallery.model;

import android.widget.Checkable;

import com.particlesdevs.photoncamera.gallery.files.ImageFile;
import com.particlesdevs.photoncamera.gallery.files.MediaFile;

import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Vibhor Srivastava on October 14, 2021
 */
public class GalleryItem implements Checkable {
    private final MediaFile file;
    private final String mediaTypeTag;
    private boolean isChecked;
    private String displayName;
    private final ArrayList<GalleryItem> files = new ArrayList<>(0);

    public GalleryItem(MediaFile file) {
        this.file = file;
        this.mediaTypeTag = getTagName(file.getDisplayName());
        this.displayName = file.getDisplayName();
    }
    public static GalleryItem createEmpty(){
        return new GalleryItem(new ImageFile(0,null,"",0,0,""));
    }

    public String getMediaTypeTag() {
        return mediaTypeTag;
    }

    private String getTagName(String fileName) {
        String ext = FilenameUtils.getExtension(fileName);
        if (ext.equalsIgnoreCase("dng")) {
            return "RAW";
        } else if (ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg")) {
            return "";
        }
        return ext.toUpperCase(Locale.ROOT);
    }

    public MediaFile getFile() {
        return file;
    }

    public ArrayList<GalleryItem> getFiles() {
        return files;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    @Override
    public void toggle() {
        isChecked = !isChecked;
    }
}
