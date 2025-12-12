package com.particlesdevs.photoncamera.processing;

import android.media.Image;
import android.os.Bundle;

import androidx.exifinterface.media.ExifInterface;

import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.processing.DefaultSaver;
import com.particlesdevs.photoncamera.processing.ImagePath;
import com.particlesdevs.photoncamera.processing.ProcessingEventsListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class HEICSaver extends DefaultSaver {
    private static final String TAG = "HEICSaver";
    public HEICSaver(ProcessingEventsListener processingEventsListener) {
        super(processingEventsListener);
    }

    public void addImage(Image image, int orientation, int targetFormat, int quality, Bundle metadata) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        try {
            //IMAGE_BUFFER.add(getFrame(image));
            byte[] bytes = new byte[buffer.remaining()];
            if (IMAGE_BUFFER.size() == PhotonCamera.getCaptureController().mMeasuredFrameCnt && PhotonCamera.getSettings().frameCount != 1) {
                Path heicPath = ImagePath.newHEICFilePath();
                buffer.duplicate().get(bytes);
                Files.write(heicPath, bytes);
                IMAGE_BUFFER.clear();
            }
            if (PhotonCamera.getSettings().frameCount == 1) {
                Path heicPath = ImagePath.newHEICFilePath();
                IMAGE_BUFFER.clear();
                buffer.get(bytes);
                Files.write(heicPath, bytes);
                image.close();
                processingEventsListener.onProcessingFinished("HEIC: Single Frame, Not Processed!");
                processingEventsListener.notifyImageSavedStatus(true, heicPath);
            }
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }
}
