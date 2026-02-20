package com.particlesdevs.photoncamera.processing;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;

import androidx.exifinterface.media.ExifInterface;

import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.capture.CaptureController;
import com.particlesdevs.photoncamera.control.GyroBurst;
import com.particlesdevs.photoncamera.processing.processor.ProcessorBase;
import com.particlesdevs.photoncamera.ui.camera.views.viewfinder.MainRenderer;

import java.util.ArrayList;
import java.util.HashMap;

public class SaverImplementation {
    private static final String TAG = "SaverImplementation";
    public volatile boolean bufferLock = true;
    public volatile boolean newBurst = false;
    public static ArrayList<ImageFrame> IMAGE_BUFFER = new ArrayList<>();
    public int frameCount = 0;
    private int imageFormat;
    public final ProcessingEventsListener processingEventsListener;

    public ImageFrame getFrame(Image image) {
        try {
            image.getFormat();
        } catch (Exception e) {
            // This image is not valid, skip it
            return null;
        }
        int width;
        int height;
        int offset = 0;
        int capacity = image.getPlanes()[0].getBuffer().capacity();
        var imageFormat = image.getFormat();
        boolean noStride = false;
        if ((imageFormat == ImageFormat.RAW10) || (imageFormat == ImageFormat.RAW12)) {
            width = image.getWidth();
            height = image.getHeight();
            //noStride = true;
        } else {
            var a = image.getPlanes()[0].getRowStride();
            var b = image.getPlanes()[0].getPixelStride();
            if (a != 0 && b != 0) {
                width = image.getPlanes()[0].getRowStride() / image.getPlanes()[0].getPixelStride();
            }
            else {
                width = image.getWidth();
            }
            height = image.getHeight();
        }
        if (PhotonCamera.getSettings().aspect169) {
            if (width > height) {
                height = width * 9 / 16;
                int offsetH = (image.getHeight() - height) / 2;
                offsetH -= offsetH % 2;
                if (image.getPlanes()[0].getRowStride() != 0) {
                    offset = image.getPlanes()[0].getRowStride() * offsetH;
                    capacity = image.getPlanes()[0].getRowStride() * height;
                }
            }
        }
        ImageFrame frame = null;
        if (!noStride && (image.getPlanes()[0].getRowStride() != 0)) {
            frame = new ImageFrame(image.getPlanes()[0].getBuffer(), image.getFormat(), width, image.getPlanes()[0].getRowStride(), offset, capacity);
        }
        else {
            frame = new ImageFrame(image.getPlanes()[0].getBuffer(), image.getFormat(), width, image.getWidth(), offset, capacity);
        }
        frame.timestamp = image.getTimestamp();

        frame.width = width;
        frame.height = height;

        return frame;
    }

    final ProcessorBase.ProcessingCallback processingCallback = new ProcessorBase.ProcessingCallback() {
        @Override
        public void onStarted() {
            CaptureController.isProcessing = true;
        }

        @Override
        public void onFailed() {
            onFinished();
        }

        @Override
        public void onFinished() {
            //clearImageReader(imageReader);
            CaptureController.isProcessing = false;
        }
    };
    public SaverImplementation(ProcessingEventsListener processingEventsListener){
        this.processingEventsListener = processingEventsListener;
    }

    public void addImage(Image image, int orientation, int targetFormat, int quality, Bundle metadata, MainRenderer renderer) {
        //image.close();
    }

    void addRAW10(Image image){
        //image.close();
    }
    protected void clearImageReader(ImageReader reader) {
        while (true) {
            try {
                reader.acquireNextImage().close();
            } catch (Exception ignored){
                break;
            }
        }
        //reader.close();
    }

    public void runRaw(int imageFormat, CameraCharacteristics characteristics, CaptureResult captureResult, CaptureRequest captureRequest, ArrayList<GyroBurst> burstShakiness, int cameraRotation, HashMap<Long, Double> exposures) {
        this.imageFormat = imageFormat;
    }
    public void processStart(int imageFormat, CameraCharacteristics characteristics, CaptureResult captureResult, CaptureRequest captureRequest, int cameraRotation) {
        this.imageFormat = imageFormat;
    }
    public void processEnd(){
    }
}