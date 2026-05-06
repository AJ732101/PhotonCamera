package com.particlesdevs.photoncamera.processing;

import com.particlesdevs.photoncamera.settings.annotations.Tunable;

public class ImageSaverSettings {

    @Tunable(
            title = "Crop Edge",
            category = "ImageSaver",
            description = "When enabled, crop from the edge (top) of the image instead of the center",
            defaultValue = 0, min = 0, max = 1, step = 1
    )
    public boolean cropType;
}
