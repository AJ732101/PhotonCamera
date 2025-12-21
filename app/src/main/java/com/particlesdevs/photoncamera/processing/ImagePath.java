package com.particlesdevs.photoncamera.processing;

import com.particlesdevs.photoncamera.api.CameraMode;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.util.FileManager;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImagePath {
    public static String generateNewFileName() {
        if (!PhotonCamera.getSpecific().specificSetting.recPrefix.isEmpty()) {
            return PhotonCamera.getSpecific().specificSetting.recPrefix + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        }
        else {
            return "PVC_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        }
    }

    public static Path newDNGFilePath() {
        return getNewImageFilePath("dng");
    }

    public static Path newJPGFilePath() {
        return getNewImageFilePath("jpg");
    }

    public static Path newHEICFilePath() {
        return getNewImageFilePath("heic");
    }

    public static Path newHEIFFilePath() {
        return getNewImageFilePath("heif");
    }

    public static Path newAudioFilePath() {
        return getNewImageFilePath("m4a");
    }

    public static Path newAVIFFilePath() {
        return getNewImageFilePath("avif");
    }

    public static Path newAPVFilePath() {
        return getNewImageFilePath("apv");
    }

    public static Path newImageFilePath() {
        return getNewImageFilePath("");
    }

    public static Path getNewImageFilePath(String extension) {
        File dir = FileManager.sDCIM_CAMERA;
        if (extension.equalsIgnoreCase("dng")) {
            dir = FileManager.sPHOTON_RAW_DIR;
        }
        else if (extension.equalsIgnoreCase("heif")) {
            dir = FileManager.sPHOTON_TEN_BIT_HEIC_DIR;
        }
        else if (extension.equalsIgnoreCase("avif")) {
            dir = FileManager.sPHOTON_AVIF_DIR;
        }
        else if (extension.equalsIgnoreCase("apv")) {
            dir = FileManager.sPHOTON_APV_DIR;
        }
        else if (extension.equalsIgnoreCase("m4a")) {
            dir = FileManager.sPHOTON_M4A_DIR;
        }
        String addOptions = "";
        if (PhotonCamera.getSettings().zoom2X) {
            addOptions += "_2x";
        }
        if (PhotonCamera.getSettings().frameCount == 1) {
            if ((PhotonCamera.getSettings().noiseProcessing != 0) || (PhotonCamera.getSettings().edgeProcessing != 0)) {
                if (PhotonCamera.getSettings().noiseProcessing != 0) {
                    addOptions += "_N";
                }
                if (PhotonCamera.getSettings().edgeProcessing != 0) {
                    addOptions += "_E";
                }
            }
        }
        if ((extension.isEmpty() || extension.isBlank()) && PhotonCamera.getSettings().selectedMode.equals(CameraMode.UNLIMITED)) {
            extension = "jpg";
        }

        return Paths.get(dir.getAbsolutePath(), generateNewFileName() + addOptions + '.' + extension);
    }

    public static Path getNewImageFolderPath() {
        File dir = FileManager.sPHOTON_RAW_DIR;
        return Paths.get(dir.getAbsolutePath(), generateNewFileName());
    }
}
