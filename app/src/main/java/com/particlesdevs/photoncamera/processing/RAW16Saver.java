package com.particlesdevs.photoncamera.processing;

import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.exifinterface.media.ExifInterface;

import com.particlesdevs.photoncamera.ui.camera.views.viewfinder.MainRenderer;
import com.particlesdevs.photoncamera.util.Log;

import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.api.CameraMode;
import com.particlesdevs.photoncamera.app.PhotonCamera;

import java.io.IOException;

public class RAW16Saver extends DefaultSaver{
    private static final String TAG = "RAW16Saver";
    public RAW16Saver(ProcessingEventsListener processingEventsListener) {
        super(processingEventsListener);
    }

    public void addImage(Image image, int orientation, int targetFormat, int quality, Bundle metadata, MainRenderer renderer) throws IOException {
        switch (PhotonCamera.getSettings().selectedMode) {
            case RAWVIDEO:
                Log.d(TAG, "rawvideoaddImage: " + this + " " + mRawVideoProcessor);
                mRawVideoProcessor.videoCycle(image);
                //image.close();
                bufferLock = false;
                break;
            case UNLIMITED:
                Log.d(TAG, "unlimitedaddImage: " + this + " " + mUnlimitedProcessor);
                mUnlimitedProcessor.unlimitedCycle(image);
                image.close();
                bufferLock = false;
                break;
            default:
                Log.d(TAG, "start buffer size:" + IMAGE_BUFFER.size());
                image.getFormat();
                /*while (bufferLock){
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ignored) {}
                }*/
                IMAGE_BUFFER.add(getFrame(image));
                image.close();
                bufferLock = false;
        }
    }
}
