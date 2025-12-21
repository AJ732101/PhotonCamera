package com.particlesdevs.photoncamera.api;

import androidx.annotation.StringRes;

import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.app.PhotonCamera;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum CameraMode {
    UNLIMITED(R.string.mode_unlimited),
    RAWVIDEO(R.string.mode_rawvideo),
    MOTION(R.string.mode_motion),
    PHOTO(R.string.mode_photo),
    NIGHT(R.string.mode_night),
    VIDEO(R.string.mode_video);

    public final int stringId;

    CameraMode(@StringRes int stringId) {
        this.stringId = stringId;
    }

    @StringRes
    public int getStringId() {
        return stringId;
    }

    public static CameraMode valueOf(int modeOrdinal) {
        for (CameraMode mode : values()) {
            if (modeOrdinal == mode.ordinal()) {
                return mode;
            }
        }
        return PHOTO;
    }

    public static Integer[] nameIds() {
        return Stream.of(values()).map(mode -> mode.stringId).toArray(Integer[]::new);
    }

    public static List<CameraMode> getAvailableModes() {
        return Stream.of(values())
                .filter(mode -> {
                    switch (mode) {
                        case UNLIMITED:
                            return PhotonCamera.getSpecific().specificSetting.modeShowUnlimited;
                        case MOTION:
                            return PhotonCamera.getSpecific().specificSetting.modeShowMotion;
                        case NIGHT:
                            return PhotonCamera.getSpecific().specificSetting.modeShowNight;
                        case RAWVIDEO:
                            return PhotonCamera.getSpecific().specificSetting.modeShowRawVideo;
                        default:
                            return true;
                    }
                })
                .collect(Collectors.toList());
    }
}
